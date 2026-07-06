package com.sysadmindoc.alarmclock.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.isPaused
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.HolidayRepository
import com.sysadmindoc.alarmclock.directboot.DirectBootAlarmCache
import com.sysadmindoc.alarmclock.receiver.AlarmReceiver
import com.sysadmindoc.alarmclock.service.BedtimeZenRuleManager
import com.sysadmindoc.alarmclock.service.SmartAlarmService
import com.sysadmindoc.alarmclock.widget.WidgetUpdater
import com.sysadmindoc.alarmclock.worker.HueSunriseWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.yield
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val calculator: NextAlarmCalculator,
    private val preferencesManager: com.sysadmindoc.alarmclock.data.preferences.PreferencesManager,
    private val holidayRepository: HolidayRepository,
    private val alarmIncidentRepository: AlarmIncidentRepository,
    private val weatherRepository: com.sysadmindoc.alarmclock.data.repository.WeatherRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_FIRE_ID = "alarm_fire_id"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"

        private val SNOW_ICE_CODES = setOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)
        fun isSnowOrIceCode(code: Int): Boolean = code in SNOW_ICE_CODES
    }

    /**
     * Schedule an alarm using setAlarmClock() for maximum reliability.
     * Checks vacation mode and holiday skip before scheduling.
     * Also starts SmartAlarmService window and enqueues HueSunriseWorker if enabled.
     */
    suspend fun schedule(alarm: Alarm, requestWidgetUpdate: Boolean = true) {
        val sanitizedAlarm = alarm.sanitized()
        if (!sanitizedAlarm.isEnabled) {
            cancel(sanitizedAlarm.id)
            return
        }

        if (!canScheduleExactAlarms()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            requestWidgetUpdateIfNeeded(requestWidgetUpdate)
            return
        }

        // v1.11.6 (roadmap N6): hard-suspend all alarms when the user has
        // tapped "Pause alarms for N days". Clears any prior PendingIntent
        // and zeroes nextTrigger so the next-alarm UI surfaces the paused
        // state. The pause expires naturally; the next reschedule pass
        // after the timestamp will re-arm the alarm.
        val currentSettings = preferencesManager.getCurrentSettings()
        if (currentSettings.isPaused()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            requestWidgetUpdateIfNeeded(requestWidgetUpdate)
            return
        }

        var triggerTime = calculator.calculate(sanitizedAlarm)
        if (triggerTime <= System.currentTimeMillis()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            if (sanitizedAlarm.isRecurringSchedule) {
                repository.updateNextTrigger(sanitizedAlarm.id, 0)
            } else {
                repository.setEnabled(sanitizedAlarm.id, enabled = false, nextTrigger = 0)
            }
            requestWidgetUpdateIfNeeded(requestWidgetUpdate)
            return
        }
        val settings = currentSettings  // already fetched for the pause-state check above

        // Vacation mode skips repeating-alarm occurrences inside the configured
        // window, then schedules the first occurrence after the window ends.
        val vacationAdjustment = VacationAlarmPolicy.adjustTrigger(
            alarm = sanitizedAlarm,
            initialTriggerTime = triggerTime,
            settings = settings,
            calculateFrom = calculator::calculate
        )
        triggerTime = vacationAdjustment.triggerTime

        // F13: Holiday auto-skip
        if (sanitizedAlarm.skipOnHolidays && settings.holidayAutoSkipEnabled) {
            if (!sanitizedAlarm.isRecurringSchedule) {
                // One-shot alarm: if the day is a holiday, don't fire at all
                val triggerDate = Instant.ofEpochMilli(triggerTime)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                if (holidayRepository.isHoliday(triggerDate)) {
                    cancelScheduledEntries(sanitizedAlarm.id)
                    repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
                    requestWidgetUpdateIfNeeded(requestWidgetUpdate)
                    return
                }
            } else {
                // Repeating alarm: advance past consecutive holidays to the next valid day.
                // v1.5.1: bumped from 14 to 30 attempts so regional 2-week
                // national holiday clusters don't fall through to firing on a holiday.
                var attempts = 0
                while (attempts < 30) {
                    val triggerDate = Instant.ofEpochMilli(triggerTime)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    if (!holidayRepository.isHoliday(triggerDate)) break
                    val nextFrom = triggerDate.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                    triggerTime = calculator.calculate(sanitizedAlarm, nextFrom)
                    attempts++
                }
                // If all 30 candidates were holidays (corrupt data or an unusually
                // long public-holiday run), suppress this occurrence rather than
                // schedule on a holiday. The alarm stays enabled; it will reschedule
                // correctly once holiday data is refreshed or the user re-saves.
                if (attempts >= 30) {
                    val finalDate = Instant.ofEpochMilli(triggerTime)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    if (holidayRepository.isHoliday(finalDate)) {
                        cancelScheduledEntries(sanitizedAlarm.id)
                        repository.updateNextTrigger(sanitizedAlarm.id, 0)
                        requestWidgetUpdateIfNeeded(requestWidgetUpdate)
                        return
                    }
                }
            }
        }

        if (sanitizedAlarm.weatherEarlyMinutes > 0 && triggerTime > 0) {
            var weather = weatherRepository.getCachedWeather()
            if (weather == null) {
                val settings = preferencesManager.getCurrentSettings()
                val lat = settings.lastKnownLatitude
                val lng = settings.lastKnownLongitude
                if (lat != 0.0 || lng != 0.0) {
                    weather = weatherRepository.getWeather(lat, lng, settings.temperatureUnit)
                        .getOrNull()
                        ?.response
                }
            }
            if (weather != null) {
                val triggerDate = java.time.Instant.ofEpochMilli(triggerTime)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val dayIndex = weather.daily?.time?.indexOfFirst { it == triggerDate.toString() } ?: -1
                if (dayIndex >= 0) {
                    val code = weather.daily?.weatherCode?.getOrNull(dayIndex)
                    if (code != null && isSnowOrIceCode(code)) {
                        triggerTime -= sanitizedAlarm.weatherEarlyMinutes * 60_000L
                    }
                }
            }
        }

        repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
        scheduleAlarmClock(sanitizedAlarm.id, triggerTime)
        DirectBootAlarmCache.saveIfEarlier(context, sanitizedAlarm, triggerTime)
        scheduleSupportingWork(sanitizedAlarm, triggerTime)
        requestWidgetUpdateIfNeeded(requestWidgetUpdate)
        syncBedtimeDndRule()
    }

    /**
     * Cancel a scheduled alarm and any associated workers/services.
     */
    fun cancel(alarmId: Long) {
        cancelScheduledEntries(alarmId, includeFollowUpWorkers = true)
        WidgetUpdater.requestUpdate(context)
    }

    /**
     * Schedule a snoozed alarm to fire after the snooze duration.
     * @param customMinutes Override snooze duration (null = use alarm's default)
     */
    suspend fun scheduleSnooze(alarm: Alarm, customMinutes: Int? = null) {
        if (!canScheduleExactAlarms()) return

        val minutes = customMinutes ?: alarm.snoozeDurationMinutes
        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        scheduleAt(alarm, snoozeTime)
    }

    /**
     * Register a precomputed trigger time without recalculating it.
     * Useful for snooze, skip-next, quick alarms, and restored exact alarms.
     */
    suspend fun scheduleAt(
        alarm: Alarm,
        triggerTime: Long,
        requestWidgetUpdate: Boolean = true
    ) {
        val sanitizedAlarm = alarm.sanitized()
        if (!canScheduleExactAlarms()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            requestWidgetUpdateIfNeeded(requestWidgetUpdate)
            return
        }
        // v1.11.6 (roadmap N6): respect "Pause alarms" for snooze + quick-add
        // paths too — snoozing inside a pause window shouldn't sneak past it.
        if (preferencesManager.getCurrentSettings().isPaused()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            requestWidgetUpdateIfNeeded(requestWidgetUpdate)
            return
        }

        repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
        scheduleAlarmClock(sanitizedAlarm.id, triggerTime)
        DirectBootAlarmCache.saveIfEarlier(context, sanitizedAlarm, triggerTime)
        scheduleSupportingWork(sanitizedAlarm, triggerTime)
        requestWidgetUpdateIfNeeded(requestWidgetUpdate)
        syncBedtimeDndRule()
    }

    /**
     * Reschedule all enabled alarms. Called after boot and app update.
     * Preserves existing future nextTriggerTime (e.g., from skip-next or snooze)
     * to avoid undoing user actions.
     */
    suspend fun rescheduleAll(forceRecalculate: Boolean = false) {
        rescheduleAllInBatches(forceRecalculate = forceRecalculate)
    }

    /**
     * Reschedule enabled alarms in batches so WorkManager recovery paths can
     * handle large alarm libraries without receiver ANRs or redundant widget
     * refreshes after every single AlarmManager registration.
     *
     * @return number of enabled alarms processed.
     */
    suspend fun rescheduleAllInBatches(
        forceRecalculate: Boolean = false,
        batchSize: Int = 25
    ): Int {
        val now = System.currentTimeMillis()
        val enabledAlarms = repository.getEnabled()
        val safeBatchSize = batchSize.coerceAtLeast(1)
        DirectBootAlarmCache.clear(context)
        enabledAlarms.chunked(safeBatchSize).forEach { batch ->
            batch.forEach { alarm ->
                if (DirectBootAlarmCache.consumeFiredOneShotMarker(context, alarm, now)) {
                    repository.setEnabled(alarm.id, enabled = false, nextTrigger = 0)
                    cancelScheduledEntries(alarm.id)
                    return@forEach
                }
                if (!forceRecalculate && alarm.nextTriggerTime > now) {
                    // Existing future trigger is still valid - just re-register with AlarmManager
                    scheduleExistingTrigger(
                        alarm = alarm,
                        triggerTime = alarm.nextTriggerTime,
                        requestWidgetUpdate = false
                    )
                } else {
                    // Needs recalculation (past or unset trigger time)
                    schedule(alarm, requestWidgetUpdate = false)
                }
            }
            yield()
        }
        WidgetUpdater.requestUpdate(context)
        syncBedtimeDndRule()
        return enabledAlarms.size
    }

    /**
     * Internal helper to schedule with AlarmManager at a specific time without recalculating.
     */
    private suspend fun scheduleExistingTrigger(
        alarm: Alarm,
        triggerTime: Long,
        requestWidgetUpdate: Boolean = true
    ) {
        val sanitizedAlarm = alarm.sanitized()
        if (!canScheduleExactAlarms()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            requestWidgetUpdateIfNeeded(requestWidgetUpdate)
            return
        }

        val settings = preferencesManager.getCurrentSettings()
        val vacationAdjustment = VacationAlarmPolicy.adjustTrigger(
            alarm = sanitizedAlarm,
            initialTriggerTime = triggerTime,
            settings = settings,
            calculateFrom = calculator::calculate
        )

        scheduleAt(
            alarm = sanitizedAlarm,
            triggerTime = vacationAdjustment.triggerTime,
            requestWidgetUpdate = requestWidgetUpdate
        )
    }

    /**
     * After an alarm fires: if repeating, schedule next occurrence.
     * If one-shot, disable it.
     */
    suspend fun handleAlarmFired(alarmId: Long) {
        val alarm = repository.getById(alarmId) ?: return

        if (!alarm.isRecurringSchedule) {
            // One-shot alarm: disable after firing. Also tear down any still-armed
            // exact-alarm entry. A normally-fired one-shot's PendingIntent was
            // already consumed by AlarmManager (this is a no-op), but a smart-wake
            // *early* fire bypasses that PendingIntent, so without this the original
            // exact alarm would fire a second time at the unmodified trigger minute.
            cancelMainScheduledAlarm(alarmId)
            repository.setEnabled(alarmId, enabled = false, nextTrigger = 0)
            syncBedtimeDndRule()
        } else {
            // Repeating alarm: schedule next occurrence (FLAG_UPDATE_CURRENT
            // replaces any stale early-fire PendingIntent with the same requestCode).
            schedule(alarm)
        }
    }

    /**
     * Cancels the main AlarmManager exact-alarm entry for [alarmId] without
     * touching the alarm row. Used by [SmartAlarmService] when it fires an alarm
     * early: the original exact alarm scheduled at the unmodified trigger time is
     * still armed and would otherwise fire a spurious second time. Safe no-op when
     * nothing is armed.
     */
    fun cancelMainScheduledAlarm(alarmId: Long) {
        val pendingIntent = createPendingIntent(alarmId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Keeps the app-owned bedtime DND condition aligned with whichever enabled
     * alarm currently wakes the user next. No-ops unless the user enabled the
     * Bedtime DND preference and granted notification policy access.
     */
    suspend fun syncBedtimeDndRule() {
        val settings = preferencesManager.getCurrentSettings()
        if (!settings.bedtimeDndEnabled &&
            !BedtimeZenRuleManager.isPolicyAccessGranted(context)) {
            return
        }
        val nextAlarmTrigger = repository.getNextAlarm()?.nextTriggerTime
        BedtimeZenRuleManager.syncRule(context, settings, nextAlarmTrigger)
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun requestWidgetUpdateIfNeeded(requestWidgetUpdate: Boolean) {
        if (requestWidgetUpdate) {
            WidgetUpdater.requestUpdate(context)
        }
    }

    private fun scheduleAlarmClock(alarmId: Long, triggerTime: Long) {
        DirectBootAlarmCache.cancelScheduledFallback(context, alarmId)
        val fireId = AlarmIncidentEvent.fireIdFor(alarmId, triggerTime)
        recordScheduleIncident(
            alarmId = alarmId,
            fireId = fireId,
            triggerTime = triggerTime,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "SET_ALARM_CLOCK"
        )
        val pendingIntent = createPendingIntent(alarmId, triggerTime, fireId)
        val showIntent = createShowIntent(alarmId)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        // v1.6.3: `canScheduleExactAlarms()` is checked upstream, but the
        // permission can be revoked between the check and this call (rare but
        // possible — Settings → "Alarms & reminders" toggle is async). Some
        // OEMs also throw SecurityException from `setAlarmClock()` even when
        // the permission appears granted (notably Samsung One UI 6 in
        // background-restricted state). Fall back to inexact-allow-while-idle
        // so the alarm still fires within the 1-2 minute Doze window instead
        // of vanishing silently.
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            recordScheduleIncident(
                alarmId = alarmId,
                fireId = fireId,
                triggerTime = triggerTime,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "SET_ALARM_CLOCK"
            )
        } catch (e: SecurityException) {
            android.util.Log.w(
                "AlarmScheduler",
                "setAlarmClock denied for alarm $alarmId — falling back to inexact",
                e
            )
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                recordScheduleIncident(
                    alarmId = alarmId,
                    fireId = fireId,
                    triggerTime = triggerTime,
                    status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                    reasonCode = "SET_AND_ALLOW_WHILE_IDLE_AFTER_SECURITY_EXCEPTION"
                )
            } catch (e2: Exception) {
                android.util.Log.e(
                    "AlarmScheduler",
                    "Inexact fallback also failed for alarm $alarmId",
                    e2
                )
                recordScheduleIncident(
                    alarmId = alarmId,
                    fireId = fireId,
                    triggerTime = triggerTime,
                    status = AlarmIncidentEvent.STATUS_FAILED,
                    reasonCode = "SET_AND_ALLOW_WHILE_IDLE_FAILED_${e2.javaClass.simpleName}"
                )
            }
        } catch (e: Exception) {
            // Defensive: AlarmManager has been seen to throw RuntimeException on
            // device-admin policy clamps. Log so users with crash-log access can
            // diagnose; the WidgetUpdater will still show "no scheduled alarm".
            android.util.Log.e(
                "AlarmScheduler",
                "Unexpected error scheduling alarm $alarmId",
                e
            )
            recordScheduleIncident(
                alarmId = alarmId,
                fireId = fireId,
                triggerTime = triggerTime,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "SET_ALARM_CLOCK_FAILED_${e.javaClass.simpleName}"
            )
        }
    }

    private fun recordScheduleIncident(
        alarmId: Long,
        fireId: String,
        triggerTime: Long,
        status: String,
        reasonCode: String
    ) {
        alarmIncidentRepository.recordAsync(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = triggerTime,
            type = AlarmIncidentEvent.TYPE_SCHEDULE,
            status = status,
            reasonCode = reasonCode,
            source = "AlarmScheduler"
        )
    }

    private fun scheduleSupportingWork(alarm: Alarm, triggerTime: Long) {
        scheduleSmartAlarmStart(alarm, triggerTime)
        scheduleHueSunrise(alarm, triggerTime)
    }

    private fun scheduleSmartAlarmStart(alarm: Alarm, triggerTime: Long) {
        if (!alarm.smartAlarmEnabled || alarm.smartAlarmWindowMinutes <= 0) {
            cancelSmartAlarmStart(alarm.id)
            return
        }

        val windowMs = alarm.smartAlarmWindowMinutes * 60_000L
        val serviceStartTime = triggerTime - windowMs
        val delayMs = (serviceStartTime - System.currentTimeMillis()).coerceAtLeast(0)
        val smartIntent = Intent(context, SmartAlarmService::class.java).apply {
            action = SmartAlarmService.ACTION_START_SMART
            putExtra(SmartAlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(SmartAlarmService.EXTRA_TARGET_TIME, triggerTime)
        }
        val smartPending = PendingIntent.getForegroundService(
            context,
            (alarm.id + 50000).toInt(),
            smartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (delayMs == 0L) {
            // v1.5.1: Android 14+ can refuse startForegroundService() if the
            // scheduler is invoked from a background-restricted path. Fall
            // back to AlarmManager — the service will start when the pending
            // intent fires (one tick later, negligible for a motion-tracking
            // window that's normally 30 min long).
            try {
                context.startForegroundService(smartIntent)
            } catch (_: Exception) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1_000L,
                    smartPending
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                serviceStartTime,
                smartPending
            )
        }
    }

    private fun scheduleHueSunrise(alarm: Alarm, triggerTime: Long) {
        val workManager = WorkManager.getInstance(context)
        if (!alarm.hueEnabled || alarm.huePreWakeMinutes <= 0) {
            workManager.cancelUniqueWork("hue_sunrise_${alarm.id}")
            return
        }

        val hueStartMs = triggerTime - (alarm.huePreWakeMinutes * 60_000L)
        val hueDelayMs = (hueStartMs - System.currentTimeMillis()).coerceAtLeast(0)
        val inputData = Data.Builder()
            .putLong(HueSunriseWorker.KEY_ALARM_ID, alarm.id)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<HueSunriseWorker>()
            .setInitialDelay(hueDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()
        workManager.enqueueUniqueWork(
            "hue_sunrise_${alarm.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelScheduledEntries(alarmId: Long, includeFollowUpWorkers: Boolean = false) {
        val pendingIntent = createPendingIntent(alarmId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        DirectBootAlarmCache.removeIfMatches(context, alarmId)

        cancelSmartAlarmStart(alarmId)

        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("hue_sunrise_$alarmId")
        if (includeFollowUpWorkers) {
            workManager.cancelUniqueWork("guardian_$alarmId")
            workManager.cancelUniqueWork("wake_confirm_$alarmId")
        }
    }

    private fun cancelSmartAlarmStart(alarmId: Long) {
        val smartIntent = Intent(context, SmartAlarmService::class.java).apply {
            action = SmartAlarmService.ACTION_START_SMART
        }
        val smartPending = PendingIntent.getForegroundService(
            context,
            (alarmId + 50000).toInt(),
            smartIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (smartPending != null) {
            alarmManager.cancel(smartPending)
            smartPending.cancel()
        }
    }

    private fun createPendingIntent(
        alarmId: Long,
        triggerTime: Long = 0L,
        fireId: String = ""
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.sysadmindoc.alarmclock.ALARM_FIRE"
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_SCHEDULED_AT, triggerTime)
            putExtra(EXTRA_ALARM_FIRE_ID, fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, triggerTime) })
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * The "tap the alarm icon in the status bar" intent used by AlarmClockInfo.
     * Falls back to an explicit MainActivity launch if the package launch intent
     * is unavailable (e.g., on devices that strip MAIN/LAUNCHER from system rebuilds).
     */
    private fun createShowIntent(alarmId: Long): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setClassName(
                context.packageName,
                "com.sysadmindoc.alarmclock.MainActivity"
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
