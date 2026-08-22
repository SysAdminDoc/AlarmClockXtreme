package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.local.CommuteHistoryStore
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.CommuteRouteRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.CommuteAlarmPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * v1.2.0: Calendar auto-alarm worker.
 * Each run looks at tomorrow's first timed calendar event (expanded from
 * `CalendarContract.Instances` so RRULE-based recurring events count) and
 * keeps a single auto-alarm pointed at it. v1.10.6 moved the cadence to a
 * settings-aware 15-minute periodic refresh plus one-shot refreshes from app
 * start / Settings changes, so moved meetings are picked up without waiting
 * for the next daily pass.
 *
 * Hardening over the original implementation:
 *  - **Single reusable auto-alarm row** identified by [AUTO_PROFILE]. The
 *    previous version inserted a fresh `Alarm` on every run, which produced
 *    7 duplicates after a week.
 *  - Queries `CalendarContract.Instances` (the expanded view) instead of raw
 *    `Events`, so a weekly stand-up scheduled with RRULE actually shows up.
 *  - Uses `specificDate` so the alarm only fires on tomorrow's date — the
 *    next worker run will re-target it.
 *  - Ignores all-day events, because "first meeting" should not become a
 *    midnight alarm for a birthday or PTO banner.
 *  - If tomorrow has no events, the auto-alarm is disabled (cancelled), not
 *    deleted — preserving the row so user-edits to time/sound persist.
 *  - Per-event cap in code instead of relying on `LIMIT` inside the cursor
 *    sort order (which CalendarContract sometimes ignores).
 */
@HiltWorker
class CalendarAutoAlarmWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val weatherRepository: WeatherRepository,
    private val commuteRouteRepository: CommuteRouteRepository,
    private val commuteHistoryStore: CommuteHistoryStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        /** Identifier stored in `Alarm.profileName` to recognise the row this
         *  worker owns. Users browsing alarm lists can also see the badge. */
        private const val AUTO_PROFILE = "calendar_auto"
        private const val GROUP = "Calendar"
        private const val INPUT_REFRESH_SOURCE = "refresh_source"
        private const val PERIODIC_INTERVAL_MINUTES = 15L

        const val WORK_NAME = "calendar_auto_alarm"
        private const val REFRESH_WORK_NAME = "calendar_auto_alarm_refresh"

        /**
         * The periodic pass and a one-shot refresh are separate unique work,
         * so they can run at the same moment in the same process. Both look
         * for an existing auto-alarm and insert one if there is none, which
         * used to leave two enabled `calendar_auto` rows behind. Serialising
         * the whole read-modify-write closes that window.
         */
        private val runLock = Mutex()

        /** Beyond this a calendar read is failing for a reason a retry cannot fix. */
        private const val MAX_RETRY_ATTEMPTS = 5

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CalendarAutoAlarmWorker>(
                PERIODIC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun enqueueRefresh(context: Context, source: String) {
            val request = OneTimeWorkRequestBuilder<CalendarAutoAlarmWorker>()
                .setInputData(workDataOf(INPUT_REFRESH_SOURCE to source.take(40)))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                REFRESH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result = runLock.withLock { syncAutoAlarm() }

    private suspend fun syncAutoAlarm(): Result {
        val settings = preferencesManager.getCurrentSettings()
        if (!settings.calendarAutoAlarmEnabled) {
            // Feature disabled — also disable any existing auto-alarm so it
            // doesn't keep firing after the user toggled it off.
            findExistingAutoAlarm()?.let { existing ->
                if (existing.isEnabled) {
                    repository.setEnabled(existing.id, enabled = false, nextTrigger = 0)
                    scheduler.cancel(existing.id)
                }
            }
            return Result.success()
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) return Result.success()

        val minutesBefore = settings.calendarAutoAlarmMinutesBefore.coerceAtLeast(0)
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowStartMs = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tomorrowEndMs = tomorrowStartMs + 24 * 60 * 60 * 1000L

        val firstEvent = try {
            queryFirstEventBetween(tomorrowStartMs, tomorrowEndMs)
        } catch (_: SecurityException) {
            return Result.success()
        } catch (_: Exception) {
            // Bounded: an unbounded retry keeps a broken provider read alive in
            // WorkManager's queue forever.
            return if (runAttemptCount >= MAX_RETRY_ATTEMPTS) Result.failure() else Result.retry()
        }

        val existing = findExistingAutoAlarm()

        if (firstEvent == null) {
            // Tomorrow is event-free: park any existing auto-alarm in disabled
            // state so it doesn't surprise-fire from a stale schedule.
            if (existing != null && existing.isEnabled) {
                repository.setEnabled(existing.id, enabled = false, nextTrigger = 0)
                scheduler.cancel(existing.id)
            }
            return Result.success()
        }

        val commuteResolution = resolveCommuteAdjustment(settings, firstEvent, minutesBefore)
        val alarmInstant = Instant.ofEpochMilli(
            firstEvent.startMs - commuteResolution.adjustment.totalLeadMinutes * 60_000L
        )
        val alarmZdt = alarmInstant.atZone(ZoneId.systemDefault())
        val alarmDate = alarmZdt.toLocalDate()

        val updated = (existing ?: Alarm()).copy(
            id = existing?.id ?: 0,
            hour = alarmZdt.hour,
            minute = alarmZdt.minute,
            label = buildString {
                append("Before: ${firstEvent.title}")
                if (commuteResolution.usedLearnedEstimate) append(" • learned commute")
            },
            isEnabled = true,
            group = GROUP,
            profileName = AUTO_PROFILE,
            specificDate = alarmDate.toString(),
            // Repeat days deliberately empty — this is a one-shot tied to a date.
            repeatDays = emptySet(),
            nextTriggerTime = 0
        )

        // Cancel any previous schedule before re-arming with the new time/date.
        if (existing != null) scheduler.cancel(existing.id)
        val savedId = repository.save(updated)
        scheduler.schedule(updated.copy(id = savedId))
        return Result.success()
    }

    private suspend fun findExistingAutoAlarm(): Alarm? {
        // The auto-alarm is uniquely identified by profileName, so a single
        // table scan over (typically a handful of) rows suffices. Avoids
        // adding a dedicated DAO query just for one feature.
        val owned = repository.getAll().filter { it.profileName == AUTO_PROFILE }
        // Older builds could race two workers into inserting a second row.
        // Keep the oldest and clear the strays out so the invariant holds.
        owned.drop(1).forEach { stray ->
            scheduler.cancel(stray.id)
            repository.delete(stray)
        }
        return owned.firstOrNull()
    }

    private suspend fun resolveCommuteAdjustment(
        settings: AppSettings,
        event: CalEvent,
        baseLeadMinutes: Int
    ): CommuteResolution = if (!settings.calendarCommuteAwareEnabled || event.location.isBlank()) {
        CommuteResolution(CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = baseLeadMinutes,
            routeDurationMinutes = null,
            baselineCommuteMinutes = settings.calendarCommuteBaselineMinutes,
            weatherExtraMinutes = 0,
            forecastDate = Instant.ofEpochMilli(event.startMs).atZone(ZoneId.systemDefault()).toLocalDate(),
            weather = null
        ))
    } else {
        val eventDate = Instant.ofEpochMilli(event.startMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val weather = loadWeather(settings)
        val liveRouteDurationMinutes = commuteRouteRepository.estimateTransitMinutes(
            apiKey = settings.googleRoutesApiKey,
            originLatitude = settings.lastKnownLatitude,
            originLongitude = settings.lastKnownLongitude,
            destinationQuery = event.location,
            arrivalTime = Instant.ofEpochMilli(event.startMs)
        ).getOrNull()
        if (liveRouteDurationMinutes != null) {
            commuteHistoryStore.record(
                originLatitude = settings.lastKnownLatitude,
                originLongitude = settings.lastKnownLongitude,
                destination = event.location,
                minutes = liveRouteDurationMinutes
            )
        }
        val learnedEstimate = if (liveRouteDurationMinutes == null) {
            commuteHistoryStore.estimate(
                originLatitude = settings.lastKnownLatitude,
                originLongitude = settings.lastKnownLongitude,
                destination = event.location
            )
        } else {
            null
        }
        CommuteResolution(CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = baseLeadMinutes,
            routeDurationMinutes = liveRouteDurationMinutes ?: learnedEstimate?.minutes,
            baselineCommuteMinutes = settings.calendarCommuteBaselineMinutes,
            weatherExtraMinutes = settings.calendarCommuteWeatherExtraMinutes,
            forecastDate = eventDate,
            weather = weather
        ), usedLearnedEstimate = learnedEstimate != null)
    }

    private data class CommuteResolution(
        val adjustment: com.sysadmindoc.alarmclock.domain.CommuteAlarmAdjustment,
        val usedLearnedEstimate: Boolean = false
    )

    private suspend fun loadWeather(settings: AppSettings): WeatherResponse? {
        val haveLocation = settings.lastKnownLatitude != 0.0 || settings.lastKnownLongitude != 0.0
        return weatherRepository.getCachedWeather(
            latitude = settings.lastKnownLatitude.takeIf { haveLocation },
            longitude = settings.lastKnownLongitude.takeIf { haveLocation }
        )
            ?: if (haveLocation) {
                weatherRepository.getWeather(
                    settings.lastKnownLatitude,
                    settings.lastKnownLongitude,
                    settings.temperatureUnit
                ).getOrNull()?.response
            } else {
                null
            }
    }

    private data class CalEvent(val startMs: Long, val title: String, val location: String)

    private fun queryFirstEventBetween(startMs: Long, endMs: Long): CalEvent? {
        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )
        val selection = "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} < ?"
        val selectionArgs = arrayOf(startMs.toString(), endMs.toString())
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        // Build the time-bounded Instances URI (this is what makes RRULE-expanded
        // recurring events appear; querying Events directly misses them).
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMs.toString())
            .appendPath(endMs.toString())
            .build()

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            while (cursor.moveToNext()) {
                if (cursor.getInt(allDayIndex) == 1) continue
                return CalEvent(
                    startMs = cursor.getLong(beginIndex),
                    title = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() } ?: "Calendar Event",
                    location = cursor.getString(locationIndex)?.trim().orEmpty()
                )
            }
        }
        return null
    }
}
