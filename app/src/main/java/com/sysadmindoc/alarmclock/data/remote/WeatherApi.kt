package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Weather API - free, no API key required, AGPL-3.0 licensed.
 * F-Droid compatible (no NonFreeNet anti-feature).
 * https://open-meteo.com/en/docs
 */
interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,apparent_temperature",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max",
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windUnit: String = "mph",
        @Query("forecast_days") days: Int = 3,
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val current: CurrentWeather?,
    val daily: DailyWeather?,
    @Json(name = "current_units") val currentUnits: CurrentUnits?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "relative_humidity_2m") val humidity: Int?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?,
    @Json(name = "apparent_temperature") val feelsLike: Double?
)

@JsonClass(generateAdapter = true)
data class CurrentUnits(
    @Json(name = "temperature_2m") val temperature: String?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>?,
    @Json(name = "temperature_2m_max") val maxTemp: List<Double>?,
    @Json(name = "temperature_2m_min") val minTemp: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "precipitation_probability_max") val precipChance: List<Int>?
)

/**
 * WMO Weather interpretation codes mapped to descriptions and icons.
 * https://open-meteo.com/en/docs
 */
object WeatherCodes {
    fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun icon(code: Int): String = when (code) {
        0 -> "clear"
        1, 2 -> "partly_cloudy"
        3 -> "cloudy"
        45, 48 -> "fog"
        51, 53, 55, 56, 57 -> "drizzle"
        61, 63, 65, 66, 67 -> "rain"
        71, 73, 75, 77, 85, 86 -> "snow"
        80, 81, 82 -> "showers"
        95, 96, 99 -> "thunderstorm"
        else -> "unknown"
    }
}
