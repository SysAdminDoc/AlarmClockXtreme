package com.sysadmindoc.alarmclock.ui.alarmedit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.model.ShiftPattern
import com.sysadmindoc.alarmclock.domain.LocationDismissPolicy
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.ringtone.RingtonePickerSheet
import com.sysadmindoc.alarmclock.ui.theme.*
import com.sysadmindoc.alarmclock.util.LocationHelper
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import com.sysadmindoc.alarmclock.worker.GuardianEscalationPolicy
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import java.time.DayOfWeek
import java.util.Locale

internal enum class AlarmEditorPage(
    val title: String,
    val subtitle: String
) {
    OVERVIEW("Alarm overview", "Time, recurrence, label, and current setting summaries."),
    SOUND("Sound & vibration", "Ringtone, volume, fade-in, and vibration behavior."),
    DISMISS("Snooze & dismiss", "Snooze timing, challenges, and dismissal safeguards."),
    SCHEDULE("Schedule & smart wake", "Upcoming rings, holiday policy, and smart wake timing."),
    WAKE("Wake experience", "Wake effects, announcements, confirmation, and morning routine."),
    INTEGRATIONS("Integrations & safety", "Hue, audio services, and guardian escalation."),
    ADVANCED("Advanced behavior", "Shift, solar, hardware, and power-user controls.")
}

private val LocalAlarmEditorPage = staticCompositionLocalOf { AlarmEditorPage.OVERVIEW }

internal fun alarmEditorCategoryColumns(availableWidthDp: Int): Int =
    if (availableWidthDp >= 720) 2 else 1

internal fun alarmEditorPageForSection(title: String): AlarmEditorPage = when (title) {
    "Label", "Group" -> AlarmEditorPage.OVERVIEW
    "Sound", "Vibration" -> AlarmEditorPage.SOUND
    "Snooze", "Dismiss challenge", "Location dismissal lock", "Mission chaining",
    "Anti-snooze" -> AlarmEditorPage.DISMISS
    "Upcoming fire dates", "Smart alarm", "Holidays" -> AlarmEditorPage.SCHEDULE
    "Wake effects", "Morning announcement", "Wake confirmation", "Sunrise simulation",
    "Morning routine" -> AlarmEditorPage.WAKE
    "Spotify ringtone", "Philips Hue sunrise", "Internet radio", "Guardian Angel" ->
        AlarmEditorPage.INTEGRATIONS
    else -> AlarmEditorPage.ADVANCED
}

internal enum class AlarmEditorExitDecision {
    SHOW_OVERVIEW,
    NAVIGATE,
    CONFIRM_DISCARD,
    STAY
}

internal fun alarmEditorExitDecision(
    hasUnsavedChanges: Boolean,
    isSaving: Boolean,
    page: AlarmEditorPage = AlarmEditorPage.OVERVIEW
): AlarmEditorExitDecision = when {
    isSaving -> AlarmEditorExitDecision.STAY
    page != AlarmEditorPage.OVERVIEW -> AlarmEditorExitDecision.SHOW_OVERVIEW
    hasUnsavedChanges -> AlarmEditorExitDecision.CONFIRM_DISCARD
    else -> AlarmEditorExitDecision.NAVIGATE
}

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
    var firingBackgroundStatus by remember { mutableStateOf("") }
    var locationDismissStatus by remember { mutableStateOf("") }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var editorPageName by rememberSaveable { mutableStateOf(AlarmEditorPage.OVERVIEW.name) }
    val editorPage = AlarmEditorPage.entries.firstOrNull { it.name == editorPageName }
        ?: AlarmEditorPage.OVERVIEW
    val editorScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val requestNavigateBack = {
        when (alarmEditorExitDecision(state.hasUnsavedChanges, state.isSaving, editorPage)) {
            AlarmEditorExitDecision.SHOW_OVERVIEW -> editorPageName = AlarmEditorPage.OVERVIEW.name
            AlarmEditorExitDecision.NAVIGATE -> onNavigateBack()
            AlarmEditorExitDecision.CONFIRM_DISCARD -> showDiscardConfirmation = true
            AlarmEditorExitDecision.STAY -> Unit
        }
    }
    BackHandler(enabled = !state.notFound) { requestNavigateBack() }

    LaunchedEffect(editorPage) {
        editorScrollState.scrollTo(0)
    }

    LaunchedEffect(state.hasUnsavedChanges) {
        if (!state.hasUnsavedChanges) showDiscardConfirmation = false
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSaveError()
        }
    }

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
    val firingBackgroundImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            firingBackgroundStatus = "No background image selected."
            return@rememberLauncherForActivityResult
        }
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }.isSuccess
        viewModel.updateFiringBackgroundImage(uri.toString())
        firingBackgroundStatus = if (persisted) {
            "Background image selected."
        } else {
            "Background image selected. Re-select it if Android revokes access."
        }
    }
    val captureLocationDismissTarget = {
        val location = LocationHelper.getLastKnownLocation(context)
        if (location != null) {
            viewModel.updateLocationDismissTarget(location.latitude, location.longitude)
            locationDismissStatus = "Saved current place for location dismissal."
        } else {
            locationDismissStatus = "Could not get a location fix. Turn on Location and try again."
        }
    }
    val locationDismissPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            captureLocationDismissTarget()
        } else {
            locationDismissStatus = "Location permission is required to save this dismissal lock."
        }
    }
    val requestLocationDismissTarget = {
        if (LocationHelper.hasLocationPermission(context)) {
            captureLocationDismissTarget()
        } else {
            locationDismissPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("Your alarm edits have not been saved. Keep editing or discard them.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val editorTitle = if (editorPage == AlarmEditorPage.OVERVIEW) {
                if (state.isEditing) "Edit Alarm" else "New Alarm"
            } else editorPage.title
            val editorSubtitle = if (editorPage == AlarmEditorPage.OVERVIEW) {
                if (state.isEditing) {
                    "Refine timing, sound, and wake-up behavior."
                } else {
                    "Build an alarm that feels intentional from the first ring."
                }
            } else editorPage.subtitle
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = editorTitle,
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
                    IconButton(onClick = requestNavigateBack, enabled = !state.isSaving) {
                        Icon(
                            imageVector = if (editorPage == AlarmEditorPage.OVERVIEW) {
                                Icons.Default.Close
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            contentDescription = if (editorPage == AlarmEditorPage.OVERVIEW) {
                                "Cancel alarm editing"
                            } else {
                                "Back to alarm overview"
                            },
                            tint = TextPrimary
                        )
                    }
                },
                actions = {},
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
        CompositionLocalProvider(LocalAlarmEditorPage provides editorPage) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(editorScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            if (editorPage == AlarmEditorPage.OVERVIEW) {
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
                        .clickable(role = Role.Button) { showTimePicker = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.is24HourFormat) {
                        Text(
                            text = "${String.format("%02d", state.hour)}:${String.format("%02d", state.minute)}",
                            style = ClockTimeLarge,
                            color = TextPrimary
                        )
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            val hour12 = if (state.hour % 12 == 0) 12 else state.hour % 12
                            val amPm = if (state.hour < 12) "AM" else "PM"
                            Text(
                                text = "$hour12:${String.format("%02d", state.minute)}",
                                style = ClockTimeLarge,
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

                // Live "rings in …" affordance so the user doesn't have to do the
                // mental math when picking a time. Sourced from the same forecast
                // the "Upcoming fire dates" section computes.
                val nextFireMillis = state.forecastDates.firstOrNull { !it.skippedByVacation }?.timeMillis
                if (nextFireMillis != null) {
                    val ringCalculator = remember { NextAlarmCalculator() }
                    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(nextFireMillis) {
                        while (true) {
                            nowTick = System.currentTimeMillis()
                            kotlinx.coroutines.delay(30_000)
                        }
                    }
                    val remaining = remember(nextFireMillis, nowTick) {
                        ringCalculator.formatRemaining(nextFireMillis)
                    }
                    Text(
                        text = "Rings in $remaining",
                        color = AccentBlue,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp)
                    )
                }

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
            }

            // Label
            SettingsSection("Label") {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::updateLabel,
                    placeholder = { Text("Alarm label", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
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
                val isCustomGroup = state.group.isNotBlank() && state.group !in defaultGroups
                SettingsRow(label = "Alarm group") {
                    Box {
                        SettingsValueButton(
                            label = if (isCustomGroup) state.group else state.group.ifBlank { "None" },
                            onClick = { showGroupMenu = true }
                        )
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
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Custom…",
                                        color = if (isCustomGroup) AccentBlue else TextMuted
                                    )
                                },
                                onClick = {
                                    // Clear to blank so the field focuses cleanly,
                                    // unless there's already a custom value to edit.
                                    if (!isCustomGroup) viewModel.updateGroup(" ")
                                    showGroupMenu = false
                                }
                            )
                        }
                    }
                }
                // Show custom text field only when a non-preset group is set.
                if (isCustomGroup || (state.group.isNotBlank() && state.group == " ")) {
                    OutlinedTextField(
                        value = state.group.trim(),
                        onValueChange = viewModel::updateGroup,
                        label = { Text("Custom group name", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }
            }

            if (editorPage == AlarmEditorPage.OVERVIEW) {
                AlarmEditorCategoryOverview(
                    state = state,
                    onSelect = { page -> editorPageName = page.name }
                )
            }

            // Sound settings
            SettingsSection("Sound") {
                val hapticOnlyActive = state.overrideSystemVolume && state.volume == 0 && state.vibrationEnabled

                SettingsRow(label = "Don't wake partner") {
                    OutlinedButton(
                        onClick = viewModel::applyDontWakePartnerProfile,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.42f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply")
                    }
                }
                SettingsHint(
                    "Sets this alarm to haptic-only: alarm audio is muted, fade-in is off, and a gentle repeating vibration stays active.",
                    tone = HintTone.Neutral
                )
                if (hapticOnlyActive) {
                    AppStatusChip(
                        label = "Haptic-only profile active",
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        color = AccentBlue,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                SettingsRow(
                    label = "Alarm sound",
                    trailing = {
                        SettingsValueButton(
                            label = when (state.ringtoneUri) {
                                "" -> "Default"
                                "silent" -> "Silent"
                                else -> "Custom"
                            },
                            onClick = { showRingtonePicker = true }
                        )
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
                        Text(
                            if (state.volume == 0) "Muted" else "${state.volume}%",
                            color = AccentBlue
                        )
                    }
                    Slider(
                        value = state.volume.toFloat(),
                        onValueChange = { viewModel.updateVolume(it.toInt()) },
                        valueRange = 0f..100f,
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
                        SettingsValueButton(
                            label = when (state.gradualVolumeSeconds) {
                                0 -> "Off"
                                else -> "${state.gradualVolumeSeconds / 60}m ${state.gradualVolumeSeconds % 60}s"
                            },
                            onClick = { showGradualMenu = true }
                        )
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
                            SettingsValueButton(
                                label = patterns.find { it.first == state.vibrationPattern }?.second?.substringBefore(" (") ?: "Default",
                                onClick = { showPatternMenu = true }
                            )
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

                    if (state.vibrationPattern == "escalating") {
                        var showIntensityMenu by remember { mutableStateOf(false) }
                        val intensities = listOf(1 to "Gentle", 2 to "Strong")
                        SettingsRow(label = "Ramp strength") {
                            Box {
                                SettingsValueButton(
                                    label = intensities.firstOrNull {
                                        it.first == state.vibrationIntensity
                                    }?.second ?: "Strong",
                                    onClick = { showIntensityMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showIntensityMenu,
                                    onDismissRequest = { showIntensityMenu = false }
                                ) {
                                    intensities.forEach { (intensity, label) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    label,
                                                    color = if (intensity == state.vibrationIntensity) {
                                                        AccentBlue
                                                    } else {
                                                        TextPrimary
                                                    }
                                                )
                                            },
                                            onClick = {
                                                viewModel.updateVibrationIntensity(intensity)
                                                showIntensityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        SettingsHint(
                            "Android 16 uses a smooth haptic envelope; older devices use the same gentle-to-strong pulse pattern.",
                            tone = HintTone.Neutral
                        )
                    }

                    // v1.12.0 (roadmap N7): vibration start-delay
                    var showVibDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Start vibration after") {
                        Box {
                            SettingsValueButton(
                                label = when (state.vibrationDelaySeconds) {
                                    0 -> "Immediately"
                                    in 1..59 -> "${state.vibrationDelaySeconds}s"
                                    else -> "${state.vibrationDelaySeconds / 60}m ${state.vibrationDelaySeconds % 60}s"
                                },
                                onClick = { showVibDelayMenu = true }
                            )
                            DropdownMenu(
                                expanded = showVibDelayMenu,
                                onDismissRequest = { showVibDelayMenu = false }
                            ) {
                                listOf(0, 10, 30, 60, 120, 300, 600).forEach { secs ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (secs) {
                                                    0 -> "Immediately (default)"
                                                    in 1..59 -> "$secs seconds"
                                                    else -> "${secs / 60} minutes"
                                                },
                                                color = if (secs == state.vibrationDelaySeconds) AccentBlue else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateVibrationDelay(secs)
                                            showVibDelayMenu = false
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
                        SettingsValueButton(
                            label = "${state.snoozeDurationMinutes} min",
                            onClick = { showSnoozeMenu = true }
                        )
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

            // Schedule forecast
            LaunchedEffect(state.hour, state.minute, state.repeatDays, state.specificDate,
                state.solarOffsetMinutes, state.solarAnchor, state.shiftPattern,
                state.shiftPatternStartDate, state.timezonePolicy, state.fixedTimezoneId,
                state.skipOnHolidays) {
                viewModel.computeForecast()
            }
            SettingsSection("Upcoming fire dates") {
                if (state.forecastDates.isNotEmpty()) {
                    state.forecastDates.forEach { entry ->
                        val instant = java.time.Instant.ofEpochMilli(entry.timeMillis)
                        val dt = instant.atZone(java.time.ZoneId.systemDefault())
                        val dateStr = dt.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                        val timeStr = dt.format(java.time.format.DateTimeFormatter.ofPattern(
                            if (state.is24HourFormat) "HH:mm" else "h:mm a"
                        ))
                        val label = if (entry.skippedByVacation) "$dateStr $timeStr (vacation skip)" else "$dateStr $timeStr"
                        val tone = if (entry.skippedByVacation) HintTone.Warning else HintTone.Neutral
                        SettingsHint(label, tone = tone)
                    }
                } else {
                    SettingsHint("This alarm will not ring with the current settings.", tone = HintTone.Warning)
                }
            }

            CollapsibleGroup(
                title = "Dismiss and wake",
                subtitle = buildList {
                    if (state.challengeType != "NONE") add(state.challengeType.lowercase()
                        .replaceFirstChar { it.uppercase() }.replace("_", " "))
                    if (state.locationDismissEnabled) add("Location lock")
                    if (state.wakeConfirmEnabled) add("Wake confirm")
                    if (state.smartAlarmEnabled) add("Smart alarm")
                }.joinToString(", ").ifEmpty { null },
                initiallyExpanded = state.challengeType != "NONE" ||
                    state.locationDismissEnabled ||
                    state.wakeConfirmEnabled ||
                    state.smartAlarmEnabled
            ) {
            // Dismiss Challenge
            SettingsSection("Dismiss challenge") {
                val challengeOptions = alarmChallengeOptions()
                var expanded by remember { mutableStateOf(false) }

                SettingsRow(label = "Challenge type") {
                    Box {
                        SettingsValueButton(
                            label = challengeOptions.find { it.first == state.challengeType }?.second ?: "None",
                            onClick = { expanded = true }
                        )
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

                // Physical-challenge readiness preflight: surface whether the device
                // has the required hardware, runtime permission, and registered
                // reference for the active challenge (and any chained challenges).
                val challengeReadiness = evaluateActiveChallengeReadiness(
                    challengeType = state.challengeType,
                    challengeChain = state.challengeChain,
                    capabilities = deviceChallengeCapabilities(context),
                    references = ChallengeReferences(
                        nfcTagId = state.nfcTagId,
                        barcodeValue = state.barcodeValue,
                        photoMatchUri = state.photoMatchUri,
                        wifiDismissSsid = state.wifiDismissSsid
                    )
                )
                if (challengeReadiness != null) {
                    if (challengeReadiness.status == ChallengeReadinessStatus.READY) {
                        SettingsHint(
                            "This challenge is ready on your device.",
                            tone = HintTone.Neutral
                        )
                    } else {
                        SettingsHint(
                            challengeReadiness.message,
                            tone = HintTone.Warning
                        )
                    }
                }

                // WALK_STEPS: step count config
                if (state.challengeType == "WALK_STEPS") {
                    var showStepsMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Steps required") {
                        Box {
                            SettingsValueButton(
                                label = "${state.walkStepsRequired} steps",
                                onClick = { showStepsMenu = true }
                            )
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

                // SQUAT: squat count config
                if (state.challengeType == "SQUAT") {
                    var showSquatsMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Squats required") {
                        Box {
                            SettingsValueButton(
                                label = "${state.requiredSquats} squats",
                                onClick = { showSquatsMenu = true }
                            )
                            DropdownMenu(
                                expanded = showSquatsMenu,
                                onDismissRequest = { showSquatsMenu = false }
                            ) {
                                listOf(5, 10, 15, 20, 30, 50).forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text("$count squats", color = if (count == state.requiredSquats) AccentBlue else TextPrimary) },
                                        onClick = { viewModel.updateRequiredSquats(count); showSquatsMenu = false }
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
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    if (state.nfcTagId.isBlank()) {
                        SettingsHint(
                            "No NFC tag registered. This challenge will be skipped at fire time until a tag ID is set.",
                            tone = HintTone.Warning
                        )
                    }
                }

                // BARCODE_SCAN: barcode value field
                if (state.challengeType == "BARCODE_SCAN") {
                    OutlinedTextField(
                        value = state.barcodeValue,
                        onValueChange = viewModel::updateBarcodeValue,
                        label = { Text("Barcode or QR value", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    if (state.barcodeValue.isBlank()) {
                        SettingsHint(
                            "No barcode or QR code registered. This challenge will be skipped at fire time until a value is set.",
                            tone = HintTone.Warning
                        )
                    }
                }

                // WIFI_CONNECT: surface SSID field in context
                if (state.challengeType == "WIFI_CONNECT") {
                    OutlinedTextField(
                        value = state.wifiDismissSsid,
                        onValueChange = viewModel::updateWifiDismissSsid,
                        label = { Text("Wi-Fi network name (SSID)", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    if (state.wifiDismissSsid.isBlank()) {
                        SettingsHint(
                            "No Wi-Fi network specified. Set the SSID the alarm should require you to connect to.",
                            tone = HintTone.Warning
                        )
                    }
                }

                // PHOTO_MATCH: reference photo URI field
                if (state.challengeType == "PHOTO_MATCH") {
                    SettingsRow(label = "Reference photo") {
                        OutlinedButton(
                            onClick = captureReferencePhoto,
                            shape = RoundedCornerShape(12.dp),
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

            SettingsSection("Location dismissal lock") {
                val hasLocationTarget = LocationDismissPolicy.hasTarget(
                    state.locationDismissLat,
                    state.locationDismissLng
                )
                SettingsRow(
                    label = "Require leaving saved place",
                    trailing = {
                        Switch(
                            checked = state.locationDismissEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateLocationDismiss(enabled)
                                if (enabled && !hasLocationTarget) requestLocationDismissTarget()
                            },
                            colors = appSwitchColors()
                        )
                    }
                )
                if (state.locationDismissEnabled) {
                    var showRadiusMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Saved place") {
                        SettingsValueButton(
                            label = if (hasLocationTarget) {
                                formatLocationDismissTarget(
                                    state.locationDismissLat,
                                    state.locationDismissLng
                                )
                            } else {
                                "Not saved"
                            },
                            onClick = requestLocationDismissTarget
                        )
                    }
                    SettingsRow(label = "Unlock radius") {
                        Box {
                            SettingsValueButton(
                                label = "${state.locationDismissRadius} m",
                                onClick = { showRadiusMenu = true }
                            )
                            DropdownMenu(
                                expanded = showRadiusMenu,
                                onDismissRequest = { showRadiusMenu = false }
                            ) {
                                listOf(50, 100, 150, 250, 500, 1_000).forEach { radius ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "$radius meters",
                                                color = if (radius == state.locationDismissRadius) AccentBlue else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateLocationDismissRadius(radius)
                                            showRadiusMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = requestLocationDismissTarget,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasLocationTarget) "Update saved place" else "Save current place")
                    }
                    SettingsHint(
                        text = if (hasLocationTarget) {
                            "Dismiss stays locked until the phone is outside the saved ${state.locationDismissRadius} m radius. Snooze remains available."
                        } else {
                            "Save the place where the alarm starts before enabling this lock."
                        },
                        tone = if (hasLocationTarget) HintTone.Neutral else HintTone.Warning
                    )
                    if (locationDismissStatus.isNotBlank()) {
                        SettingsHint(
                            locationDismissStatus,
                            tone = if (hasLocationTarget) HintTone.Neutral else HintTone.Warning
                        )
                    }
                } else {
                    SettingsHint(
                        "Optional Anti-Sleepyhead mode: dismiss unlocks only after location confirms you left the saved area.",
                        tone = HintTone.Neutral
                    )
                }
            }

            // Wake effects
            SettingsSection("Wake effects") {
                val isGentleWake = state.gradualVolumeSeconds >= 120 &&
                    state.vibrationDelaySeconds >= 60 &&
                    state.sunriseSimulation
                OutlinedButton(
                    onClick = {
                        if (!isGentleWake) {
                            viewModel.updateGradualVolume(120)
                            viewModel.updateVibrationDelay(60)
                            viewModel.updateSunriseSimulation(true)
                            viewModel.updateFlashWake(true)
                        } else {
                            viewModel.updateGradualVolume(60)
                            viewModel.updateVibrationDelay(0)
                            viewModel.updateSunriseSimulation(false)
                            viewModel.updateFlashWake(false)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isGentleWake) DismissGreen else AccentBlue
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(if (isGentleWake) "Gentle wake active" else "Apply gentle wake preset")
                }
                if (!isGentleWake) {
                    SettingsHint(
                        "Sets 2-min volume fade, 1-min vibration delay, and sunrise simulation for a calm wake.",
                        tone = HintTone.Neutral
                    )
                }

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

                SettingsRow(
                    label = "Firing background image",
                    trailing = {
                        SettingsValueButton(
                            label = if (state.firingBackgroundImageUri.isBlank()) "Choose" else "Replace",
                            onClick = { firingBackgroundImageLauncher.launch(arrayOf("image/*")) }
                        )
                    }
                )
                if (state.firingBackgroundImageUri.isBlank()) {
                    SettingsHint(
                        "Optional. The ringing screen keeps the standard gradient until you choose an image.",
                        tone = HintTone.Neutral
                    )
                } else {
                    SettingsRow(
                        label = "Show image while ringing",
                        trailing = {
                            Switch(
                                checked = state.firingBackgroundImageEnabled,
                                onCheckedChange = viewModel::updateFiringBackgroundImageEnabled,
                                colors = appSwitchColors()
                            )
                        }
                    )
                    SettingsRow(
                        label = "Blur image on Android 12+",
                        trailing = {
                            Switch(
                                checked = state.firingBackgroundBlurEnabled,
                                enabled = state.firingBackgroundImageEnabled,
                                onCheckedChange = viewModel::updateFiringBackgroundBlur,
                                colors = appSwitchColors()
                            )
                        }
                    )
                    OutlinedButton(
                        onClick = viewModel::clearFiringBackgroundImage,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Clear background image")
                    }
                    SettingsHint(
                        if (state.firingBackgroundImageEnabled) {
                            "Ringing screen image is enabled for this alarm."
                        } else {
                            "Image is saved for this alarm but the ringing screen still uses the standard gradient."
                        },
                        tone = HintTone.Neutral
                    )
                }
                if (firingBackgroundStatus.isNotBlank()) {
                    SettingsHint(firingBackgroundStatus, tone = HintTone.Neutral)
                }
            }

            // Morning Announcement (TTS)
            SettingsSection("Morning announcement") {
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
            SettingsSection("Wake confirmation") {
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
                            SettingsValueButton(
                                label = "${state.wakeConfirmDelayMinutes} min",
                                onClick = { showDelayMenu = true }
                            )
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
            SettingsSection("Smart alarm") {
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
                            SettingsValueButton(
                                label = "${state.smartAlarmWindowMinutes} min before",
                                onClick = { showWindowMenu = true }
                            )
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
                        "Uses conservative phone-motion scoring and waits for enough evidence before firing early.",
                        tone = HintTone.Neutral
                    )
                }
            }
            }

            CollapsibleGroup(
                title = "Extras and integrations",
                subtitle = buildList {
                    if (state.skipOnHolidays) add("Holiday skip")
                    if (state.hueEnabled) add("Hue")
                    if (state.guardianEnabled) add("Guardian")
                    if (state.progressiveSnooze) add("Progressive snooze")
                }.joinToString(", ").ifEmpty { null }
            ) {
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
            SettingsSection("Spotify ringtone") {
                OutlinedTextField(
                    value = state.spotifyUri,
                    onValueChange = viewModel::updateSpotifyUri,
                    label = { Text("Spotify URI (e.g. spotify:track:...)", color = TextMuted) },
                    placeholder = { Text("Leave blank to use default ringtone", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                SettingsHint(
                    "Requires Spotify installed. Falls back to default ringtone if unavailable.",
                    tone = HintTone.Warning
                )
            }

            // Philips Hue Sunrise
            SettingsSection("Philips Hue sunrise") {
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
                            SettingsValueButton(
                                label = "${state.huePreWakeMinutes} min before",
                                onClick = { showHueMenu = true }
                            )
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
            SettingsSection("Mission chaining") {
                val chainItems = state.challengeChain.toChallengeChainList()
                SettingsRow(
                    label = "Challenge chain",
                    trailing = {
                        SettingsValueButton(
                            label = if (chainItems.isEmpty()) "Choose" else "${chainItems.size} challenges",
                            onClick = { showChainPicker = true }
                        )
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
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                SettingsHint(
                    "Stack multiple challenges in sequence. Use the picker above for a guided setup, or edit the raw chain directly if you already know the codes.",
                    tone = HintTone.Neutral
                )
                if (chainItems.isNotEmpty()) {
                    val missingRefs = buildList {
                        if ("NFC_SCAN" in chainItems && state.nfcTagId.isBlank()) add("NFC tag ID")
                        if ("BARCODE_SCAN" in chainItems && state.barcodeValue.isBlank()) add("barcode value")
                        if ("PHOTO_MATCH" in chainItems && state.photoMatchUri.isBlank()) add("reference photo")
                        if ("WIFI_CONNECT" in chainItems && state.wifiDismissSsid.isBlank()) add("Wi-Fi SSID")
                    }
                    if (missingRefs.isNotEmpty()) {
                        SettingsHint(
                            "Missing ${missingRefs.joinToString(", ")}. These challenges will be skipped at fire time.",
                            tone = HintTone.Warning
                        )
                    }
                }
            }

            // v1.2.0: Anti-Snooze Features
            SettingsSection("Anti-snooze") {
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
                            SettingsValueButton(
                                label = "${state.backupSoundDelaySec}s",
                                onClick = { showDelayMenu = true }
                            )
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
                if (state.flashlightStrobe) {
                    SettingsHint(
                        "Warning: rapid flashing can trigger seizures or discomfort. Reduce motion and flashing in Settings disables this effect.",
                        tone = HintTone.Warning
                    )
                }
            }

            // v1.2.0: Sunrise Simulation
            SettingsSection("Sunrise simulation") {
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
                            SettingsValueButton(
                                label = "${state.sunriseMinutes} min",
                                onClick = { showMenu = true }
                            )
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
            SettingsSection("Internet radio") {
                OutlinedTextField(
                    value = state.internetRadioUrl,
                    onValueChange = viewModel::updateInternetRadioUrl,
                    label = { Text("Stream URL (http://...)", color = TextMuted) },
                    placeholder = { Text("Leave blank for default ringtone", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
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
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    var showDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = "Alert after") {
                        Box {
                            SettingsValueButton(
                                label = "${state.guardianDelaySec / 60} min",
                                onClick = { showDelayMenu = true }
                            )
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
                    val guardianReadiness = GuardianEscalationPolicy.readiness(
                        flavor = BuildConfig.FLAVOR,
                        enabledAlarmCount = 1,
                        hasSendSmsPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED,
                        hasCallPhonePermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                    SettingsHint(
                        guardianEditHint(guardianReadiness),
                        tone = if (guardianReadiness.needsUserAction) HintTone.Warning else HintTone.Danger
                    )
                }
            }

            // v1.2.0: Morning Routine
            SettingsSection("Morning routine") {
                OutlinedTextField(
                    value = state.morningRoutine,
                    onValueChange = viewModel::updateMorningRoutine,
                    label = { Text("Checklist items (one per line)", color = TextMuted) },
                    placeholder = { Text("Stretch\nDrink water\nBrush teeth", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    minLines = 3, maxLines = 6
                )
                SettingsHint(
                    "Shown as a checklist after dismissal on the morning briefing screen.",
                    tone = HintTone.Neutral
                )
            }
            }

            // v1.2.0: Advanced
            SettingsSection("Advanced") {
                SettingsRow(label = "Alarm profile") {
                    OutlinedTextField(
                        value = state.profileName,
                        onValueChange = viewModel::updateProfileName,
                        placeholder = { Text("e.g. Work, Travel, Weekend", color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.width(180.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = state.specificDate,
                    onValueChange = viewModel::updateSpecificDate,
                    label = { Text("Specific date (YYYY-MM-DD, leave blank for repeat days)", color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                var showTimezonePolicyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Time zone") {
                    Box {
                        SettingsValueButton(
                            label = if (state.timezonePolicy == Alarm.TIMEZONE_POLICY_FIXED) {
                                "Fixed zone"
                            } else {
                                "Follow device"
                            },
                            onClick = { showTimezonePolicyMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTimezonePolicyMenu,
                            onDismissRequest = { showTimezonePolicyMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Follow device time zone") },
                                onClick = {
                                    viewModel.updateTimezonePolicy(Alarm.TIMEZONE_POLICY_LOCAL)
                                    showTimezonePolicyMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Keep time in a fixed zone") },
                                onClick = {
                                    viewModel.updateTimezonePolicy(Alarm.TIMEZONE_POLICY_FIXED)
                                    showTimezonePolicyMenu = false
                                }
                            )
                        }
                    }
                }
                if (state.timezonePolicy == Alarm.TIMEZONE_POLICY_FIXED) {
                    val zoneIsValid = remember(state.fixedTimezoneId) {
                        runCatching { java.time.ZoneId.of(state.fixedTimezoneId.trim()) }.isSuccess
                    }
                    OutlinedTextField(
                        value = state.fixedTimezoneId,
                        onValueChange = viewModel::updateFixedTimezoneId,
                        label = { Text("IANA time zone (for example America/New_York)", color = TextMuted) },
                        supportingText = {
                            Text(
                                if (zoneIsValid) {
                                    "The alarm stays at ${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')} in this zone when you travel."
                                } else {
                                    "Unknown zone; saving will safely fall back to following the device."
                                }
                            )
                        },
                        isError = !zoneIsValid,
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }
                var showShiftPatternMenu by remember { mutableStateOf(false) }
                val selectedShiftPattern = ShiftPattern.fromKey(state.shiftPattern)
                SettingsRow(label = "Shift pattern") {
                    Box {
                        SettingsValueButton(
                            label = selectedShiftPattern?.title ?: "Disabled",
                            onClick = { showShiftPatternMenu = true }
                        )
                        DropdownMenu(
                            expanded = showShiftPatternMenu,
                            onDismissRequest = { showShiftPatternMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Disabled") },
                                onClick = {
                                    viewModel.updateShiftPattern("")
                                    showShiftPatternMenu = false
                                }
                            )
                            ShiftPattern.presets.forEach { pattern ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(pattern.title)
                                            Text(
                                                pattern.description,
                                                color = TextMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateShiftPattern(pattern.key)
                                        showShiftPatternMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = selectedShiftPattern != null) {
                    Column {
                        OutlinedTextField(
                            value = state.shiftPatternStartDate,
                            onValueChange = viewModel::updateShiftPatternStartDate,
                            label = { Text("Shift cycle start date (YYYY-MM-DD)", color = TextMuted) },
                            colors = appOutlinedTextFieldColors(),
                            shape = AppInputShape,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            singleLine = true
                        )
                        SettingsHint(
                            "Day, night, and work entries ring; off days are skipped. Weekday picks still narrow the pattern.",
                            tone = HintTone.Neutral
                        )
                    }
                }
                var showWeatherEarlyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Weather early fire") {
                    Box {
                        SettingsValueButton(
                            label = if (state.weatherEarlyMinutes == 0) "Disabled" else "${state.weatherEarlyMinutes} min",
                            onClick = { showWeatherEarlyMenu = true }
                        )
                        DropdownMenu(expanded = showWeatherEarlyMenu, onDismissRequest = { showWeatherEarlyMenu = false }) {
                            listOf(0, 10, 15, 20, 30).forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text(if (mins == 0) "Disabled" else "$mins minutes earlier") },
                                    onClick = { viewModel.updateWeatherEarlyMinutes(mins); showWeatherEarlyMenu = false }
                                )
                            }
                        }
                    }
                }
                SettingsHint(
                    "Fire this alarm earlier when snow, freezing rain, or ice is forecast. Uses cached weather data.",
                    tone = HintTone.Neutral
                )

                var showEarlyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Early dismiss window") {
                    Box {
                        SettingsValueButton(
                            label = if (state.earlyDismissMinutes == 0) "Disabled" else "${state.earlyDismissMinutes} min",
                            onClick = { showEarlyMenu = true }
                        )
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
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )

                // v1.4.0: Hardware-button action (Volume/Camera/Headset-hook keys
                // during firing). NONE = normal volume control passes through.
                var showHwMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Hardware-button action") {
                    Box {
                        SettingsValueButton(
                            label = state.hardwareButtonAction.lowercase().replaceFirstChar { it.uppercase() },
                            onClick = { showHwMenu = true }
                        )
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

                // v1.10.3: Deliberate dismiss confirmation for users who
                // accidentally swipe ready alarms while half-awake.
                SettingsRow(label = "Hold to dismiss") {
                    Switch(
                        checked = state.holdToDismissEnabled,
                        onCheckedChange = viewModel::updateHoldToDismiss,
                        colors = appSwitchColors()
                    )
                }
                SettingsHint(
                    "When enabled, the firing screen requires holding Dismiss for 1.5 seconds. Swipe-left dismissal is replaced with a visible hold prompt.",
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

                // v1.4.0: Ringtone pool (anti-habituation).
                // v1.12.2 (roadmap N9): chip-based editor replaces the
                // newline-separated textarea. Each pool entry renders as a
                // removable chip; an Add button surfaces a small URI input
                // dialog (kept lightweight so the power-user paste flow still
                // works without a full file-picker round-trip). Stored format
                // on disk is unchanged (comma-separated string) so the
                // sanitiser + Service consumer keep working with no churn.
                val ringtonePoolEntries = remember(state.ringtonePool) {
                    state.ringtonePool.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
                var showAddRingtoneDialog by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Ringtone pool",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ringtonePoolEntries.forEach { uri ->
                            val shortName = ringtoneShortName(uri)
                            AppFilterChip(
                                label = shortName,
                                selected = true,
                                leadingIcon = Icons.Default.Close,
                                selectionSemantics = false,
                                accessibilityLabel = "Remove $shortName from ringtone pool",
                                onClick = {
                                    val next = ringtonePoolEntries.filterNot { it == uri }.joinToString(",")
                                    viewModel.updateRingtonePool(next)
                                }
                            )
                        }
                        AppFilterChip(
                            label = "Add ringtone",
                            selected = false,
                            accessibilityLabel = "Add ringtone to pool",
                            onClick = { showAddRingtoneDialog = true }
                        )
                    }
                }
                SettingsHint(
                    "When the pool is non-empty, a random entry is picked each fire and overrides the single Ringtone setting above. Tap a chip to remove it.",
                    tone = HintTone.Neutral
                )
                if (showAddRingtoneDialog) {
                    var newUri by remember { mutableStateOf("") }
                    val trimmedUri = newUri.trim()
                    val duplicateUri = trimmedUri in ringtonePoolEntries
                    val canAddRingtone = trimmedUri.isNotEmpty() && !duplicateUri
                    AlertDialog(
                        onDismissRequest = { showAddRingtoneDialog = false },
                        title = { Text("Add ringtone to pool") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Paste a content:// URI or file:// path. A random pool entry plays each time this alarm fires.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = newUri,
                                    onValueChange = { newUri = it },
                                    label = { Text("Ringtone URI") },
                                    placeholder = { Text("content://... or file://...", color = TextMuted) },
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    shape = AppInputShape,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (duplicateUri) {
                                    Text(
                                        text = "That ringtone is already in the pool.",
                                        color = AccentRed,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = canAddRingtone,
                                onClick = {
                                    val next = (ringtonePoolEntries + trimmedUri).joinToString(",")
                                    viewModel.updateRingtonePool(next)
                                    showAddRingtoneDialog = false
                                }
                            ) { Text("Add ringtone") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddRingtoneDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                // v1.5.0: Sunrise/sunset-relative firing. Overrides the clock time
                // when offset is non-zero; uses last-known location for the solar
                // calc (cached by weather pulls) with a sensible fallback to clock.
                var showAnchorMenu by remember { mutableStateOf(false) }
                SettingsRow(label = "Solar anchor") {
                    Box {
                        SettingsValueButton(
                            label = state.solarAnchor.lowercase().replaceFirstChar { it.uppercase() },
                            onClick = { showAnchorMenu = true }
                        )
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
                        val solarOffsetLabel = when {
                            state.solarOffsetMinutes == 0 -> "Off (use clock time)"
                            state.solarOffsetMinutes > 0 -> "+${state.solarOffsetMinutes} min"
                            else -> "${state.solarOffsetMinutes} min"
                        }
                        SettingsValueButton(
                            label = solarOffsetLabel,
                            onClick = { showOffsetMenu = true }
                        )
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
                    "When the offset is non-zero, the alarm fires relative to the selected sunrise or sunset event at your last known location.",
                    tone = HintTone.Neutral
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            }
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
                    Text("Save time", color = AccentBlue)
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
                        label = if (state.is24HourFormat) "24-hour time" else "12-hour time",
                        icon = Icons.Default.Schedule,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Choose alarm time", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
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
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(12.dp),
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
private fun AlarmEditorCategoryOverview(
    state: AlarmEditUiState,
    onSelect: (AlarmEditorPage) -> Unit
) {
    val repeatSummary = state.repeatDays.toAlarmRepeatSummary()
    val baseScheduleSummary = when {
        state.specificDate.isNotBlank() -> "$repeatSummary • Specific date set"
        state.smartAlarmEnabled -> "$repeatSummary • Smart ${state.smartAlarmWindowMinutes} min window"
        else -> "$repeatSummary • Fixed wake time"
    }
    val scheduleSummary = if (state.timezonePolicy == Alarm.TIMEZONE_POLICY_FIXED) {
        "$baseScheduleSummary • ${state.fixedTimezoneId.ifBlank { "Invalid fixed zone" }}"
    } else {
        "$baseScheduleSummary • Follows device zone"
    }
    val wakeFeatures = listOfNotNull(
        "Flash".takeIf { state.flashWake || state.flashlightStrobe },
        "Sunrise".takeIf { state.sunriseSimulation },
        "Announcement".takeIf { state.ttsEnabled },
        "Routine".takeIf { state.morningRoutine.isNotBlank() }
    )
    val integrationCount = listOf(
        state.spotifyUri.isNotBlank(),
        state.hueEnabled,
        state.internetRadioUrl.isNotBlank(),
        state.guardianEnabled
    ).count { it }
    val advancedFeatures = listOfNotNull(
        "Profile".takeIf { state.profileName.isNotBlank() },
        "Shift pattern".takeIf { state.shiftPattern.isNotBlank() },
        "Solar timing".takeIf { state.solarOffsetMinutes != 0 },
        "Wi-Fi rule".takeIf { state.wifiDismissSsid.isNotBlank() }
    )
    val categories = listOf(
        AlarmEditorCategory(
            page = AlarmEditorPage.SOUND,
            title = "Sound & vibration",
            summary = "${state.soundSummary()} • ${if (state.vibrationEnabled) "Vibration on" else "Vibration off"}",
            icon = Icons.AutoMirrored.Filled.VolumeUp
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.DISMISS,
            title = "Snooze & dismiss",
            summary = "${state.challengeSummary()} • ${state.snoozeDurationMinutes} min snooze",
            icon = Icons.Default.TaskAlt
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.SCHEDULE,
            title = "Schedule & smart wake",
            summary = scheduleSummary,
            icon = Icons.Default.CalendarMonth
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.WAKE,
            title = "Wake experience",
            summary = wakeFeatures.takeIf { it.isNotEmpty() }?.joinToString(" • ") ?: "Standard wake",
            icon = Icons.Default.WbSunny
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.INTEGRATIONS,
            title = "Integrations & safety",
            summary = if (integrationCount == 0) "No optional integrations" else "$integrationCount active",
            icon = Icons.Default.Hub
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.ADVANCED,
            title = "Advanced behavior",
            summary = advancedFeatures.takeIf { it.isNotEmpty() }?.joinToString(" • ") ?: "Using defaults",
            icon = Icons.Default.Tune
        )
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSectionTitle(
            title = "Alarm settings",
            description = "Open a category to adjust it. Your current choices stay visible here."
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = alarmEditorCategoryColumns(maxWidth.value.toInt())
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.chunked(columns).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { category ->
                            Box(modifier = Modifier.weight(1f)) {
                                AlarmEditorCategoryCard(category, onSelect)
                            }
                        }
                        repeat(columns - rowCategories.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private data class AlarmEditorCategory(
    val page: AlarmEditorPage,
    val title: String,
    val summary: String,
    val icon: ImageVector
)

@Composable
private fun AlarmEditorCategoryCard(
    category: AlarmEditorCategory,
    onSelect: (AlarmEditorPage) -> Unit
) {
    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${category.title}. ${category.summary}"
            }
            .clickable(role = Role.Button) { onSelect(category.page) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = category.summary,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    if (alarmEditorPageForSection(title) != LocalAlarmEditorPage.current) return
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
private fun CollapsibleGroup(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val editorPage = LocalAlarmEditorPage.current
    val focusedPages = when (title) {
        "Dismiss and wake" -> setOf(
            AlarmEditorPage.DISMISS,
            AlarmEditorPage.SCHEDULE,
            AlarmEditorPage.WAKE
        )
        "Extras and integrations" -> setOf(
            AlarmEditorPage.DISMISS,
            AlarmEditorPage.SCHEDULE,
            AlarmEditorPage.WAKE,
            AlarmEditorPage.INTEGRATIONS
        )
        else -> null
    }
    if (focusedPages != null) {
        if (editorPage !in focusedPages) return
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
        return
    }

    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            color = SurfaceLight.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextMuted
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
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
        shape = RoundedCornerShape(12.dp),
        color = SurfaceLight.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, BorderSubtle)
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
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun SettingsValueButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 40.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
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
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
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
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move $label earlier",
                                            tint = if (index > 0) TextPrimary else TextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { draftChain = draftChain.moveItem(index, index + 1) },
                                        enabled = index < draftChain.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move $label later",
                                            tint = if (index < draftChain.lastIndex) TextPrimary else TextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { draftChain = draftChain.filterNot { it == type } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $label",
                                            tint = AccentRed
                                        )
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
                        shape = RoundedCornerShape(12.dp),
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
    "VOICE_PHRASE" -> "Voice phrase"
    "HANDWRITING" -> "Handwriting"
    "WALK_STEPS" -> "Walk steps"
    "NFC_SCAN" -> "NFC scan"
    "BARCODE_SCAN" -> "Barcode scan"
    "PHOTO_MATCH" -> "Photo match"
    "SQUAT" -> "Squats"
    "PUSH_UP" -> "Push-ups"
    "PLANK_HOLD" -> "Plank hold"
    "WIFI_CONNECT" -> "Wi-Fi connect"
    "MAZE" -> "Maze puzzle"
    "ROCK_PAPER_SCISSORS" -> "RPS (best-of-5)"
    "EMOJI_MEMORY" -> "Emoji memory"
    "TYPING_SPEED" -> "Typing speed"
    "WORDLE" -> "Wordle"
    "PVT" -> "Reaction test"
    "SPOT_DIFFERENCE" -> "Spot difference"
    "CHESS_MATE" -> "Mate in one"
    "RSVP_READING" -> "RSVP reading"
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
    "VOICE_PHRASE" -> "Say a wake-up phrase with Android speech recognition, with typed fallback."
    "HANDWRITING" -> "Draw a wake-up word with on-device handwriting recognition and typed fallback."
    "WALK_STEPS" -> "Walk a set number of steps to get fully upright."
    "NFC_SCAN" -> "Scan a specific NFC tag placed somewhere away from bed."
    "BARCODE_SCAN" -> "Scan a saved barcode to finish the challenge."
    "PHOTO_MATCH" -> "Match a reference photo so you need to move to the right spot."
    "SQUAT" -> "Complete a quick squat set while holding the phone."
    "PUSH_UP" -> "Do push-ups over the phone placed face-down on the floor."
    "PLANK_HOLD" -> "Hold the phone level in a plank position for 30 seconds."
    "WIFI_CONNECT" -> "Connect to a specific Wi-Fi network before the alarm stops."
    "MAZE" -> "Finish a simple maze to prevent sleepy autopilot taps."
    "ROCK_PAPER_SCISSORS" -> "Win 3 rounds of RPS against the computer to dismiss."
    "EMOJI_MEMORY" -> "Memorise 8 pairs on a 4x4 grid, then find them all face-down."
    "TYPING_SPEED" -> "Type a short phrase at 15+ wpm with at most 2 word errors."
    "WORDLE" -> "Guess a hidden 5-letter word in up to 6 tries."
    "PVT" -> "Tap a target 5 times as fast as you can. Average under 500 ms to dismiss."
    "SPOT_DIFFERENCE" -> "Compare two color grids and tap the single changed tile."
    "CHESS_MATE" -> "Pick the mate-in-1 move from a small chess position."
    "RSVP_READING" -> "Read a rapid word stream, then identify a word that appeared."
    else -> "Dismissal requires this challenge before the alarm can stop."
}

private fun formatLocationDismissTarget(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

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

private fun guardianEditHint(readiness: GuardianReadiness): String {
    val callPath = if (readiness.hasCallPhonePermission) {
        "Direct call permission is granted."
    } else {
        "Call fallback opens the dialer because CALL_PHONE is not granted."
    }
    return when (readiness.smsPath) {
        GuardianSmsPath.INACTIVE -> "Guardian Angel is off for this alarm."
        GuardianSmsPath.DIRECT_SMS ->
            "Sends an emergency SMS automatically, then opens the call path if the alarm is not dismissed. $callPath"
        GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION ->
            "F-Droid can send automatic SMS after SEND_SMS is allowed. Until then, it opens a prefilled SMS composer. $callPath"
        GuardianSmsPath.SMS_COMPOSER ->
            "Opens a prefilled emergency SMS composer if the alarm is not dismissed, then falls back to call or dialer. $callPath"
    }
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
    "VOICE_PHRASE" to "Voice Phrase",
    "HANDWRITING" to "Handwriting",
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
    "STROOP" to "Stroop color test",
    "ROCK_PAPER_SCISSORS" to "Rock Paper Scissors",
    "EMOJI_MEMORY" to "Emoji Memory",
    "TYPING_SPEED" to "Typing Speed",
    "WORDLE" to "Wordle",
    "PVT" to "Reaction Test",
    "SPOT_DIFFERENCE" to "Spot the Difference",
    "CHESS_MATE" to "Chess Mate in 1",
    "RSVP_READING" to "RSVP Speed Reading",
    "PUSH_UP" to "Push-ups",
    "PLANK_HOLD" to "Plank Hold"
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
    "Dismiss challenge" -> "Add a wake-up task so dismissing the alarm takes real intent."
    "Location dismissal lock" -> "Keep Dismiss locked until the phone leaves the saved area."
    "Wake effects" -> "Layer in extra visual or physical cues to make waking up harder to ignore."
    "Morning announcement" -> "Let the alarm speak useful context once you are up."
    "Wake confirmation" -> "Require a second check-in if this alarm needs extra accountability."
    "Smart alarm" -> "Allow the alarm to ring inside a window when the timing is more natural."
    "Holidays" -> "Prevent routine alarms from firing when the day should stay flexible."
    "Spotify ringtone" -> "Use music services when you want a less generic wake-up sound."
    "Philips Hue sunrise" -> "Coordinate bedside lighting with the alarm for a gentler rise."
    "Mission chaining" -> "Stack multiple wake-up steps when one challenge is not enough."
    "Anti-snooze" -> "Add guardrails that make repeated delay harder."
    "Sunrise simulation" -> "Blend the screen into a brighter pre-wake color transition."
    "Internet radio" -> "Wake up to a live stream instead of a local ringtone."
    "Guardian Angel" -> "Escalate if missing this alarm has consequences beyond oversleeping."
    "Morning routine" -> "Capture the first few things you want to do once the alarm is done."
    "Advanced" -> "Fine-tune fallback behavior and edge-case wake-up protections."
    else -> "Review and fine-tune how this alarm behaves."
}

/**
 * v1.12.2 (roadmap N9): pick a short, human-friendly label for a ringtone
 * pool entry. We can't resolve content:// URIs to track titles without
 * touching ContentResolver per-render, so we trim aggressively at the
 * structural boundary instead:
 *   - "content://media/external/audio/media/12345" → "audio/12345"
 *   - "file:///storage/emulated/0/Music/sun.mp3" → "sun.mp3"
 * Anything pathologically long gets truncated with an ellipsis.
 */
private fun ringtoneShortName(uri: String): String {
    val trimmed = uri.trim()
    if (trimmed.isEmpty()) return "(empty)"
    val fileName = trimmed
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { trimmed }
    val safe = fileName.take(28)
    return if (fileName.length > 28) "$safe…" else safe
}
