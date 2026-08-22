package com.sysadmindoc.alarmclock.data.remote

import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.sysadmindoc.alarmclock.R
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
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,apparent_temperature,uv_index",
        // v1.7.4: hourly added so the dashboard can show "what's the weather
        // when my alarm rings" — wired up to the 12-hour strip ported from
        // ZeusWatch's HourlyForecastStrip.
        @Query("hourly") hourly: String =
            "temperature_2m,weather_code,precipitation_probability",
        // v1.7.4: sunrise / sunset / uv_index_max ported from ZeusWatch's
        // daily card. Sunrise/sunset are particularly relevant in an alarm
        // app — they answer "is the sun up by my alarm time?"
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,weather_code," +
                "precipitation_probability_max,sunrise,sunset,uv_index_max",
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windUnit: String = "mph",
        @Query("forecast_days") days: Int = 3,
        @Query("forecast_hours") forecastHours: Int = 12,
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?,
    @Json(name = "current_units") val currentUnits: CurrentUnits?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "relative_humidity_2m") val humidity: Int?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?,
    @Json(name = "apparent_temperature") val feelsLike: Double?,
    @Json(name = "uv_index") val uvIndex: Double?
)

@JsonClass(generateAdapter = true)
data class CurrentUnits(
    @Json(name = "temperature_2m") val temperature: String?
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>?,
    @Json(name = "temperature_2m") val temperature: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "precipitation_probability") val precipChance: List<Int>?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>?,
    @Json(name = "temperature_2m_max") val maxTemp: List<Double>?,
    @Json(name = "temperature_2m_min") val minTemp: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "precipitation_probability_max") val precipChance: List<Int>?,
    val sunrise: List<String>?,
    val sunset: List<String>?,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>?
)

/**
 * WMO Weather interpretation codes mapped to descriptions and icons.
 * https://open-meteo.com/en/docs
 */
object WeatherCodes {
    /** The WMO code's description, as a resource for the caller to resolve. */
    @StringRes
    fun describeRes(code: Int): Int = when (code) {
        0 -> R.string.weather_clear_sky
        1 -> R.string.weather_mainly_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_overcast
        45, 48 -> R.string.weather_foggy
        51, 53, 55 -> R.string.weather_drizzle
        56, 57 -> R.string.weather_freezing_drizzle
        61, 63, 65 -> R.string.weather_rain
        66, 67 -> R.string.weather_freezing_rain
        71, 73, 75 -> R.string.weather_snow
        77 -> R.string.weather_snow_grains
        80, 81, 82 -> R.string.weather_rain_showers
        85, 86 -> R.string.weather_snow_showers
        95 -> R.string.weather_thunderstorm
        96, 99 -> R.string.weather_thunderstorm_hail
        else -> R.string.weather_unknown
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
