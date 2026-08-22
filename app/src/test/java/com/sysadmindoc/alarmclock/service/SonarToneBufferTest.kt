package com.sysadmindoc.alarmclock.service

import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sonar emitter fills one buffer with a sine wave starting at sample 0 and
 * then writes that same buffer on a loop for the whole night. That only sounds
 * like a continuous tone if the buffer holds a whole number of periods.
 *
 * At 44100 Hz a 18750 Hz tone repeats every 294 samples (125 cycles). A buffer
 * that is not a multiple of 294 jumps phase at every wrap, and a step
 * discontinuity repeating at 44100/bufferSize Hz is a click train with energy
 * right across the audible band. The entire point of an 18.75 kHz carrier is
 * that nobody hears it, so this is worth pinning.
 */
class SonarToneBufferTest {

    private val sampleRate = 44100
    private val toneHz = 18750

    @Test
    fun `the buffer is always a whole number of tone periods`() {
        listOf(1, 293, 294, 295, 3527, 3528, 3529, 8192).forEach { minimum ->
            val size = SonarSleepService.toneBufferSize(minimum)
            assertEquals(
                "buffer for a minimum of $minimum is not a whole number of periods",
                0,
                size % SonarSleepService.TONE_PERIOD_SAMPLES
            )
            assertTrue("buffer for a minimum of $minimum is too small", size >= minimum)
        }
    }

    @Test
    fun `it never returns a buffer below one period`() {
        // A device reporting an error code or zero would otherwise produce an
        // empty buffer and a silent, useless session.
        assertEquals(
            SonarSleepService.TONE_PERIOD_SAMPLES,
            SonarSleepService.toneBufferSize(0)
        )
        assertEquals(
            SonarSleepService.TONE_PERIOD_SAMPLES,
            SonarSleepService.toneBufferSize(-512)
        )
    }

    @Test
    fun `the wave meets itself where the loop wraps`() {
        // Rebuild what the emitter builds, then check that continuing past the
        // end lands on the value the start already holds. That is the property
        // the period alignment exists for.
        val size = SonarSleepService.toneBufferSize(3528)
        val samples = FloatArray(size) { i ->
            sin(2.0 * Math.PI * toneHz * i / sampleRate).toFloat()
        }

        // Only the wrap value is worth asserting. At 44100 Hz this tone is 2.35
        // samples per cycle, so the difference between neighbouring samples
        // swings across most of the range by design; a slope-continuity check
        // would mean nothing here.
        val nextAfterEnd = sin(2.0 * Math.PI * toneHz * size / sampleRate).toFloat()
        assertEquals(samples[0], nextAfterEnd, 1e-4f)
    }

    @Test
    fun `an unaligned buffer really would jump, which is what this guards`() {
        // 3527 is one sample short of a period boundary. Without the rounding,
        // the emitter would use exactly this and step by a large fraction of
        // full scale at every wrap.
        val unaligned = 3527
        val nextAfterEnd = sin(2.0 * Math.PI * toneHz * unaligned / sampleRate).toFloat()
        val first = 0f
        assertTrue(
            "3527 samples should not land back at the start, but got $nextAfterEnd",
            abs(nextAfterEnd - first) > 0.1f
        )
    }
}
