package com.sysadmindoc.alarmclock.domain

import kotlin.math.PI
import kotlin.math.sin

enum class SleepNoisePreset(val key: String) {
    WHITE("sleep_white_noise"),
    RAIN("sleep_rain"),
    BROWN("sleep_brown_noise"),
    OCEAN("sleep_ocean"),
    FAN("sleep_fan"),
    PINK("sleep_pink_noise"),
    VIOLET("sleep_violet_noise");

    companion object {
        fun fromKey(key: String): SleepNoisePreset =
            entries.firstOrNull { it.key == key } ?: WHITE
    }
}

class NoiseSynth(
    private val preset: SleepNoisePreset,
    seed: Long = 0x5EED_5EEDL,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) {
    private var randomState = seed.takeIf { it != 0L } ?: 0x5EED_5EEDL
    private var pinkB0 = 0.0
    private var pinkB1 = 0.0
    private var pinkB2 = 0.0
    private var brown = 0.0
    private var fanLow = 0.0
    private var phase = 0.0
    private var violetPrev = 0.0

    fun fill(buffer: ShortArray, gain: Float = 1f) {
        val clampedGain = gain.coerceIn(0f, 1f).toDouble()
        for (index in buffer.indices) {
            val sample = when (preset) {
                SleepNoisePreset.WHITE -> white() * 0.22
                SleepNoisePreset.RAIN -> rain()
                SleepNoisePreset.BROWN -> brownNoise()
                SleepNoisePreset.OCEAN -> ocean()
                SleepNoisePreset.FAN -> fan()
                SleepNoisePreset.PINK -> pink() * 1.8
                SleepNoisePreset.VIOLET -> violet()
            }
            buffer[index] = (sample.coerceIn(-1.0, 1.0) * clampedGain * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun rain(): Double {
        val bed = pink() * 0.16
        val hiss = white() * 0.06
        val drop = if (nextUnit() > 0.9965) white() * 0.42 else 0.0
        return bed + hiss + drop
    }

    private fun ocean(): Double {
        phase += (2.0 * PI) / (sampleRate * 7.5)
        if (phase > 2.0 * PI) phase -= 2.0 * PI
        val swell = 0.58 + (sin(phase) + 1.0) * 0.21
        return (brownNoise() * 0.74 + pink() * 0.08) * swell
    }

    private fun fan(): Double {
        fanLow += (white() - fanLow) * 0.045
        val blade = sin(phase * 5.0) * 0.025
        phase += (2.0 * PI) / (sampleRate * 0.18)
        if (phase > 2.0 * PI) phase -= 2.0 * PI
        return fanLow * 0.52 + blade
    }

    private fun brownNoise(): Double {
        brown = (brown + white() * 0.035).coerceIn(-1.0, 1.0)
        brown *= 0.995
        return brown * 0.62
    }

    private fun pink(): Double {
        val white = white()
        pinkB0 = 0.99765 * pinkB0 + white * 0.0990460
        pinkB1 = 0.96300 * pinkB1 + white * 0.2965164
        pinkB2 = 0.57000 * pinkB2 + white * 1.0526913
        return (pinkB0 + pinkB1 + pinkB2 + white * 0.1848) * 0.08
    }

    private fun violet(): Double {
        val w = white()
        val v = (w - violetPrev) * 0.38
        violetPrev = w
        return v
    }

    private fun white(): Double = nextUnit() * 2.0 - 1.0

    private fun nextUnit(): Double {
        randomState = randomState * 6364136223846793005L + 1442695040888963407L
        val bits = randomState ushr 11
        return bits.toDouble() / (1L shl 53).toDouble()
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
    }
}
