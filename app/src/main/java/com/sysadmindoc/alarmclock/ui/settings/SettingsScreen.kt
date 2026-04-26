package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.permissions.PermissionRequestCard
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToStopwatch: () -> Unit = {},
    onNavigateToBedtime: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshBatteryStatus()
    }

    var showDefaultSnoozeMenu by remember { mutableStateOf(false) }
    var showGradualVolumeMenu by remember { mutableStateOf(false) }
    var showAutoSilenceMenu by remember { mutableStateOf(false) }
    var showTemperatureMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
    ) {
        AlarmClockHeroHeader(
            title = "Settings",
            subtitle = "Tune the app once and it stays out of your way. Changes save immediately.",
            overline = "Preferences",
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

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PermissionRequestCard()
            SettingsOverviewRow(state)

            if (state.needsBatteryGuidance || !state.isIgnoringBatteryOptimizations) {
                BatteryOptimizationSection(state, viewModel)
            }

            VacationModeSection(state, viewModel)

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

            IntegrationsSection(state, viewModel)
            HolidaysSection(state, viewModel)
            PhilipsHueSection(state, viewModel)
            PersonalizationSection(state, viewModel)
            BackupRestoreSection(viewModel)

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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsOverviewRow(state: SettingsUiState) {
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
                value = if (state.isIgnoringBatteryOptimizations) "Protected" else "Needs review",
                supporting = if (state.isIgnoringBatteryOptimizations) "Battery rules look good" else "Battery settings can still block alarms",
                icon = if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                accent = if (state.isIgnoringBatteryOptimizations) DismissGreen else SnoozeYellow,
                modifier = Modifier.width(190.dp)
            )
            SettingsOverviewTile(
                title = "Dashboard",
                value = dashboardSummary(state),
                supporting = "Weather and calendar visibility",
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
            shape = RoundedCornerShape(16.dp),
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
                shape = RoundedCornerShape(16.dp)
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
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onToggle
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            SurfaceCard.copy(alpha = 0.28f)
        },
        border = BorderStroke(
            1.dp,
            if (checked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                TextMuted.copy(alpha = 0.14f)
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
                Text(label, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                if (!supportingText.isNullOrBlank()) {
                    Text(supportingText, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.14f))
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
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            value,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (!supportingText.isNullOrBlank()) {
                Text(supportingText, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsInfo(label: String, description: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
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
            supportingText = "Send a JSON payload when alarms fire, snooze, dismiss, or fail.",
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

        // Warn if the user pasted a plain-http endpoint — alarm metadata (label,
        // time) leaks across the network unencrypted otherwise. The Settings hint
        // takes priority over the test result so it's always visible.
        val urlLower = state.settings.webhookUrl.trim().lowercase()
        val plainHttpWarning = state.settings.webhookEnabled &&
                urlLower.startsWith("http://")

        if (plainHttpWarning) {
            Text(
                text = "This URL is plain HTTP. Alarm event payloads will be sent unencrypted — prefer https:// when possible.",
                color = SnoozeYellow,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.webhookTestResult ?: "Use a secure HTTPS endpoint for best reliability.",
                color = when {
                    state.webhookTestResult?.contains("OK") == true -> DismissGreen
                    state.webhookTestResult != null -> AccentRed
                    else -> TextMuted
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(
                onClick = viewModel::testWebhook,
                enabled = state.settings.webhookEnabled && state.settings.webhookUrl.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Test")
            }
        }
    }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.hueTestResult ?: "Run a quick bridge check once the IP and API key are in place.",
                color = when {
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
                enabled = state.settings.hueBridgeIp.isNotBlank() && state.settings.hueApiKey.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Test")
            }
        }
    }
}

/**
 * v1.2.0 personalization controls. Until this audit pass these settings
 * (`accentColor`, `showMotivationalQuotes`, `adaptiveDifficultyEnabled`,
 * `customTypingPhrases`) lived in DataStore + the backup payload but had no
 * UI surface — users couldn't change them.
 */
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
            label = "Cover-to-snooze",
            checked = state.settings.coverToSnoozeEnabled,
            supportingText = "Hold a hand over the proximity sensor for ~1.5 s during an alarm to snooze.",
            onToggle = viewModel::toggleCoverToSnooze
        )

        SettingsToggle(
            label = "Repeat missed alarms",
            checked = state.settings.repeatMissedAlarms,
            supportingText = "If an alarm auto-silences, re-fire it briefly when you unlock within 10 minutes.",
            onToggle = viewModel::toggleRepeatMissed
        )

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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(color)
                        .clickable { onPick(hex) }
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = TextPrimary,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "$label selected",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreSection(viewModel: SettingsViewModel) {
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    var encryptedPassphrase by remember { mutableStateOf("") }
    var encryptedPassphraseConfirm by remember { mutableStateOf("") }
    val encryptedExportEnabled = encryptedPassphrase.isNotBlank() &&
        encryptedPassphrase == encryptedPassphraseConfirm
    val encryptedImportEnabled = encryptedPassphrase.isNotBlank()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    val encryptedExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportEncryptedBackup(it, encryptedPassphrase) } }

    val encryptedImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importEncryptedBackup(it, encryptedPassphrase) } }

    SettingsGroup(
        title = "Backup and restore",
        description = "Keep a portable copy of alarms and app preferences for new devices or peace of mind."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { exportLauncher.launch("alarmclock_backup.json") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Import")
            }
        }

        Text(
            text = "Plain backups include alarms and global settings in a readable JSON file.",
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = appOutlinedTextFieldColors(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = encryptedPassphraseConfirm,
                onValueChange = { encryptedPassphraseConfirm = it },
                label = { Text("Confirm passphrase") },
                placeholder = { Text("Required before encrypted export") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = appOutlinedTextFieldColors(),
                shape = RoundedCornerShape(14.dp)
            )
            if (encryptedPassphraseConfirm.isNotEmpty() && encryptedPassphraseConfirm != encryptedPassphrase) {
                Text(
                    text = "Passphrases do not match. Encrypted import uses only the first field.",
                    color = AccentRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { encryptedExportLauncher.launch("alarmclock_backup_encrypted.json") },
                    enabled = encryptedExportEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Encrypt export")
                }
                OutlinedButton(
                    onClick = { encryptedImportLauncher.launch(arrayOf("application/json", "*/*")) },
                    enabled = encryptedImportEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Decrypt import")
                }
            }
        }
    }

    backupResult?.let { message ->
        AppSurfaceCard(highlighted = !message.contains("failed", ignoreCase = true)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (message.contains("failed", ignoreCase = true)) Icons.Default.Warning else Icons.Default.Backup,
                        contentDescription = null,
                        tint = if (message.contains("failed", ignoreCase = true)) AccentRed else DismissGreen
                    )
                    Text(message, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = viewModel::clearBackupResult) {
                    Icon(Icons.Default.Close, null, tint = TextMuted)
                }
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 2.dp),
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
                        shape = RoundedCornerShape(14.dp)
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

private fun dashboardSummary(state: SettingsUiState): String = when {
    state.settings.showWeatherOnDashboard && state.settings.showCalendarOnDashboard -> "Weather + calendar"
    state.settings.showWeatherOnDashboard -> "Weather only"
    state.settings.showCalendarOnDashboard -> "Calendar only"
    else -> "Minimal"
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
    commitDelayMillis: Long = if (singleLine) 220 else 350
) {
    var draft by rememberSaveable { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

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
        modifier = modifier.onFocusChanged { focusState ->
            val lostFocus = isFocused && !focusState.isFocused
            isFocused = focusState.isFocused
            if (lostFocus && draft != value) {
                onCommit(draft)
            }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions
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
                shape = RoundedCornerShape(14.dp)
            )
            .border(1.dp, TextMuted.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
