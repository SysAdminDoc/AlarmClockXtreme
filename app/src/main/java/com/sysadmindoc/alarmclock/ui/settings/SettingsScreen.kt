package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.sysadmindoc.alarmclock.R
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
import com.sysadmindoc.alarmclock.data.backup.FossifyImportErrorKind
import com.sysadmindoc.alarmclock.data.backup.FossifyImportException
import com.sysadmindoc.alarmclock.data.backup.FossifyImportPreview
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SettingsPaneCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
)

private val settingsPaneCategories = listOf(
    SettingsPaneCategory(
        id = "readiness",
        titleRes = R.string.settings_pane_readiness,
        descriptionRes = R.string.settings_pane_readiness_description,
        icon = Icons.Default.Security
    ),
    SettingsPaneCategory(
        id = "defaults",
        titleRes = R.string.settings_pane_defaults,
        descriptionRes = R.string.settings_pane_defaults_description,
        icon = Icons.Default.Alarm
    ),
    SettingsPaneCategory(
        id = "integrations",
        titleRes = R.string.settings_pane_integrations,
        descriptionRes = R.string.settings_pane_integrations_description,
        icon = Icons.Default.Link
    ),
    SettingsPaneCategory(
        id = "personalization",
        titleRes = R.string.settings_pane_personalization,
        descriptionRes = R.string.settings_pane_personalization_description,
        icon = Icons.Default.AutoAwesome
    ),
    SettingsPaneCategory(
        id = "backup",
        titleRes = R.string.settings_pane_backup,
        descriptionRes = R.string.settings_pane_backup_description,
        icon = Icons.Default.Backup
    ),
    SettingsPaneCategory(
        id = "utilities",
        titleRes = R.string.settings_pane_utilities,
        descriptionRes = R.string.settings_pane_utilities_description,
        icon = Icons.Default.Speed
    )
)

private fun LazyListScope.settingsItem(
    key: String,
    content: @Composable () -> Unit
) {
    item(key = key) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

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
    var showClearCommuteHistoryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val screenScope = rememberCoroutineScope()
    val supportBundleSubject = stringResource(R.string.settings_support_bundle_subject)
    val shareSupportBundleTitle = stringResource(R.string.settings_share_support_bundle)
    val shareUnavailableMessage = stringResource(R.string.settings_share_unavailable)
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
            putExtra(Intent.EXTRA_SUBJECT, supportBundleSubject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, shareSupportBundleTitle))
        } catch (_: Exception) {
            viewModel.setSupportExportShareFailed()
            Toast.makeText(context, shareUnavailableMessage, Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        val useTwoPane = shouldUseTwoPaneLayout(maxWidth.value)
        var selectedPaneId by rememberSaveable { mutableStateOf<String?>(null) }
        val selectedPane = settingsPaneCategories.firstOrNull { it.id == selectedPaneId }
            ?: settingsPaneCategories.first()
        val showSettingsHome = !useTwoPane && selectedPaneId == null
        val settingsListState = rememberLazyListState()

        androidx.compose.runtime.LaunchedEffect(useTwoPane, selectedPane.id) {
            if (useTwoPane) settingsListState.scrollToItem(0)
        }

        val settingsContent: @Composable (Modifier) -> Unit = { contentModifier ->
            LazyColumn(
                modifier = contentModifier,
                state = settingsListState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (useTwoPane) {
                    item(key = "pane-header-${selectedPane.id}") {
                        SettingsPaneHeader(selectedPane, state)
                    }
                } else if (showSettingsHome) {
                    item(key = "settings-hero") {
                        AlarmClockHeroHeader(
                            title = stringResource(R.string.settings_title),
                            subtitle = state.appVersion
                        )
                    }
                    settingsItem("settings-home-readiness") {
                        WakeReadinessSection(
                            state = state,
                            onOpenOnboardingChecklist = onOpenOnboardingChecklist
                        )
                    }
                    settingsItem("settings-home-categories") {
                        SettingsCategoryHome(
                            categories = settingsPaneCategories.filterNot { it.id == "readiness" },
                            onSelect = { selectedPaneId = it }
                        )
                    }
                } else {
                    item(key = "settings-pane-header-${selectedPane.id}") {
                        AlarmClockHeroHeader(
                            title = stringResource(selectedPane.titleRes),
                            subtitle = "",
                            actions = {
                                TextButton(onClick = { selectedPaneId = null }) {
                                    Text(stringResource(R.string.settings_title))
                                }
                            }
                        )
                    }
                }
            if (!showSettingsHome && selectedPane.id == "readiness") {
            settingsItem("readiness-wake") {
            WakeReadinessSection(
                state = state,
                onOpenOnboardingChecklist = onOpenOnboardingChecklist
            )
            }
            settingsItem("readiness-incidents") {
            IncidentTimelineSection(
                timeline = state.incidentTimeline,
                use24Hour = state.settings.is24HourFormat,
                onClearIncidentHistory = viewModel::clearIncidentHistory
            )
            }
            settingsItem("readiness-permissions") {
                PermissionRequestCard(includeNotifications = false)
            }
            settingsItem("readiness-overview") {
                SettingsOverviewRow(state)
            }

            if (state.needsBatteryGuidance || !state.isIgnoringBatteryOptimizations) {
                settingsItem("readiness-battery") {
                    BatteryOptimizationSection(state, viewModel)
                }
            }

            settingsItem("readiness-pause") {
                PauseAlarmsSection(state, viewModel)
            }

            settingsItem("readiness-vacation") {
                VacationModeSection(state, viewModel)
            }
            }

            if (!showSettingsHome && selectedPane.id == "defaults") {
            settingsItem("defaults-alarm") {
            SettingsGroup(
                title = stringResource(R.string.settings_alarm_defaults),
                description = stringResource(R.string.settings_alarm_defaults_description)
            ) {
                SettingsToggle(
                    label = stringResource(R.string.format_24h),
                    checked = state.settings.is24HourFormat,
                    supportingText = stringResource(R.string.settings_24h_description),
                    onToggle = viewModel::toggle24Hour
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_show_lock_screen),
                    checked = state.settings.showOnLockScreen,
                    supportingText = stringResource(R.string.settings_show_lock_screen_description),
                    onToggle = viewModel::toggleLockScreen
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_hide_public_labels),
                    checked = state.settings.hideAlarmLabelsOnPublicSurfaces,
                    supportingText = stringResource(R.string.settings_hide_public_labels_description),
                    onToggle = viewModel::toggleHideAlarmLabelsOnPublicSurfaces
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_phone_speakers),
                    checked = state.settings.usePhoneSpeakers,
                    supportingText = stringResource(R.string.settings_phone_speakers_description),
                    onToggle = viewModel::togglePhoneSpeakers
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_flip_snooze),
                    checked = state.settings.flipToSnoozeEnabled,
                    supportingText = stringResource(R.string.settings_flip_snooze_description),
                    onToggle = viewModel::toggleFlipToSnooze
                )

                SettingsActionRow(
                    label = stringResource(R.string.settings_default_snooze),
                    value = stringResource(R.string.settings_minutes_short, state.settings.defaultSnoozeDuration),
                    supportingText = stringResource(R.string.settings_default_snooze_description),
                    onClick = { showDefaultSnoozeMenu = true }
                )
                DropdownMenu(
                    expanded = showDefaultSnoozeMenu,
                    onDismissRequest = { showDefaultSnoozeMenu = false }
                ) {
                    listOf(1, 3, 5, 10, 15, 20, 30).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(pluralStringResource(R.plurals.settings_minutes, minutes, minutes)) },
                            onClick = {
                                viewModel.updateDefaultSnooze(minutes)
                                showDefaultSnoozeMenu = false
                            }
                        )
                    }
                }

                SettingsActionRow(
                    label = stringResource(R.string.settings_default_volume_ramp),
                    value = formatSeconds(state.settings.defaultGradualVolume),
                    supportingText = stringResource(R.string.settings_default_volume_ramp_description),
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
                    label = stringResource(R.string.auto_silence),
                    value = if (state.settings.autoSilenceMinutes == 0) {
                        stringResource(R.string.settings_never)
                    } else {
                        stringResource(R.string.settings_minutes_short, state.settings.autoSilenceMinutes)
                    },
                    supportingText = stringResource(R.string.settings_auto_silence_description),
                    onClick = { showAutoSilenceMenu = true }
                )
                DropdownMenu(
                    expanded = showAutoSilenceMenu,
                    onDismissRequest = { showAutoSilenceMenu = false }
                ) {
                    listOf(0, 5, 10, 15, 30).forEach { minutes ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (minutes == 0) stringResource(R.string.settings_never)
                                    else pluralStringResource(R.plurals.settings_minutes, minutes, minutes)
                                )
                            },
                            onClick = {
                                viewModel.updateAutoSilence(minutes)
                                showAutoSilenceMenu = false
                            }
                        )
                    }
                }
            }
            }

            settingsItem("defaults-dashboard") {
            SettingsGroup(
                title = stringResource(R.string.settings_dashboard),
                description = stringResource(R.string.settings_dashboard_description)
            ) {
                SettingsToggle(
                    label = stringResource(R.string.show_weather),
                    checked = state.settings.showWeatherOnDashboard,
                    supportingText = stringResource(R.string.settings_show_weather_description),
                    onToggle = viewModel::toggleShowWeather
                )
                SettingsToggle(
                    label = stringResource(R.string.show_calendar),
                    checked = state.settings.showCalendarOnDashboard,
                    supportingText = stringResource(R.string.settings_show_calendar_description),
                    onToggle = viewModel::toggleShowCalendar
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_post_dismiss_summary),
                    checked = state.settings.postDismissSummaryEnabled,
                    supportingText = stringResource(R.string.settings_post_dismiss_summary_description),
                    onToggle = viewModel::togglePostDismissSummary
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_first_meeting_alarm),
                    checked = state.settings.calendarAutoAlarmEnabled,
                    supportingText = if (state.settings.calendarAutoAlarmEnabled) {
                        stringResource(R.string.settings_first_meeting_enabled_description)
                    } else {
                        stringResource(R.string.settings_first_meeting_disabled_description)
                    },
                    onToggle = viewModel::toggleCalendarAutoAlarm
                )
                SettingsActionRow(
                    label = stringResource(R.string.settings_meeting_lead_time),
                    value = stringResource(R.string.settings_minutes_short, state.settings.calendarAutoAlarmMinutesBefore),
                    supportingText = stringResource(R.string.settings_meeting_lead_description),
                    onClick = { showCalendarLeadMenu = true }
                )
                DropdownMenu(
                    expanded = showCalendarLeadMenu,
                    onDismissRequest = { showCalendarLeadMenu = false }
                ) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(pluralStringResource(R.plurals.settings_minutes, minutes, minutes)) },
                            onClick = {
                                viewModel.updateCalendarAutoAlarmMinutes(minutes)
                                showCalendarLeadMenu = false
                            }
                        )
                    }
                }
                SettingsToggle(
                    label = stringResource(R.string.settings_commute_aware),
                    checked = state.settings.calendarCommuteAwareEnabled,
                    supportingText = stringResource(R.string.settings_commute_aware_description),
                    enabled = state.settings.calendarAutoAlarmEnabled,
                    onToggle = viewModel::toggleCalendarCommuteAware
                )
                SettingsActionRow(
                    label = stringResource(R.string.settings_normal_commute),
                    value = if (state.settings.calendarCommuteBaselineMinutes == 0) {
                        stringResource(R.string.settings_use_lead_time)
                    } else {
                        stringResource(R.string.settings_minutes_short, state.settings.calendarCommuteBaselineMinutes)
                    },
                    supportingText = stringResource(R.string.settings_normal_commute_description),
                    onClick = { showCommuteBaselineMenu = true },
                    enabled = state.settings.calendarCommuteAwareEnabled
                )
                DropdownMenu(
                    expanded = showCommuteBaselineMenu,
                    onDismissRequest = { showCommuteBaselineMenu = false }
                ) {
                    listOf(0, 15, 30, 45, 60, 90, 120).forEach { minutes ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (minutes == 0) stringResource(R.string.settings_use_meeting_lead)
                                    else pluralStringResource(R.plurals.settings_minutes, minutes, minutes)
                                )
                            },
                            onClick = {
                                viewModel.updateCalendarCommuteBaselineMinutes(minutes)
                                showCommuteBaselineMenu = false
                            }
                        )
                    }
                }
                SettingsActionRow(
                    label = stringResource(R.string.settings_weather_buffer),
                    value = stringResource(R.string.settings_minutes_short, state.settings.calendarCommuteWeatherExtraMinutes),
                    supportingText = stringResource(R.string.settings_weather_buffer_description),
                    onClick = { showCommuteWeatherMenu = true },
                    enabled = state.settings.calendarCommuteAwareEnabled
                )
                DropdownMenu(
                    expanded = showCommuteWeatherMenu,
                    onDismissRequest = { showCommuteWeatherMenu = false }
                ) {
                    listOf(0, 10, 15, 20, 30, 45, 60).forEach { minutes ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (minutes == 0) stringResource(R.string.settings_no_weather_buffer)
                                    else pluralStringResource(R.plurals.settings_minutes, minutes, minutes)
                                )
                            },
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
                    label = { Text(stringResource(R.string.settings_routes_api_key)) },
                    placeholder = { Text(stringResource(R.string.settings_routes_api_placeholder)) },
                    enabled = state.settings.calendarCommuteAwareEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (state.settings.calendarCommuteAwareEnabled && state.settings.googleRoutesApiKey.isBlank()) {
                    AppInlineNotice(
                        title = stringResource(R.string.settings_commute_fallback),
                        message = stringResource(R.string.settings_commute_fallback_description),
                        icon = Icons.Default.Cloud,
                        color = AccentBlue
                    )
                }
                SettingsActionRow(
                    label = stringResource(R.string.settings_commute_history),
                    value = stringResource(R.string.settings_clear),
                    supportingText = stringResource(R.string.settings_commute_history_description),
                    onClick = { showClearCommuteHistoryDialog = true },
                    enabled = state.settings.calendarCommuteAwareEnabled
                )
                SettingsActionRow(
                    label = stringResource(R.string.temperature_unit),
                    value = stringResource(
                        if (state.settings.temperatureUnit == "celsius") R.string.settings_celsius
                        else R.string.settings_fahrenheit
                    ),
                    supportingText = stringResource(R.string.settings_temperature_description),
                    onClick = { showTemperatureMenu = true }
                )
                DropdownMenu(
                    expanded = showTemperatureMenu,
                    onDismissRequest = { showTemperatureMenu = false }
                ) {
                    listOf(
                        "fahrenheit" to stringResource(R.string.settings_fahrenheit),
                        "celsius" to stringResource(R.string.settings_celsius)
                    ).forEach { (unit, label) ->
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

                if (showClearCommuteHistoryDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearCommuteHistoryDialog = false },
                        title = { Text(stringResource(R.string.settings_clear_commute_title)) },
                        text = { Text(stringResource(R.string.settings_clear_commute_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.clearLearnedCommuteHistory()
                                showClearCommuteHistoryDialog = false
                            }) { Text(stringResource(R.string.settings_clear_history)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearCommuteHistoryDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
            }

            settingsItem("defaults-navigation") {
            SettingsGroup(
                title = stringResource(R.string.settings_bottom_navigation),
                description = stringResource(R.string.settings_bottom_navigation_description)
            ) {
                SettingsToggle(
                    label = stringResource(R.string.settings_show_today_tab),
                    checked = state.settings.showDashboardTab,
                    supportingText = stringResource(R.string.settings_show_today_description),
                    onToggle = viewModel::toggleShowDashboardTab
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_show_timer_tab),
                    checked = state.settings.showTimerTab,
                    supportingText = stringResource(R.string.settings_show_timer_description),
                    onToggle = viewModel::toggleShowTimerTab
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_show_world_tab),
                    checked = state.settings.showWorldClockTab,
                    supportingText = stringResource(R.string.settings_show_world_description),
                    onToggle = viewModel::toggleShowWorldClockTab
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_show_news_tab),
                    checked = state.settings.showNewsTab,
                    supportingText = stringResource(R.string.settings_show_news_description),
                    onToggle = viewModel::toggleShowNewsTab
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_radar_tab),
                    checked = state.settings.showRadarEmbed,
                    supportingText = stringResource(R.string.settings_radar_description),
                    onToggle = viewModel::toggleShowRadarEmbed
                )
            }
            }
            }

            if (!showSettingsHome && selectedPane.id == "integrations") {
            settingsItem("integrations-services") {
                IntegrationsSection(state, viewModel)
            }
            settingsItem("integrations-holidays") {
                HolidaysSection(state, viewModel)
            }
            settingsItem("integrations-hue") {
                PhilipsHueSection(state, viewModel)
            }
            settingsItem("integrations-health") {
            HealthConnectSection(
                state = state,
                viewModel = viewModel,
                onRequestPermissions = requestHealthConnectPermissions
            )
            }
            settingsItem("integrations-connections") {
                ConnectionsSection(state)
            }
            }
            if (!showSettingsHome && selectedPane.id == "personalization") {
            settingsItem("personalization") {
                PersonalizationSection(state, viewModel)
            }
            }
            if (!showSettingsHome && selectedPane.id == "backup") {
            settingsItem("backup-restore") {
                BackupRestoreSection(viewModel, is24HourFormat = state.settings.is24HourFormat)
            }
            }

            if (!showSettingsHome && selectedPane.id == "utilities") {
            settingsItem("utilities-shortcuts") {
            SettingsGroup(
                title = stringResource(R.string.settings_utilities),
                description = stringResource(R.string.settings_utilities_description)
            ) {
                UtilityShortcutCard(
                    icon = Icons.Default.BarChart,
                    title = stringResource(R.string.settings_alarm_statistics),
                    description = stringResource(R.string.settings_alarm_statistics_description),
                    onClick = onNavigateToStats
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.Speed,
                    title = stringResource(R.string.nav_stopwatch),
                    description = stringResource(R.string.settings_stopwatch_description),
                    onClick = onNavigateToStopwatch
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.Bedtime,
                    title = stringResource(R.string.nav_bedtime),
                    description = stringResource(R.string.settings_bedtime_description),
                    onClick = onNavigateToBedtime
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))
                UtilityShortcutCard(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_night_clock),
                    description = stringResource(R.string.settings_night_clock_description),
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
                    title = stringResource(R.string.settings_export_support_bundle),
                    description = if (supportExportBusy) {
                        stringResource(R.string.settings_packaging_diagnostics)
                    } else {
                        stringResource(R.string.settings_support_bundle_description)
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
            }

            if (supportExportBusy) {
                settingsItem("utilities-support-progress") {
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
                            text = stringResource(R.string.settings_packaging_support_bundle),
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                }
            }

            supportExportResult?.let { message ->
                settingsItem("utilities-support-result") {
                val failed = isFailureStatusMessage(message)
                AppFeedbackCard(
                    title = stringResource(
                        if (failed) R.string.settings_support_export_failed else R.string.settings_support_bundle_ready
                    ),
                    message = message,
                    icon = if (failed) Icons.Default.Warning else Icons.Default.BugReport,
                    color = if (failed) AccentRed else DismissGreen,
                    onDismiss = viewModel::clearSupportExportResult
                )
                }
            }

            settingsItem("utilities-about") {
            SettingsGroup(
                title = stringResource(R.string.about),
                description = stringResource(R.string.settings_about_description)
            ) {
                SettingsInfo(stringResource(R.string.settings_version), state.appVersion)
                SettingsInfo(stringResource(R.string.settings_device), state.deviceModel)
                SettingsInfo(stringResource(R.string.settings_android), state.androidVersion)
                SettingsInfo(stringResource(R.string.settings_license), stringResource(R.string.settings_license_value))
                SettingsInfo(stringResource(R.string.settings_source), stringResource(R.string.settings_source_value))
            }
            }
            }

            item(key = "settings-bottom-spacer") {
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
                )
            }
        } else {
            settingsContent(
                Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun SettingsCategoryHome(
    categories: List<SettingsPaneCategory>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_preferences),
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        AppSurfaceCard(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 4.dp
            )
        ) {
            categories.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onSelect(category.id) }
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(category.titleRes),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (index < categories.lastIndex) {
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
                }
            }
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
            title = stringResource(category.titleRes),
            description = stringResource(category.descriptionRes),
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
                label = stringResource(
                    if (state.isIgnoringBatteryOptimizations) {
                        R.string.settings_battery_protected
                    } else {
                        R.string.settings_battery_setup_needed
                    }
                ),
                icon = if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                color = if (state.isIgnoringBatteryOptimizations) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = stringResource(category.titleRes),
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
    val categoriesDescription = stringResource(R.string.settings_categories_accessibility)
    val selectedDescription = stringResource(R.string.settings_selected)
    val notSelectedDescription = stringResource(R.string.settings_not_selected)
    AppSurfaceCard(
        modifier = modifier.semantics {
            contentDescription = categoriesDescription
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
                text = stringResource(R.string.settings_title),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_choose_group_hint),
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
                    label = stringResource(
                        if (state.isIgnoringBatteryOptimizations) {
                            R.string.settings_protected
                        } else {
                            R.string.settings_setup_needed
                        }
                    ),
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
                            stateDescription = if (selected) selectedDescription else notSelectedDescription
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
                                text = stringResource(category.titleRes),
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Text(
                                text = stringResource(category.descriptionRes),
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
internal fun SettingsGroup(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitle(title = title)
        AppSurfaceCard(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 4.dp,
                vertical = 4.dp
            )
        ) {
            content()
        }
    }
}

@Composable
internal fun SettingsToggle(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle
            ),
        shape = RoundedCornerShape(10.dp),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
internal fun SettingsActionRow(
    label: String,
    value: String,
    supportingText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            if (!supportingText.isNullOrBlank()) {
                Text(
                    supportingText,
                    color = if (enabled) TextSecondary else TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun SettingsInfo(label: String, description: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard.copy(alpha = 0.24f)
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

@Composable
internal fun dashboardSummary(state: SettingsUiState): String {
    val base = when {
        state.settings.showWeatherOnDashboard && state.settings.showCalendarOnDashboard ->
            stringResource(R.string.settings_dashboard_weather_calendar)
        state.settings.showWeatherOnDashboard -> stringResource(R.string.settings_dashboard_weather_only)
        state.settings.showCalendarOnDashboard -> stringResource(R.string.settings_dashboard_calendar_only)
        else -> stringResource(R.string.settings_dashboard_minimal)
    }
    return if (state.settings.calendarAutoAlarmEnabled) {
        stringResource(R.string.settings_dashboard_auto_alarm, base)
    } else {
        base
    }
}

@Composable
internal fun incidentLabel(type: String?): String {
    val unknown = stringResource(R.string.settings_unknown_code)
    val token = type.orEmpty().ifBlank { unknown }
    return token
        .replace('_', ' ')
        .lowercase(Locale.US)
        .replaceFirstChar { it.titlecase(Locale.US) }
}

@Composable
internal fun formatIncidentTimestamp(eventAt: Long?, use24Hour: Boolean): String {
    if (eventAt == null || eventAt <= 0L) return stringResource(R.string.settings_time_unknown)
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (use24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(eventAt))
}

@Composable
internal fun formatIncidentElapsed(elapsedMs: Long?): String {
    if (elapsedMs == null) return stringResource(R.string.settings_no_schedule_delta)
    val absoluteSeconds = kotlin.math.abs(elapsedMs) / 1000L
    if (absoluteSeconds < 60L) return stringResource(R.string.settings_within_minute_schedule)
    val minutes = (absoluteSeconds / 60L).toInt()
    return pluralStringResource(
        if (elapsedMs < 0L) R.plurals.settings_minutes_before_schedule
        else R.plurals.settings_minutes_after_schedule_plural,
        minutes,
        minutes
    )
}

@Composable
internal fun wakeReadinessSummary(state: SettingsUiState): String {
    val locale = LocalConfiguration.current.locales[0]
    val exactAlarms = stringResource(R.string.settings_readiness_exact_alarms)
    val notifications = stringResource(R.string.settings_readiness_notifications)
    val fullScreenAlarmAccess = stringResource(R.string.settings_readiness_fullscreen_alarm)
    val localNetworkAccess = stringResource(R.string.settings_readiness_local_network)
    val battery = stringResource(R.string.settings_readiness_battery)
    val standbyBucket = stringResource(R.string.settings_readiness_standby_bucket)
    val missing = buildList {
        if (!state.canScheduleExactAlarms) add(exactAlarms)
        if (!state.hasNotificationPermission) add(notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            state.canUseFullScreenIntent != true
        ) {
            add(fullScreenAlarmAccess)
        }
        if (requiresLocalNetworkAccess(state) && !state.hasLocalNetworkPermission) {
            add(localNetworkAccess)
        }
        if (!state.isIgnoringBatteryOptimizations) add(battery)
        // v1.11.3 (roadmap N3): include standby-bucket throttling in the
        // top-tile summary so the user sees it without expanding the section.
        if (state.appStandbyBucket != AppStandbyBucket.UNKNOWN &&
            AppStandbyBucket.isDegraded(state.appStandbyBucket)
        ) {
            add(standbyBucket)
        }
    }
    return if (missing.isEmpty()) {
        val fullScreenAccess = stringResource(R.string.settings_readiness_fullscreen)
        val lanAccess = stringResource(R.string.settings_readiness_lan)
        val standby = stringResource(R.string.settings_readiness_standby)
        val optionalChecks = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(fullScreenAccess)
            }
            if (requiresLocalNetworkAccess(state)) {
                add(lanAccess)
            }
            add(battery)
            add(standby)
        }
        val formattedChecks = android.icu.text.ListFormatter.getInstance(locale).format(optionalChecks)
        stringResource(R.string.settings_readiness_ready, formattedChecks)
    } else {
        val formattedMissing = android.icu.text.ListFormatter.getInstance(locale).format(missing)
        stringResource(R.string.settings_readiness_review, formattedMissing)
    }
}

internal fun requiresLocalNetworkAccess(state: SettingsUiState): Boolean {
    if (!LocalNetworkPermission.isRuntimeRequired()) return false
    return state.settings.hueBridgeIp.isNotBlank() ||
        LocalNetworkPermission.isLikelyLocalEndpoint(state.settings.webhookUrl)
}

@Composable
internal fun formatWebhookDeliveryStatus(settings: AppSettings): String? {
    val status = settings.webhookLastDeliveryStatus.takeIf { it.isNotBlank() } ?: return null
    val locale = LocalConfiguration.current.locales[0]
    val timestamp = settings.webhookLastDeliveryAtMillis.takeIf { it > 0 }
        ?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(
                    DateTimeFormatter.ofLocalizedDateTime(
                        java.time.format.FormatStyle.MEDIUM,
                        java.time.format.FormatStyle.SHORT
                    ).withLocale(locale)
                )
        } ?: stringResource(R.string.settings_recently)
    return stringResource(R.string.settings_last_delivery, timestamp, status)
}

@Composable
internal fun BufferedSettingsTextField(
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
internal fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onClickDescription = stringResource(R.string.settings_change_date, label.lowercase())
    val fieldDescription = stringResource(R.string.settings_date_field_description, label, value)
    Box(
        modifier = modifier
            .background(
                color = SurfaceCard.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.dp, TextMuted.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .clickable(
                onClickLabel = onClickDescription,
                role = Role.Button,
                onClick = onClick
            )
            // Merge the label + value into one actionable announcement so TalkBack
            // reads "Starts: Jun 14, 2026, button" instead of two separate nodes.
            .semantics(mergeDescendants = true) { contentDescription = fieldDescription }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun formatSeconds(totalSeconds: Int): String {
    if (totalSeconds == 0) return stringResource(R.string.settings_off)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return when {
        m == 0 -> stringResource(R.string.settings_seconds_short, s)
        s == 0 -> stringResource(R.string.settings_minutes_compact, m)
        else -> stringResource(R.string.settings_minutes_seconds_compact, m, s)
    }
}
