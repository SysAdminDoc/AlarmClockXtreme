package com.sysadmindoc.alarmclock.domain

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R
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

    /**
     * Resource ids, not sentences. This object is reached from a broadcast
     * receiver and from a ViewModel, neither of which is a Compose scope, and
     * both of which have a Context to resolve with.
     */
    @StringRes
    val DEFAULT_REMINDER_TEXT_RES: Int = R.string.bedtime_reminder_default

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

    @StringRes
    fun notificationTextRes(baseline: EnvironmentalNoiseBaseline?): Int {
        return when (baseline?.level) {
            EnvironmentalNoiseLevel.QUIET -> R.string.bedtime_room_quiet_advice
            EnvironmentalNoiseLevel.MODERATE -> R.string.bedtime_room_moderate_advice
            EnvironmentalNoiseLevel.LOUD -> R.string.bedtime_room_loud_advice
            null -> DEFAULT_REMINDER_TEXT_RES
        }
    }

    @StringRes
    fun levelLabelRes(level: EnvironmentalNoiseLevel): Int {
        return when (level) {
            EnvironmentalNoiseLevel.QUIET -> R.string.bedtime_noise_level_quiet
            EnvironmentalNoiseLevel.MODERATE -> R.string.bedtime_noise_level_moderate
            EnvironmentalNoiseLevel.LOUD -> R.string.bedtime_noise_level_loud
        }
    }
}
