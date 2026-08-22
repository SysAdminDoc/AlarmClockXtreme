package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.util.ManufacturerCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlarmHealthWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val alarmRepository: AlarmRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val hasEnabledAlarms = alarmRepository.getEnabled().isNotEmpty()
        if (!hasEnabledAlarms) {
            cancelWarningNotification()
            return Result.success()
        }

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        val exactAlarmsAllowed = if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = applicationContext.getSystemService(
                Context.ALARM_SERVICE
            ) as? android.app.AlarmManager
            alarmManager?.canScheduleExactAlarms() != false
        } else true

        val issues = alarmHealthIssues(
            AlarmHealthSignals(
                batteryOptimizationActive = powerManager?.isIgnoringBatteryOptimizations(
                    applicationContext.packageName
                ) == false,
                backgroundRestricted = Build.VERSION.SDK_INT >= 28 &&
                    activityManager?.isBackgroundRestricted == true,
                notificationsAllowed = notificationsAllowed,
                exactAlarmsAllowed = exactAlarmsAllowed,
                manufacturer = ManufacturerCompat.getManufacturer()
            )
        )

        if (issues.isEmpty()) {
            cancelWarningNotification()
            return Result.success()
        }

        postWarningNotification(issues)
        return Result.success()
    }

    private fun postWarningNotification(issues: List<AlarmHealthIssue>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val sentences = issues.map { issue ->
            issue.manufacturer
                ?.let { applicationContext.getString(issue.messageRes, it) }
                ?: applicationContext.getString(issue.messageRes)
        }
        val text = sentences.joinToString(" ")

        val notification = NotificationCompat.Builder(applicationContext, AlarmService.CHANNEL_UPCOMING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(applicationContext.getString(R.string.notif_alarm_health_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Reliability reminder, not a firing alert — categorize it so Android 16
            // Notification Cooldown treats it correctly and never groups it with the
            // wake-critical alarm-category notifications (which stay CATEGORY_ALARM).
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(applicationContext, MainActivity::class.java).apply {
                        data = Uri.parse("acx://navigate/settings")
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelWarningNotification() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
    }

    companion object {
        // Outside the open-ended 7000+timerId notification band (the old 9001
        // collided with a running timer's notification at timer id 2001).
        const val NOTIFICATION_ID = 6_503
        const val IMMEDIATE_WORK_NAME = "alarm_health_check_now"

        fun enqueueImmediate(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AlarmHealthWorker>().build()
            )
        }
    }
}

internal data class AlarmHealthSignals(
    val batteryOptimizationActive: Boolean,
    val backgroundRestricted: Boolean,
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val manufacturer: String
)

/**
 * One reliability problem worth telling the user about.
 *
 * A resource id rather than a sentence: this list used to be built as English
 * and the worker's own test asserted on substrings of it, which is the pattern
 * that quietly breaks on the first translation.
 */
internal data class AlarmHealthIssue(
    @StringRes val messageRes: Int,
    val manufacturer: String? = null
)

internal fun alarmHealthIssues(signals: AlarmHealthSignals): List<AlarmHealthIssue> = buildList {
    if (signals.backgroundRestricted) {
        add(AlarmHealthIssue(R.string.health_background_restricted))
    }
    if (signals.batteryOptimizationActive) {
        val guidance = ManufacturerCompat.getGuidance(signals.manufacturer)
        add(
            guidance?.let {
                AlarmHealthIssue(
                    R.string.health_battery_optimization_manufacturer,
                    it.manufacturer
                )
            } ?: AlarmHealthIssue(R.string.health_battery_optimization)
        )
    }
    if (!signals.notificationsAllowed) {
        add(AlarmHealthIssue(R.string.health_notifications_denied))
    }
    if (!signals.exactAlarmsAllowed) {
        add(AlarmHealthIssue(R.string.health_exact_alarms_revoked))
    }
}
