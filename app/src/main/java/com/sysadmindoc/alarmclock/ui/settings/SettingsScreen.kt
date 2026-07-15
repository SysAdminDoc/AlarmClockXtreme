package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppFeedbackCard
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.adaptive.shouldUseTwoPaneLayout
import com.sysadmindoc.alarmclock.data.backup.BackupExportWarning
import com.sysadmindoc.alarmclock.data.backup.BackupImportMode
import com.sysadmindoc.alarmclock.data.backup.BackupImportOptions
import com.sysadmindoc.alarmclock.data.backup.BackupImportPreview
import com.sysadmindoc.alarmclock.data.health.HealthConnectAvailability
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof
import com.sysadmindoc.alarmclock.data.support.SupportExportFile
import com.sysadmindoc.alarmclock.ui.permissions.PermissionRequestCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SettingsPaneCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val settingsPaneCategories = listOf(
    SettingsPaneCategory(
        id = "readiness",
        title = "Readiness",
        description = "Permissions, diagnostics, battery, vacation, and pause controls.",
        icon = Icons.Default.Security
    ),
    SettingsPaneCategory(
        id = "defaults",
        title = "Defaults",
        description = "New-alarm behavior, dashboard content, and navigation tabs.",
        icon = Icons.Default.Alarm
    ),
    SettingsPaneCategory(
        id = "integrations",
        title = "Integrations",
        description = "Webhook, holidays, Hue, Health Connect, and connection status.",
        icon = Icons.Default.Link
    ),
    SettingsPaneCategory(
        id = "personalization",
        title = "Personalization",
        description = "Theme, challenge difficulty, privacy, and firing-screen behavior.",
        icon = Icons.Default.AutoAwesome
    ),
    SettingsPaneCategory(
        id = "backup",
        title = "Backup",
        description = "Encrypted export, restore preview, and conflict-safe import.",
        icon = Icons.Default.Backup
    ),
    SettingsPaneCategory(
        id = "utilities",
        title = "Utilities",
        description = "Companion tools, support bundle export, app version, and license.",
        icon = Icons.Default.Speed
    )
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToStopwatch: () -> Unit = {},
    onNavigateToBedtime: () -> Unit = {},
    onOpenOnboardingChecklist: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val supportExportResult by viewModel.supportExportResult.collectAsStateWithLifecycle()
    val supportExportBusy by viewModel.supportExportBusy.collectAsStateWithLifecycle()

    // v1.7.1: Re-check battery-optimisation status whenever the user returns
    // to this screen — most commonly after they bounced out to the system
    // "Battery & device care" page and granted the exemption. Without the
    // resume hook the chip / banner / row would all keep reading "Needs
    // setup" until the user manually navigated away and back.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWakeReadiness()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showDefaultSnoozeMenu by remember { mutableStateOf(false) }
    var showGradualVolumeMenu by remember { mutableStateOf(false) }
    var showAutoSilenceMenu by remember { mutableStateOf(false) }
    var showTemperatureMenu by remember { mutableStateOf(false) }
    var showCalendarLeadMenu by remember { mutableStateOf(false) }
    var showCommuteBaselineMenu by remember { mutableStateOf(false) }
    var showCommuteWeatherMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val screenScope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshWakeReadiness()
    }
    val guardianSmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshWakeReadiness()
    }
    val guardianCallPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshWakeReadiness()
    }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshWakeReadiness()
    }
    val healthConnectPermissionContract = remember { viewModel.healthConnectPermissionContract() }
    val requestHealthConnectPermissions: (() -> Unit)? = if (healthConnectPermissionContract != null) {
        val launcher = rememberLauncherForActivityResult(healthConnectPermissionContract) { granted ->
            viewModel.onHealthConnectPermissionsResult(granted)
        }
        ({
            viewModel.requestHealthConnectPermissions { permissions ->
                launcher.launch(permissions)
            }
        })
    } else {
        null
    }
    fun shareSupportExport(export: SupportExportFile) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = export.mimeType
            putExtra(Intent.EXTRA_STREAM, export.uri)
            putExtra(Intent.EXTRA_SUBJECT, "AlarmClockXtreme support bundle")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, "Share support bundle"))
        } catch (_: Exception) {
            viewModel.setSupportExportShareFailed()
            Toast.makeText(context, "No app is available to share this file.", Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        val useTwoPane = shouldUseTwoPaneLayout(maxWidth.value)
        var selectedPaneId by rememberSaveable { mutableStateOf(settingsPaneCategories.first().id) }
        val selectedPane = settingsPaneCategories.firstOrNull { it.id == selectedPaneId }
            ?: settingsPaneCategories.first()
        val showAllSettings = !useTwoPane

        val settingsContent: @Composable (Modifier) -> Unit = { contentModifier ->
            Column(modifier = contentModifier) {
                if (useTwoPane) {
                    SettingsPaneHeader(selectedPane, state)
                } else {
                    AlarmClockHeroHeader(
                        title = "Settings",
                        subtitle = "Tune the app once and it stays out of your way. Changes save immediately.",
                        badge = {
                            AppStatusChip(
                                label = if (state.isIgnoringBatteryOptimizations) "Battery protected" else "Needs battery setup",
                                icon = if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                                color = if (state.isIgnoringBatteryOptimizations) DismissGreen else SnoozeYellow
                            )
                            AppStatusChip(
                                label = state.appVersion,
                                icon = Icons.Default.AutoAwesome
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
            if (showAllSettings || selectedPane.id == "readiness") {
            WakeReadinessSection(
                state = state,
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestExactAlarms = viewModel::requestExactAlarmAccess,
                onRequestFullScreenAlarms = viewModel::requestFullScreenAlarmAccess,
                onRequestLocalNetworkPermission = {
                    localNetworkPermissionLauncher.launch(LocalNetworkPermission.ACCESS_LOCAL_NETWORK)
                },
                onRequestBatteryExemption = viewModel::requestBatteryExemption,
                onRequestGuardianSmsPermission = {
                    guardianSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                },
                onRequestGuardianCallPermission = {
                    guardianCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                },
                onOpenOnboardingChecklist = onOpenOnboardingChecklist
            )
            IncidentTimelineSection(
                timeline = state.incidentTimeline,
                use24Hour = state.settings.is24HourFormat,
                onClearIncidentHistory = viewModel::clearIncidentHistory
            )
            PermissionRequestCard(includeNotifications = false)
            SettingsOverviewRow(state)

            if (state.needsBatteryGuidance || !state.isIgnoringBatteryOptimizations) {
                BatteryOptimizationSection(state, viewModel)
            }

            PauseAlarmsSection(state, viewModel)

            VacationModeSection(state, viewModel)
            }

            if (showAllSettings || selectedPane.id == "defaults") {
            SettingsGroup(
                title = "Alarm defaults",
                description = "Set the behavior new alarms start with so setup feels faster and more predictable."
            ) {
                SettingsToggle(
                    label = "24-hour format",
                    checked = state.settings.is24HourFormat,
                    supportingText = "Use military time everywhere in the app.",
                    onToggle = viewModel::toggle24Hour
                )
                SettingsToggle(
                    label = "Show on lock screen",
                    checked = state.settings.showOnLockScreen,
                    supportingText = "Keep alarm controls visible without unlocking.",
                    onToggle = viewModel::toggleLockScreen
                )
                SettingsToggle(
                    label = "Hide public alarm and timer labels",
                    checked = state.settings.hideAlarmLabelsOnPublicSurfaces,
                    supportingText = "Use neutral text on lock screen, timer notifications, widget, quick settings, and Wear surfaces.",
                    onToggle = viewModel::toggleHideAlarmLabelsOnPublicSurfaces
                )
                SettingsToggle(
                    label = "Use phone speakers",
                    checked = state.settings.usePhoneSpeakers,
                    supportingText = "Route alarm playback through the loudspeaker even with accessories connected.",
                    onToggle = viewModel::togglePhoneSpeakers
                )
                SettingsToggle(
                    label = "Flip phone to snooze",
                    checked = state.settings.flipToSnoozeEnabled,
                    supportingText = "A quick face-down gesture can snooze instead of tapping the screen.",
                    onToggle = viewModel::toggleFlipToSnooze
                )

                SettingsActionRow(
                    label = "Default snooze",
                    value = "${state.settings.defaultSnoozeDuration} min",
                    supportingText = "Used whenever a new alarm doesn’t specify its own snooze length.",
                    onClick = { showDefaultSnoozeMenu = true }
                )
                DropdownMenu(
                    expanded = showDefaultSnoozeMenu,
                    onDismissRequest = { showDefaultSnoozeMenu = false }
                ) {
                    listOf(1, 3, 5, 10, 15, 20, 30).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text("$minutes minutes") },
                            onClick = {
                                viewModel.updateDefaultSnooze(minutes)
                                showDefaultSnoozeMenu = false
                            }
                        )
                    }
                }

                SettingsActionRow(
                    label = "Default volume ramp",
                    value = formatSeconds(state.settings.defaultGradualVolume),
                    supportingText = "Controls how gently new alarms fade in before reaching full volume.",
                    onClick = { showGradualVolumeMenu = true }
                )
                DropdownMenu(
                    expanded = showGradualVolumeMenu,
                    onDismissRequest = { showGradualVolumeMenu = false }
                ) {
                    listOf(0, 15, 30, 60, 90, 120, 180, 300).forEach { seconds ->
                        DropdownMenuItem(
                            text = {
                                Text(formatSeconds(seconds))
                            },
                            onClick = {
                                viewModel.updateDefaultGradualVolume(seconds)
                                showGradualVolumeMenu = false
                            }
                        )
                    }
                }

                SettingsActionRow(
                    label = "Auto-silence",
                    value = if (state.settings.autoSilenceMinutes == 0) "Never" else "${state.settings.autoSilenceMinutes} min",
                    supportingText = "Fail-safe timeout for alarms that keep ringing unattended.",
                    onClick = { showAutoSilenceMenu = true }
                )
                DropdownMenu(
                    expanded = showAutoSilenceMenu,
                    onDismissRequest = { showAutoSilenceMenu = false }
                ) {
                    listOf(0, 5, 10, 15, 30).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(if (minutes == 0) "Never" else "$minutes minutes") },
                            onClick = {
                                viewModel.updateAutoSilence(minutes)
                                showAutoSilenceMenu = false
                            }
                        )
                    }
                }
            }

            SettingsGroup(
                title = "Dashboard",
                description = "Control what appears on the My Day screen so it stays useful without feeling busy."
            ) {
                SettingsToggle(
                    label = "Show weather",
                    checked = state.settings.showWeatherOnDashboard,
                    supportingText = "Current conditions and a short forecast.",
                    onToggle = viewModel::toggleShowWeather
                )
                SettingsToggle(
                    label = "Show calendar",
                    checked = state.settings.showCalendarOnDashboard,
                    supportingText = "Display today’s events and meeting times.",
                    onToggle = viewModel::toggleShowCalendar
                )
                SettingsToggle(
                    label = "First-meeting auto-alarm",
                    checked = state.settings.calendarAutoAlarmEnabled,
                    supportingText = if (state.settings.calendarAutoAlarmEnabled) {
                        "Keeps one Calendar alarm shifted to tomorrow's first timed event."
                    } else {
                        "Create one reusable alarm before tomorrow's first timed event."
                    },
                    onToggle = viewModel::toggleCalendarAutoAlarm
                )
                SettingsActionRow(
                    label = "Meeting lead time",
                    value = "${state.settings.calendarAutoAlarmMinutesBefore} min",
                    supportingText = "How early the Calendar alarm fires before the first meeting.",
                    onClick = { showCalendarLeadMenu = true }
                )
                DropdownMenu(
                    expanded = showCalendarLeadMenu,
                    onDismissRequest = { showCalendarLeadMenu = false }
                ) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text("$minutes minutes") },
                            onClick = {
                                viewModel.updateCalendarAutoAlarmMinutes(minutes)
                                showCalendarLeadMenu = false
                            }
                        )
                    }
                }
                SettingsToggle(
                    label = "Commute-aware auto-alarm",
                    checked = state.settings.calendarCommuteAwareEnabled,
                    supportingText = "For first meetings with a location, add transit/weather buffer before the calendar lead time.",
                    enabled = state.settings.calendarAutoAlarmEnabled,
                    onToggle = viewModel::toggleCalendarCommuteAware
                )
                SettingsActionRow(
                    label = "Normal commute",
                    value = if (state.settings.calendarCommuteBaselineMinutes == 0) {
                        "Use lead time"
                    } else {
                        "${state.settings.calendarCommuteBaselineMinutes} min"
                    },
                    supportingText = "Route estimates above this baseline shift the auto-alarm earlier.",
                    onClick = { showCommuteBaselineMenu = true },
                    enabled = state.settings.calendarCommuteAwareEnabled
                )
                DropdownMenu(
                    expanded = showCommuteBaselineMenu,
                    onDismissRequest = { showCommuteBaselineMenu = false }
                ) {
                    listOf(0, 15, 30, 45, 60, 90, 120).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(if (minutes == 0) "Use meeting lead time" else "$minutes minutes") },
                            onClick = {
                                viewModel.updateCalendarCommuteBaselineMinutes(minutes)
                                showCommuteBaselineMenu = false
                            }
                        )
                    }
                }
                SettingsActionRow(
                    label = "Bad-weather buffer",
                    value = "${state.settings.calendarCommuteWeatherExtraMinutes} min",
                    supportingText = "Added when the event day forecast has snow, ice, storms, or heavy precipitation.",
                    onClick = { showCommuteWeatherMenu = true },
                    enabled = state.settings.calendarCommuteAwareEnabled
                )
                DropdownMenu(
                    expanded = showCommuteWeatherMenu,
                    onDismissRequest = { showCommuteWeatherMenu = false }
                ) {
                    listOf(0, 10, 15, 20, 30, 45, 60).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(if (minutes == 0) "No weather buffer" else "$minutes minutes") },
                            onClick = {
                                viewModel.updateCalendarCommuteWeatherExtraMinutes(minutes)
                                showCommuteWeatherMenu = false
                            }
                        )
                    }
                }
                BufferedSettingsTextField(
                    value = state.settings.googleRoutesApiKey,
                    onCommit = viewModel::updateGoogleRoutesApiKey,
                    label = { Text("Google Routes API key") },
                    placeholder = { Text("Optional for transit ETA") },
                    enabled = state.settings.calendarCommuteAwareEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (state.settings.calendarCommuteAwareEnabled && state.settings.googleRoutesApiKey.isBlank()) {
                    AppInlineNotice(
                        title = "Weather-only commute mode",
                        message = "Transit ETA is skipped without a Routes key. Events with locations still get the bad-weather buffer when the forecast degrades.",
                        icon = Icons.Default.Cloud,
                        color = AccentBlue
                    )
                }
                SettingsActionRow(
                    label = "Temperature unit",
                    value = if (state.settings.temperatureUnit == "celsius") "Celsius (\u00B0C)" else "Fahrenheit (\u00B0F)",
                    supportingText = "Applied across the dashboard and weather cards.",
                    onClick = { showTemperatureMenu = true }
                )
                DropdownMenu(
                    expanded = showTemperatureMenu,
                    onDismissRequest = { showTemperatureMenu = false }
                ) {
                    listOf("fahrenheit" to "Fahrenheit (\u00B0F)", "celsius" to "Celsius (\u00B0C)").forEach { (unit, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                if (unit != state.settings.temperatureUnit) {
                                    viewModel.toggleTemperatureUnit()
                                }
                                showTemperatureMenu = false
                            }
                        )
                    }
                }
            }

            SettingsGroup(
                title = "Bottom navigation",
                description = "Hide tabs you never use. Alarms and Settings always stay available."
            ) {
                SettingsToggle(
                    label = "Show Today tab",
                    checked = state.settings.showDashboardTab,
                    supportingText = "Daily overview — conditions, hourly, UV, sunrise/sunset, and live radar.",
                    onToggle = viewModel::toggleShowDashboardTab
                )
                SettingsToggle(
                    label = "Show Timer tab",
                    checked = state.settings.showTimerTab,
                    supportingText = "Countdown timers with multiple lanes.",
                    onToggle = viewModel::toggleShowTimerTab
                )
                SettingsToggle(
                    label = "Show World tab",
                    checked = state.settings.showWorldClockTab,
                    supportingText = "Track time zones for cities you care about.",
                    onToggle = viewModel::toggleShowWorldClockTab
                )
                SettingsToggle(
                    label = "Show News tab",
                    checked = state.settings.showNewsTab,
                    supportingText = "Public RSS feeds — Google News, BBC, NPR, Hacker News.",
                    onToggle = viewModel::toggleShowNewsTab
                )
                SettingsToggle(
                    label = "Live radar on Weather tab",
                    checked = state.settings.showRadarEmbed,
                    supportingText = "Embed Windy.com's animated precipitation radar below the conditions card.",
                    onToggle = viewModel::toggleShowRadarEmbed
                )
            }
            }

            if (showAllSettings || selectedPane.id == "integrations") {
            IntegrationsSection(state, viewModel)
            HolidaysSection(state, viewModel)
            PhilipsHueSection(state, viewModel)
            HealthConnectSection(
                state = state,
                viewModel = viewModel,
                onRequestPermissions = requestHealthConnectPermissions
            )
            ConnectionsSection(state)
            }
            if (showAllSettings || selectedPane.id == "personalization") {
            PersonalizationSection(state, viewModel)
            }
            if (showAllSettings || selectedPane.id == "backup") {
            BackupRestoreSection(viewModel)
            }

            if (showAllSettings || selectedPane.id == "utilities") {
            SettingsGroup(
                title = "Utilities",
                description = "Quick access to companion tools that round out the app."
            ) {
                UtilityShortcutCard(
                    icon = Icons.Default.BarChart,
                    title = "Alarm statistics",
                    description = "Review streaks, response times, and habits over time.",
                    onClick = onNavigateToStats
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.Speed,
                    title = "Stopwatch",
                    description = "Track laps with best and worst splits highlighted.",
                    onClick = onNavigateToStopwatch
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.Bedtime,
                    title = "Bedtime",
                    description = "Set a sleep goal and keep your routine feeling intentional.",
                    onClick = onNavigateToBedtime
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.DarkMode,
                    title = "Night clock",
                    description = "Full-screen bedside clock with a warm low-light glow for bedside use.",
                    onClick = {
                        val intent = Intent(
                            context,
                            com.sysadmindoc.alarmclock.ui.nightclock.NightClockActivity::class.java
                        )
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.BugReport,
                    title = "Export support bundle",
                    description = if (supportExportBusy) {
                        "Packaging local crash logs and redacted diagnostics..."
                    } else {
                        "Share local crash logs plus redacted wake, incident, and alarm diagnostics."
                    },
                    onClick = {
                        if (!supportExportBusy) {
                            screenScope.launch {
                                viewModel.createSupportExport()
                                    .onSuccess { export -> shareSupportExport(export) }
                            }
                        }
                    }
                )
            }

            if (supportExportBusy) {
                AppSurfaceCard(highlighted = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Packaging support bundle",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            supportExportResult?.let { message ->
                val failed = isFailureStatusMessage(message)
                AppFeedbackCard(
                    title = if (failed) "Support export failed" else "Support bundle ready",
                    message = message,
                    icon = if (failed) Icons.Default.Warning else Icons.Default.BugReport,
                    color = if (failed) AccentRed else DismissGreen,
                    onDismiss = viewModel::clearSupportExportResult
                )
            }

            SettingsGroup(
                title = "About",
                description = "A quick reference for what’s running on this device."
            ) {
                SettingsInfo("Version", state.appVersion)
                SettingsInfo("Device", state.deviceModel)
                SettingsInfo("Android", state.androidVersion)
                SettingsInfo("License", "Apache License 2.0")
                SettingsInfo("Source", "github.com/SysAdminDoc/AlarmClockXtreme")
            }
            }

            Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (useTwoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SettingsPaneRail(
                    categories = settingsPaneCategories,
                    selectedId = selectedPane.id,
                    onSelect = { selectedPaneId = it },
                    state = state,
                    modifier = Modifier
                        .widthIn(min = 248.dp, max = 304.dp)
                        .fillMaxHeight()
                )
                settingsContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            settingsContent(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun SettingsPaneHeader(
    category: SettingsPaneCategory,
    state: SettingsUiState
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSectionTitle(
            title = category.title,
            description = category.description,
            action = {
                AppStatusChip(
                    label = state.appVersion,
                    icon = Icons.Default.AutoAwesome
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = if (state.isIgnoringBatteryOptimizations) "Battery protected" else "Needs battery setup",
                icon = if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                color = if (state.isIgnoringBatteryOptimizations) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = category.title,
                icon = category.icon,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsPaneRail(
    categories: List<SettingsPaneCategory>,
    selectedId: String,
    onSelect: (String) -> Unit,
    state: SettingsUiState,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier.semantics {
            contentDescription = "Settings categories"
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settings",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Choose a group to keep wide screens focused.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = if (state.isIgnoringBatteryOptimizations) "Protected" else "Setup needed",
                    icon = if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                    color = if (state.isIgnoringBatteryOptimizations) DismissGreen else SnoozeYellow
                )
                AppStatusChip(
                    label = state.appVersion,
                    icon = Icons.Default.AutoAwesome
                )
            }

            HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))

            categories.forEach { category ->
                val selected = category.id == selectedId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onSelect(category.id) }
                        .semantics {
                            this.selected = selected
                            stateDescription = if (selected) "Selected" else "Not selected"
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        SurfaceLight.copy(alpha = 0.52f)
                    },
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            BorderSubtle
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = category.title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Text(
                                text = category.description,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentTimelineSection(
    timeline: SettingsIncidentTimelineState,
    use24Hour: Boolean,
    onClearIncidentHistory: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    SettingsGroup(
        title = "Alarm diagnostics",
        description = "Recent redacted incident codes explain alarm delivery without storing labels, URLs, contacts, or locations."
    ) {
        if (!timeline.hasIncidents) {
            SettingsInfo(
                label = "Incident history",
                description = "No incident events yet. New alarm fires will add compact local diagnostic codes here."
            )
            Text(
                text = "This history is separate from Statistics and is bounded to the newest 100 rows or 30 days.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            AppSectionTitle(
                title = if (timeline.latestIsDegraded) "Latest degraded event" else "Latest incident event",
                description = "${timeline.recentCount} recent diagnostic events retained locally.",
                action = {
                    AppStatusChip(
                        label = timeline.latestStatus.orEmpty().ifBlank { "UNKNOWN" },
                        icon = if (timeline.latestIsDegraded) Icons.Default.Warning else Icons.Default.CheckCircle,
                        color = if (timeline.latestIsDegraded) SnoozeYellow else DismissGreen
                    )
                }
            )
            SettingsInfo(
                label = incidentLabel(timeline.latestType),
                description = buildString {
                    append(formatIncidentTimestamp(timeline.latestEventAt, use24Hour))
                    append(" - ")
                    append(formatIncidentElapsed(timeline.latestElapsedMs))
                }
            )
            SettingsInfo(
                label = "Reason code",
                description = timeline.latestReason.orEmpty().ifBlank { "NONE" }
            )
            Text(
                text = "Clearing diagnostics removes only incident rows. Alarm statistics and support-export crash logs are kept separate.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = { showClearDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
            ) {
                Text("Clear diagnostics")
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SnoozeYellow)
            },
            title = { Text("Clear alarm diagnostics?") },
            text = {
                Text(
                    text = "This deletes the redacted incident timeline only. Alarm statistics, alarms, backups, and crash logs are not changed.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearIncidentHistory()
                    }
                ) {
                    Text("Clear diagnostics", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsOverviewRow(state: SettingsUiState) {
    // v1.11.3 (roadmap N3): standby bucket also counts when known and degraded
    // — otherwise an UNKNOWN value (pre-API-28) or a healthy bucket doesn't
    // drag the tile state.
    val standbyOk = state.appStandbyBucket == AppStandbyBucket.UNKNOWN ||
        !AppStandbyBucket.isDegraded(state.appStandbyBucket)
    val fullScreenOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        state.canUseFullScreenIntent == true
    val localNetworkOk = !requiresLocalNetworkAccess(state) ||
        state.hasLocalNetworkPermission
    val reliabilityReady = state.isIgnoringBatteryOptimizations &&
        state.hasNotificationPermission &&
        state.canScheduleExactAlarms &&
        fullScreenOk &&
        localNetworkOk &&
        standbyOk
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppSectionTitle(
            title = "At a glance",
            description = "The highest-impact preferences, without digging through every category."
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsOverviewTile(
                title = "Reliability",
                value = if (reliabilityReady) "Ready" else "Needs review",
                supporting = wakeReadinessSummary(state),
                icon = if (reliabilityReady) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                accent = if (reliabilityReady) DismissGreen else SnoozeYellow,
                modifier = Modifier.width(190.dp)
            )
            SettingsOverviewTile(
                title = "Dashboard",
                value = dashboardSummary(state),
                supporting = "Visibility and calendar automation",
                icon = Icons.Default.Cloud,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(190.dp)
            )
            SettingsOverviewTile(
                title = "Wake style",
                value = if (state.settings.is24HourFormat) "24-hour" else "12-hour",
                supporting = "Default snooze ${state.settings.defaultSnoozeDuration} min",
                icon = Icons.Default.AutoAwesome,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(190.dp)
            )
        }
    }
}

@Composable
private fun SettingsOverviewTile(
    title: String,
    value: String,
    supporting: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier.heightIn(min = 160.dp),
        highlighted = accent == DismissGreen || accent == SnoozeYellow
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.14f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = title,
            color = TextMuted,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = supporting,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun WakeReadinessSection(
    state: SettingsUiState,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onRequestFullScreenAlarms: () -> Unit,
    onRequestLocalNetworkPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRequestGuardianSmsPermission: () -> Unit,
    onRequestGuardianCallPermission: () -> Unit,
    onOpenOnboardingChecklist: () -> Unit
) {
    // v1.11.3 (roadmap N3): Standby bucket is only surfaced when the API is
    // available (API 28+) AND the bucket is actually concerning. ACTIVE /
    // WORKING_SET get a quiet "Active" status; FREQUENT or worse show a
    // warning row with an action that opens the battery-exemption screen,
    // which is the right place to ask for the exemption that promotes us
    // back to WORKING_SET.
    val standbyKnown = state.appStandbyBucket != AppStandbyBucket.UNKNOWN
    val standbyDegraded = standbyKnown && AppStandbyBucket.isDegraded(state.appStandbyBucket)
    val standbyReady = standbyKnown && !standbyDegraded
    val standbyRowVisible = standbyKnown  // only show the row when we have a real value
    val fullScreenRowVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    val localNetworkRowVisible = requiresLocalNetworkAccess(state)
    val testAlarmProofReady = state.testAlarmProof.hasDetailedCompletion

    val checks = buildList {
        add(state.canScheduleExactAlarms)
        add(state.hasNotificationPermission)
        add(testAlarmProofReady)
        if (fullScreenRowVisible) add(state.canUseFullScreenIntent == true)
        if (localNetworkRowVisible) add(state.hasLocalNetworkPermission)
        add(state.isIgnoringBatteryOptimizations)
        if (standbyRowVisible) add(standbyReady)
        if (state.guardianReadiness.hasEnabledAlarms) {
            add(!state.guardianReadiness.needsUserAction)
        }
    }
    val readyCount = checks.count { it }
    val total = checks.size
    val allReady = readyCount == total

    AppSurfaceCard(highlighted = !allReady) {
        AppSectionTitle(
            title = "Wake readiness",
            description = "The system switches that matter most for precise alarms and visible alerts.",
            action = {
                AppStatusChip(
                    label = "$readyCount of $total ready",
                    icon = if (allReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (allReady) DismissGreen else SnoozeYellow
                )
            }
        )

        OutlinedButton(
            onClick = onOpenOnboardingChecklist,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
        ) {
            Text("Open setup checklist")
        }

        WakeReadinessRow(
            icon = Icons.Default.Alarm,
            title = "Real test alarm",
            description = testAlarmProofDescription(
                proof = state.testAlarmProof,
                is24HourFormat = state.settings.is24HourFormat
            ),
            ready = testAlarmProofReady,
            statusLabel = testAlarmProofStatusLabel(state.testAlarmProof),
            actionLabel = "Run setup checklist",
            onAction = onOpenOnboardingChecklist
        )
        WakeReadinessRow(
            icon = Icons.Default.Alarm,
            title = "Exact alarm access",
            description = "Keeps wake times precise through Doze and standby.",
            ready = state.canScheduleExactAlarms,
            actionLabel = "Open alarm access",
            onAction = onRequestExactAlarms
        )
        WakeReadinessRow(
            icon = Icons.Default.NotificationsActive,
            title = "Alarm notifications",
            description = "Shows next-alarm, ringing, missed, and wake-check alerts.",
            ready = state.hasNotificationPermission,
            actionLabel = "Allow notifications",
            onAction = onRequestNotifications
        )
        if (fullScreenRowVisible) {
            WakeReadinessRow(
                icon = Icons.Default.NotificationsActive,
                title = "Full-screen alarm access",
                description = when (state.canUseFullScreenIntent) {
                    true -> "Allows the ringing screen to open over the lock screen on Android 14+."
                    false -> "Android may only show a notification until full-screen alarm access is allowed."
                    null -> "Settings could not confirm full-screen alarm access; review the platform setting."
                },
                ready = state.canUseFullScreenIntent == true,
                actionLabel = "Open full-screen settings",
                onAction = onRequestFullScreenAlarms
            )
        }
        if (localNetworkRowVisible) {
            WakeReadinessRow(
                icon = Icons.Default.Link,
                title = "Local network access",
                description = "Allows Android 17+ Hue bridge checks and local webhook endpoints on your LAN.",
                ready = state.hasLocalNetworkPermission,
                actionLabel = "Allow local network",
                onAction = onRequestLocalNetworkPermission
            )
        }
        WakeReadinessRow(
            icon = Icons.Default.BatteryAlert,
            title = "Battery protection",
            description = "Reduces the chance of OEM battery rules delaying alarm work.",
            ready = state.isIgnoringBatteryOptimizations,
            actionLabel = "Open battery settings",
            onAction = onRequestBatteryExemption
        )
        if (standbyRowVisible) {
            WakeReadinessRow(
                icon = Icons.Default.BatteryAlert,
                title = "App Standby bucket",
                description = standbyBucketDescription(state.appStandbyBucket),
                ready = standbyReady,
                actionLabel = "Open battery settings",
                onAction = onRequestBatteryExemption
            )
        }
        if (state.guardianReadiness.hasEnabledAlarms) {
            WakeReadinessRow(
                icon = Icons.Default.Security,
                title = "Guardian Angel escalation",
                description = guardianReadinessDescription(state.guardianReadiness),
                ready = !state.guardianReadiness.needsUserAction,
                statusLabel = guardianReadinessStatusLabel(state.guardianReadiness),
                actionLabel = guardianReadinessActionLabel(state.guardianReadiness),
                onAction = {
                    if (state.guardianReadiness.needsSmsPermission) {
                        onRequestGuardianSmsPermission()
                    } else {
                        onRequestGuardianCallPermission()
                    }
                }
            )
        }
    }
}

/**
 * v1.11.3: Map the raw bucket value to a sentence we can show in the row.
 * Values from `UsageStatsManager.STANDBY_BUCKET_*` constants (API 28+).
 */
private fun standbyBucketDescription(bucket: Int): String = when (bucket) {
    in Int.MIN_VALUE..0 -> "Standby bucket unknown on this device."
    10 -> "Active — alarms scheduled without throttling."
    20 -> "Working set — light throttling only; alarms fire on time."
    30 -> "Frequent — Android is throttling background work; alarms may be delayed."
    40 -> "Rare — strong throttling; alarms may fire late or be skipped."
    45 -> "Restricted — Android caps you to one alarm per day. Open battery settings and exempt the app."
    else -> "Standby bucket $bucket — open battery settings to promote the app to Working set."
}

private fun testAlarmProofStatusLabel(proof: TestAlarmProof): String = when {
    proof.hasDetailedCompletion -> "Verified"
    proof.legacyCompleted -> "Refresh"
    proof.firedAt > 0L -> "Dismiss test"
    else -> "Run test"
}

private fun testAlarmProofDescription(
    proof: TestAlarmProof,
    is24HourFormat: Boolean
): String {
    if (proof.hasDetailedCompletion) {
        val completed = formatTestAlarmProofTime(proof.completedAt, is24HourFormat)
        val latency = proof.latencyMs?.let(::formatTestAlarmLatency)
        val delivery = testAlarmDeliveryPath(proof)
        return if (latency != null) {
            "Last dismissed $completed; rang $latency via $delivery."
        } else {
            "Last dismissed $completed; delivery timing was not captured."
        }
    }
    if (proof.legacyCompleted) {
        return "Completed before detailed proof was added; run the setup test again to refresh timestamp and latency."
    }
    if (proof.firedAt > 0L) {
        return "The last test rang at ${formatTestAlarmProofTime(proof.firedAt, is24HourFormat)} but was not dismissed in setup."
    }
    return "Run the setup test to prove this device can launch, alert, and dismiss a real alarm."
}

private fun formatTestAlarmProofTime(epochMillis: Long, is24HourFormat: Boolean): String {
    if (epochMillis <= 0L) return "unknown time"
    val pattern = if (is24HourFormat) "EEE HH:mm" else "EEE h:mm a"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

private fun formatTestAlarmLatency(latencyMs: Long): String {
    if (latencyMs < 1_500L) return "on time"
    val totalSeconds = ((latencyMs + 999L) / 1_000L).coerceAtLeast(1L)
    if (totalSeconds < 60L) return "${totalSeconds}s after schedule"
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (seconds == 0L) {
        "${minutes}m after schedule"
    } else {
        "${minutes}m ${seconds}s after schedule"
    }
}

private fun testAlarmDeliveryPath(proof: TestAlarmProof): String {
    val parts = buildList {
        if (proof.notificationPermissionGranted) add("notification")
        if (proof.fullScreenIntentRequested) add("full-screen request")
        if (proof.activityLaunchSucceeded) add("direct screen launch")
    }
    return parts.joinToString(" + ").ifBlank { "alarm screen" }
}

private fun guardianReadinessDescription(readiness: GuardianReadiness): String {
    val alarmCount = if (readiness.enabledAlarmCount == 1) {
        "1 Guardian alarm"
    } else {
        "${readiness.enabledAlarmCount} Guardian alarms"
    }
    val callPath = if (readiness.hasCallPhonePermission) {
        "Direct call permission is granted."
    } else {
        "Call fallback opens the dialer because CALL_PHONE is not granted."
    }
    return when (readiness.smsPath) {
        GuardianSmsPath.INACTIVE -> "No Guardian alarms are enabled."
        GuardianSmsPath.DIRECT_SMS ->
            "$alarmCount can send automatic SMS in F-Droid. $callPath"
        GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION ->
            "$alarmCount will open the SMS composer until SEND_SMS is allowed. $callPath"
        GuardianSmsPath.SMS_COMPOSER ->
            "$alarmCount uses a prefilled SMS composer in this build. $callPath"
    }
}

private fun guardianReadinessStatusLabel(readiness: GuardianReadiness): String {
    if (readiness.needsUserAction) return "Review"
    return when (readiness.smsPath) {
        GuardianSmsPath.INACTIVE -> "Off"
        GuardianSmsPath.DIRECT_SMS -> "Direct SMS"
        GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION -> "Review"
        GuardianSmsPath.SMS_COMPOSER -> "Composer"
    }
}

private fun guardianReadinessActionLabel(readiness: GuardianReadiness): String =
    if (readiness.needsSmsPermission) "Allow SMS" else "Allow calls"

@Composable
private fun WakeReadinessRow(
    icon: ImageVector,
    title: String,
    description: String,
    ready: Boolean,
    statusLabel: String = if (ready) "Ready" else "Review",
    actionLabel: String,
    onAction: () -> Unit
) {
    val accent = if (ready) DismissGreen else SnoozeYellow
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (ready) SurfaceCard.copy(alpha = 0.34f) else accent.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            if (ready) TextMuted.copy(alpha = 0.12f) else accent.copy(alpha = 0.24f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                AppStatusChip(
                    label = statusLabel,
                    icon = if (ready) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = accent
                )
            }

            if (!ready) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * v1.11.6 (roadmap N6): "Pause alarms for N days" card. Distinct from
 * vacation: this is a single-tap hard-suspend for an unplanned interruption
 * (sickness, weekend visit, etc.) and applies to one-shots too. Each chip
 * resets the pause to "N days from now"; "Resume now" clears it.
 */
@Composable
private fun PauseAlarmsSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val pauseUntil = state.settings.pauseUntilMillis
    val now = System.currentTimeMillis()
    val isPaused = pauseUntil > now
    val resumeAtLabel = if (isPaused) {
        java.time.Instant.ofEpochMilli(pauseUntil)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM d"))
    } else null

    AppSurfaceCard(highlighted = isPaused) {
        AppSectionTitle(
            title = "Pause alarms",
            description = if (isPaused) {
                "All alarms suspended until end of $resumeAtLabel."
            } else {
                "One-tap suspend for an unplanned interruption — works for one-shot alarms too."
            },
            action = {
                AppStatusChip(
                    label = if (isPaused) "Paused" else "Off",
                    icon = Icons.Default.BeachAccess,
                    color = if (isPaused) SnoozeYellow else TextMuted
                )
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1, 3, 7, 14).forEach { days ->
                AppFilterChip(
                    label = if (days == 1) "Tonight" else "$days days",
                    selected = false,
                    onClick = { viewModel.pauseAlarmsForDays(days) }
                )
            }
            if (isPaused) {
                AppFilterChip(
                    label = "Resume now",
                    selected = true,
                    onClick = { viewModel.resumeAlarms() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationModeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val settings = state.settings
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startDate = if (settings.vacationStartMillis > 0) {
        Instant.ofEpochMilli(settings.vacationStartMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } else {
        "Choose start date"
    }

    val endDate = if (settings.vacationEndMillis > 0) {
        Instant.ofEpochMilli(settings.vacationEndMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } else {
        "Choose end date"
    }

    AppSurfaceCard(highlighted = settings.vacationModeEnabled) {
        AppSectionTitle(
            title = "Vacation mode",
            description = "Silence repeating alarms during a time away without turning them off permanently.",
            action = {
                AppStatusChip(
                    label = if (settings.vacationModeEnabled) "Active" else "Off",
                    icon = Icons.Default.BeachAccess,
                    color = if (settings.vacationModeEnabled) SnoozeYellow else TextMuted
                )
            }
        )

        SettingsToggle(
            label = "Enable vacation schedule",
            checked = settings.vacationModeEnabled,
            supportingText = "Repeating alarms stay enabled but skip dates inside the range below.",
            onToggle = { enabled ->
                if (enabled) {
                    val start = settings.vacationStartMillis.takeIf { it > 0 }
                        ?: System.currentTimeMillis()
                    val end = settings.vacationEndMillis.takeIf { it > start }
                        ?: (start + 7 * 24 * 60 * 60 * 1000L)
                    viewModel.setVacationMode(true, start, end)
                } else {
                    viewModel.setVacationMode(
                        false,
                        settings.vacationStartMillis,
                        settings.vacationEndMillis
                    )
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField(
                modifier = Modifier.weight(1f),
                label = "Starts",
                value = startDate,
                onClick = { showStartPicker = true }
            )
            DateField(
                modifier = Modifier.weight(1f),
                label = "Ends",
                value = endDate,
                onClick = { showEndPicker = true }
            )
        }
    }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (settings.vacationStartMillis > 0) {
                settings.vacationStartMillis
            } else {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setVacationMode(
                            settings.vacationModeEnabled,
                            millis,
                            settings.vacationEndMillis
                        )
                    }
                    showStartPicker = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (settings.vacationEndMillis > 0) {
                settings.vacationEndMillis
            } else {
                System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setVacationMode(
                            settings.vacationModeEnabled,
                            settings.vacationStartMillis,
                            millis
                        )
                    }
                    showEndPicker = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun BatteryOptimizationSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val accent = if (state.isIgnoringBatteryOptimizations) DismissGreen else SnoozeYellow

    AppSurfaceCard(highlighted = !state.isIgnoringBatteryOptimizations) {
        AppSectionTitle(
            title = "Battery optimization",
            description = if (state.isIgnoringBatteryOptimizations) {
                "Your device is configured for reliable alarm delivery."
            } else {
                "Some Android vendors aggressively pause background work. This is the biggest reliability risk for alarms."
            },
            action = {
                AppStatusChip(
                    label = if (state.isIgnoringBatteryOptimizations) "Ready" else "Action recommended",
                    icon = if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = accent
                )
            }
        )

        if (!state.isIgnoringBatteryOptimizations) {
            Button(
                onClick = viewModel::requestBatteryExemption,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open battery settings")
            }
        }

        if (state.needsBatteryGuidance && state.batteryGuidanceSteps.isNotEmpty()) {
            HorizontalDivider(color = TextMuted.copy(alpha = 0.18f))
            Text(
                text = state.batteryGuidanceTitle.ifBlank { "${state.manufacturerName} battery steps" },
                color = accent,
                style = MaterialTheme.typography.titleSmall
            )
            state.batteryGuidanceSteps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppStatusChip(
                        label = "${index + 1}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (state.batteryGuidanceUrl.isNotBlank()) {
                val uriHandler = LocalUriHandler.current
                TextButton(
                    onClick = { runCatching { uriHandler.openUri(state.batteryGuidanceUrl) } }
                ) {
                    Text("View the up-to-date ${state.manufacturerName} guide at dontkillmyapp.com")
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppSectionTitle(
            title = title,
            description = description
        )
        AppSurfaceCard {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (!enabled) {
            SurfaceLight.copy(alpha = 0.34f)
        } else if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            SurfaceLight.copy(alpha = 0.58f)
        },
        border = BorderStroke(
            1.dp,
            if (!enabled) {
                BorderSubtle.copy(alpha = 0.55f)
            } else if (checked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            } else {
                BorderSubtle
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    color = if (enabled) TextPrimary else TextMuted,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        supportingText,
                        color = if (enabled) TextSecondary else TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Switch(
                modifier = Modifier.clearAndSetSemantics { },
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = appSwitchColors()
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    value: String,
    supportingText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) SurfaceLight.copy(alpha = 0.58f) else SurfaceLight.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, if (enabled) BorderSubtle else BorderSubtle.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = if (enabled) TextPrimary else TextMuted,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        TextMuted.copy(alpha = 0.10f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            value,
                            color = if (enabled) MaterialTheme.colorScheme.primary else TextMuted,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else TextMuted
                        )
                    }
                }
            }
            if (!supportingText.isNullOrBlank()) {
                Text(
                    supportingText,
                    color = if (enabled) TextSecondary else TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsInfo(label: String, description: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun IntegrationsSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = "Webhook integrations",
        description = "Connect alarm events to automations, home setups, or debugging endpoints."
    ) {
        SettingsToggle(
            label = "Enable webhook",
            checked = state.settings.webhookEnabled,
            supportingText = "Send v1 JSON events for alarm_fired, alarm_snoozed, alarm_dismissed, alarm_missed, and alarm_skipped.",
            onToggle = viewModel::toggleWebhook
        )

        BufferedSettingsTextField(
            value = state.settings.webhookUrl,
            onCommit = viewModel::updateWebhookUrl,
            label = { Text("Webhook URL") },
            placeholder = { Text("https://example.com/hook") },
            enabled = state.settings.webhookEnabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SettingsToggle(
            label = "Include alarm labels",
            checked = state.settings.webhookIncludeLabel,
            supportingText = "When off, payloads send labelIncluded=false instead of the alarm name.",
            enabled = state.settings.webhookEnabled,
            onToggle = viewModel::toggleWebhookLabelSharing
        )

        BufferedSettingsTextField(
            value = state.settings.webhookSigningSecret,
            onCommit = viewModel::updateWebhookSigningSecret,
            label = { Text("Signing secret") },
            placeholder = { Text("Optional HMAC secret") },
            enabled = state.settings.webhookEnabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Warn if the user pasted a plain-http endpoint. ACX intentionally
        // keeps app-wide cleartext traffic disabled, so these endpoints cannot
        // be treated as reliable on current Android.
        val urlLower = state.settings.webhookUrl.trim().lowercase()
        val plainHttpWarning = state.settings.webhookEnabled &&
                urlLower.startsWith("http://")
        val localWebhookPermissionMissing = state.settings.webhookEnabled &&
            LocalNetworkPermission.isRuntimeRequired() &&
            LocalNetworkPermission.isLikelyLocalEndpoint(state.settings.webhookUrl) &&
            !state.hasLocalNetworkPermission

        if (plainHttpWarning) {
            AppInlineNotice(
                title = "Webhook blocked",
                message = "Plain HTTP webhooks are blocked. Use an HTTPS endpoint or a local HTTPS bridge for Home Assistant or Tasker.",
                icon = Icons.Default.Warning,
                color = AccentRed
            )
        }
        if (localWebhookPermissionMissing) {
            AppInlineNotice(
                title = "Local network access needed",
                message = "Android 17+ requires local network access before this LAN webhook can be tested or fired.",
                icon = Icons.Default.Link,
                color = SnoozeYellow
            )
        }

        val lastDeliveryStatus = formatWebhookDeliveryStatus(state.settings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.webhookTestResult
                    ?: lastDeliveryStatus
                    ?: "Payloads include schemaVersion, eventId, occurredAt, scheduledFor, displayTime, and labelIncluded.",
                color = when {
                    state.isWebhookTesting -> MaterialTheme.colorScheme.primary
                    state.webhookTestResult?.contains("OK") == true -> DismissGreen
                    state.webhookTestResult == null && lastDeliveryStatus?.contains("OK") == true -> DismissGreen
                    state.webhookTestResult == null && lastDeliveryStatus != null -> AccentRed
                    state.webhookTestResult != null -> AccentRed
                    else -> TextMuted
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(
                onClick = viewModel::testWebhook,
                enabled = state.settings.webhookEnabled &&
                    state.settings.webhookUrl.isNotBlank() &&
                    !localWebhookPermissionMissing &&
                    !state.isWebhookTesting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isWebhookTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (state.isWebhookTesting) "Testing" else "Test")
            }
        }

        val deliveryLog = state.settings.webhookDeliveryLog
        if (deliveryLog.isNotBlank()) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Recent deliveries",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.size(4.dp))
            deliveryLog.lineSequence().filter { it.isNotBlank() }.take(8).forEach { line ->
                Text(
                    text = formatWebhookLogLine(line),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (line.contains("OK")) DismissGreen else AccentRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Render a stored "<ISO instant> <status>" delivery-log line as a friendly
 * local time. Falls back to the raw line if the leading token isn't an instant.
 */
private fun formatWebhookLogLine(line: String): String {
    val spaceIdx = line.indexOf(' ')
    if (spaceIdx <= 0) return line
    val instantPart = line.substring(0, spaceIdx)
    val rest = line.substring(spaceIdx + 1)
    return runCatching {
        val local = Instant.parse(instantPart)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US))
        "$local — $rest"
    }.getOrDefault(line)
}

@Composable
private fun HolidaysSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = "Public holidays",
        description = "Automatically skip alarms on country-specific holidays without disabling the alarm itself."
    ) {
        SettingsToggle(
            label = "Skip alarms on holidays",
            checked = state.settings.holidayAutoSkipEnabled,
            supportingText = "Useful for weekday alarms that should respect bank holidays and public closures.",
            onToggle = viewModel::toggleHolidayAutoSkip
        )
        BufferedSettingsTextField(
            value = state.settings.holidayCountryCode,
            onCommit = viewModel::updateHolidayCountryCode,
            transformInput = { newValue ->
                newValue
                    .filter(Char::isLetter)
                    .uppercase(Locale.US)
                    .take(2)
            },
            label = { Text("Country code") },
            placeholder = { Text("US, GB, DE...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            )
        )
        Text(
            text = if (state.settings.holidayAutoSkipEnabled) {
                "Holiday data comes from Nager.Date and is cached locally for a week."
            } else {
                "Choose the country now if you want. Holiday skipping only activates when the toggle above is on."
            },
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PhilipsHueSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showForgetCertificateDialog by remember { mutableStateOf(false) }
    val localNetworkPermissionMissing = LocalNetworkPermission.isRuntimeRequired() &&
        state.settings.hueBridgeIp.isNotBlank() &&
        !state.hasLocalNetworkPermission
    SettingsGroup(
        title = "Philips Hue sunrise",
        description = "Wake the room up gradually before the alarm sound takes over."
    ) {
        BufferedSettingsTextField(
            value = state.settings.hueBridgeIp,
            onCommit = viewModel::updateHueBridgeIp,
            label = { Text("Bridge IP address") },
            placeholder = { Text("192.168.1.100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        BufferedSettingsTextField(
            value = state.settings.hueApiKey,
            onCommit = viewModel::updateHueApiKey,
            label = { Text("Hue API key") },
            placeholder = { Text("Press the Hue bridge button first") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        BufferedSettingsTextField(
            value = state.settings.hueLightIds,
            onCommit = viewModel::updateHueLightIds,
            label = { Text("Light IDs") },
            placeholder = { Text("1,2,3") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SettingsToggle(
            label = "Allow legacy Hue API v1 over HTTP",
            checked = state.settings.hueLegacyHttpEnabled,
            supportingText = "Off by default. Enable only for an older bridge that cannot use encrypted API v2.",
            onToggle = viewModel::toggleHueLegacyHttp
        )
        if (state.settings.hueBridgeCertFingerprint.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bridge certificate pinned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = state.settings.hueBridgeCertFingerprint.take(16) + "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                TextButton(onClick = { showForgetCertificateDialog = true }) {
                    Text("Forget")
                }
            }
        }
        if (localNetworkPermissionMissing) {
            AppInlineNotice(
                title = "Local network access needed",
                message = "Android 17+ requires local network access before ACX can reach this Hue bridge.",
                icon = Icons.Default.Link,
                color = SnoozeYellow
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.hueTestResult ?: "Run a quick bridge check once the IP and API key are in place.",
                color = when {
                    state.isHueTesting -> MaterialTheme.colorScheme.primary
                    state.hueTestResult?.contains("reachable") == true -> DismissGreen
                    state.hueTestResult != null -> AccentRed
                    else -> TextMuted
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(
                onClick = viewModel::testHue,
                enabled = state.settings.hueBridgeIp.isNotBlank() &&
                    state.settings.hueApiKey.isNotBlank() &&
                    !localNetworkPermissionMissing &&
                    !state.isHueTesting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isHueTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (state.isHueTesting) "Testing" else "Test")
            }
        }
    }
    if (showForgetCertificateDialog) {
        AlertDialog(
            onDismissRequest = { showForgetCertificateDialog = false },
            title = { Text("Forget Hue certificate?") },
            text = {
                Text(
                    "Only continue after verifying the bridge was replaced or its certificate changed. " +
                        "The next successful encrypted connection will trust and save a new certificate."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgetCertificateDialog = false
                        viewModel.clearHueCertificatePin()
                    }
                ) {
                    Text("Forget certificate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetCertificateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * v1.13.2 (roadmap X1): Play builds request only Health Connect READ_SLEEP and
 * read recent sleep-session summaries for foreground Bedtime/Stats surfaces.
 * F-Droid keeps the preference for backup compatibility without shipping the
 * SDK or permission request path.
 */
@Composable
private fun HealthConnectSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onRequestPermissions: (() -> Unit)?
) {
    val isPlayFlavor = com.sysadmindoc.alarmclock.BuildConfig.FLAVOR == "play"
    val summary = state.healthConnectSleepSummary
    AppSurfaceCard {
        AppSectionTitle(
            title = "Health Connect",
            description = if (isPlayFlavor) {
                healthConnectDescription(state.settings.healthConnectEnabled, summary)
            } else {
                "The F-Droid flavor does not ship the Health Connect SDK. This setting is retained for backup compatibility only."
            }
        )
        SettingsToggle(
            label = "Read recent sleep sessions",
            checked = state.settings.healthConnectEnabled,
            supportingText = if (isPlayFlavor) {
                "Uses only android.permission.health.READ_SLEEP. Summaries stay local and are refreshed when Bedtime, Stats, or Settings is open."
            } else {
                "No Health Connect permissions are requested on F-Droid."
            },
            onToggle = { enabled ->
                if (enabled && isPlayFlavor && !summary.permissionGranted && onRequestPermissions != null) {
                    onRequestPermissions()
                } else {
                    viewModel.updateHealthConnectEnabled(enabled)
                }
            }
        )
        if (isPlayFlavor) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppStatusChip(
                    label = when (summary.availability) {
                        HealthConnectAvailability.AVAILABLE -> "SDK available"
                        HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> "Update needed"
                        HealthConnectAvailability.UNAVAILABLE -> "Unavailable"
                        HealthConnectAvailability.NOT_INCLUDED -> "Not included"
                    },
                    icon = if (summary.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (summary.isAvailable) DismissGreen else SnoozeYellow
                )
                AppStatusChip(
                    label = if (summary.permissionGranted) "READ_SLEEP granted" else "Permission needed",
                    icon = if (summary.permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (summary.permissionGranted) DismissGreen else SnoozeYellow
                )
            }
            if (summary.permissionGranted) {
                Text(
                    text = if (summary.hasRecentSession) {
                        "Last Health Connect session: ${formatSleepMinutes(summary.lastSessionDurationMinutes)} · ${summary.sessionsRead} read in the last 14 days."
                    } else {
                        "READ_SLEEP is granted, but no recent sleep sessions were returned in the last 14 days."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            summary.errorMessage?.let { error ->
                AppInlineNotice(
                    title = "Health Connect needs attention",
                    message = error,
                    icon = Icons.Default.Warning,
                    color = SnoozeYellow
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onRequestPermissions?.invoke() },
                    enabled = onRequestPermissions != null && summary.isAvailable,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (summary.permissionGranted) "Review access" else "Grant access")
                }
                OutlinedButton(
                    onClick = viewModel::refreshHealthConnectSleep,
                    enabled = summary.isAvailable,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Bedtime, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Refresh")
                }
            }
        }
    }
}

private fun healthConnectDescription(
    enabled: Boolean,
    summary: HealthConnectSleepSummary
): String = when {
    summary.availability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
        "Health Connect is installed but needs an update before sleep sessions can be read."
    summary.availability == HealthConnectAvailability.UNAVAILABLE ->
        "Health Connect is not available on this device."
    !enabled ->
        "Opt in to read recent Health Connect sleep sessions on-device for Bedtime and Stats context."
    !summary.permissionGranted ->
        "Grant READ_SLEEP in Health Connect before AlarmClockXtreme can show recent sleep summaries."
    summary.hasRecentSession ->
        "Recent sleep summaries are available locally from Health Connect."
    else ->
        "READ_SLEEP is granted; no recent Health Connect sleep sessions were found yet."
}

private fun formatSleepMinutes(minutes: Long?): String {
    val value = minutes ?: return "No session"
    val hours = value / 60
    val mins = value % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

/**
 * v1.2.0 personalization controls. Until this audit pass these settings
 * (`accentColor`, `showMotivationalQuotes`, `adaptiveDifficultyEnabled`,
 * `customTypingPhrases`) lived in DataStore + the backup payload but had no
 * UI surface — users couldn't change them.
 */
@Composable
private fun ConnectionsSection(state: SettingsUiState) {
    data class ConnectionInfo(
        val name: String,
        val enabled: Boolean,
        val domain: String,
        val dataSent: String,
        val offlineFallback: String
    )

    val connections = buildList {
        add(ConnectionInfo(
            name = "Weather",
            enabled = state.settings.showWeatherOnDashboard,
            domain = "api.open-meteo.com",
            dataSent = "Latitude, longitude",
            offlineFallback = "Last cached forecast"
        ))
        add(ConnectionInfo(
            name = "Air quality",
            enabled = state.settings.showWeatherOnDashboard,
            domain = "air-quality-api.open-meteo.com",
            dataSent = "Latitude, longitude",
            offlineFallback = "Hidden when unavailable"
        ))
        add(ConnectionInfo(
            name = "NWS weather alerts",
            enabled = state.settings.showWeatherOnDashboard,
            domain = "api.weather.gov",
            dataSent = "Latitude, longitude (US only)",
            offlineFallback = "No alerts shown"
        ))
        add(ConnectionInfo(
            name = "Public holidays",
            enabled = state.settings.holidayAutoSkipEnabled,
            domain = "date.nager.at",
            dataSent = "Country code",
            offlineFallback = "Cached holidays; skip disabled"
        ))
        add(ConnectionInfo(
            name = "Live radar",
            enabled = state.settings.showRadarEmbed,
            domain = "embed.windy.com",
            dataSent = "Latitude, longitude (via embed URL)",
            offlineFallback = "Radar card hidden"
        ))
        add(ConnectionInfo(
            name = "News feed",
            enabled = state.settings.showNewsTab,
            domain = state.settings.newsFeedUrl
                .removePrefix("https://").removePrefix("http://")
                .substringBefore("/").ifBlank { "user-configured" },
            dataSent = "Feed URL fetch only",
            offlineFallback = "Empty feed"
        ))
        if (state.settings.webhookEnabled) {
            add(ConnectionInfo(
                name = "Webhook",
                enabled = true,
                domain = state.settings.webhookUrl
                    .removePrefix("https://").removePrefix("http://")
                    .substringBefore("/").ifBlank { "not configured" },
                dataSent = "Alarm event, time, optional label",
                offlineFallback = "Events silently dropped"
            ))
        }
        if (state.settings.hueBridgeIp.isNotBlank()) {
            add(ConnectionInfo(
                name = "Philips Hue",
                enabled = true,
                domain = "${state.settings.hueBridgeIp} (LAN)",
                dataSent = "Light on/brightness/color commands",
                offlineFallback = "Sunrise simulation skipped"
            ))
        }
        add(ConnectionInfo(
            name = "Health Connect",
            enabled = state.settings.healthConnectEnabled,
            domain = "On-device (no network)",
            dataSent = "None — reads local sleep sessions",
            offlineFallback = "Always local"
        ))
    }

    SettingsGroup(
        title = "Connections and data",
        description = "Optional network services and what they send. Nothing leaves the device unless you enable it."
    ) {
        connections.forEach { conn ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (conn.enabled) SurfaceLight.copy(alpha = 0.58f)
                    else SurfaceLight.copy(alpha = 0.28f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conn.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (conn.enabled) TextPrimary else TextMuted
                        )
                        AppStatusChip(
                            label = if (conn.enabled) "Active" else "Off",
                            color = if (conn.enabled) DismissGreen else TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conn.domain,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sends: ${conn.dataSent}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Offline: ${conn.offlineFallback}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizationSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = "Personalization",
        description = "Pick the accent color, enable adaptive difficulty, and tune the wake-up screen."
    ) {
        AccentColorPicker(
            currentHex = state.settings.accentColor,
            onPick = viewModel::updateAccentColor
        )

        SettingsToggle(
            label = "Motivational quotes on alarm",
            checked = state.settings.showMotivationalQuotes,
            supportingText = "Shows a short quote on the firing screen alongside the time.",
            onToggle = viewModel::toggleShowMotivationalQuotes
        )
        SettingsToggle(
            label = "Adaptive challenge difficulty",
            checked = state.settings.adaptiveDifficultyEnabled,
            supportingText = "Auto-bumps math challenges (Easy → Medium → Hard) when your snooze rate climbs above 50%.",
            onToggle = viewModel::toggleAdaptiveDifficulty
        )

        // v1.4.0: Material You — respects the user's wallpaper palette on Android 12+.
        // On older devices the toggle is still persisted but has no visual effect,
        // so the help copy names the requirement rather than silently no-op'ing.
        SettingsToggle(
            label = "Material You dynamic color",
            checked = state.settings.dynamicColorEnabled,
            supportingText = "Blends the app accent with your wallpaper palette (Android 12+).",
            onToggle = viewModel::toggleDynamicColor
        )

        SettingsToggle(
            label = "Expressive surfaces",
            checked = state.settings.expressiveModeEnabled,
            supportingText = "Adds bolder shape rhythm and clearer accent semantics across shared app surfaces.",
            onToggle = viewModel::toggleExpressiveMode
        )

        SettingsToggle(
            label = "Cover-to-snooze",
            checked = state.settings.coverToSnoozeEnabled,
            supportingText = "Hold a hand over the proximity sensor for ~1.5 s during an alarm to snooze.",
            onToggle = viewModel::toggleCoverToSnooze
        )

        SettingsToggle(
            label = "Repeat missed alarms",
            checked = state.settings.repeatMissedAlarms,
            supportingText = "If an alarm auto-silences, re-fire it briefly when you unlock or unplug within 10 minutes.",
            onToggle = viewModel::toggleRepeatMissed
        )

        var showLockMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cancellation lock", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Prevent disabling alarms close to fire time",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showLockMenu = true }) {
                    Text(
                        if (state.settings.cancellationLockMinutes == 0) "Disabled" else "${state.settings.cancellationLockMinutes} min",
                        color = AccentBlue
                    )
                }
                DropdownMenu(expanded = showLockMenu, onDismissRequest = { showLockMenu = false }) {
                    listOf(0, 15, 30, 60).forEach { mins ->
                        DropdownMenuItem(
                            text = { Text(if (mins == 0) "Disabled" else "Lock $mins min before fire") },
                            onClick = { viewModel.updateCancellationLockMinutes(mins); showLockMenu = false }
                        )
                    }
                }
            }
        }

        var showFiringModeMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Firing controls", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "How you dismiss and snooze alarms. Buttons mode is recommended for screen readers.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showFiringModeMenu = true }) {
                    Text(
                        when (state.settings.firingControlMode) {
                            "buttons" -> "Buttons"
                            "swipe" -> "Swipe"
                            else -> "Hybrid"
                        },
                        color = AccentBlue
                    )
                }
                DropdownMenu(expanded = showFiringModeMenu, onDismissRequest = { showFiringModeMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Hybrid — swipe gestures and buttons") },
                        onClick = { viewModel.updateFiringControlMode("hybrid"); showFiringModeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Buttons only — accessible, no gestures") },
                        onClick = { viewModel.updateFiringControlMode("buttons"); showFiringModeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Swipe only — gesture-driven") },
                        onClick = { viewModel.updateFiringControlMode("swipe"); showFiringModeMenu = false }
                    )
                }
            }
        }

        BufferedSettingsTextField(
            value = state.settings.customTypingPhrases,
            onCommit = viewModel::updateCustomTypingPhrases,
            label = { Text("Custom typing phrases", color = TextMuted) },
            placeholder = { Text("One phrase per line — appended to the built-in list", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6
        )
        Text(
            text = "These are added after the built-in phrase library, so you can make typing challenges sound more like you.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        SettingsToggle(
            label = "Challenge accessibility bypass",
            supportingText = "Allow dismissing after a timed delay without completing the challenge. Helps users who cannot perform physical challenges.",
            checked = state.settings.challengeBypassEnabled,
            onToggle = { viewModel.updateChallengeBypassEnabled(it) }
        )
        if (state.settings.challengeBypassEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bypass delay", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(10, 30, 60, 120).forEach { secs ->
                        val selected = state.settings.challengeBypassDelaySeconds == secs
                        AppFilterChip(
                            selected = selected,
                            onClick = { viewModel.updateChallengeBypassDelay(secs) },
                            label = "${secs}s"
                        )
                    }
                }
            }
        }

        SettingsToggle(
            label = "Quieter audio during challenges",
            supportingText = "Reduces only this alarm's player volume while you solve a dismiss challenge; calls still mute it completely.",
            checked = state.settings.challengeAudioDuckingEnabled,
            onToggle = viewModel::updateChallengeAudioDuckingEnabled
        )
        if (state.settings.challengeAudioDuckingEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Challenge volume: ${state.settings.challengeAudioDuckPercent}%",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 35, 50, 65).forEach { percent ->
                        AppFilterChip(
                            selected = state.settings.challengeAudioDuckPercent == percent,
                            onClick = { viewModel.updateChallengeAudioDuckPercent(percent) },
                            label = "$percent%"
                        )
                    }
                }
            }
        }

        // v1.4.0: Pre-sleep checklist items, shown on the Bedtime tab.
        BufferedSettingsTextField(
            value = state.settings.bedtimeChecklist,
            onCommit = viewModel::updateBedtimeChecklist,
            label = { Text("Bedtime wind-down checklist", color = TextMuted) },
            placeholder = { Text("One item per line (e.g. Dim lights, Phone on charger, Set alarm)", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6
        )
        Text(
            text = "This appears in Bedtime so your routine stays consistent even when the rest of the day feels busy.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AccentColorPicker(currentHex: String, onPick: (String) -> Unit) {
    // Six-swatch palette covers the most common requests (cool/warm/mono).
    // Listed in the order users tend to reach for them; the first one is
    // the historical default so users always have an obvious "reset" path.
    val palette = listOf(
        "#5B9EF4" to "Default blue",
        "#7C5CFF" to "Violet",
        "#FF6F8A" to "Coral",
        "#FFB347" to "Amber",
        "#5BD49A" to "Mint",
        "#E0E4EA" to "Mono"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Accent color",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Used for the primary alarm tint, dashboard chips, and switches.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            palette.forEach { (hex, label) ->
                val isSelected = hex.equals(currentHex, ignoreCase = true)
                val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }
                    .getOrDefault(androidx.compose.ui.graphics.Color.Gray)
                val swatchShape = RoundedCornerShape(8.dp)
                val selectedIconTint = if (hex in lightAccentSwatches) SurfaceDark else TextPrimary
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(swatchShape)
                        .background(color)
                        .clickable(
                            role = Role.RadioButton,
                            onClickLabel = "Use $label accent",
                            onClick = { onPick(hex) }
                        )
                        .semantics {
                            contentDescription = "$label accent color"
                            selected = isSelected
                            stateDescription = if (isSelected) "Selected" else "Not selected"
                        }
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = TextPrimary,
                                    shape = swatchShape
                                )
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = BorderSubtle,
                                    shape = swatchShape
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = selectedIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private val lightAccentSwatches = setOf("#FFB347", "#5BD49A", "#E0E4EA")

@Composable
private fun BackupRestoreSection(viewModel: SettingsViewModel) {
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var encryptedPassphrase by remember { mutableStateOf("") }
    var encryptedPassphraseConfirm by remember { mutableStateOf("") }
    var pendingExportWarning by remember { mutableStateOf<BackupExportWarning?>(null) }
    var pendingExportKind by remember { mutableStateOf<BackupExportKind?>(null) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var importEnabledAsDisabled by remember { mutableStateOf(false) }
    var importPreviewBusy by remember { mutableStateOf(false) }
    val passphraseMismatch = encryptedPassphraseConfirm.isNotEmpty() &&
        encryptedPassphraseConfirm != encryptedPassphrase
    val encryptedExportEnabled = encryptedPassphrase.isNotBlank() &&
        encryptedPassphrase == encryptedPassphraseConfirm
    val encryptedImportEnabled = encryptedPassphrase.isNotBlank()
    val operationBusy = backupBusy || importPreviewBusy

    fun requestBackupImport(uri: Uri, encrypted: Boolean) {
        scope.launch {
            importPreviewBusy = true
            val passphrase = if (encrypted) encryptedPassphrase else ""
            try {
                val result = if (encrypted) {
                    viewModel.inspectEncryptedBackupImport(uri, passphrase)
                } else {
                    viewModel.inspectBackupImport(uri)
                }
                result
                    .onSuccess { preview ->
                        importEnabledAsDisabled = false
                        pendingImport = PendingBackupImport(
                            uri = uri,
                            encrypted = encrypted,
                            passphrase = passphrase,
                            preview = preview
                        )
                    }
                    .onFailure { error ->
                        viewModel.showBackupResult(
                            backupFailureMessage(
                                if (encrypted) BackupStatusKind.EncryptedImportPreview else BackupStatusKind.ImportPreview,
                                error
                            )
                        )
                    }
            } catch (error: Exception) {
                viewModel.showBackupResult(
                    backupFailureMessage(
                        if (encrypted) BackupStatusKind.EncryptedImportPreview else BackupStatusKind.ImportPreview,
                        error
                    )
                )
            } finally {
                importPreviewBusy = false
            }
        }
    }

    fun confirmBackupImport(mode: BackupImportMode) {
        val pending = pendingImport ?: return
        val options = BackupImportOptions(
            mode = mode,
            importEnabledAsDisabled = importEnabledAsDisabled
        )
        pendingImport = null
        if (pending.encrypted) {
            viewModel.importEncryptedBackup(pending.uri, pending.passphrase, options)
        } else {
            viewModel.importBackup(pending.uri, options)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { requestBackupImport(it, encrypted = false) } }

    val encryptedExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportEncryptedBackup(it, encryptedPassphrase) } }

    val encryptedImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { requestBackupImport(it, encrypted = true) } }

    fun launchBackupExport(kind: BackupExportKind) {
        when (kind) {
            BackupExportKind.Plain -> exportLauncher.launch("alarmclock_backup.json")
            BackupExportKind.Encrypted -> encryptedExportLauncher.launch("alarmclock_backup_encrypted.json")
        }
    }

    fun requestBackupExport(kind: BackupExportKind) {
        scope.launch {
            val warning = runCatching {
                viewModel.inspectBackupExportWarning()
            }.getOrElse { error ->
                BackupExportWarning(
                    listOf("Backup contents could not be inspected: ${error.message ?: "unexpected error"}")
                )
            }
            if (warning.shouldWarn) {
                pendingExportKind = kind
                pendingExportWarning = warning
            } else {
                launchBackupExport(kind)
            }
        }
    }

    SettingsGroup(
        title = "Backup and restore",
        description = "Keep a portable copy of alarms and app preferences for new devices or peace of mind."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { requestBackupExport(BackupExportKind.Plain) },
                enabled = !operationBusy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                enabled = !operationBusy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Import")
            }
        }

        Text(
            text = "Plain backups include alarms and global settings in a readable JSON file. AlarmClockXtreme warns before exporting configured secrets or private references.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Encrypted backup",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Use a passphrase to create an AES-256 encrypted backup. Keep the passphrase somewhere safe; it cannot be recovered by the app.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = encryptedPassphrase,
                onValueChange = { encryptedPassphrase = it },
                label = { Text("Passphrase") },
                placeholder = { Text("Required for encrypted files") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape
            )
            OutlinedTextField(
                value = encryptedPassphraseConfirm,
                onValueChange = { encryptedPassphraseConfirm = it },
                label = { Text("Confirm passphrase") },
                placeholder = { Text("Required before encrypted export") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = passphraseMismatch,
                supportingText = if (passphraseMismatch) {
                    {
                        Text(
                            "Passphrases do not match. Encrypted import uses only the first field.",
                            color = AccentRed
                        )
                    }
                } else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { requestBackupExport(BackupExportKind.Encrypted) },
                    enabled = encryptedExportEnabled && !operationBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Encrypt export")
                }
                OutlinedButton(
                    onClick = { encryptedImportLauncher.launch(arrayOf("application/json", "*/*")) },
                    enabled = encryptedImportEnabled && !operationBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Decrypt import")
                }
            }
        }
    }

    pendingExportWarning?.let { warning ->
        val kind = pendingExportKind ?: BackupExportKind.Plain
        BackupExportWarningDialog(
            warning = warning,
            encrypted = kind == BackupExportKind.Encrypted,
            onDismiss = {
                pendingExportWarning = null
                pendingExportKind = null
            },
            onContinue = {
                pendingExportWarning = null
                pendingExportKind = null
                launchBackupExport(kind)
            }
        )
    }

    pendingImport?.let { import ->
        BackupImportPreviewDialog(
            pendingImport = import,
            importEnabledAsDisabled = importEnabledAsDisabled,
            onImportEnabledAsDisabledChange = { importEnabledAsDisabled = it },
            onDismiss = { pendingImport = null },
            onImport = ::confirmBackupImport
        )
    }

    if (operationBusy) {
        AppSurfaceCard(highlighted = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (importPreviewBusy) "Inspecting backup" else "Backup operation in progress",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (importPreviewBusy) {
                            "Restore choices appear after the file is checked."
                        } else {
                            "Backup buttons stay locked until the result appears."
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    backupResult?.let { message ->
        val failed = isFailureStatusMessage(message)
        AppFeedbackCard(
            title = if (failed) "Backup needs attention" else "Backup complete",
            message = message,
            icon = if (failed) Icons.Default.Warning else Icons.Default.Backup,
            color = if (failed) AccentRed else DismissGreen,
            onDismiss = viewModel::clearBackupResult
        )
    }
}

private enum class BackupExportKind {
    Plain,
    Encrypted
}

private data class PendingBackupImport(
    val uri: Uri,
    val encrypted: Boolean,
    val passphrase: String,
    val preview: BackupImportPreview
)

@Composable
private fun BackupImportPreviewDialog(
    pendingImport: PendingBackupImport,
    importEnabledAsDisabled: Boolean,
    onImportEnabledAsDisabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onImport: (BackupImportMode) -> Unit
) {
    val preview = pendingImport.preview
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (preview.canImport) Icons.Default.Backup else Icons.Default.Warning,
                contentDescription = null,
                tint = if (preview.canImport) MaterialTheme.colorScheme.primary else AccentRed
            )
        },
        title = { Text("Review backup before restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nothing changes until you choose how to restore this backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = preview.compatibilityStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview.canImport) TextPrimary else AccentRed
                )
                Text(
                    text = "Backup v${preview.version} from app ${preview.appVersion.ifBlank { "unknown" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatBackupExportedAt(preview.exportedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${preview.alarmCount} alarms, ${preview.enabledAlarmCount} enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (preview.invalidAlarmCount > 0) {
                    Text(
                        text = "${preview.invalidAlarmCount} alarm rows could not be read and will be skipped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnoozeYellow
                    )
                }
                Text(
                    text = if (preview.settingsIncluded) {
                        "Global settings will be restored."
                    } else {
                        "This backup does not include global settings."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (preview.privateDataCategories.isNotEmpty()) {
                    Text(
                        text = "Private values detected:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    preview.privateDataCategories.forEach { category ->
                        Text(
                            text = "- $category",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                if (preview.canImport) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = importEnabledAsDisabled,
                                role = Role.Switch,
                                onValueChange = onImportEnabledAsDisabledChange
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = importEnabledAsDisabled,
                            onCheckedChange = null,
                            colors = appSwitchColors()
                        )
                        Text(
                            text = "Keep restored alarms disabled until I review them",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (preview.canImport) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onImport(BackupImportMode.Append) }) {
                        Text("Append alarms")
                    }
                    TextButton(onClick = { onImport(BackupImportMode.Replace) }) {
                        Text("Replace alarms")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (preview.canImport) "Cancel" else "Close")
            }
        }
    )
}

private fun formatBackupExportedAt(exportedAt: Long): String {
    if (exportedAt <= 0L) return "Export time unknown"
    return runCatching {
        "Exported " + DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(exportedAt))
    }.getOrElse {
        "Export time unknown"
    }
}

@Composable
private fun BackupExportWarningDialog(
    warning: BackupExportWarning,
    encrypted: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SnoozeYellow)
        },
        title = {
            Text(
                text = if (encrypted) "Encrypted backup includes private values" else "Plain backup includes private values"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (encrypted) {
                        "These values will be inside the encrypted file:"
                    } else {
                        "These values will be readable in the exported JSON:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                warning.categories.forEach { category ->
                    Text(
                        text = "- $category",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (encrypted) {
                        "Keep the passphrase private. Anyone with the file and passphrase can restore these values."
                    } else {
                        "Use encrypted export when sharing or storing backups outside your own device."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(if (encrypted) "Export encrypted backup" else "Export plain backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UtilityShortcutCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceLight.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
        }
    }
}

private fun dashboardSummary(state: SettingsUiState): String {
    val base = when {
        state.settings.showWeatherOnDashboard && state.settings.showCalendarOnDashboard -> "Weather + calendar"
        state.settings.showWeatherOnDashboard -> "Weather only"
        state.settings.showCalendarOnDashboard -> "Calendar only"
        else -> "Minimal"
    }
    return if (state.settings.calendarAutoAlarmEnabled) "$base + auto-alarm" else base
}

private fun incidentLabel(type: String?): String {
    val token = type.orEmpty().ifBlank { "UNKNOWN" }
    return token
        .replace('_', ' ')
        .lowercase(Locale.US)
        .replaceFirstChar { it.titlecase(Locale.US) }
}

private fun formatIncidentTimestamp(eventAt: Long?, use24Hour: Boolean): String {
    if (eventAt == null || eventAt <= 0L) return "Time unknown"
    val pattern = if (use24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
    return DateTimeFormatter.ofPattern(pattern, Locale.US)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(eventAt))
}

private fun formatIncidentElapsed(elapsedMs: Long?): String {
    if (elapsedMs == null) return "No schedule delta"
    val absoluteSeconds = kotlin.math.abs(elapsedMs) / 1000L
    if (absoluteSeconds < 60L) return "within 1 min of schedule"
    val minutes = absoluteSeconds / 60L
    val label = if (minutes == 1L) "1 min" else "$minutes min"
    return if (elapsedMs < 0L) "$label before schedule" else "$label after schedule"
}

private fun wakeReadinessSummary(state: SettingsUiState): String {
    val missing = buildList {
        if (!state.canScheduleExactAlarms) add("exact alarms")
        if (!state.hasNotificationPermission) add("notifications")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            state.canUseFullScreenIntent != true
        ) {
            add("full-screen alarm access")
        }
        if (requiresLocalNetworkAccess(state) && !state.hasLocalNetworkPermission) {
            add("local network access")
        }
        if (!state.isIgnoringBatteryOptimizations) add("battery")
        // v1.11.3 (roadmap N3): include standby-bucket throttling in the
        // top-tile summary so the user sees it without expanding the section.
        if (state.appStandbyBucket != AppStandbyBucket.UNKNOWN &&
            AppStandbyBucket.isDegraded(state.appStandbyBucket)
        ) {
            add("standby bucket")
        }
    }
    return if (missing.isEmpty()) {
        val optionalChecks = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add("full-screen access")
            }
            if (requiresLocalNetworkAccess(state)) {
                add("LAN access")
            }
            add("battery")
            add("standby")
        }.joinToString(", ")
        "Exact alarms, alerts, $optionalChecks are ready"
    } else {
        "Review ${missing.joinToString(", ")}"
    }
}

private fun requiresLocalNetworkAccess(state: SettingsUiState): Boolean {
    if (!LocalNetworkPermission.isRuntimeRequired()) return false
    return state.settings.hueBridgeIp.isNotBlank() ||
        LocalNetworkPermission.isLikelyLocalEndpoint(state.settings.webhookUrl)
}

private fun formatWebhookDeliveryStatus(settings: AppSettings): String? {
    val status = settings.webhookLastDeliveryStatus.takeIf { it.isNotBlank() } ?: return null
    val timestamp = settings.webhookLastDeliveryAtMillis.takeIf { it > 0 }
        ?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US))
        } ?: "recently"
    return "Last delivery $timestamp: $status"
}

@Composable
private fun BufferedSettingsTextField(
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    transformInput: (String) -> String = { it },
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    commitDelayMillis: Long = if (singleLine) 220 else 350
) {
    val focusManager = LocalFocusManager.current
    var draft by rememberSaveable { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val effectiveKeyboardOptions = if (singleLine && keyboardOptions.imeAction == ImeAction.Default) {
        keyboardOptions.copy(imeAction = ImeAction.Done)
    } else {
        keyboardOptions
    }

    LaunchedEffect(value, isFocused) {
        if (!isFocused && draft != value) {
            draft = value
        }
    }

    LaunchedEffect(draft, value, commitDelayMillis) {
        if (draft != value) {
            delay(commitDelayMillis)
            if (draft != value) {
                onCommit(draft)
            }
        }
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = transformInput(it) },
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        colors = appOutlinedTextFieldColors(),
        shape = AppInputShape,
        modifier = modifier.onFocusChanged { focusState ->
            val lostFocus = isFocused && !focusState.isFocused
            isFocused = focusState.isFocused
            if (lostFocus && draft != value) {
                onCommit(draft)
            }
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = effectiveKeyboardOptions,
        keyboardActions = KeyboardActions(
            onDone = {
                if (draft != value) {
                    onCommit(draft)
                }
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = SurfaceCard.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.dp, TextMuted.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .clickable(
                onClickLabel = "Change ${label.lowercase()} date",
                role = Role.Button,
                onClick = onClick
            )
            // Merge the label + value into one actionable announcement so TalkBack
            // reads "Starts: Jun 14, 2026, button" instead of two separate nodes.
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    if (totalSeconds == 0) return "Off"
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return when {
        m == 0  -> "${s}s"
        s == 0  -> "${m}m"
        else    -> "${m}m ${s}s"
    }
}
