package com.sysadmindoc.alarmclock.ui.settings

import android.app.Application
import android.Manifest
import android.app.AlarmManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.backup.BackupManager
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.WebhookService
import com.sysadmindoc.alarmclock.util.ManufacturerCompat
import com.sysadmindoc.alarmclock.worker.CalendarAutoAlarmWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    // Battery optimization
    val isIgnoringBatteryOptimizations: Boolean = false,
    val needsBatteryGuidance: Boolean = false,
    val manufacturerName: String = "",
    val batteryGuidanceSteps: List<String> = emptyList(),
    val batteryGuidanceTitle: String = "",
    // Device info
    val androidVersion: String = "",
    val deviceModel: String = "",
    val appVersion: String = "",
    // Webhook test result
    val webhookTestResult: String? = null,
    val isWebhookTesting: Boolean = false,
    // Hue test result
    val hueTestResult: String? = null,
    val isHueTesting: Boolean = false,
    // Wake readiness
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    // v1.11.3 (roadmap N3): App Standby bucket awareness. UsageStatsManager
    // returns one of STANDBY_BUCKET_ACTIVE / WORKING_SET / FREQUENT / RARE /
    // RESTRICTED on API 28+. ACTIVE and WORKING_SET are the alarm-friendly
    // states; FREQUENT and worse mean Android is throttling our background
    // work, which can delay or drop the alarm-schedule path. UNKNOWN (-1)
    // means the API isn't available on this device or returned no data —
    // we surface a generic "Standby bucket unknown" in that case.
    val appStandbyBucket: Int = AppStandbyBucket.UNKNOWN
)

object AppStandbyBucket {
    const val UNKNOWN: Int = -1
    /** True when the bucket actively throttles alarm scheduling. */
    fun isDegraded(bucket: Int): Boolean = bucket >= 30  // FREQUENT == 30
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager,
    private val alarmScheduler: AlarmScheduler,
    private val backupManager: BackupManager,
    private val webhookService: WebhookService
) : AndroidViewModel(application) {

    private val _batteryState = MutableStateFlow(
        BatteryState(
            isIgnoring = ManufacturerCompat.isIgnoringBatteryOptimizations(application),
            needsGuidance = ManufacturerCompat.needsBatteryGuidance()
        )
    )
    private val _webhookTestState = MutableStateFlow(IntegrationTestState())
    private val _hueTestState = MutableStateFlow(IntegrationTestState())
    private val _wakeReadinessState = MutableStateFlow(WakeReadinessState.from(application))

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.settings,
        _batteryState,
        _webhookTestState,
        _hueTestState,
        _wakeReadinessState
    ) { settings, battery, webhookState, hueState, wakeReadiness ->
        val guidance = ManufacturerCompat.getGuidance()
        SettingsUiState(
            settings = settings,
            isIgnoringBatteryOptimizations = battery.isIgnoring,
            needsBatteryGuidance = battery.needsGuidance,
            manufacturerName = guidance?.manufacturer ?: Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            batteryGuidanceSteps = guidance?.steps ?: emptyList(),
            batteryGuidanceTitle = guidance?.title ?: "",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = BuildConfig.VERSION_NAME,
            webhookTestResult = webhookState.message,
            isWebhookTesting = webhookState.isRunning,
            hueTestResult = hueState.message,
            isHueTesting = hueState.isRunning,
            hasNotificationPermission = wakeReadiness.hasNotificationPermission,
            canScheduleExactAlarms = wakeReadiness.canScheduleExactAlarms,
            appStandbyBucket = wakeReadiness.appStandbyBucket
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun requestBatteryExemption() {
        val context = getApplication<Application>()
        ManufacturerCompat.requestBatteryOptimizationExemption(context)
    }

    fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val context = getApplication<Application>()
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun refreshBatteryStatus() {
        val context = getApplication<Application>()
        _batteryState.value = BatteryState(
            isIgnoring = ManufacturerCompat.isIgnoringBatteryOptimizations(context),
            needsGuidance = ManufacturerCompat.needsBatteryGuidance()
        )
        _wakeReadinessState.value = WakeReadinessState.from(context)
    }

    // v1.2.0 personalization — these settings exist in PreferencesManager but
    // had no UI surface until this audit pass. Setters live alongside the
    // existing toggle helpers so the SettingsScreen call-site stays uniform.
    fun toggleShowMotivationalQuotes(enabled: Boolean) =
        updateSettings { it.copy(showMotivationalQuotes = enabled) }
    fun toggleAdaptiveDifficulty(enabled: Boolean) =
        updateSettings { it.copy(adaptiveDifficultyEnabled = enabled) }
    fun updateAccentColor(hex: String) {
        // Defensive: only persist a value that parses cleanly so a corrupt
        // input can't blank-out the entire theme.
        val sanitised = hex.trim()
        val parses = runCatching { android.graphics.Color.parseColor(sanitised) }.isSuccess
        if (parses && sanitised.startsWith("#")) {
            updateSettings { it.copy(accentColor = sanitised) }
        }
    }
    fun updateCustomTypingPhrases(phrases: String) =
        updateSettings { it.copy(customTypingPhrases = phrases) }

    // v1.4.0 personalization + wake-up settings
    fun toggleDynamicColor(enabled: Boolean) =
        updateSettings { it.copy(dynamicColorEnabled = enabled) }
    fun toggleExpressiveMode(enabled: Boolean) =
        updateSettings { it.copy(expressiveModeEnabled = enabled) }
    fun toggleCoverToSnooze(enabled: Boolean) =
        updateSettings { it.copy(coverToSnoozeEnabled = enabled) }
    fun toggleRepeatMissed(enabled: Boolean) =
        updateSettings { it.copy(repeatMissedAlarms = enabled) }
    fun updateBedtimeChecklist(items: String) =
        updateSettings { it.copy(bedtimeChecklist = items) }
    fun updateSleepSoundTimer(minutes: Int) =
        updateSettings { it.copy(sleepSoundTimerMinutes = minutes.coerceAtLeast(0)) }
    fun updateSleepSoundFade(seconds: Int) =
        updateSettings { it.copy(sleepSoundFadeSeconds = seconds.coerceIn(5, 600)) }

    // v1.7.1: Bottom-nav visibility toggles
    fun toggleShowDashboardTab(enabled: Boolean) =
        updateSettings { it.copy(showDashboardTab = enabled) }
    fun toggleShowTimerTab(enabled: Boolean) =
        updateSettings { it.copy(showTimerTab = enabled) }
    fun toggleShowWorldClockTab(enabled: Boolean) =
        updateSettings { it.copy(showWorldClockTab = enabled) }
    // v1.8.0
    fun toggleShowNewsTab(enabled: Boolean) =
        updateSettings { it.copy(showNewsTab = enabled) }
    fun toggleShowRadarEmbed(enabled: Boolean) =
        updateSettings { it.copy(showRadarEmbed = enabled) }

    fun toggle24Hour(enabled: Boolean) = updateSettings { it.copy(is24HourFormat = enabled) }
    fun togglePhoneSpeakers(enabled: Boolean) = updateSettings { it.copy(usePhoneSpeakers = enabled) }
    fun toggleLockScreen(enabled: Boolean) = updateSettings { it.copy(showOnLockScreen = enabled) }
    fun updateDefaultSnooze(minutes: Int) = updateSettings { it.copy(defaultSnoozeDuration = minutes) }
    fun updateDefaultGradualVolume(seconds: Int) = updateSettings { it.copy(defaultGradualVolume = seconds) }
    fun toggleShowWeather(enabled: Boolean) = updateSettings { it.copy(showWeatherOnDashboard = enabled) }
    fun toggleShowCalendar(enabled: Boolean) = updateSettings { it.copy(showCalendarOnDashboard = enabled) }
    fun toggleCalendarAutoAlarm(enabled: Boolean) = updateCalendarAutoAlarmSettings {
        it.copy(calendarAutoAlarmEnabled = enabled)
    }
    fun updateCalendarAutoAlarmMinutes(minutes: Int) = updateCalendarAutoAlarmSettings {
        it.copy(calendarAutoAlarmMinutesBefore = minutes.coerceIn(0, 720))
    }
    fun updateAutoSilence(minutes: Int) = updateSettings { it.copy(autoSilenceMinutes = minutes) }
    fun toggleTemperatureUnit() = updateSettings {
        it.copy(temperatureUnit = if (it.temperatureUnit == "fahrenheit") "celsius" else "fahrenheit")
    }
    // F2
    fun toggleFlipToSnooze(enabled: Boolean) = updateSettings { it.copy(flipToSnoozeEnabled = enabled) }
    // F11: Webhooks
    fun toggleWebhook(enabled: Boolean) = updateSettings { it.copy(webhookEnabled = enabled) }
    fun updateWebhookUrl(url: String) = updateSettings { it.copy(webhookUrl = url) }
    fun testWebhook() {
        viewModelScope.launch(Dispatchers.IO) {
            _webhookTestState.value = IntegrationTestState(
                message = "Checking webhook endpoint...",
                isRunning = true
            )
            val url = preferencesManager.getCurrentSettings().webhookUrl
            val ok = if (url.isBlank()) false else webhookService.test(url)
            val result = if (ok) "Webhook OK" else "Webhook failed — check URL"
            _webhookTestState.value = IntegrationTestState(message = result, isRunning = false)
            kotlinx.coroutines.delay(4000)
            if (_webhookTestState.value.message == result) {
                _webhookTestState.value = IntegrationTestState()
            }
        }
    }
    // F13: Holidays
    fun toggleHolidayAutoSkip(enabled: Boolean) =
        updateSettingsAndReschedule { it.copy(holidayAutoSkipEnabled = enabled) }
    fun updateHolidayCountryCode(code: String) = updateSettingsAndReschedule {
        it.copy(holidayCountryCode = code.trim().uppercase(Locale.US))
    }
    // F15: Hue
    fun updateHueBridgeIp(ip: String) = updateSettings { it.copy(hueBridgeIp = ip.trim()) }
    fun updateHueApiKey(key: String) = updateSettings { it.copy(hueApiKey = key.trim()) }
    fun updateHueLightIds(ids: String) = updateSettings { it.copy(hueLightIds = ids.trim()) }
    fun testHue() {
        viewModelScope.launch(Dispatchers.IO) {
            _hueTestState.value = IntegrationTestState(
                message = "Checking Hue bridge...",
                isRunning = true
            )
            val settings = preferencesManager.getCurrentSettings()
            val ok = try {
                val url = "http://${settings.hueBridgeIp}/api/${settings.hueApiKey}/lights"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
                val response = client.newCall(okhttp3.Request.Builder().url(url).build()).execute()
                response.isSuccessful.also { response.close() }
            } catch (_: Exception) { false }
            val result = if (ok) "Hue bridge reachable" else "Hue bridge not found — check IP and key"
            _hueTestState.value = IntegrationTestState(message = result, isRunning = false)
            kotlinx.coroutines.delay(4000)
            if (_hueTestState.value.message == result) {
                _hueTestState.value = IntegrationTestState()
            }
        }
    }

    fun setVacationMode(enabled: Boolean, startMillis: Long = 0, endMillis: Long = 0) {
        viewModelScope.launch {
            // Validate: end must be after start when enabling
            val validEnabled = enabled && startMillis > 0 && endMillis > 0 && endMillis > startMillis

            preferencesManager.update {
                it.copy(
                    vacationModeEnabled = validEnabled,
                    vacationStartMillis = startMillis,
                    vacationEndMillis = endMillis
                )
            }
            // Reschedule all alarms to apply/remove vacation filter
            alarmScheduler.rescheduleAll(forceRecalculate = true)
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            preferencesManager.update(transform)
        }
    }

    private fun updateSettingsAndReschedule(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            preferencesManager.update(transform)
            alarmScheduler.rescheduleAll(forceRecalculate = true)
        }
    }

    private fun updateCalendarAutoAlarmSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            preferencesManager.update(transform)
            val context = getApplication<Application>()
            val settings = preferencesManager.getCachedSettings()
            if (settings.calendarAutoAlarmEnabled) {
                CalendarAutoAlarmWorker.schedulePeriodic(context)
            } else {
                CalendarAutoAlarmWorker.cancelPeriodic(context)
            }
            CalendarAutoAlarmWorker.enqueueRefresh(context, "settings")
        }
    }

    // Backup/restore
    private val _backupResult = MutableStateFlow<String?>(null)
    val backupResult: StateFlow<String?> = _backupResult.asStateFlow()
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.exportToUri(uri)
                    .onSuccess { count -> setBackupResult("Exported $count alarms") }
                    .onFailure { setBackupResult("Export failed: ${it.message}") }
            } catch (e: Exception) {
                setBackupResult("Export failed: ${e.message ?: "unexpected error"}")
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun exportEncryptedBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.exportEncryptedToUri(uri, passphrase)
                    .onSuccess { count -> setBackupResult("Exported encrypted backup with $count alarms") }
                    .onFailure { setBackupResult("Encrypted export failed: ${it.message}") }
            } catch (e: Exception) {
                setBackupResult("Encrypted export failed: ${e.message ?: "unexpected error"}")
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.importFromUri(uri)
                    .onSuccess { count -> setBackupResult("Imported $count alarms") }
                    .onFailure { setBackupResult("Import failed: ${it.message}") }
            } catch (e: Exception) {
                setBackupResult("Import failed: ${e.message ?: "unexpected error"}")
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importEncryptedBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.importEncryptedFromUri(uri, passphrase)
                    .onSuccess { count -> setBackupResult("Imported encrypted backup with $count alarms") }
                    .onFailure { setBackupResult("Encrypted import failed: ${it.message}") }
            } catch (e: Exception) {
                setBackupResult("Encrypted import failed: ${e.message ?: "unexpected error"}")
            } finally {
                _backupBusy.value = false
            }
        }
    }

    private fun setBackupResult(message: String) {
        _backupResult.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_backupResult.value == message) {
                _backupResult.value = null
            }
        }
    }

    fun clearBackupResult() { _backupResult.value = null }

    private data class BatteryState(val isIgnoring: Boolean, val needsGuidance: Boolean)
    private data class IntegrationTestState(
        val message: String? = null,
        val isRunning: Boolean = false
    )

    private data class WakeReadinessState(
        val hasNotificationPermission: Boolean,
        val canScheduleExactAlarms: Boolean,
        val appStandbyBucket: Int
    ) {
        companion object {
            fun from(context: Context): WakeReadinessState {
                val notificationsReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                val exactAlarmsReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
                } else {
                    true
                }
                // UsageStatsManager.getAppStandbyBucket() is API 28+. We never
                // require PACKAGE_USAGE_STATS for the self-query — the system
                // returns the calling app's own bucket without it.
                val bucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching {
                        (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
                            ?.appStandbyBucket
                            ?: AppStandbyBucket.UNKNOWN
                    }.getOrDefault(AppStandbyBucket.UNKNOWN)
                } else {
                    AppStandbyBucket.UNKNOWN
                }
                return WakeReadinessState(
                    hasNotificationPermission = notificationsReady,
                    canScheduleExactAlarms = exactAlarmsReady,
                    appStandbyBucket = bucket
                )
            }
        }
    }
}
