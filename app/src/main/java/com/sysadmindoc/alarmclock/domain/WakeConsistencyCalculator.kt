package com.sysadmindoc.alarmclock.domain

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A locally-computed "wake consistency" score derived from recent alarm-dismiss
 * times — how tightly clustered the user's wake time-of-day is. This is the
 * honest, no-cloud/no-ML slice of a sleep-regularity metric: we only have wake
 * (dismiss) times, not full sleep/wake state, so it measures wake-time
 * regularity rather than clinical Sleep Regularity Index.
 *
 * Uses circular statistics (the mean resultant length R of the wake times mapped
 * onto a 24-hour circle) so that, e.g., 23:55 and 00:05 count as close rather
 * than a full day apart. R is in [0,1]; 1 = identical times every day.
 */
object WakeConsistencyCalculator {

    /** Below this many samples the score is not meaningful and we return null. */
    const val MIN_SAMPLES = 4

    data class Result(val score: Int, val sampleCount: Int)

    /**
     * @param wakeMinutesOfDay minutes past local midnight (0..1439) for each
     *   recent wake event. Out-of-range values are ignored.
     * @return a 0-100 score (100 = perfectly regular), or null if there isn't
     *   enough data to be meaningful.
     */
    fun consistencyScore(wakeMinutesOfDay: List<Int>): Result? {
        val valid = wakeMinutesOfDay.filter { it in 0..1439 }
        if (valid.size < MIN_SAMPLES) return null
        var sumSin = 0.0
        var sumCos = 0.0
        for (minute in valid) {
            val angle = minute / 1440.0 * 2.0 * PI
            sumSin += sin(angle)
            sumCos += cos(angle)
        }
        val n = valid.size
        val r = sqrt((sumSin / n).pow(2) + (sumCos / n).pow(2)).coerceIn(0.0, 1.0)
        return Result(score = (r * 100).roundToInt().coerceIn(0, 100), sampleCount = n)
    }

    @StringRes
    fun labelRes(score: Int): Int = when {
        score >= 85 -> R.string.wake_consistency_very
        score >= 65 -> R.string.wake_consistency_fairly
        score >= 40 -> R.string.wake_consistency_somewhat
        else -> R.string.wake_consistency_highly
    }
}
