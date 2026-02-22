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

data class TimerUiState(
    val inputDigits: String = "",          // Raw numpad input "01030" = 1m 30s
    val totalSeconds: Long = 0,            // Resolved total seconds
    val remainingMillis: Long = 0,         // Countdown remaining
    val state: TimerState = TimerState.IDLE,
    val gradualVolumeEnabled: Boolean = true,
    val overrideSystemVolume: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
) {
    val displayHours: Int get() = (remainingMillis / 3600000).toInt()
    val displayMinutes: Int get() = ((remainingMillis % 3600000) / 60000).toInt()
    val displaySeconds: Int get() = ((remainingMillis % 60000) / 1000).toInt()

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

    val progress: Float get() = if (totalSeconds > 0) {
        remainingMillis.toFloat() / (totalSeconds * 1000f)
    } else 0f

    val canStart: Boolean get() = inputDigits.isNotEmpty() && state == TimerState.IDLE
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

    private var countdownJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun appendDigit(digit: Int) {
        val current = _uiState.value
        if (current.state != TimerState.IDLE) return
        if (current.inputDigits.length >= 6) return
        // Don't allow leading zeros beyond what makes sense
        val newDigits = current.inputDigits + digit.toString()
        _uiState.value = current.copy(inputDigits = newDigits)
    }

    fun deleteDigit() {
        val current = _uiState.value
        if (current.state != TimerState.IDLE) return
        if (current.inputDigits.isEmpty()) return
        _uiState.value = current.copy(inputDigits = current.inputDigits.dropLast(1))
    }

    fun clearInput() {
        val current = _uiState.value
        if (current.state != TimerState.IDLE) return
        _uiState.value = current.copy(inputDigits = "")
    }

    fun selectPreset(preset: TimerPreset) {
        val hours = (preset.seconds / 3600).toInt()
        val mins = ((preset.seconds % 3600) / 60).toInt()
        val secs = (preset.seconds % 60).toInt()
        val digits = String.format("%02d%02d%02d", hours, mins, secs).trimStart('0')
        _uiState.value = _uiState.value.copy(
            inputDigits = digits,
            state = TimerState.IDLE
        )
    }

    fun start() {
        val current = _uiState.value
        if (current.inputDigits.isEmpty()) return

        val totalSecs = current.inputHours * 3600L +
                current.inputMinutes * 60L +
                current.inputSeconds
        if (totalSecs <= 0) return

        val totalMillis = totalSecs * 1000L
        _uiState.value = current.copy(
            totalSeconds = totalSecs,
            remainingMillis = totalMillis,
            state = TimerState.RUNNING
        )
        startCountdown(totalMillis)
    }

    fun pause() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
    }

    fun resume() {
        val remaining = _uiState.value.remainingMillis
        _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)
        startCountdown(remaining)
    }

    fun stop() {
        countdownJob?.cancel()
        stopAudio()
        _uiState.value = TimerUiState()
    }

    fun toggleGradualVolume(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(gradualVolumeEnabled = enabled)
    }

    fun toggleOverrideVolume(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(overrideSystemVolume = enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(keepScreenOn = enabled)
    }

    fun dismissFinished() {
        stopAudio()
        _uiState.value = TimerUiState()
    }

    private fun startCountdown(millis: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + millis

            while (isActive) {
                val now = System.currentTimeMillis()
                val remaining = (endTime - now).coerceAtLeast(0)
                _uiState.value = _uiState.value.copy(remainingMillis = remaining)

                if (remaining <= 0) {
                    _uiState.value = _uiState.value.copy(state = TimerState.FINISHED)
                    playFinishSound()
                    break
                }
                delay(50) // Update ~20 times/sec for smooth display
            }
        }
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

            if (_uiState.value.vibrationEnabled) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
                val pattern = longArrayOf(0, 500, 500, 500, 500)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        countdownJob?.cancel()
        stopAudio()
        super.onCleared()
    }
}
