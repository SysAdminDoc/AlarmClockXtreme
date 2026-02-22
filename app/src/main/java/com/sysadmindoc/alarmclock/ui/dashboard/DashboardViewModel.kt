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
import com.sysadmindoc.alarmclock.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardUiState(
    val todayDate: String = "",
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
    private val geocodingApi: GeocodingApi
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        _uiState.value = _uiState.value.copy(
            todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        )
        loadData()
    }

    fun loadData() {
        loadWeather()
        loadCalendar()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(weatherLoading = true, weatherError = null)

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

            if (settings.useManualLocation && settings.lastKnownLatitude != 0.0) {
                lat = settings.lastKnownLatitude
                lon = settings.lastKnownLongitude
                locName = settings.locationName
            } else {
                // Try GPS
                val context = getApplication<Application>()
                val location = LocationHelper.getLastKnownLocation(context)

                if (location == null) {
                    _uiState.value = _uiState.value.copy(
                        weatherLoading = false,
                        hasLocation = false,
                        weatherError = "Tap the location icon to set your city"
                    )
                    return@launch
                }
                lat = location.latitude
                lon = location.longitude
                locName = "Current Location"

                preferencesManager.update {
                    it.copy(lastKnownLatitude = lat, lastKnownLongitude = lon)
                }
            }

            weatherRepository.getWeather(lat, lon, apiTempUnit, apiWindUnit)
                .onSuccess { response ->
                    val current = response.current
                    val daily = response.daily

                    _uiState.value = _uiState.value.copy(
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
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        weatherLoading = false,
                        weatherError = "Weather unavailable"
                    )
                }
        }
    }

    fun showLocationPicker() {
        _uiState.value = _uiState.value.copy(showLocationPicker = true, locationSearchResults = emptyList())
    }

    fun hideLocationPicker() {
        _uiState.value = _uiState.value.copy(showLocationPicker = false, locationSearchResults = emptyList())
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchLocation(query: String) {
        if (query.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(locationSearching = true)
            kotlinx.coroutines.delay(300) // Debounce 300ms
            try {
                val response = geocodingApi.search(query)
                _uiState.value = _uiState.value.copy(
                    locationSearchResults = response.results ?: emptyList(),
                    locationSearching = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    locationSearchResults = emptyList(),
                    locationSearching = false
                )
            }
        }
    }

    fun selectLocation(result: GeocodingResult) {
        viewModelScope.launch {
            val lat = result.latitude ?: return@launch
            val lon = result.longitude ?: return@launch
            preferencesManager.update {
                it.copy(
                    lastKnownLatitude = lat,
                    lastKnownLongitude = lon,
                    locationName = result.displayName,
                    useManualLocation = true
                )
            }
            _uiState.value = _uiState.value.copy(
                showLocationPicker = false,
                locationSearchResults = emptyList()
            )
            loadWeather()
        }
    }

    fun useDeviceLocation() {
        viewModelScope.launch {
            preferencesManager.update {
                it.copy(useManualLocation = false, locationName = "")
            }
            _uiState.value = _uiState.value.copy(
                showLocationPicker = false,
                locationSearchResults = emptyList()
            )
            loadWeather()
        }
    }

    fun loadCalendar() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = calendarRepository.getTodayEvents()
            result.onSuccess { events ->
                _uiState.value = _uiState.value.copy(
                    calendarEvents = events,
                    calendarError = null,
                    calendarPermissionNeeded = false
                )
            }.onFailure { e ->
                if (e is SecurityException) {
                    _uiState.value = _uiState.value.copy(
                        calendarPermissionNeeded = true,
                        calendarError = "Calendar permission needed"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        calendarError = "Unable to load calendar"
                    )
                }
            }
        }
    }

    private fun buildForecast(daily: DailyWeather?): List<ForecastDay> {
        if (daily == null) return emptyList()
        val dates = daily.time ?: return emptyList()

        return dates.mapIndexed { i, dateStr ->
            val date = LocalDate.parse(dateStr)
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
}
