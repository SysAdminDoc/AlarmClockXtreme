package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveUpdateCountdownTest {
    @Test
    fun `progress clamps before during and after countdown`() {
        assertEquals(0, LiveUpdateCountdown.progress(1_000L, 11_000L, 0L))
        assertEquals(500, LiveUpdateCountdown.progress(1_000L, 11_000L, 6_000L))
        assertEquals(1_000, LiveUpdateCountdown.progress(1_000L, 11_000L, 12_000L))
    }

    @Test
    fun `elapsed realtime deadline converts to wall clock deadline`() {
        assertEquals(
            1_300_000L,
            LiveUpdateCountdown.elapsedEndToWallClock(
                endElapsedRealtime = 500_000L,
                nowElapsedRealtime = 200_000L,
                nowWallClockMillis = 1_000_000L
            )
        )
    }
}
