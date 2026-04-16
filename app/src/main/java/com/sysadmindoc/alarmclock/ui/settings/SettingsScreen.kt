package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
                    value = if (state.settings.defaultGradualVolume == 0) {
                        "Off"
                    } else {
                        "${state.settings.defaultGradualVolume / 60}m ${state.settings.defaultGradualVolume % 60}s"
                    },
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
                                Text(
                                    if (seconds == 0) "Off" else "${seconds / 60}m ${seconds % 60}s"
                                )
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
            BackupRestoreSection(viewModel)

            AppSectionTitle(
                title = "Utilities",
                description = "Quick access to companion tools that round out the app."
            )
            UtilityShortcutCard(
                icon = Icons.Default.BarChart,
                title = "Alarm statistics",
                description = "Review streaks, response times, and habits over time.",
                onClick = onNavigateToStats
            )
            UtilityShortcutCard(
                icon = Icons.Default.Speed,
                title = "Stopwatch",
                description = "Track laps with best and worst splits highlighted.",
                onClick = onNavigateToStopwatch
            )
            UtilityShortcutCard(
                icon = Icons.Default.Bedtime,
                title = "Bedtime",
                description = "Set a sleep goal and keep your routine feeling intentional.",
                onClick = onNavigateToBedtime
            )

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
                if (enabled && settings.vacationStartMillis > 0 && settings.vacationEndMillis > 0) {
                    viewModel.setVacationMode(true, settings.vacationStartMillis, settings.vacationEndMillis)
                } else {
                    viewModel.setVacationMode(false)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            onCheckedChange = onToggle,
            colors = appSwitchColors()
        )
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    value: String,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
            }
        }
        if (!supportingText.isNullOrBlank()) {
            Text(supportingText, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsInfo(label: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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

        OutlinedTextField(
            value = state.settings.webhookUrl,
            onValueChange = viewModel::updateWebhookUrl,
            label = { Text("Webhook URL") },
            placeholder = { Text("https://example.com/hook") },
            enabled = state.settings.webhookEnabled,
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )

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
        OutlinedTextField(
            value = state.settings.holidayCountryCode,
            onValueChange = viewModel::updateHolidayCountryCode,
            label = { Text("Country code") },
            placeholder = { Text("US, GB, DE...") },
            enabled = state.settings.holidayAutoSkipEnabled,
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )
        Text(
            text = "Holiday data comes from Nager.Date and is cached locally for a week.",
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
        OutlinedTextField(
            value = state.settings.hueBridgeIp,
            onValueChange = viewModel::updateHueBridgeIp,
            label = { Text("Bridge IP address") },
            placeholder = { Text("192.168.1.100") },
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )
        OutlinedTextField(
            value = state.settings.hueApiKey,
            onValueChange = viewModel::updateHueApiKey,
            label = { Text("Hue API key") },
            placeholder = { Text("Press the Hue bridge button first") },
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )
        OutlinedTextField(
            value = state.settings.hueLightIds,
            onValueChange = viewModel::updateHueLightIds,
            label = { Text("Light IDs") },
            placeholder = { Text("1,2,3") },
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
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

@Composable
private fun BackupRestoreSection(viewModel: SettingsViewModel) {
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

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
            text = "Backups include alarms and global settings in a JSON file.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
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
    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
