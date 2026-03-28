package com.sysadmindoc.alarmclock.ui.bedtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.service.SleepSoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BedtimeUiState(
    val isEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val sleepGoalHours: Int = 8,
    val sleepGoalMinutes: Int = 0,
    val nextAlarmTime: String = "",
    val suggestedBedtime: String = "",
    val sleepDeficit: String = "",
    val reminderMinutesBefore: Int = 30,
    val bedtimeFormatted: String = "11:00 PM",
    val wakeTimeFormatted: String = "",
    val sleepDurationFormatted: String = "8h 0m",
    val is24HourFormat: Boolean = false,
    // F9: Sleep cycle calculator — list of formatted optimal sleep times
    val sleepCycleOptions: List<String> = emptyList(),
    // F10: Sleep sounds
    val activeSoundResId: Int = 0,        // 0 = stopped
    val sleepSoundFadeMinutes: Int = 30   // 0 = no fade
)

@HiltViewModel
class BedtimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // F10: Sleep sound player
    private val sleepSoundPlayer = SleepSoundPlayer(context)

    private val _uiState = MutableStateFlow(BedtimeUiState())
    val uiState: StateFlow<BedtimeUiState> = _uiState.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            _uiState.value = BedtimeUiState(
                isEnabled = settings.bedtimeEnabled,
                bedtimeHour = settings.bedtimeHour,
                bedtimeMinute = settings.bedtimeMinute,
                sleepGoalHours = settings.sleepGoalHours,
                sleepGoalMinutes = settings.sleepGoalMinutes,
                reminderMinutesBefore = settings.bedtimeReminderMinutes,
                bedtimeFormatted = formatTime(settings.bedtimeHour, settings.bedtimeMinute, settings.is24HourFormat),
                sleepDurationFormatted = "${settings.sleepGoalHours}h ${settings.sleepGoalMinutes}m",
                is24HourFormat = settings.is24HourFormat
            )
            refreshAlarmInfo()
        }
    }

    private suspend fun refreshAlarmInfo() {
        val current = _uiState.value
        val nextAlarm = repository.getNextAlarm()

        if (nextAlarm != null && nextAlarm.nextTriggerTime > System.currentTimeMillis()) {
            val wakeTime = java.time.Instant.ofEpochMilli(nextAlarm.nextTriggerTime)
                .atZone(ZoneId.systemDefault()).toLocalTime()
            val wakeFormatted = wakeTime.format(DateTimeFormatter.ofPattern("h:mm a"))

            val sleepMinutes = current.sleepGoalHours * 60 + current.sleepGoalMinutes
            val suggestedBedtime = wakeTime.minusMinutes(sleepMinutes.toLong())
            val suggestedFormatted = suggestedBedtime.format(DateTimeFormatter.ofPattern("h:mm a"))

            // F9: Compute sleep cycle options (90-min cycles, 15 min to fall asleep)
            val cycles = computeSleepCycles(wakeTime, current.is24HourFormat)

            _uiState.value = current.copy(
                nextAlarmTime = "Next alarm: $wakeFormatted",
                wakeTimeFormatted = wakeFormatted,
                suggestedBedtime = suggestedFormatted,
                sleepCycleOptions = cycles
            )
        } else {
            _uiState.value = current.copy(
                nextAlarmTime = "No alarm set",
                wakeTimeFormatted = "",
                suggestedBedtime = "",
                sleepCycleOptions = emptyList()
            )
        }
    }

    /**
     * F9: Compute optimal sleep times based on 90-minute sleep cycles.
     * Formula: wake_time - N * 90 minutes - 15 minutes (avg fall-asleep time)
     * Returns 4 options for N = 5, 4, 3, 2 cycles (7.5h, 6h, 4.5h, 3h).
     */
    private fun computeSleepCycles(wakeTime: LocalTime, is24h: Boolean): List<String> {
        val pattern = if (is24h) "HH:mm" else "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return (5 downTo 2).map { cycles ->
            val totalMinutes = cycles * 90 + 15
            val sleepTime = wakeTime.minusMinutes(totalMinutes.toLong())
            "${sleepTime.format(formatter)} (${cycles * 90 / 60}h ${cycles * 90 % 60}m)"
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEnabled = enabled)
        persistAndSchedule()
    }

    fun updateBedtime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(
            bedtimeHour = hour,
            bedtimeMinute = minute,
            bedtimeFormatted = formatTime(hour, minute, _uiState.value.is24HourFormat)
        )
        persistAndSchedule()
    }

    fun updateSleepGoal(hours: Int, minutes: Int) {
        _uiState.value = _uiState.value.copy(
            sleepGoalHours = hours,
            sleepGoalMinutes = minutes,
            sleepDurationFormatted = "${hours}h ${minutes}m"
        )
        viewModelScope.launch {
            persistSettings()
            refreshAlarmInfo()
        }
    }

    fun updateReminderMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(reminderMinutesBefore = minutes)
        persistAndSchedule()
    }

    private fun persistAndSchedule() {
        viewModelScope.launch {
            persistSettings()
            if (_uiState.value.isEnabled) {
                scheduleBedtimeReminder()
            } else {
                cancelBedtimeReminder()
            }
        }
    }

    private suspend fun persistSettings() {
        val s = _uiState.value
        preferencesManager.update {
            it.copy(
                bedtimeEnabled = s.isEnabled,
                bedtimeHour = s.bedtimeHour,
                bedtimeMinute = s.bedtimeMinute,
                sleepGoalHours = s.sleepGoalHours,
                sleepGoalMinutes = s.sleepGoalMinutes,
                bedtimeReminderMinutes = s.reminderMinutesBefore
            )
        }
    }

    private fun scheduleBedtimeReminder() {
        val state = _uiState.value
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = ZonedDateTime.now()
        var bedtime = now.with(LocalTime.of(state.bedtimeHour, state.bedtimeMinute))
            .minusMinutes(state.reminderMinutesBefore.toLong())

        if (bedtime.isBefore(now)) {
            bedtime = bedtime.plusDays(1)
        }

        val intent = Intent("com.sysadmindoc.alarmclock.BEDTIME_REMINDER")
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            bedtime.toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    private fun cancelBedtimeReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.sysadmindoc.alarmclock.BEDTIME_REMINDER")
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // F10: Sleep sound controls
    fun playSound(rawResId: Int) {
        val fadeMinutes = _uiState.value.sleepSoundFadeMinutes
        sleepSoundPlayer.play(rawResId, fadeMinutes)
        _uiState.value = _uiState.value.copy(activeSoundResId = rawResId)
    }

    fun stopSound() {
        sleepSoundPlayer.stop()
        _uiState.value = _uiState.value.copy(activeSoundResId = 0)
    }

    fun setSleepSoundFade(minutes: Int) {
        _uiState.value = _uiState.value.copy(sleepSoundFadeMinutes = minutes)
    }

    override fun onCleared() {
        sleepSoundPlayer.release()
        super.onCleared()
    }

    private fun formatTime(hour: Int, minute: Int, is24h: Boolean = false): String {
        return if (is24h) {
            "${String.format("%02d", hour)}:${String.format("%02d", minute)}"
        } else {
            val h = if (hour % 12 == 0) 12 else hour % 12
            val amPm = if (hour < 12) "AM" else "PM"
            "$h:${String.format("%02d", minute)} $amPm"
        }
    }
}
