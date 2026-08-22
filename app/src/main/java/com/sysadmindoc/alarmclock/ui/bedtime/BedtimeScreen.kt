@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sysadmindoc.alarmclock.ui.bedtime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.BreathingPattern
import com.sysadmindoc.alarmclock.domain.JetLagDirection
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.BlueLight
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BedtimeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showJetLagWakePicker by rememberSaveable { mutableStateOf(false) }
    var breathingPattern by rememberSaveable { mutableStateOf(BreathingPattern.FOUR_SEVEN_EIGHT) }
    var breathingElapsedSeconds by rememberSaveable { mutableStateOf(0) }
    var breathingRunning by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sonarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startSonarTracking()
        } else {
            viewModel.noteSonarPermissionDenied()
        }
    }
    val toggleSonarTracking = {
        if (state.sonarTrackingActive) {
            viewModel.stopSonarTracking()
        } else if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startSonarTracking()
        } else {
            sonarPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBedtimeDndStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(breathingRunning, breathingPattern) {
        while (breathingRunning && breathingElapsedSeconds < breathingPattern.totalSeconds) {
            delay(1_000L)
            breathingElapsedSeconds = (breathingElapsedSeconds + 1)
                .coerceAtMost(breathingPattern.totalSeconds)
        }
        if (breathingElapsedSeconds >= breathingPattern.totalSeconds) {
            breathingRunning = false
        }
    }

    val summaryLine = when {
        state.wakeTimeFormatted.isNotBlank() -> {
            "Plan around your ${state.wakeTimeFormatted} alarm and protect ${state.sleepDurationFormatted} of sleep."
        }
        state.isEnabled -> "Your wind-down reminder is set. Add an alarm to get a recommended bedtime."
        else -> "Build a calmer night routine with a reminder, a target, and gentler wind-down cues."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            AlarmClockHeroHeader(
                title = stringResource(R.string.shortcut_bedtime),
                subtitle = summaryLine,
                overline = "Sleep planning",
                badge = {
                    AppStatusChip(
                        label = if (state.isEnabled) "Reminder on" else "Reminder off",
                        icon = Icons.Default.Bedtime,
                        color = if (state.isEnabled) DismissGreen else TextMuted
                    )
                    AppStatusChip(
                        label = state.sleepDurationFormatted,
                        icon = Icons.Default.NightsStay,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppStatusChip(
                        label = state.wakeTimeFormatted.takeIf { it.isNotBlank() } ?: "No alarm linked",
                        icon = if (state.wakeTimeFormatted.isNotBlank()) Icons.Default.WbSunny else Icons.Default.AlarmOff,
                        color = if (state.wakeTimeFormatted.isNotBlank()) SnoozeYellow else TextMuted
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.bedtime_close_bedtime), tint = TextMuted)
                    }
                }
            )
        }

        item {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                highlighted = state.isEnabled
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = stringResource(R.string.bedtime_bedtime_reminder),
                            tint = if (state.isEnabled) DismissGreen else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.bedtime_bedtime_reminder),
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (state.isEnabled) {
                                    "Remind me ${state.reminderMinutesBefore} minutes before ${state.bedtimeFormatted}."
                                } else {
                                    "Get a nudge before your target bedtime so nights feel less rushed."
                                },
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = state.nextAlarmTime,
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppStatusChip(
                                    label = if (state.isEnabled) "${state.reminderMinutesBefore} min early" else "Optional",
                                    icon = Icons.Default.Schedule,
                                    color = if (state.isEnabled) DismissGreen else TextMuted
                                )
                                AppStatusChip(
                                    label = if (state.wakeTimeFormatted.isNotBlank()) "Linked to alarm" else "Needs alarm",
                                    icon = if (state.wakeTimeFormatted.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AlarmOff,
                                    color = if (state.wakeTimeFormatted.isNotBlank()) MaterialTheme.colorScheme.primary else SnoozeYellow
                                )
                            }
                        }
                    }
                    // Named so TalkBack reads "Bedtime reminder, switch, on"
                    // instead of an unlabelled control.
                    val reminderSwitchLabel = stringResource(R.string.bedtime_bedtime_reminder)
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = viewModel::toggleEnabled,
                        colors = appSwitchColors(),
                        modifier = Modifier.semantics {
                            contentDescription = reminderSwitchLabel
                        }
                    )
                }
            }
        }

        item {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                highlighted = state.bedtimeDndEnabled && state.bedtimeDndAccessGranted
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = stringResource(R.string.bedtime_dnd_rule_type),
                            tint = when {
                                !state.bedtimeDndAccessGranted -> SnoozeYellow
                                state.bedtimeDndActive -> DismissGreen
                                state.bedtimeDndEnabled -> MaterialTheme.colorScheme.primary
                                else -> TextMuted
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.bedtime_dnd_rule_type),
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when {
                                    !state.bedtimeDndAccessGranted ->
                                        "Grant DND access so AlarmClockXtreme can own an alarms-only sleep rule."
                                    state.bedtimeDndActive ->
                                        "Alarms-only mode is active for your sleep window."
                                    state.bedtimeDndEnabled ->
                                        "Silences interruptions from bedtime until your next alarm."
                                    else ->
                                        "Create an app-owned alarms-only rule for your sleep window."
                                },
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppStatusChip(
                                    label = state.bedtimeDndStatus,
                                    icon = if (state.bedtimeDndActive) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    color = when {
                                        !state.bedtimeDndAccessGranted -> SnoozeYellow
                                        state.bedtimeDndActive -> DismissGreen
                                        state.bedtimeDndEnabled -> MaterialTheme.colorScheme.primary
                                        else -> TextMuted
                                    }
                                )
                                AppStatusChip(
                                    label = stringResource(R.string.bedtime_alarms_only),
                                    icon = Icons.Default.AlarmOff,
                                    color = TextMuted
                                )
                            }
                            state.bedtimeDndError?.let { error ->
                                Text(
                                    text = stringResource(R.string.bedtime_dnd_sync_error, error),
                                    color = SnoozeYellow,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!state.bedtimeDndAccessGranted) {
                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                ) {
                                    Text(stringResource(R.string.bedtime_grant_dnd_access), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    val dndSwitchLabel = stringResource(R.string.bedtime_do_not_disturb_at_bedtime)
                    Switch(
                        modifier = Modifier.semantics {
                            contentDescription = dndSwitchLabel
                        },
                        checked = state.bedtimeDndEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleBedtimeDnd(enabled)
                            if (enabled && !state.bedtimeDndAccessGranted) {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        colors = appSwitchColors()
                    )
                }
            }
        }

        if (state.batteryLow && state.isEnabled) {
            item {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    highlighted = true
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SnoozeYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.bedtime_battery_warning, state.batteryPercent),
                            color = SnoozeYellow,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (state.isEnabled) {
            item {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    highlighted = state.stayUpLateActive
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AlarmOff,
                                    contentDescription = null,
                                    tint = if (state.stayUpLateActive) SnoozeYellow else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (state.stayUpLateActive)
                                        "Staying up late until ${state.stayUpLateLabel}"
                                    else "Stay up late tonight",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (!state.stayUpLateActive) {
                                Text(
                                    text = stringResource(R.string.bedtime_delay_tonight_s_bedtime_reminder),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 28.dp)
                                )
                            }
                        }
                        if (state.stayUpLateActive) {
                            TextButton(onClick = { viewModel.clearStayUpLate() }) {
                                Text(stringResource(R.string.alarm_list_cancel))
                            }
                        }
                    }
                    if (!state.stayUpLateActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3).forEach { hours ->
                                AppFilterChip(
                                    selected = false,
                                    onClick = { viewModel.stayUpLate(hours) },
                                    label = "+${hours}h"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AppSectionTitle(
                    title = stringResource(R.string.bedtime_tonight_s_sleep_window),
                    description = stringResource(R.string.bedtime_use_current_goal_next_alarm)
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SleepArc(
                        bedtimeHour = state.bedtimeHour,
                        bedtimeMinute = state.bedtimeMinute,
                        sleepHours = state.sleepGoalHours,
                        sleepMinutes = state.sleepGoalMinutes,
                        modifier = Modifier.size(240.dp)
                    )
                }

                Text(
                    text = state.sleepDurationFormatted,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.bedtime_current_sleep_target),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        BedtimeMetricCard(
                            title = stringResource(R.string.shortcut_bedtime),
                            value = state.bedtimeFormatted,
                            icon = Icons.Default.NightsStay,
                            accent = BlueLight,
                            modifier = Modifier.width(152.dp),
                            helper = "Tap to edit",
                            onClick = { showTimePicker = true }
                        )
                    }
                    item {
                        BedtimeMetricCard(
                            title = stringResource(R.string.bedtime_wake),
                            value = state.wakeTimeFormatted.ifBlank { "--:--" },
                            icon = Icons.Default.WbSunny,
                            accent = SnoozeYellow,
                            modifier = Modifier.width(152.dp),
                            helper = if (state.wakeTimeFormatted.isBlank()) "No alarm linked" else "Next alarm"
                        )
                    }
                    item {
                        BedtimeMetricCard(
                            title = stringResource(R.string.bedtime_reminder),
                            value = "${state.reminderMinutesBefore} min",
                            icon = Icons.Default.Schedule,
                            accent = DismissGreen,
                            modifier = Modifier.width(152.dp),
                            helper = if (state.isEnabled) "Before bedtime" else "Turn on above"
                        )
                    }
                    item {
                        BedtimeMetricCard(
                            title = stringResource(R.string.bedtime_room),
                            value = state.noiseBaselineLabel,
                            icon = Icons.Default.Air,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(176.dp),
                            helper = state.noiseBaselineHelper
                        )
                    }
                }

                if (state.wakeTimeFormatted.isBlank()) {
                    AppEmptyState(
                        icon = Icons.Default.AlarmOff,
                        title = stringResource(R.string.bedtime_no_upcoming_alarm),
                        description = stringResource(R.string.bedtime_set_alarm_unlock_suggested_bedtimes)
                    )
                }
            }
        }

        if (state.suggestedBedtime.isNotBlank()) {
            item {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    highlighted = true
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = stringResource(R.string.bedtime_suggestion),
                            tint = DismissGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.bedtime_suggested_bedtime),
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.bedtime_aim_for_bedtime,
                                    state.suggestedBedtime,
                                    state.sleepDurationFormatted
                                ),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            ChronotypeSection(
                state = state,
                onAnswer = viewModel::updateChronotypeAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            JetLagPlannerSection(
                state = state,
                onTargetWakeClick = { showJetLagWakePicker = true },
                onDaysChange = viewModel::updateJetLagAdjustmentDays,
                onDirectionChange = viewModel::updateJetLagDirection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.healthConnectEnabled) {
            item {
                HealthConnectSleepSection(
                    summary = state.healthConnectSleepSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        item {
            SonarSleepTrackingSection(
                state = state,
                onToggle = toggleSonarTracking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            BreathingExerciseSection(
                pattern = breathingPattern,
                elapsedSeconds = breathingElapsedSeconds,
                running = breathingRunning,
                onPatternSelected = { selected ->
                    breathingPattern = selected
                    breathingElapsedSeconds = 0
                    breathingRunning = false
                },
                onToggleRunning = {
                    if (breathingElapsedSeconds >= breathingPattern.totalSeconds) {
                        breathingElapsedSeconds = 0
                    }
                    breathingRunning = !breathingRunning
                },
                onReset = {
                    breathingElapsedSeconds = 0
                    breathingRunning = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AppSectionTitle(
                    title = stringResource(R.string.bedtime_sleep_goal_reminder_timing),
                    description = stringResource(R.string.bedtime_adjust_30_minute_steps_pick)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lowerGoal = state.sleepGoalHours * 60 + state.sleepGoalMinutes - 30
                    val upperGoal = state.sleepGoalHours * 60 + state.sleepGoalMinutes + 30

                    BedtimeAdjusterButton(
                        label = stringResource(R.string.bedtime_less),
                        icon = Icons.Default.Remove,
                        enabled = lowerGoal >= 300,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (lowerGoal >= 300) {
                                viewModel.updateSleepGoal(lowerGoal / 60, lowerGoal % 60)
                            }
                        }
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text(
                            text = state.sleepDurationFormatted,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.bedtime_jetlag_helper_sleep_target),
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        AppStatusChip(
                            label = "30-minute steps",
                            icon = Icons.Default.Schedule,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    BedtimeAdjusterButton(
                        label = stringResource(R.string.bedtime_more),
                        icon = Icons.Default.Add,
                        enabled = upperGoal <= 720,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (upperGoal <= 720) {
                                viewModel.updateSleepGoal(upperGoal / 60, upperGoal % 60)
                            }
                        }
                    )
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))

                Text(
                    text = stringResource(R.string.bedtime_reminder_lead_time),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        AppFilterChip(
                            label = "$minutes min",
                            selected = state.reminderMinutesBefore == minutes,
                            onClick = { viewModel.updateReminderMinutes(minutes) },
                            selectionSemantics = true,
                        )
                    }
                }
            }
        }

        item {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AppSectionTitle(
                    title = stringResource(R.string.bedtime_cycle_friendly_options),
                    description = stringResource(R.string.bedtime_based_90_minute_sleep_cycles)
                )

                if (state.sleepCycleOptions.isEmpty()) {
                    AppEmptyState(
                        icon = Icons.Default.NightsStay,
                        title = stringResource(R.string.bedtime_waiting_next_alarm),
                        description = stringResource(R.string.bedtime_once_alarm_set_bedtime_will)
                    )
                } else {
                    state.sleepCycleOptions.forEachIndexed { index, option ->
                        SleepCycleOptionRow(index = index, option = option)
                    }
                }
            }
        }

        if (state.bedtimeChecklist.isNotEmpty()) {
            item {
                WindDownChecklistSection(
                    state = state,
                    onToggle = viewModel::toggleChecklistItem,
                    onReset = viewModel::resetChecklist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        item {
            PreSleepTagSection(
                state = state,
                onToggle = viewModel::togglePreSleepTag,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            SleepSoundsSection(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.bedtimeHour,
            initialMinute = state.bedtimeMinute,
            is24Hour = state.is24HourFormat
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateBedtime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.bedtime_save_bedtime), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.bedtime_picker_keep_current), color = TextSecondary)
                }
            },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStatusChip(
                        label = stringResource(R.string.bedtime_wind_down_target),
                        icon = Icons.Default.Bedtime,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.bedtime_choose_bedtime), color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(containerColor = SurfaceCard)
                )
            },
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showJetLagWakePicker) {
        val targetWake = state.jetLagTargetWakeMinutes
        val timePickerState = rememberTimePickerState(
            initialHour = targetWake / 60,
            initialMinute = targetWake % 60,
            is24Hour = state.is24HourFormat
        )
        AlertDialog(
            onDismissRequest = { showJetLagWakePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateJetLagTargetWake(timePickerState.hour, timePickerState.minute)
                        showJetLagWakePicker = false
                    }
                ) {
                    Text(stringResource(R.string.bedtime_jetlag_save_target), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJetLagWakePicker = false }) {
                    Text(stringResource(R.string.bedtime_picker_keep_current), color = TextSecondary)
                }
            },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStatusChip(
                        label = stringResource(R.string.bedtime_jetlag_travel_target),
                        icon = Icons.Default.WbSunny,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.bedtime_jetlag_choose_target_wake),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(containerColor = SurfaceCard)
                )
            },
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
internal fun BedtimeMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    helper: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            if (!helper.isNullOrBlank()) {
                Text(
                    text = helper,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SleepArc(
    bedtimeHour: Int,
    bedtimeMinute: Int,
    sleepHours: Int,
    sleepMinutes: Int,
    modifier: Modifier = Modifier
) {
    val sleepTotalMinutes = sleepHours * 60f + sleepMinutes
    val bedtimeTotalMinutes = bedtimeHour * 60f + bedtimeMinute
    val startAngle = ((bedtimeTotalMinutes % 720f) / 720f) * 360f - 90f
    val sweepAngle = (sleepTotalMinutes / 720f) * 360f

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 20f

        drawCircle(
            color = SurfaceCard.copy(alpha = 0.8f),
            radius = radius,
            center = center,
            style = Stroke(width = 12f)
        )

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(BlueLight, AccentBlue, SnoozeYellow, BlueLight),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )

        for (i in 0 until 12) {
            val angle = (i / 12f) * 2 * PI - PI / 2
            val inner = radius - 16f
            val outer = radius - 6f
            drawLine(
                color = TextMuted.copy(alpha = if (i % 3 == 0) 1f else 0.6f),
                start = Offset(
                    center.x + (inner * cos(angle)).toFloat(),
                    center.y + (inner * sin(angle)).toFloat()
                ),
                end = Offset(
                    center.x + (outer * cos(angle)).toFloat(),
                    center.y + (outer * sin(angle)).toFloat()
                ),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f
            )
        }

        val bedAngleRad = (startAngle + 90) * PI / 180
        drawCircle(
            color = BlueLight,
            radius = 8f,
            center = Offset(
                center.x + (radius * cos(bedAngleRad - PI / 2)).toFloat(),
                center.y + (radius * sin(bedAngleRad - PI / 2)).toFloat()
            )
        )

        val wakeAngleRad = (startAngle + sweepAngle + 90) * PI / 180
        drawCircle(
            color = SnoozeYellow,
            radius = 8f,
            center = Offset(
                center.x + (radius * cos(wakeAngleRad - PI / 2)).toFloat(),
                center.y + (radius * sin(wakeAngleRad - PI / 2)).toFloat()
            )
        )
    }
}

@Composable
private fun BedtimeAdjusterButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) SurfaceCard.copy(alpha = 0.78f) else SurfaceCard.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) TextSecondary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (enabled) TextPrimary else TextMuted,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
