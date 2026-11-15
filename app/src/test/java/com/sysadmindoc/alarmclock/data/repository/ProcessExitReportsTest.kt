package com.sysadmindoc.alarmclock.data.repository

import android.app.ApplicationExitInfo
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessExitReportsTest {

    private val exitAt = 1_000_000L
    private val alarmAt = exitAt + 60_000L

    private fun incident(
        type: String,
        eventAt: Long,
        scheduledAt: Long = alarmAt,
        alarmId: Long = 7L,
        status: String = AlarmIncidentEvent.STATUS_SUCCEEDED,
        reasonCode: String = AlarmIncidentEvent.REASON_NONE
    ) = AlarmIncidentEvent(
        fireId = AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt),
        alarmId = alarmId,
        scheduledAt = scheduledAt,
        eventAt = eventAt,
        elapsedMs = eventAt - scheduledAt,
        type = type,
        status = status,
        reasonCode = reasonCode,
        source = "test",
        sdkInt = 35,
        standbyBucket = "ACTIVE_10",
        exactAlarmAllowed = "TRUE",
        notificationPermissionGranted = "TRUE",
        fullScreenIntentAllowed = "TRUE",
        batteryOptimizationsIgnored = "TRUE",
        algorithmVersion = AlarmIncidentEvent.VALUE_NONE
    )

    private val armedBeforeExit = incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = exitAt - 5_000L)

    private fun outcome(
        incidents: List<AlarmIncidentEvent>,
        now: Long,
        enabled: Set<Long> = setOf(7L, 8L)
    ) = ProcessExitReports.alarmOutcome(exitAt, incidents, enabled, now)

    /** What AlarmScheduler.cancel() writes: no time, reason CANCELLED. */
    private fun cancelled(eventAt: Long, alarmId: Long = 7L) = incident(
        AlarmIncidentEvent.TYPE_SCHEDULE,
        eventAt = eventAt,
        scheduledAt = 0L,
        alarmId = alarmId,
        reasonCode = AlarmIncidentEvent.REASON_CANCELLED
    )

    @Test
    fun `the memory limiter tag wins over the reason code`() {
        assertEquals(
            ProcessExitCause.MEMORY_LIMIT,
            ProcessExitReports.cause(ApplicationExitInfo.REASON_OTHER, "MemoryLimiter:AnonSwap")
        )
    }

    @Test
    fun `reason codes map to what a user can act on`() {
        val expected = mapOf(
            ApplicationExitInfo.REASON_USER_REQUESTED to ProcessExitCause.FORCE_STOPPED,
            ApplicationExitInfo.REASON_USER_STOPPED to ProcessExitCause.TASK_MANAGER,
            ApplicationExitInfo.REASON_LOW_MEMORY to ProcessExitCause.LOW_MEMORY,
            ApplicationExitInfo.REASON_CRASH to ProcessExitCause.CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE to ProcessExitCause.CRASH,
            ApplicationExitInfo.REASON_ANR to ProcessExitCause.NOT_RESPONDING,
            ApplicationExitInfo.REASON_PACKAGE_UPDATED to ProcessExitCause.APP_UPDATED,
            ApplicationExitInfo.REASON_FREEZER to ProcessExitCause.FROZEN,
            ApplicationExitInfo.REASON_SIGNALED to ProcessExitCause.SYSTEM_KILLED,
            ApplicationExitInfo.REASON_UNKNOWN to ProcessExitCause.UNKNOWN,
            999 to ProcessExitCause.UNKNOWN
        )
        expected.forEach { (code, cause) ->
            assertEquals("reason $code", cause, ProcessExitReports.cause(code, "remove task"))
        }
    }

    @Test
    fun `an exit older than the incident log is unknown`() {
        val laterOnly = listOf(incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = exitAt + 1L))
        assertEquals(
            ExitAlarmOutcome.UNKNOWN to 0L,
            outcome(laterOnly, now = alarmAt + 1L)
        )
        assertEquals(
            ExitAlarmOutcome.UNKNOWN to 0L,
            outcome(emptyList(), now = alarmAt + 1L)
        )
    }

    @Test
    fun `an alarm armed across the exit that was delivered rang`() {
        val incidents = listOf(
            armedBeforeExit,
            incident(AlarmIncidentEvent.TYPE_BROADCAST, eventAt = alarmAt + 200L)
        )
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a direct service start counts as a ring`() {
        val incidents = listOf(
            armedBeforeExit,
            incident(AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE, eventAt = alarmAt + 200L)
        )
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a failed delivery row is not a ring`() {
        val incidents = listOf(
            armedBeforeExit,
            incident(
                AlarmIncidentEvent.TYPE_BROADCAST,
                eventAt = alarmAt + 200L,
                status = AlarmIncidentEvent.STATUS_FAILED
            ),
            incident(
                AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                eventAt = alarmAt + 300L,
                status = AlarmIncidentEvent.STATUS_FAILED
            )
        )
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a past-due armed alarm with no delivery has no ring on record`() {
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(listOf(armedBeforeExit), now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `an armed alarm still in the future has not come up yet`() {
        assertEquals(
            ExitAlarmOutcome.NOT_DUE_YET to alarmAt,
            outcome(listOf(armedBeforeExit), now = exitAt + 1_000L)
        )
    }

    @Test
    fun `nothing armed across the exit means no alarm was set`() {
        val incidents = listOf(
            // Rang before the exit, so it wasn't pending when the process died.
            incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = exitAt - 90_000L, scheduledAt = exitAt - 30_000L),
            incident(AlarmIncidentEvent.TYPE_BROADCAST, eventAt = exitAt - 30_000L, scheduledAt = exitAt - 30_000L)
        )
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(incidents, now = alarmAt)
        )
    }

    @Test
    fun `a failed schedule does not count as armed`() {
        val failed = incident(
            AlarmIncidentEvent.TYPE_SCHEDULE,
            eventAt = exitAt - 5_000L,
            status = AlarmIncidentEvent.STATUS_FAILED
        )
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(listOf(failed), now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `the earliest armed alarm is the one reported`() {
        val later = incident(
            AlarmIncidentEvent.TYPE_SCHEDULE,
            eventAt = exitAt - 4_000L,
            scheduledAt = alarmAt + 3_600_000L,
            alarmId = 8L
        )
        val incidents = listOf(
            later,
            armedBeforeExit,
            // Only the later alarm rang; the earlier one is what the dead process owed.
            incident(AlarmIncidentEvent.TYPE_BROADCAST, eventAt = alarmAt + 3_600_000L, scheduledAt = alarmAt + 3_600_000L, alarmId = 8L)
        )
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 7_200_000L)
        )
    }

    @Test
    fun `an alarm deleted before the exit was not armed across it`() {
        // Seen on a phone: deleted at 1:22, force-stopped at 1:23, and the
        // report still waited on the deleted 6:00 alarm.
        val incidents = listOf(armedBeforeExit, cancelled(eventAt = exitAt - 1_000L))
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `an edited alarm counts only at its new time, whichever row sorts first`() {
        val editedAt = exitAt - 1_000L
        val newTime = alarmAt + 3_600_000L
        val rearmed = incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = editedAt, scheduledAt = newTime)
        listOf(
            listOf(armedBeforeExit, cancelled(editedAt), rearmed),
            listOf(rearmed, cancelled(editedAt), armedBeforeExit)
        ).forEach { incidents ->
            assertEquals(
                ExitAlarmOutcome.NOT_DUE_YET to newTime,
                outcome(incidents, now = alarmAt + 60_000L)
            )
        }
    }

    @Test
    fun `a delete after the exit does not change what was armed at the exit`() {
        val incidents = listOf(
            armedBeforeExit,
            incident(AlarmIncidentEvent.TYPE_BROADCAST, eventAt = alarmAt, scheduledAt = alarmAt),
            cancelled(eventAt = alarmAt + 120_000L)
        )
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 180_000L, enabled = emptySet())
        )
    }

    @Test
    fun `a missed alarm deleted after the exit is still reported`() {
        val incidents = listOf(armedBeforeExit, cancelled(eventAt = alarmAt + 120_000L))
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 180_000L, enabled = emptySet())
        )
    }

    @Test
    fun `an armed row whose alarm is gone with no cancel logged does not count`() {
        // Seen on a phone after updating: the 6:00 alarm was deleted on the
        // older build, which logged no cancel, and every later exit still
        // waited on it.
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(listOf(armedBeforeExit), now = exitAt + 1_000L, enabled = emptySet())
        )
    }

    @Test
    fun `an unbacked armed row gives way to the next alarm that is still on`() {
        val laterAt = alarmAt + 3_600_000L
        val incidents = listOf(
            armedBeforeExit,
            incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = exitAt - 4_000L, scheduledAt = laterAt, alarmId = 8L)
        )
        assertEquals(
            ExitAlarmOutcome.NOT_DUE_YET to laterAt,
            outcome(incidents, now = exitAt + 1_000L, enabled = setOf(8L))
        )
    }

    @Test
    fun `a one-shot that rang and switched itself off still rang`() {
        val incidents = listOf(
            armedBeforeExit,
            incident(AlarmIncidentEvent.TYPE_BROADCAST, eventAt = alarmAt + 200L)
        )
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L, enabled = emptySet())
        )
    }
}
