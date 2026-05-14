package com.sysadmindoc.alarmclock.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Air Quality API - free for non-commercial use, no API key required.
 * https://open-meteo.com/en/docs/air-quality-api
 */
interface AirQualityApi {

    @GET("v1/air-quality")
    suspend fun getCurrentAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "us_aqi,pm10,pm2_5,ozone,nitrogen_dioxide,carbon_monoxide," +
                "sulphur_dioxide,alder_pollen,birch_pollen,grass_pollen," +
                "mugwort_pollen,olive_pollen,ragweed_pollen",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): AirQualityResponse
}

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    val current: CurrentAirQuality?,
    @Json(name = "current_units") val currentUnits: AirQualityUnits?
)

@JsonClass(generateAdapter = true)
data class CurrentAirQuality(
    val time: String?,
    @Json(name = "us_aqi") val usAqi: Int?,
    val pm10: Double?,
    @Json(name = "pm2_5") val pm25: Double?,
    val ozone: Double?,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double?,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double?,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: Double?,
    @Json(name = "alder_pollen") val alderPollen: Double?,
    @Json(name = "birch_pollen") val birchPollen: Double?,
    @Json(name = "grass_pollen") val grassPollen: Double?,
    @Json(name = "mugwort_pollen") val mugwortPollen: Double?,
    @Json(name = "olive_pollen") val olivePollen: Double?,
    @Json(name = "ragweed_pollen") val ragweedPollen: Double?
)

@JsonClass(generateAdapter = true)
data class AirQualityUnits(
    @Json(name = "us_aqi") val usAqi: String?,
    val pm10: String?,
    @Json(name = "pm2_5") val pm25: String?,
    val ozone: String?,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: String?,
    @Json(name = "carbon_monoxide") val carbonMonoxide: String?,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: String?,
    @Json(name = "alder_pollen") val alderPollen: String?,
    @Json(name = "birch_pollen") val birchPollen: String?,
    @Json(name = "grass_pollen") val grassPollen: String?,
    @Json(name = "mugwort_pollen") val mugwortPollen: String?,
    @Json(name = "olive_pollen") val olivePollen: String?,
    @Json(name = "ragweed_pollen") val ragweedPollen: String?
)
