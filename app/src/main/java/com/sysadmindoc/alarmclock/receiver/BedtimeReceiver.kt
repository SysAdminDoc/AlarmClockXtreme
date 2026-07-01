package com.sysadmindoc.alarmclock.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.BedtimeNotificationTiming
import com.sysadmindoc.alarmclock.service.PromotedOngoingNotification

/**
 * Fires when bedtime reminder triggers.
 * Shows a gentle notification reminding the user to go to sleep.
 */
class BedtimeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BEDTIME_REMINDER = "com.sysadmindoc.alarmclock.BEDTIME_REMINDER"
        private const val ACTION_BEDTIME_COUNTDOWN_UPDATE =
            "com.sysadmindoc.alarmclock.BEDTIME_COUNTDOWN_UPDATE"
        const val CHANNEL_BEDTIME = "bedtime_channel"
        private const val CHANNEL_BEDTIME_COUNTDOWN = "bedtime_countdown_channel"
        const val NOTIFICATION_ID = 3001
        private const val NOTIFICATION_ID_COUNTDOWN = 3002
        private const val REQUEST_CODE_REMINDER = 9999
        private const val REQUEST_CODE_COUNTDOWN = 10000
        private const val EXTRA_REMINDER_TIME_MILLIS =
            "com.sysadmindoc.alarmclock.extra.REMINDER_TIME_MILLIS"
        private const val ANDROID_16_API = 36

        fun schedule(context: Context, reminderTimeMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                return
            }

            scheduleExact(
                context = context,
                alarmManager = alarmManager,
                action = ACTION_BEDTIME_REMINDER,
                requestCode = REQUEST_CODE_REMINDER,
                triggerAtMillis = reminderTimeMillis,
                reminderTimeMillis = reminderTimeMillis
            )
            scheduleCountdownStart(
                context = context,
                alarmManager = alarmManager,
                reminderTimeMillis = reminderTimeMillis,
                now = System.currentTimeMillis()
            )
        }

        fun cancelScheduled(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(
                pendingIntent(
                    context = context,
                    action = ACTION_BEDTIME_REMINDER,
                    requestCode = REQUEST_CODE_REMINDER,
                    reminderTimeMillis = 0L
                )
            )
            alarmManager.cancel(
                pendingIntent(
                    context = context,
                    action = ACTION_BEDTIME_COUNTDOWN_UPDATE,
                    requestCode = REQUEST_CODE_COUNTDOWN,
                    reminderTimeMillis = 0L
                )
            )
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(NOTIFICATION_ID_COUNTDOWN)
        }

        private fun scheduleCountdownStart(
            context: Context,
            alarmManager: AlarmManager,
            reminderTimeMillis: Long,
            now: Long
        ) {
            if (Build.VERSION.SDK_INT < ANDROID_16_API) return
            val firstUpdate = BedtimeNotificationTiming.firstCountdownUpdateAt(
                nowMillis = now,
                reminderTimeMillis = reminderTimeMillis
            )
            if (firstUpdate <= 0L) return
            scheduleExact(
                context = context,
                alarmManager = alarmManager,
                action = ACTION_BEDTIME_COUNTDOWN_UPDATE,
                requestCode = REQUEST_CODE_COUNTDOWN,
                triggerAtMillis = firstUpdate,
                reminderTimeMillis = reminderTimeMillis
            )
        }

        private fun scheduleNextCountdownRefresh(
            context: Context,
            reminderTimeMillis: Long,
            now: Long
        ) {
            if (Build.VERSION.SDK_INT < ANDROID_16_API) return
            val delay = BedtimeNotificationTiming.millisUntilNextRefresh(
                nowMillis = now,
                reminderTimeMillis = reminderTimeMillis
            )
            if (delay <= 0L) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            scheduleExact(
                context = context,
                alarmManager = alarmManager,
                action = ACTION_BEDTIME_COUNTDOWN_UPDATE,
                requestCode = REQUEST_CODE_COUNTDOWN,
                triggerAtMillis = now + delay,
                reminderTimeMillis = reminderTimeMillis
            )
        }

        private fun scheduleExact(
            context: Context,
            alarmManager: AlarmManager,
            action: String,
            requestCode: Int,
            triggerAtMillis: Long,
            reminderTimeMillis: Long
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent(context, action, requestCode, reminderTimeMillis)
            )
        }

        private fun pendingIntent(
            context: Context,
            action: String,
            requestCode: Int,
            reminderTimeMillis: Long
        ): PendingIntent {
            val intent = Intent(action).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_REMINDER_TIME_MILLIS, reminderTimeMillis)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        createChannel(notificationManager)

        when (action) {
            ACTION_BEDTIME_REMINDER -> handleReminder(context, intent, notificationManager)
            ACTION_BEDTIME_COUNTDOWN_UPDATE -> handleCountdownUpdate(context, intent, notificationManager)
        }
    }

    private fun createChannel(notificationManager: NotificationManager) {
        val reminderChannel = NotificationChannel(
            CHANNEL_BEDTIME,
            "Bedtime Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you when it's time to sleep"
        }
        val countdownChannel = NotificationChannel(
            CHANNEL_BEDTIME_COUNTDOWN,
            "Bedtime Countdown",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the final-hour countdown before the bedtime reminder"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannels(listOf(reminderChannel, countdownChannel))
    }

    private fun handleReminder(
        context: Context,
        intent: Intent,
        notificationManager: NotificationManager
    ) {
        notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN)
        val notification = NotificationCompat.Builder(context, CHANNEL_BEDTIME)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Time to wind down")
            .setContentText("Your bedtime is approaching. Start getting ready for sleep.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Reschedule for tomorrow only if bedtime is still enabled
        val prefs = context.getSharedPreferences("app_prefs", 0)
        val bedtimeEnabled = prefs.getBoolean("bedtime_reschedule", true)
        if (bedtimeEnabled) {
            val previousReminder = intent.getLongExtra(EXTRA_REMINDER_TIME_MILLIS, 0L)
            val nextReminder = BedtimeNotificationTiming.nextDailyReminderTime(
                previousReminderTimeMillis = previousReminder,
                nowMillis = System.currentTimeMillis()
            )
            schedule(context, nextReminder)
        }
    }

    private fun handleCountdownUpdate(
        context: Context,
        intent: Intent,
        notificationManager: NotificationManager
    ) {
        if (Build.VERSION.SDK_INT < ANDROID_16_API) return
        val reminderTime = intent.getLongExtra(EXTRA_REMINDER_TIME_MILLIS, 0L)
        val now = System.currentTimeMillis()
        if (reminderTime <= now) {
            notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN)
            return
        }

        if (BedtimeNotificationTiming.shouldUseLiveUpdate(now, reminderTime)) {
            notificationManager.notify(
                NOTIFICATION_ID_COUNTDOWN,
                buildCountdownNotification(context, reminderTime, now)
            )
        }
        scheduleNextCountdownRefresh(context, reminderTime, now)
    }

    @RequiresApi(ANDROID_16_API)
    private fun buildCountdownNotification(
        context: Context,
        reminderTimeMillis: Long,
        now: Long
    ): Notification {
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(
                BedtimeNotificationTiming.liveUpdateProgress(
                    nowMillis = now,
                    reminderTimeMillis = reminderTimeMillis
                )
            )
            .setProgressSegments(
                listOf(
                    Notification.ProgressStyle.Segment(
                        BedtimeNotificationTiming.LIVE_UPDATE_PROGRESS_MAX
                    )
                )
            )
            .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_alarm))

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_BEDTIME_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Wind down soon")
            .setContentText(formatRemaining(reminderTimeMillis - now))
            .setSubText("Bedtime countdown")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(reminderTimeMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .setStyle(progressStyle)

        PromotedOngoingNotification.request(notification)
        return notification.build()
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val minutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
        return if (minutes == 1L) {
            "Bedtime reminder in 1 minute"
        } else {
            "Bedtime reminder in $minutes minutes"
        }
    }
}
