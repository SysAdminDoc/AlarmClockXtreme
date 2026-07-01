package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.remote.GeocodingApi
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesApi
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesLatLng
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesLocation
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesRequest
import com.sysadmindoc.alarmclock.data.remote.GoogleRoutesWaypoint
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class CommuteRouteRepository @Inject constructor(
    private val geocodingApi: GeocodingApi,
    private val googleRoutesApi: GoogleRoutesApi
) {
    suspend fun estimateTransitMinutes(
        apiKey: String,
        originLatitude: Double,
        originLongitude: Double,
        destinationQuery: String,
        arrivalTime: Instant
    ): Result<Int?> {
        val key = apiKey.trim()
        val query = destinationQuery.trim()
        if (key.isBlank() || query.isBlank() || !originLatitude.isValidLatitude() ||
            !originLongitude.isValidLongitude() || (originLatitude == 0.0 && originLongitude == 0.0)
        ) {
            return Result.success(null)
        }

        return try {
            val destination = geocodingApi.search(query, count = 1)
                .results
                .orEmpty()
                .firstOrNull { it.latitude != null && it.longitude != null }
                ?: return Result.success(null)

            val response = googleRoutesApi.computeRoutes(
                apiKey = key,
                request = GoogleRoutesRequest(
                    origin = waypoint(originLatitude, originLongitude),
                    destination = waypoint(destination.latitude!!, destination.longitude!!),
                    arrivalTime = arrivalTime.toString()
                )
            )
            val seconds = response.routes
                .orEmpty()
                .firstOrNull()
                ?.duration
                ?.parseGoogleDurationSeconds()
            Result.success(seconds?.let { ceil(it / 60.0).toInt().coerceAtLeast(1) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun waypoint(latitude: Double, longitude: Double): GoogleRoutesWaypoint =
        GoogleRoutesWaypoint(
            location = GoogleRoutesLocation(
                latLng = GoogleRoutesLatLng(
                    latitude = latitude,
                    longitude = longitude
                )
            )
        )

    private fun Double.isValidLatitude(): Boolean = this in -90.0..90.0

    private fun Double.isValidLongitude(): Boolean = this in -180.0..180.0

    private fun String.parseGoogleDurationSeconds(): Double? {
        val value = trim()
        if (!value.endsWith("s")) return null
        return value.dropLast(1).toDoubleOrNull()?.takeIf { it > 0.0 }
    }
}
