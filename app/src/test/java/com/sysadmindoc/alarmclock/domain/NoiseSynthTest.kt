package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NoiseSynthTest {
    @Test
    fun allPresetsProduceAudibleBoundedOutput() {
        SleepNoisePreset.entries.forEach { preset ->
            val synth = NoiseSynth(preset = preset, seed = 1234L)
            val samples = ShortArray(NoiseSynth.DEFAULT_SAMPLE_RATE)

            synth.fill(samples)

            val peak = samples.maxOf { abs(it.toInt()) }
            val mean = samples.sumOf { it.toLong() }.toDouble() / samples.size
            assertTrue("$preset should not be silent", peak > 450)
            assertTrue("$preset should keep headroom", peak < Short.MAX_VALUE)
            assertTrue("$preset should avoid major DC offset", abs(mean) < 1_400.0)
        }
    }

    @Test
    fun gainScalesOutputWithoutChangingPresetShape() {
        val full = ShortArray(4096)
        val quiet = ShortArray(4096)

        NoiseSynth(SleepNoisePreset.RAIN, seed = 42L).fill(full, gain = 1f)
        NoiseSynth(SleepNoisePreset.RAIN, seed = 42L).fill(quiet, gain = 0.25f)

        val fullPeak = full.maxOf { abs(it.toInt()) }
        val quietPeak = quiet.maxOf { abs(it.toInt()) }
        assertTrue(quietPeak in (fullPeak * 0.20).toInt()..(fullPeak * 0.30).toInt())
    }
}
