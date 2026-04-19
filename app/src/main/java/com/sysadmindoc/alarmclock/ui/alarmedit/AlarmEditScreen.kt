package com.sysadmindoc.alarmclock.ui.alarmedit

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.ringtone.RingtonePickerSheet
import com.sysadmindoc.alarmclock.ui.theme.*
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlarmEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    var showRingtonePicker by remember { mutableStateOf(false) }
    var showChainPicker by remember { mutableStateOf(false) }
    var photoReferenceStatus by remember { mutableStateOf("") }

    val photoReferenceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            photoReferenceStatus = "No reference photo captured."
            return@rememberLauncherForActivityResult
        }

        val referenceKey = if (state.isEditing && state.createdAt > 0) {
            state.createdAt
        } else {
            System.currentTimeMillis()
        }
        runCatching {
            PhotoMatcher.saveReference(context, referenceKey, bitmap)
        }.onSuccess { uri ->
            viewModel.updatePhotoMatchUri(uri)
            photoReferenceStatus = "Reference photo saved."
        }.onFailure {
            photoReferenceStatus = "Could not save reference photo."
        }
        if (!bitmap.isRecycled) bitmap.recycle()
    }
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            photoReferenceLauncher.launch(null)
        } else {
            photoReferenceStatus = "Camera permission is required to capture a reference photo."
        }
    }
    val captureReferencePhoto = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            photoReferenceLauncher.launch(null)
        } else {
            photoPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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

    if (showChainPicker) {
        ChallengeChainPickerSheet(
            currentChain = state.challengeChain.toChallengeChainList(),
            onApply = { chain ->
                viewModel.updateChallengeChain(chain.toChallengeChainValue())
                showChainPicker = false
            },
            onDismiss = { showChainPicker = false }
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            val editorSubtitle = if (state.isEditing) {
                "Refine timing, sound, and wake-up behavior."
            } else {
                "Build an alarm that feels intentional from the first ring."
            }
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (state.isEditing) "Edit Alarm" else "New Alarm",
                            color = TextPrimary
                        )
                        Text(
                            text = editorSubtitle,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
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
                        Text("Save alarm", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceDark.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
                shadowElevation = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { viewModel.save(onNavigateBack) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isSaving) Icons.Default.Schedule else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isSaving) "Saving alarm..." else if (state.isEditing) "Save changes" else "Create alarm",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                highlighted = true
            ) {
                AppSectionTitle(
                    title = "Alarm preview",
                    description = "Tap the time or days below to shape when this alarm should ring."
                )

                AppStatusChip(
                    label = if (state.isEditing) "Editing existing alarm" else "New alarm",
                    icon = if (state.isEditing) Icons.Default.Edit else Icons.Default.AddAlarm,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                        .padding(vertical = 8.dp),
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

                Text(
                    text = "Tap the time to adjust it precisely.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                DaySelector(
                    selectedDays = state.repeatDays,
                    onToggleDay = viewModel::toggleDay
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = state.repeatDays.toAlarmRepeatSummary(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppStatusChip(
                        label = state.challengeSummary(),
                        color = if (state.challengeType == "NONE" && state.challengeChain.isBlank()) TextMuted else SnoozeYellow
                    )
                    AppStatusChip(
                        label = state.soundSummary(),
                        color = DismissGreen
                    )
                }
            }

            // Label
            SettingsSection("Label") {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::updateLabel,
                    placeholder = { Text("Alarm label", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )
            }

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
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
            }

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
                            colors = appSwitchColors()
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

            // Vibration
            SettingsSection("Vibration") {
                SettingsRow(
                    label = "Vibration",
                    trailing = {
                        Switch(
                            checked = state.vibrationEnabled,
                            onCheckedChange = viewModel::updateVibration,
                            colors = appSwitchColors()
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
                val challengeOptions = alarmChallengeOptions()
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
                    SettingsHint(
                        text = state.challengeType.toAlarmChallengeDescription()
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
                        label = { Text("NFC tag ID", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }

                // BARCODE_SCAN: barcode value field
                if (state.challengeType == "BARCODE_SCAN") {
                    OutlinedTextField(
                        value = state.barcodeValue,
                        onValueChange = viewModel::updateBarcodeValue,
                        label = { Text("Barcode or QR value", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }

                // PHOTO_MATCH: reference photo URI field
                if (state.challengeType == "PHOTO_MATCH") {
                    SettingsRow(label = "Reference photo") {
                        OutlinedButton(
                            onClick = captureReferencePhoto,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.photoMatchUri.isBlank()) "Capture" else "Replace")
                        }
                    }
                    SettingsHint(
                        if (state.photoMatchUri.isBlank()) {
                            "Capture a reference photo from the place or angle the alarm should require."
                        } else {
                            "Reference photo saved for this alarm."
                        },
                        tone = if (state.photoMatchUri.isBlank()) HintTone.Warning else HintTone.Neutral
                    )
                    if (photoReferenceStatus.isNotBlank()) {
                        SettingsHint(
                            photoReferenceStatus,
                            tone = if (state.photoMatchUri.isBlank()) HintTone.Warning else HintTone.Neutral
                        )
                    }
                }
            }

            // Wake effects
            SettingsSection("Wake Effects") {
                SettingsRow(
                    label = "Flash wake (brighten screen)",
                    trailing = {
                        Switch(
                            checked = state.flashWake,
                            onCheckedChange = viewModel::updateFlashWake,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    "Gradually increases screen brightness alongside volume",
                    tone = HintTone.Neutral
                )
            }

            // Morning Announcement (TTS)
            SettingsSection("Morning Announcement") {
                SettingsRow(
                    label = "Speak time, date & weather",
                    trailing = {
                        Switch(
                            checked = state.ttsEnabled,
                            onCheckedChange = viewModel::updateTtsEnabled,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    "Uses on-device text-to-speech to announce the time, date, and weather after dismissal",
                    tone = HintTone.Neutral
                )
            }

            // Wake Confirmation
            SettingsSection("Wake Confirmation") {
                SettingsRow(
                    label = "Confirm you're awake",
                    trailing = {
                        Switch(
                            checked = state.wakeConfirmEnabled,
                            onCheckedChange = { viewModel.updateWakeConfirm(it) },
                            colors = appSwitchColors()
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
                    SettingsHint(
                        "A notification will appear after dismissal. If you don't confirm within the delay, the alarm re-fires.",
                        tone = HintTone.Warning
                    )
                }
            }

            // Smart Alarm
            SettingsSection("Smart Alarm") {
                SettingsRow(
                    label = "Wake during light sleep",
                    trailing = {
                        Switch(
                            checked = state.smartAlarmEnabled,
                            onCheckedChange = { viewModel.updateSmartAlarm(it) },
                            colors = appSwitchColors()
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
                    SettingsHint(
                        "Monitors motion via accelerometer. Fires the alarm early if light sleep is detected within the window.",
                        tone = HintTone.Neutral
                    )
                }
            }

            // Holiday Skip
            SettingsSection("Holidays") {
                SettingsRow(
                    label = "Skip on public holidays",
                    trailing = {
                        Switch(
                            checked = state.skipOnHolidays,
                            onCheckedChange = viewModel::updateSkipOnHolidays,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    "Requires holiday auto-skip and country code configured in Settings → Integrations",
                    tone = HintTone.Warning
                )
            }

            // Spotify Ringtone
            SettingsSection("Spotify Ringtone") {
                OutlinedTextField(
                    value = state.spotifyUri,
                    onValueChange = viewModel::updateSpotifyUri,
                    label = { Text("Spotify URI (e.g. spotify:track:...)", color = TextMuted) },
                    placeholder = { Text("Leave blank to use default ringtone", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                SettingsHint(
                    "Requires Spotify installed. Falls back to default ringtone if unavailable.",
                    tone = HintTone.Warning
                )
            }

            // Philips Hue Sunrise
            SettingsSection("Philips Hue Sunrise") {
                SettingsRow(
                    label = "Sunrise light simulation",
                    trailing = {
                        Switch(
                            checked = state.hueEnabled,
                            onCheckedChange = { viewModel.updateHue(it) },
                            colors = appSwitchColors()
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
                    SettingsHint(
                        "Requires Hue bridge IP and API key configured in Settings → Philips Hue",
                        tone = HintTone.Warning
                    )
                }
            }

            // v1.2.0: Mission Chaining
            SettingsSection("Mission Chaining") {
                val chainItems = state.challengeChain.toChallengeChainList()
                SettingsRow(
                    label = "Challenge chain",
                    trailing = {
                        TextButton(onClick = { showChainPicker = true }) {
                            Text(
                                if (chainItems.isEmpty()) "Choose" else "${chainItems.size} challenges",
                                color = AccentBlue
                            )
                        }
                    }
                )
                if (chainItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chainItems.forEachIndexed { index, challenge ->
                            AppStatusChip(
                                label = "${index + 1}. ${challenge.toAlarmChallengeSummary()}",
                                color = SnoozeYellow
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.challengeChain,
                    onValueChange = viewModel::updateChallengeChain,
                    label = { Text("Advanced chain override", color = TextMuted) },
                    placeholder = { Text("MATH_EASY,SHAKE,TYPING", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                SettingsHint(
                    "Stack multiple challenges in sequence. Use the picker above for a guided setup, or edit the raw chain directly if you already know the codes.",
                    tone = HintTone.Neutral
                )
            }

            // v1.2.0: Anti-Snooze Features
            SettingsSection("Anti-Snooze") {
                SettingsRow(
                    label = "Progressive snooze (shorter each time)",
                    trailing = {
                        Switch(
                            checked = state.progressiveSnooze,
                            onCheckedChange = viewModel::updateProgressiveSnooze,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    "Each snooze shortens by 1 minute, such as 10 → 9 → 8.",
                    tone = HintTone.Neutral
                )

                SettingsRow(
                    label = "Backup sound escalation",
                    trailing = {
                        Switch(
                            checked = state.backupSoundEnabled,
                            onCheckedChange = { viewModel.updateBackupSound(it) },
                            colors = appSwitchColors()
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
                    SettingsHint(
                        "Cranks volume to maximum if there is no interaction within the delay.",
                        tone = HintTone.Warning
                    )
                }

                SettingsRow(
                    label = "Flashlight strobe",
                    trailing = {
                        Switch(
                            checked = state.flashlightStrobe,
                            onCheckedChange = viewModel::updateFlashlightStrobe,
                            colors = appSwitchColors()
                        )
                    }
                )
            }

            // v1.2.0: Sunrise Simulation
            SettingsSection("Sunrise Simulation") {
                SettingsRow(
                    label = "Screen sunrise (color transition)",
                    trailing = {
                        Switch(
                            checked = state.sunriseSimulation,
                            onCheckedChange = { viewModel.updateSunriseSimulation(it) },
                            colors = appSwitchColors()
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
                    SettingsHint(
                        "Transitions the screen from deep red to warm yellow to simulate sunrise.",
                        tone = HintTone.Neutral
                    )
                }
            }

            // v1.2.0: Sound Source
            SettingsSection("Internet Radio") {
                OutlinedTextField(
                    value = state.internetRadioUrl,
                    onValueChange = viewModel::updateInternetRadioUrl,
                    label = { Text("Stream URL (http://...)", color = TextMuted) },
                    placeholder = { Text("Leave blank for default ringtone", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                SettingsHint(
                    "Streams internet radio as the alarm sound and falls back to the default ringtone on failure.",
                    tone = HintTone.Warning
                )
            }

            // v1.2.0: Guardian Angel
            SettingsSection("Guardian Angel") {
                SettingsRow(
                    label = "Emergency contact alert",
                    trailing = {
                        Switch(
                            checked = state.guardianEnabled,
                            onCheckedChange = { viewModel.updateGuardian(it) },
                            colors = appSwitchColors()
                        )
                    }
                )
                if (state.guardianEnabled) {
                    OutlinedTextField(
                        value = state.guardianPhone,
                        onValueChange = { viewModel.updateGuardian(true, phone = it) },
                        label = { Text("Emergency phone number", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
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
                    SettingsHint(
                        "Sends an SMS and places a call if the alarm is not dismissed within the delay.",
                        tone = HintTone.Danger
                    )
                }
            }

            // v1.2.0: Morning Routine
            SettingsSection("Morning Routine") {
                OutlinedTextField(
                    value = state.morningRoutine,
                    onValueChange = viewModel::updateMorningRoutine,
                    label = { Text("Checklist items (one per line)", color = TextMuted) },
                    placeholder = { Text("Stretch\nDrink water\nBrush teeth", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    minLines = 3, maxLines = 6
                )
                SettingsHint(
                    "Shown as a checklist after dismissal on the morning briefing screen.",
                    tone = HintTone.Neutral
                )
            }

            // v1.2.0: Advanced
            SettingsSection("Advanced") {
                SettingsRow(label = "Alarm profile") {
                    OutlinedTextField(
                        value = state.profileName,
                        onValueChange = viewModel::updateProfileName,
                        placeholder = { Text("e.g. Work, Travel, Weekend", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        modifier = Modifier.width(180.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = state.specificDate,
                    onValueChange = viewModel::updateSpecificDate,
                    label = { Text("Specific date (YYYY-MM-DD, leave blank for repeat days)", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
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
                SettingsHint(
                    "Allows a deliberate early skip from the upcoming-alarm notification before the ring begins.",
                    tone = HintTone.Neutral
                )

                OutlinedTextField(
                    value = state.wifiDismissSsid,
                    onValueChange = viewModel::updateWifiDismissSsid,
                    label = { Text("Wi-Fi dismiss SSID (connect to this network to dismiss)", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )

                // v1.4.0: Hardware-button action (Volume/Camera/Headset-hook keys
                // during firing). NONE = normal volume control passes through.
                var showHwMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Hardware-button action") {
                    Box {
                        TextButton(onClick = { showHwMenu = true }) {
                            Text(state.hardwareButtonAction.lowercase().replaceFirstChar { it.uppercase() }, color = AccentBlue)
                        }
                        DropdownMenu(expanded = showHwMenu, onDismissRequest = { showHwMenu = false }) {
                            listOf("NONE" to "None (default)", "SNOOZE" to "Snooze on any key", "DISMISS" to "Dismiss on any key").forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { viewModel.updateHardwareButtonAction(value); showHwMenu = false }
                                )
                            }
                        }
                    }
                }
                SettingsHint(
                    "Volume, camera, or headset keys during the alarm trigger the chosen action. NONE leaves normal volume control intact.",
                    tone = HintTone.Neutral
                )

                // v1.4.0: Dismiss-at-ringtone-end. Great for single-song wake-ups.
                SettingsRow(label = "Dismiss when song finishes") {
                    Switch(
                        checked = state.dismissAtRingtoneEnd,
                        onCheckedChange = viewModel::updateDismissAtRingtoneEnd,
                        colors = appSwitchColors()
                    )
                }
                SettingsHint(
                    "Turns off looping and auto-dismisses the alarm when the chosen ringtone or track finishes naturally. Internet radio ignores this setting.",
                    tone = HintTone.Neutral
                )

                // v1.4.0: Ringtone pool (anti-habituation). Newline-separated for
                // manual paste of content:// URIs or file:// paths — power-user
                // feature, intentionally not a file-picker (the existing picker
                // only handles one URI at a time).
                OutlinedTextField(
                    value = state.ringtonePool.replace(",", "\n"),
                    onValueChange = { viewModel.updateRingtonePool(it.replace("\n", ",")) },
                    label = { Text("Ringtone pool (one URI per line)", color = TextMuted) },
                    placeholder = { Text("Paste content:// or file:// URIs; a random one plays per fire", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    minLines = 2,
                    maxLines = 6
                )
                SettingsHint(
                    "When the pool is non-empty, a random entry is picked each fire and overrides the single Ringtone setting above. Leave empty to disable.",
                    tone = HintTone.Neutral
                )

                // v1.5.0: Sunrise/sunset-relative firing. Overrides the clock time
                // when offset is non-zero; uses last-known location for the solar
                // calc (cached by weather pulls) with a sensible fallback to clock.
                var showAnchorMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Solar anchor") {
                    Box {
                        TextButton(onClick = { showAnchorMenu = true }) {
                            Text(state.solarAnchor.lowercase().replaceFirstChar { it.uppercase() }, color = AccentBlue)
                        }
                        DropdownMenu(expanded = showAnchorMenu, onDismissRequest = { showAnchorMenu = false }) {
                            listOf("SUNRISE" to "Sunrise", "SUNSET" to "Sunset").forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { viewModel.updateSolarAnchor(value); showAnchorMenu = false }
                                )
                            }
                        }
                    }
                }
                var showOffsetMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Solar offset") {
                    Box {
                        TextButton(onClick = { showOffsetMenu = true }) {
                            val label = when {
                                state.solarOffsetMinutes == 0 -> "Off (use clock time)"
                                state.solarOffsetMinutes > 0 -> "+${state.solarOffsetMinutes} min"
                                else -> "${state.solarOffsetMinutes} min"
                            }
                            Text(label, color = AccentBlue)
                        }
                        DropdownMenu(expanded = showOffsetMenu, onDismissRequest = { showOffsetMenu = false }) {
                            listOf(0, -30, -15, 15, 30, 60, 120).forEach { mins ->
                                DropdownMenuItem(
                                    text = {
                                        val lbl = when {
                                            mins == 0 -> "Off (use clock time)"
                                            mins > 0 -> "+$mins min (after ${state.solarAnchor.lowercase()})"
                                            else -> "$mins min (before ${state.solarAnchor.lowercase()})"
                                        }
                                        Text(lbl)
                                    },
                                    onClick = { viewModel.updateSolarOffset(mins); showOffsetMenu = false }
                                )
                            }
                        }
                    }
                }
                SettingsHint(
                    "When the offset is non-zero the clock time above is ignored and the alarm fires relative to solar noon/dusk at your last known location.",
                    tone = HintTone.Neutral
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
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
                        selectorColor = MaterialTheme.colorScheme.primary,
                        containerColor = SurfaceMedium,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
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
        DayOfWeek.MONDAY to "Mo",
        DayOfWeek.TUESDAY to "Tu",
        DayOfWeek.WEDNESDAY to "We",
        DayOfWeek.THURSDAY to "Th",
        DayOfWeek.FRIDAY to "Fr",
        DayOfWeek.SATURDAY to "Sa",
        DayOfWeek.SUNDAY to "Su"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (day, label) ->
            val isSelected = day in selectedDays
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.Checkbox,
                        onClick = { onToggleDay(day) }
                    ),
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    SurfaceCard.copy(alpha = 0.86f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    } else {
                        TextMuted.copy(alpha = 0.14f)
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSectionTitle(
            title = title,
            description = alarmEditSectionDescription(title)
        )
        AppSurfaceCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

private enum class HintTone {
    Neutral,
    Warning,
    Danger
}

@Composable
private fun SettingsHint(
    text: String,
    tone: HintTone = HintTone.Neutral
) {
    val accentColor = when (tone) {
        HintTone.Neutral -> MaterialTheme.colorScheme.primary
        HintTone.Warning -> SnoozeYellow
        HintTone.Danger -> AccentRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (tone) {
                    HintTone.Neutral -> Icons.Default.Info
                    HintTone.Warning -> Icons.Default.WarningAmber
                    HintTone.Danger -> Icons.Default.PriorityHigh
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                text = text,
                color = if (tone == HintTone.Neutral) TextSecondary else accentColor,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeChainPickerSheet(
    currentChain: List<String>,
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var draftChain by remember(currentChain) { mutableStateOf(currentChain) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceMedium,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppSectionTitle(
                title = "Challenge chain",
                description = "Pick the wake-up tasks in the order they should happen. The first item appears first when the alarm rings."
            )

            if (draftChain.isEmpty()) {
                SettingsHint(
                    "Start with two or more challenges when you want dismissal to feel more deliberate than a single math or shake task.",
                    tone = HintTone.Neutral
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    draftChain.forEachIndexed { index, challenge ->
                        AppStatusChip(
                            label = "${index + 1}. ${challenge.toAlarmChallengeSummary()}",
                            color = SnoozeYellow
                        )
                    }
                }
            }

            alarmChallengeOptions()
                .filter { (type, _) -> type != "NONE" }
                .forEach { (type, label) ->
                    val index = draftChain.indexOf(type)
                    val isSelected = index >= 0

                    AppSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = isSelected,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = type.toAlarmChallengeDescription(),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (isSelected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    AppStatusChip(
                                        label = "#${index + 1}",
                                        color = SnoozeYellow
                                    )
                                    IconButton(
                                        onClick = { draftChain = draftChain.moveItem(index, index - 1) },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, null, tint = if (index > 0) TextPrimary else TextMuted)
                                    }
                                    IconButton(
                                        onClick = { draftChain = draftChain.moveItem(index, index + 1) },
                                        enabled = index < draftChain.lastIndex
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = if (index < draftChain.lastIndex) TextPrimary else TextMuted)
                                    }
                                    IconButton(
                                        onClick = { draftChain = draftChain.filterNot { it == type } }
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = AccentRed)
                                    }
                                }
                            } else {
                                TextButton(onClick = { draftChain = draftChain + type }) {
                                    Text("Add", color = AccentBlue)
                                }
                            }
                        }
                    }
                }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { draftChain = emptyList() },
                    enabled = draftChain.isNotEmpty()
                ) {
                    Text("Clear chain", color = if (draftChain.isNotEmpty()) AccentRed else TextMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = { onApply(draftChain) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text(if (draftChain.isEmpty()) "Disable chain" else "Use chain")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun Set<DayOfWeek>.toAlarmRepeatSummary(): String {
    val orderedDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    val weekdaySet = orderedDays.take(5).toSet()
    val weekendSet = orderedDays.takeLast(2).toSet()

    return when {
        isEmpty() -> "One-time"
        size == orderedDays.size -> "Every day"
        this == weekdaySet -> "Weekdays"
        this == weekendSet -> "Weekends"
        else -> orderedDays
            .filter { it in this }
            .joinToString(", ") { day ->
                day.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
                    .take(3)
            }
    }
}

private fun String.toAlarmChallengeSummary(): String = when (this) {
    "NONE" -> "No challenge"
    "MATH_EASY" -> "Easy math"
    "MATH_MEDIUM" -> "Math puzzle"
    "MATH_HARD" -> "Hard math"
    "SHAKE" -> "Shake phone"
    "SEQUENCE" -> "Number sequence"
    "MEMORY_PATTERN" -> "Memory pattern"
    "TYPING" -> "Type phrase"
    "WALK_STEPS" -> "Walk steps"
    "NFC_SCAN" -> "NFC scan"
    "BARCODE_SCAN" -> "Barcode scan"
    "PHOTO_MATCH" -> "Photo match"
    "SQUAT" -> "Squats"
    "WIFI_CONNECT" -> "Wi-Fi connect"
    "MAZE" -> "Maze puzzle"
    else -> replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

private fun String.toAlarmChallengeDescription(): String = when (this) {
    "MATH_EASY" -> "Solve a simple math problem to dismiss."
    "MATH_MEDIUM" -> "Solve a two-operation problem before the alarm stops."
    "MATH_HARD" -> "Use larger numbers and more focus when you need a stronger wake-up."
    "SHAKE" -> "Shake the phone repeatedly to prove you are actually moving."
    "SEQUENCE" -> "Tap numbers in ascending order without missing a step."
    "MEMORY_PATTERN" -> "Memorize and recreate a short visual pattern."
    "TYPING" -> "Type a wake-up phrase accurately before dismissal."
    "WALK_STEPS" -> "Walk a set number of steps to get fully upright."
    "NFC_SCAN" -> "Scan a specific NFC tag placed somewhere away from bed."
    "BARCODE_SCAN" -> "Scan a saved barcode to finish the challenge."
    "PHOTO_MATCH" -> "Match a reference photo so you need to move to the right spot."
    "SQUAT" -> "Complete a quick squat set while holding the phone."
    "WIFI_CONNECT" -> "Connect to a specific Wi-Fi network before the alarm stops."
    "MAZE" -> "Finish a simple maze to prevent sleepy autopilot taps."
    else -> "Dismissal requires this challenge before the alarm can stop."
}

private fun AlarmEditUiState.challengeSummary(): String {
    if (challengeChain.isNotBlank()) {
        val count = challengeChain.split(",")
            .map { it.trim() }
            .count { it.isNotEmpty() }
        if (count > 0) return "$count-step chain"
    }
    return challengeType.toAlarmChallengeSummary()
}

private fun AlarmEditUiState.soundSummary(): String = when {
    internetRadioUrl.isNotBlank() -> "Internet radio"
    spotifyUri.isNotBlank() -> "Spotify"
    ringtoneUri == "silent" -> "Silent wake"
    ringtoneUri.isBlank() -> "Default sound"
    else -> "Custom tone"
}

private fun alarmChallengeOptions(): List<Pair<String, String>> = listOf(
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
    "MAZE" to "Maze Puzzle",
    "COUNT_SHEEP" to "Count the Sheep",
    "SIMON_SAYS" to "Simon Says",
    "DATE_BACKWARDS" to "Type date backwards",
    "STROOP" to "Stroop color test"
)

private fun String.toChallengeChainList(): List<String> = split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

private fun List<String>.toChallengeChainValue(): String = joinToString(",")

private fun List<String>.moveItem(fromIndex: Int, toIndex: Int): List<String> {
    if (fromIndex !in indices || toIndex !in indices) return this
    val updated = toMutableList()
    val item = updated.removeAt(fromIndex)
    updated.add(toIndex, item)
    return updated
}

private fun alarmEditSectionDescription(title: String): String = when (title) {
    "Label" -> "Give the alarm a name that is easy to recognize at a glance."
    "Group" -> "Organize alarms by context so related schedules stay easy to manage."
    "Sound" -> "Shape the tone, volume, and ramp-up behavior of the alarm."
    "Vibration" -> "Control how physical feedback supports the ring pattern."
    "Snooze" -> "Decide how much room this alarm gives you to delay getting up."
    "Dismiss Challenge" -> "Add a wake-up task so dismissing the alarm takes real intent."
    "Wake Effects" -> "Layer in extra visual or physical cues to make waking up harder to ignore."
    "Morning Announcement" -> "Let the alarm speak useful context once you are up."
    "Wake Confirmation" -> "Require a second check-in if this alarm needs extra accountability."
    "Smart Alarm" -> "Allow the alarm to ring inside a window when the timing is more natural."
    "Holidays" -> "Prevent routine alarms from firing when the day should stay flexible."
    "Spotify Ringtone" -> "Use music services when you want a less generic wake-up sound."
    "Philips Hue Sunrise" -> "Coordinate bedside lighting with the alarm for a gentler rise."
    "Mission Chaining" -> "Stack multiple wake-up steps when one challenge is not enough."
    "Anti-Snooze" -> "Add guardrails that make repeated delay harder."
    "Sunrise Simulation" -> "Blend the screen into a brighter pre-wake color transition."
    "Internet Radio" -> "Wake up to a live stream instead of a local ringtone."
    "Guardian Angel" -> "Escalate if missing this alarm has consequences beyond oversleeping."
    "Morning Routine" -> "Capture the first few things you want to do once the alarm is done."
    "Advanced" -> "Fine-tune fallback behavior and edge-case wake-up protections."
    else -> "Review and fine-tune how this alarm behaves."
}
