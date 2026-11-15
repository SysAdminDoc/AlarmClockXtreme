package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

/**
 * The newest published GitHub release, for the opt-in update check.
 * Anonymous, so it shares GitHub's 60-an-hour limit per IP; a daily
 * check is nowhere near it.
 */
interface ReleaseApi {
    @GET("repos/SysAdminDoc/AlarmClockXtreme/releases/latest")
    suspend fun latest(): ReleaseDto
}

/** Only the tag is read: the release page link is built from it, never taken from the response. */
@JsonClass(generateAdapter = true)
data class ReleaseDto(
    @Json(name = "tag_name") val tagName: String
)
