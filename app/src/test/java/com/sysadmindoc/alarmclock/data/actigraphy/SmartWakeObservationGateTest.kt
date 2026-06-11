package com.sysadmindoc.alarmclock.data.actigraphy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartWakeObservationGateTest {

    @Test
    fun requiresEightMinutesBeforeShortWindowCanFireEarly() {
        val start = 1_000L
        val target = start + 15 * 60_000L

        assertFalse(
            SmartWakeObservationGate.canConsiderEarlyFire(
                sessionStartMs = start,
                nowMs = start + 7 * 60_000L + 59_000L,
                targetTimeMs = target
            )
        )
        assertTrue(
            SmartWakeObservationGate.canConsiderEarlyFire(
                sessionStartMs = start,
                nowMs = start + 8 * 60_000L,
                targetTimeMs = target
            )
        )
    }

    @Test
    fun longerWindowsRequireAtLeastOneThirdObserved() {
        val start = 1_000L
        val target = start + 30 * 60_000L

        assertFalse(
            SmartWakeObservationGate.canConsiderEarlyFire(
                sessionStartMs = start,
                nowMs = start + 9 * 60_000L + 59_000L,
                targetTimeMs = target
            )
        )
        assertTrue(
            SmartWakeObservationGate.canConsiderEarlyFire(
                sessionStartMs = start,
                nowMs = start + 10 * 60_000L,
                targetTimeMs = target
            )
        )
    }

    @Test
    fun invalidTimesNeverPassObservationGate() {
        assertFalse(
            SmartWakeObservationGate.canConsiderEarlyFire(
                sessionStartMs = 0L,
                nowMs = 60_000L,
                targetTimeMs = 120_000L
            )
        )
        assertFalse(
            SmartWakeObservationGate.canConsiderEarlyFire(
                sessionStartMs = 120_000L,
                nowMs = 180_000L,
                targetTimeMs = 120_000L
            )
        )
    }
}
