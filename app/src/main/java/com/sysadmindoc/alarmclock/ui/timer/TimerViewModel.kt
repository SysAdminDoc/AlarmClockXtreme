package com.sysadmindoc.alarmclock.ui.timer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

data class TimerPreset(val label: String, val seconds: Long)

data class TimerInstance(
    val id: Int,
    val label: String = "",
    val totalSeconds: Long = 0,
    val remainingMillis: Long = 0,
    val state: TimerState = TimerState.RUNNING,
) {
    val displayHours: Int get() = (remainingMillis / 3600000).toInt()
    val displayMinutes: Int get() = ((remainingMillis % 3600000) / 60000).toInt()
    val displaySeconds: Int get() = ((remainingMillis % 60000) / 1000).toInt()
    val progress: Float get() = if (totalSeconds > 0) {
        remainingMillis.toFloat() / (totalSeconds * 1000f)
    } else 0f
}

data class TimerUiState(
    val inputDigits: String = "",
    val activeTimers: List<TimerInstance> = emptyList(),
    val isInputMode: Boolean = true,
) {
    val inputHours: Int get() {
        val padded = inputDigits.padStart(6, '0')
        return padded.substring(0, 2).toIntOrNull() ?: 0
    }
    val inputMinutes: Int get() {
        val padded = inputDigits.padStart(6, '0')
        return padded.substring(2, 4).toIntOrNull() ?: 0
    }
    val inputSeconds: Int get() {
        val padded = inputDigits.padStart(6, '0')
        return padded.substring(4, 6).toIntOrNull() ?: 0
    }

    val canStart: Boolean get() = (inputHours * 3600L + inputMinutes * 60L + inputSeconds) > 0

    // For backward compat with single-timer UI properties
    val state: TimerState get() = when {
        activeTimers.any { it.state == TimerState.FINISHED } -> TimerState.FINISHED
        activeTimers.any { it.state == TimerState.RUNNING } -> TimerState.RUNNING
        activeTimers.any { it.state == TimerState.PAUSED } -> TimerState.PAUSED
        else -> TimerState.IDLE
    }

    val totalSeconds: Long get() = activeTimers.firstOrNull()?.totalSeconds ?: 0
    val remainingMillis: Long get() = activeTimers.firstOrNull()?.remainingMillis ?: 0
    val displayHours: Int get() = activeTimers.firstOrNull()?.displayHours ?: 0
    val displayMinutes: Int get() = activeTimers.firstOrNull()?.displayMinutes ?: 0
    val displaySeconds: Int get() = activeTimers.firstOrNull()?.displaySeconds ?: 0
    val progress: Float get() = activeTimers.firstOrNull()?.progress ?: 0f

    // Keep these for TimerScreen backward compat
    val gradualVolumeEnabled: Boolean = true
    val overrideSystemVolume: Boolean = false
    val vibrationEnabled: Boolean = true
    val keepScreenOn: Boolean = false
}

val defaultPresets = listOf(
    TimerPreset("1 min", 60),
    TimerPreset("3 min", 180),
    TimerPreset("5 min", 300),
    TimerPreset("10 min", 600),
    TimerPreset("15 min", 900),
    TimerPreset("30 min", 1800),
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val COUNTDOWN_TICK_MS = 250L
    }

    private val appContext = application.applicationContext
    private val timerStore = TimerStore(appContext)
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val countdownJobs = mutableMapOf<Int, Job>()
    private val runningEndTimes = mutableMapOf<Int, Long>()

    init {
        restorePersistedTimers()
    }

    fun appendDigit(digit: Int) {
        val current = _uiState.value
        if (current.inputDigits.length >= 6) return
        _uiState.value = current.copy(inputDigits = current.inputDigits + digit.toString())
    }

    fun appendDoubleZero() {
        appendDigit(0)
        appendDigit(0)
    }

    fun deleteDigit() {
        val current = _uiState.value
        if (current.inputDigits.isEmpty()) return
        _uiState.value = current.copy(inputDigits = current.inputDigits.dropLast(1))
    }

    fun clearInput() {
        _uiState.value = _uiState.value.copy(inputDigits = "")
    }

    fun selectPreset(preset: TimerPreset) {
        val hours = (preset.seconds / 3600).toInt()
        val mins = ((preset.seconds % 3600) / 60).toInt()
        val secs = (preset.seconds % 60).toInt()
        val digits = String.format("%02d%02d%02d", hours, mins, secs).trimStart('0')
        _uiState.value = _uiState.value.copy(inputDigits = digits)
    }

    fun start() {
        val current = _uiState.value
        if (current.inputDigits.isEmpty()) return

        val totalSecs = current.inputHours * 3600L +
                current.inputMinutes * 60L +
                current.inputSeconds
        if (totalSecs <= 0) return

        // Allocate under the store's write lock: external writers (notification
        // Restart, Assistant SET_TIMER) also insert records, and a cached
        // in-memory counter here once reused their id, silently overwriting
        // the externally created timer's record and AlarmManager deadline.
        val id = timerStore.nextId()
        val totalMillis = totalSecs * 1000L
        val label = formatTimerLabel(current.inputHours, current.inputMinutes, current.inputSeconds)
        val timer = TimerInstance(
            id = id,
            label = label,
            totalSeconds = totalSecs,
            remainingMillis = totalMillis,
            state = TimerState.RUNNING
        )

        val newTimers = _uiState.value.activeTimers + timer
        _uiState.value = _uiState.value.copy(
            activeTimers = newTimers,
            inputDigits = "",
            isInputMode = true
        )
        val endElapsedRealtime = SystemClock.elapsedRealtime() + totalMillis
        val persisted = timer.toPersistedRecord(endElapsedRealtime)
        runningEndTimes[id] = endElapsedRealtime
        timerStore.upsert(persisted)
        TimerAlarmScheduler.schedule(appContext, id, endElapsedRealtime)
        TimerNotifications.postRunning(appContext, persisted)
        startCountdownUntil(id, endElapsedRealtime)
    }

    fun pause(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.RUNNING }?.id ?: return
        countdownJobs.remove(id)?.cancel()
        runningEndTimes.remove(id)
        TimerAlarmScheduler.cancel(appContext, id)
        TimerNotifications.cancelTimer(appContext, id)
        updateTimer(id) { timer ->
            timer.copy(state = TimerState.PAUSED).also { timerStore.upsert(it.toPersistedRecord()) }
        }
    }

    fun resume(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.PAUSED }?.id ?: return
        val timer = _uiState.value.activeTimers.find { it.id == id } ?: return
        val endElapsedRealtime = SystemClock.elapsedRealtime() + timer.remainingMillis
        val resumed = timer.copy(state = TimerState.RUNNING)
        val persisted = resumed.toPersistedRecord(endElapsedRealtime)
        runningEndTimes[id] = endElapsedRealtime
        updateTimer(id) { resumed }
        timerStore.upsert(persisted)
        TimerAlarmScheduler.schedule(appContext, id, endElapsedRealtime)
        TimerNotifications.postRunning(appContext, persisted)
        startCountdownUntil(id, endElapsedRealtime)
    }

    /**
     * Stop sits next to Pause and removes the timer outright, so a mis-tap used
     * to end a countdown with nothing to fall back on. The action stays
     * immediate (no confirmation dialogs by project rule) and the screen offers
     * an undo instead.
     */
    fun stop(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull()?.id ?: return
        _uiState.value.activeTimers.firstOrNull { it.id == id }?.let { stopped ->
            undoStopSnapshot = stopped
        }
        countdownJobs[id]?.cancel()
        countdownJobs.remove(id)
        runningEndTimes.remove(id)
        TimerAlarmScheduler.cancel(appContext, id)
        TimerAlarmService.dismiss(appContext, id)
        cancelTimerFinishedNotification(id)
        timerStore.remove(id)
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.filter { it.id != id }
        )
    }

    /** True when [stop] removed a timer that can still be put back. */
    val canUndoStop: Boolean get() = undoStopSnapshot != null

    /** Re-creates the timer [stop] removed, with the time it had left. */
    fun undoStop() {
        val stopped = undoStopSnapshot ?: return
        undoStopSnapshot = null
        val remainingMillis = stopped.remainingMillis.coerceAtLeast(1_000L)
        val id = timerStore.nextId()
        val restored = stopped.copy(
            id = id,
            remainingMillis = remainingMillis,
            state = TimerState.RUNNING
        )
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers + restored
        )
        val endElapsedRealtime = SystemClock.elapsedRealtime() + remainingMillis
        val persisted = restored.toPersistedRecord(endElapsedRealtime)
        runningEndTimes[id] = endElapsedRealtime
        timerStore.upsert(persisted)
        TimerAlarmScheduler.schedule(appContext, id, endElapsedRealtime)
        TimerNotifications.postRunning(appContext, persisted)
        startCountdownUntil(id, endElapsedRealtime)
    }

    fun clearUndoStop() {
        undoStopSnapshot = null
    }

    private var undoStopSnapshot: TimerInstance? = null

    fun dismissFinished(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.FINISHED }?.id ?: return
        TimerAlarmService.dismiss(appContext, id)
        cancelTimerFinishedNotification(id)
        timerStore.remove(id)
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.filter { it.id != id }
        )
    }

    // Legacy compat for single-timer UI
    fun pause() = pause(null)
    fun resume() = resume(null)
    fun stop() = stop(null)
    fun dismissFinished() = dismissFinished(null)
    private fun updateTimer(id: Int, transform: (TimerInstance) -> TimerInstance) {
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.map {
                if (it.id == id) transform(it) else it
            }
        )
    }

    private fun startCountdownUntil(id: Int, endTime: Long) {
        countdownJobs.remove(id)?.cancel()
        var countdownJob: Job? = null
        countdownJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val now = SystemClock.elapsedRealtime()
                    val remaining = (endTime - now).coerceAtLeast(0)
                    updateTimer(id) { it.copy(remainingMillis = remaining) }

                    if (remaining <= 0) {
                        runningEndTimes.remove(id)
                        TimerAlarmScheduler.cancel(appContext, id)
                        val finished = timerStore.markFinished(id)
                        updateTimer(id) { timer ->
                            timer.copy(
                                remainingMillis = 0L,
                                state = TimerState.FINISHED
                            )
                        }
                        if (finished != null) {
                            TimerAlarmService.fire(appContext, finished.id, finished.label)
                        }
                        break
                    }
                    // The UI only renders down to whole seconds plus a progress
                    // ring, so a 250ms cadence is visually smooth while rebuilding
                    // the active-timers state 5x less often than the old 50ms tick.
                    delay(COUNTDOWN_TICK_MS)
                }
            } finally {
                if (countdownJobs[id] === countdownJob) {
                    countdownJobs.remove(id)
                }
            }
        }
        countdownJobs[id] = countdownJob
    }

    /**
     * Reconciles UI state with the persisted store after external writers
     * mutate timers while this ViewModel is alive: notification Restart,
     * Assistant SET_TIMER, and notification-side dismissals all write the
     * store directly. Without this the Timer tab keeps rendering its stale
     * in-memory list until process death.
     */
    fun resyncFromStore() {
        val snapshot = timerStore.restoreSnapshot()
        // Failsafe: restoreSnapshot atomically claims overdue running timers;
        // markFinished single-ownership makes this a no-op when the service
        // or a countdown job already alerted for them.
        snapshot.newlyFinished.forEach { finished ->
            TimerAlarmService.fire(appContext, finished.id, finished.label)
        }
        val records = snapshot.records
        val recordIds = records.mapTo(mutableSetOf()) { it.id }
        (countdownJobs.keys - recordIds).toList().forEach { removedId ->
            countdownJobs.remove(removedId)?.cancel()
            runningEndTimes.remove(removedId)
        }
        _uiState.value = _uiState.value.copy(activeTimers = records.map { it.toTimerInstance() })
        records.forEach { record ->
            when (record.state) {
                TimerState.RUNNING -> {
                    // The external writer already armed AlarmManager and posted
                    // the running notification; only the countdown UI needs to
                    // start tracking it here.
                    if (runningEndTimes[record.id] != record.endElapsedRealtime) {
                        runningEndTimes[record.id] = record.endElapsedRealtime
                        startCountdownUntil(record.id, record.endElapsedRealtime)
                    }
                }
                else -> {
                    countdownJobs.remove(record.id)?.cancel()
                    runningEndTimes.remove(record.id)
                }
            }
        }
    }

    private fun restorePersistedTimers() {
        val snapshot = timerStore.restoreSnapshot()
        snapshot.newlyFinished.forEach { finished ->
            TimerAlarmService.fire(appContext, finished.id, finished.label)
        }
        val records = snapshot.records
        val timers = records.map { it.toTimerInstance() }
        if (timers.isEmpty()) return

        _uiState.value = _uiState.value.copy(activeTimers = timers)
        records.forEach { record ->
            when (record.state) {
                TimerState.RUNNING -> {
                    runningEndTimes[record.id] = record.endElapsedRealtime
                    TimerAlarmScheduler.schedule(appContext, record.id, record.endElapsedRealtime)
                    TimerNotifications.postRunning(appContext, record)
                    startCountdownUntil(record.id, record.endElapsedRealtime)
                }
                TimerState.FINISHED -> Unit
                TimerState.IDLE,
                TimerState.PAUSED -> Unit
            }
        }
    }

    private fun formatTimerLabel(h: Int, m: Int, s: Int): String {
        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0) append("${s}s")
        }.trim()
    }

    override fun onCleared() {
        countdownJobs.values.forEach { it.cancel() }
        super.onCleared()
    }

    private fun cancelTimerFinishedNotification(id: Int) {
        TimerNotifications.cancelFinished(appContext, id)
    }

    private fun TimerInstance.toPersistedRecord(endElapsedRealtime: Long = 0L): PersistedTimerRecord =
        PersistedTimerRecord(
            id = id,
            label = label,
            totalSeconds = totalSeconds,
            remainingMillis = remainingMillis,
            state = state,
            endElapsedRealtime = endElapsedRealtime
        )
}
