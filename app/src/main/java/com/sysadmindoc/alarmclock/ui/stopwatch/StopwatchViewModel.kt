package com.sysadmindoc.alarmclock.ui.stopwatch

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
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
class StopwatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var startTime: Long = 0
    private var accumulatedTime: Long = 0

    init {
        restore()
        viewModelScope.launch {
            _uiState.subscriptionCount.collect { count ->
                if (count > 0 && _uiState.value.state == StopwatchState.RUNNING && tickerJob?.isActive != true) {
                    startTicker()
                } else if (count == 0 && tickerJob?.isActive == true) {
                    tickerJob?.cancel()
                }
            }
        }
    }

    fun start() {
        // SystemClock.elapsedRealtime() is monotonic and unaffected by NTP, DST,
        // or user clock-set actions — wall time would let the stopwatch jump
        // backwards or forwards mid-run.
        startTime = SystemClock.elapsedRealtime()
        _uiState.value = _uiState.value.copy(state = StopwatchState.RUNNING)
        startTicker()
        persist()
    }

    fun pause() {
        tickerJob?.cancel()
        accumulatedTime += SystemClock.elapsedRealtime() - startTime
        _uiState.value = _uiState.value.copy(
            state = StopwatchState.PAUSED,
            elapsedMillis = accumulatedTime
        )
        persist()
    }

    fun resume() {
        startTime = SystemClock.elapsedRealtime()
        _uiState.value = _uiState.value.copy(state = StopwatchState.RUNNING)
        startTicker()
        persist()
    }

    /**
     * Reset throws away the run and every lap with it. The project rule is no
     * confirmation dialogs, so the escape hatch is an undo instead: one tap
     * cannot be the end of a session someone was timing.
     */
    fun reset() {
        val discarded = _uiState.value
        val discardedAccumulated = accumulatedTime
        tickerJob?.cancel()
        accumulatedTime = 0
        _uiState.value = StopwatchUiState()
        persist()
        undoSnapshot = if (discarded.elapsedMillis > 0L || discarded.laps.isNotEmpty()) {
            UndoSnapshot(discarded, discardedAccumulated)
        } else {
            null
        }
    }

    /** True when [reset] discarded something worth offering back. */
    val canUndoReset: Boolean get() = undoSnapshot != null

    /** Restores the run [reset] discarded, paused so nothing keeps counting. */
    fun undoReset() {
        val snapshot = undoSnapshot ?: return
        undoSnapshot = null
        accumulatedTime = snapshot.accumulatedTime
        _uiState.value = snapshot.state.copy(state = StopwatchState.PAUSED)
        persist()
    }

    fun clearUndo() {
        undoSnapshot = null
    }

    private var undoSnapshot: UndoSnapshot? = null

    private data class UndoSnapshot(
        val state: StopwatchUiState,
        val accumulatedTime: Long
    )

    fun lap() {
        val current = _uiState.value
        if (current.state != StopwatchState.RUNNING) return

        val totalAtLap = current.elapsedMillis
        val previousTotal = current.laps.maxByOrNull { it.number }?.totalMillis ?: 0
        val splitTime = totalAtLap - previousTotal

        val newLap = Lap(
            number = current.laps.size + 1,
            splitMillis = splitTime,
            totalMillis = totalAtLap
        )

        val allLaps = listOf(newLap) + current.laps
        val markedLaps = markBestWorst(allLaps)

        _uiState.value = current.copy(laps = markedLaps)
        persist()
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
                val elapsed = accumulatedTime + (SystemClock.elapsedRealtime() - startTime)
                _uiState.value = _uiState.value.copy(elapsedMillis = elapsed)
                delay(16) // ~60fps
            }
        }
    }

    /**
     * Persist enough to reconstruct the stopwatch across process death. We store
     * the monotonic [startTime] anchor (elapsedRealtime) rather than a computed
     * elapsed, so a RUNNING stopwatch keeps advancing correctly while the app is
     * gone. [bootToken] lets us detect a reboot (which resets elapsedRealtime) so
     * we don't compute a bogus running delta against a stale anchor.
     */
    private fun persist() {
        val state = _uiState.value
        runCatching {
            val laps = JSONArray()
            state.laps.forEach { lap ->
                laps.put(
                    JSONObject()
                        .put("n", lap.number)
                        .put("s", lap.splitMillis)
                        .put("t", lap.totalMillis)
                )
            }
            val editor = prefs.edit()
                .putString("state", state.state.name)
                .putLong("accumulated", accumulatedTime)
                .putLong("startTime", startTime)
                .putLong("bootToken", bootToken())
                .putString("laps", laps.toString())
            val bootCount = currentBootCount()
            if (bootCount >= 0L) {
                editor.putLong("bootCount", bootCount)
            }
            editor.apply()
        }
    }

    private fun restore() {
        runCatching {
            val stateName = prefs.getString("state", null) ?: return
            val restoredState = runCatching { StopwatchState.valueOf(stateName) }
                .getOrDefault(StopwatchState.IDLE)
            if (restoredState == StopwatchState.IDLE) return
            accumulatedTime = prefs.getLong("accumulated", 0L).coerceAtLeast(0L)
            startTime = prefs.getLong("startTime", 0L)
            val laps = parseLaps(prefs.getString("laps", null))

            if (restoredState == StopwatchState.RUNNING) {
                // Prefer the OS boot counter: the wall-vs-monotonic delta
                // false-positives on any >5 s wall-clock adjustment (NTP,
                // carrier time while traveling, manual set) and silently
                // dropped the running segment. Legacy token kept as fallback
                // for records persisted before bootCount existed.
                val storedBootCount = prefs.getLong("bootCount", -1L)
                val liveBootCount = currentBootCount()
                val rebooted = if (storedBootCount >= 0L && liveBootCount >= 0L) {
                    storedBootCount != liveBootCount
                } else {
                    kotlin.math.abs(bootToken() - prefs.getLong("bootToken", 0L)) > BOOT_TOKEN_TOLERANCE_MS
                }
                val delta = SystemClock.elapsedRealtime() - startTime
                if (rebooted || delta < 0) {
                    // Reboot reset the monotonic clock; the running segment can't be
                    // recovered. Keep the accumulated time and restore as paused.
                    _uiState.value = StopwatchUiState(
                        elapsedMillis = accumulatedTime,
                        state = StopwatchState.PAUSED,
                        laps = laps
                    )
                } else {
                    _uiState.value = StopwatchUiState(
                        elapsedMillis = accumulatedTime + delta,
                        state = StopwatchState.RUNNING,
                        laps = laps
                    )
                    startTicker()
                }
            } else {
                _uiState.value = StopwatchUiState(
                    elapsedMillis = accumulatedTime,
                    state = StopwatchState.PAUSED,
                    laps = laps
                )
            }
        }
    }

    private fun parseLaps(raw: String?): List<Lap> {
        if (raw.isNullOrBlank()) return emptyList()
        val restored = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(Lap(obj.optInt("n"), obj.optLong("s"), obj.optLong("t")))
                }
            }
        }.getOrDefault(emptyList())
        return markBestWorst(restored)
    }

    private fun bootToken(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    /** Monotonic per-boot counter (API 24+). Returns -1 when unavailable. */
    private fun currentBootCount(): Long = runCatching {
        android.provider.Settings.Global.getLong(
            context.contentResolver,
            android.provider.Settings.Global.BOOT_COUNT
        )
    }.getOrDefault(-1L)

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val PREFS_NAME = "stopwatch_state"
        // Clock drift can nudge (currentTimeMillis - elapsedRealtime) by a little;
        // only a difference beyond this indicates an actual reboot.
        const val BOOT_TOKEN_TOLERANCE_MS = 5_000L
    }
}
