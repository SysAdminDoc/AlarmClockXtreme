package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.annotation.VisibleForTesting
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
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.local.CommuteHistoryStore
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.CommuteRouteRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.CommuteAlarmPolicy
import com.sysadmindoc.alarmclock.service.AlarmRuntimeState
import com.sysadmindoc.alarmclock.service.AlarmService
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
 * Each run finds the next first timed calendar event of a day still to wake
 * for, today's or tomorrow's (expanded from `CalendarContract.Instances` so
 * RRULE-based recurring events count), and keeps a single auto-alarm pointed
 * at it. v1.10.6 moved the cadence to a
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
 *  - Uses `specificDate` so the alarm only fires on its own date. Today's
 *    first event keeps the alarm until it has rung, been missed or been
 *    skipped, then the next run targets tomorrow's.
 *  - Ignores all-day events, because "first meeting" should not become a
 *    midnight alarm for a birthday or PTO banner.
 *  - If there's nothing left to wake for, the auto-alarm is disabled (cancelled), not
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
    private val commuteHistoryStore: CommuteHistoryStore,
    private val eventRepository: AlarmEventRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        /** Identifier stored in `Alarm.profileName` to recognise the row this
         *  worker owns. Users browsing alarm lists can also see the badge. */
        const val AUTO_PROFILE = "calendar_auto"
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

        /**
         * How long a ring that was never dismissed still holds the row, and how
         * far ahead a snooze can be set and still count as one. A snooze count
         * a ring left behind shouldn't freeze a row armed for another day.
         */
        private const val RING_WINDOW_MS = 3 * 60 * 60 * 1000L

        @VisibleForTesting
        internal var clock: () -> Long = { System.currentTimeMillis() }

        /** When [alarm]'s own date and time come up, or null for a row without a date. */
        internal fun dueAt(alarm: Alarm, zone: ZoneId): Long? =
            runCatching { LocalDate.parse(alarm.specificDate) }.getOrNull()
                ?.atTime(alarm.hour, alarm.minute)?.atZone(zone)?.toInstant()?.toEpochMilli()

        /**
         * The row's occurrence came up and hasn't been finished: it's ringing,
         * snoozed, or waiting on a dismissal. Dismissing switches the one-shot
         * off, so until then a pass must not move it to another day. Moving it
         * would drop a snooze, and the cancel that comes with it would take the
         * wake-up check and the snooze count down mid-ring.
         *
         * [inService] is the ringing service naming this alarm. A smart-wake or
         * bad-weather ring starts before the row's time and leaves its trigger
         * where it was, so the row alone can't show it. [snoozeTaken] covers
         * the same early ring once it's snoozed.
         */
        internal fun occurrenceUnderway(
            existing: Alarm?,
            now: Long,
            zone: ZoneId,
            inService: Boolean = false,
            snoozeTaken: Boolean = false
        ): Boolean {
            if (existing == null) return false
            if (inService) return true
            if (!existing.isEnabled) return false
            val due = dueAt(existing, zone)
            val armedAt = existing.nextTriggerTime
            val snoozed = armedAt > now &&
                ((due != null && due <= now) || (snoozeTaken && armedAt - now <= RING_WINDOW_MS))
            val ringing = due != null && armedAt in 1..now && now - armedAt < RING_WINDOW_MS
            return snoozed || ringing
        }

        /**
         * The stored auto-alarm already matches this pass's and is armed for a
         * time still ahead. [registered] is the scheduler's own word that the
         * arm took, since a failed arm still wrote the trigger to the row. The
         * row was saved sanitized, so the rebuilt one is compared the same way.
         */
        internal fun alreadyArmed(existing: Alarm?, updated: Alarm, now: Long, registered: Boolean): Boolean =
            existing != null && registered && existing.nextTriggerTime > now &&
                updated.sanitized().copy(nextTriggerTime = existing.nextTriggerTime) == existing

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
        val now = clock()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        val (todayEvent, tomorrowEvent) = try {
            queryFirstEventOn(today, zone) to queryFirstEventOn(today.plusDays(1), zone)
        } catch (_: SecurityException) {
            return Result.success()
        } catch (_: Exception) {
            // Bounded: an unbounded retry keeps a broken provider read alive in
            // WorkManager's queue forever.
            return if (runAttemptCount >= MAX_RETRY_ATTEMPTS) Result.failure() else Result.retry()
        }

        val existing = findExistingAutoAlarm()
        val inService = existing != null && AlarmService.activeAlarm.get()
            ?.let { it.alarmId == existing.id && !it.isTestRun } == true
        val snoozeTaken = existing != null && AlarmRuntimeState.snoozeCount(context, existing.id) > 0
        if (occurrenceUnderway(existing, now, zone, inService, snoozeTaken)) return Result.success()

        // A day is done once its alarm rang, was missed or was skipped. After
        // that a calendar edit must not ring it again, and a skip must hold.
        // History rows carry the time the ring was set for, and a meeting's
        // alarm comes the lead before it, so each day's window is moved back
        // by the lead. A 23:30 ring for a 00:30 meeting counts for the
        // meeting's day.
        val leadMs = minutesBefore * 60_000L
        suspend fun done(date: LocalDate): Boolean = existing != null && eventRepository.hasEventBetween(
            existing.id,
            date.atStartOfDay(zone).toInstant().toEpochMilli() - leadMs,
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - leadMs
        )
        // A row still ahead whose own ring is already in the history was
        // switched back on by hand after a skip, so it stays the way it was
        // left. Moving it to the next meeting would undo that. Only that ring
        // counts. This morning's ring doesn't make a row armed for tonight a
        // hand re-enable. The history keeps the time a ring was set for, which
        // the row's weather lead can move ahead of the row's own time. A wider
        // window would let the day's earlier ring pass for this one.
        val rowDue = existing?.takeIf { it.isEnabled }?.let { dueAt(it, zone) }
        if (existing != null && rowDue != null && rowDue > now &&
            eventRepository.hasEventBetween(
                existing.id,
                rowDue - existing.sanitized().weatherEarlyMinutes * 60_000L,
                rowDue + 60_000L
            )
        ) return Result.success()

        // Today's alarm, set and not yet come up.
        val pendingDue = existing?.takeIf { it.isEnabled && it.specificDate == today.toString() }
            ?.let { dueAt(it, zone) }?.takeIf { it > now }

        // Today's first event keeps the alarm until it's done. Looking only at
        // tomorrow meant the first pass after midnight moved the alarm set the
        // evening before, so it never rang.
        val target = todayEvent?.takeIf { it.startMs > now && !done(today) }
            ?.let { event ->
                val commute = resolveCommuteAdjustment(settings, event, minutesBefore)
                val atMs = alarmAtMs(event, commute)
                when {
                    atMs > now -> Target(event, commute, atMs)
                    // The meeting moved earlier or the lead grew after today's
                    // alarm was set. Ringing soon beats not ringing. Two
                    // minutes so the whole minute it lands on is still ahead,
                    // and never later than the time already set, so a pass a
                    // minute on doesn't push it back again.
                    pendingDue != null -> Target(event, commute, minOf(pendingDue, now + 2 * 60_000L))
                    else -> null
                }
            }
            ?: tomorrowEvent?.takeIf { !done(today.plusDays(1)) }?.let { event ->
                val commute = resolveCommuteAdjustment(settings, event, minutesBefore)
                Target(event, commute, alarmAtMs(event, commute))
            }

        if (target == null) {
            // Nothing left to wake for: park any existing auto-alarm in
            // disabled state so it doesn't surprise-fire from a stale schedule.
            if (existing != null && existing.isEnabled) {
                repository.setEnabled(existing.id, enabled = false, nextTrigger = 0)
                scheduler.cancel(existing.id, keepFollowUps = true)
            }
            return Result.success()
        }

        val (firstEvent, commuteResolution, ringAtMs) = target
        val alarmZdt = Instant.ofEpochMilli(ringAtMs).atZone(zone)
        val alarmDate = alarmZdt.toLocalDate()

        val updated = (existing ?: Alarm()).copy(
            id = existing?.id ?: 0,
            hour = alarmZdt.hour,
            minute = alarmZdt.minute,
            label = buildString {
                append("Before: ${firstEvent.title}")
                if (commuteResolution.usedLearnedEstimate) append(" · learned commute")
            },
            isEnabled = true,
            group = GROUP,
            profileName = AUTO_PROFILE,
            specificDate = alarmDate.toString(),
            // Repeat days deliberately empty — this is a one-shot tied to a date.
            repeatDays = emptySet(),
            nextTriggerTime = 0
        )

        // Most passes find the same first event where it was. Re-arming the
        // same alarm every 15 minutes would push real history out of the
        // 100-row incident log the app closure report reads.
        val registered = existing != null && scheduler.isArmed(existing.id)
        if (alreadyArmed(existing, updated, now, registered)) return Result.success()

        // Cancel any previous schedule before re-arming with the new time/date.
        // The guardian and wake-up check follow the last ring, which may still
        // be minutes old, so they stay. The cancel still clears a snooze count
        // left over when the scheduler switched the row off without a ring
        // ending.
        if (existing != null) scheduler.cancel(existing.id, keepFollowUps = true)
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

    private data class Target(val event: CalEvent, val commute: CommuteResolution, val atMs: Long)

    private fun alarmAtMs(event: CalEvent, commute: CommuteResolution): Long =
        event.startMs - commute.adjustment.totalLeadMinutes * 60_000L

    /** The day's first timed event, midnight to midnight local time (23 or 25 hours across a clock change). */
    private fun queryFirstEventOn(date: LocalDate, zone: ZoneId): CalEvent? = queryFirstEventBetween(
        date.atStartOfDay(zone).toInstant().toEpochMilli(),
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    )

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
                    title = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() }
                        ?: applicationContext.getString(R.string.calendar_event_fallback_title),
                    location = cursor.getString(locationIndex)?.trim().orEmpty()
                )
            }
        }
        return null
    }
}
