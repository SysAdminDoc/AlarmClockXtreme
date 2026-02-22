package com.sysadmindoc.alarmclock.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StatsUiState(
    val stats: AlarmStats = AlarmStats(),
    val recentEvents: List<AlarmEvent> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val eventRepository: AlarmEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        observeRecent()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val stats = eventRepository.getStats()
            _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
        }
    }

    private fun observeRecent() {
        viewModelScope.launch {
            eventRepository.observeRecent(20).collect { events ->
                _uiState.value = _uiState.value.copy(recentEvents = events)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            eventRepository.clearHistory()
            loadStats()
        }
    }
}
