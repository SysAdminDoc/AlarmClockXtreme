package com.sysadmindoc.alarmclock.ui.timer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.AlarmService

object TimerNotifications {
    private const val TAG = "TimerNotifications"
    private const val TIMER_NOTIFICATION_BASE_ID = 7_000
    private const val TIMER_REBOOT_NOTIFICATION_ID = 7_999
    internal const val FINISHED_TITLE = "Timer finished"
    internal const val GENERIC_TIMER_TEXT = "Timer"

    fun postFinished(
        context: Context,
        timerId: Int,
        label: String,
        hidePublicLabel: Boolean = shouldHidePublicLabels(context)
    ) {
        AlarmService.createNotificationChannels(context)
        try {
            NotificationManagerCompat.from(context).notify(
                TIMER_NOTIFICATION_BASE_ID + timerId,
                buildFinishedNotification(context, timerId, label, hidePublicLabel)
            )
        } catch (_: SecurityException) {
            // Notification permission denied; expiry is still persisted so the
            // timer shows as finished when the app is reopened.
        } catch (error: Exception) {
            Log.w(TAG, "Failed to post timer-finished notification", error)
        }
    }

    internal fun buildFinishedNotification(
        context: Context,
        timerId: Int,
        label: String,
        hidePublicLabel: Boolean
    ): Notification {
        val contentIntent = openAppIntent(context, TIMER_NOTIFICATION_BASE_ID + timerId)
        val privateBuilder = NotificationCompat.Builder(context, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(FINISHED_TITLE)
            .setContentText(label.ifBlank { GENERIC_TIMER_TEXT })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentIntent)
        val publicVersion = NotificationCompat.Builder(context, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(FINISHED_TITLE)
            .setContentText(GENERIC_TIMER_TEXT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .build()
        return applyPublicLabelPolicy(privateBuilder, hidePublicLabel, publicVersion).build()
    }

    internal fun applyPublicLabelPolicy(
        privateBuilder: NotificationCompat.Builder,
        hidePublicLabel: Boolean,
        publicVersion: Notification
    ): NotificationCompat.Builder {
        return if (hidePublicLabel) {
            privateBuilder
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
        } else {
            privateBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
    }

    internal fun shouldHidePublicLabels(context: Context): Boolean {
        val app = context.applicationContext as? AlarmClockApp ?: return true
        return runCatching {
            app.preferencesManager.shouldHideLabelsOnPublicSurfaces()
        }.getOrDefault(true)
    }

    fun postTimersCanceledAfterRestart(context: Context, canceledCount: Int) {
        if (canceledCount <= 0) return
        AlarmService.createNotificationChannels(context)
        try {
            NotificationManagerCompat.from(context).notify(
                TIMER_REBOOT_NOTIFICATION_ID,
                NotificationCompat.Builder(context, AlarmService.CHANNEL_TIMER)
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle("Timers canceled after restart")
                    .setContentText(
                        if (canceledCount == 1) {
                            "A running timer could not survive the device restart."
                        } else {
                            "$canceledCount running timers could not survive the device restart."
                        }
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(openAppIntent(context, TIMER_REBOOT_NOTIFICATION_ID))
                    .build()
            )
        } catch (_: SecurityException) {
            // Notification permission denied.
        } catch (error: Exception) {
            Log.w(TAG, "Failed to post timer restart notification", error)
        }
    }

    fun cancelFinished(context: Context, timerId: Int) {
        try {
            NotificationManagerCompat.from(context)
                .cancel(TIMER_NOTIFICATION_BASE_ID + timerId)
        } catch (_: Exception) {
            // Notification is already gone.
        }
    }

    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
