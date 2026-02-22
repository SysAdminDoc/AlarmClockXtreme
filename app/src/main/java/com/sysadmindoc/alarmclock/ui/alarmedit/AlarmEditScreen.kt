package com.sysadmindoc.alarmclock.ui.alarmedit

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.ringtone.RingtonePickerSheet
import com.sysadmindoc.alarmclock.ui.theme.*
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlarmEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    var showRingtonePicker by remember { mutableStateOf(false) }

    // Handle invalid alarm ID
    if (state.notFound) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    // Ringtone picker sheet
    if (showRingtonePicker) {
        RingtonePickerSheet(
            currentUri = state.ringtoneUri,
            onSelect = viewModel::updateRingtoneUri,
            onDismiss = { showRingtonePicker = false }
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Alarm" else "New Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Cancel", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onNavigateBack) },
                        enabled = !state.isSaving
                    ) {
                        Text("SAVE", color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderTop
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Time display - tap to edit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val hour12 = if (state.hour % 12 == 0) 12 else state.hour % 12
                    val amPm = if (state.hour < 12) "AM" else "PM"
                    Text(
                        text = "$hour12:${String.format("%02d", state.minute)}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary
                    )
                    Text(
                        text = " $amPm",
                        fontSize = 24.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Day selector
            DaySelector(
                selectedDays = state.repeatDays,
                onToggleDay = viewModel::toggleDay
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Label
            SettingsSection("Label") {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::updateLabel,
                    placeholder = { Text("Alarm label", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound settings
            SettingsSection("Sound") {
                SettingsRow(
                    label = "Alarm sound",
                    trailing = {
                        TextButton(onClick = { showRingtonePicker = true }) {
                            Text(
                                when (state.ringtoneUri) {
                                    "" -> "Default"
                                    "silent" -> "Silent"
                                    else -> "Custom"
                                },
                                color = AccentBlue
                            )
                        }
                    }
                )

                SettingsRow(
                    label = "Override system volume",
                    trailing = {
                        Switch(
                            checked = state.overrideSystemVolume,
                            onCheckedChange = viewModel::updateOverrideVolume,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )

                if (state.overrideSystemVolume) {
                    SettingsRow(label = "Volume") {
                        Text("${state.volume}%", color = AccentBlue)
                    }
                    Slider(
                        value = state.volume.toFloat(),
                        onValueChange = { viewModel.updateVolume(it.toInt()) },
                        valueRange = 10f..100f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentBlue,
                            activeTrackColor = AccentBlue
                        )
                    )
                }

                SettingsRow(label = "Gradually increase volume") {
                    Text(
                        text = "${state.gradualVolumeSeconds / 60} min ${state.gradualVolumeSeconds % 60} sec",
                        color = AccentBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vibration
            SettingsSection("Vibration") {
                SettingsRow(
                    label = "Vibration",
                    trailing = {
                        Switch(
                            checked = state.vibrationEnabled,
                            onCheckedChange = viewModel::updateVibration,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze
            SettingsSection("Snooze") {
                SettingsRow(label = "Snooze duration") {
                    Text("${state.snoozeDurationMinutes} min", color = AccentBlue)
                }
            }

            // Dismiss Challenge
            SettingsSection("Dismiss Challenge") {
                val challengeOptions = listOf(
                    "NONE" to "None",
                    "MATH_EASY" to "Math (Easy)",
                    "MATH_MEDIUM" to "Math (Medium)",
                    "MATH_HARD" to "Math (Hard)",
                    "SHAKE" to "Shake Phone",
                    "SEQUENCE" to "Number Sequence",
                    "MEMORY_PATTERN" to "Memory Pattern"
                )
                var expanded by remember { mutableStateOf(false) }

                SettingsRow(label = "Challenge type") {
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(
                                challengeOptions.find { it.first == state.challengeType }?.second ?: "None",
                                color = AccentBlue
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            challengeOptions.forEach { (type, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            color = if (type == state.challengeType) AccentBlue else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateChallengeType(type)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (state.challengeType != "NONE") {
                    Text(
                        text = when (state.challengeType) {
                            "MATH_EASY" -> "Solve a simple math problem to dismiss"
                            "MATH_MEDIUM" -> "Solve a two-operation math problem"
                            "MATH_HARD" -> "Solve a harder math problem with larger numbers"
                            "SHAKE" -> "Shake your phone 30 times to dismiss"
                            "SEQUENCE" -> "Tap 6 numbers in ascending order"
                            "MEMORY_PATTERN" -> "Memorize and recreate a 4-tile pattern on a 3x3 grid"
                            else -> ""
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent save button
            Button(
                onClick = { viewModel.save(onNavigateBack) },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Alarm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.hour,
            initialMinute = state.minute,
            is24Hour = state.is24HourFormat
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = SurfaceCard,
                        selectorColor = AccentBlue,
                        containerColor = SurfaceMedium,
                        timeSelectorSelectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        timeSelectorUnselectedContainerColor = SurfaceCard
                    )
                )
            },
            containerColor = SurfaceMedium
        )
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit
) {
    val days = listOf(
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "S"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { (day, label) ->
            val isSelected = day in selectedDays
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) AccentBlue else SurfaceCard)
                    .clickable { onToggleDay(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = AccentBlue,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextPrimary)
        if (trailing != null) trailing()
    }
}
