package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * F13: Public holiday API — Nager.Date (free, no key required).
 * Endpoint: https://date.nager.at/api/v3/PublicHolidays/{year}/{countryCode}
 */
interface HolidayApi {
    @GET("v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String
    ): List<HolidayDto>
}

@JsonClass(generateAdapter = true)
data class HolidayDto(
    @Json(name = "date") val date: String,          // ISO 8601: "2026-01-01"
    @Json(name = "localName") val localName: String,
    @Json(name = "name") val name: String,
    @Json(name = "countryCode") val countryCode: String
)
