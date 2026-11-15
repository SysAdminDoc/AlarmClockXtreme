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
        reasonCode: String = AlarmIncidentEvent.REASON_NONE,
        source: String = "AlarmScheduler"
    ) = AlarmIncidentEvent(
        fireId = AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt),
        alarmId = alarmId,
        scheduledAt = scheduledAt,
        eventAt = eventAt,
        elapsedMs = eventAt - scheduledAt,
        type = type,
        status = status,
        reasonCode = reasonCode,
        source = source,
        sdkInt = 35,
        standbyBucket = "ACTIVE_10",
        exactAlarmAllowed = "TRUE",
        notificationPermissionGranted = "TRUE",
        fullScreenIntentAllowed = "TRUE",
        batteryOptimizationsIgnored = "TRUE",
        algorithmVersion = AlarmIncidentEvent.VALUE_NONE
    )

    private val armedBeforeExit = incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = exitAt - 5_000L)

    private fun armed(eventAt: Long, scheduledAt: Long, alarmId: Long = 7L) =
        incident(AlarmIncidentEvent.TYPE_SCHEDULE, eventAt = eventAt, scheduledAt = scheduledAt, alarmId = alarmId)

    /** What AlarmService writes once it takes the start command, the first row of a real ring. */
    private fun serviceStart(scheduledAt: Long = alarmAt, alarmId: Long = 7L) = incident(
        AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
        eventAt = scheduledAt + 300L,
        scheduledAt = scheduledAt,
        alarmId = alarmId,
        status = AlarmIncidentEvent.STATUS_RECEIVED,
        reasonCode = "START_COMMAND_RECEIVED",
        source = "AlarmService"
    )

    /** What AlarmReceiver writes when AlarmManager wakes it, before the service is up. */
    private fun receiverRows(scheduledAt: Long = alarmAt) = listOf(
        incident(
            AlarmIncidentEvent.TYPE_BROADCAST,
            eventAt = scheduledAt + 100L,
            scheduledAt = scheduledAt,
            status = AlarmIncidentEvent.STATUS_RECEIVED,
            reasonCode = "ALARM_MANAGER_DELIVERED",
            source = "AlarmReceiver"
        ),
        incident(
            AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
            eventAt = scheduledAt + 150L,
            scheduledAt = scheduledAt,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "START_FOREGROUND_SERVICE",
            source = "AlarmReceiver"
        )
    )

    /** Alarms on now, id to creation time. Alarm 7 has been around since before the exit. */
    private fun outcome(
        incidents: List<AlarmIncidentEvent>,
        now: Long,
        enabled: Map<Long, Long> = mapOf(7L to 0L)
    ) = ProcessExitReports.alarmOutcome(exitAt, incidents, enabled, now)

    /** What AlarmScheduler writes when it disarms an alarm: no time, reason CANCELLED. */
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
    fun `the ringing service taking the start is a ring`() {
        val incidents = listOf(armedBeforeExit) + receiverRows() + serviceStart()
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `the receiver waking up is not a ring on its own`() {
        // The process can die between the broadcast and the service start,
        // which is the very case this report is for.
        val incidents = listOf(armedBeforeExit) + receiverRows()
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a refused service start is not a ring`() {
        val incidents = listOf(armedBeforeExit) + receiverRows().first() + incident(
            AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
            eventAt = alarmAt + 150L,
            status = AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = "START_FOREGROUND_SERVICE_FAILED_ForegroundServiceStartNotAllowedException",
            source = "AlarmReceiver"
        )
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a service start that found no alarm row is not a ring`() {
        val incidents = listOf(armedBeforeExit, serviceStart()) + incident(
            AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
            eventAt = alarmAt + 400L,
            status = AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = "ALARM_ROW_MISSING",
            source = "AlarmService"
        )
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a redelivered start that finds the row gone does not undo the ring before it`() {
        // The service rang, the system killed it, and the redelivered start
        // came after a one-shot was deleted from the ringing screen.
        val incidents = listOf(armedBeforeExit, serviceStart()) + incident(
            AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
            eventAt = alarmAt + 90_000L,
            status = AlarmIncidentEvent.STATUS_RECEIVED,
            reasonCode = "START_COMMAND_REDELIVERED",
            source = "AlarmService"
        ) + incident(
            AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
            eventAt = alarmAt + 90_100L,
            status = AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = "ALARM_ROW_MISSING",
            source = "AlarmService"
        )
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 120_000L)
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
            armed(eventAt = exitAt - 90_000L, scheduledAt = exitAt - 30_000L),
            serviceStart(scheduledAt = exitAt - 30_000L)
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
            outcome(listOf(cancelled(eventAt = exitAt - 10_000L), failed), now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `the earliest armed alarm is the one reported`() {
        val laterAt = alarmAt + 3_600_000L
        val incidents = listOf(
            armed(eventAt = exitAt - 4_000L, scheduledAt = laterAt, alarmId = 8L),
            armedBeforeExit,
            // Only the later alarm rang. The earlier one is what the dead process owed.
            serviceStart(scheduledAt = laterAt, alarmId = 8L)
        )
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 7_200_000L, enabled = mapOf(7L to 0L, 8L to 0L))
        )
    }

    @Test
    fun `an alarm deleted before the exit was not armed across it`() {
        // Seen on a phone: deleted at 1:22, force-stopped at 1:23, and the
        // report still waited on the deleted 6:00 alarm.
        val incidents = listOf(armedBeforeExit, cancelled(eventAt = exitAt - 1_000L))
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(incidents, now = alarmAt + 60_000L, enabled = emptyMap())
        )
    }

    @Test
    fun `an edited alarm counts only at its new time, whichever row sorts first`() {
        val editedAt = exitAt - 1_000L
        val newTime = alarmAt + 3_600_000L
        val rearmed = armed(eventAt = editedAt, scheduledAt = newTime)
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
        val incidents = listOf(armedBeforeExit, serviceStart(), cancelled(eventAt = alarmAt + 120_000L))
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 180_000L, enabled = emptyMap())
        )
    }

    @Test
    fun `a missed alarm deleted after it was due is still reported`() {
        val incidents = listOf(armedBeforeExit, cancelled(eventAt = alarmAt + 120_000L))
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 180_000L, enabled = emptyMap())
        )
    }

    @Test
    fun `an alarm skipped after the exit, before it was due, was changed rather than missed`() {
        // Skip next re-arms the alarm for the following day.
        val incidents = listOf(armedBeforeExit, armed(eventAt = exitAt + 10_000L, scheduledAt = alarmAt + 86_400_000L))
        assertEquals(
            ExitAlarmOutcome.CHANGED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `an alarm deleted or paused after the exit, before it was due, was changed`() {
        val incidents = listOf(armedBeforeExit, cancelled(eventAt = exitAt + 10_000L))
        assertEquals(
            ExitAlarmOutcome.CHANGED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L, enabled = emptyMap())
        )
        // Not due yet either: it won't come up at that time any more.
        assertEquals(
            ExitAlarmOutcome.CHANGED to alarmAt,
            outcome(incidents, now = exitAt + 20_000L)
        )
    }

    @Test
    fun `re-arming the same time after the exit leaves it owed`() {
        // App start and boot passes arm every alarm again at its stored time.
        val incidents = listOf(armedBeforeExit, armed(eventAt = exitAt + 10_000L, scheduledAt = alarmAt))
        assertEquals(
            ExitAlarmOutcome.NO_RING_RECORDED to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L)
        )
    }

    @Test
    fun `a changed alarm gives way to a later one that stayed armed`() {
        val laterAt = alarmAt + 3_600_000L
        val incidents = listOf(
            armedBeforeExit,
            armed(eventAt = exitAt - 4_000L, scheduledAt = laterAt, alarmId = 8L),
            cancelled(eventAt = exitAt + 10_000L)
        )
        assertEquals(
            ExitAlarmOutcome.NOT_DUE_YET to laterAt,
            outcome(incidents, now = exitAt + 20_000L, enabled = mapOf(8L to 0L))
        )
    }

    @Test
    fun `an armed row whose alarm is gone with no cancel logged does not count`() {
        // Seen on a phone after updating: the 6:00 alarm was deleted on the
        // older build, which logged no cancel, and every later exit still
        // waited on it.
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(listOf(armedBeforeExit), now = exitAt + 1_000L, enabled = emptyMap())
        )
    }

    @Test
    fun `an unbacked armed row gives way to the next alarm that is still on`() {
        val laterAt = alarmAt + 3_600_000L
        val incidents = listOf(
            armedBeforeExit,
            armed(eventAt = exitAt - 4_000L, scheduledAt = laterAt, alarmId = 8L)
        )
        assertEquals(
            ExitAlarmOutcome.NOT_DUE_YET to laterAt,
            outcome(incidents, now = exitAt + 1_000L, enabled = mapOf(8L to 0L))
        )
    }

    @Test
    fun `a one-shot that rang and switched itself off still rang`() {
        val incidents = listOf(armedBeforeExit, serviceStart())
        assertEquals(
            ExitAlarmOutcome.RANG to alarmAt,
            outcome(incidents, now = alarmAt + 60_000L, enabled = emptyMap())
        )
    }

    @Test
    fun `an alarm that is on but whose state fell out of the log leaves it unknown`() {
        // The table keeps 100 rows, so a busy log can drop an old arm. Alarm 8
        // existed before the exit and has nothing on record from then.
        val incidents = listOf(cancelled(eventAt = exitAt - 1_000L))
        assertEquals(
            ExitAlarmOutcome.UNKNOWN to 0L,
            outcome(incidents, now = alarmAt, enabled = mapOf(8L to 0L))
        )
        // Made after the exit, it can't have been set at the time.
        assertEquals(
            ExitAlarmOutcome.NO_ALARM_SET to 0L,
            outcome(incidents, now = alarmAt, enabled = mapOf(8L to exitAt + 5_000L))
        )
    }
}
