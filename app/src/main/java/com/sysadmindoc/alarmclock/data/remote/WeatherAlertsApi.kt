package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * National Weather Service active-alerts endpoint. Free, no key, US-only.
 * Returns a GeoJSON `FeatureCollection`; we only model the fields we care
 * about (event name, severity, urgency, headline). Outside the US this
 * endpoint reliably returns an empty `features` array, so it's safe to
 * call unconditionally — non-US users just get no tornado warnings, which
 * is correct because the NWS doesn't issue them.
 *
 * NWS asks for a User-Agent that identifies the app + a contact channel.
 * They use it to throttle abusive clients; failing to send one risks 403.
 *
 * Docs: https://www.weather.gov/documentation/services-web-api
 */
interface WeatherAlertsApi {

    @GET("alerts/active")
    suspend fun activeAlerts(
        @Query("point") point: String,
        @Query("status") status: String = "actual",
        @Query("urgency") urgency: String = "Immediate,Expected",
        @Header("User-Agent") userAgent: String =
            "AlarmClockXtreme (github.com/SysAdminDoc/AlarmClockXtreme)",
        @Header("Accept") accept: String = "application/geo+json",
    ): NwsAlertsResponse
}

@JsonClass(generateAdapter = true)
data class NwsAlertsResponse(
    @Json(name = "features") val features: List<NwsAlertFeature> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class NwsAlertFeature(
    @Json(name = "properties") val properties: NwsAlertProperties,
)

@JsonClass(generateAdapter = true)
data class NwsAlertProperties(
    /** "Tornado Warning", "Tornado Watch", "Severe Thunderstorm Warning", etc. */
    @Json(name = "event") val event: String? = null,
    @Json(name = "severity") val severity: String? = null,
    @Json(name = "urgency") val urgency: String? = null,
    @Json(name = "headline") val headline: String? = null,
)
