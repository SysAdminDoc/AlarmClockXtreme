package com.sysadmindoc.alarmclock.ui.worldclock

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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
import java.util.Locale
import javax.inject.Inject

data class WorldClockEntry(
    val zoneId: String,
    val cityName: String,
    val time: String = "",
    val date: String = "",
    val offsetHours: Double = 0.0,
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
    application: Application,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private var is24Hour = false

    // The ticker calls updateTimes() every second; building a DateTimeFormatter
    // per zone per tick (ofPattern re-parses the pattern each call) churned ~9
    // formatter allocations/second forever. Cache them and rebuild only when the
    // 12/24-hour preference actually changes.
    private var cachedIs24Hour: Boolean? = null
    private var localTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a")
    private var zoneTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val zoneDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    private fun refreshFormatters() {
        if (cachedIs24Hour == is24Hour) return
        localTimeFormatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm:ss" else "h:mm:ss a")
        zoneTimeFormatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
        cachedIs24Hour = is24Hour
    }

    private val _uiState = MutableStateFlow(WorldClockUiState())
    val uiState: StateFlow<WorldClockUiState> = _uiState.asStateFlow()

    /**
     * Saved zones are persisted in a tiny SharedPreferences string list so that
     * adding/removing zones survives app restarts. Without this the user-curated
     * world-clock list reset to defaults every cold-start.
     */
    private val prefs = application.getSharedPreferences("world_clock_prefs", Context.MODE_PRIVATE)
    private val savedZones = mutableListOf<String>().apply {
        val stored = prefs.getString("zones", null)
        if (stored.isNullOrBlank()) {
            addAll(DEFAULT_ZONES)
        } else {
            // Filter out any zones the JVM no longer knows about so a stale value
            // can't crash ZoneId.of() during updateTimes().
            val available = ZoneId.getAvailableZoneIds()
            addAll(stored.split('|').filter { it in available })
            if (isEmpty()) addAll(DEFAULT_ZONES)
        }
    }

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
                updateTimes()
            }
        }
        viewModelScope.launch {
            _uiState.subscriptionCount.collectLatest { count ->
                if (count > 0) {
                    while (true) {
                        updateTimes()
                        delay(1000)
                    }
                }
            }
        }
    }

    private fun updateTimes() {
        refreshFormatters()
        val now = ZonedDateTime.now()
        val localZone = ZoneId.systemDefault()

        // Each zone parse is wrapped so a single corrupt persisted entry can't
        // poison the entire world-clock render.
        val entries = savedZones.mapNotNull { zoneId ->
            buildWorldClockEntry(zoneId = zoneId, now = now)
        }

        _uiState.value = _uiState.value.copy(
            clocks = entries,
            localTime = now.format(localTimeFormatter),
            localZone = localZone.id.substringAfterLast("/").replace("_", " ")
        )
    }

    private fun formatDiff(hours: Double): String {
        val abs = kotlin.math.abs(hours)
        return if (abs == abs.toLong().toDouble()) "${abs.toLong()}" else String.format(Locale.US, "%.1f", abs)
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
        val now = ZonedDateTime.now()
        val results = allZones
            .filter { (zoneId, city) ->
                city.contains(query, ignoreCase = true) ||
                zoneId.contains(query, ignoreCase = true)
            }
            .filter { (zoneId, _) -> zoneId !in savedZones }
            .take(20)
            .mapNotNull { (zoneId, _) -> buildWorldClockEntry(zoneId = zoneId, now = now) }
        _uiState.value = _uiState.value.copy(searchResults = results)
    }

    fun addZone(zoneId: String) {
        // Refuse zones the JVM doesn't know — addZone is callable via search
        // results so this also sanity-checks the search filter output.
        if (zoneId !in ZoneId.getAvailableZoneIds()) {
            hideAddDialog()
            return
        }
        if (zoneId !in savedZones) {
            savedZones.add(zoneId)
            persistZones()
            updateTimes()
        }
        hideAddDialog()
    }

    fun removeZone(zoneId: String) {
        if (savedZones.remove(zoneId)) {
            persistZones()
            updateTimes()
        }
    }

    private fun persistZones() {
        prefs.edit().putString("zones", savedZones.joinToString("|")).apply()
    }

    private fun buildWorldClockEntry(
        zoneId: String,
        now: ZonedDateTime
    ): WorldClockEntry? {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return null
        val zdt = now.withZoneSameInstant(zone)
        val localOffset = now.offset.totalSeconds
        val offset = zdt.offset.totalSeconds
        val diffHours = (offset - localOffset) / 3600.0
        return WorldClockEntry(
            zoneId = zoneId,
            cityName = zoneId.substringAfterLast("/").replace("_", " "),
            time = zdt.format(zoneTimeFormatter),
            date = zdt.format(zoneDateFormatter),
            offsetHours = diffHours,
            isAhead = diffHours >= 0
        )
    }

    companion object {
        private val DEFAULT_ZONES = listOf(
            "America/New_York",
            "America/Los_Angeles",
            "Europe/London",
            "Asia/Tokyo"
        )
    }
}
