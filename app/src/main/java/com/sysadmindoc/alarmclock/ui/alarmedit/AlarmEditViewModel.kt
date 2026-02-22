package com.sysadmindoc.alarmclock.ui.alarmedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class AlarmEditUiState(
    val hour: Int = 9,
    val minute: Int = 0,
    val label: String = "",
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val ringtoneUri: String = "",
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 2,
    val volume: Int = 100,
    val overrideSystemVolume: Boolean = true,
    val gradualVolumeSeconds: Int = 60,
    val snoozeDurationMinutes: Int = 10,
    val maxSnoozeCount: Int = 3,
    val showOnLockScreen: Boolean = true,
    val challengeType: String = "NONE",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAt: Long = 0,
    val is24HourFormat: Boolean = false,
    val notFound: Boolean = false
)

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val calculator: NextAlarmCalculator,
    private val preferencesManager: com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>("alarmId") ?: -1

    private val _uiState = MutableStateFlow(AlarmEditUiState())
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            val is24h = settings.is24HourFormat

            if (alarmId > 0) {
                val alarm = repository.getById(alarmId)
                if (alarm != null) {
                    _uiState.value = AlarmEditUiState(
                        hour = alarm.hour,
                        minute = alarm.minute,
                        label = alarm.label,
                        repeatDays = alarm.repeatDays,
                        ringtoneUri = alarm.ringtoneUri,
                        vibrationEnabled = alarm.vibrationEnabled,
                        vibrationIntensity = alarm.vibrationIntensity,
                        volume = alarm.volume,
                        overrideSystemVolume = alarm.overrideSystemVolume,
                        gradualVolumeSeconds = alarm.gradualVolumeSeconds,
                        snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                        maxSnoozeCount = alarm.maxSnoozeCount,
                        showOnLockScreen = alarm.showOnLockScreen,
                        challengeType = alarm.challengeType,
                        isEditing = true,
                        isEnabled = alarm.isEnabled,
                        createdAt = alarm.createdAt,
                        is24HourFormat = is24h
                    )
                } else {
                    _uiState.value = _uiState.value.copy(notFound = true, is24HourFormat = is24h)
                }
            } else {
                // New alarm: default to current time rounded up to next 5 minutes
                val now = LocalTime.now()
                val roundedMinute = ((now.minute / 5) + 1) * 5
                val adjustedHour = if (roundedMinute >= 60) (now.hour + 1) % 24 else now.hour
                _uiState.value = AlarmEditUiState(
                    hour = adjustedHour,
                    minute = roundedMinute % 60,
                    is24HourFormat = is24h
                )
            }
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    fun updateLabel(label: String) {
        _uiState.value = _uiState.value.copy(label = label)
    }

    fun toggleDay(day: DayOfWeek) {
        val current = _uiState.value.repeatDays
        _uiState.value = _uiState.value.copy(
            repeatDays = if (day in current) current - day else current + day
        )
    }

    fun updateVibration(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }

    fun updateVolume(volume: Int) {
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun updateGradualVolume(seconds: Int) {
        _uiState.value = _uiState.value.copy(gradualVolumeSeconds = seconds)
    }

    fun updateOverrideVolume(override: Boolean) {
        _uiState.value = _uiState.value.copy(overrideSystemVolume = override)
    }

    fun updateSnoozeDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(snoozeDurationMinutes = minutes)
    }

    fun updateChallengeType(type: String) {
        _uiState.value = _uiState.value.copy(challengeType = type)
    }

    fun updateRingtoneUri(uri: String) {
        _uiState.value = _uiState.value.copy(ringtoneUri = uri)
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val s = _uiState.value

            val alarm = Alarm(
                id = if (s.isEditing) alarmId else 0,
                hour = s.hour,
                minute = s.minute,
                label = s.label,
                isEnabled = if (s.isEditing) s.isEnabled else true,
                repeatDays = s.repeatDays,
                ringtoneUri = s.ringtoneUri,
                vibrationEnabled = s.vibrationEnabled,
                vibrationIntensity = s.vibrationIntensity,
                volume = s.volume,
                overrideSystemVolume = s.overrideSystemVolume,
                gradualVolumeSeconds = s.gradualVolumeSeconds,
                snoozeDurationMinutes = s.snoozeDurationMinutes,
                maxSnoozeCount = s.maxSnoozeCount,
                showOnLockScreen = s.showOnLockScreen,
                challengeType = s.challengeType,
                createdAt = if (s.isEditing && s.createdAt > 0) s.createdAt else System.currentTimeMillis()
            )

            val savedId = repository.save(alarm)
            val savedAlarm = alarm.copy(
                id = if (s.isEditing) alarmId else savedId
            )
            if (savedAlarm.isEnabled) {
                scheduler.schedule(savedAlarm)
            }
            onComplete()
        }
    }
}
