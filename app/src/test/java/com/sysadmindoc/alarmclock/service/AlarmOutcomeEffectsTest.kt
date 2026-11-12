package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.wear.WearNextAlarmBridge
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A test ring has to leave nothing behind: no history row for stats and
 * streaks, no incident, no webhook, no post-dismiss action, nothing on the
 * watch, and no change to when the alarm next rings. Every one of those goes
 * through [AlarmOutcomeEffects], so this drives each entry point in test mode
 * and expects zero calls on every dependency, then runs the same calls for a
 * real ring to show the gate is the only thing standing in the way.
 */
class AlarmOutcomeEffectsTest {

    private val eventRepository = mockk<AlarmEventRepository>(relaxed = true)
    private val incidentRepository = mockk<AlarmIncidentRepository>(relaxed = true)
    private val webhookService = mockk<WebhookService>(relaxed = true)
    private val dismissActionExecutor = mockk<DismissActionExecutor>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val wearBridge = mockk<WearNextAlarmBridge>(relaxed = true)

    private val effects = AlarmOutcomeEffects(
        eventRepository = eventRepository,
        incidentRepository = incidentRepository,
        webhookService = webhookService,
        dismissActionExecutor = dismissActionExecutor,
        alarmScheduler = alarmScheduler,
        wearBridge = wearBridge
    )

    private val alarm = Alarm(id = 7L, hour = 6, minute = 30, label = "Gym", isEnabled = true)

    private var eventBuilt = false
    private var testSnoozeAt = -1L

    private suspend fun runEveryEffect(testRun: Boolean): Boolean {
        effects.recordEvent(testRun) {
            eventBuilt = true
            AlarmEvent(
                alarmId = alarm.id,
                scheduledTime = 1_000L,
                firedAt = 2_000L,
                action = AlarmEvent.ACTION_DISMISSED
            )
        }
        effects.recordIncidentAsync(testRun, alarm.id, "fire-7", 1_000L, "type", "status", "reason", "source")
        effects.recordIncident(testRun, alarm.id, "fire-7", 1_000L, "type", "status", "reason", "source")
        WebhookEvent.entries.forEach { event ->
            effects.fireWebhook(testRun, event, alarm, "6:30", 1_000L, "fire-7")
        }
        effects.runDismissAction(testRun, alarm)
        effects.publishFiring(testRun, alarm)
        effects.publishIdle(testRun, alarm.id)
        effects.finishOccurrence(testRun, alarm.id, 1_000L)
        return effects.scheduleSnooze(testRun, alarm, 99_000L) { _, atMillis ->
            testSnoozeAt = atMillis
            true
        }
    }

    @Test
    fun `a test ring calls none of the lasting side effects`() = runTest {
        val armed = runEveryEffect(testRun = true)

        confirmVerified(
            eventRepository,
            incidentRepository,
            webhookService,
            dismissActionExecutor,
            alarmScheduler,
            wearBridge
        )
        assertFalse("the history row was built for a test", eventBuilt)
        // The test snooze still rings again, just on its own timer.
        assertTrue(armed)
        assertEquals(99_000L, testSnoozeAt)
    }

    @Test
    fun `a real ring reaches every one of them`() = runTest {
        coEvery { alarmScheduler.scheduleSnoozeAt(any(), any()) } returns true

        val armed = runEveryEffect(testRun = false)

        assertTrue(eventBuilt)
        coVerify(exactly = 1) { eventRepository.record(match { it.alarmId == 7L }) }
        verify(exactly = 1) {
            incidentRepository.recordAsync(
                alarmId = 7L,
                fireId = "fire-7",
                scheduledAt = 1_000L,
                eventAt = any(),
                type = "type",
                status = "status",
                reasonCode = "reason",
                source = "source",
                algorithmVersion = any()
            )
        }
        coVerify(exactly = 1) {
            incidentRepository.record(
                alarmId = 7L,
                fireId = "fire-7",
                scheduledAt = 1_000L,
                eventAt = any(),
                type = "type",
                status = "status",
                reasonCode = "reason",
                source = "source",
                algorithmVersion = any()
            )
        }
        WebhookEvent.entries.forEach { event ->
            verify(exactly = 1) {
                webhookService.fireAsync(event, 7L, "Gym", "6:30", 1_000L, "fire-7")
            }
        }
        verify(exactly = 1) { dismissActionExecutor.executeAsync(alarm) }
        verify(exactly = 1) { wearBridge.publishAlarmFiring(alarm) }
        verify(exactly = 1) { wearBridge.publishAlarmIdle(7L) }
        coVerify(exactly = 1) { alarmScheduler.handleAlarmFired(7L, 1_000L) }
        coVerify(exactly = 1) { alarmScheduler.scheduleSnoozeAt(alarm, 99_000L) }
        assertTrue(armed)
        assertEquals("a real snooze took the test path", -1L, testSnoozeAt)
    }

    @Test
    fun `an incident without a saved alarm is dropped either way`() = runTest {
        effects.recordIncidentAsync(false, 0L, "fire-0", 1_000L, "type", "status", "reason", "source")
        effects.recordIncident(false, -1L, "fire-0", 1_000L, "type", "status", "reason", "source")

        confirmVerified(incidentRepository)
    }
}
