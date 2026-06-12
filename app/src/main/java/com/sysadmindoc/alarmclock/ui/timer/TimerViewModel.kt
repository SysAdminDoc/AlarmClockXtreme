package com.sysadmindoc.alarmclock.ui.timer

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.util.Log
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
        private const val TAG = "TimerViewModel"
    }

    private val appContext = application.applicationContext
    private val timerStore = TimerStore(appContext)
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var nextId = 1
    private val countdownJobs = mutableMapOf<Int, Job>()
    private val runningEndTimes = mutableMapOf<Int, Long>()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

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

        val id = nextId++
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
        runningEndTimes[id] = endElapsedRealtime
        timerStore.upsert(timer.toPersistedRecord(endElapsedRealtime))
        TimerAlarmScheduler.schedule(appContext, id, endElapsedRealtime)
        startCountdownUntil(id, endElapsedRealtime)
    }

    fun pause(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.RUNNING }?.id ?: return
        countdownJobs.remove(id)?.cancel()
        runningEndTimes.remove(id)
        TimerAlarmScheduler.cancel(appContext, id)
        updateTimer(id) { timer ->
            timer.copy(state = TimerState.PAUSED).also { timerStore.upsert(it.toPersistedRecord()) }
        }
    }

    fun resume(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.PAUSED }?.id ?: return
        val timer = _uiState.value.activeTimers.find { it.id == id } ?: return
        val endElapsedRealtime = SystemClock.elapsedRealtime() + timer.remainingMillis
        runningEndTimes[id] = endElapsedRealtime
        updateTimer(id) { running ->
            running.copy(state = TimerState.RUNNING).also {
                timerStore.upsert(it.toPersistedRecord(endElapsedRealtime))
            }
        }
        TimerAlarmScheduler.schedule(appContext, id, endElapsedRealtime)
        startCountdownUntil(id, endElapsedRealtime)
    }

    fun stop(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull()?.id ?: return
        countdownJobs[id]?.cancel()
        countdownJobs.remove(id)
        runningEndTimes.remove(id)
        TimerAlarmScheduler.cancel(appContext, id)
        stopAudioForTimer(id)
        cancelTimerFinishedNotification(id)
        timerStore.remove(id)
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.filter { it.id != id }
        )
    }

    fun dismissFinished(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.FINISHED }?.id ?: return
        stopAudioForTimer(id)
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
    fun toggleGradualVolume(enabled: Boolean) {}
    fun toggleOverrideVolume(enabled: Boolean) {}
    fun toggleVibration(enabled: Boolean) {}
    fun toggleKeepScreenOn(enabled: Boolean) {}

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
                        updateTimer(id) { timer ->
                            timer.copy(state = TimerState.FINISHED).also {
                                timerStore.upsert(it.toPersistedRecord())
                            }
                        }
                        playFinishSound()
                        // v1.12.1 (roadmap N8): surface the finished timer
                        // even when the app is in the background.
                        postTimerFinishedNotification(id)
                        break
                    }
                    delay(50)
                }
            } finally {
                if (countdownJobs[id] === countdownJob) {
                    countdownJobs.remove(id)
                }
            }
        }
        countdownJobs[id] = countdownJob
    }

    private fun restorePersistedTimers() {
        val records = timerStore.loadRecords()
        val timers = records.map { it.toTimerInstance() }
        nextId = ((records.maxOfOrNull { it.id } ?: 0) + 1).coerceAtLeast(1)
        if (timers.isEmpty()) return

        timerStore.replace(records)
        _uiState.value = _uiState.value.copy(activeTimers = timers)
        records.forEach { record ->
            when (record.state) {
                TimerState.RUNNING -> {
                    runningEndTimes[record.id] = record.endElapsedRealtime
                    TimerAlarmScheduler.schedule(appContext, record.id, record.endElapsedRealtime)
                    startCountdownUntil(record.id, record.endElapsedRealtime)
                }
                TimerState.FINISHED -> TimerNotifications.postFinished(appContext, record.id, record.label)
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

    private fun playFinishSound() {
        // If audio is already playing for a previous finished timer, don't allocate a
        // second MediaPlayer — the existing tone covers all simultaneously-finished timers.
        if (mediaPlayer != null) return
        try {
            val context = getApplication<Application>()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }

            if (vibrator == null) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
                val pattern = longArrayOf(0, 500, 500, 500, 500)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        } catch (e: Exception) {
            // Free the partial player on any prepare/start failure to avoid leaking it.
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
            Log.w(TAG, "Failed to play timer completion sound", e)
        }
    }

    private fun stopAudioForTimer(id: Int) {
        // Only stop audio if no other timer is finished
        if (_uiState.value.activeTimers.none { it.id != id && it.state == TimerState.FINISHED }) {
            stopAudio()
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) { /* already released */ }
        mediaPlayer = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
    }

    override fun onCleared() {
        countdownJobs.values.forEach { it.cancel() }
        stopAudio()
        // v1.12.1 (roadmap N8): the notifications themselves intentionally
        // survive process death — they are the "missed timer" surface for
        // the user who closed the app — so we do NOT cancel them here.
        super.onCleared()
    }

    /**
     * v1.12.1 (roadmap N8): post a heads-up notification for a finished
     * timer. The intent re-opens the timer screen so a tap restores
     * focus to the running list. Caller must hold an active timer with
     * id [id]; we look up its label once and bail silently if it's gone.
     *
     * Notification permission (POST_NOTIFICATIONS) is declared in the
     * manifest and granted as part of the alarm-readiness flow — the
     * runtime check is done by `NotificationManagerCompat.from()` and
     * `notify()` silently no-ops if the user denied it.
     */
    private fun postTimerFinishedNotification(id: Int) {
        val timer = _uiState.value.activeTimers.find { it.id == id } ?: return
        TimerNotifications.postFinished(appContext, id, timer.label)
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
