package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.remote.DailyWeather
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommuteAlarmPolicyTest {
    private val date = LocalDate.parse("2026-07-02")

    @Test
    fun routeDurationAboveBaselineAddsExtraLead() {
        val adjustment = CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = 45,
            routeDurationMinutes = 70,
            baselineCommuteMinutes = 40,
            weatherExtraMinutes = 0,
            forecastDate = date,
            weather = null
        )

        assertEquals(75, adjustment.totalLeadMinutes)
        assertEquals(30, adjustment.routeExtraMinutes)
        assertEquals(0, adjustment.weatherExtraMinutes)
    }

    @Test
    fun weatherFallbackAddsBufferWithoutRoute() {
        val adjustment = CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = 45,
            routeDurationMinutes = null,
            baselineCommuteMinutes = 0,
            weatherExtraMinutes = 20,
            forecastDate = date,
            weather = weather(code = 71, precipChance = 20)
        )

        assertEquals(65, adjustment.totalLeadMinutes)
        assertEquals(0, adjustment.routeExtraMinutes)
        assertEquals(20, adjustment.weatherExtraMinutes)
    }

    @Test
    fun heavyPrecipitationCountsAsDegradingWeather() {
        assertTrue(
            CommuteAlarmPolicy.isDegradingWeather(
                weather = weather(code = 3, precipChance = 80),
                forecastDate = date
            )
        )
        assertFalse(
            CommuteAlarmPolicy.isDegradingWeather(
                weather = weather(code = 1, precipChance = 20),
                forecastDate = date
            )
        )
    }

    @Test
    fun totalExtraLeadIsCapped() {
        val adjustment = CommuteAlarmPolicy.adjustLeadMinutes(
            baseLeadMinutes = 30,
            routeDurationMinutes = 300,
            baselineCommuteMinutes = 30,
            weatherExtraMinutes = 60,
            forecastDate = date,
            weather = weather(code = 95, precipChance = 90)
        )

        assertEquals(150, adjustment.totalLeadMinutes)
    }

    private fun weather(code: Int, precipChance: Int): WeatherResponse = WeatherResponse(
        current = null,
        hourly = null,
        daily = DailyWeather(
            time = listOf(date.toString()),
            maxTemp = null,
            minTemp = null,
            weatherCode = listOf(code),
            precipChance = listOf(precipChance),
            sunrise = null,
            sunset = null,
            uvIndexMax = null
        ),
        currentUnits = null
    )
}
