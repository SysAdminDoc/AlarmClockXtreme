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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.ui.semantics.Role
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
import com.sysadmindoc.alarmclock.data.health.HealthConnectAvailability
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.domain.BreathingPattern
import com.sysadmindoc.alarmclock.domain.ChronotypeEstimator
import com.sysadmindoc.alarmclock.domain.SleepNoisePreset
import com.sysadmindoc.alarmclock.domain.formatBreathingDuration
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BedtimeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    var breathingPattern by remember { mutableStateOf(BreathingPattern.FOUR_SEVEN_EIGHT) }
    var breathingElapsedSeconds by remember { mutableStateOf(0) }
    var breathingRunning by remember { mutableStateOf(false) }
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
                title = "Bedtime",
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
                        Icon(Icons.Default.Close, contentDescription = "Close bedtime", tint = TextMuted)
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
                            contentDescription = "Bedtime reminder",
                            tint = if (state.isEnabled) DismissGreen else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Bedtime reminder",
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
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = viewModel::toggleEnabled,
                        colors = appSwitchColors()
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
                            contentDescription = "Bedtime DND",
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
                                text = "Bedtime DND",
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
                                    label = "Alarms only",
                                    icon = Icons.Default.AlarmOff,
                                    color = TextMuted
                                )
                            }
                            state.bedtimeDndError?.let { error ->
                                Text(
                                    text = "DND sync needs attention: $error",
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
                                    Text("Grant DND access", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Switch(
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
                            text = "Battery at ${state.batteryPercent}% — plug in before sleep to avoid alarm failure",
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
                                    text = "Delay tonight's bedtime reminder",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 28.dp)
                                )
                            }
                        }
                        if (state.stayUpLateActive) {
                            TextButton(onClick = { viewModel.clearStayUpLate() }) {
                                Text("Cancel")
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
                    title = "Tonight's sleep window",
                    description = "Use your current goal and next alarm to see when sleep should start."
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
                    text = "Current sleep target",
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
                            title = "Bedtime",
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
                            title = "Wake",
                            value = state.wakeTimeFormatted.ifBlank { "--:--" },
                            icon = Icons.Default.WbSunny,
                            accent = SnoozeYellow,
                            modifier = Modifier.width(152.dp),
                            helper = if (state.wakeTimeFormatted.isBlank()) "No alarm linked" else "Next alarm"
                        )
                    }
                    item {
                        BedtimeMetricCard(
                            title = "Reminder",
                            value = "${state.reminderMinutesBefore} min",
                            icon = Icons.Default.Schedule,
                            accent = DismissGreen,
                            modifier = Modifier.width(152.dp),
                            helper = if (state.isEnabled) "Before bedtime" else "Turn on above"
                        )
                    }
                    item {
                        BedtimeMetricCard(
                            title = "Room",
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
                        title = "No upcoming alarm",
                        description = "Set an alarm to unlock suggested bedtimes and cycle-friendly options."
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
                            contentDescription = "Suggestion",
                            tint = DismissGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Suggested bedtime",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Aim for ${state.suggestedBedtime} to protect ${state.sleepDurationFormatted} before your next alarm.",
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
                    title = "Sleep goal and reminder timing",
                    description = "Adjust in 30-minute steps and pick how early you want the reminder."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lowerGoal = state.sleepGoalHours * 60 + state.sleepGoalMinutes - 30
                    val upperGoal = state.sleepGoalHours * 60 + state.sleepGoalMinutes + 30

                    BedtimeAdjusterButton(
                        label = "Less",
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
                            text = "Sleep target",
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
                        label = "More",
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
                    text = "Reminder lead time",
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
                    title = "Cycle-friendly options",
                    description = "Based on 90-minute sleep cycles and a short fall-asleep buffer."
                )

                if (state.sleepCycleOptions.isEmpty()) {
                    AppEmptyState(
                        icon = Icons.Default.NightsStay,
                        title = "Waiting for your next alarm",
                        description = "Once an alarm is set, bedtime will suggest easier cycle-aligned sleep times."
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
                    Text("Save bedtime", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Keep current", color = TextSecondary)
                }
            },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStatusChip(
                        label = "Wind-down target",
                        icon = Icons.Default.Bedtime,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Choose bedtime", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
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

private data class SleepSound(
    val label: String,
    val icon: ImageVector,
    val preset: SleepNoisePreset
)

private val SLEEP_SOUNDS = listOf(
    SleepSound("White Noise", Icons.Default.Waves, SleepNoisePreset.WHITE),
    SleepSound("Rain", Icons.Default.WaterDrop, SleepNoisePreset.RAIN),
    SleepSound("Brown Noise", Icons.Default.GraphicEq, SleepNoisePreset.BROWN),
    SleepSound("Ocean", Icons.Default.Sailing, SleepNoisePreset.OCEAN),
    SleepSound("Fan", Icons.Default.Air, SleepNoisePreset.FAN),
    SleepSound("Pink Noise", Icons.Default.GraphicEq, SleepNoisePreset.PINK),
    SleepSound("Violet Noise", Icons.Default.Waves, SleepNoisePreset.VIOLET),
)

@Composable
private fun ChronotypeSection(
    state: BedtimeUiState,
    onAnswer: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = state.chronotypeComplete
    ) {
        AppSectionTitle(
            title = "Chronotype estimate",
            description = if (state.chronotypeComplete) {
                "Your natural timing preference is folded into the sleep target."
            } else {
                "Estimate your morning/evening tilt for a calmer target window."
            }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = state.chronotypeCategoryLabel,
                icon = Icons.Default.Lightbulb,
                color = if (state.chronotypeComplete) MaterialTheme.colorScheme.primary else TextMuted
            )
            AppStatusChip(
                label = state.chronotypeTimingLabel,
                icon = Icons.Default.Schedule,
                color = if (state.chronotypeComplete) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = "${state.chronotypeAnsweredCount}/${ChronotypeEstimator.QUESTION_COUNT}",
                icon = Icons.Default.CheckCircle,
                color = if (state.chronotypeComplete) DismissGreen else TextMuted
            )
        }

        Text(
            text = state.chronotypeHelper,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        ChronotypeEstimator.questions.forEachIndexed { questionIndex, question ->
            if (questionIndex > 0) {
                HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = question.prompt,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.options.forEachIndexed { answerIndex, label ->
                        AppFilterChip(
                            label = label,
                            selected = state.chronotypeAnswers.getOrNull(questionIndex) == answerIndex,
                            onClick = { onAnswer(questionIndex, answerIndex) },
                            selectionSemantics = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreathingExerciseSection(
    pattern: BreathingPattern,
    elapsedSeconds: Int,
    running: Boolean,
    onPatternSelected: (BreathingPattern) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = pattern.phaseAt(elapsedSeconds)
    val remainingSessionSeconds = (pattern.totalSeconds - elapsedSeconds).coerceAtLeast(0)

    AppSurfaceCard(
        modifier = modifier,
        highlighted = running
    ) {
        AppSectionTitle(
            title = "Guided breathing",
            description = if (running) {
                "Follow the count and keep the phone nearby while you settle down."
            } else {
                "Run a short 4-7-8 or box-breathing reset before sleep."
            }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BreathingPattern.entries.forEach { option ->
                AppFilterChip(
                    label = option.displayName,
                    selected = option == pattern,
                    onClick = { onPatternSelected(option) },
                    selectionSemantics = true
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard.copy(alpha = 0.72f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = "Cycle ${phase.cycleNumber}/${phase.cycleCount}",
                    icon = Icons.Default.Schedule,
                    color = if (phase.completed) DismissGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = phase.label,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (phase.completed) "Done" else "${phase.remainingSeconds}",
                    color = if (phase.completed) DismissGreen else SnoozeYellow,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = phase.cue,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${formatBreathingDuration(remainingSessionSeconds)} left",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppFilterChip(
                label = if (running) "Pause" else if (phase.completed) "Restart" else "Start",
                selected = running,
                onClick = onToggleRunning,
                selectionSemantics = false,
                accessibilityLabel = if (running) "Pause guided breathing" else "Start guided breathing"
            )
            TextButton(onClick = onReset) {
                Text("Reset", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SonarSleepTrackingSection(
    state: BedtimeUiState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = state.sonarTrackingActive
    ) {
        AppSectionTitle(
            title = "Sonar sleep tracking",
            description = if (state.sonarTrackingActive) {
                "Experimental overnight movement monitoring is running locally."
            } else {
                "Start a local ultrasonic movement session from Bedtime when you want extra context."
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = if (state.sonarTrackingActive) "Active" else "Off",
                        icon = if (state.sonarTrackingActive) Icons.Default.CheckCircle else Icons.Default.GraphicEq,
                        color = if (state.sonarTrackingActive) DismissGreen else TextMuted
                    )
                    AppStatusChip(
                        label = "No audio saved",
                        icon = Icons.Default.NightsStay,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = state.sonarTrackingStatus,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.sonarLastSessionLabel.isNotBlank()) {
                    Text(
                        text = state.sonarLastSessionLabel,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.snoreTimeline.isNotEmpty()) {
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.snoreTimeline.forEach { event ->
                            SnoreTimelineRow(event = event)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            AppFilterChip(
                label = if (state.sonarTrackingActive) "Stop" else "Start",
                selected = state.sonarTrackingActive,
                onClick = onToggle,
                selectionSemantics = false,
                accessibilityLabel = if (state.sonarTrackingActive) {
                    "Stop sleep-motion tracking"
                } else {
                    "Start sleep-motion tracking"
                }
            )
        }
    }
}

@Composable
private fun SnoreTimelineRow(event: SnoreTimelineItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = SnoozeYellow.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = SnoozeYellow,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${event.timeLabel} - ${event.durationLabel}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = event.intensityLabel,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HealthConnectSleepSection(
    summary: HealthConnectSleepSummary,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = summary.permissionGranted && summary.hasRecentSession
    ) {
        AppSectionTitle(
            title = "Health Connect sleep",
            description = when {
                summary.availability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
                    "Update Health Connect before recent sleep sessions can appear here."
                summary.availability == HealthConnectAvailability.UNAVAILABLE ->
                    "Health Connect is not available on this device."
                !summary.permissionGranted ->
                    "Grant READ_SLEEP in Settings to fold recent sessions into bedtime planning."
                summary.hasRecentSession ->
                    "Recent sessions stay local and help compare your target with actual sleep."
                else ->
                    "READ_SLEEP is granted, but no recent sleep sessions were found in the last 14 days."
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppStatusChip(
                label = if (summary.permissionGranted) "READ_SLEEP granted" else "Permission needed",
                icon = if (summary.permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                color = if (summary.permissionGranted) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = "${summary.sessionsRead} sessions",
                icon = Icons.Default.Bedtime,
                color = if (summary.sessionsRead > 0) MaterialTheme.colorScheme.primary else TextMuted
            )
        }
        if (summary.hasRecentSession) {
            Text(
                text = "Last session: ${formatSleepMinutes(summary.lastSessionDurationMinutes)}",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleepStageChip("Light", summary.lightStageMinutes)
                SleepStageChip("Deep", summary.deepStageMinutes)
                SleepStageChip("REM", summary.remStageMinutes)
                SleepStageChip("Awake", summary.awakeStageMinutes)
            }
        }
        summary.errorMessage?.let { error ->
            AppInlineNotice(
                title = "Health Connect needs attention",
                message = error,
                icon = Icons.Default.Warning,
                color = SnoozeYellow
            )
        }
    }
}

@Composable
private fun SleepStageChip(label: String, minutes: Long) {
    AppStatusChip(
        label = "$label ${formatSleepMinutes(minutes)}",
        icon = Icons.Default.NightsStay,
        color = if (minutes > 0) MaterialTheme.colorScheme.primary else TextMuted
    )
}

private fun formatSleepMinutes(minutes: Long?): String {
    val value = minutes ?: return "0m"
    val hours = value / 60
    val mins = value % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

@Composable
private fun SleepSoundsSection(
    state: BedtimeUiState,
    viewModel: BedtimeViewModel,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier) {
        AppSectionTitle(
            title = "Sleep sounds",
            description = "Continuous procedural soundscapes with no looping artifacts."
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(SLEEP_SOUNDS) { _, sound ->
                val isActive = state.activeSoundKey == sound.preset.key

                Card(
                    modifier = Modifier
                        .size(width = 112.dp, height = 108.dp)
                        .clickable(role = Role.Button) {
                            if (isActive) viewModel.stopSound()
                            else viewModel.playSound(sound.preset)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            SurfaceCard
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = sound.icon,
                            contentDescription = sound.label,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = sound.label,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isActive) "Playing" else "Tap to preview",
                                color = if (isActive) MaterialTheme.colorScheme.primary else TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))

        Text(
            text = "Fade out after",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(0, 15, 30, 45, 60).forEach { minutes ->
                AppFilterChip(
                    label = if (minutes == 0) "Never" else "$minutes min",
                    selected = state.sleepSoundFadeMinutes == minutes,
                    onClick = { viewModel.setSleepSoundFade(minutes) },
                    selectionSemantics = true,
                )
            }
        }

        // v1.5.0: Final-taper duration. Until this pass the fade was hard-coded
        // to 60s; users with deeper-sleep routines asked for a longer slide.
        Text(
            text = "Final taper length",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tapers = listOf(15, 30, 60, 120, 300, 600)
            tapers.forEach { seconds ->
                AppFilterChip(
                    label = when {
                        seconds < 60 -> "${seconds}s"
                        seconds % 60 == 0 -> "${seconds / 60} min"
                        else -> "${seconds}s"
                    },
                    selected = state.sleepSoundFadeSeconds == seconds,
                    onClick = {
                        // The VM setter only exists for the underlying DataStore
                        // path; adjust our local UI state and persist directly.
                        viewModel.setSleepSoundFadeSeconds(seconds)
                    },
                    selectionSemantics = true,
                )
            }
        }

        if (state.activeSoundKey.isNotBlank()) {
            TextButton(
                onClick = viewModel::stopSound,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Stop sound", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun BedtimeMetricCard(
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
private fun SleepCycleOptionRow(index: Int, option: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard.copy(alpha = if (index == 0) 0.82f else 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppStatusChip(
                label = if (index == 0) "Best match" else "${index + 1}",
                color = if (index == 0) DismissGreen else MaterialTheme.colorScheme.primary
            )
            Text(
                text = option,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
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
private fun WindDownChecklistSection(
    state: BedtimeUiState,
    onToggle: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier) {
        AppSectionTitle(
            title = "Wind-down checklist",
            description = "Tick each step as you settle in for the night.",
            action = {
                AppStatusChip(
                    label = "${state.bedtimeChecklistDone.size}/${state.bedtimeChecklist.size} done",
                    icon = Icons.Default.CheckCircle,
                    color = if (state.bedtimeChecklistDone.isEmpty()) TextMuted else DismissGreen
                )
            }
        )

        state.bedtimeChecklist.forEachIndexed { index, item ->
            val done = index in state.bedtimeChecklistDone
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Checkbox) { onToggle(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (done) DismissGreen.copy(alpha = 0.09f) else SurfaceCard.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Bedtime,
                        contentDescription = if (done) "Completed" else "Not done",
                        tint = if (done) DismissGreen else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = item,
                        color = if (done) TextMuted else TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    AppStatusChip(
                        label = if (done) "Done" else "Up next",
                        color = if (done) DismissGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (state.bedtimeChecklistDone.isNotEmpty()) {
            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Reset checklist", color = TextSecondary)
            }
        }
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
