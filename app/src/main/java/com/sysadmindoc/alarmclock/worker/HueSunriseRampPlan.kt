package com.sysadmindoc.alarmclock.worker

/**
 * Wall-clock brightness curve for the Hue sunrise ramp.
 *
 * The ramp used to be a single worker sleeping its way through 20 steps, which
 * WorkManager stops after ten minutes. Anything longer than that (the default
 * pre-wake is 30 minutes, the maximum is 180) was cut off around a third
 * brightness and restarted from 1 on retry.
 *
 * Deriving brightness from the wall clock instead means a run can be split
 * into segments, and a segment that starts late still resumes at the right
 * point rather than replaying the ramp from the beginning.
 */
object HueSunriseRampPlan {
    const val MIN_BRIGHTNESS = 1
    const val MAX_BRIGHTNESS = 254

    /** Steps across the whole ramp, whatever its length. */
    const val STEPS = 20

    /**
     * Longest a single worker run may last. WorkManager stops a worker at ten
     * minutes; the margin covers the bridge probe and the per-step HTTP calls,
     * which each carry a 5 second connect and read timeout.
     */
    const val SEGMENT_MS = 6 * 60_000L

    /** Brightness for [nowMillis], clamped to the Hue range. */
    fun brightnessAt(startMillis: Long, endMillis: Long, nowMillis: Long): Int {
        val total = endMillis - startMillis
        if (total <= 0L) return MAX_BRIGHTNESS
        val elapsed = (nowMillis - startMillis).coerceIn(0L, total)
        val step = Math.round(elapsed.toDouble() / total * STEPS).toInt()
        return (step * MAX_BRIGHTNESS / STEPS).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    /** Wall-clock time of the step after [nowMillis], or null once the ramp is done. */
    fun nextStepAt(startMillis: Long, endMillis: Long, nowMillis: Long): Long? {
        val total = endMillis - startMillis
        if (total <= 0L || nowMillis >= endMillis) return null
        val stepMs = total / STEPS
        if (stepMs <= 0L) return endMillis
        val elapsed = (nowMillis - startMillis).coerceAtLeast(0L)
        val nextStep = (elapsed / stepMs) + 1
        return (startMillis + nextStep * stepMs).coerceAtMost(endMillis)
    }

    /** True once [nowMillis] has reached the end of the ramp. */
    fun isComplete(endMillis: Long, nowMillis: Long): Boolean = nowMillis >= endMillis

    /**
     * When the current worker run should stop and hand over to a follow-up, or
     * null when the ramp finishes inside this segment.
     */
    fun segmentEndsAt(endMillis: Long, segmentStartMillis: Long): Long? {
        val cutoff = segmentStartMillis + SEGMENT_MS
        return if (cutoff < endMillis) cutoff else null
    }
}
