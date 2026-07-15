package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RandomRingtoneStartOffsetTest {

    @Test
    fun `short ringtone always begins at the start`() {
        assertEquals(0L, randomRingtoneStartOffsetMs(29_999L, 0.8))
    }

    @Test
    fun `random start preserves the final fifteen seconds`() {
        assertEquals(0L, randomRingtoneStartOffsetMs(60_000L, 0.0))
        assertEquals(22_500L, randomRingtoneStartOffsetMs(60_000L, 0.5))
        assertEquals(45_000L, randomRingtoneStartOffsetMs(60_000L, 1.0))
    }

    @Test
    fun `random input is clamped`() {
        assertEquals(0L, randomRingtoneStartOffsetMs(45_000L, -1.0))
        assertEquals(30_000L, randomRingtoneStartOffsetMs(45_000L, 2.0))
    }
}
