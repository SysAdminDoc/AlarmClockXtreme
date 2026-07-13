package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeConsistencyCalculatorTest {

    @Test
    fun `returns null below the minimum sample count`() {
        assertNull(WakeConsistencyCalculator.consistencyScore(listOf(420, 420, 420)))
        assertNull(WakeConsistencyCalculator.consistencyScore(emptyList()))
    }

    @Test
    fun `identical wake times score 100`() {
        val sevenAm = List(7) { 7 * 60 }
        val result = WakeConsistencyCalculator.consistencyScore(sevenAm)!!
        assertEquals(100, result.score)
        assertEquals(7, result.sampleCount)
    }

    @Test
    fun `tightly clustered times score high`() {
        // Wake within a ~15-minute band around 6:30.
        val times = listOf(6 * 60 + 25, 6 * 60 + 30, 6 * 60 + 35, 6 * 60 + 28, 6 * 60 + 33)
        val score = WakeConsistencyCalculator.consistencyScore(times)!!.score
        assertTrue("expected high score, was $score", score >= 90)
    }

    @Test
    fun `wraps around midnight so 2355 and 0005 are close`() {
        val times = listOf(23 * 60 + 55, 5, 23 * 60 + 58, 2)
        val score = WakeConsistencyCalculator.consistencyScore(times)!!.score
        // If midnight weren't handled circularly these would look ~12h apart and
        // score near zero; circular stats keep them tightly clustered.
        assertTrue("expected high score across midnight, was $score", score >= 90)
    }

    @Test
    fun `spread-out times score low`() {
        // Roughly evenly spread around the clock -> near-zero resultant length.
        val times = listOf(0, 6 * 60, 12 * 60, 18 * 60)
        val score = WakeConsistencyCalculator.consistencyScore(times)!!.score
        assertTrue("expected low score, was $score", score <= 10)
    }

    @Test
    fun `out-of-range samples are ignored`() {
        val result = WakeConsistencyCalculator.consistencyScore(
            listOf(420, 420, 420, 420, -5, 5000)
        )!!
        assertEquals(4, result.sampleCount)
        assertEquals(100, result.score)
    }
}
