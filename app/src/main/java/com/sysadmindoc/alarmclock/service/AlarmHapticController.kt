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

    fun waveform(alarm: Alarm): AlarmVibrationWaveform {
        val (pattern, amplitudes) = when (alarm.vibrationPattern) {
            "gentle" -> longArrayOf(0, 200, 1200, 200, 1200) to intArrayOf(0, 60, 0, 60, 0)
            "heartbeat" -> longArrayOf(0, 150, 100, 150, 800) to intArrayOf(0, 200, 0, 255, 0)
            "escalating" -> longArrayOf(0, 200, 600, 300, 500, 400, 400, 500, 300) to
                intArrayOf(0, 60, 0, 120, 0, 180, 0, 255, 0)
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
