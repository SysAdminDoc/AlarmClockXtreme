package com.sysadmindoc.alarmclock.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * F5: Post-alarm wake confirmation worker.
 *
 * Fires [wakeConfirmDelayMinutes] after alarm dismiss.
 * If the user has not confirmed they are awake (via WakeConfirmActivity),
 * re-fires the alarm by starting AlarmService with ACTION_START_ALARM.
 */
@HiltWorker
class WakeConfirmWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val alarmRepository: AlarmRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        const val CHANNEL_WAKE_CONFIRM = "wake_confirm_channel"
        const val NOTIF_ID_BASE = 5000
    }

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1L)
        if (alarmId == -1L) return Result.success()

        val alarm = alarmRepository.getById(alarmId) ?: return Result.success()
        if (!alarm.isEnabled) return Result.success()

        // Check if user confirmed via the notification button
        val prefs = context.getSharedPreferences("wake_confirm", Context.MODE_PRIVATE)
        val confirmed = prefs.getBoolean("confirmed_$alarmId", false)
        prefs.edit().remove("confirmed_$alarmId").apply()

        if (confirmed) return Result.success()

        // User did not confirm — re-fire the alarm
        val startIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        context.startForegroundService(startIntent)

        return Result.success()
    }
}
