package com.sysadmindoc.alarmclock.ui.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.service.LiveUpdateCountdown
import com.sysadmindoc.alarmclock.service.PromotedOngoingNotification

object TimerNotifications {
    private const val TAG = "TimerNotifications"
    private const val TIMER_NOTIFICATION_BASE_ID = 7_000

    // Kept outside the open-ended 7000+timerId band (the old 7_999 collided
    // with a running timer's notification at timer id 999).
    private const val TIMER_REBOOT_NOTIFICATION_ID = 6_502
    private const val CHANNEL_RUNNING_TIMER = "running_timer_channel"
    private const val ANDROID_16_API = 36

    fun postRunning(
        context: Context,
        timer: PersistedTimerRecord,
        hidePublicLabel: Boolean = shouldHidePublicLabels(context),
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime(),
        nowWallClockMillis: Long = System.currentTimeMillis()
    ) {
        if (timer.state != TimerState.RUNNING || timer.endElapsedRealtime <= nowElapsedRealtime) return
        createRunningChannel(context)
        val notification = buildRunningNotification(
            context = context,
            timer = timer,
            hidePublicLabel = hidePublicLabel,
            nowElapsedRealtime = nowElapsedRealtime,
            nowWallClockMillis = nowWallClockMillis
        )
        try {
            NotificationManagerCompat.from(context).notify(notificationId(timer.id), notification)
        } catch (_: SecurityException) {
            // The exact alarm remains scheduled even when notification access is denied.
        } catch (error: Exception) {
            Log.w(TAG, "Failed to post running timer notification", error)
        }
    }

    internal fun buildRunningNotification(
        context: Context,
        timer: PersistedTimerRecord,
        hidePublicLabel: Boolean,
        nowElapsedRealtime: Long,
        nowWallClockMillis: Long
    ): Notification {
        val endWallClock = LiveUpdateCountdown.elapsedEndToWallClock(
            endElapsedRealtime = timer.endElapsedRealtime,
            nowElapsedRealtime = nowElapsedRealtime,
            nowWallClockMillis = nowWallClockMillis
        )
        val contentIntent = openAppIntent(context, notificationId(timer.id))
        return if (Build.VERSION.SDK_INT >= ANDROID_16_API) {
            buildLiveUpdateNotification(
                context = context,
                timer = timer,
                hidePublicLabel = hidePublicLabel,
                endWallClock = endWallClock,
                contentIntent = contentIntent,
                nowElapsedRealtime = nowElapsedRealtime
            )
        } else {
            buildCompatRunningNotification(
                context = context,
                timer = timer,
                hidePublicLabel = hidePublicLabel,
                endWallClock = endWallClock,
                contentIntent = contentIntent
            )
        }
    }

    private fun buildCompatRunningNotification(
        context: Context,
        timer: PersistedTimerRecord,
        hidePublicLabel: Boolean,
        endWallClock: Long,
        contentIntent: PendingIntent
    ): Notification {
        val privateBuilder = compatRunningBuilder(
            context = context,
            title = timer.label.ifBlank { context.getString(R.string.notif_timer_generic) },
            endWallClock = endWallClock,
            contentIntent = contentIntent
        )
        val publicVersion = compatRunningBuilder(
            context = context,
            title = context.getString(R.string.notif_timer_generic),
            endWallClock = endWallClock,
            contentIntent = contentIntent
        ).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()
        return applyPublicLabelPolicy(privateBuilder, hidePublicLabel, publicVersion).build()
    }

    private fun compatRunningBuilder(
        context: Context,
        title: String,
        endWallClock: Long,
        contentIntent: PendingIntent
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_RUNNING_TIMER)
        .setSmallIcon(R.drawable.ic_alarm)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.notif_timer_running_text))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(true)
        .setWhen(endWallClock)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setContentIntent(contentIntent)

    @RequiresApi(ANDROID_16_API)
    private fun buildLiveUpdateNotification(
        context: Context,
        timer: PersistedTimerRecord,
        hidePublicLabel: Boolean,
        endWallClock: Long,
        contentIntent: PendingIntent,
        nowElapsedRealtime: Long
    ): Notification {
        val startElapsedRealtime = timer.endElapsedRealtime - timer.totalSeconds * 1_000L
        val progress = LiveUpdateCountdown.progress(
            startMillis = startElapsedRealtime,
            endMillis = timer.endElapsedRealtime,
            nowMillis = nowElapsedRealtime
        )
        val publicVersion = platformRunningBuilder(
            context = context,
            title = context.getString(R.string.notif_timer_generic),
            endWallClock = endWallClock,
            contentIntent = contentIntent,
            progress = progress
        ).setVisibility(Notification.VISIBILITY_PUBLIC).build()
        val builder = platformRunningBuilder(
            context = context,
            title = timer.label.ifBlank { context.getString(R.string.notif_timer_generic) },
            endWallClock = endWallClock,
            contentIntent = contentIntent,
            progress = progress
        )
        if (hidePublicLabel) {
            builder
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
        } else {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }
        PromotedOngoingNotification.request(builder)
        return builder.build()
    }

    @RequiresApi(ANDROID_16_API)
    private fun platformRunningBuilder(
        context: Context,
        title: String,
        endWallClock: Long,
        contentIntent: PendingIntent,
        progress: Int
    ): Notification.Builder = Notification.Builder(context, CHANNEL_RUNNING_TIMER)
        .setSmallIcon(R.drawable.ic_alarm)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.notif_timer_running_text))
        .setSubText(context.getString(R.string.notif_timer_generic))
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setShowWhen(true)
        .setWhen(endWallClock)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setContentIntent(contentIntent)
        .setStyle(LiveUpdateCountdown.progressStyle(context, progress, R.drawable.ic_alarm))

    private fun createRunningChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING_TIMER,
                context.getString(R.string.notif_channel_running_timers),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_running_timers_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    fun postFinished(
        context: Context,
        timerId: Int,
        label: String,
        hidePublicLabel: Boolean = shouldHidePublicLabels(context)
    ) {
        AlarmService.createNotificationChannels(context)
        try {
            NotificationManagerCompat.from(context).notify(
                notificationId(timerId),
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
        val contentIntent = openAppIntent(context, notificationId(timerId))
        val restartIntent = restartPendingIntent(context, timerId, notificationId(timerId))
        val privateBuilder = NotificationCompat.Builder(context, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notif_timer_finished_title))
            .setContentText(label.ifBlank { context.getString(R.string.notif_timer_generic) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_alarm, context.getString(R.string.timer_restart), restartIntent)
        val publicVersion = NotificationCompat.Builder(context, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notif_timer_finished_title))
            .setContentText(context.getString(R.string.notif_timer_generic))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_alarm, context.getString(R.string.timer_restart), restartIntent)
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
                    .setContentTitle(context.getString(R.string.notif_timers_canceled_title))
                    .setContentText(
                        context.resources.getQuantityString(
                            R.plurals.notif_timers_canceled_text,
                            canceledCount,
                            canceledCount
                        )
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

    fun cancelTimer(context: Context, timerId: Int) {
        try {
            NotificationManagerCompat.from(context)
                .cancel(notificationId(timerId))
        } catch (_: Exception) {
            // Notification is already gone.
        }
    }

    fun cancelFinished(context: Context, timerId: Int) = cancelTimer(context, timerId)

    internal fun notificationId(timerId: Int): Int = TIMER_NOTIFICATION_BASE_ID + timerId

    internal fun restartPendingIntent(
        context: Context,
        timerId: Int,
        requestCode: Int
    ): PendingIntent = PendingIntent.getService(
        context,
        requestCode,
        Intent(context, TimerAlarmService::class.java)
            .setAction(TimerAlarmService.ACTION_RESTART)
            .putExtra(TimerAlarmService.EXTRA_TIMER_ID, timerId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

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
