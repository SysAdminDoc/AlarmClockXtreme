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
                if (state.is24HourFormat) {
                    Text(
                        text = "${String.format("%02d", state.hour)}:${String.format("%02d", state.minute)}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary
                    )
                } else {
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

            // Group
            SettingsSection("Group") {
                var showGroupMenu by remember { mutableStateOf(false) }
                val defaultGroups = listOf("", "Work", "School", "Gym", "Medication", "Personal")
                SettingsRow(label = "Alarm group") {
                    Box {
                        TextButton(onClick = { showGroupMenu = true }) {
                            Text(
                                state.group.ifBlank { "None" },
                                color = AccentBlue
                            )
                        }
                        DropdownMenu(
                            expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false }
                        ) {
                            defaultGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            group.ifBlank { "None" },
                                            color = if (group == state.group) AccentBlue else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateGroup(group)
                                        showGroupMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Custom group input
                if (state.group.isNotBlank() && state.group !in defaultGroups) {
                    Text(
                        "Custom: ${state.group}",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = state.group,
                    onValueChange = viewModel::updateGroup,
                    placeholder = { Text("Custom group name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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

                // Gradual volume - interactive slider
                var showGradualMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Gradually increase volume") {
                    Box {
                        TextButton(onClick = { showGradualMenu = true }) {
                            Text(
                                text = when (state.gradualVolumeSeconds) {
                                    0 -> "Off"
                                    else -> "${state.gradualVolumeSeconds / 60}m ${state.gradualVolumeSeconds % 60}s"
                                },
                                color = AccentBlue
                            )
                        }
                        DropdownMenu(
                            expanded = showGradualMenu,
                            onDismissRequest = { showGradualMenu = false }
                        ) {
                            listOf(0, 15, 30, 60, 90, 120, 180, 300).forEach { secs ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (secs) {
                                                0 -> "Off (full volume immediately)"
                                                else -> "${secs / 60}m ${secs % 60}s"
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateGradualVolume(secs)
                                        showGradualMenu = false
                                    }
                                )
                            }
                        }
                    }
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

                if (state.vibrationEnabled) {
                    // Vibration pattern picker
                    var showPatternMenu by remember { mutableStateOf(false) }
                    val patterns = listOf(
                        "default" to "Default (strong pulse)",
                        "gentle" to "Gentle (soft pulse)",
                        "heartbeat" to "Heartbeat (double tap)",
                        "escalating" to "Escalating (builds up)",
                        "sos" to "SOS (urgent pattern)"
                    )
                    SettingsRow(label = "Vibration pattern") {
                        Box {
                            TextButton(onClick = { showPatternMenu = true }) {
                                Text(
                                    patterns.find { it.first == state.vibrationPattern }?.second?.substringBefore(" (") ?: "Default",
                                    color = AccentBlue
                                )
                            }
                            DropdownMenu(
                                expanded = showPatternMenu,
                                onDismissRequest = { showPatternMenu = false }
                            ) {
                                patterns.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color = if (key == state.vibrationPattern) AccentBlue else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateVibrationPattern(key)
                                            showPatternMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze - interactive picker
            SettingsSection("Snooze") {
                var showSnoozeMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Snooze duration") {
                    Box {
                        TextButton(onClick = { showSnoozeMenu = true }) {
                            Text("${state.snoozeDurationMinutes} min", color = AccentBlue)
                        }
                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            listOf(1, 3, 5, 10, 15, 20, 30).forEach { mins ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$mins minutes",
                                            color = if (mins == state.snoozeDurationMinutes) AccentBlue else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateSnoozeDuration(mins)
                                        showSnoozeMenu = false
                                    }
                                )
                            }
                        }
                    }
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
                    "MEMORY_PATTERN" to "Memory Pattern",
                    "TYPING" to "Type a Phrase",
                    "WALK_STEPS" to "Walk Steps",
                    "NFC_SCAN" to "NFC Tag Scan",
                    "BARCODE_SCAN" to "Barcode Scan",
                    "PHOTO_MATCH" to "Photo Match",
                    "SQUAT" to "Squats",
                    "WIFI_CONNECT" to "Wi-Fi Connect",
                    "MAZE" to "Maze Puzzle"
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
                            "TYPING" -> "Type a random wake-up phrase to dismiss"
                            "WALK_STEPS" -> "Walk a set number of steps to dismiss"
                            "NFC_SCAN" -> "Scan a specific NFC tag to dismiss"
                            "BARCODE_SCAN" -> "Scan the registered barcode to dismiss"
                            "PHOTO_MATCH" -> "Take a photo matching a reference to dismiss"
                            "SQUAT" -> "Do 10 squats with your phone to dismiss"
                            "WIFI_CONNECT" -> "Connect to a specific Wi-Fi network to dismiss"
                            "MAZE" -> "Navigate through a simple maze to dismiss"
                            else -> ""
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // WALK_STEPS: step count config
                if (state.challengeType == "WALK_STEPS") {
                    var showStepsMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Steps required") {
                        Box {
                            TextButton(onClick = { showStepsMenu = true }) {
                                Text("${state.walkStepsRequired} steps", color = AccentBlue)
                            }
                            DropdownMenu(
                                expanded = showStepsMenu,
                                onDismissRequest = { showStepsMenu = false }
                            ) {
                                listOf(10, 20, 30, 50, 100).forEach { steps ->
                                    DropdownMenuItem(
                                        text = { Text("$steps steps", color = if (steps == state.walkStepsRequired) AccentBlue else TextPrimary) },
                                        onClick = { viewModel.updateWalkSteps(steps); showStepsMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // NFC_SCAN: tag registration field
                if (state.challengeType == "NFC_SCAN") {
                    OutlinedTextField(
                        value = state.nfcTagId,
                        onValueChange = viewModel::updateNfcTagId,
                        label = { Text("NFC Tag ID (tap tag to register in alarm screen)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                            cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentBlue
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }

                // BARCODE_SCAN: barcode value field
                if (state.challengeType == "BARCODE_SCAN") {
                    OutlinedTextField(
                        value = state.barcodeValue,
                        onValueChange = viewModel::updateBarcodeValue,
                        label = { Text("Barcode value (scan to register in alarm screen)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                            cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentBlue
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }

                // PHOTO_MATCH: reference photo URI field
                if (state.challengeType == "PHOTO_MATCH") {
                    Text(
                        "Reference photo URI: ${state.photoMatchUri.ifBlank { "Not set" }}",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wake effects
            SettingsSection("Wake Effects") {
                SettingsRow(
                    label = "Flash wake (brighten screen)",
                    trailing = {
                        Switch(
                            checked = state.flashWake,
                            onCheckedChange = viewModel::updateFlashWake,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                Text(
                    "Gradually increases screen brightness alongside volume",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Morning Announcement (TTS)
            SettingsSection("Morning Announcement") {
                SettingsRow(
                    label = "Speak time, date & weather",
                    trailing = {
                        Switch(
                            checked = state.ttsEnabled,
                            onCheckedChange = viewModel::updateTtsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                Text(
                    "Uses on-device text-to-speech to announce the time, date, and weather after dismissal",
                    color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wake Confirmation
            SettingsSection("Wake Confirmation") {
                SettingsRow(
                    label = "Confirm you're awake",
                    trailing = {
                        Switch(
                            checked = state.wakeConfirmEnabled,
                            onCheckedChange = { viewModel.updateWakeConfirm(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                if (state.wakeConfirmEnabled) {
                    var showDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Re-alarm delay if not confirmed") {
                        Box {
                            TextButton(onClick = { showDelayMenu = true }) {
                                Text("${state.wakeConfirmDelayMinutes} min", color = AccentBlue)
                            }
                            DropdownMenu(
                                expanded = showDelayMenu,
                                onDismissRequest = { showDelayMenu = false }
                            ) {
                                listOf(5, 10, 15, 20, 30).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes", color = if (mins == state.wakeConfirmDelayMinutes) AccentBlue else TextPrimary) },
                                        onClick = { viewModel.updateWakeConfirm(true, mins); showDelayMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "A notification will appear after dismissal. If you don't confirm within the delay, the alarm re-fires.",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Alarm
            SettingsSection("Smart Alarm") {
                SettingsRow(
                    label = "Wake during light sleep",
                    trailing = {
                        Switch(
                            checked = state.smartAlarmEnabled,
                            onCheckedChange = { viewModel.updateSmartAlarm(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                if (state.smartAlarmEnabled) {
                    var showWindowMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Detection window") {
                        Box {
                            TextButton(onClick = { showWindowMenu = true }) {
                                Text("${state.smartAlarmWindowMinutes} min before", color = AccentBlue)
                            }
                            DropdownMenu(
                                expanded = showWindowMenu,
                                onDismissRequest = { showWindowMenu = false }
                            ) {
                                listOf(15, 20, 30, 45, 60).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes before alarm", color = if (mins == state.smartAlarmWindowMinutes) AccentBlue else TextPrimary) },
                                        onClick = { viewModel.updateSmartAlarm(true, mins); showWindowMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Monitors motion via accelerometer. Fires the alarm early if light sleep is detected within the window.",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Holiday Skip
            SettingsSection("Holidays") {
                SettingsRow(
                    label = "Skip on public holidays",
                    trailing = {
                        Switch(
                            checked = state.skipOnHolidays,
                            onCheckedChange = viewModel::updateSkipOnHolidays,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                Text(
                    "Requires holiday auto-skip and country code configured in Settings → Integrations",
                    color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spotify Ringtone
            SettingsSection("Spotify Ringtone") {
                OutlinedTextField(
                    value = state.spotifyUri,
                    onValueChange = viewModel::updateSpotifyUri,
                    label = { Text("Spotify URI (e.g. spotify:track:...)", color = TextMuted) },
                    placeholder = { Text("Leave blank to use default ringtone", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AccentBlue
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                Text(
                    "Requires Spotify installed. Falls back to default ringtone if unavailable.",
                    color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Philips Hue Sunrise
            SettingsSection("Philips Hue Sunrise") {
                SettingsRow(
                    label = "Sunrise light simulation",
                    trailing = {
                        Switch(
                            checked = state.hueEnabled,
                            onCheckedChange = { viewModel.updateHue(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                if (state.hueEnabled) {
                    var showHueMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Start lights before alarm") {
                        Box {
                            TextButton(onClick = { showHueMenu = true }) {
                                Text("${state.huePreWakeMinutes} min before", color = AccentBlue)
                            }
                            DropdownMenu(
                                expanded = showHueMenu,
                                onDismissRequest = { showHueMenu = false }
                            ) {
                                listOf(10, 15, 20, 30, 45, 60, 90).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes before", color = if (mins == state.huePreWakeMinutes) AccentBlue else TextPrimary) },
                                        onClick = { viewModel.updateHue(true, mins); showHueMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Requires Hue bridge IP and API key configured in Settings → Philips Hue",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Mission Chaining
            SettingsSection("Mission Chaining") {
                SettingsRow(
                    label = "Challenge chain",
                    trailing = {
                        TextButton(onClick = { /* TODO: chain picker dialog */ }) {
                            Text(
                                if (state.challengeChain.isBlank()) "Off" else "${state.challengeChain.split(",").size} challenges",
                                color = AccentBlue
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = state.challengeChain,
                    onValueChange = viewModel::updateChallengeChain,
                    label = { Text("Chain (comma-separated: MATH_EASY,SHAKE,TYPING)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                Text(
                    "Stack multiple challenges in sequence. Overrides single challenge type when set.",
                    color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Anti-Snooze Features
            SettingsSection("Anti-Snooze") {
                SettingsRow(
                    label = "Progressive snooze (shorter each time)",
                    trailing = {
                        Switch(
                            checked = state.progressiveSnooze,
                            onCheckedChange = viewModel::updateProgressiveSnooze,
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                        )
                    }
                )
                Text("Each snooze shortens by 1 minute (10 -> 9 -> 8 -> ...)", color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                SettingsRow(
                    label = "Backup sound escalation",
                    trailing = {
                        Switch(
                            checked = state.backupSoundEnabled,
                            onCheckedChange = { viewModel.updateBackupSound(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                        )
                    }
                )
                if (state.backupSoundEnabled) {
                    var showDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Escalate after") {
                        Box {
                            TextButton(onClick = { showDelayMenu = true }) {
                                Text("${state.backupSoundDelaySec}s", color = AccentBlue)
                            }
                            DropdownMenu(expanded = showDelayMenu, onDismissRequest = { showDelayMenu = false }) {
                                listOf(20, 30, 40, 60, 90, 120).forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text("$sec seconds") },
                                        onClick = { viewModel.updateBackupSound(true, sec); showDelayMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Text("Cranks volume to max if no interaction within the delay", color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }

                SettingsRow(
                    label = "Flashlight strobe",
                    trailing = {
                        Switch(
                            checked = state.flashlightStrobe,
                            onCheckedChange = viewModel::updateFlashlightStrobe,
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Sunrise Simulation
            SettingsSection("Sunrise Simulation") {
                SettingsRow(
                    label = "Screen sunrise (color transition)",
                    trailing = {
                        Switch(
                            checked = state.sunriseSimulation,
                            onCheckedChange = { viewModel.updateSunriseSimulation(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                        )
                    }
                )
                if (state.sunriseSimulation) {
                    var showMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Duration") {
                        Box {
                            TextButton(onClick = { showMenu = true }) {
                                Text("${state.sunriseMinutes} min", color = AccentBlue)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                listOf(5, 10, 15, 20, 30).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes") },
                                        onClick = { viewModel.updateSunriseSimulation(true, mins); showMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Text("Screen transitions from deep red to warm yellow, simulating a sunrise", color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Sound Source
            SettingsSection("Internet Radio") {
                OutlinedTextField(
                    value = state.internetRadioUrl,
                    onValueChange = viewModel::updateInternetRadioUrl,
                    label = { Text("Stream URL (http://...)", color = TextMuted) },
                    placeholder = { Text("Leave blank for default ringtone", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                Text("Stream internet radio as alarm sound. Falls back to default on failure.", color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Guardian Angel
            SettingsSection("Guardian Angel") {
                SettingsRow(
                    label = "Emergency contact alert",
                    trailing = {
                        Switch(
                            checked = state.guardianEnabled,
                            onCheckedChange = { viewModel.updateGuardian(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                        )
                    }
                )
                if (state.guardianEnabled) {
                    OutlinedTextField(
                        value = state.guardianPhone,
                        onValueChange = { viewModel.updateGuardian(true, phone = it) },
                        label = { Text("Emergency phone number", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                            cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    var showDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Alert after") {
                        Box {
                            TextButton(onClick = { showDelayMenu = true }) {
                                Text("${state.guardianDelaySec / 60} min", color = AccentBlue)
                            }
                            DropdownMenu(expanded = showDelayMenu, onDismissRequest = { showDelayMenu = false }) {
                                listOf(120, 180, 300, 600, 900).forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text("${sec / 60} minutes") },
                                        onClick = { viewModel.updateGuardian(true, delaySec = sec); showDelayMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Text("Sends SMS and calls your emergency contact if the alarm is not dismissed within the delay", color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Morning Routine
            SettingsSection("Morning Routine") {
                OutlinedTextField(
                    value = state.morningRoutine,
                    onValueChange = viewModel::updateMorningRoutine,
                    label = { Text("Checklist items (one per line)", color = TextMuted) },
                    placeholder = { Text("Stretch\nDrink water\nBrush teeth", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    minLines = 3, maxLines = 6
                )
                Text("Shown as a checklist after alarm dismissal on the morning briefing screen", color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.2.0: Advanced
            SettingsSection("Advanced") {
                SettingsRow(label = "Alarm profile") {
                    OutlinedTextField(
                        value = state.profileName,
                        onValueChange = viewModel::updateProfileName,
                        placeholder = { Text("e.g. Work, Travel, Weekend", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                            cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.width(180.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = state.specificDate,
                    onValueChange = viewModel::updateSpecificDate,
                    label = { Text("Specific date (YYYY-MM-DD, leave blank for repeat days)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                var showEarlyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Early dismiss window") {
                    Box {
                        TextButton(onClick = { showEarlyMenu = true }) {
                            Text(if (state.earlyDismissMinutes == 0) "Disabled" else "${state.earlyDismissMinutes} min", color = AccentBlue)
                        }
                        DropdownMenu(expanded = showEarlyMenu, onDismissRequest = { showEarlyMenu = false }) {
                            listOf(0, 15, 30, 60).forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text(if (mins == 0) "Disabled" else "$mins minutes before") },
                                    onClick = { viewModel.updateEarlyDismiss(mins); showEarlyMenu = false }
                                )
                            }
                        }
                    }
                }
                Text("Skip upcoming alarm from the persistent notification up to N minutes before it fires", color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                OutlinedTextField(
                    value = state.wifiDismissSsid,
                    onValueChange = viewModel::updateWifiDismissSsid,
                    label = { Text("Wi-Fi dismiss SSID (connect to this network to dismiss)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
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
        Text(text = label, color = TextPrimary, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}
