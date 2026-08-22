package com.sysadmindoc.alarmclock.ui.stats

import com.sysadmindoc.alarmclock.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepRepository
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.repository.ActigraphyRepository
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import com.sysadmindoc.alarmclock.domain.WakeConsistencyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class StatsUiState(
    val stats: AlarmStats = AlarmStats(),
    val recentEvents: List<AlarmEvent> = emptyList(),
    val isLoading: Boolean = true,
    val is24Hour: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val healthConnectSleepSummary: HealthConnectSleepSummary = HealthConnectSleepSummary(),
    val sleepWakeAnalytics: SleepWakeAnalytics = SleepWakeAnalytics(),
    val actigraphySessions: List<ActigraphySession> = emptyList(),
    val sleepNeedMinutes: Long = DEFAULT_SLEEP_NEED_MINUTES,
    val wakeConsistency: WakeConsistencyCalculator.Result? = null,
    /** One-shot feedback for an action the user took, cleared once shown. */
    val statusMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context,
    private val eventRepository: AlarmEventRepository,
    private val actigraphyRepository: ActigraphyRepository,
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
            eventRepository.observeRecent(50).collect { events ->
                val stats = runCatching { eventRepository.getStats() }
                    .getOrDefault(_uiState.value.stats)
                val analytics = readSleepWakeAnalytics(
                    _uiState.value.healthConnectSleepSummary,
                    _uiState.value.sleepNeedMinutes
                )
                _uiState.value = _uiState.value.copy(
                    recentEvents = events,
                    stats = stats,
                    sleepWakeAnalytics = analytics,
                    wakeConsistency = computeWakeConsistency(events),
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            actigraphyRepository.observeRecent(5).collect { sessions ->
                _uiState.value = _uiState.value.copy(actigraphySessions = sessions)
            }
        }
        // Track the user's 24-hour preference so EventRow timestamps respect
        // the global setting (the screen previously hardcoded 12-hour format
        // because the nav graph wasn't passing the parameter through).
        viewModelScope.launch {
            preferencesManager.settings.collect { settings ->
                val needMinutes = (settings.sleepGoalHours * 60L + settings.sleepGoalMinutes)
                    .coerceAtLeast(60L)
                _uiState.value = _uiState.value.copy(
                    is24Hour = settings.is24HourFormat,
                    healthConnectEnabled = settings.healthConnectEnabled,
                    sleepNeedMinutes = needMinutes
                )
                val summary = if (settings.healthConnectEnabled) {
                    healthConnectSleepRepository.readRecentSleepSummary()
                } else {
                    HealthConnectSleepSummary()
                }
                _uiState.value = _uiState.value.copy(
                    healthConnectSleepSummary = summary,
                    sleepWakeAnalytics = readSleepWakeAnalytics(summary, needMinutes)
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            // An unguarded Room failure here took the whole process down from a
            // button the user pressed on purpose.
            val cleared = runCatching { eventRepository.clearHistory() }.isSuccess
            _uiState.value = _uiState.value.copy(
                statusMessage = if (cleared) appContext.getString(R.string.stats_history_cleared) else appContext.getString(R.string.stats_could_not_clear_history)
            )
            // observeRecent() will fire with an empty list and trigger getStats()
            // in the collect block above, so no manual reload needed.
        }
    }

    fun consumeStatusMessage() {
        if (_uiState.value.statusMessage != null) {
            _uiState.value = _uiState.value.copy(statusMessage = null)
        }
    }

    private suspend fun readSleepWakeAnalytics(
        sleepSummary: HealthConnectSleepSummary,
        sleepNeedMinutes: Long
    ): SleepWakeAnalytics {
        val sinceMs = System.currentTimeMillis() - SLEEP_WAKE_WINDOW_MS
        val events = runCatching { eventRepository.getSince(sinceMs) }
            .getOrDefault(emptyList())
        return buildSleepWakeAnalytics(
            events = events,
            sleepSessions = sleepSummary.recentSessions,
            days = SLEEP_WAKE_WINDOW_DAYS,
            sleepNeedMinutes = sleepNeedMinutes
        )
    }

    /**
     * Wake-time consistency from recent dismisses. Only successful wake-ups
     * (DISMISSED with a real action time) count; snoozes/skips/misses aren't a
     * "when did you get up" signal. Times are taken in the device's current
     * zone as minutes past midnight.
     */
    private fun computeWakeConsistency(events: List<AlarmEvent>): WakeConsistencyCalculator.Result? {
        val zone = ZoneId.systemDefault()
        val wakeMinutes = events
            .filter { it.action == AlarmEvent.ACTION_DISMISSED && it.actionAt > 0 }
            .map {
                val time = Instant.ofEpochMilli(it.actionAt).atZone(zone)
                time.hour * 60 + time.minute
            }
        return WakeConsistencyCalculator.consistencyScore(wakeMinutes)
    }

    private companion object {
        const val SLEEP_WAKE_WINDOW_DAYS = 14
        const val SLEEP_WAKE_WINDOW_MS = SLEEP_WAKE_WINDOW_DAYS * 24L * 60L * 60L * 1000L
    }
}
