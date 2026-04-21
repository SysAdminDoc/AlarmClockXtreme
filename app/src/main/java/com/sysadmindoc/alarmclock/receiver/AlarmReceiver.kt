package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService

/**
 * Receives the alarm broadcast from AlarmManager and starts the foreground
 * AlarmService, which handles audio playback, vibration, and launching the
 * full-screen dismiss/snooze UI.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (API 31+) when the app is
            // background-restricted at the exact moment AlarmManager wakes it.
            // The AlarmManager exact-alarm guarantee means this is extremely rare;
            // log for diagnostics and let the system handle retries.
            Log.e("AlarmReceiver", "startForegroundService failed for alarm $alarmId", e)
        }
    }
}
