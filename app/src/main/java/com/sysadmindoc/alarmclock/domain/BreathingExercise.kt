package com.sysadmindoc.alarmclock.domain

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R

data class BreathingPhase(
    // Ids, not sentences: the phase is computed in a domain object with no
    // Context and rendered by a composable that has stringResource.
    @StringRes val labelRes: Int,
    @StringRes val cueRes: Int,
    val remainingSeconds: Int,
    val cycleNumber: Int,
    val cycleCount: Int,
    val completed: Boolean = false
)

enum class BreathingPattern(
    val displayName: String,
    val inhaleSeconds: Int,
    val holdAfterInhaleSeconds: Int,
    val exhaleSeconds: Int,
    val holdAfterExhaleSeconds: Int,
    val cycleCount: Int
) {
    FOUR_SEVEN_EIGHT(
        displayName = "4-7-8",
        inhaleSeconds = 4,
        holdAfterInhaleSeconds = 7,
        exhaleSeconds = 8,
        holdAfterExhaleSeconds = 0,
        cycleCount = 4
    ),
    BOX(
        displayName = "Box",
        inhaleSeconds = 4,
        holdAfterInhaleSeconds = 4,
        exhaleSeconds = 4,
        holdAfterExhaleSeconds = 4,
        cycleCount = 4
    );

    val cycleSeconds: Int
        get() = inhaleSeconds + holdAfterInhaleSeconds + exhaleSeconds + holdAfterExhaleSeconds

    val totalSeconds: Int
        get() = cycleSeconds * cycleCount

    fun phaseAt(elapsedSeconds: Int): BreathingPhase {
        val clamped = elapsedSeconds.coerceIn(0, totalSeconds)
        if (clamped >= totalSeconds) {
            return BreathingPhase(
                labelRes = R.string.breathing_phase_complete,
                cueRes = R.string.breathing_cue_complete,
                remainingSeconds = 0,
                cycleNumber = cycleCount,
                cycleCount = cycleCount,
                completed = true
            )
        }

        val cycleIndex = clamped / cycleSeconds
        val intoCycle = clamped % cycleSeconds
        val cycleNumber = cycleIndex + 1
        var phaseStart = 0

        fun phase(@StringRes labelRes: Int, @StringRes cueRes: Int, duration: Int): BreathingPhase {
            val remaining = phaseStart + duration - intoCycle
            return BreathingPhase(
                labelRes = labelRes,
                cueRes = cueRes,
                remainingSeconds = remaining.coerceAtLeast(1),
                cycleNumber = cycleNumber,
                cycleCount = cycleCount
            )
        }

        if (intoCycle < phaseStart + inhaleSeconds) {
            return phase(
                R.string.breathing_phase_inhale,
                R.string.breathing_cue_inhale,
                inhaleSeconds
            )
        }
        phaseStart += inhaleSeconds

        if (holdAfterInhaleSeconds > 0 && intoCycle < phaseStart + holdAfterInhaleSeconds) {
            return phase(
                R.string.breathing_phase_hold,
                R.string.breathing_cue_hold_after_inhale,
                holdAfterInhaleSeconds
            )
        }
        phaseStart += holdAfterInhaleSeconds

        if (intoCycle < phaseStart + exhaleSeconds) {
            return phase(
                R.string.breathing_phase_exhale,
                R.string.breathing_cue_exhale,
                exhaleSeconds
            )
        }
        phaseStart += exhaleSeconds

        return phase(
            R.string.breathing_phase_hold,
            R.string.breathing_cue_hold_after_exhale,
            holdAfterExhaleSeconds
        )
    }
}

fun formatBreathingDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remainder = safe % 60
    return if (minutes > 0) {
        "${minutes}:${remainder.toString().padStart(2, '0')}"
    } else {
        "${remainder}s"
    }
}
