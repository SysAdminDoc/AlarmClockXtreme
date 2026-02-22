package com.sysadmindoc.alarmclock.ui.bedtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
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
    val sleepDurationFormatted: String = "8h 0m"
)

@HiltViewModel
class BedtimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

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
                bedtimeFormatted = formatTime(settings.bedtimeHour, settings.bedtimeMinute),
                sleepDurationFormatted = "${settings.sleepGoalHours}h ${settings.sleepGoalMinutes}m"
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

            _uiState.value = current.copy(
                nextAlarmTime = "Next alarm: $wakeFormatted",
                wakeTimeFormatted = wakeFormatted,
                suggestedBedtime = suggestedFormatted
            )
        } else {
            _uiState.value = current.copy(
                nextAlarmTime = "No alarm set",
                wakeTimeFormatted = "",
                suggestedBedtime = ""
            )
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
            bedtimeFormatted = formatTime(hour, minute)
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

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        return "$h:${String.format("%02d", minute)} $amPm"
    }
}
