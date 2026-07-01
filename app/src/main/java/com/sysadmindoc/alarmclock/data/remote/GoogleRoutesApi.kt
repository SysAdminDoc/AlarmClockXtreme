package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Google Routes API v2 transit endpoint. ACX only calls this when the user
 * supplies their own API key; commute-aware alarms otherwise use the no-key
 * weather fallback.
 */
interface GoogleRoutesApi {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = "routes.duration",
        @Body request: GoogleRoutesRequest
    ): GoogleRoutesResponse
}

@JsonClass(generateAdapter = true)
data class GoogleRoutesRequest(
    val origin: GoogleRoutesWaypoint,
    val destination: GoogleRoutesWaypoint,
    val travelMode: String = "TRANSIT",
    val arrivalTime: String,
    val transitPreferences: GoogleTransitPreferences = GoogleTransitPreferences()
)

@JsonClass(generateAdapter = true)
data class GoogleRoutesWaypoint(
    val location: GoogleRoutesLocation
)

@JsonClass(generateAdapter = true)
data class GoogleRoutesLocation(
    val latLng: GoogleRoutesLatLng
)

@JsonClass(generateAdapter = true)
data class GoogleRoutesLatLng(
    val latitude: Double,
    val longitude: Double
)

@JsonClass(generateAdapter = true)
data class GoogleTransitPreferences(
    val routingPreference: String = "FEWER_TRANSFERS"
)

@JsonClass(generateAdapter = true)
data class GoogleRoutesResponse(
    val routes: List<GoogleRoute>?
)

@JsonClass(generateAdapter = true)
data class GoogleRoute(
    val duration: String?
)
