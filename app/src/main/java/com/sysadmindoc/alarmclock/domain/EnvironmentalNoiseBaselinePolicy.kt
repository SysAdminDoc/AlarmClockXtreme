package com.sysadmindoc.alarmclock.domain

import kotlin.math.log10

enum class EnvironmentalNoiseLevel {
    QUIET,
    MODERATE,
    LOUD
}

data class EnvironmentalNoiseBaseline(
    val dbfs: Float,
    val level: EnvironmentalNoiseLevel
)

object EnvironmentalNoiseBaselinePolicy {
    private const val MIN_RMS = 0.000001f
    private const val QUIET_MAX_DBFS = -52f
    private const val MODERATE_MAX_DBFS = -38f

    const val DEFAULT_REMINDER_TEXT =
        "Your bedtime is approaching. Start getting ready for sleep."

    fun fromNormalizedRms(rms: Float): EnvironmentalNoiseBaseline? {
        if (!rms.isFinite() || rms <= 0f) return null
        val normalized = rms.coerceIn(MIN_RMS, 1f)
        val dbfs = (20.0 * log10(normalized.toDouble())).toFloat()
        return EnvironmentalNoiseBaseline(
            dbfs = dbfs,
            level = when {
                dbfs < QUIET_MAX_DBFS -> EnvironmentalNoiseLevel.QUIET
                dbfs < MODERATE_MAX_DBFS -> EnvironmentalNoiseLevel.MODERATE
                else -> EnvironmentalNoiseLevel.LOUD
            }
        )
    }

    fun notificationText(baseline: EnvironmentalNoiseBaseline?): String {
        return when (baseline?.level) {
            EnvironmentalNoiseLevel.QUIET ->
                "Room baseline is quiet. Keep lights low and start winding down."
            EnvironmentalNoiseLevel.MODERATE ->
                "Room baseline is moderate. Lower TV, fan, or conversation before sleep."
            EnvironmentalNoiseLevel.LOUD ->
                "Room baseline is loud. Reduce noise before bed if you can."
            null -> DEFAULT_REMINDER_TEXT
        }
    }

    fun levelLabel(level: EnvironmentalNoiseLevel): String {
        return when (level) {
            EnvironmentalNoiseLevel.QUIET -> "Quiet"
            EnvironmentalNoiseLevel.MODERATE -> "Moderate"
            EnvironmentalNoiseLevel.LOUD -> "Loud"
        }
    }
}
