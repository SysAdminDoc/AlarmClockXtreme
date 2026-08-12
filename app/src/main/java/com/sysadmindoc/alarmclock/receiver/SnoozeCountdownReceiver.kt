package com.sysadmindoc.alarmclock.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.LiveUpdateCountdown
import com.sysadmindoc.alarmclock.service.PromotedOngoingNotification
import com.sysadmindoc.alarmclock.service.SnoozeCountdownTiming

/** Owns the persistent countdown surface for the currently snoozed alarm. */
class SnoozeCountdownReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE_COUNTDOWN_UPDATE =
            "com.sysadmindoc.alarmclock.SNOOZE_COUNTDOWN_UPDATE"
        const val CHANNEL_SNOOZE_COUNTDOWN = "snooze_countdown_channel"
        const val NOTIFICATION_ID = 3003

        private const val REQUEST_CODE = 10001
        private const val ANDROID_16_API = 36
        private const val PREFS_NAME = "snooze_countdown"
        private const val PREF_START_AT = "start_at_millis"
        private const val PREF_END_AT = "end_at_millis"
        private const val EXTRA_START_AT =
            "com.sysadmindoc.alarmclock.extra.SNOOZE_START_AT_MILLIS"
        private const val EXTRA_END_AT =
            "com.sysadmindoc.alarmclock.extra.SNOOZE_END_AT_MILLIS"

        fun schedule(context: Context, startAtMillis: Long, endAtMillis: Long) {
            val now = System.currentTimeMillis()
            val end = endAtMillis.coerceAtLeast(now + 1_000L)
            val start = startAtMillis.coerceAtMost(now)
            val appContext = context.applicationContext

            createChannel(appContext)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_START_AT, start)
                .putLong(PREF_END_AT, end)
                .apply()

            val notificationManager =
                appContext.getSystemService(NotificationManager::class.java)
            notificationManager?.notify(
                NOTIFICATION_ID,
                buildNotification(appContext, start, end, now)
            )
            scheduleNextUpdate(appContext, start, end, now)
        }

        fun cancel(context: Context) {
            val appContext = context.applicationContext
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            val alarmManager = appContext.getSystemService(AlarmManager::class.java)
            alarmManager?.cancel(pendingIntent(appContext, 0L, 0L))
            appContext.getSystemService(NotificationManager::class.java)
                ?.cancel(NOTIFICATION_ID)
        }

        private fun scheduleNextUpdate(
            context: Context,
            startAtMillis: Long,
            endAtMillis: Long,
            now: Long
        ) {
            val delay = if (Build.VERSION.SDK_INT >= ANDROID_16_API) {
                SnoozeCountdownTiming.millisUntilNextRefresh(now, endAtMillis)
            } else {
                (endAtMillis - now).coerceAtLeast(0L)
            }
            if (delay <= 0L) return

            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val triggerAtMillis = now + delay
            val updateIntent = pendingIntent(context, startAtMillis, endAtMillis)
            try {
                if (canScheduleExactAlarms(alarmManager)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        updateIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        updateIntent
                    )
                }
            } catch (_: Exception) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        updateIntent
                    )
                }
            }
        }

        private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        private fun pendingIntent(
            context: Context,
            startAtMillis: Long,
            endAtMillis: Long
        ): PendingIntent {
            val intent = Intent(ACTION_SNOOZE_COUNTDOWN_UPDATE).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_START_AT, startAtMillis)
                putExtra(EXTRA_END_AT, endAtMillis)
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_SNOOZE_COUNTDOWN,
                context.getString(R.string.notif_channel_snooze_countdown),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_snooze_countdown_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        private fun buildNotification(
            context: Context,
            startAtMillis: Long,
            endAtMillis: Long,
            now: Long
        ): Notification = if (Build.VERSION.SDK_INT >= ANDROID_16_API) {
            buildAndroid16Notification(context, startAtMillis, endAtMillis, now)
        } else {
            NotificationCompat.Builder(context, CHANNEL_SNOOZE_COUNTDOWN)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(context.getString(R.string.notif_snooze_countdown_title))
                .setContentText(
                    context.getString(
                        R.string.notif_snooze_countdown_text,
                        formatRemaining(context, endAtMillis - now)
                    )
                )
                .setSubText(context.getString(R.string.notif_snooze_countdown_subtext))
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setWhen(endAtMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openAppPendingIntent(context))
                .setSilent(true)
                .build()
        }

        @RequiresApi(ANDROID_16_API)
        private fun buildAndroid16Notification(
            context: Context,
            startAtMillis: Long,
            endAtMillis: Long,
            now: Long
        ): Notification {
            val builder = Notification.Builder(context, CHANNEL_SNOOZE_COUNTDOWN)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(context.getString(R.string.notif_snooze_countdown_title))
                .setContentText(
                    context.getString(
                        R.string.notif_snooze_countdown_text,
                        formatRemaining(context, endAtMillis - now)
                    )
                )
                .setSubText(context.getString(R.string.notif_snooze_countdown_subtext))
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setWhen(endAtMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(openAppPendingIntent(context))
                .setStyle(
                    LiveUpdateCountdown.progressStyle(
                        context = context,
                        progress = LiveUpdateCountdown.progress(startAtMillis, endAtMillis, now),
                        trackerIcon = R.drawable.ic_alarm
                    )
                )
            PromotedOngoingNotification.request(builder)
            return builder.build()
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun formatRemaining(context: Context, remainingMillis: Long): String {
            val minutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
            return context.resources.getQuantityString(
                R.plurals.notif_snooze_countdown_minutes,
                minutes.toInt(),
                minutes
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SNOOZE_COUNTDOWN_UPDATE) return

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedEnd = prefs.getLong(PREF_END_AT, 0L)
        val endAtMillis = intent.getLongExtra(EXTRA_END_AT, storedEnd)
        val storedStart = prefs.getLong(PREF_START_AT, 0L)
        val startAtMillis = intent.getLongExtra(EXTRA_START_AT, storedStart)
        if (endAtMillis <= 0L || storedEnd != endAtMillis) return

        val now = System.currentTimeMillis()
        if (endAtMillis <= now) {
            cancel(appContext)
            return
        }

        createChannel(appContext)
        appContext.getSystemService(NotificationManager::class.java)?.notify(
            NOTIFICATION_ID,
            buildNotification(appContext, startAtMillis, endAtMillis, now)
        )
        scheduleNextUpdate(appContext, startAtMillis, endAtMillis, now)
    }
}
