package com.sysadmindoc.alarmclock.ui.settings

import android.app.Application
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
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
import com.sysadmindoc.alarmclock.data.backup.BackupImportOptions
import com.sysadmindoc.alarmclock.data.backup.BackupImportPreview
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.backup.BackupExportWarning
import com.sysadmindoc.alarmclock.data.backup.BackupManager
import com.sysadmindoc.alarmclock.data.backup.FossifyImportManager
import com.sysadmindoc.alarmclock.data.backup.FossifyImportPreview
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepRepository
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.local.CommuteHistoryStore
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProofStore
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.support.SupportExportFile
import com.sysadmindoc.alarmclock.data.support.SupportExportManager
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.integration.hue.HueBridgeClient
import com.sysadmindoc.alarmclock.integration.hue.HueConnectionResult
import com.sysadmindoc.alarmclock.integration.hue.HuePinResult
import com.sysadmindoc.alarmclock.integration.hue.HueTrustStore
import com.sysadmindoc.alarmclock.service.WebhookService
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import com.sysadmindoc.alarmclock.util.ManufacturerCompat
import com.sysadmindoc.alarmclock.worker.CalendarAutoAlarmWorker
import com.sysadmindoc.alarmclock.worker.GuardianEscalationPolicy
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val batteryGuidanceUrl: String = "",
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
    val canUseFullScreenIntent: Boolean? = null,
    val hasLocalNetworkPermission: Boolean = true,
    val guardianReadiness: GuardianReadiness = GuardianReadiness(
        enabledAlarmCount = 0,
        smsPath = GuardianSmsPath.INACTIVE,
        hasSendSmsPermission = false,
        hasCallPhonePermission = false
    ),
    // v1.11.3 (roadmap N3): App Standby bucket awareness. UsageStatsManager
    // returns one of STANDBY_BUCKET_ACTIVE / WORKING_SET / FREQUENT / RARE /
    // RESTRICTED on API 28+. ACTIVE and WORKING_SET are the alarm-friendly
    // states; FREQUENT and worse mean Android is throttling our background
    // work, which can delay or drop the alarm-schedule path. UNKNOWN (-1)
    // means the API isn't available on this device or returned no data —
    // we surface a generic "Standby bucket unknown" in that case.
    val appStandbyBucket: Int = AppStandbyBucket.UNKNOWN,
    val testAlarmProof: TestAlarmProof = TestAlarmProof(),
    val healthConnectSleepSummary: HealthConnectSleepSummary = HealthConnectSleepSummary(),
    val incidentTimeline: SettingsIncidentTimelineState = SettingsIncidentTimelineState()
)

object AppStandbyBucket {
    const val UNKNOWN: Int = -1
    /** True when the bucket actively throttles alarm scheduling. */
    fun isDegraded(bucket: Int): Boolean = bucket >= 30  // FREQUENT == 30
}

data class SettingsIncidentTimelineState(
    val recentCount: Int = 0,
    val latestType: String? = null,
    val latestStatus: String? = null,
    val latestReason: String? = null,
    val latestEventAt: Long? = null,
    val latestElapsedMs: Long? = null,
    val latestIsDegraded: Boolean = false
) {
    val hasIncidents: Boolean
        get() = recentCount > 0

    companion object {
        fun from(events: List<AlarmIncidentEvent>): SettingsIncidentTimelineState {
            val sanitized = events.map { it.sanitized() }
            val notable = sanitized.firstOrNull { it.isNotable() }
            val latest = notable ?: sanitized.firstOrNull()
            return SettingsIncidentTimelineState(
                recentCount = sanitized.size,
                latestType = latest?.type,
                latestStatus = latest?.status,
                latestReason = latest?.reasonCode,
                latestEventAt = latest?.eventAt,
                latestElapsedMs = latest?.elapsedMs,
                latestIsDegraded = notable != null
            )
        }

        private fun AlarmIncidentEvent.isNotable(): Boolean {
            if (status == AlarmIncidentEvent.STATUS_FAILED ||
                status == AlarmIncidentEvent.STATUS_SKIPPED
            ) {
                return true
            }
            return type == AlarmIncidentEvent.TYPE_AUTO_SILENCE ||
                reasonCode.contains("FALLBACK") ||
                reasonCode.contains("MISSING") ||
                reasonCode.contains("BLOCKED")
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager,
    private val alarmScheduler: AlarmScheduler,
    private val backupManager: BackupManager,
    private val webhookService: WebhookService,
    private val healthConnectSleepRepository: HealthConnectSleepRepository,
    private val supportExportManager: SupportExportManager,
    private val alarmRepository: AlarmRepository,
    private val alarmIncidentRepository: AlarmIncidentRepository,
    private val hueBridgeClient: HueBridgeClient,
    private val hueTrustStore: HueTrustStore,
    private val commuteHistoryStore: CommuteHistoryStore,
    private val fossifyImportManager: FossifyImportManager
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
    private val _healthConnectSleepState = MutableStateFlow(HealthConnectSleepSummary())

    private val incidentTimelineState = alarmIncidentRepository.observeRecent(limit = 10)
        .map { SettingsIncidentTimelineState.from(it) }
        .catch { emit(SettingsIncidentTimelineState()) }

    private val guardianAlarmCountState = alarmRepository.observeAll()
        .map { alarms -> alarms.count { it.isEnabled && it.guardianEnabled } }
        .distinctUntilChanged()
        .catch { emit(0) }

    private val auxiliaryState = combine(
        _wakeReadinessState,
        _healthConnectSleepState,
        incidentTimelineState,
        guardianAlarmCountState
    ) { wakeReadiness, healthConnectSleep, incidentTimeline, guardianAlarmCount ->
        SettingsAuxiliaryState(wakeReadiness, healthConnectSleep, incidentTimeline, guardianAlarmCount)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.settings,
        _batteryState,
        _webhookTestState,
        _hueTestState,
        auxiliaryState
    ) { settings, battery, webhookState, hueState, auxiliary ->
        val wakeReadiness = auxiliary.wakeReadiness
        val guidance = ManufacturerCompat.getGuidance()
        SettingsUiState(
            settings = settings,
            isIgnoringBatteryOptimizations = battery.isIgnoring,
            needsBatteryGuidance = battery.needsGuidance,
            manufacturerName = guidance?.manufacturer ?: Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            batteryGuidanceSteps = guidance?.steps ?: emptyList(),
            batteryGuidanceTitle = guidance?.title ?: "",
            batteryGuidanceUrl = guidance?.dontKillMyAppUrl ?: "",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = BuildConfig.VERSION_NAME,
            webhookTestResult = webhookState.message,
            isWebhookTesting = webhookState.isRunning,
            hueTestResult = hueState.message,
            isHueTesting = hueState.isRunning,
            hasNotificationPermission = wakeReadiness.hasNotificationPermission,
            canScheduleExactAlarms = wakeReadiness.canScheduleExactAlarms,
            canUseFullScreenIntent = wakeReadiness.canUseFullScreenIntent,
            hasLocalNetworkPermission = wakeReadiness.hasLocalNetworkPermission,
            guardianReadiness = GuardianEscalationPolicy.readiness(
                flavor = BuildConfig.FLAVOR,
                enabledAlarmCount = auxiliary.guardianAlarmCount,
                hasSendSmsPermission = wakeReadiness.hasSendSmsPermission,
                hasCallPhonePermission = wakeReadiness.hasCallPhonePermission
            ),
            appStandbyBucket = wakeReadiness.appStandbyBucket,
            testAlarmProof = wakeReadiness.testAlarmProof,
            healthConnectSleepSummary = auxiliary.healthConnectSleep,
            incidentTimeline = auxiliary.incidentTimeline
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        refreshHealthConnectSleep()
    }

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

    fun requestFullScreenAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val context = getApplication<Application>()
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
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

    fun refreshWakeReadiness() {
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
    fun toggleReduceMotionAndFlashing(enabled: Boolean) =
        updateSettings { it.copy(reduceMotionAndFlashing = enabled) }
    fun toggleCoverToSnooze(enabled: Boolean) =
        updateSettings { it.copy(coverToSnoozeEnabled = enabled) }
    fun toggleRepeatMissed(enabled: Boolean) =
        updateSettings { it.copy(repeatMissedAlarms = enabled) }
    fun updateCancellationLockMinutes(minutes: Int) =
        updateSettings { it.copy(cancellationLockMinutes = minutes.coerceIn(0, 120)) }
    fun updateFiringControlMode(mode: String) =
        updateSettings { it.copy(firingControlMode = mode) }
    fun updateChallengeBypassEnabled(enabled: Boolean) =
        updateSettings { it.copy(challengeBypassEnabled = enabled) }
    fun updateChallengeBypassDelay(seconds: Int) =
        updateSettings { it.copy(challengeBypassDelaySeconds = seconds) }
    fun updateChallengeAudioDuckingEnabled(enabled: Boolean) =
        updateSettings { it.copy(challengeAudioDuckingEnabled = enabled) }
    fun updateChallengeAudioDuckPercent(percent: Int) =
        updateSettings { it.copy(challengeAudioDuckPercent = percent.coerceIn(10, 80)) }
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
    fun toggleHideAlarmLabelsOnPublicSurfaces(enabled: Boolean) =
        updateSettings { it.copy(hideAlarmLabelsOnPublicSurfaces = enabled) }
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
    fun toggleCalendarCommuteAware(enabled: Boolean) = updateCalendarAutoAlarmSettings {
        it.copy(calendarCommuteAwareEnabled = enabled)
    }
    fun updateCalendarCommuteBaselineMinutes(minutes: Int) = updateCalendarAutoAlarmSettings {
        it.copy(calendarCommuteBaselineMinutes = minutes.coerceIn(0, 240))
    }
    fun updateCalendarCommuteWeatherExtraMinutes(minutes: Int) = updateCalendarAutoAlarmSettings {
        it.copy(calendarCommuteWeatherExtraMinutes = minutes.coerceIn(0, 120))
    }
    fun clearLearnedCommuteHistory() {
        viewModelScope.launch(Dispatchers.IO) { commuteHistoryStore.clear() }
    }
    fun updateGoogleRoutesApiKey(key: String) = updateCalendarAutoAlarmSettings {
        it.copy(googleRoutesApiKey = key.trim())
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
    fun toggleWebhookLabelSharing(enabled: Boolean) = updateSettings { it.copy(webhookIncludeLabel = enabled) }
    fun updateWebhookSigningSecret(secret: String) = updateSettings {
        it.copy(webhookSigningSecret = secret.trim())
    }
    fun testWebhook() {
        viewModelScope.launch(Dispatchers.IO) {
            _webhookTestState.value = IntegrationTestState(
                message = "Checking webhook endpoint...",
                isRunning = true
            )
            val settings = preferencesManager.getCurrentSettings()
            val url = settings.webhookUrl
            val result = when {
                url.isBlank() -> "Webhook failed — add an HTTPS URL first"
                !webhookService.isAllowedUrl(url) && url.trim().startsWith("http://", ignoreCase = true) ->
                    "Webhook failed — use HTTPS; cleartext HTTP is blocked"
                !webhookService.isAllowedUrl(url) -> "Webhook failed — enter a valid HTTPS URL"
                LocalNetworkPermission.requiresPermissionForUrl(url) &&
                    !LocalNetworkPermission.isGranted(getApplication()) ->
                    "Webhook failed — allow local network access first"
                webhookService.test(
                    url = url,
                    includeLabel = settings.webhookIncludeLabel,
                    signingSecret = settings.webhookSigningSecret
                ) -> "Webhook OK"
                else -> "Webhook failed — endpoint did not return 2xx"
            }
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
    fun toggleHueLegacyHttp(enabled: Boolean) = updateSettings {
        it.copy(hueLegacyHttpEnabled = enabled)
    }
    fun clearHueCertificatePin() = updateSettings {
        it.copy(hueBridgeCertFingerprint = "")
    }
    fun testHue() {
        viewModelScope.launch(Dispatchers.IO) {
            _hueTestState.value = IntegrationTestState(
                message = "Checking Hue bridge...",
                isRunning = true
            )
            val settings = preferencesManager.getCurrentSettings()
            if (LocalNetworkPermission.isRuntimeRequired() &&
                !LocalNetworkPermission.isGranted(getApplication())
            ) {
                val result = "Hue bridge not checked — allow local network access first"
                _hueTestState.value = IntegrationTestState(message = result, isRunning = false)
                kotlinx.coroutines.delay(4000)
                if (_hueTestState.value.message == result) {
                    _hueTestState.value = IntegrationTestState()
                }
                return@launch
            }
            val connection = hueBridgeClient.testConnection(
                rawBridgeHost = settings.hueBridgeIp,
                rawApiKey = settings.hueApiKey,
                pinnedFingerprint = settings.hueBridgeCertFingerprint,
                allowLegacyHttp = settings.hueLegacyHttpEnabled
            )
            val result = when (connection) {
                is HueConnectionResult.V2Reachable -> when (
                    val pin = hueTrustStore.rememberFirstUse(connection.observedFingerprint)
                ) {
                    is HuePinResult.Accepted -> if (pin.newlyPinned) {
                        "Hue bridge reachable (API v2) — certificate saved"
                    } else {
                        "Hue bridge reachable (API v2)"
                    }
                    is HuePinResult.Changed ->
                        "Hue certificate changed — verify the bridge, then forget the saved certificate"
                    HuePinResult.Invalid -> "Hue bridge returned an invalid certificate fingerprint"
                }
                HueConnectionResult.V1Reachable ->
                    "Hue bridge reachable (legacy API v1 over HTTP)"
                is HueConnectionResult.CertificateChanged ->
                    "Hue certificate changed — verify the bridge, then forget the saved certificate"
                HueConnectionResult.InvalidConfiguration ->
                    "Hue bridge not checked — enter a valid IP and API key"
                is HueConnectionResult.Unreachable -> if (settings.hueLegacyHttpEnabled) {
                    "Hue bridge not found — check IP and key"
                } else {
                    "Hue API v2 not reachable — legacy HTTP is off"
                }
            }
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

    /**
     * v1.11.6 (roadmap N6): "Pause alarms for N days" — distinct from
     * vacation. `days` of 0 (or negative) clears the pause and resumes
     * normal scheduling. Otherwise the pause expires at midnight `days`
     * days from now (so "Pause for 1 day" means "skip tonight's and
     * tomorrow morning's alarms; resume the morning after"). Reschedule
     * is always called so the cancellation propagates immediately.
     */
    fun pauseAlarmsForDays(days: Int) {
        viewModelScope.launch {
            val expiry = if (days <= 0) 0L else {
                val nowMs = System.currentTimeMillis()
                val endOfDay = java.time.ZonedDateTime.now()
                    .plusDays(days.toLong())
                    .toLocalDate()
                    .atTime(java.time.LocalTime.MAX)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                // Defensive: never go backwards.
                maxOf(endOfDay, nowMs + 60_000L)
            }
            preferencesManager.update { it.copy(pauseUntilMillis = expiry) }
            alarmScheduler.rescheduleAll(forceRecalculate = true)
        }
    }

    /**
     * v1.13.2 (roadmap X1): toggles the local Health Connect opt-in. The
     * Play flavor requests only READ_SLEEP and reads recent sessions in
     * foreground UI; the F-Droid flavor keeps the value only for backup
     * compatibility.
     */
    fun updateHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.update { it.copy(healthConnectEnabled = enabled) }
            refreshHealthConnectSleep()
        }
    }

    fun healthConnectPermissionContract() =
        healthConnectSleepRepository.createPermissionRequestContract()

    fun requestHealthConnectPermissions(launch: (Set<String>) -> Unit) {
        val permissions = healthConnectSleepRepository.requiredPermissions
        if (permissions.isNotEmpty()) {
            launch(permissions)
        }
    }

    fun onHealthConnectPermissionsResult(grantedPermissions: Set<String>) {
        viewModelScope.launch {
            val granted = grantedPermissions.containsAll(healthConnectSleepRepository.requiredPermissions)
            if (granted) {
                preferencesManager.update { it.copy(healthConnectEnabled = true) }
            }
            refreshHealthConnectSleep()
        }
    }

    fun refreshHealthConnectSleep() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = preferencesManager.getCurrentSettings().healthConnectEnabled
            _healthConnectSleepState.value =
                healthConnectSleepRepository.readRecentSleepSummary(includeRecords = enabled)
        }
    }

    /** v1.11.6: Clear an active pause and re-arm alarms. */
    fun resumeAlarms() {
        viewModelScope.launch {
            preferencesManager.update { it.copy(pauseUntilMillis = 0L) }
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
    private val _supportExportResult = MutableStateFlow<String?>(null)
    val supportExportResult: StateFlow<String?> = _supportExportResult.asStateFlow()
    private val _supportExportBusy = MutableStateFlow(false)
    val supportExportBusy: StateFlow<Boolean> = _supportExportBusy.asStateFlow()

    suspend fun inspectBackupExportWarning(): BackupExportWarning =
        backupManager.inspectExportWarning()

    suspend fun inspectBackupImport(uri: Uri): Result<BackupImportPreview> =
        backupManager.inspectImportFromUri(uri)

    suspend fun inspectEncryptedBackupImport(
        uri: Uri,
        passphrase: String
    ): Result<BackupImportPreview> = backupManager.inspectEncryptedImportFromUri(uri, passphrase)

    suspend fun inspectFossifyImport(uri: Uri): Result<FossifyImportPreview> =
        fossifyImportManager.inspect(uri)

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.exportToUri(uri)
                    .onSuccess { count -> setBackupResult(backupSuccessMessage(BackupStatusKind.PlainExport, count)) }
                    .onFailure { setBackupResult(backupFailureMessage(BackupStatusKind.PlainExport, it)) }
            } catch (e: Exception) {
                setBackupResult(backupFailureMessage(BackupStatusKind.PlainExport, e))
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
                    .onSuccess { count -> setBackupResult(backupSuccessMessage(BackupStatusKind.EncryptedExport, count)) }
                    .onFailure { setBackupResult(backupFailureMessage(BackupStatusKind.EncryptedExport, it)) }
            } catch (e: Exception) {
                setBackupResult(backupFailureMessage(BackupStatusKind.EncryptedExport, e))
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importBackup(
        uri: Uri,
        options: BackupImportOptions = BackupImportOptions()
    ) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.importFromUri(uri, options)
                    .onSuccess { count -> setBackupResult(backupSuccessMessage(BackupStatusKind.PlainImport, count)) }
                    .onFailure { setBackupResult(backupFailureMessage(BackupStatusKind.PlainImport, it)) }
            } catch (e: Exception) {
                setBackupResult(backupFailureMessage(BackupStatusKind.PlainImport, e))
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importEncryptedBackup(
        uri: Uri,
        passphrase: String,
        options: BackupImportOptions = BackupImportOptions()
    ) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                backupManager.importEncryptedFromUri(uri, passphrase, options)
                    .onSuccess { count -> setBackupResult(backupSuccessMessage(BackupStatusKind.EncryptedImport, count)) }
                    .onFailure { setBackupResult(backupFailureMessage(BackupStatusKind.EncryptedImport, it)) }
            } catch (e: Exception) {
                setBackupResult(backupFailureMessage(BackupStatusKind.EncryptedImport, e))
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importFossifyAlarms(uri: Uri, expectedFingerprint: String) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                fossifyImportManager.import(uri, expectedFingerprint)
                    .onSuccess { count ->
                        setBackupResult("Imported $count Fossify alarm${if (count == 1) "" else "s"} as disabled for review.")
                    }
                    .onFailure { setBackupResult("Fossify import failed: ${it.message ?: "unexpected error"}") }
            } catch (e: Exception) {
                setBackupResult("Fossify import failed: ${e.message ?: "unexpected error"}")
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun showBackupResult(message: String) {
        setBackupResult(message)
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

    suspend fun createSupportExport(): Result<SupportExportFile> {
        _supportExportBusy.value = true
        return try {
            val export = withContext(Dispatchers.IO) {
                supportExportManager.createSupportExport()
            }
            setSupportExportResult("Support bundle ready to share")
            Result.success(export)
        } catch (e: Exception) {
            val message = backupFailureMessage(BackupStatusKind.SupportExport, e)
            setSupportExportResult(message)
            Result.failure(e)
        } finally {
            _supportExportBusy.value = false
        }
    }

    fun setSupportExportShareFailed() {
        setSupportExportResult("No app is available to share the support bundle")
    }

    fun clearSupportExportResult() { _supportExportResult.value = null }

    fun clearIncidentHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            alarmIncidentRepository.clearHistory()
        }
    }

    private fun setSupportExportResult(message: String) {
        _supportExportResult.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_supportExportResult.value == message) {
                _supportExportResult.value = null
            }
        }
    }

    private data class BatteryState(val isIgnoring: Boolean, val needsGuidance: Boolean)
    private data class IntegrationTestState(
        val message: String? = null,
        val isRunning: Boolean = false
    )

    private data class SettingsAuxiliaryState(
        val wakeReadiness: WakeReadinessState,
        val healthConnectSleep: HealthConnectSleepSummary,
        val incidentTimeline: SettingsIncidentTimelineState,
        val guardianAlarmCount: Int
    )

    private data class WakeReadinessState(
        val hasNotificationPermission: Boolean,
        val canScheduleExactAlarms: Boolean,
        val canUseFullScreenIntent: Boolean?,
        val hasLocalNetworkPermission: Boolean,
        val hasSendSmsPermission: Boolean,
        val hasCallPhonePermission: Boolean,
        val appStandbyBucket: Int,
        val testAlarmProof: TestAlarmProof
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
                val fullScreenIntentReady = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    runCatching {
                        context.getSystemService(NotificationManager::class.java)
                            ?.canUseFullScreenIntent()
                    }.getOrNull()
                } else {
                    null
                }
                val localNetworkReady = LocalNetworkPermission.isGranted(context)
                val sendSmsGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
                val callPhoneGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
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
                    canUseFullScreenIntent = fullScreenIntentReady,
                    hasLocalNetworkPermission = localNetworkReady,
                    hasSendSmsPermission = sendSmsGranted,
                    hasCallPhonePermission = callPhoneGranted,
                    appStandbyBucket = bucket,
                    testAlarmProof = TestAlarmProofStore.lastProof(context)
                )
            }
        }
    }
}
