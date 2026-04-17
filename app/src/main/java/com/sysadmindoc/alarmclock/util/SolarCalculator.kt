package com.sysadmindoc.alarmclock.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * v1.5.0: Solar sunrise / sunset calculator.
 *
 * Uses the general solar-position approximation (NOAA style) — good to
 * within ~1 minute for mid-latitudes, which is adequate for alarm firing.
 * Returns [LocalTime] in the caller's zone or `null` for locations inside
 * polar day / polar night where the sun doesn't rise or set.
 */
object SolarCalculator {

    /**
     * Compute sunrise for [date] at [latitude]/[longitude] in [zone].
     * Returns null in the polar-night case.
     */
    fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): LocalTime? =
        solarEvent(date, latitude, longitude, zone, riseNotSet = true)

    /** Sunset — see [sunrise]. Returns null in polar-day case. */
    fun sunset(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): LocalTime? =
        solarEvent(date, latitude, longitude, zone, riseNotSet = false)

    private fun solarEvent(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        riseNotSet: Boolean
    ): LocalTime? {
        // Day of year 1..366. Solar noon as an approximation.
        val n = date.dayOfYear
        val gamma = 2.0 * PI / 365.0 * (n - 1 + (12.0 - 12.0) / 24.0)

        // Solar declination in radians (per NOAA approximation).
        val decl = 0.006918 -
            0.399912 * cos(gamma) +
            0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) +
            0.00148 * sin(3 * gamma)

        // Equation of time (minutes).
        val eqTime = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma)
            )

        // Hour angle for standard sunrise/sunset (sun at 90.833° zenith, which
        // accounts for atmospheric refraction + mean solar disk).
        val latRad = Math.toRadians(latitude)
        val zenithRad = Math.toRadians(90.833)
        val cosHa = (cos(zenithRad) / (cos(latRad) * cos(decl))) - tan(latRad) * tan(decl)

        if (cosHa < -1.0 || cosHa > 1.0) return null // polar day / night

        val haRad = acos(cosHa)
        val haDeg = Math.toDegrees(haRad)

        // Minutes from UTC midnight.
        val solarNoonMinutesUtc = 720.0 - 4.0 * longitude - eqTime
        val eventMinutesUtc = if (riseNotSet) {
            solarNoonMinutesUtc - 4.0 * haDeg
        } else {
            solarNoonMinutesUtc + 4.0 * haDeg
        }

        // Convert UTC minutes to ZonedDateTime at the requested zone.
        val zdtUtc = ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.of("UTC"))
            .plusSeconds((eventMinutesUtc * 60.0).toLong())
        return zdtUtc.withZoneSameInstant(zone).toLocalTime()
    }
}
