package com.sysadmindoc.alarmclock.ui.worldclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WorldClockEntry(
    val zoneId: String,
    val cityName: String,
    val time: String = "",
    val date: String = "",
    val offsetLabel: String = "",
    val isAhead: Boolean = true
)

data class WorldClockUiState(
    val clocks: List<WorldClockEntry> = emptyList(),
    val localTime: String = "",
    val localZone: String = "",
    val showAddDialog: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<WorldClockEntry> = emptyList()
)

@HiltViewModel
class WorldClockViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var is24Hour = false

    private val _uiState = MutableStateFlow(WorldClockUiState())
    val uiState: StateFlow<WorldClockUiState> = _uiState.asStateFlow()

    private val savedZones = mutableListOf(
        "America/New_York",
        "America/Los_Angeles",
        "Europe/London",
        "Asia/Tokyo"
    )

    private val allZones: List<Pair<String, String>> by lazy {
        ZoneId.getAvailableZoneIds()
            .filter { it.contains("/") && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .sorted()
            .map { zoneId ->
                val city = zoneId.substringAfterLast("/").replace("_", " ")
                zoneId to city
            }
    }

    init {
        viewModelScope.launch {
            preferencesManager.settings.collectLatest { settings ->
                is24Hour = settings.is24HourFormat
            }
        }
        viewModelScope.launch {
            while (isActive) {
                updateTimes()
                delay(1000)
            }
        }
    }

    private fun updateTimes() {
        val now = ZonedDateTime.now()
        val localZone = ZoneId.systemDefault()
        val localOffset = now.offset.totalSeconds

        val entries = savedZones.map { zoneId ->
            val zone = ZoneId.of(zoneId)
            val zdt = now.withZoneSameInstant(zone)
            val offset = zdt.offset.totalSeconds
            val diffHours = (offset - localOffset) / 3600.0
            val diffLabel = when {
                diffHours == 0.0 -> "Same time"
                diffHours > 0 -> "${formatDiff(diffHours)}h ahead"
                else -> "${formatDiff(diffHours)}h behind"
            }
            WorldClockEntry(
                zoneId = zoneId,
                cityName = zoneId.substringAfterLast("/").replace("_", " "),
                time = zdt.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")),
                date = zdt.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                offsetLabel = diffLabel,
                isAhead = diffHours >= 0
            )
        }

        _uiState.value = _uiState.value.copy(
            clocks = entries,
            localTime = now.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm:ss" else "h:mm:ss a")),
            localZone = localZone.id.substringAfterLast("/").replace("_", " ")
        )
    }

    private fun formatDiff(hours: Double): String {
        val abs = kotlin.math.abs(hours)
        return if (abs == abs.toLong().toDouble()) "${abs.toLong()}" else String.format("%.1f", abs)
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, searchQuery = "", searchResults = emptyList())
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, searchQuery = "", searchResults = emptyList())
    }

    fun searchZones(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        val results = allZones
            .filter { (zoneId, city) ->
                city.contains(query, ignoreCase = true) ||
                zoneId.contains(query, ignoreCase = true)
            }
            .filter { (zoneId, _) -> zoneId !in savedZones }
            .take(20)
            .map { (zoneId, city) -> WorldClockEntry(zoneId = zoneId, cityName = city) }
        _uiState.value = _uiState.value.copy(searchResults = results)
    }

    fun addZone(zoneId: String) {
        if (zoneId !in savedZones) {
            savedZones.add(zoneId)
            updateTimes()
        }
        hideAddDialog()
    }

    fun removeZone(zoneId: String) {
        savedZones.remove(zoneId)
        updateTimes()
    }
}
