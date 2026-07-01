package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import java.time.LocalDate

data class CommuteAlarmAdjustment(
    val totalLeadMinutes: Int,
    val routeExtraMinutes: Int,
    val weatherExtraMinutes: Int
)

object CommuteAlarmPolicy {
    private const val MAX_EXTRA_MINUTES = 120
    private const val HEAVY_PRECIPITATION_THRESHOLD = 60
    private val RAIN_AND_STORM_CODES = setOf(61, 63, 65, 80, 81, 82, 95, 96, 99)

    fun adjustLeadMinutes(
        baseLeadMinutes: Int,
        routeDurationMinutes: Int?,
        baselineCommuteMinutes: Int,
        weatherExtraMinutes: Int,
        forecastDate: LocalDate,
        weather: WeatherResponse?
    ): CommuteAlarmAdjustment {
        val baseLead = baseLeadMinutes.coerceIn(0, 720)
        val baseline = baselineCommuteMinutes.coerceIn(0, 240).takeIf { it > 0 } ?: baseLead
        val routeExtra = routeDurationMinutes
            ?.coerceAtLeast(0)
            ?.minus(baseline)
            ?.coerceAtLeast(0)
            ?: 0
        val weatherExtra = if (weatherExtraMinutes > 0 && isDegradingWeather(weather, forecastDate)) {
            weatherExtraMinutes.coerceIn(0, 120)
        } else {
            0
        }
        val cappedExtra = (routeExtra + weatherExtra).coerceAtMost(MAX_EXTRA_MINUTES)
        return CommuteAlarmAdjustment(
            totalLeadMinutes = baseLead + cappedExtra,
            routeExtraMinutes = routeExtra.coerceAtMost(MAX_EXTRA_MINUTES),
            weatherExtraMinutes = weatherExtra.coerceAtMost(MAX_EXTRA_MINUTES)
        )
    }

    fun isDegradingWeather(weather: WeatherResponse?, forecastDate: LocalDate): Boolean {
        val daily = weather?.daily ?: return false
        val dayIndex = daily.time?.indexOfFirst { it == forecastDate.toString() } ?: -1
        if (dayIndex < 0) return false
        val code = daily.weatherCode?.getOrNull(dayIndex)
        val precipChance = daily.precipChance?.getOrNull(dayIndex) ?: 0
        return code != null && (AlarmScheduler.isSnowOrIceCode(code) || code in RAIN_AND_STORM_CODES) ||
            precipChance >= HEAVY_PRECIPITATION_THRESHOLD
    }
}
