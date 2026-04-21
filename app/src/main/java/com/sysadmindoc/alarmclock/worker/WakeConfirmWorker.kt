package com.sysadmindoc.alarmclock.worker

import android.app.NotificationChannel
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
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.alarmfiring.WakeConfirmActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * F5: Post-alarm wake confirmation worker.
 *
 * Fires `wakeConfirmDelayMinutes` after alarm dismiss. Posts a high-priority
 * notification with a full-screen intent that opens [WakeConfirmActivity].
 * Waits up to [CONFIRM_WAIT_MS] for the user to confirm via the activity
 * (which writes "confirmed_<id>=true" into SharedPreferences).
 * If still unconfirmed when the wait expires, re-fires the alarm.
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
        // Time the user has to tap "I'm awake" before the alarm re-fires.
        private const val CONFIRM_WAIT_MS = 60_000L
        private const val POLL_INTERVAL_MS = 1_000L
    }

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1L)
        if (alarmId == -1L) return Result.success()

        val alarm = alarmRepository.getById(alarmId) ?: return Result.success()
        // If the alarm was disabled after it fired (user cancelled, or it was
        // a one-shot that auto-disabled), skip the confirmation prompt.
        if (!alarm.isEnabled) return Result.success()

        val prefs = context.getSharedPreferences("wake_confirm", Context.MODE_PRIVATE)
        // Clean any prior confirmation token before posting the prompt.
        prefs.edit().remove("confirmed_$alarmId").apply()

        ensureChannel()
        postPrompt(alarmId, alarm.label)

        // Poll for confirmation up to CONFIRM_WAIT_MS.
        val deadline = System.currentTimeMillis() + CONFIRM_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (prefs.getBoolean("confirmed_$alarmId", false)) {
                prefs.edit().remove("confirmed_$alarmId").apply()
                cancelPrompt(alarmId)
                return Result.success()
            }
            delay(POLL_INTERVAL_MS)
        }

        // No confirmation in time: clear the prompt and re-fire the alarm.
        cancelPrompt(alarmId)
        prefs.edit().remove("confirmed_$alarmId").apply()

        val startIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        try {
            context.startForegroundService(startIntent)
        } catch (_: Exception) {
            // Background-start restrictions may block this on rare OEMs; the
            // notification's full-screen intent stays available.
        }
        return Result.success()
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_WAKE_CONFIRM) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WAKE_CONFIRM,
                    "Wake Confirmation",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Asks you to confirm you are awake after dismissing an alarm"
                    enableVibration(true)
                    setBypassDnd(true)
                }
            )
        }
    }

    private fun postPrompt(alarmId: Long, label: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val activityIntent = Intent(context, WakeConfirmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(WakeConfirmActivity.EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            (NOTIF_ID_BASE + alarmId).toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (label.isBlank()) "Are you awake?" else "Awake check: $label"
        val notification = NotificationCompat.Builder(context, CHANNEL_WAKE_CONFIRM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText("Tap to confirm — otherwise the alarm will ring again.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        nm.notify((NOTIF_ID_BASE + alarmId).toInt(), notification)
    }

    private fun cancelPrompt(alarmId: Long) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel((NOTIF_ID_BASE + alarmId).toInt())
    }
}
