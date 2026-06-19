package com.sysadmindoc.alarmclock.ui.bedtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepRepository
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.SleepNoisePreset
import com.sysadmindoc.alarmclock.service.BedtimeZenRuleManager
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
    val activeSoundKey: String = "",      // blank = stopped
    val sleepSoundFadeMinutes: Int = 30,  // Total timer; 0 = no fade
    val sleepSoundFadeSeconds: Int = 60,  // v1.4.0: length of the final taper
    // v1.4.0: Pre-sleep checklist (newline-separated wind-down items)
    val bedtimeChecklist: List<String> = emptyList(),
    val bedtimeChecklistDone: Set<Int> = emptySet(),
    // v1.10.5: App-owned alarms-only DND rule for the sleep window.
    val bedtimeDndEnabled: Boolean = false,
    val bedtimeDndAccessGranted: Boolean = false,
    val bedtimeDndActive: Boolean = false,
    val bedtimeDndStatus: String = "Off",
    val bedtimeDndError: String? = null,
    val healthConnectEnabled: Boolean = false,
    val healthConnectSleepSummary: HealthConnectSleepSummary = HealthConnectSleepSummary()
)

@HiltViewModel
class BedtimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val healthConnectSleepRepository: HealthConnectSleepRepository
) : ViewModel() {

    // F10: Sleep sound player
    private val sleepSoundPlayer = SleepSoundPlayer()

    private val _uiState = MutableStateFlow(BedtimeUiState())
    val uiState: StateFlow<BedtimeUiState> = _uiState.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            val checklistItems = settings.bedtimeChecklist
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            _uiState.value = BedtimeUiState(
                isEnabled = settings.bedtimeEnabled,
                bedtimeHour = settings.bedtimeHour,
                bedtimeMinute = settings.bedtimeMinute,
                sleepGoalHours = settings.sleepGoalHours,
                sleepGoalMinutes = settings.sleepGoalMinutes,
                reminderMinutesBefore = settings.bedtimeReminderMinutes,
                bedtimeFormatted = formatTime(settings.bedtimeHour, settings.bedtimeMinute, settings.is24HourFormat),
                sleepDurationFormatted = "${settings.sleepGoalHours}h ${settings.sleepGoalMinutes}m",
                is24HourFormat = settings.is24HourFormat,
                sleepSoundFadeMinutes = if (settings.sleepSoundTimerMinutes > 0) settings.sleepSoundTimerMinutes else 30,
                sleepSoundFadeSeconds = settings.sleepSoundFadeSeconds.coerceIn(5, 600),
                bedtimeChecklist = checklistItems,
                bedtimeDndEnabled = settings.bedtimeDndEnabled,
                healthConnectEnabled = settings.healthConnectEnabled
            )
            refreshAlarmInfo()
            refreshHealthConnectSleep()
        }
    }

    private suspend fun refreshAlarmInfo() {
        val nextAlarm = repository.getNextAlarm()

        if (nextAlarm != null && nextAlarm.nextTriggerTime > System.currentTimeMillis()) {
            val wakeTime = java.time.Instant.ofEpochMilli(nextAlarm.nextTriggerTime)
                .atZone(ZoneId.systemDefault()).toLocalTime()
            val wakeFormatted = formatTime(
                hour = wakeTime.hour,
                minute = wakeTime.minute,
                is24h = _uiState.value.is24HourFormat
            )

            val sleepMinutes = _uiState.value.sleepGoalHours * 60 + _uiState.value.sleepGoalMinutes
            val suggestedBedtime = wakeTime.minusMinutes(sleepMinutes.toLong())
            val suggestedFormatted = formatTime(
                hour = suggestedBedtime.hour,
                minute = suggestedBedtime.minute,
                is24h = _uiState.value.is24HourFormat
            )

            // F9: Compute sleep cycle options (90-min cycles, 15 min to fall asleep)
            val cycles = computeSleepCycles(wakeTime, _uiState.value.is24HourFormat)

            _uiState.update {
                it.copy(
                    nextAlarmTime = "Next alarm: $wakeFormatted",
                    wakeTimeFormatted = wakeFormatted,
                    suggestedBedtime = suggestedFormatted,
                    sleepCycleOptions = cycles
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    nextAlarmTime = "No alarm set",
                    wakeTimeFormatted = "",
                    suggestedBedtime = "",
                    sleepCycleOptions = emptyList()
                )
            }
        }
        syncBedtimeDndRule(nextAlarm?.nextTriggerTime)
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
            syncBedtimeDndRule()
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
                bedtimeReminderMinutes = s.reminderMinutesBefore,
                bedtimeDndEnabled = s.bedtimeDndEnabled
            )
        }
        // Mirror the enabled flag into a synchronous SharedPreferences key so
        // BedtimeReceiver.onReceive() — which can't suspend on DataStore — knows
        // whether to re-arm tomorrow's reminder. Without this, disabling bedtime
        // in the UI would still leave the reminder rescheduling itself forever.
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("bedtime_reschedule", s.isEnabled)
            .apply()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

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

    // F10: Sleep sound controls. v1.4.0 — honours the fade-duration setting
    // so users can choose a slower taper than the previous hard-coded 60s.
    fun playSound(preset: SleepNoisePreset) {
        val fadeMinutes = _uiState.value.sleepSoundFadeMinutes
        val fadeSeconds = _uiState.value.sleepSoundFadeSeconds
        sleepSoundPlayer.play(preset, fadeMinutes, fadeSeconds)
        _uiState.value = _uiState.value.copy(activeSoundKey = preset.key)
    }

    fun stopSound() {
        sleepSoundPlayer.stop()
        _uiState.value = _uiState.value.copy(activeSoundKey = "")
    }

    fun setSleepSoundFade(minutes: Int) {
        _uiState.value = _uiState.value.copy(sleepSoundFadeMinutes = minutes)
    }

    /** v1.5.0: Seconds-scale final-taper control, exposed directly on the
     *  Bedtime tab rather than buried in Settings. Persists so the choice
     *  survives the current sleep session. */
    fun setSleepSoundFadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 600)
        _uiState.value = _uiState.value.copy(sleepSoundFadeSeconds = clamped)
        viewModelScope.launch {
            preferencesManager.update { it.copy(sleepSoundFadeSeconds = clamped) }
        }
    }

    fun toggleBedtimeDnd(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            bedtimeDndEnabled = enabled,
            bedtimeDndError = null
        )
        viewModelScope.launch {
            preferencesManager.update { it.copy(bedtimeDndEnabled = enabled) }
            syncBedtimeDndRule()
            val finalEnabled = _uiState.value.bedtimeDndEnabled
            if (finalEnabled != enabled) {
                preferencesManager.update { it.copy(bedtimeDndEnabled = finalEnabled) }
            }
        }
    }

    fun refreshBedtimeDndStatus() {
        viewModelScope.launch {
            syncBedtimeDndRule()
            refreshHealthConnectSleep()
        }
    }

    fun refreshHealthConnectSleep() {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            val summary = if (settings.healthConnectEnabled) {
                healthConnectSleepRepository.readRecentSleepSummary()
            } else {
                HealthConnectSleepSummary()
            }
            _uiState.update {
                it.copy(
                    healthConnectEnabled = settings.healthConnectEnabled,
                    healthConnectSleepSummary = summary
                )
            }
        }
    }

    // v1.4.0: Toggle an individual pre-sleep checklist entry.
    fun toggleChecklistItem(index: Int) {
        val current = _uiState.value.bedtimeChecklistDone
        val updated = if (index in current) current - index else current + index
        _uiState.value = _uiState.value.copy(bedtimeChecklistDone = updated)
    }

    fun resetChecklist() {
        _uiState.value = _uiState.value.copy(bedtimeChecklistDone = emptySet())
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

    private suspend fun syncBedtimeDndRule(nextAlarmTriggerMillis: Long? = null) {
        val settings = preferencesManager.getCurrentSettings()
        val trigger = nextAlarmTriggerMillis ?: repository.getNextAlarm()?.nextTriggerTime
        val status = BedtimeZenRuleManager.syncRule(context, settings, trigger)
        _uiState.update {
            it.copy(
                bedtimeDndEnabled = status.enabled,
                bedtimeDndAccessGranted = status.accessGranted,
                bedtimeDndActive = status.active,
                bedtimeDndStatus = status.summary,
                bedtimeDndError = status.error
            )
        }
    }
}
