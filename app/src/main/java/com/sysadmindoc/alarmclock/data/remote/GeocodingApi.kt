package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Geocoding API - converts city names/zipcodes to lat/lon.
 * https://open-meteo.com/en/docs/geocoding-api
 */
interface GeocodingApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") query: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val country: String?,
    @Json(name = "admin1") val state: String?,
    val timezone: String?
) {
    val displayName: String
        get() = buildString {
            append(name ?: "Unknown")
            if (!state.isNullOrBlank()) append(", $state")
            if (!country.isNullOrBlank()) append(", $country")
        }
}
