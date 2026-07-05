package com.sysadmindoc.alarmclock.domain

data class BreathingPhase(
    val label: String,
    val cue: String,
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
                label = "Complete",
                cue = "Let your breathing return to normal.",
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

        fun phase(label: String, cue: String, duration: Int): BreathingPhase {
            val remaining = phaseStart + duration - intoCycle
            return BreathingPhase(
                label = label,
                cue = cue,
                remainingSeconds = remaining.coerceAtLeast(1),
                cycleNumber = cycleNumber,
                cycleCount = cycleCount
            )
        }

        if (intoCycle < phaseStart + inhaleSeconds) {
            return phase("Inhale", "Breathe in slowly through your nose.", inhaleSeconds)
        }
        phaseStart += inhaleSeconds

        if (holdAfterInhaleSeconds > 0 && intoCycle < phaseStart + holdAfterInhaleSeconds) {
            return phase("Hold", "Keep your chest relaxed and still.", holdAfterInhaleSeconds)
        }
        phaseStart += holdAfterInhaleSeconds

        if (intoCycle < phaseStart + exhaleSeconds) {
            return phase("Exhale", "Release the breath slowly.", exhaleSeconds)
        }
        phaseStart += exhaleSeconds

        return phase("Hold", "Stay soft before the next breath.", holdAfterExhaleSeconds)
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
