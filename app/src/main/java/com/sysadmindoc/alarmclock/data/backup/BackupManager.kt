package com.sysadmindoc.alarmclock.data.backup

import android.content.Context
import android.net.Uri
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.DEFAULT_NEWS_FEED_URL
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
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
    val morningRoutine: String = "",
    // v1.4.0 fields
    val hardwareButtonAction: String = "NONE",
    val dismissAtRingtoneEnd: Boolean = false,
    val ringtonePool: String = "",
    // v1.5.0 fields
    val solarOffsetMinutes: Int = 0,
    val solarAnchor: String = "SUNRISE",
    // v1.10.3 fields
    val holdToDismissEnabled: Boolean = false,
    // v1.12.0 fields (roadmap N7): per-alarm pre-vibration delay (seconds).
    val vibrationDelaySeconds: Int = 0,
    val weatherEarlyMinutes: Int = 0,
    // v1.14.19: configurable squat challenge count.
    val requiredSquats: Int = 10,
    // v1.15.1: per-alarm dismiss action.
    val dismissActionType: String = "NONE",
    val dismissActionPayload: String = ""
)

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 11,
    val appVersion: String = BuildConfig.VERSION_NAME,
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
    val hideAlarmLabelsOnPublicSurfaces: Boolean = false,
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
    val bedtimeDndEnabled: Boolean = false,
    val flipToSnoozeEnabled: Boolean = false,
    val webhookEnabled: Boolean = false,
    val webhookUrl: String = "",
    val webhookIncludeLabel: Boolean = true,
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
    val showMotivationalQuotes: Boolean = true,
    // v1.4.0 settings
    val dynamicColorEnabled: Boolean = false,
    val expressiveModeEnabled: Boolean = false,
    val coverToSnoozeEnabled: Boolean = false,
    val bedtimeChecklist: String = "",
    val sleepSoundTimerMinutes: Int = 0,
    val sleepSoundFadeSeconds: Int = 60,
    val repeatMissedAlarms: Boolean = true,
    val napDefaultMinutes: Int = 20,
    // v1.11.6 (roadmap N6): pause-alarms timestamp. Round-trips through
    // backup so a restore of "I'm pausing for 3 days" survives device
    // swaps; stale restored values are harmless because the scheduler
    // checks `> now` per fire.
    val pauseUntilMillis: Long = 0,
    // v1.13.1 (roadmap N12): Health Connect opt-in survives backup so a
    // user who restores onto a new Play-flavor device doesn't have to
    // re-tap the toggle. F-Droid restore ignores it.
    val healthConnectEnabled: Boolean = false,
    // v1.13.3 (roadmap X2): round-trip the user's selected feed URL and warn
    // before readable exports when custom/private feed endpoints are present.
    val newsFeedUrl: String = DEFAULT_NEWS_FEED_URL,
    // Backup v9+: AppSettings fields that previously never made it into the
    // backup, so every restore silently reset them to defaults — the same
    // drift class as the v1.1.0 backup data loss. Defaults below match
    // AppSettings so v1-v9 backups import unchanged.
    val upcomingAlarmMinutes: Int = 60,
    val showNoAlarmsWarning: Boolean = true,
    val autoSilenceMinutes: Int = 10,
    val locationName: String = "",
    val useManualLocation: Boolean = false,
    val lastKnownLatitude: Double = 0.0,
    val lastKnownLongitude: Double = 0.0,
    val showDashboardTab: Boolean = true,
    val showTimerTab: Boolean = true,
    val showWorldClockTab: Boolean = true,
    val showNewsTab: Boolean = true,
    val showRadarEmbed: Boolean = true,
    val cancellationLockMinutes: Int = 0,
    val hueBridgeCertFingerprint: String = "",
    val hueLegacyHttpEnabled: Boolean = false,
    val firingControlMode: String = "hybrid",
    val challengeBypassEnabled: Boolean = false,
    val challengeBypassDelaySeconds: Int = 30
)

data class BackupExportWarning(
    val categories: List<String> = emptyList()
) {
    val shouldWarn: Boolean
        get() = categories.isNotEmpty()
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val scheduler: AlarmScheduler
) {
    private val moshi = Moshi.Builder().build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    companion object {
        /** Highest backup format version we know how to read end-to-end. */
        const val MAX_SUPPORTED_BACKUP_VERSION = 11

        fun assessExportWarning(
            settings: AppSettings,
            alarms: List<Alarm>
        ): BackupExportWarning {
            val categories = buildList {
                if (settings.webhookUrl.isNotBlank()) {
                    add("Webhook endpoint URL")
                }
                if (
                    settings.hueBridgeIp.isNotBlank() ||
                    settings.hueApiKey.isNotBlank() ||
                    settings.hueLightIds.isNotBlank()
                ) {
                    add("Philips Hue bridge details and API key")
                }
                if (settings.newsFeedUrl.isCustomFeedUrl()) {
                    add("Custom news feed URL")
                }
                // Backup v9+ carries the saved weather location; surface it the
                // same way the per-alarm location-dismiss coordinates are.
                if (settings.locationName.isNotBlank() ||
                    settings.lastKnownLatitude != 0.0 ||
                    settings.lastKnownLongitude != 0.0
                ) {
                    add("Saved weather location")
                }
                if (alarms.any { it.internetRadioUrl.isNotBlank() }) {
                    add("Internet radio stream URLs")
                }
                if (alarms.any { it.hasDeviceLocalUris() }) {
                    add("Device-local ringtone or photo URIs")
                }
                if (alarms.any { it.hasPrivateAlarmDetails() }) {
                    add("Wi-Fi, location, or guardian contact details")
                }
                if (alarms.any { it.nfcTagId.isNotBlank() || it.barcodeValue.isNotBlank() }) {
                    add("NFC or barcode challenge values")
                }
            }
            return BackupExportWarning(categories)
        }

        private fun String.isCustomFeedUrl(): Boolean {
            val value = trim()
            return value.isNotEmpty() && !value.equals(DEFAULT_NEWS_FEED_URL, ignoreCase = true)
        }

        private fun Alarm.hasDeviceLocalUris(): Boolean {
            val poolHasLocalUri = ringtonePool
                .split(",")
                .any { it.hasDeviceLocalUriScheme() }
            return ringtoneUri.hasDeviceLocalUriScheme() ||
                photoMatchUri.hasDeviceLocalUriScheme() ||
                poolHasLocalUri
        }

        private fun Alarm.hasPrivateAlarmDetails(): Boolean =
            guardianPhone.isNotBlank() ||
                wifiDismissSsid.isNotBlank() ||
                (locationDismissEnabled && (locationDismissLat != 0.0 || locationDismissLng != 0.0))

        private fun String.hasDeviceLocalUriScheme(): Boolean {
            val value = trim()
            if (value.isEmpty()) return false
            val lower = value.lowercase(Locale.US)
            return lower.startsWith("content://") ||
                lower.startsWith("file://") ||
                lower.startsWith("/") ||
                Regex("^[a-z]:[\\\\/].*").matches(lower)
        }
    }

    suspend fun inspectExportWarning(): BackupExportWarning {
        val alarms = repository.getAll()
        val settings = preferencesManager.getCurrentSettings()
        return assessExportWarning(settings, alarms)
    }

    suspend fun export(): String {
        val alarms = repository.getAll()
        val settings = preferencesManager.getCurrentSettings()

        val backup = BackupData(
            alarms = alarms.map { alarm -> alarm.toAlarmBackup() },
            settings = SettingsBackup(
                is24HourFormat = settings.is24HourFormat,
                defaultSnoozeDuration = settings.defaultSnoozeDuration,
                defaultGradualVolume = settings.defaultGradualVolume,
                usePhoneSpeakers = settings.usePhoneSpeakers,
                showOnLockScreen = settings.showOnLockScreen,
                hideAlarmLabelsOnPublicSurfaces = settings.hideAlarmLabelsOnPublicSurfaces,
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
                bedtimeDndEnabled = settings.bedtimeDndEnabled,
                flipToSnoozeEnabled = settings.flipToSnoozeEnabled,
                webhookEnabled = settings.webhookEnabled,
                webhookUrl = settings.webhookUrl,
                webhookIncludeLabel = settings.webhookIncludeLabel,
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
                showMotivationalQuotes = settings.showMotivationalQuotes,
                dynamicColorEnabled = settings.dynamicColorEnabled,
                expressiveModeEnabled = settings.expressiveModeEnabled,
                coverToSnoozeEnabled = settings.coverToSnoozeEnabled,
                bedtimeChecklist = settings.bedtimeChecklist,
                sleepSoundTimerMinutes = settings.sleepSoundTimerMinutes,
                sleepSoundFadeSeconds = settings.sleepSoundFadeSeconds,
                repeatMissedAlarms = settings.repeatMissedAlarms,
                napDefaultMinutes = settings.napDefaultMinutes,
                pauseUntilMillis = settings.pauseUntilMillis,
                healthConnectEnabled = settings.healthConnectEnabled,
                newsFeedUrl = settings.newsFeedUrl,
                upcomingAlarmMinutes = settings.upcomingAlarmMinutes,
                showNoAlarmsWarning = settings.showNoAlarmsWarning,
                autoSilenceMinutes = settings.autoSilenceMinutes,
                locationName = settings.locationName,
                useManualLocation = settings.useManualLocation,
                lastKnownLatitude = settings.lastKnownLatitude,
                lastKnownLongitude = settings.lastKnownLongitude,
                showDashboardTab = settings.showDashboardTab,
                showTimerTab = settings.showTimerTab,
                showWorldClockTab = settings.showWorldClockTab,
                showNewsTab = settings.showNewsTab,
                showRadarEmbed = settings.showRadarEmbed,
                cancellationLockMinutes = settings.cancellationLockMinutes,
                hueBridgeCertFingerprint = settings.hueBridgeCertFingerprint,
                hueLegacyHttpEnabled = settings.hueLegacyHttpEnabled,
                firingControlMode = settings.firingControlMode,
                challengeBypassEnabled = settings.challengeBypassEnabled,
                challengeBypassDelaySeconds = settings.challengeBypassDelaySeconds
            )
        )

        return adapter.toJson(backup)
    }

    suspend fun exportToUri(uri: Uri): Result<Int> {
        return try {
            // Open the output stream FIRST so a permission-denied / cancelled
            // SAF intent fails fast without doing any DB work. Previously we
            // read every alarm and built the JSON before discovering the URI
            // was unwritable.
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(java.io.IOException("Unable to open file for writing"))
            val json = export()
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            // Re-parse the JSON we just wrote to count alarms — cheap and avoids
            // a second DB query that could race with a concurrent edit.
            val alarmCount = adapter.fromJson(json)?.alarms?.size ?: 0
            Result.success(alarmCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportEncryptedToUri(uri: Uri, passphrase: String): Result<Int> {
        if (passphrase.isBlank()) {
            return Result.failure(IllegalArgumentException("Backup passphrase is required"))
        }

        return try {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(java.io.IOException("Unable to open file for writing"))
            val json = export()
            val encryptedJson = EncryptedBackupCodec.encrypt(json, passphrase)
            stream.use { it.write(encryptedJson.toByteArray(Charsets.UTF_8)) }
            val alarmCount = adapter.fromJson(json)?.alarms?.size ?: 0
            Result.success(alarmCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Int> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Unable to read file"))

            importFromJson(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importEncryptedFromUri(uri: Uri, passphrase: String): Result<Int> {
        if (passphrase.isBlank()) {
            return Result.failure(IllegalArgumentException("Backup passphrase is required"))
        }

        return try {
            val encryptedJson = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Unable to read file"))

            val json = runCatching {
                EncryptedBackupCodec.decrypt(encryptedJson, passphrase)
            }.getOrElse {
                return Result.failure(Exception("Unable to decrypt backup. Check the passphrase."))
            }

            importFromJson(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun importFromJson(json: String): Result<Int> {
        return try {
            val backup = adapter.fromJson(json)
                ?: return Result.failure(Exception("Invalid backup format"))

            // Version sanity. We tolerate older backups (1/2 -> 3 with defaults
            // for missing fields, a deliberate Moshi behavior) but reject
            // anything outside the known range so a random JSON file can't be
            // mistaken for a backup and silently wipe nothing into the app.
            if (backup.version !in 1..MAX_SUPPORTED_BACKUP_VERSION) {
                return Result.failure(
                    Exception(
                        "Unsupported backup version ${backup.version}. " +
                            "This app understands versions 1-$MAX_SUPPORTED_BACKUP_VERSION."
                    )
                )
            }

            val importedAlarms = backup.alarms.mapNotNull { it.toAlarmOrNull() }

            // Import settings
            backup.settings?.let { s ->
                preferencesManager.update {
                    it.copy(
                        is24HourFormat = s.is24HourFormat,
                        defaultSnoozeDuration = s.defaultSnoozeDuration,
                        defaultGradualVolume = s.defaultGradualVolume,
                        usePhoneSpeakers = s.usePhoneSpeakers,
                        showOnLockScreen = s.showOnLockScreen,
                        hideAlarmLabelsOnPublicSurfaces = s.hideAlarmLabelsOnPublicSurfaces,
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
                        bedtimeDndEnabled = s.bedtimeDndEnabled,
                        flipToSnoozeEnabled = s.flipToSnoozeEnabled,
                        webhookEnabled = s.webhookEnabled,
                        webhookUrl = s.webhookUrl,
                        webhookIncludeLabel = s.webhookIncludeLabel,
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
                        showMotivationalQuotes = s.showMotivationalQuotes,
                        dynamicColorEnabled = s.dynamicColorEnabled,
                        expressiveModeEnabled = s.expressiveModeEnabled,
                        coverToSnoozeEnabled = s.coverToSnoozeEnabled,
                        bedtimeChecklist = s.bedtimeChecklist,
                        sleepSoundTimerMinutes = s.sleepSoundTimerMinutes,
                        sleepSoundFadeSeconds = s.sleepSoundFadeSeconds,
                        repeatMissedAlarms = s.repeatMissedAlarms,
                        napDefaultMinutes = s.napDefaultMinutes,
                        pauseUntilMillis = s.pauseUntilMillis,
                        healthConnectEnabled = s.healthConnectEnabled,
                        newsFeedUrl = s.newsFeedUrl,
                        upcomingAlarmMinutes = s.upcomingAlarmMinutes,
                        showNoAlarmsWarning = s.showNoAlarmsWarning,
                        autoSilenceMinutes = s.autoSilenceMinutes,
                        locationName = s.locationName,
                        useManualLocation = s.useManualLocation,
                        lastKnownLatitude = s.lastKnownLatitude,
                        lastKnownLongitude = s.lastKnownLongitude,
                        showDashboardTab = s.showDashboardTab,
                        showTimerTab = s.showTimerTab,
                        showWorldClockTab = s.showWorldClockTab,
                        showNewsTab = s.showNewsTab,
                        showRadarEmbed = s.showRadarEmbed,
                        cancellationLockMinutes = s.cancellationLockMinutes,
                        hueBridgeCertFingerprint = s.hueBridgeCertFingerprint,
                        hueLegacyHttpEnabled = s.hueLegacyHttpEnabled,
                        firingControlMode = s.firingControlMode,
                        challengeBypassEnabled = s.challengeBypassEnabled,
                        challengeBypassDelaySeconds = s.challengeBypassDelaySeconds
                    )
                }
            }

            // v1.6.3: Make import per-alarm-resilient. The previous loop had
            // no per-alarm try/catch despite the comment in CLAUDE.md
            // claiming "individual alarm failures don't abort the batch."
            // A single corrupt alarm row (e.g. an `Uri.parse`-hostile
            // ringtone URI from a much older backup) would now fail the
            // entire import after partially saving earlier rows. Wrap each
            // save+schedule so genuine bad rows are skipped with a log
            // entry while the rest of the backup still lands.
            var count = 0
            val alarmsToSchedule = mutableListOf<Alarm>()
            for (alarm in importedAlarms) {
                try {
                    val savedId = repository.save(alarm.copy(nextTriggerTime = 0))
                    val savedAlarm = alarm.copy(id = savedId, nextTriggerTime = 0)
                    if (savedAlarm.isEnabled) {
                        alarmsToSchedule += savedAlarm
                    }
                    count++
                } catch (e: Exception) {
                    android.util.Log.w(
                        "BackupManager",
                        "Skipped one alarm during import",
                        e
                    )
                }
            }

            alarmsToSchedule.forEach { alarm ->
                try {
                    scheduler.schedule(alarm)
                } catch (e: Exception) {
                    // The alarm row was saved successfully — only the schedule
                    // attempt failed. The user can re-enable it from the list.
                    android.util.Log.w(
                        "BackupManager",
                        "Saved but failed to schedule alarm ${alarm.id}",
                        e
                    )
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
