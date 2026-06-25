package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.service.AlarmService
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
        if (!hasEnabledAlarms) return Result.success()

        val issues = mutableListOf<String>()

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null &&
            !powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName)
        ) {
            issues.add("Battery optimization is active — alarms may not fire reliably")
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            issues.add("Notification permission is denied — you may not hear alarms")
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = applicationContext.getSystemService(
                Context.ALARM_SERVICE
            ) as? android.app.AlarmManager
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                issues.add("Exact alarm permission revoked — alarms cannot fire on time")
            }
        }

        if (issues.isEmpty()) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
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
            .setAutoCancel(true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    applicationContext,
                    0,
                    applicationContext.packageManager.getLaunchIntentForPackage(
                        applicationContext.packageName
                    ),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 9001
    }
}
