package com.sysadmindoc.alarmclock.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.data.remote.AirQualityApi
import com.sysadmindoc.alarmclock.data.remote.AirQualityResponse
import com.sysadmindoc.alarmclock.data.remote.CurrentUnits
import com.sysadmindoc.alarmclock.data.remote.CurrentWeather
import com.sysadmindoc.alarmclock.data.remote.DailyWeather
import com.sysadmindoc.alarmclock.data.remote.HourlyWeather
import com.sysadmindoc.alarmclock.data.remote.WeatherApi
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import java.io.IOException
import java.util.ArrayDeque
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherRepositoryTest {
    private lateinit var context: Context
    private val moshi = Moshi.Builder().build()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.resolve("weather_last_good_v1.json").delete()
    }

    @Test
    fun getWeatherReturnsStaleDiskCacheWhenRefreshFails() = runTest {
        val freshResponse = weatherResponse(temp = 72.4)
        val writer = repository(QueueWeatherApi(listOf(Result.success(freshResponse))))

        val fresh = writer.getWeather(
            latitude = 45.51,
            longitude = -122.68,
            tempUnit = "fahrenheit",
            windUnit = "mph"
        ).getOrThrow()

        assertFalse(fresh.isStale)
        assertEquals(freshResponse, fresh.response)

        val offline = repository(QueueWeatherApi(listOf(Result.failure(IOException("offline")))))
            .getWeather(
                latitude = 45.51,
                longitude = -122.68,
                tempUnit = "fahrenheit",
                windUnit = "mph"
            )
            .getOrThrow()

        assertTrue(offline.isStale)
        assertEquals(freshResponse, offline.response)
        assertTrue(offline.refreshError is IOException)
    }

    @Test
    fun getWeatherDoesNotServeCacheForDifferentLocation() = runTest {
        repository(QueueWeatherApi(listOf(Result.success(weatherResponse(temp = 68.0)))))
            .getWeather(
                latitude = 45.51,
                longitude = -122.68,
                tempUnit = "fahrenheit",
                windUnit = "mph"
            )
            .getOrThrow()

        val result = repository(QueueWeatherApi(listOf(Result.failure(IOException("offline")))))
            .getWeather(
                latitude = 40.71,
                longitude = -74.00,
                tempUnit = "fahrenheit",
                windUnit = "mph"
            )

        assertTrue(result.isFailure)
    }

    private fun repository(api: WeatherApi): WeatherRepository =
        WeatherRepository(
            context = context,
            api = api,
            airQualityApi = FakeAirQualityApi,
            moshi = moshi
        )

    private fun weatherResponse(temp: Double): WeatherResponse = WeatherResponse(
        current = CurrentWeather(
            temperature = temp,
            humidity = 57,
            weatherCode = 1,
            windSpeed = 8.0,
            feelsLike = temp - 1.0,
            uvIndex = 3.0
        ),
        hourly = HourlyWeather(
            time = listOf("2026-07-02T07:00"),
            temperature = listOf(temp),
            weatherCode = listOf(1),
            precipChance = listOf(10)
        ),
        daily = DailyWeather(
            time = listOf("2026-07-02"),
            maxTemp = listOf(temp + 6),
            minTemp = listOf(temp - 7),
            weatherCode = listOf(1),
            precipChance = listOf(10),
            sunrise = listOf("2026-07-02T05:30"),
            sunset = listOf("2026-07-02T20:50"),
            uvIndexMax = listOf(5.0)
        ),
        currentUnits = CurrentUnits(temperature = "°F")
    )

    private class QueueWeatherApi(
        responses: List<Result<WeatherResponse>>
    ) : WeatherApi {
        private val responses = ArrayDeque(responses)

        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            tempUnit: String,
            windUnit: String,
            days: Int,
            forecastHours: Int,
            timezone: String
        ): WeatherResponse = responses.removeFirst().getOrThrow()
    }

    private object FakeAirQualityApi : AirQualityApi {
        override suspend fun getCurrentAirQuality(
            latitude: Double,
            longitude: Double,
            current: String,
            timezone: String,
            forecastDays: Int
        ): AirQualityResponse = AirQualityResponse(current = null, currentUnits = null)
    }
}
