package com.sysadmindoc.alarmclock.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepRepository
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val stats: AlarmStats = AlarmStats(),
    val recentEvents: List<AlarmEvent> = emptyList(),
    val isLoading: Boolean = true,
    val is24Hour: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val healthConnectSleepSummary: HealthConnectSleepSummary = HealthConnectSleepSummary()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val eventRepository: AlarmEventRepository,
    private val preferencesManager: PreferencesManager,
    private val healthConnectSleepRepository: HealthConnectSleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        // Recompute aggregate stats every time a new event lands so the totals
        // and streak update live alongside the recent-events list. Without this
        // the stats were stale until the screen was reopened.
        viewModelScope.launch {
            eventRepository.observeRecent(20).collect { events ->
                val stats = runCatching { eventRepository.getStats() }
                    .getOrDefault(_uiState.value.stats)
                _uiState.value = _uiState.value.copy(
                    recentEvents = events,
                    stats = stats,
                    isLoading = false
                )
            }
        }
        // Track the user's 24-hour preference so EventRow timestamps respect
        // the global setting (the screen previously hardcoded 12-hour format
        // because the nav graph wasn't passing the parameter through).
        viewModelScope.launch {
            preferencesManager.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    is24Hour = settings.is24HourFormat,
                    healthConnectEnabled = settings.healthConnectEnabled
                )
                val summary = if (settings.healthConnectEnabled) {
                    healthConnectSleepRepository.readRecentSleepSummary()
                } else {
                    HealthConnectSleepSummary()
                }
                _uiState.value = _uiState.value.copy(healthConnectSleepSummary = summary)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            eventRepository.clearHistory()
            // observeRecent() will fire with an empty list and trigger getStats()
            // in the collect block above, so no manual reload needed.
        }
    }
}
