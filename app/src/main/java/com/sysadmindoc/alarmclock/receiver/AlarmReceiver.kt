package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract

/**
 * Receives the alarm broadcast from AlarmManager and starts the foreground
 * AlarmService, which handles audio playback, vibration, and launching the
 * full-screen dismiss/snooze UI.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L)
        val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
            ?: AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)
        val incidents = mutableListOf(
            ReceiverAlarmIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_BROADCAST,
                status = AlarmIncidentEvent.STATUS_RECEIVED,
                reasonCode = "ALARM_MANAGER_DELIVERED",
                source = "AlarmReceiver"
            )
        )

        val serviceIntent = AlarmFireDismissContract.startServiceIntent(context, alarmId, scheduledAt, fireId)
        try {
            context.startForegroundService(serviceIntent)
            incidents += ReceiverAlarmIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = "START_FOREGROUND_SERVICE",
                source = "AlarmReceiver"
            )
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (API 31+) when the app is
            // background-restricted at the exact moment AlarmManager wakes it.
            // The AlarmManager exact-alarm guarantee means this is extremely rare;
            // log for diagnostics and let the system handle retries.
            Log.e("AlarmReceiver", "startForegroundService failed for alarm $alarmId", e)
            incidents += ReceiverAlarmIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "START_FOREGROUND_SERVICE_FAILED_${e.javaClass.simpleName}",
                source = "AlarmReceiver"
            )
        } finally {
            recordAlarmIncidentsAsync(context, incidents)
        }
    }
}
