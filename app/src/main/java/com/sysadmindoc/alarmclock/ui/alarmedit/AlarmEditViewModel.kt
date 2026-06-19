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
    val group: String = "",
    val flashWake: Boolean = false,
    val vibrationPattern: String = "default",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = 0,
    val is24HourFormat: Boolean = false,
    val notFound: Boolean = false,
    // F3: TTS morning announcement
    val ttsEnabled: Boolean = false,
    // F4: Walk-steps challenge
    val walkStepsRequired: Int = 30,
    // Squat challenge
    val requiredSquats: Int = 10,
    // F5: Wake confirmation
    val wakeConfirmEnabled: Boolean = false,
    val wakeConfirmDelayMinutes: Int = 10,
    // F6: Smart alarm (light-sleep detection)
    val smartAlarmEnabled: Boolean = false,
    val smartAlarmWindowMinutes: Int = 30,
    // F13: Holiday skip
    val skipOnHolidays: Boolean = false,
    // F7: NFC tag challenge
    val nfcTagId: String = "",
    // F8: Barcode challenge
    val barcodeValue: String = "",
    // F14: Spotify ringtone
    val spotifyUri: String = "",
    // F15: Philips Hue sunrise
    val hueEnabled: Boolean = false,
    val huePreWakeMinutes: Int = 30,
    // F16: Photo match challenge
    val photoMatchUri: String = "",
    // v1.2.0 new features
    val challengeChain: String = "",
    val progressiveSnooze: Boolean = false,
    val backupSoundEnabled: Boolean = false,
    val backupSoundDelaySec: Int = 40,
    val sunriseSimulation: Boolean = false,
    val sunriseMinutes: Int = 15,
    val specificDate: String = "",
    val profileName: String = "",
    val earlyDismissMinutes: Int = 0,
    val guardianEnabled: Boolean = false,
    val guardianPhone: String = "",
    val guardianDelaySec: Int = 300,
    val locationDismissEnabled: Boolean = false,
    val locationDismissLat: Double = 0.0,
    val locationDismissLng: Double = 0.0,
    val locationDismissRadius: Int = 100,
    val wifiDismissSsid: String = "",
    val internetRadioUrl: String = "",
    val flashlightStrobe: Boolean = false,
    val morningRoutine: String = "",
    // v1.4.0: Hardware button action during firing (NONE/SNOOZE/DISMISS)
    val hardwareButtonAction: String = "NONE",
    // v1.4.0: Auto-dismiss when the chosen ringtone finishes naturally
    val dismissAtRingtoneEnd: Boolean = false,
    // v1.10.3: Require a deliberate hold before dismiss on the firing screen
    val holdToDismissEnabled: Boolean = false,
    // v1.4.0: Random ringtone pool (comma-separated URIs)
    val ringtonePool: String = "",
    // v1.5.0: Sunrise/sunset-relative firing (minutes offset; 0 = use fixed time)
    val solarOffsetMinutes: Int = 0,
    // v1.5.0: Solar-offset anchor: "SUNRISE" or "SUNSET"
    val solarAnchor: String = "SUNRISE",
    // v1.12.0 (roadmap N7): pre-vibration delay in seconds (pairs with
    // gradualVolumeSeconds for a "gentle wake" preset).
    val vibrationDelaySeconds: Int = 0,
    val weatherEarlyMinutes: Int = 0,
    val forecastDates: List<ForecastEntry> = emptyList()
)

data class ForecastEntry(
    val timeMillis: Long,
    val skippedByVacation: Boolean = false
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
                        group = alarm.group,
                        flashWake = alarm.flashWake,
                        vibrationPattern = alarm.vibrationPattern,
                        isEditing = true,
                        isEnabled = alarm.isEnabled,
                        createdAt = alarm.createdAt,
                        is24HourFormat = is24h,
                        ttsEnabled = alarm.ttsEnabled,
                        walkStepsRequired = alarm.walkStepsRequired,
                        requiredSquats = alarm.requiredSquats,
                        wakeConfirmEnabled = alarm.wakeConfirmEnabled,
                        wakeConfirmDelayMinutes = alarm.wakeConfirmDelayMinutes,
                        smartAlarmEnabled = alarm.smartAlarmEnabled,
                        smartAlarmWindowMinutes = alarm.smartAlarmWindowMinutes,
                        skipOnHolidays = alarm.skipOnHolidays,
                        nfcTagId = alarm.nfcTagId,
                        barcodeValue = alarm.barcodeValue,
                        spotifyUri = alarm.spotifyUri,
                        hueEnabled = alarm.hueEnabled,
                        huePreWakeMinutes = alarm.huePreWakeMinutes,
                        photoMatchUri = alarm.photoMatchUri,
                        challengeChain = alarm.challengeChain,
                        progressiveSnooze = alarm.progressiveSnooze,
                        backupSoundEnabled = alarm.backupSoundEnabled,
                        backupSoundDelaySec = alarm.backupSoundDelaySec,
                        sunriseSimulation = alarm.sunriseSimulation,
                        sunriseMinutes = alarm.sunriseMinutes,
                        specificDate = alarm.specificDate,
                        profileName = alarm.profileName,
                        earlyDismissMinutes = alarm.earlyDismissMinutes,
                        guardianEnabled = alarm.guardianEnabled,
                        guardianPhone = alarm.guardianPhone,
                        guardianDelaySec = alarm.guardianDelaySec,
                        locationDismissEnabled = alarm.locationDismissEnabled,
                        locationDismissLat = alarm.locationDismissLat,
                        locationDismissLng = alarm.locationDismissLng,
                        locationDismissRadius = alarm.locationDismissRadius,
                        wifiDismissSsid = alarm.wifiDismissSsid,
                        internetRadioUrl = alarm.internetRadioUrl,
                        flashlightStrobe = alarm.flashlightStrobe,
                        morningRoutine = alarm.morningRoutine,
                        hardwareButtonAction = alarm.hardwareButtonAction,
                        dismissAtRingtoneEnd = alarm.dismissAtRingtoneEnd,
                        holdToDismissEnabled = alarm.holdToDismissEnabled,
                        ringtonePool = alarm.ringtonePool,
                        solarOffsetMinutes = alarm.solarOffsetMinutes,
                        solarAnchor = alarm.solarAnchor,
                        vibrationDelaySeconds = alarm.vibrationDelaySeconds,
                        weatherEarlyMinutes = alarm.weatherEarlyMinutes
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
        _uiState.value = _uiState.value.copy(volume = volume.coerceIn(0, 100))
    }

    fun updateGradualVolume(seconds: Int) {
        _uiState.value = _uiState.value.copy(gradualVolumeSeconds = seconds)
    }

    /** v1.12.0 (roadmap N7): set per-alarm vibration start-delay (seconds). */
    fun updateVibrationDelay(seconds: Int) {
        _uiState.value = _uiState.value.copy(vibrationDelaySeconds = seconds.coerceIn(0, 600))
    }

    fun updateWeatherEarlyMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(weatherEarlyMinutes = minutes.coerceIn(0, 60))
    }

    fun updateOverrideVolume(override: Boolean) {
        _uiState.value = _uiState.value.copy(overrideSystemVolume = override)
    }

    fun applyDontWakePartnerProfile() {
        _uiState.value = _uiState.value.copy(
            ringtoneUri = "",
            overrideSystemVolume = true,
            volume = 0,
            gradualVolumeSeconds = 0,
            vibrationEnabled = true,
            vibrationIntensity = 1,
            vibrationPattern = "default",
            backupSoundEnabled = false
        )
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

    fun updateGroup(group: String) {
        _uiState.value = _uiState.value.copy(group = group)
    }

    fun updateFlashWake(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(flashWake = enabled)
    }

    fun updateVibrationPattern(pattern: String) {
        _uiState.value = _uiState.value.copy(vibrationPattern = pattern)
    }

    fun updateTtsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(ttsEnabled = enabled)
    }

    fun updateWalkSteps(steps: Int) {
        _uiState.value = _uiState.value.copy(walkStepsRequired = steps)
    }

    fun updateRequiredSquats(count: Int) {
        _uiState.value = _uiState.value.copy(requiredSquats = count)
    }

    fun updateWakeConfirm(enabled: Boolean, delayMinutes: Int? = null) {
        _uiState.value = _uiState.value.copy(
            wakeConfirmEnabled = enabled,
            wakeConfirmDelayMinutes = delayMinutes ?: _uiState.value.wakeConfirmDelayMinutes
        )
    }

    fun updateSmartAlarm(enabled: Boolean, windowMinutes: Int? = null) {
        _uiState.value = _uiState.value.copy(
            smartAlarmEnabled = enabled,
            smartAlarmWindowMinutes = (windowMinutes ?: _uiState.value.smartAlarmWindowMinutes)
                .coerceIn(0, Alarm.MAX_SMART_ALARM_WINDOW_MINUTES)
        )
    }

    fun updateSkipOnHolidays(skip: Boolean) {
        _uiState.value = _uiState.value.copy(skipOnHolidays = skip)
    }

    fun updateNfcTagId(tagId: String) {
        _uiState.value = _uiState.value.copy(nfcTagId = tagId)
    }

    fun updateBarcodeValue(value: String) {
        _uiState.value = _uiState.value.copy(barcodeValue = value)
    }

    fun updateSpotifyUri(uri: String) {
        _uiState.value = _uiState.value.copy(spotifyUri = uri)
    }

    fun updateHue(enabled: Boolean, preWakeMinutes: Int? = null) {
        _uiState.value = _uiState.value.copy(
            hueEnabled = enabled,
            huePreWakeMinutes = preWakeMinutes ?: _uiState.value.huePreWakeMinutes
        )
    }

    fun updatePhotoMatchUri(uri: String) {
        _uiState.value = _uiState.value.copy(photoMatchUri = uri)
    }

    fun updateChallengeChain(chain: String) { _uiState.value = _uiState.value.copy(challengeChain = chain) }
    fun updateProgressiveSnooze(enabled: Boolean) { _uiState.value = _uiState.value.copy(progressiveSnooze = enabled) }
    fun updateBackupSound(enabled: Boolean, delaySec: Int? = null) {
        _uiState.value = _uiState.value.copy(backupSoundEnabled = enabled, backupSoundDelaySec = delaySec ?: _uiState.value.backupSoundDelaySec)
    }
    fun updateSunriseSimulation(enabled: Boolean, minutes: Int? = null) {
        _uiState.value = _uiState.value.copy(sunriseSimulation = enabled, sunriseMinutes = minutes ?: _uiState.value.sunriseMinutes)
    }
    fun updateSpecificDate(date: String) { _uiState.value = _uiState.value.copy(specificDate = date) }
    fun updateProfileName(name: String) { _uiState.value = _uiState.value.copy(profileName = name) }
    fun updateEarlyDismiss(minutes: Int) { _uiState.value = _uiState.value.copy(earlyDismissMinutes = minutes) }
    fun updateGuardian(enabled: Boolean, phone: String? = null, delaySec: Int? = null) {
        _uiState.value = _uiState.value.copy(
            guardianEnabled = enabled,
            guardianPhone = phone ?: _uiState.value.guardianPhone,
            guardianDelaySec = delaySec ?: _uiState.value.guardianDelaySec
        )
    }
    fun updateLocationDismiss(enabled: Boolean) { _uiState.value = _uiState.value.copy(locationDismissEnabled = enabled) }
    fun updateWifiDismissSsid(ssid: String) { _uiState.value = _uiState.value.copy(wifiDismissSsid = ssid) }
    fun updateInternetRadioUrl(url: String) { _uiState.value = _uiState.value.copy(internetRadioUrl = url) }
    fun updateFlashlightStrobe(enabled: Boolean) { _uiState.value = _uiState.value.copy(flashlightStrobe = enabled) }
    fun updateMorningRoutine(routine: String) { _uiState.value = _uiState.value.copy(morningRoutine = routine) }
    // v1.4.0 setters
    fun updateHardwareButtonAction(action: String) {
        val sanitized = when (action.uppercase()) {
            "SNOOZE", "DISMISS" -> action.uppercase()
            else -> "NONE"
        }
        _uiState.value = _uiState.value.copy(hardwareButtonAction = sanitized)
    }
    fun updateDismissAtRingtoneEnd(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dismissAtRingtoneEnd = enabled)
    }
    fun updateHoldToDismiss(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(holdToDismissEnabled = enabled)
    }
    fun updateRingtonePool(pool: String) {
        // Normalise: trim each URI, drop blanks, dedupe while preserving order.
        val cleaned = pool.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        _uiState.value = _uiState.value.copy(ringtonePool = cleaned)
    }
    fun addRingtoneToPool(uri: String) {
        if (uri.isBlank()) return
        val current = _uiState.value.ringtonePool
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (uri !in current) current.add(uri)
        _uiState.value = _uiState.value.copy(ringtonePool = current.joinToString(","))
    }
    fun removeRingtoneFromPool(uri: String) {
        val remaining = _uiState.value.ringtonePool
            .split(",").map { it.trim() }.filter { it.isNotEmpty() && it != uri }
        _uiState.value = _uiState.value.copy(ringtonePool = remaining.joinToString(","))
    }
    // v1.5.0 setters
    fun updateSolarOffset(minutes: Int) {
        // Clamp ±12h so a typo can't strand an alarm off the day grid.
        _uiState.value = _uiState.value.copy(solarOffsetMinutes = minutes.coerceIn(-720, 720))
    }
    fun updateSolarAnchor(anchor: String) {
        val sanitized = if (anchor.uppercase() == "SUNSET") "SUNSET" else "SUNRISE"
        _uiState.value = _uiState.value.copy(solarAnchor = sanitized)
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }

    fun computeForecast() {
        viewModelScope.launch {
            val s = _uiState.value
            val probe = Alarm(
                hour = s.hour,
                minute = s.minute,
                repeatDays = s.repeatDays,
                specificDate = s.specificDate,
                solarOffsetMinutes = s.solarOffsetMinutes,
                solarAnchor = s.solarAnchor,
                skipOnHolidays = s.skipOnHolidays,
                isEnabled = true
            )
            val settings = preferencesManager.getCurrentSettings()
            val zone = java.time.ZoneId.systemDefault()
            val entries = mutableListOf<ForecastEntry>()
            var cursor = java.time.ZonedDateTime.now()
            repeat(7) {
                val raw = calculator.calculate(probe, cursor)
                if (raw <= 0L) return@repeat
                val adj = com.sysadmindoc.alarmclock.domain.VacationAlarmPolicy
                    .adjustTrigger(probe, raw, settings, zone) { a, from ->
                        calculator.calculate(a, from)
                    }
                entries.add(ForecastEntry(adj.triggerTime, adj.skippedByVacation))
                cursor = java.time.Instant.ofEpochMilli(raw)
                    .atZone(zone)
                    .plusMinutes(1)
            }
            _uiState.value = _uiState.value.copy(forecastDates = entries)
        }
    }

    private fun String.toChallengeReferenceLabel(): String = when (this) {
        "NFC_SCAN" -> "an NFC tag"
        "BARCODE_SCAN" -> "a barcode value"
        "PHOTO_MATCH" -> "a reference photo"
        "WIFI_CONNECT" -> "a Wi-Fi network name"
        else -> "challenge details"
    }

    fun save(onComplete: () -> Unit) {
        // Re-entrancy guard: a fast double-tap on the Save button would otherwise
        // create two alarm rows. The state flag still updates, but races against
        // recomposition; this synchronous check is reliable.
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            val s = _uiState.value

            // Physical-challenge readiness preflight: refuse to save an alarm whose
            // dismiss challenge has no registered reference, because it could never
            // be dismissed. Hardware/permission gaps only warn (handled in the UI);
            // missing references are a hard block.
            val missingReferences = missingChallengeReferences(
                challengeType = s.challengeType,
                challengeChain = s.challengeChain,
                references = ChallengeReferences(
                    nfcTagId = s.nfcTagId,
                    barcodeValue = s.barcodeValue,
                    photoMatchUri = s.photoMatchUri,
                    wifiDismissSsid = s.wifiDismissSsid
                )
            )
            if (missingReferences.isNotEmpty()) {
                val labels = missingReferences.joinToString(", ") { it.toChallengeReferenceLabel() }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "Finish the challenge setup first: missing $labels."
                )
                return@launch
            }

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
                group = s.group,
                flashWake = s.flashWake,
                vibrationPattern = s.vibrationPattern,
                createdAt = if (s.isEditing && s.createdAt > 0) s.createdAt else System.currentTimeMillis(),
                ttsEnabled = s.ttsEnabled,
                walkStepsRequired = s.walkStepsRequired,
                requiredSquats = s.requiredSquats,
                wakeConfirmEnabled = s.wakeConfirmEnabled,
                wakeConfirmDelayMinutes = s.wakeConfirmDelayMinutes,
                smartAlarmEnabled = s.smartAlarmEnabled,
                smartAlarmWindowMinutes = s.smartAlarmWindowMinutes,
                skipOnHolidays = s.skipOnHolidays,
                nfcTagId = s.nfcTagId,
                barcodeValue = s.barcodeValue,
                spotifyUri = s.spotifyUri,
                hueEnabled = s.hueEnabled,
                huePreWakeMinutes = s.huePreWakeMinutes,
                photoMatchUri = s.photoMatchUri,
                challengeChain = s.challengeChain,
                progressiveSnooze = s.progressiveSnooze,
                backupSoundEnabled = s.backupSoundEnabled,
                backupSoundDelaySec = s.backupSoundDelaySec,
                sunriseSimulation = s.sunriseSimulation,
                sunriseMinutes = s.sunriseMinutes,
                specificDate = s.specificDate,
                profileName = s.profileName,
                earlyDismissMinutes = s.earlyDismissMinutes,
                guardianEnabled = s.guardianEnabled,
                guardianPhone = s.guardianPhone,
                guardianDelaySec = s.guardianDelaySec,
                locationDismissEnabled = s.locationDismissEnabled,
                locationDismissLat = s.locationDismissLat,
                locationDismissLng = s.locationDismissLng,
                locationDismissRadius = s.locationDismissRadius,
                wifiDismissSsid = s.wifiDismissSsid,
                internetRadioUrl = s.internetRadioUrl,
                flashlightStrobe = s.flashlightStrobe,
                morningRoutine = s.morningRoutine,
                hardwareButtonAction = s.hardwareButtonAction,
                dismissAtRingtoneEnd = s.dismissAtRingtoneEnd,
                holdToDismissEnabled = s.holdToDismissEnabled,
                ringtonePool = s.ringtonePool,
                solarOffsetMinutes = s.solarOffsetMinutes,
                solarAnchor = s.solarAnchor,
                vibrationDelaySeconds = s.vibrationDelaySeconds,
                weatherEarlyMinutes = s.weatherEarlyMinutes
            )

            try {
                val savedId = repository.save(alarm)
                val savedAlarm = alarm.copy(
                    id = if (s.isEditing) alarmId else savedId
                )
                if (savedAlarm.isEnabled) {
                    scheduler.schedule(savedAlarm)
                } else {
                    // Edit flow may have just disabled the alarm — make sure any
                    // previously-armed AlarmManager / worker entry is torn down.
                    if (s.isEditing) scheduler.cancel(alarmId)
                    scheduler.syncBedtimeDndRule()
                }
                onComplete()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "Could not save this alarm. Check the details and try again."
                )
            }
        }
    }
}
