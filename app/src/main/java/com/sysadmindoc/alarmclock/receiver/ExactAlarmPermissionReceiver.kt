package com.sysadmindoc.alarmclock.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.worker.ExactAlarmPermissionRescheduleWorker

/**
 * Reschedules enabled alarms as soon as the user grants "Alarms & reminders"
 * after install or restore. Without this receiver, alarms saved while exact
 * alarm access was denied stay unscheduled until the next boot or manual edit.
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        if (!alarmManager.canScheduleExactAlarms()) return

        val request = OneTimeWorkRequestBuilder<ExactAlarmPermissionRescheduleWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            ExactAlarmPermissionRescheduleWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
        Log.i(TAG, "Queued exact-alarm permission recovery")
    }

    private companion object {
        const val TAG = "ExactAlarmPermReceiver"
    }
}
