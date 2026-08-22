package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.R
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

        // The phase carries a resource id rather than a word now, because it
        // is computed here and rendered by a composable.
        assertEquals(R.string.breathing_phase_inhale, pattern.phaseAt(0).labelRes)
        assertEquals(4, pattern.phaseAt(0).remainingSeconds)
        assertEquals(R.string.breathing_phase_hold, pattern.phaseAt(4).labelRes)
        assertEquals(7, pattern.phaseAt(4).remainingSeconds)
        assertEquals(R.string.breathing_phase_exhale, pattern.phaseAt(11).labelRes)
        assertEquals(8, pattern.phaseAt(11).remainingSeconds)
        assertEquals(2, pattern.phaseAt(19).cycleNumber)
    }

    @Test
    fun boxBreathingIncludesSecondHold() {
        val phase = BreathingPattern.BOX.phaseAt(12)

        assertEquals(R.string.breathing_phase_hold, phase.labelRes)
        // The second hold and the first share a label but not a cue, which is
        // the whole point of this case.
        assertEquals(R.string.breathing_cue_hold_after_exhale, phase.cueRes)
        assertEquals(4, phase.remainingSeconds)
    }

    @Test
    fun completedPhaseClampsAfterSessionEnd() {
        val phase = BreathingPattern.BOX.phaseAt(999)

        assertEquals(R.string.breathing_phase_complete, phase.labelRes)
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
