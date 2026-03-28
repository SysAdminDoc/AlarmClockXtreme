package com.sysadmindoc.alarmclock.data.backup

import android.content.Context
import android.net.Uri
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class AlarmBackup(
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean,
    val repeatDays: List<String>, // DayOfWeek names
    val ringtoneUri: String,
    val vibrationEnabled: Boolean,
    val vibrationIntensity: Int,
    val volume: Int,
    val overrideSystemVolume: Boolean,
    val gradualVolumeSeconds: Int,
    val snoozeDurationMinutes: Int,
    val maxSnoozeCount: Int,
    val showOnLockScreen: Boolean,
    val challengeType: String,
    // F1-F17 feature fields
    val group: String = "",
    val flashWake: Boolean = false,
    val vibrationPattern: String = "default",
    val ttsEnabled: Boolean = false,
    val walkStepsRequired: Int = 30,
    val wakeConfirmEnabled: Boolean = false,
    val wakeConfirmDelayMinutes: Int = 10,
    val smartAlarmEnabled: Boolean = false,
    val smartAlarmWindowMinutes: Int = 30,
    val skipOnHolidays: Boolean = false,
    val nfcTagId: String = "",
    val barcodeValue: String = "",
    val spotifyUri: String = "",
    val hueEnabled: Boolean = false,
    val huePreWakeMinutes: Int = 30,
    val photoMatchUri: String = "",
    // v1.2.0 fields
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
    val morningRoutine: String = ""
)

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 3,
    val appVersion: String = "1.2.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val alarms: List<AlarmBackup>,
    val settings: SettingsBackup?
)

@JsonClass(generateAdapter = true)
data class SettingsBackup(
    val is24HourFormat: Boolean,
    val defaultSnoozeDuration: Int,
    val defaultGradualVolume: Int,
    val usePhoneSpeakers: Boolean,
    val showOnLockScreen: Boolean,
    val vacationModeEnabled: Boolean,
    val vacationStartMillis: Long,
    val vacationEndMillis: Long,
    val showWeatherOnDashboard: Boolean,
    val showCalendarOnDashboard: Boolean,
    // Previously missing settings
    val temperatureUnit: String = "fahrenheit",
    val bedtimeEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val sleepGoalHours: Int = 8,
    val sleepGoalMinutes: Int = 0,
    val bedtimeReminderMinutes: Int = 30,
    val flipToSnoozeEnabled: Boolean = false,
    val webhookEnabled: Boolean = false,
    val webhookUrl: String = "",
    val holidayAutoSkipEnabled: Boolean = false,
    val holidayCountryCode: String = "",
    val hueBridgeIp: String = "",
    val hueApiKey: String = "",
    val hueLightIds: String = "",
    // v1.2.0 settings
    val accentColor: String = "#5B9EF4",
    val adaptiveDifficultyEnabled: Boolean = false,
    val calendarAutoAlarmEnabled: Boolean = false,
    val calendarAutoAlarmMinutesBefore: Int = 60,
    val guardianContactName: String = "",
    val guardianContactPhone: String = "",
    val customTypingPhrases: String = "",
    val nightClockEnabled: Boolean = false,
    val showMotivationalQuotes: Boolean = true
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val scheduler: AlarmScheduler
) {
    private val moshi = Moshi.Builder().build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun export(): String {
        val alarms = repository.getAll()
        val settings = preferencesManager.getCurrentSettings()

        val backup = BackupData(
            alarms = alarms.map { alarm ->
                AlarmBackup(
                    hour = alarm.hour,
                    minute = alarm.minute,
                    label = alarm.label,
                    isEnabled = alarm.isEnabled,
                    repeatDays = alarm.repeatDays.map { it.name },
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
                    ttsEnabled = alarm.ttsEnabled,
                    walkStepsRequired = alarm.walkStepsRequired,
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
                    morningRoutine = alarm.morningRoutine
                )
            },
            settings = SettingsBackup(
                is24HourFormat = settings.is24HourFormat,
                defaultSnoozeDuration = settings.defaultSnoozeDuration,
                defaultGradualVolume = settings.defaultGradualVolume,
                usePhoneSpeakers = settings.usePhoneSpeakers,
                showOnLockScreen = settings.showOnLockScreen,
                vacationModeEnabled = settings.vacationModeEnabled,
                vacationStartMillis = settings.vacationStartMillis,
                vacationEndMillis = settings.vacationEndMillis,
                showWeatherOnDashboard = settings.showWeatherOnDashboard,
                showCalendarOnDashboard = settings.showCalendarOnDashboard,
                temperatureUnit = settings.temperatureUnit,
                bedtimeEnabled = settings.bedtimeEnabled,
                bedtimeHour = settings.bedtimeHour,
                bedtimeMinute = settings.bedtimeMinute,
                sleepGoalHours = settings.sleepGoalHours,
                sleepGoalMinutes = settings.sleepGoalMinutes,
                bedtimeReminderMinutes = settings.bedtimeReminderMinutes,
                flipToSnoozeEnabled = settings.flipToSnoozeEnabled,
                webhookEnabled = settings.webhookEnabled,
                webhookUrl = settings.webhookUrl,
                holidayAutoSkipEnabled = settings.holidayAutoSkipEnabled,
                holidayCountryCode = settings.holidayCountryCode,
                hueBridgeIp = settings.hueBridgeIp,
                hueApiKey = settings.hueApiKey,
                hueLightIds = settings.hueLightIds,
                accentColor = settings.accentColor,
                adaptiveDifficultyEnabled = settings.adaptiveDifficultyEnabled,
                calendarAutoAlarmEnabled = settings.calendarAutoAlarmEnabled,
                calendarAutoAlarmMinutesBefore = settings.calendarAutoAlarmMinutesBefore,
                guardianContactName = settings.guardianContactName,
                guardianContactPhone = settings.guardianContactPhone,
                customTypingPhrases = settings.customTypingPhrases,
                nightClockEnabled = settings.nightClockEnabled,
                showMotivationalQuotes = settings.showMotivationalQuotes
            )
        )

        return adapter.toJson(backup)
    }

    suspend fun exportToUri(uri: Uri): Result<Int> {
        return try {
            val json = export()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
            val backup = adapter.fromJson(json)
            Result.success(backup?.alarms?.size ?: 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Int> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Unable to read file"))

            val backup = adapter.fromJson(json)
                ?: return Result.failure(Exception("Invalid backup format"))

            var count = 0
            for (ab in backup.alarms) {
                try {
                    val alarm = Alarm(
                        hour = ab.hour,
                        minute = ab.minute,
                        label = ab.label,
                        isEnabled = ab.isEnabled,
                        repeatDays = ab.repeatDays.mapNotNull {
                            try { DayOfWeek.valueOf(it) } catch (_: Exception) { null }
                        }.toSet(),
                        ringtoneUri = ab.ringtoneUri,
                        vibrationEnabled = ab.vibrationEnabled,
                        vibrationIntensity = ab.vibrationIntensity,
                        volume = ab.volume,
                        overrideSystemVolume = ab.overrideSystemVolume,
                        gradualVolumeSeconds = ab.gradualVolumeSeconds,
                        snoozeDurationMinutes = ab.snoozeDurationMinutes,
                        maxSnoozeCount = ab.maxSnoozeCount,
                        showOnLockScreen = ab.showOnLockScreen,
                        challengeType = ab.challengeType,
                        group = ab.group,
                        flashWake = ab.flashWake,
                        vibrationPattern = ab.vibrationPattern,
                        ttsEnabled = ab.ttsEnabled,
                        walkStepsRequired = ab.walkStepsRequired,
                        wakeConfirmEnabled = ab.wakeConfirmEnabled,
                        wakeConfirmDelayMinutes = ab.wakeConfirmDelayMinutes,
                        smartAlarmEnabled = ab.smartAlarmEnabled,
                        smartAlarmWindowMinutes = ab.smartAlarmWindowMinutes,
                        skipOnHolidays = ab.skipOnHolidays,
                        nfcTagId = ab.nfcTagId,
                        barcodeValue = ab.barcodeValue,
                        spotifyUri = ab.spotifyUri,
                        hueEnabled = ab.hueEnabled,
                        huePreWakeMinutes = ab.huePreWakeMinutes,
                        photoMatchUri = ab.photoMatchUri,
                        challengeChain = ab.challengeChain,
                        progressiveSnooze = ab.progressiveSnooze,
                        backupSoundEnabled = ab.backupSoundEnabled,
                        backupSoundDelaySec = ab.backupSoundDelaySec,
                        sunriseSimulation = ab.sunriseSimulation,
                        sunriseMinutes = ab.sunriseMinutes,
                        specificDate = ab.specificDate,
                        profileName = ab.profileName,
                        earlyDismissMinutes = ab.earlyDismissMinutes,
                        guardianEnabled = ab.guardianEnabled,
                        guardianPhone = ab.guardianPhone,
                        guardianDelaySec = ab.guardianDelaySec,
                        locationDismissEnabled = ab.locationDismissEnabled,
                        locationDismissLat = ab.locationDismissLat,
                        locationDismissLng = ab.locationDismissLng,
                        locationDismissRadius = ab.locationDismissRadius,
                        wifiDismissSsid = ab.wifiDismissSsid,
                        internetRadioUrl = ab.internetRadioUrl,
                        flashlightStrobe = ab.flashlightStrobe,
                        morningRoutine = ab.morningRoutine
                    )
                    val id = repository.save(alarm)
                    if (alarm.isEnabled) {
                        scheduler.schedule(alarm.copy(id = id))
                    }
                    count++
                } catch (_: Exception) {
                    // Skip malformed alarm entries, continue importing
                }
            }

            // Import settings
            backup.settings?.let { s ->
                preferencesManager.update {
                    it.copy(
                        is24HourFormat = s.is24HourFormat,
                        defaultSnoozeDuration = s.defaultSnoozeDuration,
                        defaultGradualVolume = s.defaultGradualVolume,
                        usePhoneSpeakers = s.usePhoneSpeakers,
                        showOnLockScreen = s.showOnLockScreen,
                        vacationModeEnabled = s.vacationModeEnabled,
                        vacationStartMillis = s.vacationStartMillis,
                        vacationEndMillis = s.vacationEndMillis,
                        showWeatherOnDashboard = s.showWeatherOnDashboard,
                        showCalendarOnDashboard = s.showCalendarOnDashboard,
                        temperatureUnit = s.temperatureUnit,
                        bedtimeEnabled = s.bedtimeEnabled,
                        bedtimeHour = s.bedtimeHour,
                        bedtimeMinute = s.bedtimeMinute,
                        sleepGoalHours = s.sleepGoalHours,
                        sleepGoalMinutes = s.sleepGoalMinutes,
                        bedtimeReminderMinutes = s.bedtimeReminderMinutes,
                        flipToSnoozeEnabled = s.flipToSnoozeEnabled,
                        webhookEnabled = s.webhookEnabled,
                        webhookUrl = s.webhookUrl,
                        holidayAutoSkipEnabled = s.holidayAutoSkipEnabled,
                        holidayCountryCode = s.holidayCountryCode,
                        hueBridgeIp = s.hueBridgeIp,
                        hueApiKey = s.hueApiKey,
                        hueLightIds = s.hueLightIds,
                        accentColor = s.accentColor,
                        adaptiveDifficultyEnabled = s.adaptiveDifficultyEnabled,
                        calendarAutoAlarmEnabled = s.calendarAutoAlarmEnabled,
                        calendarAutoAlarmMinutesBefore = s.calendarAutoAlarmMinutesBefore,
                        guardianContactName = s.guardianContactName,
                        guardianContactPhone = s.guardianContactPhone,
                        customTypingPhrases = s.customTypingPhrases,
                        nightClockEnabled = s.nightClockEnabled,
                        showMotivationalQuotes = s.showMotivationalQuotes
                    )
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
