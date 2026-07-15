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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.MainActivity
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

    private fun postWarningNotification(issues: List<String>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val text = if (issues.size == 1) issues.first()
        else issues.joinToString(". ")

        val notification = NotificationCompat.Builder(applicationContext, AlarmService.CHANNEL_UPCOMING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Alarm reliability warning")
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
        const val NOTIFICATION_ID = 9001
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

internal fun alarmHealthIssues(signals: AlarmHealthSignals): List<String> = buildList {
    if (signals.backgroundRestricted) {
        add("Android has restricted background activity — review Alarm reliability in Settings")
    }
    if (signals.batteryOptimizationActive) {
        val guidance = ManufacturerCompat.getGuidance(signals.manufacturer)
        add(
            guidance?.let {
                "Battery optimization is active on ${it.manufacturer} — review its alarm reliability steps"
            } ?: "Battery optimization is active — alarms may not fire reliably"
        )
    }
    if (!signals.notificationsAllowed) {
        add("Notification permission is denied — you may not hear alarms")
    }
    if (!signals.exactAlarmsAllowed) {
        add("Exact alarm permission revoked — alarms cannot fire on time")
    }
}
