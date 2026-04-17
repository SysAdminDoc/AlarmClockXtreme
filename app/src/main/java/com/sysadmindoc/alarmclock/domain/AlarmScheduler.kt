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
import java.time.LocalDate
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
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        if (!canScheduleExactAlarms()) return

        var triggerTime = calculator.calculate(alarm)
        val settings = preferencesManager.getCurrentSettings()

        // Check vacation mode - skip scheduling if trigger falls within vacation window
        if (settings.vacationModeEnabled &&
            settings.vacationStartMillis > 0 &&
            settings.vacationEndMillis > 0 &&
            triggerTime in settings.vacationStartMillis..settings.vacationEndMillis
        ) {
            repository.updateNextTrigger(alarm.id, triggerTime)
            return // Don't schedule with AlarmManager, but keep nextTrigger for display
        }

        // F13: Holiday auto-skip
        if (alarm.skipOnHolidays && settings.holidayAutoSkipEnabled) {
            if (alarm.repeatDays.isEmpty()) {
                // One-shot alarm: if the day is a holiday, don't fire at all
                val triggerDate = Instant.ofEpochMilli(triggerTime)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                if (holidayRepository.isHoliday(triggerDate)) {
                    repository.updateNextTrigger(alarm.id, triggerTime)
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
                    triggerTime = calculator.calculate(alarm, nextFrom)
                    attempts++
                }
            }
        }

        repository.updateNextTrigger(alarm.id, triggerTime)

        val pendingIntent = createPendingIntent(alarm.id)
        val showIntent = createShowIntent(alarm.id)

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        WidgetUpdater.requestUpdate(context)

        // F6: Smart alarm — start the motion-monitoring service early
        if (alarm.smartAlarmEnabled && alarm.smartAlarmWindowMinutes > 0) {
            val windowMs = alarm.smartAlarmWindowMinutes * 60_000L
            val serviceStartTime = triggerTime - windowMs
            val delayMs = (serviceStartTime - System.currentTimeMillis()).coerceAtLeast(0)
            val smartIntent = Intent(context, SmartAlarmService::class.java).apply {
                action = SmartAlarmService.ACTION_START_SMART
                putExtra(SmartAlarmService.EXTRA_ALARM_ID, alarm.id)
                putExtra(SmartAlarmService.EXTRA_TARGET_TIME, triggerTime)
            }
            // Schedule via a one-shot pending intent instead of WorkManager to preserve
            // service startup accuracy (WorkManager has ~15min minimum flex)
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

        // F15: Philips Hue sunrise — enqueue worker to run before alarm fires
        if (alarm.hueEnabled && alarm.huePreWakeMinutes > 0) {
            val hueStartMs = triggerTime - (alarm.huePreWakeMinutes * 60_000L)
            val hueDelayMs = (hueStartMs - System.currentTimeMillis()).coerceAtLeast(0)
            val inputData = Data.Builder()
                .putLong(HueSunriseWorker.KEY_ALARM_ID, alarm.id)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<HueSunriseWorker>()
                .setInitialDelay(hueDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "hue_sunrise_${alarm.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    /**
     * Cancel a scheduled alarm and any associated workers/services.
     */
    fun cancel(alarmId: Long) {
        val pendingIntent = createPendingIntent(alarmId)
        alarmManager.cancel(pendingIntent)

        // Cancel SmartAlarm pending intent (uses alarmId + 50000 offset)
        val smartIntent = Intent(context, SmartAlarmService::class.java).apply {
            action = SmartAlarmService.ACTION_START_SMART
        }
        val smartPending = PendingIntent.getForegroundService(
            context,
            (alarmId + 50000).toInt(),
            smartIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (smartPending != null) alarmManager.cancel(smartPending)

        // Cancel Hue sunrise worker if enqueued
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork("hue_sunrise_$alarmId")
        // Cancel guardian + wake-confirm workers if either is queued for this alarm
        wm.cancelUniqueWork("guardian_$alarmId")
        wm.cancelUniqueWork("wake_confirm_$alarmId")
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
        repository.updateNextTrigger(alarm.id, snoozeTime)

        val pendingIntent = createPendingIntent(alarm.id)
        val showIntent = createShowIntent(alarm.id)

        val alarmClockInfo = AlarmManager.AlarmClockInfo(snoozeTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    /**
     * Reschedule all enabled alarms. Called after boot and app update.
     * Preserves existing future nextTriggerTime (e.g., from skip-next or snooze)
     * to avoid undoing user actions.
     */
    suspend fun rescheduleAll() {
        repository.getEnabled().forEach { alarm ->
            if (alarm.nextTriggerTime > System.currentTimeMillis()) {
                // Existing future trigger is still valid - just re-register with AlarmManager
                scheduleExact(alarm, alarm.nextTriggerTime)
            } else {
                // Needs recalculation (past or unset trigger time)
                schedule(alarm)
            }
        }
    }

    /**
     * Internal helper to schedule with AlarmManager at a specific time without recalculating.
     */
    private suspend fun scheduleExact(alarm: Alarm, triggerTime: Long) {
        if (!canScheduleExactAlarms()) return

        val settings = preferencesManager.getCurrentSettings()
        if (settings.vacationModeEnabled &&
            settings.vacationStartMillis > 0 &&
            settings.vacationEndMillis > 0 &&
            triggerTime in settings.vacationStartMillis..settings.vacationEndMillis
        ) {
            return
        }

        val pendingIntent = createPendingIntent(alarm.id)
        val showIntent = createShowIntent(alarm.id)

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        WidgetUpdater.requestUpdate(context)
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
