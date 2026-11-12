package com.sysadmindoc.alarmclock.worker

import kotlin.math.roundToInt

/**
 * Translates the Hue sunrise curve into DIRIGERA terms: a 1-100 light level,
 * a color temperature that warms up from the light's warmest white toward
 * neutral, and a fade between steps so the change is gradual instead of a
 * jump every step.
 */
object DirigeraSunrisePlan {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 100

    /** Where the sunrise stops cooling, even on a light that can go bluer. */
    const val NEUTRAL_KELVIN = 4000

    /** Longest fade the hub is asked for in one command. */
    const val MAX_TRANSITION_MS = 60_000L

    fun lightLevel(hueBrightness: Int): Int =
        (hueBrightness * MAX_LEVEL / HueSunriseRampPlan.MAX_BRIGHTNESS.toDouble())
            .roundToInt()
            .coerceIn(MIN_LEVEL, MAX_LEVEL)

    /**
     * Color temperature for [hueBrightness] on a light that covers
     * [rangeEndA]..[rangeEndB] kelvin, in either order.
     */
    fun kelvin(hueBrightness: Int, rangeEndA: Int, rangeEndB: Int): Int {
        val warm = minOf(rangeEndA, rangeEndB)
        val cool = maxOf(rangeEndA, rangeEndB).coerceAtMost(maxOf(warm, NEUTRAL_KELVIN))
        val span = HueSunriseRampPlan.MAX_BRIGHTNESS - HueSunriseRampPlan.MIN_BRIGHTNESS
        val fraction = (hueBrightness - HueSunriseRampPlan.MIN_BRIGHTNESS)
            .coerceIn(0, span) / span.toDouble()
        return (warm + (cool - warm) * fraction).roundToInt()
    }

    /** Fade length for one step of a ramp from [rampStartMillis] to [rampEndMillis]. */
    fun transitionMillis(rampStartMillis: Long, rampEndMillis: Long): Long =
        ((rampEndMillis - rampStartMillis) / HueSunriseRampPlan.STEPS)
            .coerceIn(0L, MAX_TRANSITION_MS)
}
