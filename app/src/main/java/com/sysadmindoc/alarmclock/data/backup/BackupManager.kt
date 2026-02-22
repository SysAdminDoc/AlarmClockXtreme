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
import com.squareup.moshi.Types
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
    val challengeType: String
)

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val appVersion: String = "0.8.1",
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
    val showCalendarOnDashboard: Boolean
)

// Note: autoSilenceMinutes not backed up as it's device-specific preference

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val scheduler: AlarmScheduler
) {
    private val moshi = Moshi.Builder().build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    /**
     * Export all alarms and settings to a JSON string.
     */
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
                    challengeType = alarm.challengeType
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
                showCalendarOnDashboard = settings.showCalendarOnDashboard
            )
        )

        return adapter.toJson(backup)
    }

    /**
     * Export to a file URI (content:// from SAF).
     */
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

    /**
     * Import alarms and settings from a file URI.
     * Returns the number of alarms imported.
     */
    suspend fun importFromUri(uri: Uri): Result<Int> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Unable to read file"))

            val backup = adapter.fromJson(json)
                ?: return Result.failure(Exception("Invalid backup format"))

            // Import alarms
            var count = 0
            for (ab in backup.alarms) {
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
                    challengeType = ab.challengeType
                )
                val id = repository.save(alarm)
                if (alarm.isEnabled) {
                    scheduler.schedule(alarm.copy(id = id))
                }
                count++
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
                        showCalendarOnDashboard = s.showCalendarOnDashboard
                    )
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
