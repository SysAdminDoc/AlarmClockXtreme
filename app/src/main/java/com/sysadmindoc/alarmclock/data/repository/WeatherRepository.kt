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
    suspend fun getWeather(latitude: Double, longitude: Double, tempUnit: String = "fahrenheit", windUnit: String = "mph"): Result<WeatherResponse> {
        return try {
            val response = api.getForecast(latitude, longitude, tempUnit = tempUnit, windUnit = windUnit)
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
