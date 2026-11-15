package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Radio Browser (radio-browser.info): a free, community-run station
 * directory with no key. Its data is public domain.
 *
 * `is_https=true` is asked for on the server because cleartext is blocked at
 * this targetSdk, and an http station would only ever ring the fallback tone.
 */
interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = SEARCH_LIMIT,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("is_https") httpsOnly: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioStationDto>

    /** The directory asks apps to report a play so its popularity order stays useful. */
    @GET("json/url/{stationUuid}")
    suspend fun countClick(@Path("stationUuid") stationUuid: String): Response<ResponseBody>

    companion object {
        const val SEARCH_LIMIT = 40

        /** Tried in order. `all` is DNS round robin across whatever servers are live. */
        val MIRRORS = listOf(
            "https://de1.api.radio-browser.info/",
            "https://de2.api.radio-browser.info/",
            "https://all.api.radio-browser.info/"
        )
    }
}

// Every field is nullable: the directory sends null for fields a station's
// submitter left out, and one null in a non-null field fails the whole list.
@JsonClass(generateAdapter = true)
data class RadioStationDto(
    @Json(name = "stationuuid") val stationUuid: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "url_resolved") val urlResolved: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "countrycode") val countryCode: String? = null,
    @Json(name = "tags") val tags: String? = null,
    @Json(name = "codec") val codec: String? = null,
    @Json(name = "bitrate") val bitrate: Int? = null,
    @Json(name = "hls") val hls: Int? = null,
    @Json(name = "lastcheckok") val lastCheckOk: Int? = null,
    @Json(name = "clickcount") val clickCount: Int? = null
)
