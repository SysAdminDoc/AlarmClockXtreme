package com.sysadmindoc.alarmclock.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.remote.CurrentWeather
import com.sysadmindoc.alarmclock.data.remote.DailyWeather
import com.sysadmindoc.alarmclock.data.remote.GeocodingApi
import com.sysadmindoc.alarmclock.data.remote.GeocodingResult
import com.sysadmindoc.alarmclock.data.remote.WeatherCodes
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import com.sysadmindoc.alarmclock.data.repository.CalendarRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import javax.inject.Inject

data class DashboardUiState(
    val todayDate: String = "",
    val showWeather: Boolean = true,
    val showCalendar: Boolean = true,
    // Weather
    val weatherLoading: Boolean = false,
    val temperature: String = "",
    val feelsLike: String = "",
    val humidity: String = "",
    val windSpeed: String = "",
    val weatherDescription: String = "",
    val weatherIcon: String = "",
    val highTemp: String = "",
    val lowTemp: String = "",
    val precipChance: String = "",
    val weatherError: String? = null,
    val hasLocation: Boolean = false,
    val locationName: String = "",
    val tempUnit: String = "F", // "F" or "C"
    val windUnit: String = "mph", // "mph" or "km/h"
    // Calendar
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val calendarError: String? = null,
    val calendarPermissionNeeded: Boolean = false,
    // Forecast
    val forecast: List<ForecastDay> = emptyList(),
    // Location search
    val showLocationPicker: Boolean = false,
    val locationSearchResults: List<GeocodingResult> = emptyList(),
    val locationSearching: Boolean = false
)

data class ForecastDay(
    val date: String,
    val dayName: String,
    val high: String,
    val low: String,
    val description: String,
    val precipChance: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val weatherRepository: WeatherRepository,
    private val calendarRepository: CalendarRepository,
    private val preferencesManager: PreferencesManager,
    private val alarmScheduler: AlarmScheduler,
    private val geocodingApi: GeocodingApi
) : AndroidViewModel(application) {

    companion object {
        private const val SOLAR_RESCHEDULE_LOCATION_DELTA = 0.1
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        _uiState.update { it.copy(
            todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        ) }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            _uiState.update { it.copy(
                showWeather = settings.showWeatherOnDashboard,
                showCalendar = settings.showCalendarOnDashboard
            ) }

            if (settings.showWeatherOnDashboard) {
                loadWeather()
            } else {
                _uiState.update { it.copy(
                    weatherLoading = false,
                    weatherError = null,
                    forecast = emptyList()
                ) }
            }

            if (settings.showCalendarOnDashboard) {
                loadCalendar()
            } else {
                _uiState.update { it.copy(
                    calendarEvents = emptyList(),
                    calendarError = null,
                    calendarPermissionNeeded = false
                ) }
            }
        }
    }

    fun loadWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(weatherLoading = true, weatherError = null) }

            val settings = preferencesManager.getCurrentSettings()
            val isCelsius = settings.temperatureUnit == "celsius"
            val tempUnitLabel = if (isCelsius) "C" else "F"
            val windUnitLabel = if (isCelsius) "km/h" else "mph"
            val apiTempUnit = if (isCelsius) "celsius" else "fahrenheit"
            val apiWindUnit = if (isCelsius) "kmh" else "mph"

            // Check for manual location first
            val lat: Double
            val lon: Double
            val locName: String

            if (settings.useManualLocation && settings.locationName.isNotBlank()) {
                lat = settings.lastKnownLatitude
                lon = settings.lastKnownLongitude
                locName = settings.locationName
            } else {
                // Try GPS
                val context = getApplication<Application>()
                val location = LocationHelper.getLastKnownLocation(context)

                if (location == null) {
                    _uiState.update { it.copy(
                        weatherLoading = false,
                        hasLocation = false,
                        locationName = "",
                        temperature = "",
                        feelsLike = "",
                        humidity = "",
                        windSpeed = "",
                        weatherDescription = "",
                        weatherIcon = "",
                        highTemp = "",
                        lowTemp = "",
                        precipChance = "",
                        forecast = emptyList(),
                        weatherError = "Tap the location icon to set your city"
                    ) }
                    return@launch
                }
                lat = location.latitude
                lon = location.longitude
                locName = "Current Location"

                val shouldRescheduleSolarAlarms = shouldRescheduleSolarAlarms(
                    previous = settings,
                    newLatitude = lat,
                    newLongitude = lon,
                    newLocationName = "",
                    useManualLocation = false
                )
                preferencesManager.update {
                    it.copy(lastKnownLatitude = lat, lastKnownLongitude = lon)
                }
                if (shouldRescheduleSolarAlarms) {
                    alarmScheduler.rescheduleAll(forceRecalculate = true)
                }
            }

            weatherRepository.getWeather(lat, lon, apiTempUnit, apiWindUnit)
                .onSuccess { response ->
                    val current = response.current
                    val daily = response.daily

                    _uiState.update { it.copy(
                        weatherLoading = false,
                        hasLocation = true,
                        locationName = locName,
                        tempUnit = tempUnitLabel,
                        windUnit = windUnitLabel,
                        temperature = current?.temperature?.let { "${it.toInt()}" } ?: "--",
                        feelsLike = current?.feelsLike?.let { "Feels like ${it.toInt()}" } ?: "",
                        humidity = current?.humidity?.let { "${it}%" } ?: "",
                        windSpeed = current?.windSpeed?.let { "${it.toInt()} $windUnitLabel" } ?: "",
                        weatherDescription = current?.weatherCode?.let { WeatherCodes.describe(it) } ?: "",
                        weatherIcon = current?.weatherCode?.let { WeatherCodes.icon(it) } ?: "unknown",
                        highTemp = daily?.maxTemp?.firstOrNull()?.let { "${it.toInt()}" } ?: "--",
                        lowTemp = daily?.minTemp?.firstOrNull()?.let { "${it.toInt()}" } ?: "--",
                        precipChance = daily?.precipChance?.firstOrNull()?.let { "${it}%" } ?: "",
                        forecast = buildForecast(daily)
                    ) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        weatherLoading = false,
                        hasLocation = locName.isNotBlank(),
                        locationName = locName,
                        temperature = "",
                        feelsLike = "",
                        humidity = "",
                        windSpeed = "",
                        weatherDescription = "",
                        weatherIcon = "",
                        highTemp = "",
                        lowTemp = "",
                        precipChance = "",
                        forecast = emptyList(),
                        weatherError = "Weather unavailable"
                    ) }
                }
        }
    }

    fun showLocationPicker() {
        _uiState.update { it.copy(showLocationPicker = true, locationSearchResults = emptyList()) }
    }

    fun hideLocationPicker() {
        _uiState.update { it.copy(showLocationPicker = false, locationSearchResults = emptyList()) }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchLocation(query: String) {
        if (query.length < 2) {
            searchJob?.cancel()
            _uiState.update { it.copy(locationSearchResults = emptyList(), locationSearching = false) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(locationSearching = true) }
            kotlinx.coroutines.delay(300) // Debounce 300ms
            try {
                val response = geocodingApi.search(query)
                _uiState.update { it.copy(
                    locationSearchResults = response.results ?: emptyList(),
                    locationSearching = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    locationSearchResults = emptyList(),
                    locationSearching = false
                ) }
            }
        }
    }

    fun selectLocation(result: GeocodingResult) {
        viewModelScope.launch {
            val lat = result.latitude ?: return@launch
            val lon = result.longitude ?: return@launch
            val settings = preferencesManager.getCurrentSettings()
            val displayName = result.displayName
            val shouldRescheduleSolarAlarms = shouldRescheduleSolarAlarms(
                previous = settings,
                newLatitude = lat,
                newLongitude = lon,
                newLocationName = displayName,
                useManualLocation = true
            )
            preferencesManager.update {
                it.copy(
                    lastKnownLatitude = lat,
                    lastKnownLongitude = lon,
                    locationName = displayName,
                    useManualLocation = true
                )
            }
            if (shouldRescheduleSolarAlarms) {
                alarmScheduler.rescheduleAll(forceRecalculate = true)
            }
            _uiState.update { it.copy(
                showLocationPicker = false,
                locationSearchResults = emptyList()
            ) }
            loadWeather()
        }
    }

    fun useDeviceLocation() {
        viewModelScope.launch {
            preferencesManager.update {
                it.copy(useManualLocation = false, locationName = "")
            }
            _uiState.update { it.copy(
                showLocationPicker = false,
                locationSearchResults = emptyList()
            ) }
            loadWeather()
        }
    }

    fun loadCalendar() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = calendarRepository.getTodayEvents()
            result.onSuccess { events ->
                _uiState.update { it.copy(
                    calendarEvents = events,
                    calendarError = null,
                    calendarPermissionNeeded = false
                ) }
            }.onFailure { e ->
                if (e is SecurityException) {
                    _uiState.update { it.copy(
                        calendarEvents = emptyList(),
                        calendarPermissionNeeded = true,
                        calendarError = "Calendar permission needed"
                    ) }
                } else {
                    _uiState.update { it.copy(
                        calendarEvents = emptyList(),
                        calendarError = "Unable to load calendar"
                    ) }
                }
            }
        }
    }

    private fun buildForecast(daily: DailyWeather?): List<ForecastDay> {
        if (daily == null) return emptyList()
        val dates = daily.time ?: return emptyList()

        // Tolerate malformed entries: a single bad date string from the upstream
        // weather API should not crash the entire dashboard, so each row is
        // built independently and skipped on parse failure.
        return dates.mapIndexedNotNull { i, dateStr ->
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapIndexedNotNull null
            ForecastDay(
                date = dateStr,
                dayName = if (i == 0) "Today" else date.format(DateTimeFormatter.ofPattern("EEE")),
                high = daily.maxTemp?.getOrNull(i)?.let { "${it.toInt()}" } ?: "--",
                low = daily.minTemp?.getOrNull(i)?.let { "${it.toInt()}" } ?: "--",
                description = daily.weatherCode?.getOrNull(i)?.let { WeatherCodes.describe(it) } ?: "",
                precipChance = daily.precipChance?.getOrNull(i)?.let { "${it}%" } ?: ""
            )
        }
    }

    private fun shouldRescheduleSolarAlarms(
        previous: com.sysadmindoc.alarmclock.data.preferences.AppSettings,
        newLatitude: Double,
        newLongitude: Double,
        newLocationName: String,
        useManualLocation: Boolean
    ): Boolean {
        if (previous.useManualLocation != useManualLocation) return true
        if (useManualLocation && previous.locationName != newLocationName) return true
        return abs(previous.lastKnownLatitude - newLatitude) >= SOLAR_RESCHEDULE_LOCATION_DELTA ||
            abs(previous.lastKnownLongitude - newLongitude) >= SOLAR_RESCHEDULE_LOCATION_DELTA
    }
}
