package com.sysadmindoc.alarmclock.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HueSunriseRampPlanTest {

    private val start = 1_000_000L
    private val thirtyMinutes = 30 * 60_000L
    private val end = start + thirtyMinutes

    @Test
    fun `ramp opens dim and finishes at full brightness`() {
        assertEquals(1, HueSunriseRampPlan.brightnessAt(start, end, start))
        assertEquals(254, HueSunriseRampPlan.brightnessAt(start, end, end))
    }

    @Test
    fun `brightness follows the wall clock, not the number of steps run`() {
        // A worker resuming halfway through must pick up halfway up the ramp.
        val halfway = start + thirtyMinutes / 2
        assertEquals(127, HueSunriseRampPlan.brightnessAt(start, end, halfway))
    }

    @Test
    fun `brightness never leaves the Hue range`() {
        assertEquals(1, HueSunriseRampPlan.brightnessAt(start, end, start - 10 * 60_000L))
        assertEquals(254, HueSunriseRampPlan.brightnessAt(start, end, end + 10 * 60_000L))
    }

    @Test
    fun `a degenerate window is treated as already finished`() {
        assertEquals(254, HueSunriseRampPlan.brightnessAt(start, start, start))
        assertNull(HueSunriseRampPlan.nextStepAt(start, start, start))
    }

    @Test
    fun `next step advances by one interval and stops at the end`() {
        val stepMs = thirtyMinutes / HueSunriseRampPlan.STEPS
        assertEquals(start + stepMs, HueSunriseRampPlan.nextStepAt(start, end, start))
        assertEquals(
            start + 2 * stepMs,
            HueSunriseRampPlan.nextStepAt(start, end, start + stepMs)
        )
        assertNull(HueSunriseRampPlan.nextStepAt(start, end, end))
    }

    @Test
    fun `a ramp longer than one segment hands over before the ten minute limit`() {
        val handOver = HueSunriseRampPlan.segmentEndsAt(end, start)
        assertNotNull("A 30 minute ramp cannot finish in one worker run", handOver)
        assertTrue(
            "Hand-over must happen inside WorkManager's ten-minute window",
            handOver!! - start < 10 * 60_000L
        )
    }

    @Test
    fun `a short ramp finishes inside a single segment`() {
        val shortEnd = start + 5 * 60_000L
        assertNull(HueSunriseRampPlan.segmentEndsAt(shortEnd, start))
    }

    @Test
    fun `the maximum pre-wake window still completes through segments`() {
        val longEnd = start + 180 * 60_000L
        var now = start
        var segments = 0
        while (!HueSunriseRampPlan.isComplete(longEnd, now) && segments < 100) {
            now = HueSunriseRampPlan.segmentEndsAt(longEnd, now) ?: longEnd
            segments++
        }
        assertTrue(HueSunriseRampPlan.isComplete(longEnd, now))
        assertEquals(254, HueSunriseRampPlan.brightnessAt(start, longEnd, now))
        assertFalse("Segment loop should converge", segments >= 100)
    }
}
