package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.remote.WeatherAlertsApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Distilled alert flags for the current location. The repository deliberately
 * doesn't return the raw NWS response — the rest of the app only needs to
 * know "is anything actionable happening right now", not the full headlines.
 */
data class WeatherAlertFlags(
    val tornadoActive: Boolean = false,
    val severeStorm: Boolean = false,
    val headline: String? = null,
)

/**
 * Thin wrapper over [WeatherAlertsApi]. Returns [WeatherAlertFlags] with
 * everything off when the call fails or returns empty (the typical case
 * outside the US). Failure cases are intentionally absorbed — alerts are
 * a *bonus* signal, never a critical path. The user shouldn't see an
 * error if the NWS endpoint times out.
 */
@Singleton
class WeatherAlertsRepository @Inject constructor(
    private val api: WeatherAlertsApi,
) {

    suspend fun fetch(latitude: Double, longitude: Double): WeatherAlertFlags = runCatching {
        // NWS expects "lat,lon" with 4-decimal precision. Higher precision
        // is silently truncated by the API but adds nothing useful.
        val point = "%.4f,%.4f".format(latitude, longitude)
        val response = api.activeAlerts(point = point)
        val tornado = response.features.any { feature ->
            val event = feature.properties.event?.lowercase().orEmpty()
            event.contains("tornado") &&
                (event.contains("warning") || event.contains("watch"))
        }
        val severe = response.features.any { feature ->
            val event = feature.properties.event?.lowercase().orEmpty()
            event.contains("severe thunderstorm") &&
                (event.contains("warning") || event.contains("watch"))
        }
        val headline = response.features.firstOrNull()?.properties?.headline
        WeatherAlertFlags(
            tornadoActive = tornado,
            severeStorm = severe,
            headline = if (tornado || severe) headline else null,
        )
    }.getOrDefault(WeatherAlertFlags())
}
