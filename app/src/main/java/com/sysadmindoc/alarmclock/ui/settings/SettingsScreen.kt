package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.permissions.PermissionRequestCard
import com.sysadmindoc.alarmclock.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToStopwatch: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshBatteryStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // Top bar with back arrow
        TopAppBar(
            title = { Text("Settings") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceMedium)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Permissions
            PermissionRequestCard()

            // Battery Optimization
            if (state.needsBatteryGuidance || !state.isIgnoringBatteryOptimizations) {
                BatteryOptimizationSection(state, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Vacation Mode
            VacationModeSection(state, viewModel)
            Spacer(modifier = Modifier.height(16.dp))

            // Alarm Defaults
            SettingsGroup("Alarm Defaults") {
                SettingsToggle("24-hour format", state.settings.is24HourFormat, viewModel::toggle24Hour)
                SettingsToggle("Show on lock screen", state.settings.showOnLockScreen, viewModel::toggleLockScreen)
                SettingsToggle("Use phone speakers", state.settings.usePhoneSpeakers, viewModel::togglePhoneSpeakers)
                SettingsValue("Default snooze", "${state.settings.defaultSnoozeDuration} min")
                SettingsValue("Gradual volume", "${state.settings.defaultGradualVolume / 60}m ${state.settings.defaultGradualVolume % 60}s")

                // Auto-silence
                var showAutoSilenceMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-silence after", color = TextPrimary)
                    Box {
                        TextButton(onClick = { showAutoSilenceMenu = true }) {
                            Text(
                                if (state.settings.autoSilenceMinutes == 0) "Never" else "${state.settings.autoSilenceMinutes} min",
                                color = AccentBlue
                            )
                        }
                        DropdownMenu(
                            expanded = showAutoSilenceMenu,
                            onDismissRequest = { showAutoSilenceMenu = false }
                        ) {
                            listOf(0, 5, 10, 15, 30).forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text(if (mins == 0) "Never" else "$mins minutes") },
                                    onClick = {
                                        viewModel.updateAutoSilence(mins)
                                        showAutoSilenceMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dashboard
            SettingsGroup("Dashboard") {
                SettingsToggle("Show weather", state.settings.showWeatherOnDashboard, viewModel::toggleShowWeather)
                SettingsToggle("Show calendar", state.settings.showCalendarOnDashboard, viewModel::toggleShowCalendar)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature unit", color = TextPrimary)
                    TextButton(onClick = viewModel::toggleTemperatureUnit) {
                        Text(
                            if (state.settings.temperatureUnit == "celsius") "Celsius (\u00B0C)" else "Fahrenheit (\u00B0F)",
                            color = AccentBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup & Restore
            BackupRestoreSection(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // Alarm Statistics
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                onClick = onNavigateToStats
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BarChart, null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alarm Statistics", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("View history, streaks, and patterns", color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stopwatch
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                onClick = onNavigateToStopwatch
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Speed, null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stopwatch", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Lap tracking with best/worst highlighting", color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About
            SettingsGroup("About") {
                SettingsValue("Version", state.appVersion)
                SettingsValue("Device", state.deviceModel)
                SettingsValue("Android", state.androidVersion)
                SettingsInfo("License", "Apache License 2.0")
                SettingsInfo("Source Code", "github.com/SysAdminDoc/AlarmClockXtreme")
            }

            Spacer(modifier = Modifier.height(32.dp))
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
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } else "Not set"

    val endDate = if (settings.vacationEndMillis > 0) {
        Instant.ofEpochMilli(settings.vacationEndMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } else "Not set"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (settings.vacationModeEnabled)
                SnoozeYellow.copy(alpha = 0.1f) else SurfaceMedium
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BeachAccess,
                    contentDescription = "Vacation mode",
                    tint = if (settings.vacationModeEnabled) SnoozeYellow else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Vacation Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.vacationModeEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && settings.vacationStartMillis > 0 && settings.vacationEndMillis > 0) {
                            viewModel.setVacationMode(true, settings.vacationStartMillis, settings.vacationEndMillis)
                        } else {
                            viewModel.setVacationMode(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SnoozeYellow,
                        checkedTrackColor = SnoozeYellow.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Repeating alarms will be silenced during vacation dates. They stay enabled but won't fire.",
                color = TextSecondary, fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier
                    .weight(1f)
                    .clickable { showStartPicker = true }) {
                    Text("Start", color = TextMuted, fontSize = 12.sp)
                    Text(startDate, color = AccentBlue, fontSize = 14.sp)
                }
                Column(modifier = Modifier
                    .weight(1f)
                    .clickable { showEndPicker = true }) {
                    Text("End", color = TextMuted, fontSize = 12.sp)
                    Text(endDate, color = AccentBlue, fontSize = 14.sp)
                }
            }
        }
    }

    // Start date picker
    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (settings.vacationStartMillis > 0)
                settings.vacationStartMillis else System.currentTimeMillis()
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
                }) { Text("OK", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceMedium)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End date picker
    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (settings.vacationEndMillis > 0)
                settings.vacationEndMillis else System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
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
                }) { Text("OK", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceMedium)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun BatteryOptimizationSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isIgnoringBatteryOptimizations)
                DismissGreen.copy(alpha = 0.1f) else AccentRed.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle
                    else Icons.Default.Warning,
                    null,
                    tint = if (state.isIgnoringBatteryOptimizations) DismissGreen else AccentRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Battery Optimization", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isIgnoringBatteryOptimizations) {
                Text("Battery optimization disabled. Alarms will fire reliably.", color = TextSecondary, fontSize = 14.sp)
            } else {
                Text("Battery optimization may prevent alarms from firing.", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::requestBatteryExemption,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Disable Battery Optimization") }
            }

            if (state.needsBatteryGuidance && state.batteryGuidanceSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    state.batteryGuidanceTitle.ifBlank { "${state.manufacturerName} Settings" },
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SnoozeYellow
                )
                Spacer(modifier = Modifier.height(8.dp))
                state.batteryGuidanceSteps.forEachIndexed { i, step ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("${i + 1}.", color = AccentBlue, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                        Text(step, color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title, style = MaterialTheme.typography.labelLarge, color = AccentBlue,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
        ) { Column { content() } }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentBlue,
                checkedTrackColor = AccentBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = ToggleOff,
                uncheckedTrackColor = ToggleTrackOff
            )
        )
    }
}

@Composable
private fun SettingsValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextPrimary)
        Text(value, color = AccentBlue)
    }
}

@Composable
private fun SettingsInfo(label: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(label, color = TextPrimary)
        Text(description, color = TextSecondary, fontSize = 12.sp)
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

    SettingsGroup("Backup & Restore") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { exportLauncher.launch("alarmclock_backup.json") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import")
            }
        }
        Text(
            "Export alarms and settings to JSON. Import on another device to restore.",
            color = TextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    // Result snackbar
    backupResult?.let { message ->
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DismissGreen.copy(alpha = 0.15f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = DismissGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(message, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::clearBackupResult) {
                    Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
