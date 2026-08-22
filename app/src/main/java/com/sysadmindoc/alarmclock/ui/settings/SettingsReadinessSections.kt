package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.text.font.FontWeight
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
import com.sysadmindoc.alarmclock.data.support.SupportExportFile
import com.sysadmindoc.alarmclock.domain.OnCallDndOverride
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
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun IncidentTimelineSection(
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
internal fun SettingsOverviewRow(state: SettingsUiState) {
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
internal fun SettingsOverviewTile(
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
internal fun WakeReadinessSection(
    state: SettingsUiState,
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
        // Only counts against readiness while the risk is actually present, so
        // it surfaces as a warning without permanently changing the X/Y count.
        if (state.alarmMutedByDnd) add(false)
        if (standbyRowVisible) add(standbyReady)
        if (state.guardianReadiness.hasEnabledAlarms) {
            add(!state.guardianReadiness.needsUserAction)
        }
    }
    val readyCount = checks.count { it }
    val total = checks.size
    val allReady = readyCount == total

    AppSurfaceCard(highlighted = false) {
        AppSectionTitle(
            title = stringResource(R.string.settings_wake_readiness),
            action = {
                TextButton(onClick = onOpenOnboardingChecklist) {
                    Text(stringResource(R.string.settings_review))
                }
            }
        )
        Text(
            text = stringResource(R.string.settings_ready_count, readyCount, total),
            color = if (allReady) DismissGreen else SnoozeYellow,
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else readyCount.toFloat() / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = if (allReady) DismissGreen else SnoozeYellow,
            trackColor = TextMuted.copy(alpha = 0.18f)
        )
    }
}

@Composable
internal fun OnCallModeSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val policyAccessGranted = OnCallDndOverride.isPolicyAccessGranted(context)
    AppSurfaceCard(highlighted = state.settings.onCallModeEnabled) {
        AppSectionTitle(
            title = stringResource(R.string.settings_on_call_mode),
            description = stringResource(R.string.settings_on_call_mode_description),
            action = {
                AppStatusChip(
                    label = stringResource(
                        if (state.settings.onCallModeEnabled) R.string.settings_active else R.string.settings_off
                    ),
                    icon = Icons.Default.NotificationsActive,
                    color = if (state.settings.onCallModeEnabled) SnoozeYellow else TextMuted
                )
            }
        )
        SettingsToggle(
            label = stringResource(R.string.settings_on_call_mode),
            supportingText = stringResource(R.string.settings_on_call_mode_toggle_description),
            checked = state.settings.onCallModeEnabled,
            onToggle = viewModel::toggleOnCallMode
        )
        if (state.settings.onCallModeEnabled && !policyAccessGranted) {
            AppInlineNotice(
                title = stringResource(R.string.settings_on_call_access_needed),
                message = stringResource(R.string.settings_on_call_access_description),
                icon = Icons.Default.Warning,
                color = SnoozeYellow
            )
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_open_dnd))
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
internal fun PauseAlarmsSection(state: SettingsUiState, viewModel: SettingsViewModel) {
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
internal fun VacationModeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
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
internal fun BatteryOptimizationSection(state: SettingsUiState, viewModel: SettingsViewModel) {
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

        if (!state.isIgnoringBatteryOptimizations || state.needsBatteryGuidance) {
            Button(
                onClick = viewModel::requestBatteryExemption,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (state.needsBatteryGuidance) {
                        stringResource(
                            R.string.settings_open_manufacturer_battery,
                            state.manufacturerName
                        )
                    } else {
                        stringResource(R.string.settings_open_battery)
                    }
                )
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
