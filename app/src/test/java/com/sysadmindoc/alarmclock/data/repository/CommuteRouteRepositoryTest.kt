package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.remote.GeocodingApi
import com.sysadmindoc.alarmclock.data.remote.GeocodingResponse
import com.sysadmindoc.alarmclock.data.remote.GeocodingResult
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesApi
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesRequest
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesResponse
import com.sysadmindoc.alarmclock.data.remote.GoogleRoute
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommuteRouteRepositoryTest {

    @Test
    fun blankApiKeyReturnsNullWithoutNetworkCalls() = runTest {
        val geocodingApi = RecordingGeocodingApi()
        val routesApi = RecordingRoutesApi()
        val repository = CommuteRouteRepository(geocodingApi, routesApi)

        val minutes = repository.estimateTransitMinutes(
            apiKey = "",
            originLatitude = 32.7767,
            originLongitude = -96.7970,
            destinationQuery = "Union Station",
            arrivalTime = Instant.parse("2026-07-02T14:00:00Z")
        ).getOrThrow()

        assertNull(minutes)
        assertEquals(0, geocodingApi.calls)
        assertEquals(0, routesApi.calls)
    }

    @Test
    fun googleDurationRoundsUpToMinutes() = runTest {
        val repository = CommuteRouteRepository(
            geocodingApi = RecordingGeocodingApi(),
            googleRoutesApi = RecordingRoutesApi(duration = "3599s")
        )

        val minutes = repository.estimateTransitMinutes(
            apiKey = "key",
            originLatitude = 32.7767,
            originLongitude = -96.7970,
            destinationQuery = "Union Station",
            arrivalTime = Instant.parse("2026-07-02T14:00:00Z")
        )

        assertTrue(minutes.isSuccess)
        assertEquals(60, minutes.getOrThrow())
    }

    private class RecordingGeocodingApi : GeocodingApi {
        var calls = 0

        override suspend fun search(
            query: String,
            count: Int,
            language: String,
            format: String
        ): GeocodingResponse {
            calls++
            return GeocodingResponse(
                results = listOf(
                    GeocodingResult(
                        id = 1,
                        name = "Union Station",
                        latitude = 32.7767,
                        longitude = -96.8070,
                        country = "United States",
                        state = "Texas",
                        timezone = "America/Chicago"
                    )
                )
            )
        }
    }

    private class RecordingRoutesApi(
        private val duration: String = "1800s"
    ) : GoogleRoutesApi {
        var calls = 0

        override suspend fun computeRoutes(
            apiKey: String,
            fieldMask: String,
            request: GoogleRoutesRequest
        ): GoogleRoutesResponse {
            calls++
            return GoogleRoutesResponse(routes = listOf(GoogleRoute(duration = duration)))
        }
    }
}
