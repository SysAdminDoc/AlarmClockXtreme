package com.sysadmindoc.alarmclock.platform

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
 * Says so when another app created an alarm without showing anything.
 *
 * android.provider.AlarmClock lets any app holding SET_ALARM, a normal
 * permission, add an enabled alarm and skip the UI. That is the platform
 * contract and the app honours it, but the person whose phone will ring gets
 * to know it happened.
 */
object ExternalAlarmNotice {
    private const val CHANNEL_ID = "external_alarm_added"
    private const val NOTIFICATION_ID = 90_210

    fun post(context: Context, callerPackage: String?) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_external_alarm),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            val text = callerPackage
                ?.let { context.getString(R.string.notif_external_alarm_text_named, appLabel(context, it)) }
                ?: context.getString(R.string.notif_external_alarm_text)
            val openAlarms = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle(context.getString(R.string.notif_external_alarm_title))
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setContentIntent(openAlarms)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    /** The caller's own name if the package manager knows it, the id otherwise. */
    private fun appLabel(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}
