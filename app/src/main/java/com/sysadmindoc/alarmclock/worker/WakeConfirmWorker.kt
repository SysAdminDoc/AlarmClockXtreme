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
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.alarmfiring.WakeConfirmActivity
import com.sysadmindoc.alarmclock.util.AlarmPublicText
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
    private val alarmRepository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val alarmIncidentRepository: AlarmIncidentRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        const val KEY_ALARM_FIRE_ID = "alarm_fire_id"
        const val KEY_SCHEDULED_AT = "scheduled_at"
        const val CHANNEL_WAKE_CONFIRM = "wake_confirm_channel"
        // Far above the other per-id notification ranges (TimerFinished is
        // 7000 + timerId, also unbounded) so a long-lived install with alarm
        // ids past 2000 can't collide wake-confirm and timer notifications.
        const val NOTIF_ID_BASE = 500_000
        // Time the user has to tap "I'm awake" before the alarm re-fires.
        private const val CONFIRM_WAIT_MS = 60_000L
        private const val POLL_INTERVAL_MS = 1_000L
    }

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1L)
        if (alarmId == -1L) return Result.success()
        val scheduledAt = inputData.getLong(KEY_SCHEDULED_AT, 0L)
        val fireId = inputData.getString(KEY_ALARM_FIRE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)

        val alarm = alarmRepository.getById(alarmId) ?: run {
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "WAKE_CONFIRM_ALARM_ROW_MISSING"
            )
            return Result.success()
        }
        // If the alarm was disabled after it fired (user cancelled, or it was
        // a one-shot that auto-disabled), skip the confirmation prompt.
        if (!alarm.isEnabled) {
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "WAKE_CONFIRM_ALARM_DISABLED"
            )
            return Result.success()
        }

        val prefs = context.getSharedPreferences("wake_confirm", Context.MODE_PRIVATE)
        // Clean any prior confirmation token before posting the prompt.
        prefs.edit().remove("confirmed_$alarmId").apply()

        ensureChannel()
        recordIncident(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "WAKE_CONFIRM_PROMPT_REQUESTED"
        )
        val promptPosted = postPrompt(
            alarmId = alarmId,
            label = alarm.label,
            hideLabel = preferencesManager.getCurrentSettings().hideAlarmLabelsOnPublicSurfaces,
            fireId = fireId,
            scheduledAt = scheduledAt
        )
        recordIncident(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            status = if (promptPosted) AlarmIncidentEvent.STATUS_SUCCEEDED else AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = if (promptPosted) "WAKE_CONFIRM_PROMPT_POSTED" else "WAKE_CONFIRM_PROMPT_NO_NOTIFICATION_MANAGER"
        )

        // Poll for confirmation up to CONFIRM_WAIT_MS.
        val deadline = System.currentTimeMillis() + CONFIRM_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (prefs.getBoolean("confirmed_$alarmId", false)) {
                prefs.edit().remove("confirmed_$alarmId").apply()
                cancelPrompt(alarmId)
                recordIncident(
                    alarmId = alarmId,
                    fireId = fireId,
                    scheduledAt = scheduledAt,
                    status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                    reasonCode = "WAKE_CONFIRM_CONFIRMED"
                )
                return Result.success()
            }
            delay(POLL_INTERVAL_MS)
        }

        // No confirmation in time: clear the prompt and re-fire the alarm.
        cancelPrompt(alarmId)
        prefs.edit().remove("confirmed_$alarmId").apply()
        recordIncident(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            status = AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = "WAKE_CONFIRM_TIMED_OUT"
        )

        val startIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
        }
        try {
            context.startForegroundService(startIntent)
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = "WAKE_CONFIRM_REFIRE_REQUESTED"
            )
        } catch (_: Exception) {
            // Background-start restrictions may block this on rare OEMs; the
            // notification's full-screen intent stays available.
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "WAKE_CONFIRM_REFIRE_START_FAILED"
            )
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

    private fun postPrompt(
        alarmId: Long,
        label: String,
        hideLabel: Boolean,
        fireId: String,
        scheduledAt: Long
    ): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        val activityIntent = Intent(context, WakeConfirmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(WakeConfirmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(WakeConfirmActivity.EXTRA_ALARM_FIRE_ID, fireId)
            putExtra(WakeConfirmActivity.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            (NOTIF_ID_BASE + alarmId).toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = AlarmPublicText.wakeConfirmTitle(label, hideLabel)
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
        return true
    }

    private fun cancelPrompt(alarmId: Long) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel((NOTIF_ID_BASE + alarmId).toInt())
    }

    private suspend fun recordIncident(
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        status: String,
        reasonCode: String,
        type: String = AlarmIncidentEvent.TYPE_WAKE_CONFIRM
    ) {
        alarmIncidentRepository.record(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = "WakeConfirmWorker"
        )
    }
}
