package com.sysadmindoc.alarmclock.directboot

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.receiver.AlarmDeliveryWakeLock

class DirectBootAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_DIRECT_BOOT_ALARM) return

        val serviceIntent = Intent(context, DirectBootAlarmService::class.java).apply {
            action = DirectBootAlarmService.ACTION_START
            putExtra(EXTRA_ALARM_ID, intent.getLongExtra(EXTRA_ALARM_ID, -1L))
            putExtra(EXTRA_TRIGGER_TIME, intent.getLongExtra(EXTRA_TRIGGER_TIME, 0L))
            putExtra(EXTRA_LABEL, intent.getStringExtra(EXTRA_LABEL).orEmpty())
            putExtra(EXTRA_TIME_LABEL, intent.getStringExtra(EXTRA_TIME_LABEL).orEmpty())
            putExtra(EXTRA_PLAY_DEFAULT_SOUND, intent.getBooleanExtra(EXTRA_PLAY_DEFAULT_SOUND, true))
            putExtra(EXTRA_VIBRATION_ENABLED, intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true))
        }

        val deliveryWakeLock = AlarmDeliveryWakeLock.acquire(context)
        var serviceStartSucceeded = false
        try {
            context.startForegroundService(serviceIntent)
            serviceStartSucceeded = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start direct-boot alarm service", e)
        } finally {
            if (!serviceStartSucceeded) AlarmDeliveryWakeLock.release(deliveryWakeLock)
        }
    }

    companion object {
        const val ACTION_FIRE_DIRECT_BOOT_ALARM =
            "com.sysadmindoc.alarmclock.directboot.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "direct_boot_alarm_id"
        const val EXTRA_TRIGGER_TIME = "direct_boot_trigger_time"
        const val EXTRA_LABEL = "direct_boot_label"
        const val EXTRA_TIME_LABEL = "direct_boot_time_label"
        const val EXTRA_PLAY_DEFAULT_SOUND = "direct_boot_play_default_sound"
        const val EXTRA_VIBRATION_ENABLED = "direct_boot_vibration_enabled"

        private const val TAG = "DirectBootAlarmReceiver"

        fun pendingIntent(
            context: Context,
            snapshot: DirectBootAlarmSnapshot,
            flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ): PendingIntent? {
            val intent = Intent(context, DirectBootAlarmReceiver::class.java).apply {
                action = ACTION_FIRE_DIRECT_BOOT_ALARM
                putExtra(EXTRA_ALARM_ID, snapshot.alarmId)
                putExtra(EXTRA_TRIGGER_TIME, snapshot.triggerTime)
                putExtra(EXTRA_LABEL, snapshot.label)
                putExtra(EXTRA_TIME_LABEL, snapshot.timeLabel)
                putExtra(EXTRA_PLAY_DEFAULT_SOUND, snapshot.playDefaultSound)
                putExtra(EXTRA_VIBRATION_ENABLED, snapshot.vibrationEnabled)
            }
            return PendingIntent.getBroadcast(
                context,
                snapshot.alarmId.toInt(),
                intent,
                flags
            )
        }
    }
}
