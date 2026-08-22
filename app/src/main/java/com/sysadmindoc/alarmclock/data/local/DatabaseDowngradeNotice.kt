package com.sysadmindoc.alarmclock.data.local

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R

/**
 * Tells the user when a database stamped by a newer build was dropped.
 *
 * The app allows platform restores from any version, so a database from a
 * newer build can land on an older one. Room cannot migrate downwards, and
 * before the destructive fallback it threw on open and crashed every screen
 * that read it. Dropping the tables keeps the app usable, but silently losing
 * someone's alarms is exactly the kind of quiet failure this project does not
 * ship, so say what happened and why no alarm will ring.
 */
object DatabaseDowngradeNotice {
    private const val CHANNEL_ID = "database_downgrade_channel"
    private const val NOTIFICATION_ID = 1_012

    fun post(context: Context) {
        runCatching {
            // On API 33+ posting without the runtime grant is a no-op at best,
            // so check rather than rely on the surrounding runCatching.
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_database_downgrade),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            val openApp = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(context.getString(R.string.notif_database_downgrade_title))
                .setContentText(context.getString(R.string.notif_database_downgrade_text))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notif_database_downgrade_text))
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
