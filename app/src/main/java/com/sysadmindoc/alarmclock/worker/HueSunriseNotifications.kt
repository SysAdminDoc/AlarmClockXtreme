package com.sysadmindoc.alarmclock.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.LiveUpdateCountdown
import com.sysadmindoc.alarmclock.service.PromotedOngoingNotification

internal object HueSunriseNotifications {
    private const val TAG = "HueSunriseNotify"
    private const val CHANNEL_ID = "hue_sunrise_channel"
    private const val NOTIFICATION_BASE_ID = 800_000
    private const val NOTIFICATION_ID_RANGE = 100_000L
    private const val ANDROID_16_API = 36
    private const val TITLE = "Sunrise in progress"
    private const val TEXT = "Hue lights are brightening before your alarm"

    fun post(
        context: Context,
        alarmId: Long,
        startWallClockMillis: Long,
        endWallClockMillis: Long,
        nowWallClockMillis: Long = System.currentTimeMillis()
    ) {
        createChannel(context)
        val notification = if (Build.VERSION.SDK_INT >= ANDROID_16_API) {
            buildLiveUpdate(
                context = context,
                alarmId = alarmId,
                startWallClockMillis = startWallClockMillis,
                endWallClockMillis = endWallClockMillis,
                nowWallClockMillis = nowWallClockMillis
            )
        } else {
            buildCompat(context, alarmId, endWallClockMillis)
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(alarmId), notification)
        } catch (_: SecurityException) {
            // The light ramp is still useful when notification access is denied.
        } catch (error: Exception) {
            Log.w(TAG, "Failed to post Hue sunrise notification", error)
        }
    }

    internal fun buildCompat(
        context: Context,
        alarmId: Long,
        endWallClockMillis: Long
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_alarm)
        .setContentTitle(TITLE)
        .setContentText(TEXT)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(true)
        .setWhen(endWallClockMillis)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setContentIntent(openAppIntent(context, alarmId))
        .build()

    @RequiresApi(ANDROID_16_API)
    private fun buildLiveUpdate(
        context: Context,
        alarmId: Long,
        startWallClockMillis: Long,
        endWallClockMillis: Long,
        nowWallClockMillis: Long
    ): Notification {
        val progress = LiveUpdateCountdown.progress(
            startMillis = startWallClockMillis,
            endMillis = endWallClockMillis,
            nowMillis = nowWallClockMillis
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(TITLE)
            .setContentText(TEXT)
            .setSubText("Hue sunrise")
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(endWallClockMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(openAppIntent(context, alarmId))
            .setStyle(LiveUpdateCountdown.progressStyle(context, progress, R.drawable.ic_alarm))
        PromotedOngoingNotification.request(builder)
        return builder.build()
    }

    fun cancel(context: Context, alarmId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationId(alarmId))
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Hue sunrise",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while Hue lights brighten before an alarm"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    private fun openAppIntent(context: Context, alarmId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId(alarmId),
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun notificationId(alarmId: Long): Int =
        NOTIFICATION_BASE_ID + (alarmId % NOTIFICATION_ID_RANGE).toInt()
}
