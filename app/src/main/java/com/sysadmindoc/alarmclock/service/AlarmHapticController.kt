package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.model.Alarm

internal data class AlarmVibrationWaveform(
    val pattern: LongArray,
    val amplitudes: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlarmVibrationWaveform) return false
        return pattern.contentEquals(other.pattern) &&
            amplitudes.contentEquals(other.amplitudes)
    }

    override fun hashCode(): Int {
        var result = pattern.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        return result
    }
}

internal data class AlarmHapticEnvelopePoint(
    val intensity: Float,
    val sharpness: Float,
    val durationMillis: Long
)

internal data class AlarmHapticEnvelopePlan(
    val initialSharpness: Float,
    val points: List<AlarmHapticEnvelopePoint>
)

internal object AlarmHapticController {
    const val HAPTIC_ONLY_COMPOSITION_INTERVAL_MS = 1_450L

    fun vibrationDelayMillis(alarm: Alarm): Long? {
        if (!alarm.vibrationEnabled) return null
        return alarm.vibrationDelaySeconds.coerceAtLeast(0) * 1000L
    }

    fun usesMutedAlarmAudio(alarm: Alarm): Boolean {
        return alarm.overrideSystemVolume && alarm.volume <= 0
    }

    fun usesHapticOnlyProfile(alarm: Alarm): Boolean {
        return usesMutedAlarmAudio(alarm) && alarm.vibrationEnabled
    }

    fun escalatingEnvelope(alarm: Alarm): AlarmHapticEnvelopePlan? {
        if (!alarm.vibrationEnabled || alarm.vibrationPattern != "escalating") return null
        val peakIntensity = when (alarm.vibrationIntensity.coerceIn(0, 2)) {
            0 -> return null
            1 -> 0.55f
            else -> 1f
        }
        val peakSharpness = if (alarm.vibrationIntensity == 1) 0.35f else 0.55f
        return AlarmHapticEnvelopePlan(
            initialSharpness = 0.1f,
            points = listOf(
                AlarmHapticEnvelopePoint(peakIntensity * 0.08f, 0.10f, 700L),
                AlarmHapticEnvelopePoint(peakIntensity * 0.18f, 0.14f, 900L),
                AlarmHapticEnvelopePoint(peakIntensity * 0.32f, 0.20f, 1_000L),
                AlarmHapticEnvelopePoint(peakIntensity * 0.50f, 0.26f, 1_000L),
                AlarmHapticEnvelopePoint(peakIntensity * 0.72f, 0.32f, 1_000L),
                AlarmHapticEnvelopePoint(peakIntensity, peakSharpness, 1_000L),
                AlarmHapticEnvelopePoint(peakIntensity, peakSharpness, 600L),
                AlarmHapticEnvelopePoint(0f, 0.10f, 700L)
            )
        )
    }

    fun waveform(alarm: Alarm): AlarmVibrationWaveform {
        val (pattern, amplitudes) = when (alarm.vibrationPattern) {
            "gentle" -> longArrayOf(0, 200, 1200, 200, 1200) to intArrayOf(0, 60, 0, 60, 0)
            "heartbeat" -> longArrayOf(0, 150, 100, 150, 800) to intArrayOf(0, 200, 0, 255, 0)
            "escalating" -> {
                val peak = if (alarm.vibrationIntensity == 1) 140 else 255
                longArrayOf(0, 200, 600, 300, 500, 400, 400, 500, 300) to
                    intArrayOf(0, peak * 60 / 255, 0, peak * 120 / 255, 0, peak * 180 / 255, 0, peak, 0)
            }
            "sos" -> longArrayOf(0, 150, 100, 150, 100, 150, 300, 400, 100, 400, 100, 400, 300, 150, 100, 150, 100, 150, 600) to
                intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0)
            else -> {
                if (usesHapticOnlyProfile(alarm)) {
                    longArrayOf(0, 90, 140, 140, 720, 180, 1300) to
                        intArrayOf(0, 95, 0, 140, 0, 185, 0)
                } else {
                    when (alarm.vibrationIntensity) {
                        1 -> longArrayOf(0, 200, 1000, 200, 1000) to intArrayOf(0, 80, 0, 80, 0)
                        else -> longArrayOf(0, 500, 500, 500, 500) to intArrayOf(0, 255, 0, 255, 0)
                    }
                }
            }
        }
        return AlarmVibrationWaveform(pattern, amplitudes)
    }
}
