package com.sysadmindoc.alarmclock.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.R

/**
 * Fires when bedtime reminder triggers.
 * Shows a gentle notification reminding the user to go to sleep.
 */
class BedtimeReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_BEDTIME = "bedtime_channel"
        const val NOTIFICATION_ID = 3001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.sysadmindoc.alarmclock.BEDTIME_REMINDER") return

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Create channel
        val channel = NotificationChannel(
            CHANNEL_BEDTIME,
            "Bedtime Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you when it's time to sleep"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_BEDTIME)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Time to wind down")
            .setContentText("Your bedtime is approaching. Start getting ready for sleep.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Reschedule for tomorrow (same time + 24h)
        rescheduleForTomorrow(context)
    }

    private fun rescheduleForTomorrow(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val rescheduleIntent = Intent("com.sysadmindoc.alarmclock.BEDTIME_REMINDER")
        rescheduleIntent.setPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, 9999, rescheduleIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule 24 hours from now
        val nextTrigger = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            nextTrigger,
            pendingIntent
        )
    }
}
