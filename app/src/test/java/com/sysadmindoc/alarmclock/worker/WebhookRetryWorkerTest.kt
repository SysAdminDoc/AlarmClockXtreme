package com.sysadmindoc.alarmclock.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.sysadmindoc.alarmclock.service.WebhookDeliveryOutcome
import com.sysadmindoc.alarmclock.service.WebhookEvent
import com.sysadmindoc.alarmclock.service.WebhookService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebhookRetryWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun buildWorker(
        outcome: WebhookDeliveryOutcome?,
        runAttemptCount: Int = 0,
        event: String? = WebhookEvent.AlarmFired.wireName,
        eventId: String? = "evt-1",
        occurredAt: Long = 1_000L
    ): WebhookRetryWorker {
        val service = mockk<WebhookService>()
        if (outcome != null) {
            coEvery { service.deliverOnce(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns outcome
        }
        val data = workDataOf(
            "event" to event,
            "event_id" to eventId,
            "alarm_id" to 42L,
            "label" to "Wake",
            "display_time" to "07:00",
            "include_label" to true,
            "scheduled_for" to -1L,
            "fire_id" to "fire-1",
            "occurred_at" to occurredAt
        )
        return TestListenableWorkerBuilder<WebhookRetryWorker>(context)
            .setInputData(data)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(
                object : androidx.work.WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: androidx.work.WorkerParameters
                    ): WebhookRetryWorker =
                        WebhookRetryWorker(appContext, workerParameters, service)
                }
            )
            .build()
    }

    @Test
    fun deliveredOutcomeSucceeds() = runBlocking {
        val worker = buildWorker(WebhookDeliveryOutcome.Delivered)
        assertEquals(Result.success(), worker.doWork())
    }

    @Test
    fun skippedOutcomeSucceedsAndStopsRetrying() = runBlocking {
        // Integration turned off / URL cleared since the miss — no point retrying.
        val worker = buildWorker(WebhookDeliveryOutcome.Skipped)
        assertEquals(Result.success(), worker.doWork())
    }

    @Test
    fun failedOutcomeRetriesWhileUnderCap() = runBlocking {
        val worker = buildWorker(WebhookDeliveryOutcome.Failed, runAttemptCount = 0)
        assertEquals(Result.retry(), worker.doWork())
    }

    @Test
    fun failedOutcomeRetriesOnFinalAttemptBelowCap() = runBlocking {
        // runAttemptCount is 0-based; attempt index 2 is still < MAX_ATTEMPTS (3).
        val worker = buildWorker(
            WebhookDeliveryOutcome.Failed,
            runAttemptCount = WebhookRetryWorker.MAX_ATTEMPTS - 1
        )
        assertEquals(Result.retry(), worker.doWork())
    }

    @Test
    fun failedOutcomeGivesUpAtCap() = runBlocking {
        val worker = buildWorker(
            WebhookDeliveryOutcome.Failed,
            runAttemptCount = WebhookRetryWorker.MAX_ATTEMPTS
        )
        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun unknownEventFailsFastWithoutDelivery() = runBlocking {
        val worker = buildWorker(outcome = null, event = "not_a_real_event")
        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun missingEventIdFailsFast() = runBlocking {
        val worker = buildWorker(outcome = null, eventId = null)
        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun nonPositiveOccurredAtFailsFast() = runBlocking {
        val worker = buildWorker(outcome = null, occurredAt = 0L)
        assertEquals(Result.failure(), worker.doWork())
    }
}
