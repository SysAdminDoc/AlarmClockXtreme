package com.sysadmindoc.alarmclock.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.receiver.AlarmReceiver
import com.sysadmindoc.alarmclock.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val calculator: NextAlarmCalculator,
    private val preferencesManager: com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    /**
     * Schedule an alarm using setAlarmClock() for maximum reliability.
     * Checks vacation mode before scheduling - if the next trigger time
     * falls within a vacation window, the alarm is skipped but stays enabled.
     */
    suspend fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        if (!canScheduleExactAlarms()) return

        val triggerTime = calculator.calculate(alarm)

        // Check vacation mode - skip scheduling if trigger falls within vacation window
        val settings = preferencesManager.getCurrentSettings()
        if (settings.vacationModeEnabled &&
            settings.vacationStartMillis > 0 &&
            settings.vacationEndMillis > 0 &&
            triggerTime in settings.vacationStartMillis..settings.vacationEndMillis
        ) {
            repository.updateNextTrigger(alarm.id, triggerTime)
            return // Don't schedule with AlarmManager, but keep nextTrigger for display
        }

        repository.updateNextTrigger(alarm.id, triggerTime)

        val pendingIntent = createPendingIntent(alarm.id)

        // Show info intent - opens app when user taps the alarm icon in status bar
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        WidgetUpdater.requestUpdate(context)
    }

    /**
     * Cancel a scheduled alarm.
     */
    fun cancel(alarmId: Long) {
        val pendingIntent = createPendingIntent(alarmId)
        alarmManager.cancel(pendingIntent)
        WidgetUpdater.requestUpdate(context)
    }

    /**
     * Schedule a snoozed alarm to fire after the snooze duration.
     */
    suspend fun scheduleSnooze(alarm: Alarm) {
        if (!canScheduleExactAlarms()) return

        val snoozeTime = System.currentTimeMillis() + (alarm.snoozeDurationMinutes * 60 * 1000L)
        repository.updateNextTrigger(alarm.id, snoozeTime)

        val pendingIntent = createPendingIntent(alarm.id)
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
}
