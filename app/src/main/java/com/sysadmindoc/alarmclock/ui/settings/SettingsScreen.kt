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
        var selectedPaneId by rememberSaveable { mutableStateOf(settingsPaneCategories.first().id) }
        val selectedPane = settingsPaneCategories.firstOrNull { it.id == selectedPaneId }
            ?: settingsPaneCategories.first()
        val showAllSettings = !useTwoPane
        val settingsListState = rememberLazyListState()

        androidx.compose.runtime.LaunchedEffect(useTwoPane, selectedPane.id) {
            if (useTwoPane) settingsListState.scrollToItem(0)
        }

        val settingsContent: @Composable (Modifier) -> Unit = { contentModifier ->
            LazyColumn(
                modifier = contentModifier,
                state = settingsListState,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (useTwoPane) {
                    item(key = "pane-header-${selectedPane.id}") {
                        SettingsPaneHeader(selectedPane, state)
                    }
                } else {
                    item(key = "settings-hero") {
                    AlarmClockHeroHeader(
                        title = stringResource(R.string.settings_title),
                        subtitle = stringResource(R.string.settings_subtitle),
                        badge = {
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
                                label = state.appVersion,
                                icon = Icons.Default.AutoAwesome
                            )
                        }
                    )
                    }
                }
            if (showAllSettings || selectedPane.id == "readiness") {
            settingsItem("readiness-wake") {
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

            if (showAllSettings || selectedPane.id == "defaults") {
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

            if (showAllSettings || selectedPane.id == "integrations") {
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
            if (showAllSettings || selectedPane.id == "personalization") {
            settingsItem("personalization") {
                PersonalizationSection(state, viewModel)
            }
            }
            if (showAllSettings || selectedPane.id == "backup") {
            settingsItem("backup-restore") {
                BackupRestoreSection(viewModel, is24HourFormat = state.settings.is24HourFormat)
            }
            }

            if (showAllSettings || selectedPane.id == "utilities") {
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
private fun IncidentTimelineSection(
    timeline: SettingsIncidentTimelineState,
    use24Hour: Boolean,
    onClearIncidentHistory: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    SettingsGroup(
        title = stringResource(R.string.settings_diagnostics),
        description = stringResource(R.string.settings_diagnostics_description)
    ) {
        if (!timeline.hasIncidents) {
            SettingsInfo(
                label = stringResource(R.string.settings_incident_history),
                description = stringResource(R.string.settings_incident_empty)
            )
            Text(
                text = stringResource(R.string.settings_incident_retention),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            AppSectionTitle(
                title = stringResource(
                    if (timeline.latestIsDegraded) R.string.settings_latest_degraded else R.string.settings_latest_incident
                ),
                description = pluralStringResource(
                    R.plurals.settings_recent_diagnostics,
                    timeline.recentCount,
                    timeline.recentCount
                ),
                action = {
                    AppStatusChip(
                        label = timeline.latestStatus.orEmpty().ifBlank {
                            stringResource(R.string.settings_unknown_code)
                        },
                        icon = if (timeline.latestIsDegraded) Icons.Default.Warning else Icons.Default.CheckCircle,
                        color = if (timeline.latestIsDegraded) SnoozeYellow else DismissGreen
                    )
                }
            )
            val incidentTimestamp = formatIncidentTimestamp(timeline.latestEventAt, use24Hour)
            val incidentElapsed = formatIncidentElapsed(timeline.latestElapsedMs)
            SettingsInfo(
                label = incidentLabel(timeline.latestType),
                description = stringResource(
                    R.string.settings_incident_timing,
                    incidentTimestamp,
                    incidentElapsed
                )
            )
            SettingsInfo(
                label = stringResource(R.string.settings_reason_code),
                description = timeline.latestReason.orEmpty().ifBlank {
                    stringResource(R.string.settings_none_code)
                }
            )
            Text(
                text = stringResource(R.string.settings_clear_diagnostics_hint),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = { showClearDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
            ) {
                Text(stringResource(R.string.settings_clear_diagnostics))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SnoozeYellow)
            },
            title = { Text(stringResource(R.string.settings_clear_diagnostics_title)) },
            text = {
                Text(
                    text = stringResource(R.string.settings_clear_diagnostics_message),
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
                    Text(stringResource(R.string.settings_clear_diagnostics), color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = stringResource(R.string.settings_at_a_glance),
            description = stringResource(R.string.settings_at_a_glance_description)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsOverviewTile(
                title = stringResource(R.string.settings_reliability),
                value = stringResource(
                    if (reliabilityReady) R.string.settings_ready else R.string.settings_needs_review
                ),
                supporting = wakeReadinessSummary(state),
                icon = if (reliabilityReady) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                accent = if (reliabilityReady) DismissGreen else SnoozeYellow,
                modifier = Modifier.width(190.dp)
            )
            SettingsOverviewTile(
                title = stringResource(R.string.settings_dashboard),
                value = dashboardSummary(state),
                supporting = stringResource(R.string.settings_dashboard_overview_description),
                icon = Icons.Default.Cloud,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(190.dp)
            )
            SettingsOverviewTile(
                title = stringResource(R.string.settings_wake_style),
                value = stringResource(
                    if (state.settings.is24HourFormat) R.string.settings_24_hour else R.string.settings_12_hour
                ),
                supporting = stringResource(
                    R.string.settings_default_snooze_summary,
                    state.settings.defaultSnoozeDuration
                ),
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
            title = stringResource(R.string.settings_wake_readiness),
            description = stringResource(R.string.settings_wake_readiness_description),
            action = {
                AppStatusChip(
                    label = stringResource(R.string.settings_ready_count, readyCount, total),
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
            Text(stringResource(R.string.settings_open_setup_checklist))
        }

        WakeReadinessRow(
            icon = Icons.Default.Alarm,
            title = stringResource(R.string.settings_real_test_alarm),
            description = testAlarmProofDescription(
                proof = state.testAlarmProof,
                is24HourFormat = state.settings.is24HourFormat
            ),
            ready = testAlarmProofReady,
            statusLabel = testAlarmProofStatusLabel(state.testAlarmProof),
            actionLabel = stringResource(R.string.settings_run_setup_checklist),
            onAction = onOpenOnboardingChecklist
        )
        WakeReadinessRow(
            icon = Icons.Default.Alarm,
            title = stringResource(R.string.settings_exact_alarm_access),
            description = stringResource(R.string.settings_exact_alarm_description),
            ready = state.canScheduleExactAlarms,
            actionLabel = stringResource(R.string.settings_open_alarm_access),
            onAction = onRequestExactAlarms
        )
        WakeReadinessRow(
            icon = Icons.Default.NotificationsActive,
            title = stringResource(R.string.settings_alarm_notifications),
            description = stringResource(R.string.settings_alarm_notifications_description),
            ready = state.hasNotificationPermission,
            actionLabel = stringResource(R.string.settings_allow_notifications),
            onAction = onRequestNotifications
        )
        if (fullScreenRowVisible) {
            WakeReadinessRow(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(R.string.settings_fullscreen_access),
                description = when (state.canUseFullScreenIntent) {
                    true -> stringResource(R.string.settings_fullscreen_allowed)
                    false -> stringResource(R.string.settings_fullscreen_blocked)
                    null -> stringResource(R.string.settings_fullscreen_unknown)
                },
                ready = state.canUseFullScreenIntent == true,
                actionLabel = stringResource(R.string.settings_open_fullscreen),
                onAction = onRequestFullScreenAlarms
            )
        }
        if (localNetworkRowVisible) {
            WakeReadinessRow(
                icon = Icons.Default.Link,
                title = stringResource(R.string.settings_local_network_access),
                description = stringResource(R.string.settings_local_network_description),
                ready = state.hasLocalNetworkPermission,
                actionLabel = stringResource(R.string.settings_allow_local_network),
                onAction = onRequestLocalNetworkPermission
            )
        }
        WakeReadinessRow(
            icon = Icons.Default.BatteryAlert,
            title = stringResource(R.string.settings_battery_protection),
            description = stringResource(R.string.settings_battery_protection_description),
            ready = state.isIgnoringBatteryOptimizations,
            actionLabel = stringResource(R.string.settings_open_battery),
            onAction = onRequestBatteryExemption
        )
        if (standbyRowVisible) {
            WakeReadinessRow(
                icon = Icons.Default.BatteryAlert,
                title = stringResource(R.string.settings_standby_bucket),
                description = standbyBucketDescription(state.appStandbyBucket),
                ready = standbyReady,
                actionLabel = stringResource(R.string.settings_open_battery),
                onAction = onRequestBatteryExemption
            )
        }
        if (state.guardianReadiness.hasEnabledAlarms) {
            WakeReadinessRow(
                icon = Icons.Default.Security,
                title = stringResource(R.string.settings_guardian_escalation),
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
@Composable
private fun standbyBucketDescription(bucket: Int): String = when (bucket) {
    in Int.MIN_VALUE..0 -> stringResource(R.string.settings_standby_unknown)
    10 -> stringResource(R.string.settings_standby_active)
    20 -> stringResource(R.string.settings_standby_working)
    30 -> stringResource(R.string.settings_standby_frequent)
    40 -> stringResource(R.string.settings_standby_rare)
    45 -> stringResource(R.string.settings_standby_restricted)
    else -> stringResource(R.string.settings_standby_other, bucket)
}

@Composable
private fun testAlarmProofStatusLabel(proof: TestAlarmProof): String = stringResource(when {
    proof.hasDetailedCompletion -> R.string.settings_verified
    proof.legacyCompleted -> R.string.settings_refresh
    proof.firedAt > 0L -> R.string.settings_dismiss_test
    else -> R.string.settings_run_test
})

@Composable
private fun testAlarmProofDescription(
    proof: TestAlarmProof,
    is24HourFormat: Boolean
): String {
    if (proof.hasDetailedCompletion) {
        val completed = formatTestAlarmProofTime(proof.completedAt, is24HourFormat)
        val latency = proof.latencyMs?.let { formatTestAlarmLatency(it) }
        val delivery = testAlarmDeliveryPath(proof)
        return if (latency != null) {
            stringResource(R.string.settings_test_dismissed_with_latency, completed, latency, delivery)
        } else {
            stringResource(R.string.settings_test_dismissed_no_latency, completed)
        }
    }
    if (proof.legacyCompleted) return stringResource(R.string.settings_test_legacy)
    if (proof.firedAt > 0L) {
        return stringResource(
            R.string.settings_test_not_dismissed,
            formatTestAlarmProofTime(proof.firedAt, is24HourFormat)
        )
    }
    return stringResource(R.string.settings_test_run_description)
}

@Composable
private fun formatTestAlarmProofTime(epochMillis: Long, is24HourFormat: Boolean): String {
    if (epochMillis <= 0L) return stringResource(R.string.settings_unknown_time)
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (is24HourFormat) "EEE HH:mm" else "EEE h:mm a"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, locale))
}

@Composable
private fun formatTestAlarmLatency(latencyMs: Long): String {
    if (latencyMs < 1_500L) return stringResource(R.string.settings_on_time)
    val totalSeconds = ((latencyMs + 999L) / 1_000L).coerceAtLeast(1L)
    if (totalSeconds < 60L) return stringResource(R.string.settings_seconds_after_schedule, totalSeconds)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (seconds == 0L) {
        stringResource(R.string.settings_minutes_after_schedule, minutes)
    } else {
        stringResource(R.string.settings_minutes_seconds_after_schedule, minutes, seconds)
    }
}

@Composable
private fun testAlarmDeliveryPath(proof: TestAlarmProof): String {
    val parts = buildList {
        if (proof.notificationPermissionGranted) add(stringResource(R.string.settings_delivery_notification))
        if (proof.fullScreenIntentRequested) add(stringResource(R.string.settings_delivery_fullscreen))
        if (proof.activityLaunchSucceeded) add(stringResource(R.string.settings_delivery_direct))
    }
    return parts.joinToString(" + ").ifBlank { stringResource(R.string.settings_delivery_alarm_screen) }
}

@Composable
private fun guardianReadinessDescription(readiness: GuardianReadiness): String {
    val alarmCount = pluralStringResource(
        R.plurals.settings_guardian_alarms,
        readiness.enabledAlarmCount,
        readiness.enabledAlarmCount
    )
    val callPath = if (readiness.hasCallPhonePermission) {
        stringResource(R.string.settings_guardian_call_granted)
    } else {
        stringResource(R.string.settings_guardian_call_dialer)
    }
    return when (readiness.smsPath) {
        GuardianSmsPath.INACTIVE -> stringResource(R.string.settings_guardian_none)
        GuardianSmsPath.DIRECT_SMS ->
            stringResource(R.string.settings_guardian_direct, alarmCount, callPath)
        GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION ->
            stringResource(R.string.settings_guardian_needs_sms, alarmCount, callPath)
        GuardianSmsPath.SMS_COMPOSER ->
            stringResource(R.string.settings_guardian_composer, alarmCount, callPath)
    }
}

@Composable
private fun guardianReadinessStatusLabel(readiness: GuardianReadiness): String = stringResource(
    if (readiness.needsUserAction) R.string.settings_review else when (readiness.smsPath) {
        GuardianSmsPath.INACTIVE -> R.string.settings_off
        GuardianSmsPath.DIRECT_SMS -> R.string.settings_direct_sms
        GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION -> R.string.settings_review
        GuardianSmsPath.SMS_COMPOSER -> R.string.settings_composer
    }
)

@Composable
private fun guardianReadinessActionLabel(readiness: GuardianReadiness): String = stringResource(
    if (readiness.needsSmsPermission) R.string.settings_allow_sms else R.string.settings_allow_calls
)

@Composable
private fun WakeReadinessRow(
    icon: ImageVector,
    title: String,
    description: String,
    ready: Boolean,
    statusLabel: String? = null,
    actionLabel: String,
    onAction: () -> Unit
) {
    val accent = if (ready) DismissGreen else SnoozeYellow
    val resolvedStatusLabel = statusLabel ?: stringResource(
        if (ready) R.string.settings_ready else R.string.settings_review
    )
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
                    label = resolvedStatusLabel,
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
            .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    } else null

    AppSurfaceCard(highlighted = isPaused) {
        AppSectionTitle(
            title = stringResource(R.string.settings_pause_alarms),
            description = if (isPaused) {
                stringResource(R.string.settings_pause_until, resumeAtLabel.orEmpty())
            } else {
                stringResource(R.string.settings_pause_description)
            },
            action = {
                AppStatusChip(
                    label = stringResource(
                        if (isPaused) R.string.settings_paused else R.string.settings_off
                    ),
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
                    label = if (days == 1) {
                        stringResource(R.string.settings_tonight)
                    } else {
                        pluralStringResource(R.plurals.settings_days, days, days)
                    },
                    selected = false,
                    onClick = { viewModel.pauseAlarmsForDays(days) }
                )
            }
            if (isPaused) {
                AppFilterChip(
                    label = stringResource(R.string.settings_resume_now),
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
            .format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    } else {
        stringResource(R.string.settings_choose_start_date)
    }

    val endDate = if (settings.vacationEndMillis > 0) {
        Instant.ofEpochMilli(settings.vacationEndMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    } else {
        stringResource(R.string.settings_choose_end_date)
    }

    AppSurfaceCard(highlighted = settings.vacationModeEnabled) {
        AppSectionTitle(
            title = stringResource(R.string.vacation_mode),
            description = stringResource(R.string.settings_vacation_description),
            action = {
                AppStatusChip(
                    label = stringResource(
                        if (settings.vacationModeEnabled) R.string.settings_active else R.string.settings_off
                    ),
                    icon = Icons.Default.BeachAccess,
                    color = if (settings.vacationModeEnabled) SnoozeYellow else TextMuted
                )
            }
        )

        SettingsToggle(
            label = stringResource(R.string.settings_enable_vacation),
            checked = settings.vacationModeEnabled,
            supportingText = stringResource(R.string.settings_enable_vacation_description),
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
                label = stringResource(R.string.settings_starts),
                value = startDate,
                onClick = { showStartPicker = true }
            )
            DateField(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.settings_ends),
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
                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
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
                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
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
            title = stringResource(R.string.settings_battery_optimization),
            description = if (state.isIgnoringBatteryOptimizations) {
                stringResource(R.string.settings_battery_ready_description)
            } else {
                stringResource(R.string.settings_battery_risk_description)
            },
            action = {
                AppStatusChip(
                    label = stringResource(
                        if (state.isIgnoringBatteryOptimizations) R.string.settings_ready
                        else R.string.settings_action_recommended
                    ),
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
                Text(stringResource(R.string.settings_open_battery))
            }
        }

        if (state.needsBatteryGuidance && state.batteryGuidanceSteps.isNotEmpty()) {
            HorizontalDivider(color = TextMuted.copy(alpha = 0.18f))
            Text(
                text = state.batteryGuidanceTitle.ifBlank {
                    stringResource(R.string.settings_manufacturer_battery_steps, state.manufacturerName)
                },
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
                    Text(stringResource(R.string.settings_battery_guide_link, state.manufacturerName))
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
        title = stringResource(R.string.settings_webhook_integrations),
        description = stringResource(R.string.settings_webhook_description)
    ) {
        SettingsToggle(
            label = stringResource(R.string.settings_enable_webhook),
            checked = state.settings.webhookEnabled,
            supportingText = stringResource(R.string.settings_enable_webhook_description),
            onToggle = viewModel::toggleWebhook
        )

        BufferedSettingsTextField(
            value = state.settings.webhookUrl,
            onCommit = viewModel::updateWebhookUrl,
            label = { Text(stringResource(R.string.settings_webhook_url)) },
            placeholder = { Text(stringResource(R.string.settings_webhook_url_placeholder)) },
            enabled = state.settings.webhookEnabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SettingsToggle(
            label = stringResource(R.string.settings_webhook_include_labels),
            checked = state.settings.webhookIncludeLabel,
            supportingText = stringResource(R.string.settings_webhook_labels_description),
            enabled = state.settings.webhookEnabled,
            onToggle = viewModel::toggleWebhookLabelSharing
        )

        BufferedSettingsTextField(
            value = state.settings.webhookSigningSecret,
            onCommit = viewModel::updateWebhookSigningSecret,
            label = { Text(stringResource(R.string.settings_signing_secret)) },
            placeholder = { Text(stringResource(R.string.settings_signing_secret_placeholder)) },
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
                title = stringResource(R.string.settings_webhook_blocked),
                message = stringResource(R.string.settings_webhook_blocked_description),
                icon = Icons.Default.Warning,
                color = AccentRed
            )
        }
        if (localWebhookPermissionMissing) {
            AppInlineNotice(
                title = stringResource(R.string.settings_local_network_needed),
                message = stringResource(R.string.settings_webhook_network_description),
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
                    ?: stringResource(R.string.settings_webhook_payload_description),
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
                Text(
                    stringResource(
                        if (state.isWebhookTesting) R.string.settings_testing else R.string.settings_test
                    )
                )
            }
        }

        val deliveryLog = state.settings.webhookDeliveryLog
        if (deliveryLog.isNotBlank()) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.settings_recent_deliveries),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.size(4.dp))
            deliveryLog.lineSequence().filter { it.isNotBlank() }.take(8).forEach { line ->
                Text(
                    text = formatWebhookLogLine(line),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWebhookLogLineSuccess(line)) DismissGreen else AccentRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (state.settings.webhookEnabled) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.settings_recent_deliveries),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.settings_webhook_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Delivery-log lines are stored by WebhookService as
 * "<ISO instant> <event wire name> <OK|failed>[ (code)][: Reason]", so the
 * third whitespace token is the structured status — never key success off a
 * substring match (a failure reason could legitimately contain "OK").
 */
private fun isWebhookLogLineSuccess(line: String): Boolean =
    line.split(' ').getOrNull(2) == "OK"

/**
 * Render a stored "<ISO instant> <status>" delivery-log line as a friendly
 * local time. Falls back to the raw line if the leading token isn't an instant.
 */
@Composable
private fun formatWebhookLogLine(line: String): String {
    val spaceIdx = line.indexOf(' ')
    if (spaceIdx <= 0) return line
    val locale = LocalConfiguration.current.locales[0]
    val instantPart = line.substring(0, spaceIdx)
    val rest = line.substring(spaceIdx + 1)
    val local = runCatching {
        val local = Instant.parse(instantPart)
            .atZone(ZoneId.systemDefault())
            .format(
                DateTimeFormatter.ofLocalizedDateTime(
                    java.time.format.FormatStyle.MEDIUM,
                    java.time.format.FormatStyle.SHORT
                ).withLocale(locale)
            )
        local
    }.getOrNull() ?: return line
    return stringResource(R.string.settings_webhook_log_line, local, rest)
}

@Composable
private fun HolidaysSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = stringResource(R.string.settings_public_holidays),
        description = stringResource(R.string.settings_public_holidays_description)
    ) {
        SettingsToggle(
            label = stringResource(R.string.settings_skip_holidays),
            checked = state.settings.holidayAutoSkipEnabled,
            supportingText = stringResource(R.string.settings_skip_holidays_description),
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
            label = { Text(stringResource(R.string.settings_country_code)) },
            placeholder = { Text(stringResource(R.string.settings_country_code_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            )
        )
        Text(
            text = if (state.settings.holidayAutoSkipEnabled) {
                stringResource(R.string.settings_holiday_enabled_description)
            } else {
                stringResource(R.string.settings_holiday_disabled_description)
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
        title = stringResource(R.string.settings_hue_sunrise),
        description = stringResource(R.string.settings_hue_sunrise_description)
    ) {
        BufferedSettingsTextField(
            value = state.settings.hueBridgeIp,
            onCommit = viewModel::updateHueBridgeIp,
            label = { Text(stringResource(R.string.settings_hue_ip)) },
            placeholder = { Text(stringResource(R.string.settings_hue_ip_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        BufferedSettingsTextField(
            value = state.settings.hueApiKey,
            onCommit = viewModel::updateHueApiKey,
            label = { Text(stringResource(R.string.settings_hue_api_key)) },
            placeholder = { Text(stringResource(R.string.settings_hue_api_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        BufferedSettingsTextField(
            value = state.settings.hueLightIds,
            onCommit = viewModel::updateHueLightIds,
            label = { Text(stringResource(R.string.settings_hue_light_ids)) },
            placeholder = { Text(stringResource(R.string.settings_hue_light_ids_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SettingsToggle(
            label = stringResource(R.string.settings_hue_legacy),
            checked = state.settings.hueLegacyHttpEnabled,
            supportingText = stringResource(R.string.settings_hue_legacy_description),
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
                        text = stringResource(R.string.settings_hue_cert_pinned),
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
                    Text(stringResource(R.string.settings_forget))
                }
            }
        }
        if (localNetworkPermissionMissing) {
            AppInlineNotice(
                title = stringResource(R.string.settings_local_network_needed),
                message = stringResource(R.string.settings_hue_network_description),
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
                text = state.hueTestResult ?: stringResource(R.string.settings_hue_test_description),
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
                Text(
                    stringResource(
                        if (state.isHueTesting) R.string.settings_testing else R.string.settings_test
                    )
                )
            }
        }
    }
    if (showForgetCertificateDialog) {
        AlertDialog(
            onDismissRequest = { showForgetCertificateDialog = false },
            title = { Text(stringResource(R.string.settings_hue_forget_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_hue_forget_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgetCertificateDialog = false
                        viewModel.clearHueCertificatePin()
                    }
                ) {
                    Text(stringResource(R.string.settings_hue_forget_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetCertificateDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = stringResource(R.string.settings_health_connect),
            description = if (isPlayFlavor) {
                healthConnectDescription(state.settings.healthConnectEnabled, summary)
            } else {
                stringResource(R.string.settings_health_fdroid_description)
            }
        )
        SettingsToggle(
            label = stringResource(R.string.settings_health_read_sleep),
            checked = state.settings.healthConnectEnabled,
            supportingText = if (isPlayFlavor) {
                stringResource(R.string.settings_health_read_sleep_description)
            } else {
                stringResource(R.string.settings_health_fdroid_permission)
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
                        HealthConnectAvailability.AVAILABLE -> stringResource(R.string.settings_health_sdk_available)
                        HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> stringResource(R.string.settings_health_update_needed)
                        HealthConnectAvailability.UNAVAILABLE -> stringResource(R.string.settings_health_unavailable)
                        HealthConnectAvailability.NOT_INCLUDED -> stringResource(R.string.settings_health_not_included)
                    },
                    icon = if (summary.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (summary.isAvailable) DismissGreen else SnoozeYellow
                )
                AppStatusChip(
                    label = stringResource(
                        if (summary.permissionGranted) R.string.settings_health_permission_granted
                        else R.string.settings_health_permission_needed
                    ),
                    icon = if (summary.permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (summary.permissionGranted) DismissGreen else SnoozeYellow
                )
            }
            if (summary.permissionGranted) {
                Text(
                    text = if (summary.hasRecentSession) {
                        stringResource(
                            R.string.settings_health_last_session,
                            formatSleepMinutes(summary.lastSessionDurationMinutes),
                            summary.sessionsRead
                        )
                    } else {
                        stringResource(R.string.settings_health_no_recent_sessions)
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            summary.errorMessage?.let { error ->
                AppInlineNotice(
                    title = stringResource(R.string.settings_health_attention),
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
                    Text(
                        stringResource(
                            if (summary.permissionGranted) R.string.settings_health_review_access
                            else R.string.settings_health_grant_access
                        )
                    )
                }
                OutlinedButton(
                    onClick = viewModel::refreshHealthConnectSleep,
                    enabled = summary.isAvailable,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Bedtime, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_refresh))
                }
            }
        }
    }
}

@Composable
private fun healthConnectDescription(
    enabled: Boolean,
    summary: HealthConnectSleepSummary
): String = when {
    summary.availability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
        stringResource(R.string.settings_health_update_description)
    summary.availability == HealthConnectAvailability.UNAVAILABLE ->
        stringResource(R.string.settings_health_unavailable_description)
    !enabled ->
        stringResource(R.string.settings_health_opt_in_description)
    !summary.permissionGranted ->
        stringResource(R.string.settings_health_grant_description)
    summary.hasRecentSession ->
        stringResource(R.string.settings_health_available_description)
    else ->
        stringResource(R.string.settings_health_empty_description)
}

@Composable
private fun formatSleepMinutes(minutes: Long?): String {
    val value = minutes ?: return stringResource(R.string.settings_health_no_session)
    val hours = value / 60
    val mins = value % 60
    return when {
        hours > 0 && mins > 0 -> stringResource(R.string.settings_hours_minutes_short, hours, mins)
        hours > 0 -> stringResource(R.string.settings_hours_short, hours)
        else -> stringResource(R.string.settings_minutes_compact, mins)
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
            name = stringResource(R.string.settings_connection_weather),
            enabled = state.settings.showWeatherOnDashboard,
            domain = "api.open-meteo.com",
            dataSent = stringResource(R.string.settings_connection_location_data),
            offlineFallback = stringResource(R.string.settings_connection_cached_forecast)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_air_quality),
            enabled = state.settings.showWeatherOnDashboard,
            domain = "air-quality-api.open-meteo.com",
            dataSent = stringResource(R.string.settings_connection_location_data),
            offlineFallback = stringResource(R.string.settings_connection_hidden_unavailable)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_nws),
            enabled = state.settings.showWeatherOnDashboard,
            domain = "api.weather.gov",
            dataSent = stringResource(R.string.settings_connection_us_location_data),
            offlineFallback = stringResource(R.string.settings_connection_no_alerts)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_public_holidays),
            enabled = state.settings.holidayAutoSkipEnabled,
            domain = "date.nager.at",
            dataSent = stringResource(R.string.settings_country_code),
            offlineFallback = stringResource(R.string.settings_connection_cached_holidays)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_radar),
            enabled = state.settings.showRadarEmbed,
            domain = "embed.windy.com",
            dataSent = stringResource(R.string.settings_connection_embed_location),
            offlineFallback = stringResource(R.string.settings_connection_radar_hidden)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_news),
            enabled = state.settings.showNewsTab,
            domain = state.settings.newsFeedUrl
                .removePrefix("https://").removePrefix("http://")
                .substringBefore("/").ifBlank { stringResource(R.string.settings_connection_user_configured) },
            dataSent = stringResource(R.string.settings_connection_feed_data),
            offlineFallback = stringResource(R.string.settings_connection_empty_feed)
        ))
        if (state.settings.webhookEnabled) {
            add(ConnectionInfo(
                name = stringResource(R.string.settings_connection_webhook),
                enabled = true,
                domain = state.settings.webhookUrl
                    .removePrefix("https://").removePrefix("http://")
                    .substringBefore("/").ifBlank { stringResource(R.string.settings_connection_not_configured) },
                dataSent = stringResource(R.string.settings_connection_webhook_data),
                offlineFallback = stringResource(R.string.settings_connection_events_dropped)
            ))
        }
        if (state.settings.hueBridgeIp.isNotBlank()) {
            add(ConnectionInfo(
                name = stringResource(R.string.settings_connection_hue),
                enabled = true,
                domain = stringResource(R.string.settings_connection_lan_domain, state.settings.hueBridgeIp),
                dataSent = stringResource(R.string.settings_connection_hue_data),
                offlineFallback = stringResource(R.string.settings_connection_sunrise_skipped)
            ))
        }
        add(ConnectionInfo(
            name = stringResource(R.string.settings_health_connect),
            enabled = state.settings.healthConnectEnabled,
            domain = stringResource(R.string.settings_connection_on_device),
            dataSent = stringResource(R.string.settings_connection_health_data),
            offlineFallback = stringResource(R.string.settings_connection_always_local)
        ))
    }

    SettingsGroup(
        title = stringResource(R.string.settings_connections_data),
        description = stringResource(R.string.settings_connections_data_description)
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
                            label = stringResource(
                                if (conn.enabled) R.string.settings_active else R.string.settings_off
                            ),
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
                        text = stringResource(R.string.settings_connection_sends, conn.dataSent),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = stringResource(R.string.settings_connection_offline, conn.offlineFallback),
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
        title = stringResource(R.string.settings_personalization),
        description = stringResource(R.string.settings_personalization_description)
    ) {
        AccentColorPicker(
            currentHex = state.settings.accentColor,
            onPick = viewModel::updateAccentColor
        )

        SettingsToggle(
            label = stringResource(R.string.settings_motivational_quotes),
            checked = state.settings.showMotivationalQuotes,
            supportingText = stringResource(R.string.settings_motivational_quotes_description),
            onToggle = viewModel::toggleShowMotivationalQuotes
        )
        SettingsToggle(
            label = stringResource(R.string.settings_adaptive_difficulty),
            checked = state.settings.adaptiveDifficultyEnabled,
            supportingText = stringResource(R.string.settings_adaptive_difficulty_description),
            onToggle = viewModel::toggleAdaptiveDifficulty
        )

        // v1.4.0: Material You — respects the user's wallpaper palette on Android 12+.
        // On older devices the toggle is still persisted but has no visual effect,
        // so the help copy names the requirement rather than silently no-op'ing.
        SettingsToggle(
            label = stringResource(R.string.settings_dynamic_color),
            checked = state.settings.dynamicColorEnabled,
            supportingText = stringResource(R.string.settings_dynamic_color_description),
            onToggle = viewModel::toggleDynamicColor
        )

        SettingsToggle(
            label = stringResource(R.string.settings_expressive_surfaces),
            checked = state.settings.expressiveModeEnabled,
            supportingText = stringResource(R.string.settings_expressive_surfaces_description),
            onToggle = viewModel::toggleExpressiveMode
        )

        SettingsToggle(
            label = stringResource(R.string.settings_reduce_motion),
            checked = state.settings.reduceMotionAndFlashing,
            supportingText = stringResource(R.string.settings_reduce_motion_description),
            onToggle = viewModel::toggleReduceMotionAndFlashing
        )

        SettingsToggle(
            label = stringResource(R.string.settings_cover_to_snooze),
            checked = state.settings.coverToSnoozeEnabled,
            supportingText = stringResource(R.string.settings_cover_to_snooze_description),
            onToggle = viewModel::toggleCoverToSnooze
        )

        SettingsToggle(
            label = stringResource(R.string.settings_repeat_missed),
            checked = state.settings.repeatMissedAlarms,
            supportingText = stringResource(R.string.settings_repeat_missed_description),
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
                Text(stringResource(R.string.settings_cancellation_lock), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_cancellation_lock_description),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showLockMenu = true }) {
                    Text(
                        if (state.settings.cancellationLockMinutes == 0) {
                            stringResource(R.string.settings_disabled)
                        } else {
                            stringResource(R.string.settings_minutes_short, state.settings.cancellationLockMinutes)
                        },
                        color = AccentBlue
                    )
                }
                DropdownMenu(expanded = showLockMenu, onDismissRequest = { showLockMenu = false }) {
                    listOf(0, 15, 30, 60).forEach { mins ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (mins == 0) {
                                        stringResource(R.string.settings_disabled)
                                    } else {
                                        stringResource(R.string.settings_lock_before_fire, mins)
                                    }
                                )
                            },
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
                Text(stringResource(R.string.settings_firing_controls), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_firing_controls_description),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showFiringModeMenu = true }) {
                    Text(
                        when (state.settings.firingControlMode) {
                            "buttons" -> stringResource(R.string.settings_firing_buttons)
                            "swipe" -> stringResource(R.string.settings_firing_swipe)
                            else -> stringResource(R.string.settings_firing_hybrid)
                        },
                        color = AccentBlue
                    )
                }
                DropdownMenu(expanded = showFiringModeMenu, onDismissRequest = { showFiringModeMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_firing_hybrid_description)) },
                        onClick = { viewModel.updateFiringControlMode("hybrid"); showFiringModeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_firing_buttons_description)) },
                        onClick = { viewModel.updateFiringControlMode("buttons"); showFiringModeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_firing_swipe_description)) },
                        onClick = { viewModel.updateFiringControlMode("swipe"); showFiringModeMenu = false }
                    )
                }
            }
        }

        BufferedSettingsTextField(
            value = state.settings.customTypingPhrases,
            onCommit = viewModel::updateCustomTypingPhrases,
            label = { Text(stringResource(R.string.settings_custom_typing_phrases), color = TextMuted) },
            placeholder = { Text(stringResource(R.string.settings_custom_typing_phrases_placeholder), color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6
        )
        Text(
            text = stringResource(R.string.settings_custom_typing_phrases_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        SettingsToggle(
            label = stringResource(R.string.settings_challenge_bypass),
            supportingText = stringResource(R.string.settings_challenge_bypass_description),
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
                Text(stringResource(R.string.settings_bypass_delay), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(10, 30, 60, 120).forEach { secs ->
                        val selected = state.settings.challengeBypassDelaySeconds == secs
                        AppFilterChip(
                            selected = selected,
                            onClick = { viewModel.updateChallengeBypassDelay(secs) },
                            label = stringResource(R.string.settings_seconds_short, secs)
                        )
                    }
                }
            }
        }

        SettingsToggle(
            label = stringResource(R.string.settings_challenge_audio_ducking),
            supportingText = stringResource(R.string.settings_challenge_audio_ducking_description),
            checked = state.settings.challengeAudioDuckingEnabled,
            onToggle = viewModel::updateChallengeAudioDuckingEnabled
        )
        if (state.settings.challengeAudioDuckingEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_challenge_volume, state.settings.challengeAudioDuckPercent),
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
                            label = stringResource(R.string.settings_percent, percent)
                        )
                    }
                }
            }
        }

        // v1.4.0: Pre-sleep checklist items, shown on the Bedtime tab.
        BufferedSettingsTextField(
            value = state.settings.bedtimeChecklist,
            onCommit = viewModel::updateBedtimeChecklist,
            label = { Text(stringResource(R.string.settings_bedtime_checklist), color = TextMuted) },
            placeholder = { Text(stringResource(R.string.settings_bedtime_checklist_placeholder), color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6
        )
        Text(
            text = stringResource(R.string.settings_bedtime_checklist_description),
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
        "#5B9EF4" to R.string.settings_accent_default_blue,
        "#7C5CFF" to R.string.settings_accent_violet,
        "#FF6F8A" to R.string.settings_accent_coral,
        "#FFB347" to R.string.settings_accent_amber,
        "#5BD49A" to R.string.settings_accent_mint,
        "#E0E4EA" to R.string.settings_accent_mono
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_accent_color),
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.settings_accent_color_description),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            palette.forEach { (hex, labelRes) ->
                val isSelected = hex.equals(currentHex, ignoreCase = true)
                val label = stringResource(labelRes)
                val accentContentDescription = stringResource(R.string.settings_accent_semantics, label)
                val accentStateDescription = stringResource(
                    if (isSelected) R.string.settings_selected else R.string.settings_not_selected
                )
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
                            onClickLabel = stringResource(R.string.settings_use_accent, label),
                            onClick = { onPick(hex) }
                        )
                        .semantics {
                            contentDescription = accentContentDescription
                            selected = isSelected
                            stateDescription = accentStateDescription
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
private fun BackupRestoreSection(viewModel: SettingsViewModel, is24HourFormat: Boolean) {
    val resources = LocalResources.current
    val unexpectedError = stringResource(R.string.settings_unexpected_error)
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var encryptedPassphrase by remember { mutableStateOf("") }
    var encryptedPassphraseConfirm by remember { mutableStateOf("") }
    var pendingExportWarning by remember { mutableStateOf<BackupExportWarning?>(null) }
    var pendingExportKind by remember { mutableStateOf<BackupExportKind?>(null) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingFossifyImport by remember { mutableStateOf<PendingFossifyImport?>(null) }
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

    fun requestFossifyImport(uri: Uri) {
        scope.launch {
            importPreviewBusy = true
            try {
                viewModel.inspectFossifyImport(uri)
                    .onSuccess { preview -> pendingFossifyImport = PendingFossifyImport(uri, preview) }
                    .onFailure { error ->
                        // Fixed calm copy only — the raw exception detail stays in the log
                        // (see FossifyImportManager), never in a user-facing notice.
                        viewModel.showBackupResult(
                            resources.getString(fossifyPreviewFailureRes(error))
                        )
                    }
            } finally {
                importPreviewBusy = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { requestBackupImport(it, encrypted = false) } }

    val fossifyImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(::requestFossifyImport) }

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
                    listOf(
                        resources.getString(
                            R.string.settings_backup_inspection_failed,
                            error.message ?: unexpectedError
                        )
                    )
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
        title = stringResource(R.string.settings_backup_restore),
        description = stringResource(R.string.settings_backup_restore_description)
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
                Text(stringResource(R.string.settings_export))
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
                Text(stringResource(R.string.settings_import))
            }
        }

        Text(
            text = stringResource(R.string.settings_plain_backup_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))

        OutlinedButton(
            onClick = { fossifyImportLauncher.launch(arrayOf("application/json", "text/plain")) },
            enabled = !operationBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.settings_import_fossify))
        }
        Text(
            text = stringResource(R.string.settings_import_fossify_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_encrypted_backup),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_encrypted_backup_description),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = encryptedPassphrase,
                onValueChange = { encryptedPassphrase = it },
                label = { Text(stringResource(R.string.settings_passphrase)) },
                placeholder = { Text(stringResource(R.string.settings_passphrase_required)) },
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
                label = { Text(stringResource(R.string.settings_confirm_passphrase)) },
                placeholder = { Text(stringResource(R.string.settings_confirm_passphrase_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = passphraseMismatch,
                supportingText = if (passphraseMismatch) {
                    {
                        Text(
                            stringResource(R.string.settings_passphrase_mismatch),
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
                    Text(stringResource(R.string.settings_encrypt_export))
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
                    Text(stringResource(R.string.settings_decrypt_import))
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

    pendingFossifyImport?.let { pending ->
        FossifyImportPreviewDialog(
            pending = pending,
            is24HourFormat = is24HourFormat,
            onDismiss = { pendingFossifyImport = null },
            onImport = {
                pendingFossifyImport = null
                viewModel.importFossifyAlarms(pending.uri, pending.preview.fingerprint)
            }
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
                        text = stringResource(
                            if (importPreviewBusy) R.string.settings_inspecting_backup
                            else R.string.settings_backup_in_progress
                        ),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (importPreviewBusy) {
                            stringResource(R.string.settings_restore_choices_description)
                        } else {
                            stringResource(R.string.settings_backup_locked_description)
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
            title = stringResource(
                if (failed) R.string.settings_backup_attention else R.string.settings_backup_complete
            ),
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

private data class PendingFossifyImport(
    val uri: Uri,
    val preview: FossifyImportPreview
)

/** Maps a sanitized Fossify inspect failure to its fixed user-facing copy. */
private fun fossifyPreviewFailureRes(error: Throwable): Int =
    when ((error as? FossifyImportException)?.kind) {
        FossifyImportErrorKind.UNREADABLE -> R.string.settings_fossify_preview_unreadable
        else -> R.string.settings_fossify_preview_not_export
    }

@Composable
private fun fossifyShortDayLabels(): Map<DayOfWeek, String> = mapOf(
    DayOfWeek.MONDAY to stringResource(R.string.alarm_edit_day_monday_short),
    DayOfWeek.TUESDAY to stringResource(R.string.alarm_edit_day_tuesday_short),
    DayOfWeek.WEDNESDAY to stringResource(R.string.alarm_edit_day_wednesday_short),
    DayOfWeek.THURSDAY to stringResource(R.string.alarm_edit_day_thursday_short),
    DayOfWeek.FRIDAY to stringResource(R.string.alarm_edit_day_friday_short),
    DayOfWeek.SATURDAY to stringResource(R.string.alarm_edit_day_saturday_short),
    DayOfWeek.SUNDAY to stringResource(R.string.alarm_edit_day_sunday_short)
)

@Composable
private fun FossifyImportPreviewDialog(
    pending: PendingFossifyImport,
    is24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    val preview = pending.preview
    val defaultAlarmLabel = stringResource(R.string.direct_boot_alarm_title)
    val dayLabels = fossifyShortDayLabels()
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(is24HourFormat, locale) {
        DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "h:mm a", locale)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restore, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_review_fossify_alarms)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        R.string.settings_fossify_preview_counts,
                        preview.alarmCount,
                        preview.invalidAlarmCount
                    ),
                    color = TextPrimary
                )
                Text(
                    stringResource(
                        R.string.settings_fossify_import_disabled_summary,
                        preview.sourceEnabledAlarmCount
                    ),
                    color = SnoozeYellow,
                    style = MaterialTheme.typography.bodySmall
                )
                if (preview.unreadableRingtoneCount > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.settings_fossify_unreadable_ringtones,
                            preview.unreadableRingtoneCount,
                            preview.unreadableRingtoneCount
                        ),
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                preview.alarms.take(5).forEach { alarm ->
                    val days = alarm.repeatDays.mapNotNull(dayLabels::get).joinToString(", ")
                    val daySummary = if (days.isBlank()) {
                        ""
                    } else {
                        stringResource(R.string.settings_fossify_days_suffix, days)
                    }
                    Text(
                        stringResource(
                            R.string.settings_fossify_alarm_summary,
                            LocalTime.of(alarm.hour, alarm.minute).format(timeFormatter),
                            alarm.label.ifBlank { defaultAlarmLabel },
                            daySummary
                        ),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (preview.alarmCount > 5) {
                    val remaining = preview.alarmCount - 5
                    Text(
                        pluralStringResource(R.plurals.settings_more_items, remaining, remaining),
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            if (preview.canImport) {
                TextButton(onClick = onImport) { Text(stringResource(R.string.settings_import_disabled)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun BackupImportPreviewDialog(
    pendingImport: PendingBackupImport,
    importEnabledAsDisabled: Boolean,
    onImportEnabledAsDisabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onImport: (BackupImportMode) -> Unit
) {
    val preview = pendingImport.preview
    val unknownAppVersion = stringResource(R.string.settings_unknown)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (preview.canImport) Icons.Default.Backup else Icons.Default.Warning,
                contentDescription = null,
                tint = if (preview.canImport) MaterialTheme.colorScheme.primary else AccentRed
            )
        },
        title = { Text(stringResource(R.string.settings_review_backup)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_review_backup_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = preview.compatibilityStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview.canImport) TextPrimary else AccentRed
                )
                Text(
                    text = stringResource(
                        R.string.settings_backup_version,
                        preview.version,
                        preview.appVersion.ifBlank { unknownAppVersion }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatBackupExportedAt(preview.exportedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = stringResource(
                        R.string.settings_backup_alarm_counts,
                        preview.alarmCount,
                        preview.enabledAlarmCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (preview.invalidAlarmCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.settings_backup_invalid_rows,
                            preview.invalidAlarmCount,
                            preview.invalidAlarmCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = SnoozeYellow
                    )
                }
                Text(
                    text = if (preview.settingsIncluded) {
                        stringResource(R.string.settings_global_settings_restored)
                    } else {
                        stringResource(R.string.settings_global_settings_missing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (preview.privateDataCategories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_private_values_detected),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    preview.privateDataCategories.forEach { category ->
                        Text(
                            text = stringResource(R.string.settings_list_item, category),
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
                            text = stringResource(R.string.settings_keep_restored_disabled),
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
                        Text(stringResource(R.string.settings_append_alarms))
                    }
                    TextButton(onClick = { onImport(BackupImportMode.Replace) }) {
                        Text(stringResource(R.string.settings_replace_alarms))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(if (preview.canImport) R.string.cancel else R.string.settings_close)
                )
            }
        }
    )
}

@Composable
private fun formatBackupExportedAt(exportedAt: Long): String {
    if (exportedAt <= 0L) return stringResource(R.string.settings_export_time_unknown)
    val locale = LocalConfiguration.current.locales[0]
    val formatted = runCatching {
        DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(exportedAt))
    }.getOrNull()
    return if (formatted == null) {
        stringResource(R.string.settings_export_time_unknown)
    } else {
        stringResource(R.string.settings_exported_at, formatted)
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
                text = stringResource(
                    if (encrypted) R.string.settings_encrypted_backup_private
                    else R.string.settings_plain_backup_private
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (encrypted) {
                        stringResource(R.string.settings_private_values_encrypted)
                    } else {
                        stringResource(R.string.settings_private_values_plain)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                warning.categories.forEach { category ->
                    Text(
                        text = stringResource(R.string.settings_list_item, category),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (encrypted) {
                        stringResource(R.string.settings_encrypted_backup_warning)
                    } else {
                        stringResource(R.string.settings_plain_backup_warning)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    stringResource(
                        if (encrypted) R.string.settings_export_encrypted_backup
                        else R.string.settings_export_plain_backup
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

@Composable
private fun dashboardSummary(state: SettingsUiState): String {
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
private fun incidentLabel(type: String?): String {
    val unknown = stringResource(R.string.settings_unknown_code)
    val token = type.orEmpty().ifBlank { unknown }
    return token
        .replace('_', ' ')
        .lowercase(Locale.US)
        .replaceFirstChar { it.titlecase(Locale.US) }
}

@Composable
private fun formatIncidentTimestamp(eventAt: Long?, use24Hour: Boolean): String {
    if (eventAt == null || eventAt <= 0L) return stringResource(R.string.settings_time_unknown)
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (use24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(eventAt))
}

@Composable
private fun formatIncidentElapsed(elapsedMs: Long?): String {
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
private fun wakeReadinessSummary(state: SettingsUiState): String {
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

private fun requiresLocalNetworkAccess(state: SettingsUiState): Boolean {
    if (!LocalNetworkPermission.isRuntimeRequired()) return false
    return state.settings.hueBridgeIp.isNotBlank() ||
        LocalNetworkPermission.isLikelyLocalEndpoint(state.settings.webhookUrl)
}

@Composable
private fun formatWebhookDeliveryStatus(settings: AppSettings): String? {
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
private fun formatSeconds(totalSeconds: Int): String {
    if (totalSeconds == 0) return stringResource(R.string.settings_off)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return when {
        m == 0 -> stringResource(R.string.settings_seconds_short, s)
        s == 0 -> stringResource(R.string.settings_minutes_compact, m)
        else -> stringResource(R.string.settings_minutes_seconds_compact, m, s)
    }
}
