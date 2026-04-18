package com.sysadmindoc.alarmclock.ui.settings

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.backup.BackupManager
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.WebhookService
import com.sysadmindoc.alarmclock.util.ManufacturerCompat
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
    // Hue test result
    val hueTestResult: String? = null
)

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
    private val _webhookTestResult = MutableStateFlow<String?>(null)
    private val _hueTestResult = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.settings,
        _batteryState,
        _webhookTestResult,
        _hueTestResult
    ) { settings, battery, webhookResult, hueResult ->
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
            webhookTestResult = webhookResult,
            hueTestResult = hueResult
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun requestBatteryExemption() {
        val context = getApplication<Application>()
        ManufacturerCompat.requestBatteryOptimizationExemption(context)
    }

    fun refreshBatteryStatus() {
        val context = getApplication<Application>()
        _batteryState.value = BatteryState(
            isIgnoring = ManufacturerCompat.isIgnoringBatteryOptimizations(context),
            needsGuidance = ManufacturerCompat.needsBatteryGuidance()
        )
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

    fun toggle24Hour(enabled: Boolean) = updateSettings { it.copy(is24HourFormat = enabled) }
    fun togglePhoneSpeakers(enabled: Boolean) = updateSettings { it.copy(usePhoneSpeakers = enabled) }
    fun toggleLockScreen(enabled: Boolean) = updateSettings { it.copy(showOnLockScreen = enabled) }
    fun updateDefaultSnooze(minutes: Int) = updateSettings { it.copy(defaultSnoozeDuration = minutes) }
    fun updateDefaultGradualVolume(seconds: Int) = updateSettings { it.copy(defaultGradualVolume = seconds) }
    fun toggleShowWeather(enabled: Boolean) = updateSettings { it.copy(showWeatherOnDashboard = enabled) }
    fun toggleShowCalendar(enabled: Boolean) = updateSettings { it.copy(showCalendarOnDashboard = enabled) }
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
            val url = preferencesManager.getCurrentSettings().webhookUrl
            val ok = if (url.isBlank()) false else webhookService.test(url)
            _webhookTestResult.value = if (ok) "Webhook OK" else "Webhook failed — check URL"
            kotlinx.coroutines.delay(4000)
            _webhookTestResult.value = null
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
            val settings = preferencesManager.getCurrentSettings()
            val ok = try {
                val url = "http://${settings.hueBridgeIp}/api/${settings.hueApiKey}/lights"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
                val response = client.newCall(okhttp3.Request.Builder().url(url).build()).execute()
                response.isSuccessful.also { response.close() }
            } catch (_: Exception) { false }
            _hueTestResult.value = if (ok) "Hue bridge reachable" else "Hue bridge not found — check IP and key"
            kotlinx.coroutines.delay(4000)
            _hueTestResult.value = null
        }
    }

    fun setVacationMode(enabled: Boolean, startMillis: Long = 0, endMillis: Long = 0) {
        viewModelScope.launch {
            // Validate: end must be after start when enabling
            val validEnabled = if (enabled && startMillis > 0 && endMillis > 0) {
                endMillis > startMillis
            } else {
                enabled && startMillis > 0 && endMillis > 0
            }

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

    // Backup/restore
    private val _backupResult = MutableStateFlow<String?>(null)
    val backupResult: StateFlow<String?> = _backupResult.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportToUri(uri)
                .onSuccess { count -> setBackupResult("Exported $count alarms") }
                .onFailure { setBackupResult("Export failed: ${it.message}") }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.importFromUri(uri)
                .onSuccess { count -> setBackupResult("Imported $count alarms") }
                .onFailure { setBackupResult("Import failed: ${it.message}") }
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
}
