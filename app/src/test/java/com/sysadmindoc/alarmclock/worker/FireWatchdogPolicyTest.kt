package com.sysadmindoc.alarmclock.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FireWatchdogPolicyTest {

    private val scheduledAt = 1_000_000_000_000L
    // Two minutes after the scheduled fire — the normal watchdog check time.
    private val checkNow = scheduledAt + FireWatchdogPolicy.WATCHDOG_DELAY_MS

    private fun decide(
        repeatMissedEnabled: Boolean = true,
        alarmExists: Boolean = true,
        isEnabled: Boolean = true,
        deliveryCount: Int = 0,
        scheduledAtMs: Long = scheduledAt,
        nowMs: Long = checkNow
    ) = FireWatchdogPolicy.decide(
        repeatMissedEnabled, alarmExists, isEnabled, deliveryCount, scheduledAtMs, nowMs
    )

    @Test
    fun suppressedFireInWindowRefires() {
        // The core case: AlarmManager never delivered (0 broadcasts), alarm is
        // still enabled, and we are inside the recovery window.
        val decision = decide()
        assertEquals(FireWatchdogPolicy.Decision.REFIRE, decision)
        assertTrue(decision.shouldRefire)
    }

    @Test
    fun deliveredFireDoesNotRefire() {
        // A working alarm always leaves a BROADCAST incident — never double-fire.
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_ALREADY_FIRED,
            decide(deliveryCount = 1)
        )
        assertFalse(decide(deliveryCount = 3).shouldRefire)
    }

    @Test
    fun repeatMissedOptOutSkips() {
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_DISABLED_SETTING,
            decide(repeatMissedEnabled = false)
        )
    }

    @Test
    fun missingAlarmRowSkips() {
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_NO_ALARM,
            decide(alarmExists = false)
        )
    }

    @Test
    fun disabledAlarmSkips() {
        // A one-shot that fired is auto-disabled; a recurring alarm the user
        // turned off is disabled. Either way, don't re-fire.
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_DISABLED,
            decide(isEnabled = false)
        )
    }

    @Test
    fun tooEarlySkips() {
        // Original fire may still be arriving right at the scheduled instant.
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_STALE,
            decide(nowMs = scheduledAt + FireWatchdogPolicy.MIN_AGE_MS - 1)
        )
    }

    @Test
    fun tooLateSkips() {
        // Past the ceiling the user has moved on — don't surprise them.
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_STALE,
            decide(nowMs = scheduledAt + FireWatchdogPolicy.MAX_AGE_MS + 1)
        )
    }

    @Test
    fun nonPositiveScheduledAtSkips() {
        assertEquals(
            FireWatchdogPolicy.Decision.SKIP_STALE,
            decide(scheduledAtMs = 0L, nowMs = FireWatchdogPolicy.WATCHDOG_DELAY_MS)
        )
    }

    @Test
    fun windowBoundariesAreInclusive() {
        assertEquals(
            FireWatchdogPolicy.Decision.REFIRE,
            decide(nowMs = scheduledAt + FireWatchdogPolicy.MIN_AGE_MS)
        )
        assertEquals(
            FireWatchdogPolicy.Decision.REFIRE,
            decide(nowMs = scheduledAt + FireWatchdogPolicy.MAX_AGE_MS)
        )
    }

    @Test
    fun `a smart-wake early fire counts as delivered`() {
        // Smart wake and Direct Boot start AlarmService directly, so they write
        // a FOREGROUND_SERVICE incident and never a BROADCAST one. Counting
        // only broadcasts made the watchdog re-ring an alarm that was already
        // ringing, resetting playback and the auto-silence timer.
        assertFalse(decide(deliveryCount = 1).shouldRefire)
        assertTrue(decide(deliveryCount = 0).shouldRefire)
    }
}
