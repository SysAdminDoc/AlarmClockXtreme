package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingExerciseTest {

    @Test
    fun fourSevenEightUsesExpectedCycleLength() {
        val pattern = BreathingPattern.FOUR_SEVEN_EIGHT

        assertEquals(19, pattern.cycleSeconds)
        assertEquals(76, pattern.totalSeconds)
    }

    @Test
    fun fourSevenEightPhaseBoundariesAreStable() {
        val pattern = BreathingPattern.FOUR_SEVEN_EIGHT

        assertEquals("Inhale", pattern.phaseAt(0).label)
        assertEquals(4, pattern.phaseAt(0).remainingSeconds)
        assertEquals("Hold", pattern.phaseAt(4).label)
        assertEquals(7, pattern.phaseAt(4).remainingSeconds)
        assertEquals("Exhale", pattern.phaseAt(11).label)
        assertEquals(8, pattern.phaseAt(11).remainingSeconds)
        assertEquals(2, pattern.phaseAt(19).cycleNumber)
    }

    @Test
    fun boxBreathingIncludesSecondHold() {
        val phase = BreathingPattern.BOX.phaseAt(12)

        assertEquals("Hold", phase.label)
        assertEquals("Stay soft before the next breath.", phase.cue)
        assertEquals(4, phase.remainingSeconds)
    }

    @Test
    fun completedPhaseClampsAfterSessionEnd() {
        val phase = BreathingPattern.BOX.phaseAt(999)

        assertEquals("Complete", phase.label)
        assertEquals(0, phase.remainingSeconds)
        assertTrue(phase.completed)
        assertFalse(BreathingPattern.BOX.phaseAt(0).completed)
    }

    @Test
    fun durationFormatterUsesClockStyleAfterOneMinute() {
        assertEquals("0s", formatBreathingDuration(-1))
        assertEquals("59s", formatBreathingDuration(59))
        assertEquals("1:00", formatBreathingDuration(60))
        assertEquals("1:05", formatBreathingDuration(65))
    }
}
