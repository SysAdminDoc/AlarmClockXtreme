package com.sysadmindoc.alarmclock.ui.bedtime

import com.sysadmindoc.alarmclock.R
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepRepository
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.local.entity.SnoreEvent
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.domain.ChronotypeEstimator
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.PreSleepTagRepository
import com.sysadmindoc.alarmclock.data.repository.SnoreEventRepository
import com.sysadmindoc.alarmclock.domain.EnvironmentalNoiseBaselinePolicy
import com.sysadmindoc.alarmclock.domain.JetLagDirection
import com.sysadmindoc.alarmclock.domain.JetLagPlan
import com.sysadmindoc.alarmclock.domain.JetLagPlanner
import com.sysadmindoc.alarmclock.domain.PreSleepTagAnalytics
import com.sysadmindoc.alarmclock.domain.PreSleepTagCorrelation
import com.sysadmindoc.alarmclock.domain.PreSleepTags
import com.sysadmindoc.alarmclock.domain.SleepNoisePreset
import com.sysadmindoc.alarmclock.receiver.BedtimeReceiver
import com.sysadmindoc.alarmclock.service.BedtimeZenRuleManager
import com.sysadmindoc.alarmclock.service.BedtimeNoiseBaselineSampler
import com.sysadmindoc.alarmclock.service.BedtimeNoiseBaselineSnapshot
import com.sysadmindoc.alarmclock.service.SonarSleepSnapshot
import com.sysadmindoc.alarmclock.service.SonarSleepService
import com.sysadmindoc.alarmclock.service.SleepSoundPlayer
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

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
    val healthConnectSleepSummary: HealthConnectSleepSummary = HealthConnectSleepSummary(),
    // v1.15.0: stay up late tonight. Shows how long the override is active.
    val stayUpLateUntilMillis: Long = 0,
    val stayUpLateActive: Boolean = false,
    val stayUpLateLabel: String = "",
    val batteryPercent: Int = -1,
    val batteryLow: Boolean = false,
    val noiseBaselineLabel: String = "No baseline",
    val noiseBaselineHelper: String = "Checks at reminder",
    val chronotypeAnswers: List<Int?> = List(ChronotypeEstimator.QUESTION_COUNT) { null },
    val chronotypeAnsweredCount: Int = 0,
    val chronotypeCategoryLabel: String = "Not set",
    val chronotypeTimingLabel: String = "Answer 5 prompts",
    val chronotypeHelper: String = "Private estimate",
    val chronotypeComplete: Boolean = false,
    val jetLagTargetWakeMinutes: Int = 8 * 60,
    val jetLagAdjustmentDays: Int = 4,
    val jetLagDirection: JetLagDirection = JetLagDirection.AUTO,
    val jetLagPlan: JetLagPlan = JetLagPlanner.plan(
        currentWakeMinutes = 7 * 60,
        targetWakeMinutes = 8 * 60,
        sleepGoalMinutes = 8 * 60,
        adjustmentDays = 4,
        direction = JetLagDirection.AUTO
    ),
    val sonarTrackingActive: Boolean = false,
    val sonarTrackingStatus: String = "Ready to monitor local movement during sleep.",
    val sonarLastSessionLabel: String = "",
    val snoreTimeline: List<SnoreTimelineItem> = emptyList(),
    val preSleepTagDateLabel: String = "Tonight",
    val preSleepTags: List<PreSleepTagTile> = PreSleepTags.all.map {
        PreSleepTagTile(
            key = it.key,
            label = it.label,
            helper = it.helper,
            selected = false
        )
    },
    val preSleepCorrelations: List<PreSleepCorrelationItem> = emptyList()
)

data class PreSleepTagTile(
    val key: String,
    val label: String,
    val helper: String,
    val selected: Boolean
)

data class PreSleepCorrelationItem(
    val key: String,
    val label: String,
    val nightsLabel: String,
    val deltaLabel: String,
    val deltaMinutes: Int?,
    val averageRestlessMinutes: Int?
)

data class SnoreTimelineItem(
    val id: Long,
    val timeLabel: String,
    val intensityLabel: String,
    val durationLabel: String
)

private data class ChronotypeUiModel(
    val answers: List<Int?>,
    val answeredCount: Int,
    val categoryLabel: String,
    val timingLabel: String,
    val helper: String,
    val complete: Boolean
)

@HiltViewModel
class BedtimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val healthConnectSleepRepository: HealthConnectSleepRepository,
    private val preSleepTagRepository: PreSleepTagRepository,
    private val snoreEventRepository: SnoreEventRepository
) : ViewModel() {

    // F10: Sleep sound player
    private val sleepSoundPlayer = SleepSoundPlayer()

    private val _uiState = MutableStateFlow(BedtimeUiState())
    val uiState: StateFlow<BedtimeUiState> = _uiState.asStateFlow()
    private var sonarStartConfirmationJob: Job? = null

    init {
        loadPersistedState()
        observePreSleepTags()
        observeSnoreTimeline()
    }

    private fun loadPersistedState() {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            val noiseSnapshot = BedtimeNoiseBaselineSampler.readSnapshot(context)
            val chronotype = chronotypeUiModel(
                rawAnswers = settings.chronotypeAnswers,
                sleepGoalHours = settings.sleepGoalHours,
                sleepGoalMinutes = settings.sleepGoalMinutes,
                is24h = settings.is24HourFormat
            )
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
                healthConnectEnabled = settings.healthConnectEnabled,
                stayUpLateUntilMillis = settings.bedtimeStayUpLateUntilMillis,
                stayUpLateActive = settings.bedtimeStayUpLateUntilMillis > System.currentTimeMillis(),
                stayUpLateLabel = formatStayUpLateLabel(settings.bedtimeStayUpLateUntilMillis, settings.is24HourFormat),
                batteryPercent = getBatteryPercent(),
                batteryLow = getBatteryPercent() in 1..15,
                noiseBaselineLabel = noiseBaselineLabel(noiseSnapshot),
                noiseBaselineHelper = noiseBaselineHelper(noiseSnapshot, settings.is24HourFormat),
                chronotypeAnswers = chronotype.answers,
                chronotypeAnsweredCount = chronotype.answeredCount,
                chronotypeCategoryLabel = chronotype.categoryLabel,
                chronotypeTimingLabel = chronotype.timingLabel,
                chronotypeHelper = chronotype.helper,
                chronotypeComplete = chronotype.complete,
                jetLagTargetWakeMinutes = settings.jetLagTargetWakeMinutes,
                jetLagAdjustmentDays = settings.jetLagAdjustmentDays,
                jetLagDirection = JetLagDirection.fromKey(settings.jetLagDirection)
            ).withJetLagPlan()
            refreshSonarTrackingStatus()
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
            val wakeMinutes = wakeTime.hour * 60 + wakeTime.minute

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
                ).withJetLagPlan(wakeMinutes)
            }
        } else {
            _uiState.update {
                it.copy(
                    nextAlarmTime = "No alarm set",
                    wakeTimeFormatted = "",
                    suggestedBedtime = "",
                    sleepCycleOptions = emptyList()
                ).withJetLagPlan()
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
        val formatter = AlarmTimeFormatter.formatter(is24h)
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
        val linkedWake = _uiState.value.jetLagPlan.currentWakeMinutes
            .takeIf { _uiState.value.wakeTimeFormatted.isNotBlank() }
        _uiState.value = _uiState.value.copy(
            bedtimeHour = hour,
            bedtimeMinute = minute,
            bedtimeFormatted = formatTime(hour, minute, _uiState.value.is24HourFormat)
        ).withJetLagPlan(linkedWake)
        persistAndSchedule()
    }

    fun updateSleepGoal(hours: Int, minutes: Int) {
        // Only keep the previous wake anchor while an alarm actually links one;
        // otherwise let withJetLagPlan() re-infer the wake from bedtime + goal.
        val linkedWake = _uiState.value.jetLagPlan.currentWakeMinutes
            .takeIf { _uiState.value.wakeTimeFormatted.isNotBlank() }
        _uiState.value = _uiState.value.copy(
            sleepGoalHours = hours,
            sleepGoalMinutes = minutes,
            sleepDurationFormatted = "${hours}h ${minutes}m"
        ).withJetLagPlan(linkedWake)
        refreshChronotypeRecommendation()
        viewModelScope.launch {
            persistSettings()
            refreshAlarmInfo()
        }
    }

    fun updateReminderMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(reminderMinutesBefore = minutes)
        persistAndSchedule()
    }

    fun updateJetLagTargetWake(hour: Int, minute: Int) {
        val targetWake = (hour * 60 + minute).coerceIn(0, 1_439)
        val currentWake = _uiState.value.jetLagPlan.currentWakeMinutes
        _uiState.update {
            it.copy(jetLagTargetWakeMinutes = targetWake).withJetLagPlan(currentWake)
        }
        viewModelScope.launch {
            preferencesManager.update { it.copy(jetLagTargetWakeMinutes = targetWake) }
        }
    }

    fun updateJetLagAdjustmentDays(days: Int) {
        val clamped = days.coerceIn(1, 14)
        val currentWake = _uiState.value.jetLagPlan.currentWakeMinutes
        _uiState.update {
            it.copy(jetLagAdjustmentDays = clamped).withJetLagPlan(currentWake)
        }
        viewModelScope.launch {
            preferencesManager.update { it.copy(jetLagAdjustmentDays = clamped) }
        }
    }

    fun updateJetLagDirection(direction: JetLagDirection) {
        val currentWake = _uiState.value.jetLagPlan.currentWakeMinutes
        _uiState.update {
            it.copy(jetLagDirection = direction).withJetLagPlan(currentWake)
        }
        viewModelScope.launch {
            preferencesManager.update { it.copy(jetLagDirection = direction.storageKey) }
        }
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
                chronotypeAnswers = ChronotypeEstimator.encodeAnswers(s.chronotypeAnswers),
                jetLagTargetWakeMinutes = s.jetLagTargetWakeMinutes,
                jetLagAdjustmentDays = s.jetLagAdjustmentDays,
                jetLagDirection = s.jetLagDirection.storageKey,
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

        val now = ZonedDateTime.now()
        var bedtime = now.with(LocalTime.of(state.bedtimeHour, state.bedtimeMinute))
            .minusMinutes(state.reminderMinutesBefore.toLong())

        if (bedtime.isBefore(now)) {
            bedtime = bedtime.plusDays(1)
        }

        // v1.15.0: If a "stay up late" override is active and the computed
        // reminder time falls before the override deadline, push the reminder
        // to the override deadline. This effectively delays tonight's bedtime
        // reminder without changing the configured bedtime permanently.
        val stayUpUntil = state.stayUpLateUntilMillis
        if (stayUpUntil > 0) {
            val bedtimeMillis = bedtime.toInstant().toEpochMilli()
            if (bedtimeMillis < stayUpUntil) {
                bedtime = java.time.Instant.ofEpochMilli(stayUpUntil)
                    .atZone(ZoneId.systemDefault())
            }
        }

        BedtimeReceiver.schedule(context, bedtime.toInstant().toEpochMilli())
    }

    private fun cancelBedtimeReminder() {
        BedtimeReceiver.cancelScheduled(context)
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
            refreshNoiseBaselineStatus()
            refreshSonarTrackingStatus()
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

    fun startSonarTracking() {
        sonarStartConfirmationJob?.cancel()
        if (!hasRecordAudioPermission()) {
            _uiState.update {
                it.copy(
                    sonarTrackingActive = false,
                    sonarTrackingStatus = "Grant microphone permission to use local sonar sleep tracking."
                )
            }
            return
        }

        val intent = Intent(context, SonarSleepService::class.java)
            .setAction(SonarSleepService.ACTION_START)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.onSuccess {
            _uiState.update {
                it.copy(
                    sonarTrackingActive = false,
                    sonarTrackingStatus = "Starting sonar and confirming microphone monitoring."
                )
            }
            confirmSonarTrackingStarted()
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    sonarTrackingActive = false,
                    sonarTrackingStatus = "Sonar could not start: ${error.message ?: context.getString(R.string.bedtime_service_unavailable)}"
                )
            }
        }
    }

    fun stopSonarTracking() {
        sonarStartConfirmationJob?.cancel()
        val intent = Intent(context, SonarSleepService::class.java)
            .setAction(SonarSleepService.ACTION_STOP)
        runCatching { context.startService(intent) }
            .onSuccess {
                _uiState.update {
                    it.copy(
                        sonarTrackingActive = false,
                        sonarTrackingStatus = "Stopping sonar tracking and saving a local summary."
                    )
                }
                refreshSonarTrackingStatusAfterStop()
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        sonarTrackingStatus = "Sonar could not stop cleanly: ${error.message ?: context.getString(R.string.bedtime_service_unavailable)}"
                    )
                }
            }
    }

    fun noteSonarPermissionDenied() {
        sonarStartConfirmationJob?.cancel()
        _uiState.update {
            it.copy(
                sonarTrackingActive = false,
                sonarTrackingStatus = "Microphone permission was denied. Sonar stays off."
            )
        }
    }

    fun refreshSonarTrackingStatus() {
        val snapshot = SonarSleepService.readSnapshot(context)
        _uiState.update {
            it.copy(
                sonarTrackingActive = snapshot.active,
                sonarTrackingStatus = when {
                    snapshot.active -> context.getString(R.string.bedtime_monitoring_movement_and_loud_sleep_sounds)
                    snapshot.lastEndedAt > 0L -> context.getString(R.string.bedtime_last_sonar_session_saved_locally)
                    else -> context.getString(R.string.bedtime_ready_to_monitor_local_movement_during)
                },
                sonarLastSessionLabel = sonarLastSessionLabel(snapshot)
            )
        }
    }

    /**
     * Takes the room measurement, then shows it.
     *
     * Sampling belongs here because this is the only moment the app is in the
     * foreground and allowed to hear anything: a broadcast receiver is handed
     * silence from API 30. But `sampleAndPersist` opens an AudioRecord and
     * busy-reads it against a 700 ms deadline, so it goes on the IO dispatcher.
     * Its caller resumes on Main, and this runs on every ON_RESUME of the
     * Bedtime screen, which is exactly where a 700 ms block is a visible freeze.
     */
    private suspend fun refreshNoiseBaselineStatus() {
        withContext(Dispatchers.IO) {
            BedtimeNoiseBaselineSampler.sampleAndPersist(context)
        }
        val snapshot = BedtimeNoiseBaselineSampler.readSnapshot(context)
        _uiState.update {
            it.copy(
                noiseBaselineLabel = noiseBaselineLabel(snapshot),
                noiseBaselineHelper = noiseBaselineHelper(snapshot, it.is24HourFormat)
            )
        }
    }

    /**
     * The service tears its recorder down and writes its summary after the
     * stop intent returns, so poll a few times until it reports itself down.
     *
     * This used to loop while the status still began "Stopping sonar", which
     * never held: refreshSonarTrackingStatus overwrites that status with one
     * of three other sentences every time it runs, so the check was false on
     * the first pass and the remaining two attempts never happened. Reading
     * the flag the service owns is both correct and immune to the copy being
     * reworded or translated.
     */
    private fun refreshSonarTrackingStatusAfterStop() {
        viewModelScope.launch {
            repeat(3) {
                delay(1_000L)
                refreshSonarTrackingStatus()
                if (!_uiState.value.sonarTrackingActive) return@launch
            }
        }
    }

    private fun confirmSonarTrackingStarted() {
        sonarStartConfirmationJob = viewModelScope.launch {
            repeat(SONAR_START_CONFIRMATION_ATTEMPTS) { attempt ->
                delay(SONAR_START_CONFIRMATION_DELAY_MS)
                val snapshot = SonarSleepService.readSnapshot(context)
                when (
                    sonarStartConfirmation(
                        snapshotActive = snapshot.active,
                        attemptsRemaining = SONAR_START_CONFIRMATION_ATTEMPTS - attempt - 1
                    )
                ) {
                    SonarStartConfirmation.MONITORING -> {
                        _uiState.update {
                            it.copy(
                                sonarTrackingActive = true,
                                sonarTrackingStatus = "Monitoring movement and loud sleep sounds. No raw audio is recorded.",
                                sonarLastSessionLabel = sonarLastSessionLabel(snapshot)
                            )
                        }
                        return@launch
                    }
                    SonarStartConfirmation.WAITING -> Unit
                    SonarStartConfirmation.FAILED -> {
                        _uiState.update {
                            it.copy(
                                sonarTrackingActive = false,
                                sonarTrackingStatus = "Sonar could not confirm microphone monitoring. Try again."
                            )
                        }
                        return@launch
                    }
                }
            }
        }
    }

    private fun observeSnoreTimeline() {
        viewModelScope.launch {
            snoreEventRepository.observeRecent(limit = 6).collect { events ->
                _uiState.update {
                    it.copy(snoreTimeline = events.map(::snoreTimelineItem))
                }
            }
        }
    }

    private fun observePreSleepTags() {
        val tagDate = currentPreSleepTagDate()
        viewModelScope.launch {
            preSleepTagRepository.observeForDate(tagDate).collect { rows ->
                val selected = rows.map { it.tagKey }.toSet()
                _uiState.update {
                    it.copy(
                        preSleepTagDateLabel = preSleepDateLabel(tagDate),
                        preSleepTags = PreSleepTags.all.map { tag ->
                            PreSleepTagTile(
                                key = tag.key,
                                label = tag.label,
                                helper = tag.helper,
                                selected = tag.key in selected
                            )
                        }
                    )
                }
                refreshPreSleepCorrelations(tagDate)
            }
        }
        viewModelScope.launch {
            refreshPreSleepCorrelations(tagDate)
        }
    }

    fun togglePreSleepTag(tagKey: String) {
        val selected = _uiState.value.preSleepTags.firstOrNull { it.key == tagKey }?.selected ?: false
        val tagDate = currentPreSleepTagDate()
        viewModelScope.launch {
            preSleepTagRepository.setTag(
                localDate = tagDate,
                tagKey = tagKey,
                selected = !selected
            )
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

    fun updateChronotypeAnswer(questionIndex: Int, answerIndex: Int) {
        val current = _uiState.value
        val encoded = ChronotypeEstimator.withAnswer(
            raw = ChronotypeEstimator.encodeAnswers(current.chronotypeAnswers),
            questionIndex = questionIndex,
            answerIndex = answerIndex
        )
        val chronotype = chronotypeUiModel(
            rawAnswers = encoded,
            sleepGoalHours = current.sleepGoalHours,
            sleepGoalMinutes = current.sleepGoalMinutes,
            is24h = current.is24HourFormat
        )
        _uiState.update { it.copyChronotype(chronotype) }
        viewModelScope.launch {
            preferencesManager.update { it.copy(chronotypeAnswers = encoded) }
        }
    }

    /**
     * v1.15.0: Delay tonight's bedtime reminder by [hours] hours from now.
     * The override is stored as an epoch-millis deadline; once it expires
     * the next reminder fires at the normal configured time.
     */
    fun stayUpLate(hours: Int) {
        val until = System.currentTimeMillis() + hours * 3_600_000L
        _uiState.update {
            it.copy(
                stayUpLateUntilMillis = until,
                stayUpLateActive = true,
                stayUpLateLabel = formatStayUpLateLabel(until, it.is24HourFormat)
            )
        }
        viewModelScope.launch {
            preferencesManager.update { it.copy(bedtimeStayUpLateUntilMillis = until) }
            if (_uiState.value.isEnabled) {
                scheduleBedtimeReminder()
            }
        }
    }

    /** Clear the stay-up-late override and re-arm at the normal time. */
    fun clearStayUpLate() {
        _uiState.update {
            it.copy(
                stayUpLateUntilMillis = 0,
                stayUpLateActive = false,
                stayUpLateLabel = ""
            )
        }
        viewModelScope.launch {
            preferencesManager.update { it.copy(bedtimeStayUpLateUntilMillis = 0) }
            if (_uiState.value.isEnabled) {
                scheduleBedtimeReminder()
            }
        }
    }

    override fun onCleared() {
        sleepSoundPlayer.release()
        super.onCleared()
    }

    private fun formatTime(hour: Int, minute: Int, is24h: Boolean = false): String =
        AlarmTimeFormatter.format(hour, minute, is24h)

    private fun getBatteryPercent(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            ?: return -1
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun formatStayUpLateLabel(untilMillis: Long, is24h: Boolean): String {
        if (untilMillis <= System.currentTimeMillis()) return ""
        val time = java.time.Instant.ofEpochMilli(untilMillis)
            .atZone(ZoneId.systemDefault()).toLocalTime()
        return context.getString(
            R.string.bedtime_delayed_until,
            formatTime(time.hour, time.minute, is24h)
        )
    }

    private fun BedtimeUiState.withJetLagPlan(currentWakeMinutes: Int? = null): BedtimeUiState {
        val sleepMinutes = sleepGoalHours * 60 + sleepGoalMinutes
        val inferredWake = JetLagPlanner.normalizeMinuteOfDay(
            bedtimeHour * 60 + bedtimeMinute + sleepMinutes
        )
        return copy(
            jetLagPlan = JetLagPlanner.plan(
                currentWakeMinutes = currentWakeMinutes ?: inferredWake,
                targetWakeMinutes = jetLagTargetWakeMinutes,
                sleepGoalMinutes = sleepMinutes,
                adjustmentDays = jetLagAdjustmentDays,
                direction = jetLagDirection
            )
        )
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun sonarLastSessionLabel(snapshot: SonarSleepSnapshot): String {
        if (snapshot.lastEndedAt <= 0L || snapshot.lastTotalMinutes <= 0) return ""
        val snore = if (snapshot.lastSnoreEventCount > 0) {
            context.getString(
                R.string.bedtime_sonar_loud_bursts,
                snapshot.lastSnoreEventCount,
                snapshot.lastSnorePeakDb.roundToInt()
            )
        } else {
            context.getString(R.string.bedtime_sonar_no_loud_bursts)
        }
        return context.getString(
            R.string.bedtime_sonar_last_session,
            snapshot.lastTotalMinutes,
            snapshot.lastAwakeMinutes,
            snapshot.lastLightMinutes,
            snapshot.lastDeepMinutes
        ) + snore
    }

    private fun snoreTimelineItem(event: SnoreEvent): SnoreTimelineItem {
        val start = java.time.Instant.ofEpochMilli(event.startedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return SnoreTimelineItem(
            id = event.id,
            timeLabel = formatTime(start.hour, start.minute, _uiState.value.is24HourFormat),
            intensityLabel = "Peak ${event.peakDb.roundToInt()} dB est. / avg ${event.averageDb.roundToInt()}",
            durationLabel = formatDurationMillis(event.durationMillis)
        )
    }

    private fun formatDurationMillis(durationMillis: Long): String {
        val seconds = ((durationMillis + 999L) / 1_000L).coerceAtLeast(1L)
        if (seconds < 60L) return "${seconds}s"
        val minutes = seconds / 60L
        val remainder = seconds % 60L
        return if (remainder == 0L) "${minutes}m" else "${minutes}m ${remainder}s"
    }

    private suspend fun refreshPreSleepCorrelations(today: LocalDate) {
        val items = runCatching {
            preSleepTagRepository.readCorrelations(today).map(::preSleepCorrelationItem)
        }.getOrDefault(emptyList())
        _uiState.update { it.copy(preSleepCorrelations = items) }
    }

    private fun preSleepCorrelationItem(correlation: PreSleepTagCorrelation): PreSleepCorrelationItem {
        val nightsLabel = when {
            correlation.loggedNights == 0 -> context.getString(R.string.bedtime_no_tagged_nights_yet)
            correlation.nightsWithSessions == 0 -> context.getString(R.string.bedtime_tagged_waiting_for_sleep_sessions, correlation.loggedNights)
            else -> context.getString(R.string.bedtime_tagged_nights_with_local_sleep_data, correlation.nightsWithSessions, correlation.loggedNights)
        }
        val delta = correlation.deltaRestlessMinutes
        val deltaLabel = when {
            delta == null -> context.getString(R.string.bedtime_start_sonar_or_smart_wake_to)
            delta > 0 -> context.getString(R.string.bedtime_m_restless_vs_baseline, delta)
            delta < 0 -> context.getString(R.string.bedtime_m_restless_vs_baseline_2, delta)
            else -> context.getString(R.string.bedtime_matches_baseline_restlessness)
        }
        return PreSleepCorrelationItem(
            key = correlation.key,
            label = correlation.label,
            nightsLabel = nightsLabel,
            deltaLabel = deltaLabel,
            deltaMinutes = delta,
            averageRestlessMinutes = correlation.averageRestlessMinutes
        )
    }

    private fun currentPreSleepTagDate(): LocalDate {
        return PreSleepTagAnalytics.tagDateFor(System.currentTimeMillis(), ZoneId.systemDefault())
    }

    private fun preSleepDateLabel(tagDate: LocalDate): String {
        val today = LocalDate.now(ZoneId.systemDefault())
        return when (tagDate) {
            today -> context.getString(R.string.settings_tonight)
            today.minusDays(1) -> context.getString(R.string.bedtime_last_night)
            else -> tagDate.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }

    private fun noiseBaselineLabel(snapshot: BedtimeNoiseBaselineSnapshot): String {
        val baseline = snapshot.baseline
            ?: return context.getString(R.string.bedtime_noise_no_baseline)
        return context.getString(EnvironmentalNoiseBaselinePolicy.levelLabelRes(baseline.level))
    }

    private fun noiseBaselineHelper(
        snapshot: BedtimeNoiseBaselineSnapshot,
        is24h: Boolean
    ): String {
        val baseline = snapshot.baseline ?: return context.getString(
            if (hasRecordAudioPermission()) {
                R.string.bedtime_noise_checks_at_reminder
            } else {
                R.string.bedtime_noise_mic_permission_needed
            }
        )
        val measured = java.time.Instant.ofEpochMilli(snapshot.measuredAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return context.getString(
            R.string.bedtime_noise_last_measured,
            formatTime(measured.hour, measured.minute, is24h)
        )
    }

    private fun refreshChronotypeRecommendation() {
        val current = _uiState.value
        val model = chronotypeUiModel(
            rawAnswers = ChronotypeEstimator.encodeAnswers(current.chronotypeAnswers),
            sleepGoalHours = current.sleepGoalHours,
            sleepGoalMinutes = current.sleepGoalMinutes,
            is24h = current.is24HourFormat
        )
        _uiState.update { it.copyChronotype(model) }
    }

    private fun chronotypeUiModel(
        rawAnswers: String,
        sleepGoalHours: Int,
        sleepGoalMinutes: Int,
        is24h: Boolean
    ): ChronotypeUiModel {
        val estimate = ChronotypeEstimator.estimate(
            rawAnswers = rawAnswers,
            sleepGoalMinutes = sleepGoalHours * 60 + sleepGoalMinutes
        )
        val category = estimate.category
        return ChronotypeUiModel(
            answers = estimate.answers,
            answeredCount = estimate.answeredCount,
            categoryLabel = category?.let(ChronotypeEstimator::categoryLabel) ?: context.getString(R.string.bedtime_not_set),
            timingLabel = if (
                estimate.idealBedtimeMinutes != null &&
                estimate.idealWakeMinutes != null
            ) {
                "${formatMinuteOfDay(estimate.idealBedtimeMinutes, is24h)} - " +
                    "${formatMinuteOfDay(estimate.idealWakeMinutes, is24h)}"
            } else {
                "${estimate.answeredCount}/${ChronotypeEstimator.QUESTION_COUNT} answered"
            },
            helper = when (category) {
                null -> context.getString(R.string.bedtime_local_estimate)
                else -> context.getString(R.string.bedtime_fits_sleep_target, formatSleepGoal(sleepGoalHours, sleepGoalMinutes))
            },
            complete = estimate.isComplete
        )
    }

    private fun BedtimeUiState.copyChronotype(model: ChronotypeUiModel): BedtimeUiState = copy(
        chronotypeAnswers = model.answers,
        chronotypeAnsweredCount = model.answeredCount,
        chronotypeCategoryLabel = model.categoryLabel,
        chronotypeTimingLabel = model.timingLabel,
        chronotypeHelper = model.helper,
        chronotypeComplete = model.complete
    )

    private fun formatMinuteOfDay(minutes: Int, is24h: Boolean): String {
        val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        return formatTime(
            hour = normalized / 60,
            minute = normalized % 60,
            is24h = is24h
        )
    }

    private fun formatSleepGoal(hours: Int, minutes: Int): String {
        return when {
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
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

    private companion object {
        const val SONAR_START_CONFIRMATION_ATTEMPTS = 12
        const val SONAR_START_CONFIRMATION_DELAY_MS = 250L
    }
}
