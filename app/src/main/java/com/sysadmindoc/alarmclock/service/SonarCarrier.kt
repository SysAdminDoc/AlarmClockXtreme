package com.sysadmindoc.alarmclock.service

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Measures how much of the sonar carrier came back.
 *
 * [SonarSleepService] emits an 18.75 kHz tone and then decides stillness from
 * the variance of the broadband RMS of everything the microphone hears. That is
 * a room-loudness detector: a fan, traffic or a partner breathing dwarfs a
 * carrier emitted at 1% amplitude, so the tone contributes almost nothing to the
 * measurement it exists for.
 *
 * This is the missing half. A Goertzel filter is the right tool because only one
 * bin is wanted: it costs one multiply-accumulate per sample against the whole
 * FFT for a spectrum nobody needs.
 *
 * Not yet wired into the stillness decision. The existing threshold is tuned to
 * broadband RMS variance and there is no device data here to calibrate an
 * equivalent for carrier magnitude, so swapping it blind would be trading a
 * measurement that works by accident for one that might not work at all.
 */
object SonarCarrier {

    private const val SAMPLE_RATE = 44100
    private const val TONE_HZ = 18750

    /**
     * The analysis length, in samples.
     *
     * Goertzel is exact only when the target lands on a bin centre, which needs
     * N * 18750 / 44100 to be a whole number. That reduces to N being a multiple
     * of 294, so this is the largest multiple of 294 that fits inside the
     * service's 50 ms (2205 sample) read. Anything else smears the carrier
     * across neighbouring bins and reads low.
     */
    const val ANALYSIS_SAMPLES = 2058

    /** The exact bin the carrier lands in for [ANALYSIS_SAMPLES]. 875. */
    const val CARRIER_BIN = ANALYSIS_SAMPLES * TONE_HZ / SAMPLE_RATE

    /**
     * Magnitude of the carrier in [buffer], normalised so a full-scale tone at
     * exactly [TONE_HZ] reads 1.0 and silence reads 0.
     *
     * @param length how much of the buffer holds real samples; anything past
     * [ANALYSIS_SAMPLES] is ignored, and a short read returns 0 rather than
     * measuring a partial window at the wrong bin spacing.
     */
    fun magnitude(buffer: ShortArray, length: Int = buffer.size): Float {
        val usable = minOf(length, buffer.size)
        if (usable < ANALYSIS_SAMPLES) return 0f

        val omega = 2.0 * Math.PI * CARRIER_BIN / ANALYSIS_SAMPLES
        val coefficient = 2.0 * cos(omega)
        var previous = 0.0
        var beforeThat = 0.0
        for (i in 0 until ANALYSIS_SAMPLES) {
            val sample = buffer[i].toDouble() / 32768.0
            val current = sample + coefficient * previous - beforeThat
            beforeThat = previous
            previous = current
        }

        val power = previous * previous + beforeThat * beforeThat -
            coefficient * previous * beforeThat
        if (power <= 0.0) return 0f
        // A real sinusoid of amplitude A puts A*N/2 into the bin.
        return (sqrt(power) * 2.0 / ANALYSIS_SAMPLES).toFloat()
    }
}
