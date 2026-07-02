package com.sysadmindoc.alarmclock.data.repository

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.data.remote.AirQualityApi
import com.sysadmindoc.alarmclock.data.remote.AirQualityResponse
import com.sysadmindoc.alarmclock.data.remote.WeatherApi
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@JsonClass(generateAdapter = true)
data class WeatherCacheEnvelope(
    val latitude: Double,
    val longitude: Double,
    val tempUnit: String,
    val windUnit: String,
    val fetchedAtMillis: Long,
    val response: WeatherResponse
)

@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: WeatherApi,
    private val airQualityApi: AirQualityApi,
    moshi: Moshi
) {
    @Volatile
    private var lastWeather: CachedWeather? = null

    private val cacheAdapter = moshi.adapter(WeatherCacheEnvelope::class.java).indent("  ")
    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    data class CachedWeather(
        val response: WeatherResponse,
        val fetchedAtMillis: Long
    )

    data class WeatherSnapshot(
        val response: WeatherResponse,
        val fetchedAtMillis: Long,
        val isStale: Boolean,
        val refreshError: Throwable? = null
    )

    fun getCachedWeather(): WeatherResponse? {
        val cached = lastWeather
        if (cached != null && System.currentTimeMillis() - cached.fetchedAtMillis <= FRESH_CACHE_MS) {
            return cached.response
        }
        val disk = readCache(maxAgeMs = FRESH_CACHE_MS) ?: return null
        lastWeather = disk
        return disk.response
    }

    suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        tempUnit: String = "fahrenheit",
        windUnit: String = "mph"
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        try {
            val response = api.getForecast(latitude, longitude, tempUnit = tempUnit, windUnit = windUnit)
            val fetchedAt = System.currentTimeMillis()
            val cached = CachedWeather(response, fetchedAt)
            lastWeather = cached
            writeCache(
                WeatherCacheEnvelope(
                    latitude = latitude,
                    longitude = longitude,
                    tempUnit = tempUnit,
                    windUnit = windUnit,
                    fetchedAtMillis = fetchedAt,
                    response = response
                )
            )
            Result.success(
                WeatherSnapshot(
                    response = response,
                    fetchedAtMillis = fetchedAt,
                    isStale = false
                )
            )
        } catch (e: Exception) {
            val cached = readCache(
                latitude = latitude,
                longitude = longitude,
                tempUnit = tempUnit,
                windUnit = windUnit,
                maxAgeMs = MAX_STALE_CACHE_MS
            )
            if (cached != null) {
                lastWeather = cached
                Result.success(
                    WeatherSnapshot(
                        response = cached.response,
                        fetchedAtMillis = cached.fetchedAtMillis,
                        isStale = true,
                        refreshError = e
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getAirQuality(latitude: Double, longitude: Double): Result<AirQualityResponse> {
        return try {
            Result.success(airQualityApi.getCurrentAirQuality(latitude, longitude))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeCache(envelope: WeatherCacheEnvelope) {
        runCatching {
            cacheFile.writeText(cacheAdapter.toJson(envelope))
        }
    }

    private fun readCache(
        latitude: Double? = null,
        longitude: Double? = null,
        tempUnit: String? = null,
        windUnit: String? = null,
        maxAgeMs: Long
    ): CachedWeather? {
        return runCatching {
            if (!cacheFile.exists()) return null
            val envelope = cacheAdapter.fromJson(cacheFile.readText()) ?: return null
            val ageMs = System.currentTimeMillis() - envelope.fetchedAtMillis
            if (ageMs < 0 || ageMs > maxAgeMs) return null
            if (latitude != null && longitude != null) {
                val sameLocation = abs(envelope.latitude - latitude) <= LOCATION_CACHE_DELTA &&
                    abs(envelope.longitude - longitude) <= LOCATION_CACHE_DELTA
                if (!sameLocation) return null
            }
            if (tempUnit != null && envelope.tempUnit != tempUnit) return null
            if (windUnit != null && envelope.windUnit != windUnit) return null
            CachedWeather(envelope.response, envelope.fetchedAtMillis)
        }.getOrNull()
    }

    companion object {
        private const val CACHE_FILE_NAME = "weather_last_good_v1.json"
        private const val FRESH_CACHE_MS = 60L * 60 * 1000
        private const val MAX_STALE_CACHE_MS = 72L * 60 * 60 * 1000
        private const val LOCATION_CACHE_DELTA = 0.1
    }
}
