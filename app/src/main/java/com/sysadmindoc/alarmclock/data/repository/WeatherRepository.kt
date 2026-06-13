package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.remote.AirQualityApi
import com.sysadmindoc.alarmclock.data.remote.AirQualityResponse
import com.sysadmindoc.alarmclock.data.remote.WeatherApi
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val airQualityApi: AirQualityApi
) {
    @Volatile
    private var lastWeather: CachedWeather? = null

    data class CachedWeather(
        val response: WeatherResponse,
        val fetchedAtMillis: Long
    )

    fun getCachedWeather(): WeatherResponse? {
        val cached = lastWeather ?: return null
        val ageMs = System.currentTimeMillis() - cached.fetchedAtMillis
        if (ageMs > 3_600_000) return null
        return cached.response
    }

    suspend fun getWeather(latitude: Double, longitude: Double, tempUnit: String = "fahrenheit", windUnit: String = "mph"): Result<WeatherResponse> {
        return try {
            val response = api.getForecast(latitude, longitude, tempUnit = tempUnit, windUnit = windUnit)
            lastWeather = CachedWeather(response, System.currentTimeMillis())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAirQuality(latitude: Double, longitude: Double): Result<AirQualityResponse> {
        return try {
            Result.success(airQualityApi.getCurrentAirQuality(latitude, longitude))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
