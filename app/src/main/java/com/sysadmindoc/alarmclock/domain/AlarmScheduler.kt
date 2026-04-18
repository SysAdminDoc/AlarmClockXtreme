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
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.HolidayRepository
import com.sysadmindoc.alarmclock.receiver.AlarmReceiver
import com.sysadmindoc.alarmclock.service.SmartAlarmService
import com.sysadmindoc.alarmclock.widget.WidgetUpdater
import com.sysadmindoc.alarmclock.worker.HueSunriseWorker
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val holidayRepository: HolidayRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    /**
     * Schedule an alarm using setAlarmClock() for maximum reliability.
     * Checks vacation mode and holiday skip before scheduling.
     * Also starts SmartAlarmService window and enqueues HueSunriseWorker if enabled.
     */
    suspend fun schedule(alarm: Alarm) {
        val sanitizedAlarm = alarm.sanitized()
        if (!sanitizedAlarm.isEnabled) {
            cancel(sanitizedAlarm.id)
            return
        }

        if (!canScheduleExactAlarms()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            WidgetUpdater.requestUpdate(context)
            return
        }

        var triggerTime = calculator.calculate(sanitizedAlarm)
        val settings = preferencesManager.getCurrentSettings()

        // Check vacation mode - skip scheduling if trigger falls within vacation window
        if (isSuppressedByVacation(triggerTime, settings)) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
            WidgetUpdater.requestUpdate(context)
            return // Don't schedule with AlarmManager, but keep nextTrigger for display
        }

        // F13: Holiday auto-skip
        if (sanitizedAlarm.skipOnHolidays && settings.holidayAutoSkipEnabled) {
            if (sanitizedAlarm.repeatDays.isEmpty()) {
                // One-shot alarm: if the day is a holiday, don't fire at all
                val triggerDate = Instant.ofEpochMilli(triggerTime)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                if (holidayRepository.isHoliday(triggerDate)) {
                    cancelScheduledEntries(sanitizedAlarm.id)
                    repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
                    WidgetUpdater.requestUpdate(context)
                    return
                }
            } else {
                // Repeating alarm: advance past consecutive holidays to the next valid day
                var attempts = 0
                while (attempts < 14) {
                    val triggerDate = Instant.ofEpochMilli(triggerTime)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    if (!holidayRepository.isHoliday(triggerDate)) break
                    val nextFrom = triggerDate.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                    triggerTime = calculator.calculate(sanitizedAlarm, nextFrom)
                    attempts++
                }
            }
        }

        repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
        scheduleAlarmClock(sanitizedAlarm.id, triggerTime)
        scheduleSupportingWork(sanitizedAlarm, triggerTime)
        WidgetUpdater.requestUpdate(context)
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
    suspend fun scheduleAt(alarm: Alarm, triggerTime: Long) {
        val sanitizedAlarm = alarm.sanitized()
        if (!canScheduleExactAlarms()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            WidgetUpdater.requestUpdate(context)
            return
        }

        repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
        scheduleAlarmClock(sanitizedAlarm.id, triggerTime)
        scheduleSupportingWork(sanitizedAlarm, triggerTime)
        WidgetUpdater.requestUpdate(context)
    }

    /**
     * Reschedule all enabled alarms. Called after boot and app update.
     * Preserves existing future nextTriggerTime (e.g., from skip-next or snooze)
     * to avoid undoing user actions.
     */
    suspend fun rescheduleAll(forceRecalculate: Boolean = false) {
        val now = System.currentTimeMillis()
        repository.getEnabled().forEach { alarm ->
            if (!forceRecalculate && alarm.nextTriggerTime > now) {
                // Existing future trigger is still valid - just re-register with AlarmManager
                scheduleExistingTrigger(alarm, alarm.nextTriggerTime)
            } else {
                // Needs recalculation (past or unset trigger time)
                schedule(alarm)
            }
        }
    }

    /**
     * Internal helper to schedule with AlarmManager at a specific time without recalculating.
     */
    private suspend fun scheduleExistingTrigger(alarm: Alarm, triggerTime: Long) {
        val sanitizedAlarm = alarm.sanitized()
        if (!canScheduleExactAlarms()) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, 0)
            WidgetUpdater.requestUpdate(context)
            return
        }

        val settings = preferencesManager.getCurrentSettings()
        if (isSuppressedByVacation(triggerTime, settings)) {
            cancelScheduledEntries(sanitizedAlarm.id)
            repository.updateNextTrigger(sanitizedAlarm.id, triggerTime)
            WidgetUpdater.requestUpdate(context)
            return
        }

        scheduleAt(sanitizedAlarm, triggerTime)
    }

    /**
     * After an alarm fires: if repeating, schedule next occurrence.
     * If one-shot, disable it.
     */
    suspend fun handleAlarmFired(alarmId: Long) {
        val alarm = repository.getById(alarmId) ?: return

        if (alarm.repeatDays.isEmpty()) {
            // One-shot alarm: disable after firing
            repository.setEnabled(alarmId, enabled = false, nextTrigger = 0)
        } else {
            // Repeating alarm: schedule next occurrence
            schedule(alarm)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun isSuppressedByVacation(
        triggerTime: Long,
        settings: com.sysadmindoc.alarmclock.data.preferences.AppSettings
    ): Boolean {
        return settings.vacationModeEnabled &&
            settings.vacationStartMillis > 0 &&
            settings.vacationEndMillis > 0 &&
            triggerTime in settings.vacationStartMillis..settings.vacationEndMillis
    }

    private fun scheduleAlarmClock(alarmId: Long, triggerTime: Long) {
        val pendingIntent = createPendingIntent(alarmId)
        val showIntent = createShowIntent(alarmId)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
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
            context.startForegroundService(smartIntent)
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

    private fun createPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.sysadmindoc.alarmclock.ALARM_FIRE"
            putExtra(EXTRA_ALARM_ID, alarmId)
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
