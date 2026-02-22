package com.sysadmindoc.alarmclock.ui.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class StopwatchState { IDLE, RUNNING, PAUSED }

data class Lap(
    val number: Int,
    val splitMillis: Long,     // Time of this individual lap
    val totalMillis: Long,     // Cumulative time at lap mark
    val isBest: Boolean = false,
    val isWorst: Boolean = false
)

data class StopwatchUiState(
    val elapsedMillis: Long = 0,
    val state: StopwatchState = StopwatchState.IDLE,
    val laps: List<Lap> = emptyList()
) {
    val hours: Int get() = (elapsedMillis / 3600000).toInt()
    val minutes: Int get() = ((elapsedMillis % 3600000) / 60000).toInt()
    val seconds: Int get() = ((elapsedMillis % 60000) / 1000).toInt()
    val centiseconds: Int get() = ((elapsedMillis % 1000) / 10).toInt()
}

@HiltViewModel
class StopwatchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var startTime: Long = 0
    private var accumulatedTime: Long = 0

    fun start() {
        startTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(state = StopwatchState.RUNNING)
        startTicker()
    }

    fun pause() {
        tickerJob?.cancel()
        accumulatedTime += System.currentTimeMillis() - startTime
        _uiState.value = _uiState.value.copy(
            state = StopwatchState.PAUSED,
            elapsedMillis = accumulatedTime
        )
    }

    fun resume() {
        startTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(state = StopwatchState.RUNNING)
        startTicker()
    }

    fun reset() {
        tickerJob?.cancel()
        accumulatedTime = 0
        _uiState.value = StopwatchUiState()
    }

    fun lap() {
        val current = _uiState.value
        if (current.state != StopwatchState.RUNNING) return

        val totalAtLap = current.elapsedMillis
        val previousTotal = current.laps.firstOrNull()?.totalMillis ?: 0
        val splitTime = totalAtLap - previousTotal

        val newLap = Lap(
            number = current.laps.size + 1,
            splitMillis = splitTime,
            totalMillis = totalAtLap
        )

        val allLaps = listOf(newLap) + current.laps
        val markedLaps = markBestWorst(allLaps)

        _uiState.value = current.copy(laps = markedLaps)
    }

    private fun markBestWorst(laps: List<Lap>): List<Lap> {
        if (laps.size < 3) return laps // Need at least 3 laps to compare

        val bestSplit = laps.minOf { it.splitMillis }
        val worstSplit = laps.maxOf { it.splitMillis }

        return laps.map { lap ->
            lap.copy(
                isBest = lap.splitMillis == bestSplit && bestSplit != worstSplit,
                isWorst = lap.splitMillis == worstSplit && bestSplit != worstSplit
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = accumulatedTime + (System.currentTimeMillis() - startTime)
                _uiState.value = _uiState.value.copy(elapsedMillis = elapsed)
                delay(16) // ~60fps
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}
