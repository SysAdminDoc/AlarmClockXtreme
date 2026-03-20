package com.sysadmindoc.alarmclock.ui.timer

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
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

    val canStart: Boolean get() = inputDigits.isNotEmpty()

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

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var nextId = 1
    private val countdownJobs = mutableMapOf<Int, Job>()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun appendDigit(digit: Int) {
        val current = _uiState.value
        if (current.inputDigits.length >= 6) return
        _uiState.value = current.copy(inputDigits = current.inputDigits + digit.toString())
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
        startCountdown(id, totalMillis)
    }

    fun pause(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.RUNNING }?.id ?: return
        countdownJobs[id]?.cancel()
        updateTimer(id) { it.copy(state = TimerState.PAUSED) }
    }

    fun resume(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.PAUSED }?.id ?: return
        val timer = _uiState.value.activeTimers.find { it.id == id } ?: return
        updateTimer(id) { it.copy(state = TimerState.RUNNING) }
        startCountdown(id, timer.remainingMillis)
    }

    fun stop(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull()?.id ?: return
        countdownJobs[id]?.cancel()
        countdownJobs.remove(id)
        stopAudioForTimer(id)
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.filter { it.id != id }
        )
    }

    fun dismissFinished(timerId: Int? = null) {
        val id = timerId ?: _uiState.value.activeTimers.firstOrNull { it.state == TimerState.FINISHED }?.id ?: return
        stopAudio()
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

    private fun startCountdown(id: Int, millis: Long) {
        countdownJobs[id]?.cancel()
        countdownJobs[id] = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + millis

            while (isActive) {
                val now = System.currentTimeMillis()
                val remaining = (endTime - now).coerceAtLeast(0)
                updateTimer(id) { it.copy(remainingMillis = remaining) }

                if (remaining <= 0) {
                    updateTimer(id) { it.copy(state = TimerState.FINISHED) }
                    playFinishSound()
                    break
                }
                delay(50)
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
        try {
            val context = getApplication<Application>()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

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

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            val pattern = longArrayOf(0, 500, 500, 500, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudioForTimer(id: Int) {
        // Only stop audio if no other timer is finished
        if (_uiState.value.activeTimers.none { it.id != id && it.state == TimerState.FINISHED }) {
            stopAudio()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onCleared() {
        countdownJobs.values.forEach { it.cancel() }
        stopAudio()
        super.onCleared()
    }
}
