package com.sysadmindoc.alarmclock.ui.theme

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * Three-stop vertical sky gradient. `top` is the zenith, `mid` is the middle
 * band, `bot` is the horizon. All three colors are fully opaque so the
 * brush composes cleanly against any underlying surface.
 */
data class SkyGradient(
    val top: Color,
    val mid: Color,
    val bot: Color,
)

/**
 * One row in the time-of-day keyframe table. `t` is the fractional position
 * along the day cycle: t=0 is sunrise, t=1 is sunset. The table extends
 * past those bounds (negative for pre-dawn, >1 for post-sunset) so a single
 * 24-hour cycle resolves to a continuous interpolation over [-0.40, 1.40].
 */
data class SkyKeyframe(val t: Float, val gradient: SkyGradient)

/**
 * Time-of-day sky engine.
 *
 * Anchored to real sunrise (t=0) and sunset (t=1), 15 keyframes describe
 * the sky from deep night before dawn through golden hour into late dusk.
 * For any current time, [computeT] returns the fractional position along
 * the cycle, and [gradientForT] linearly interpolates between the two
 * surrounding keyframes.
 *
 * Color values were dialed in by the user (see CHANGELOG v1.9.0) — they
 * intentionally lean warm at the horizon during sunrise / sunset and cool
 * at the zenith, matching the spectrum a viewer actually sees.
 *
 * The same `t` is reused at later layers to drive lightning intensity and
 * tornado-warning legibility, so this is the canonical "where in the day
 * are we" function for the whole UI.
 */
object TimeOfDaySky {

    private val KEYFRAMES = listOf(
        // Pre-dawn ─────────────────────────────────────────────────────
        SkyKeyframe(-0.40f, SkyGradient(Color(0xFF02030F), Color(0xFF070A22), Color(0xFF01020A))),
        SkyKeyframe(-0.08f, SkyGradient(Color(0xFF0C1230), Color(0xFF3A2A55), Color(0xFF7A4258))),
        SkyKeyframe(-0.02f, SkyGradient(Color(0xFF3B2855), Color(0xFFA85A5E), Color(0xFFEC8E5A))),
        // Day ──────────────────────────────────────────────────────────
        SkyKeyframe(0.02f,  SkyGradient(Color(0xFF7FAECF), Color(0xFFF0A888), Color(0xFFFCD99A))),
        SkyKeyframe(0.10f,  SkyGradient(Color(0xFF5FA4D8), Color(0xFF9CC9ED), Color(0xFFCBE4F1))),
        SkyKeyframe(0.30f,  SkyGradient(Color(0xFF3E84D2), Color(0xFF79BEE6), Color(0xFFBCE2F3))),
        SkyKeyframe(0.50f,  SkyGradient(Color(0xFF3478C8), Color(0xFF74BDEC), Color(0xFFBBE0F5))),
        SkyKeyframe(0.70f,  SkyGradient(Color(0xFF4385C8), Color(0xFF80C0E0), Color(0xFFCDD0D8))),
        SkyKeyframe(0.85f,  SkyGradient(Color(0xFF5B6A9C), Color(0xFFC08570), Color(0xFFF5B878))),
        SkyKeyframe(0.93f,  SkyGradient(Color(0xFF5E3461), Color(0xFFED7651), Color(0xFFFAB86B))),
        SkyKeyframe(0.98f,  SkyGradient(Color(0xFF48214E), Color(0xFFCF5050), Color(0xFFF06A3A))),
        // Post-sunset ──────────────────────────────────────────────────
        SkyKeyframe(1.02f,  SkyGradient(Color(0xFF1F0F30), Color(0xFF7A2A48), Color(0xFFB03C30))),
        SkyKeyframe(1.06f,  SkyGradient(Color(0xFF0D1130), Color(0xFF2C2147), Color(0xFF5B366A))),
        SkyKeyframe(1.15f,  SkyGradient(Color(0xFF06091E), Color(0xFF10122E), Color(0xFF1A1838))),
        SkyKeyframe(1.40f,  SkyGradient(Color(0xFF02030F), Color(0xFF070A22), Color(0xFF01020A))),
    )

    /**
     * Map a clock time to the day-cycle fraction. With sunrise at 06:00 and
     * sunset at 20:00 (14h day), midnight maps to t ≈ -0.43 and 23:00 to
     * t ≈ 1.07 — both correctly land in the "deep night" / "dusk" zones.
     *
     * The computation deliberately uses day length on both sides of the
     * cycle. Real night length differs from day length, but the keyframe
     * table was built around symmetric `t` so this is the right model.
     */
    fun computeT(now: LocalTime, sunrise: LocalTime, sunset: LocalTime): Float {
        val nowMin = now.toSecondOfDay() / 60f
        val sunriseMin = sunrise.toSecondOfDay() / 60f
        val sunsetMin = sunset.toSecondOfDay() / 60f
        val dayLength = (sunsetMin - sunriseMin).coerceAtLeast(60f) // avoid /0
        return (nowMin - sunriseMin) / dayLength
    }

    fun gradientForT(t: Float): SkyGradient {
        val first = KEYFRAMES.first()
        val last = KEYFRAMES.last()
        if (t <= first.t) return first.gradient
        if (t >= last.t) return last.gradient
        // Find the two keyframes bracketing `t` and lerp.
        val idx = KEYFRAMES.indexOfLast { it.t <= t }.coerceAtMost(KEYFRAMES.size - 2)
        val a = KEYFRAMES[idx]
        val b = KEYFRAMES[idx + 1]
        val span = b.t - a.t
        val frac = if (span <= 0f) 0f else ((t - a.t) / span).coerceIn(0f, 1f)
        return SkyGradient(
            top = lerp(a.gradient.top, b.gradient.top, frac),
            mid = lerp(a.gradient.mid, b.gradient.mid, frac),
            bot = lerp(a.gradient.bot, b.gradient.bot, frac),
        )
    }

    /** Convenience: t in roughly daylight territory (between civil dawn and civil dusk). */
    fun isDaytime(t: Float): Boolean = t in -0.02f..1.02f

    /** Convenience: t in deep-night territory (no horizon glow at all). */
    fun isDeepNight(t: Float): Boolean = t < -0.08f || t > 1.15f

    private fun lerp(a: Color, b: Color, t: Float): Color = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )
}

/**
 * Storm and tornado overrides. These bypass the time-of-day table when
 * weather conditions are active. The returned gradient is meant to be the
 * base; callers stack a lightning flash overlay (storm + night) or a
 * funnel-cloud animation (tornado) on top.
 */
object WeatherSkyOverrides {

    /** Gray-blue ominous overcast storm sky. Used for weather codes 95-97. */
    val STORM_DAY = SkyGradient(
        top = Color(0xFF1B2735),
        mid = Color(0xFF3A4D63),
        bot = Color(0xFF5C6F82),
    )

    /** Near-black night storm. Lightning overlay does the visual work. */
    val STORM_NIGHT = SkyGradient(
        top = Color(0xFF03050A),
        mid = Color(0xFF080C18),
        bot = Color(0xFF0D1424),
    )

    /**
     * Classic tornado sky — sickly yellow-green at the horizon under a dark
     * olive ceiling. The pattern ANY plains-state resident recognizes as
     * "go inside now."
     */
    val TORNADO_SKY = SkyGradient(
        top = Color(0xFF1A1F12),
        mid = Color(0xFF4A5320),
        bot = Color(0xFFB39A40),
    )

    /**
     * Open-Meteo WMO codes that mean active thunderstorms.
     * 95 = thunderstorm (slight or moderate)
     * 96 = thunderstorm with slight hail
     * 99 = thunderstorm with heavy hail
     */
    fun isStorm(weatherCode: Int?): Boolean = weatherCode in 95..99
}
