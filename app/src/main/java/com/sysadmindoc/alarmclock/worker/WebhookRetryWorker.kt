package com.sysadmindoc.alarmclock.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.service.WebhookDeliveryOutcome
import com.sysadmindoc.alarmclock.service.WebhookEvent
import com.sysadmindoc.alarmclock.service.WebhookService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Bounded retry for wake-critical webhook deliveries (`alarm_fired` /
 * `alarm_missed`). A dropped delivery can silently break a Tasker flow that a
 * user relies on as a backup alarm, so when the immediate fire-time attempt
 * fails we re-send with exponential backoff a few times. The event identity
 * (eventId + occurredAt) is preserved across attempts so a receiver can dedupe.
 *
 * Non-critical events (snoozed/dismissed/skipped/test) are NOT retried — they
 * stay fire-and-forget.
 */
@HiltWorker
class WebhookRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val webhookService: WebhookService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val event = WebhookEvent.fromWireName(inputData.getString(KEY_EVENT))
            ?: return Result.failure()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1L)
        val occurredAt = inputData.getLong(KEY_OCCURRED_AT, 0L)
        if (occurredAt <= 0L) return Result.failure()

        val outcome = webhookService.deliverOnce(
            event = event,
            alarmId = alarmId,
            label = inputData.getString(KEY_LABEL).orEmpty(),
            displayTime = inputData.getString(KEY_DISPLAY_TIME).orEmpty(),
            includeLabel = inputData.getBoolean(KEY_INCLUDE_LABEL, true),
            scheduledForMillis = inputData.getLong(KEY_SCHEDULED_FOR, -1L).takeIf { it >= 0L },
            fireId = inputData.getString(KEY_FIRE_ID),
            eventId = eventId,
            occurredAtMillis = occurredAt
        )
        return when (outcome) {
            WebhookDeliveryOutcome.Delivered -> Result.success()
            // Integration turned off / URL cleared since the miss — stop retrying.
            WebhookDeliveryOutcome.Skipped -> Result.success()
            WebhookDeliveryOutcome.Failed ->
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val KEY_EVENT = "event"
        private const val KEY_EVENT_ID = "event_id"
        private const val KEY_ALARM_ID = "alarm_id"
        private const val KEY_LABEL = "label"
        private const val KEY_DISPLAY_TIME = "display_time"
        private const val KEY_INCLUDE_LABEL = "include_label"
        private const val KEY_SCHEDULED_FOR = "scheduled_for"
        private const val KEY_FIRE_ID = "fire_id"
        private const val KEY_OCCURRED_AT = "occurred_at"

        /** ~30s, 60s, 120s backoff — 3 retries after the initial fire-time attempt. */
        const val MAX_ATTEMPTS = 3

        fun enqueue(
            context: Context,
            event: WebhookEvent,
            alarmId: Long,
            label: String,
            displayTime: String,
            includeLabel: Boolean,
            scheduledForMillis: Long?,
            fireId: String?,
            eventId: String,
            occurredAtMillis: Long
        ) {
            val data = Data.Builder()
                .putString(KEY_EVENT, event.wireName)
                .putString(KEY_EVENT_ID, eventId)
                .putLong(KEY_ALARM_ID, alarmId)
                .putString(KEY_LABEL, label)
                .putString(KEY_DISPLAY_TIME, displayTime)
                .putBoolean(KEY_INCLUDE_LABEL, includeLabel)
                .putLong(KEY_SCHEDULED_FOR, scheduledForMillis ?: -1L)
                .putString(KEY_FIRE_ID, fireId)
                .putLong(KEY_OCCURRED_AT, occurredAtMillis)
                .build()

            val request = OneTimeWorkRequestBuilder<WebhookRetryWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(data)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "webhook_retry_$eventId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        const val TAG = "webhook_retry"
    }
}
