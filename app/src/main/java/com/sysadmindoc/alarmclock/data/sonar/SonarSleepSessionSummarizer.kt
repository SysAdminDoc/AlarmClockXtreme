package com.sysadmindoc.alarmclock.data.sonar

import com.sysadmindoc.alarmclock.data.actigraphy.ActigraphySessionSummary
import kotlin.math.roundToInt

object SonarSleepSessionSummarizer {
    const val ALGORITHM_VERSION = "sonar_rms_experimental_v1"

    fun summarize(
        startedAt: Long,
        endedAt: Long,
        stillWindows: Int,
        movementWindows: Int
    ): ActigraphySessionSummary {
        val durationMs = (endedAt - startedAt).coerceAtLeast(0L)
        val totalMinutes = ((durationMs + 59_999L) / 60_000L)
            .toInt()
            .coerceAtLeast(1)
        val totalWindows = (stillWindows + movementWindows).coerceAtLeast(0)

        if (totalWindows == 0) {
            return ActigraphySessionSummary(
                totalMinutes = totalMinutes,
                awakeMinutes = 0,
                lightMinutes = totalMinutes,
                deepMinutes = 0,
                averageSleepIndex = 0.5f,
                algorithm = ALGORITHM_VERSION
            )
        }

        val stillRatio = stillWindows.coerceAtLeast(0).toFloat() / totalWindows
        val movementRatio = movementWindows.coerceAtLeast(0).toFloat() / totalWindows
        val deepMinutes = (totalMinutes * stillRatio)
            .roundToInt()
            .coerceIn(0, totalMinutes)
        val awakeCapacity = totalMinutes - deepMinutes
        val awakeMinutes = (totalMinutes * movementRatio * 0.75f)
            .roundToInt()
            .coerceIn(0, awakeCapacity)
        val lightMinutes = (totalMinutes - deepMinutes - awakeMinutes).coerceAtLeast(0)

        return ActigraphySessionSummary(
            totalMinutes = totalMinutes,
            awakeMinutes = awakeMinutes,
            lightMinutes = lightMinutes,
            deepMinutes = deepMinutes,
            averageSleepIndex = movementRatio.coerceIn(0f, 1f),
            algorithm = ALGORITHM_VERSION
        )
    }
}
