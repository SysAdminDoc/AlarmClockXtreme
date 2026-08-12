package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeCountdownTimingTest {
    @Test
    fun firstRefreshWaitsUntilTheFinalTwoMinutes() {
        val now = 1_000_000L

        assertEquals(
            60_000L,
            SnoozeCountdownTiming.millisUntilNextRefresh(now, now + 3 * 60_000L)
        )
    }

    @Test
    fun refreshesWhenTheRemainingMinuteLabelChanges() {
        val now = 1_000_000L

        assertEquals(
            31_000L,
            SnoozeCountdownTiming.millisUntilNextRefresh(now, now + 90_000L)
        )
    }

    @Test
    fun finalRefreshRunsAtTheSnoozeTarget() {
        val now = 1_000_000L

        assertEquals(
            30_000L,
            SnoozeCountdownTiming.millisUntilNextRefresh(now, now + 30_000L)
        )
        assertEquals(0L, SnoozeCountdownTiming.millisUntilNextRefresh(now, now))
    }
}
