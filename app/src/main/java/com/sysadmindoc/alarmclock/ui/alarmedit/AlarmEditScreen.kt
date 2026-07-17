package com.sysadmindoc.alarmclock.ui.alarmedit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
import com.sysadmindoc.alarmclock.R
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
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int
) {
    OVERVIEW(R.string.alarm_edit_page_overview, R.string.alarm_edit_page_overview_subtitle),
    SOUND(R.string.alarm_edit_page_sound, R.string.alarm_edit_page_sound_subtitle),
    DISMISS(R.string.alarm_edit_page_dismiss, R.string.alarm_edit_page_dismiss_subtitle),
    SCHEDULE(R.string.alarm_edit_page_schedule, R.string.alarm_edit_page_schedule_subtitle),
    WAKE(R.string.alarm_edit_page_wake, R.string.alarm_edit_page_wake_subtitle),
    INTEGRATIONS(R.string.alarm_edit_page_integrations, R.string.alarm_edit_page_integrations_subtitle),
    ADVANCED(R.string.alarm_edit_page_advanced, R.string.alarm_edit_page_advanced_subtitle)
}

internal enum class AlarmEditorSection(
    val page: AlarmEditorPage,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
) {
    LABEL(AlarmEditorPage.OVERVIEW, R.string.label, R.string.alarm_edit_section_label_description),
    GROUP(AlarmEditorPage.OVERVIEW, R.string.alarm_edit_group, R.string.alarm_edit_section_group_description),
    SOUND(AlarmEditorPage.SOUND, R.string.alarm_edit_sound, R.string.alarm_edit_section_sound_description),
    VIBRATION(AlarmEditorPage.SOUND, R.string.vibration, R.string.alarm_edit_section_vibration_description),
    SNOOZE(AlarmEditorPage.DISMISS, R.string.alarm_edit_snooze, R.string.alarm_edit_section_snooze_description),
    UPCOMING(AlarmEditorPage.SCHEDULE, R.string.alarm_edit_upcoming_dates, R.string.alarm_edit_section_upcoming_description),
    DISMISS_CHALLENGE(AlarmEditorPage.DISMISS, R.string.dismiss_challenge, R.string.alarm_edit_section_challenge_description),
    LOCATION(AlarmEditorPage.DISMISS, R.string.alarm_edit_location_lock, R.string.alarm_edit_section_location_description),
    WAKE_EFFECTS(AlarmEditorPage.WAKE, R.string.alarm_edit_wake_effects, R.string.alarm_edit_section_wake_effects_description),
    ANNOUNCEMENT(AlarmEditorPage.WAKE, R.string.alarm_edit_announcement, R.string.alarm_edit_section_announcement_description),
    WAKE_CONFIRM(AlarmEditorPage.WAKE, R.string.alarm_edit_wake_confirmation, R.string.alarm_edit_section_wake_confirmation_description),
    SMART_ALARM(AlarmEditorPage.SCHEDULE, R.string.alarm_edit_smart_alarm, R.string.alarm_edit_section_smart_alarm_description),
    HOLIDAYS(AlarmEditorPage.SCHEDULE, R.string.alarm_edit_holidays, R.string.alarm_edit_section_holidays_description),
    SPOTIFY(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_spotify, R.string.alarm_edit_section_spotify_description),
    HUE(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_hue, R.string.alarm_edit_section_hue_description),
    CHAIN(AlarmEditorPage.DISMISS, R.string.alarm_edit_mission_chain, R.string.alarm_edit_section_chain_description),
    ANTI_SNOOZE(AlarmEditorPage.DISMISS, R.string.alarm_edit_anti_snooze, R.string.alarm_edit_section_anti_snooze_description),
    SUNRISE(AlarmEditorPage.WAKE, R.string.alarm_edit_sunrise, R.string.alarm_edit_section_sunrise_description),
    RADIO(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_internet_radio, R.string.alarm_edit_section_radio_description),
    GUARDIAN(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_guardian, R.string.alarm_edit_section_guardian_description),
    ROUTINE(AlarmEditorPage.WAKE, R.string.alarm_edit_morning_routine, R.string.alarm_edit_section_routine_description),
    ADVANCED(AlarmEditorPage.ADVANCED, R.string.alarm_edit_advanced, R.string.alarm_edit_section_advanced_description)
}

private val LocalAlarmEditorPage = staticCompositionLocalOf { AlarmEditorPage.OVERVIEW }

internal fun alarmEditorCategoryColumns(availableWidthDp: Int): Int =
    if (availableWidthDp >= 720) 2 else 1

internal data class AlarmNumpadTime(val hour: Int, val minute: Int)

internal fun parseAlarmNumpadTime(
    digits: String,
    is24Hour: Boolean,
    isPm: Boolean
): AlarmNumpadTime? {
    if (digits.length != 4 || digits.any { !it.isDigit() }) return null
    val enteredHour = digits.take(2).toInt()
    val minute = digits.takeLast(2).toInt()
    if (minute !in 0..59) return null

    val hour = if (is24Hour) {
        enteredHour.takeIf { it in 0..23 } ?: return null
    } else {
        if (enteredHour !in 1..12) return null
        (enteredHour % 12) + if (isPm) 12 else 0
    }
    return AlarmNumpadTime(hour, minute)
}

// v1.13.15: seed the numpad buffer from an existing time so sticky numpad mode
// round-trips through parseAlarmNumpadTime (12h entry uses display hours 1-12).
internal fun formatAlarmNumpadDigits(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) {
        "%02d%02d".format(hour, minute)
    } else {
        "%02d%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute)
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
    var useTimeNumpad by rememberSaveable { mutableStateOf(false) }
    var timeNumpadDigits by rememberSaveable { mutableStateOf("") }
    var timeNumpadIsPm by rememberSaveable { mutableStateOf(false) }
    var showRingtonePicker by remember { mutableStateOf(false) }
    var showChainPicker by remember { mutableStateOf(false) }
    var photoReferenceStatus by remember { mutableStateOf("") }
    var firingBackgroundStatus by remember { mutableStateOf("") }
    var locationDismissStatus by remember { mutableStateOf("") }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var editorPageName by rememberSaveable { mutableStateOf(AlarmEditorPage.OVERVIEW.name) }
    val editorPage = AlarmEditorPage.entries.firstOrNull { it.name == editorPageName }
        ?: AlarmEditorPage.OVERVIEW
    val editorScrollState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val noReferencePhotoMessage = stringResource(R.string.alarm_edit_photo_none_captured)
    val referencePhotoSavedMessage = stringResource(R.string.alarm_edit_photo_saved)
    val referencePhotoSaveFailedMessage = stringResource(R.string.alarm_edit_photo_save_failed)
    val cameraPermissionMessage = stringResource(R.string.alarm_edit_camera_permission_required)
    val noBackgroundMessage = stringResource(R.string.alarm_edit_background_none_selected)
    val backgroundSelectedMessage = stringResource(R.string.alarm_edit_background_selected)
    val backgroundPermissionMessage = stringResource(R.string.alarm_edit_background_permission_warning)
    val locationSavedMessage = stringResource(R.string.alarm_edit_location_saved)
    val locationFixFailedMessage = stringResource(R.string.alarm_edit_location_fix_failed)
    val locationPermissionMessage = stringResource(R.string.alarm_edit_location_permission_required)

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
        editorScrollState.scrollToItem(0)
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
            photoReferenceStatus = noReferencePhotoMessage
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
            photoReferenceStatus = referencePhotoSavedMessage
        }.onFailure {
            photoReferenceStatus = referencePhotoSaveFailedMessage
        }
        if (!bitmap.isRecycled) bitmap.recycle()
    }
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            photoReferenceLauncher.launch(null)
        } else {
            photoReferenceStatus = cameraPermissionMessage
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
            firingBackgroundStatus = noBackgroundMessage
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
            backgroundSelectedMessage
        } else {
            backgroundPermissionMessage
        }
    }
    val captureLocationDismissTarget = {
        val location = LocationHelper.getLastKnownLocation(context)
        if (location != null) {
            viewModel.updateLocationDismissTarget(location.latitude, location.longitude)
            locationDismissStatus = locationSavedMessage
        } else {
            locationDismissStatus = locationFixFailedMessage
        }
    }
    val locationDismissPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            captureLocationDismissTarget()
        } else {
            locationDismissStatus = locationPermissionMessage
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
            title = { Text(stringResource(R.string.alarm_edit_discard_title)) },
            text = { Text(stringResource(R.string.alarm_edit_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.alarm_edit_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.alarm_edit_keep_editing))
                }
            }
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val editorTitle = if (editorPage == AlarmEditorPage.OVERVIEW) {
                stringResource(if (state.isEditing) R.string.edit_alarm else R.string.new_alarm)
            } else stringResource(editorPage.titleRes)
            val editorSubtitle = if (editorPage == AlarmEditorPage.OVERVIEW) {
                if (state.isEditing) {
                    stringResource(R.string.alarm_edit_existing_subtitle)
                } else {
                    stringResource(R.string.alarm_edit_new_subtitle)
                }
            } else stringResource(editorPage.subtitleRes)
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
                                stringResource(R.string.alarm_edit_cancel_accessibility)
                            } else {
                                stringResource(R.string.alarm_edit_back_overview_accessibility)
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                            text = when {
                                state.isSaving -> stringResource(R.string.alarm_edit_saving)
                                state.isEditing -> stringResource(R.string.alarm_edit_save_changes)
                                else -> stringResource(R.string.alarm_edit_create)
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LaunchedEffect(
            state.hour,
            state.minute,
            state.repeatDays,
            state.specificDate,
            state.solarOffsetMinutes,
            state.solarAnchor,
            state.shiftPattern,
            state.shiftPatternStartDate,
            state.timezonePolicy,
            state.fixedTimezoneId,
            state.skipOnHolidays
        ) {
            viewModel.computeForecast()
        }
        CompositionLocalProvider(LocalAlarmEditorPage provides editorPage) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = editorScrollState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            if (editorPage == AlarmEditorPage.OVERVIEW) {
            item(key = "overview-preview") {
            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                highlighted = true
            ) {
                AppSectionTitle(
                    title = stringResource(R.string.alarm_edit_preview_title),
                    description = stringResource(R.string.alarm_edit_preview_description)
                )

                AppStatusChip(
                    label = stringResource(
                        if (state.isEditing) R.string.alarm_edit_existing_status else R.string.alarm_edit_new_status
                    ),
                    icon = if (state.isEditing) Icons.Default.Edit else Icons.Default.AddAlarm,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            // v1.13.15: sticky numpad mode opens prefilled with the
                            // current time instead of empty digits + disabled Save.
                            timeNumpadDigits = if (useTimeNumpad) {
                                formatAlarmNumpadDigits(state.hour, state.minute, state.is24HourFormat)
                            } else {
                                ""
                            }
                            timeNumpadIsPm = state.hour >= 12
                            showTimePicker = true
                        }
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
                            val amPm = java.time.LocalTime.of(state.hour, state.minute)
                                .format(java.time.format.DateTimeFormatter.ofPattern("a"))
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
                    text = stringResource(R.string.alarm_edit_adjust_time_hint),
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
                        text = stringResource(R.string.alarm_edit_rings_in, remaining),
                        color = MaterialTheme.colorScheme.primary,
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
            }

            // Label
            SettingsSection(editorPage, AlarmEditorSection.LABEL) {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::updateLabel,
                    placeholder = { Text(stringResource(R.string.alarm_edit_label_placeholder), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )
            }

            // Group
            SettingsSection(editorPage, AlarmEditorSection.GROUP) {
                var showGroupMenu by remember { mutableStateOf(false) }
                val defaultGroups = listOf(
                    "" to stringResource(R.string.alarm_edit_group_none),
                    "Work" to stringResource(R.string.alarm_edit_group_work),
                    "School" to stringResource(R.string.alarm_edit_group_school),
                    "Gym" to stringResource(R.string.alarm_edit_group_gym),
                    "Medication" to stringResource(R.string.alarm_edit_group_medication),
                    "Personal" to stringResource(R.string.alarm_edit_group_personal)
                )
                val defaultGroupValues = defaultGroups.map { it.first }
                val isCustomGroup = state.group.isNotBlank() && state.group !in defaultGroupValues
                SettingsRow(label = stringResource(R.string.alarm_edit_alarm_group)) {
                    Box {
                        SettingsValueButton(
                            label = if (isCustomGroup) {
                                state.group
                            } else {
                                defaultGroups.firstOrNull { it.first == state.group }?.second
                                    ?: stringResource(R.string.alarm_edit_group_none)
                            },
                            onClick = { showGroupMenu = true }
                        )
                        DropdownMenu(
                            expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false }
                        ) {
                            defaultGroups.forEach { (group, groupLabel) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            groupLabel,
                                            color = if (group == state.group) MaterialTheme.colorScheme.primary else TextPrimary
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
                                        stringResource(R.string.alarm_edit_group_custom),
                                        color = if (isCustomGroup) MaterialTheme.colorScheme.primary else TextMuted
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
                        label = { Text(stringResource(R.string.alarm_edit_group_custom_name), color = TextMuted) },
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
                item(key = "overview-categories") {
                AlarmEditorCategoryOverview(
                    state = state,
                    onSelect = { page -> editorPageName = page.name }
                )
                }
            }

            // Sound settings
            SettingsSection(editorPage, AlarmEditorSection.SOUND) {
                val hapticOnlyActive = state.overrideSystemVolume && state.volume == 0 && state.vibrationEnabled

                SettingsRow(label = stringResource(R.string.alarm_edit_partner_mode)) {
                    OutlinedButton(
                        onClick = viewModel::applyDontWakePartnerProfile,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.alarm_edit_apply))
                    }
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_partner_mode_hint),
                    tone = HintTone.Neutral
                )
                if (hapticOnlyActive) {
                    AppStatusChip(
                        label = stringResource(R.string.alarm_edit_haptic_profile_active),
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                SettingsRow(
                    label = stringResource(R.string.alarm_edit_alarm_sound),
                    trailing = {
                        SettingsValueButton(
                            label = when (state.ringtoneUri) {
                                "" -> stringResource(R.string.alarm_edit_default)
                                "silent" -> stringResource(R.string.alarm_edit_silent)
                                else -> stringResource(R.string.alarm_edit_custom)
                            },
                            onClick = { showRingtonePicker = true }
                        )
                    }
                )

                SettingsRow(
                    label = stringResource(R.string.alarm_edit_override_volume),
                    trailing = {
                        Switch(
                            checked = state.overrideSystemVolume,
                            onCheckedChange = viewModel::updateOverrideVolume,
                            colors = appSwitchColors()
                        )
                    }
                )

                if (state.overrideSystemVolume) {
                    SettingsRow(label = stringResource(R.string.volume)) {
                        Text(
                            if (state.volume == 0) {
                                stringResource(R.string.alarm_edit_muted)
                            } else {
                                stringResource(R.string.alarm_edit_percent, state.volume)
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.volume.toFloat(),
                        onValueChange = { viewModel.updateVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Gradual volume - interactive slider
                var showGradualMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.alarm_edit_gradual_volume)) {
                    Box {
                        SettingsValueButton(
                            label = when (state.gradualVolumeSeconds) {
                                0 -> stringResource(R.string.alarm_edit_off)
                                else -> stringResource(
                                    R.string.alarm_edit_minutes_seconds_short,
                                    state.gradualVolumeSeconds / 60,
                                    state.gradualVolumeSeconds % 60
                                )
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
                                                0 -> stringResource(R.string.alarm_edit_off_full_volume)
                                                else -> stringResource(
                                                    R.string.alarm_edit_minutes_seconds_short,
                                                    secs / 60,
                                                    secs % 60
                                                )
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
            SettingsSection(editorPage, AlarmEditorSection.VIBRATION) {
                SettingsRow(
                    label = stringResource(R.string.vibration),
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
                        Triple("default", stringResource(R.string.alarm_edit_default), stringResource(R.string.alarm_edit_vibration_default)),
                        Triple("gentle", stringResource(R.string.alarm_edit_gentle), stringResource(R.string.alarm_edit_vibration_gentle)),
                        Triple("heartbeat", stringResource(R.string.alarm_edit_heartbeat), stringResource(R.string.alarm_edit_vibration_heartbeat)),
                        Triple("escalating", stringResource(R.string.alarm_edit_escalating), stringResource(R.string.alarm_edit_vibration_escalating)),
                        Triple("sos", stringResource(R.string.alarm_edit_sos), stringResource(R.string.alarm_edit_vibration_sos))
                    )
                    SettingsRow(label = stringResource(R.string.alarm_edit_vibration_pattern)) {
                        Box {
                            SettingsValueButton(
                                label = patterns.find { it.first == state.vibrationPattern }?.second
                                    ?: stringResource(R.string.alarm_edit_default),
                                onClick = { showPatternMenu = true }
                            )
                            DropdownMenu(
                                expanded = showPatternMenu,
                                onDismissRequest = { showPatternMenu = false }
                            ) {
                                patterns.forEach { (key, _, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color = if (key == state.vibrationPattern) MaterialTheme.colorScheme.primary else TextPrimary
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
                        val intensities = listOf(
                            1 to stringResource(R.string.alarm_edit_gentle),
                            2 to stringResource(R.string.alarm_edit_strong)
                        )
                        SettingsRow(label = stringResource(R.string.alarm_edit_ramp_strength)) {
                            Box {
                                SettingsValueButton(
                                    label = intensities.firstOrNull {
                                        it.first == state.vibrationIntensity
                                    }?.second ?: stringResource(R.string.alarm_edit_strong),
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
                                                        MaterialTheme.colorScheme.primary
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
                            stringResource(R.string.alarm_edit_haptic_envelope_hint),
                            tone = HintTone.Neutral
                        )
                    }

                    // v1.12.0 (roadmap N7): vibration start-delay
                    var showVibDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = stringResource(R.string.alarm_edit_vibration_delay)) {
                        Box {
                            SettingsValueButton(
                                label = when (state.vibrationDelaySeconds) {
                                    0 -> stringResource(R.string.alarm_edit_immediately)
                                    in 1..59 -> stringResource(R.string.alarm_edit_seconds_short, state.vibrationDelaySeconds)
                                    else -> stringResource(
                                        R.string.alarm_edit_minutes_seconds_short,
                                        state.vibrationDelaySeconds / 60,
                                        state.vibrationDelaySeconds % 60
                                    )
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
                                                    0 -> stringResource(R.string.alarm_edit_immediately_default)
                                                    in 1..59 -> pluralStringResource(R.plurals.alarm_edit_seconds, secs, secs)
                                                    else -> pluralStringResource(R.plurals.alarm_edit_minutes, secs / 60, secs / 60)
                                                },
                                                color = if (secs == state.vibrationDelaySeconds) MaterialTheme.colorScheme.primary else TextPrimary
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
            SettingsSection(editorPage, AlarmEditorSection.SNOOZE) {
                var showSnoozeMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.snooze_duration)) {
                    Box {
                        SettingsValueButton(
                            label = stringResource(R.string.alarm_edit_minutes_short, state.snoozeDurationMinutes),
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
                                            pluralStringResource(R.plurals.alarm_edit_minutes, mins, mins),
                                            color = if (mins == state.snoozeDurationMinutes) MaterialTheme.colorScheme.primary else TextPrimary
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
            SettingsSection(editorPage, AlarmEditorSection.UPCOMING) {
                if (state.forecastDates.isNotEmpty()) {
                    state.forecastDates.forEach { entry ->
                        val instant = java.time.Instant.ofEpochMilli(entry.timeMillis)
                        val dt = instant.atZone(java.time.ZoneId.systemDefault())
                        val dateStr = dt.format(
                            java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                        )
                        val timeStr = dt.format(java.time.format.DateTimeFormatter.ofPattern(
                            if (state.is24HourFormat) "HH:mm" else "h:mm a"
                        ))
                        val label = if (entry.skippedByVacation) {
                            stringResource(R.string.alarm_edit_vacation_skip_date, dateStr, timeStr)
                        } else {
                            stringResource(R.string.alarm_edit_fire_date, dateStr, timeStr)
                        }
                        val tone = if (entry.skippedByVacation) HintTone.Warning else HintTone.Neutral
                        SettingsHint(label, tone = tone)
                    }
                } else {
                    SettingsHint(stringResource(R.string.alarm_edit_will_not_ring), tone = HintTone.Warning)
                }
            }

            // Dismiss Challenge
            SettingsSection(editorPage, AlarmEditorSection.DISMISS_CHALLENGE) {
                val challengeOptions = alarmChallengeOptions()
                var expanded by remember { mutableStateOf(false) }

                SettingsRow(label = stringResource(R.string.alarm_edit_challenge_type)) {
                    Box {
                        SettingsValueButton(
                            label = challengeOptions.find { it.first == state.challengeType }?.second
                                ?: stringResource(R.string.alarm_edit_none),
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
                                            color = if (type == state.challengeType) MaterialTheme.colorScheme.primary else TextPrimary
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
                            stringResource(R.string.alarm_edit_challenge_ready),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_steps_required)) {
                        Box {
                            SettingsValueButton(
                                label = pluralStringResource(
                                    R.plurals.alarm_edit_steps,
                                    state.walkStepsRequired,
                                    state.walkStepsRequired
                                ),
                                onClick = { showStepsMenu = true }
                            )
                            DropdownMenu(
                                expanded = showStepsMenu,
                                onDismissRequest = { showStepsMenu = false }
                            ) {
                                listOf(10, 20, 30, 50, 100).forEach { steps ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                pluralStringResource(R.plurals.alarm_edit_steps, steps, steps),
                                                color = if (steps == state.walkStepsRequired) MaterialTheme.colorScheme.primary else TextPrimary
                                            )
                                        },
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_squats_required)) {
                        Box {
                            SettingsValueButton(
                                label = pluralStringResource(
                                    R.plurals.alarm_edit_squats,
                                    state.requiredSquats,
                                    state.requiredSquats
                                ),
                                onClick = { showSquatsMenu = true }
                            )
                            DropdownMenu(
                                expanded = showSquatsMenu,
                                onDismissRequest = { showSquatsMenu = false }
                            ) {
                                listOf(5, 10, 15, 20, 30, 50).forEach { count ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                pluralStringResource(R.plurals.alarm_edit_squats, count, count),
                                                color = if (count == state.requiredSquats) MaterialTheme.colorScheme.primary else TextPrimary
                                            )
                                        },
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
                        label = { Text(stringResource(R.string.alarm_edit_nfc_id), color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    if (state.nfcTagId.isBlank()) {
                        SettingsHint(
                            stringResource(R.string.alarm_edit_nfc_missing),
                            tone = HintTone.Warning
                        )
                    }
                }

                // BARCODE_SCAN: barcode value field
                if (state.challengeType == "BARCODE_SCAN") {
                    OutlinedTextField(
                        value = state.barcodeValue,
                        onValueChange = viewModel::updateBarcodeValue,
                        label = { Text(stringResource(R.string.alarm_edit_barcode_value), color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    if (state.barcodeValue.isBlank()) {
                        SettingsHint(
                            stringResource(R.string.alarm_edit_barcode_missing),
                            tone = HintTone.Warning
                        )
                    }
                }

                // WIFI_CONNECT: surface SSID field in context
                if (state.challengeType == "WIFI_CONNECT") {
                    OutlinedTextField(
                        value = state.wifiDismissSsid,
                        onValueChange = viewModel::updateWifiDismissSsid,
                        label = { Text(stringResource(R.string.alarm_edit_wifi_name), color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    if (state.wifiDismissSsid.isBlank()) {
                        SettingsHint(
                            stringResource(R.string.alarm_edit_wifi_missing),
                            tone = HintTone.Warning
                        )
                    }
                }

                // PHOTO_MATCH: reference photo URI field
                if (state.challengeType == "PHOTO_MATCH") {
                    SettingsRow(label = stringResource(R.string.alarm_edit_reference_photo)) {
                        OutlinedButton(
                            onClick = captureReferencePhoto,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(
                                    if (state.photoMatchUri.isBlank()) R.string.alarm_edit_capture else R.string.alarm_edit_replace
                                )
                            )
                        }
                    }
                    SettingsHint(
                        if (state.photoMatchUri.isBlank()) {
                            stringResource(R.string.alarm_edit_capture_photo_hint)
                        } else {
                            stringResource(R.string.alarm_edit_photo_saved_for_alarm)
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

            SettingsSection(editorPage, AlarmEditorSection.LOCATION) {
                val hasLocationTarget = LocationDismissPolicy.hasTarget(
                    state.locationDismissLat,
                    state.locationDismissLng
                )
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_require_leaving),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_saved_place)) {
                        SettingsValueButton(
                            label = if (hasLocationTarget) {
                                formatLocationDismissTarget(
                                    state.locationDismissLat,
                                    state.locationDismissLng
                                )
                            } else {
                                stringResource(R.string.alarm_edit_not_saved)
                            },
                            onClick = requestLocationDismissTarget
                        )
                    }
                    SettingsRow(label = stringResource(R.string.alarm_edit_unlock_radius)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_meters_short, state.locationDismissRadius),
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
                                                pluralStringResource(R.plurals.alarm_edit_meters, radius, radius),
                                                color = if (radius == state.locationDismissRadius) MaterialTheme.colorScheme.primary else TextPrimary
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (hasLocationTarget) R.string.alarm_edit_update_saved_place else R.string.alarm_edit_save_current_place
                            )
                        )
                    }
                    SettingsHint(
                        text = if (hasLocationTarget) {
                            stringResource(R.string.alarm_edit_location_radius_hint, state.locationDismissRadius)
                        } else {
                            stringResource(R.string.alarm_edit_location_save_first)
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
                        stringResource(R.string.alarm_edit_location_optional_hint),
                        tone = HintTone.Neutral
                    )
                }
            }

            // Wake effects
            SettingsSection(editorPage, AlarmEditorSection.WAKE_EFFECTS) {
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
                        contentColor = if (isGentleWake) DismissGreen else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(
                            if (isGentleWake) R.string.alarm_edit_gentle_wake_active else R.string.alarm_edit_apply_gentle_wake
                        )
                    )
                }
                if (!isGentleWake) {
                    SettingsHint(
                        stringResource(R.string.alarm_edit_gentle_wake_hint),
                        tone = HintTone.Neutral
                    )
                }

                SettingsRow(
                    label = stringResource(R.string.alarm_edit_flash_wake),
                    trailing = {
                        Switch(
                            checked = state.flashWake,
                            onCheckedChange = viewModel::updateFlashWake,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_flash_wake_hint),
                    tone = HintTone.Neutral
                )

                SettingsRow(
                    label = stringResource(R.string.alarm_edit_background_image),
                    trailing = {
                        SettingsValueButton(
                            label = stringResource(
                                if (state.firingBackgroundImageUri.isBlank()) R.string.alarm_edit_choose else R.string.alarm_edit_replace
                            ),
                            onClick = { firingBackgroundImageLauncher.launch(arrayOf("image/*")) }
                        )
                    }
                )
                if (state.firingBackgroundImageUri.isBlank()) {
                    SettingsHint(
                        stringResource(R.string.alarm_edit_background_optional_hint),
                        tone = HintTone.Neutral
                    )
                } else {
                    SettingsRow(
                        label = stringResource(R.string.alarm_edit_show_ringing_image),
                        trailing = {
                            Switch(
                                checked = state.firingBackgroundImageEnabled,
                                onCheckedChange = viewModel::updateFiringBackgroundImageEnabled,
                                colors = appSwitchColors()
                            )
                        }
                    )
                    SettingsRow(
                        label = stringResource(R.string.alarm_edit_blur_image),
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
                        Text(stringResource(R.string.alarm_edit_clear_background))
                    }
                    SettingsHint(
                        if (state.firingBackgroundImageEnabled) {
                            stringResource(R.string.alarm_edit_background_enabled_hint)
                        } else {
                            stringResource(R.string.alarm_edit_background_disabled_hint)
                        },
                        tone = HintTone.Neutral
                    )
                }
                if (firingBackgroundStatus.isNotBlank()) {
                    SettingsHint(firingBackgroundStatus, tone = HintTone.Neutral)
                }
            }

            // Morning Announcement (TTS)
            SettingsSection(editorPage, AlarmEditorSection.ANNOUNCEMENT) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_speak_context),
                    trailing = {
                        Switch(
                            checked = state.ttsEnabled,
                            onCheckedChange = viewModel::updateTtsEnabled,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_speak_context_hint),
                    tone = HintTone.Neutral
                )
            }

            // Wake Confirmation
            SettingsSection(editorPage, AlarmEditorSection.WAKE_CONFIRM) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_confirm_awake),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_realarm_delay)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_minutes_short, state.wakeConfirmDelayMinutes),
                                onClick = { showDelayMenu = true }
                            )
                            DropdownMenu(
                                expanded = showDelayMenu,
                                onDismissRequest = { showDelayMenu = false }
                            ) {
                                listOf(5, 10, 15, 20, 30).forEach { mins ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                pluralStringResource(R.plurals.alarm_edit_minutes, mins, mins),
                                                color = if (mins == state.wakeConfirmDelayMinutes) MaterialTheme.colorScheme.primary else TextPrimary
                                            )
                                        },
                                        onClick = { viewModel.updateWakeConfirm(true, mins); showDelayMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    SettingsHint(
                        stringResource(R.string.alarm_edit_realarm_hint),
                        tone = HintTone.Warning
                    )
                }
            }

            // Smart Alarm
            SettingsSection(editorPage, AlarmEditorSection.SMART_ALARM) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_light_sleep),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_detection_window)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_minutes_before_short, state.smartAlarmWindowMinutes),
                                onClick = { showWindowMenu = true }
                            )
                            DropdownMenu(
                                expanded = showWindowMenu,
                                onDismissRequest = { showWindowMenu = false }
                            ) {
                                listOf(15, 20, 30, 45, 60).forEach { mins ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.alarm_edit_minutes_before_alarm, mins),
                                                color = if (mins == state.smartAlarmWindowMinutes) MaterialTheme.colorScheme.primary else TextPrimary
                                            )
                                        },
                                        onClick = { viewModel.updateSmartAlarm(true, mins); showWindowMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    SettingsHint(
                        stringResource(R.string.alarm_edit_smart_alarm_hint),
                        tone = HintTone.Neutral
                    )
                }
            }

            // Holiday Skip
            SettingsSection(editorPage, AlarmEditorSection.HOLIDAYS) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_skip_holidays),
                    trailing = {
                        Switch(
                            checked = state.skipOnHolidays,
                            onCheckedChange = viewModel::updateSkipOnHolidays,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_holidays_hint),
                    tone = HintTone.Warning
                )
            }

            // Spotify Ringtone
            SettingsSection(editorPage, AlarmEditorSection.SPOTIFY) {
                OutlinedTextField(
                    value = state.spotifyUri,
                    onValueChange = viewModel::updateSpotifyUri,
                    label = { Text(stringResource(R.string.alarm_edit_spotify_uri), color = TextMuted) },
                    placeholder = { Text(stringResource(R.string.alarm_edit_default_ringtone_placeholder), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_spotify_hint),
                    tone = HintTone.Warning
                )
            }

            // Philips Hue Sunrise
            SettingsSection(editorPage, AlarmEditorSection.HUE) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_hue_sunrise),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_hue_start)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_minutes_before_short, state.huePreWakeMinutes),
                                onClick = { showHueMenu = true }
                            )
                            DropdownMenu(
                                expanded = showHueMenu,
                                onDismissRequest = { showHueMenu = false }
                            ) {
                                listOf(10, 15, 20, 30, 45, 60, 90).forEach { mins ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.alarm_edit_minutes_before, mins),
                                                color = if (mins == state.huePreWakeMinutes) MaterialTheme.colorScheme.primary else TextPrimary
                                            )
                                        },
                                        onClick = { viewModel.updateHue(true, mins); showHueMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    SettingsHint(
                        stringResource(R.string.alarm_edit_hue_hint),
                        tone = HintTone.Warning
                    )
                }
            }

            // v1.2.0: Mission Chaining
            SettingsSection(editorPage, AlarmEditorSection.CHAIN) {
                val chainItems = state.challengeChain.toChallengeChainList()
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_challenge_chain),
                    trailing = {
                        SettingsValueButton(
                            label = if (chainItems.isEmpty()) {
                                stringResource(R.string.alarm_edit_choose)
                            } else {
                                pluralStringResource(R.plurals.alarm_edit_challenges, chainItems.size, chainItems.size)
                            },
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
                    label = { Text(stringResource(R.string.alarm_edit_chain_override), color = TextMuted) },
                    placeholder = { Text(stringResource(R.string.alarm_edit_chain_placeholder), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_chain_hint),
                    tone = HintTone.Neutral
                )
                if (chainItems.isNotEmpty()) {
                    val missingRefs = buildList {
                        if ("NFC_SCAN" in chainItems && state.nfcTagId.isBlank()) add(stringResource(R.string.alarm_edit_nfc_id))
                        if ("BARCODE_SCAN" in chainItems && state.barcodeValue.isBlank()) add(stringResource(R.string.alarm_edit_barcode_value_short))
                        if ("PHOTO_MATCH" in chainItems && state.photoMatchUri.isBlank()) add(stringResource(R.string.alarm_edit_reference_photo_lower))
                        if ("WIFI_CONNECT" in chainItems && state.wifiDismissSsid.isBlank()) add(stringResource(R.string.alarm_edit_wifi_ssid))
                    }
                    if (missingRefs.isNotEmpty()) {
                        SettingsHint(
                            stringResource(R.string.alarm_edit_missing_references, missingRefs.joinToString(", ")),
                            tone = HintTone.Warning
                        )
                    }
                }
            }

            // v1.2.0: Anti-Snooze Features
            SettingsSection(editorPage, AlarmEditorSection.ANTI_SNOOZE) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_progressive_snooze),
                    trailing = {
                        Switch(
                            checked = state.progressiveSnooze,
                            onCheckedChange = viewModel::updateProgressiveSnooze,
                            colors = appSwitchColors()
                        )
                    }
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_progressive_snooze_hint),
                    tone = HintTone.Neutral
                )

                SettingsRow(
                    label = stringResource(R.string.alarm_edit_backup_sound),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_escalate_after)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_seconds_short, state.backupSoundDelaySec),
                                onClick = { showDelayMenu = true }
                            )
                            DropdownMenu(expanded = showDelayMenu, onDismissRequest = { showDelayMenu = false }) {
                                listOf(20, 30, 40, 60, 90, 120).forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text(pluralStringResource(R.plurals.alarm_edit_seconds, sec, sec)) },
                                        onClick = { viewModel.updateBackupSound(true, sec); showDelayMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    SettingsHint(
                        stringResource(R.string.alarm_edit_backup_sound_hint),
                        tone = HintTone.Warning
                    )
                }

                SettingsRow(
                    label = stringResource(R.string.alarm_edit_flashlight_strobe),
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
                        stringResource(R.string.alarm_edit_flashlight_warning),
                        tone = HintTone.Warning
                    )
                }
            }

            // v1.2.0: Sunrise Simulation
            SettingsSection(editorPage, AlarmEditorSection.SUNRISE) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_screen_sunrise),
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
                    SettingsRow(label = stringResource(R.string.alarm_edit_duration)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_minutes_short, state.sunriseMinutes),
                                onClick = { showMenu = true }
                            )
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                listOf(5, 10, 15, 20, 30).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text(pluralStringResource(R.plurals.alarm_edit_minutes, mins, mins)) },
                                        onClick = { viewModel.updateSunriseSimulation(true, mins); showMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    SettingsHint(
                        stringResource(R.string.alarm_edit_sunrise_hint),
                        tone = HintTone.Neutral
                    )
                }
            }

            // v1.2.0: Sound Source
            SettingsSection(editorPage, AlarmEditorSection.RADIO) {
                OutlinedTextField(
                    value = state.internetRadioUrl,
                    onValueChange = viewModel::updateInternetRadioUrl,
                    label = { Text(stringResource(R.string.alarm_edit_stream_url), color = TextMuted) },
                    placeholder = { Text(stringResource(R.string.alarm_edit_default_ringtone_placeholder), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_radio_hint),
                    tone = HintTone.Warning
                )
            }

            // v1.2.0: Guardian Angel
            SettingsSection(editorPage, AlarmEditorSection.GUARDIAN) {
                SettingsRow(
                    label = stringResource(R.string.alarm_edit_emergency_alert),
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
                        label = { Text(stringResource(R.string.alarm_edit_emergency_phone), color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    var showDelayMenu by remember { mutableStateOf(false) }
                    SettingsRow(label = stringResource(R.string.alarm_edit_alert_after)) {
                        Box {
                            SettingsValueButton(
                                label = stringResource(R.string.alarm_edit_minutes_short, state.guardianDelaySec / 60),
                                onClick = { showDelayMenu = true }
                            )
                            DropdownMenu(expanded = showDelayMenu, onDismissRequest = { showDelayMenu = false }) {
                                listOf(120, 180, 300, 600, 900).forEach { sec ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                pluralStringResource(
                                                    R.plurals.alarm_edit_minutes,
                                                    sec / 60,
                                                    sec / 60
                                                )
                                            )
                                        },
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
            SettingsSection(editorPage, AlarmEditorSection.ROUTINE) {
                OutlinedTextField(
                    value = state.morningRoutine,
                    onValueChange = viewModel::updateMorningRoutine,
                    label = { Text(stringResource(R.string.alarm_edit_checklist_label), color = TextMuted) },
                    placeholder = { Text(stringResource(R.string.alarm_edit_checklist_placeholder), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    minLines = 3, maxLines = 6
                )
                SettingsHint(
                    stringResource(R.string.alarm_edit_checklist_hint),
                    tone = HintTone.Neutral
                )
            }
            // v1.2.0: Advanced
            SettingsSection(editorPage, AlarmEditorSection.ADVANCED) {
                SettingsRow(label = stringResource(R.string.alarm_edit_profile)) {
                    OutlinedTextField(
                        value = state.profileName,
                        onValueChange = viewModel::updateProfileName,
                        placeholder = { Text(stringResource(R.string.alarm_edit_profile_placeholder), color = TextMuted) },
                        colors = appOutlinedTextFieldColors(),
                        shape = AppInputShape,
                        modifier = Modifier.width(180.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = state.specificDate,
                    onValueChange = viewModel::updateSpecificDate,
                    label = { Text(stringResource(R.string.alarm_edit_specific_date), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
                var showTimezonePolicyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.alarm_edit_time_zone)) {
                    Box {
                        SettingsValueButton(
                            label = if (state.timezonePolicy == Alarm.TIMEZONE_POLICY_FIXED) {
                                stringResource(R.string.alarm_edit_fixed_zone)
                            } else {
                                stringResource(R.string.alarm_edit_follow_device)
                            },
                            onClick = { showTimezonePolicyMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTimezonePolicyMenu,
                            onDismissRequest = { showTimezonePolicyMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_edit_follow_device_zone)) },
                                onClick = {
                                    viewModel.updateTimezonePolicy(Alarm.TIMEZONE_POLICY_LOCAL)
                                    showTimezonePolicyMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_edit_keep_fixed_zone)) },
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
                        label = { Text(stringResource(R.string.alarm_edit_iana_zone), color = TextMuted) },
                        supportingText = {
                            Text(
                                if (zoneIsValid) {
                                    stringResource(
                                        R.string.alarm_edit_fixed_zone_hint,
                                        state.hour.toString().padStart(2, '0'),
                                        state.minute.toString().padStart(2, '0')
                                    )
                                } else {
                                    stringResource(R.string.alarm_edit_unknown_zone)
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
                SettingsRow(label = stringResource(R.string.alarm_edit_shift_pattern)) {
                    Box {
                        SettingsValueButton(
                            label = selectedShiftPattern?.title ?: stringResource(R.string.alarm_edit_disabled),
                            onClick = { showShiftPatternMenu = true }
                        )
                        DropdownMenu(
                            expanded = showShiftPatternMenu,
                            onDismissRequest = { showShiftPatternMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_edit_disabled)) },
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
                                                shiftPatternDescription(pattern),
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
                            label = { Text(stringResource(R.string.alarm_edit_shift_start), color = TextMuted) },
                            colors = appOutlinedTextFieldColors(),
                            shape = AppInputShape,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            singleLine = true
                        )
                        SettingsHint(
                            stringResource(R.string.alarm_edit_shift_hint),
                            tone = HintTone.Neutral
                        )
                    }
                }
                var showWeatherEarlyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.alarm_edit_weather_early)) {
                    Box {
                        SettingsValueButton(
                            label = if (state.weatherEarlyMinutes == 0) {
                                stringResource(R.string.alarm_edit_disabled)
                            } else {
                                stringResource(R.string.alarm_edit_minutes_short, state.weatherEarlyMinutes)
                            },
                            onClick = { showWeatherEarlyMenu = true }
                        )
                        DropdownMenu(expanded = showWeatherEarlyMenu, onDismissRequest = { showWeatherEarlyMenu = false }) {
                            listOf(0, 10, 15, 20, 30).forEach { mins ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (mins == 0) {
                                                stringResource(R.string.alarm_edit_disabled)
                                            } else {
                                                stringResource(R.string.alarm_edit_minutes_earlier, mins)
                                            }
                                        )
                                    },
                                    onClick = { viewModel.updateWeatherEarlyMinutes(mins); showWeatherEarlyMenu = false }
                                )
                            }
                        }
                    }
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_weather_early_hint),
                    tone = HintTone.Neutral
                )

                var showEarlyMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.alarm_edit_early_dismiss)) {
                    Box {
                        SettingsValueButton(
                            label = if (state.earlyDismissMinutes == 0) {
                                stringResource(R.string.alarm_edit_disabled)
                            } else {
                                stringResource(R.string.alarm_edit_minutes_short, state.earlyDismissMinutes)
                            },
                            onClick = { showEarlyMenu = true }
                        )
                        DropdownMenu(expanded = showEarlyMenu, onDismissRequest = { showEarlyMenu = false }) {
                            listOf(0, 15, 30, 60).forEach { mins ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (mins == 0) {
                                                stringResource(R.string.alarm_edit_disabled)
                                            } else {
                                                stringResource(R.string.alarm_edit_minutes_before, mins)
                                            }
                                        )
                                    },
                                    onClick = { viewModel.updateEarlyDismiss(mins); showEarlyMenu = false }
                                )
                            }
                        }
                    }
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_early_dismiss_hint),
                    tone = HintTone.Neutral
                )

                OutlinedTextField(
                    value = state.wifiDismissSsid,
                    onValueChange = viewModel::updateWifiDismissSsid,
                    label = { Text(stringResource(R.string.alarm_edit_wifi_dismiss_ssid), color = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )

                // v1.4.0: Hardware-button action (Volume/Camera/Headset-hook keys
                // during firing). NONE = normal volume control passes through.
                var showHwMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.alarm_edit_hardware_action)) {
                    Box {
                        val hardwareActions = listOf(
                            "NONE" to stringResource(R.string.alarm_edit_hardware_none),
                            "SNOOZE" to stringResource(R.string.alarm_edit_hardware_snooze),
                            "DISMISS" to stringResource(R.string.alarm_edit_hardware_dismiss)
                        )
                        SettingsValueButton(
                            label = hardwareActions.firstOrNull { it.first == state.hardwareButtonAction }?.second
                                ?: stringResource(R.string.alarm_edit_none),
                            onClick = { showHwMenu = true }
                        )
                        DropdownMenu(expanded = showHwMenu, onDismissRequest = { showHwMenu = false }) {
                            hardwareActions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { viewModel.updateHardwareButtonAction(value); showHwMenu = false }
                                )
                            }
                        }
                    }
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_hardware_hint),
                    tone = HintTone.Neutral
                )

                // v1.10.3: Deliberate dismiss confirmation for users who
                // accidentally swipe ready alarms while half-awake.
                SettingsRow(label = stringResource(R.string.alarm_edit_hold_to_dismiss)) {
                    Switch(
                        checked = state.holdToDismissEnabled,
                        onCheckedChange = viewModel::updateHoldToDismiss,
                        colors = appSwitchColors()
                    )
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_hold_to_dismiss_hint),
                    tone = HintTone.Neutral
                )

                // v1.4.0: Dismiss-at-ringtone-end. Great for single-song wake-ups.
                SettingsRow(label = stringResource(R.string.alarm_edit_dismiss_song_end)) {
                    Switch(
                        checked = state.dismissAtRingtoneEnd,
                        onCheckedChange = viewModel::updateDismissAtRingtoneEnd,
                        colors = appSwitchColors()
                    )
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_dismiss_song_end_hint),
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
                        text = stringResource(R.string.alarm_edit_ringtone_pool),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ringtonePoolEntries.forEach { uri ->
                            val shortName = ringtoneShortName(
                                uri,
                                stringResource(R.string.alarm_edit_empty_ringtone)
                            )
                            AppFilterChip(
                                label = shortName,
                                selected = true,
                                leadingIcon = Icons.Default.Close,
                                selectionSemantics = false,
                                accessibilityLabel = stringResource(R.string.alarm_edit_remove_ringtone, shortName),
                                onClick = {
                                    val next = ringtonePoolEntries.filterNot { it == uri }.joinToString(",")
                                    viewModel.updateRingtonePool(next)
                                }
                            )
                        }
                        AppFilterChip(
                            label = stringResource(R.string.alarm_edit_add_ringtone),
                            selected = false,
                            accessibilityLabel = stringResource(R.string.alarm_edit_add_ringtone_accessibility),
                            onClick = { showAddRingtoneDialog = true }
                        )
                    }
                }
                SettingsHint(
                    stringResource(R.string.alarm_edit_ringtone_pool_hint),
                    tone = HintTone.Neutral
                )
                if (showAddRingtoneDialog) {
                    var newUri by remember { mutableStateOf("") }
                    val trimmedUri = newUri.trim()
                    val duplicateUri = trimmedUri in ringtonePoolEntries
                    val canAddRingtone = trimmedUri.isNotEmpty() && !duplicateUri
                    AlertDialog(
                        onDismissRequest = { showAddRingtoneDialog = false },
                        title = { Text(stringResource(R.string.alarm_edit_add_ringtone_title)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.alarm_edit_ringtone_uri_hint),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = newUri,
                                    onValueChange = { newUri = it },
                                    label = { Text(stringResource(R.string.alarm_edit_ringtone_uri)) },
                                    placeholder = { Text(stringResource(R.string.alarm_edit_ringtone_uri_placeholder), color = TextMuted) },
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    shape = AppInputShape,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (duplicateUri) {
                                    Text(
                                        text = stringResource(R.string.alarm_edit_ringtone_duplicate),
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
                            ) { Text(stringResource(R.string.alarm_edit_add_ringtone)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddRingtoneDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                // v1.5.0: Sunrise/sunset-relative firing. Overrides the clock time
                // when offset is non-zero; uses last-known location for the solar
                // calc (cached by weather pulls) with a sensible fallback to clock.
                var showAnchorMenu by remember { mutableStateOf(false) }
                val solarAnchors = listOf(
                    "SUNRISE" to stringResource(R.string.alarm_edit_solar_sunrise),
                    "SUNSET" to stringResource(R.string.alarm_edit_solar_sunset)
                )
                SettingsRow(label = stringResource(R.string.alarm_edit_solar_anchor)) {
                    Box {
                        SettingsValueButton(
                            label = solarAnchors.firstOrNull { it.first == state.solarAnchor }?.second
                                ?: stringResource(R.string.alarm_edit_solar_sunrise),
                            onClick = { showAnchorMenu = true }
                        )
                        DropdownMenu(expanded = showAnchorMenu, onDismissRequest = { showAnchorMenu = false }) {
                            solarAnchors.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { viewModel.updateSolarAnchor(value); showAnchorMenu = false }
                                )
                            }
                        }
                    }
                }
                var showOffsetMenu by remember { mutableStateOf(false) }
                SettingsRow(label = stringResource(R.string.alarm_edit_solar_offset)) {
                    Box {
                        val solarOffsetLabel = when {
                            state.solarOffsetMinutes == 0 -> stringResource(R.string.alarm_edit_solar_off)
                            state.solarOffsetMinutes > 0 -> stringResource(
                                R.string.alarm_edit_positive_minutes_short,
                                state.solarOffsetMinutes
                            )
                            else -> stringResource(R.string.alarm_edit_minutes_short, state.solarOffsetMinutes)
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
                                            mins == 0 -> stringResource(R.string.alarm_edit_solar_off)
                                            mins > 0 -> stringResource(
                                                R.string.alarm_edit_solar_after,
                                                mins,
                                                solarAnchors.firstOrNull { it.first == state.solarAnchor }?.second.orEmpty()
                                            )
                                            else -> stringResource(
                                                R.string.alarm_edit_solar_before,
                                                mins,
                                                solarAnchors.firstOrNull { it.first == state.solarAnchor }?.second.orEmpty()
                                            )
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
                    stringResource(R.string.alarm_edit_solar_hint),
                    tone = HintTone.Neutral
                )
            }

            item(key = "bottom-spacer") {
                Spacer(modifier = Modifier.height(28.dp))
            }
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
        val numpadTime = parseAlarmNumpadTime(
            digits = timeNumpadDigits,
            is24Hour = state.is24HourFormat,
            isPm = timeNumpadIsPm
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime = if (useTimeNumpad) {
                        numpadTime
                    } else {
                        AlarmNumpadTime(timePickerState.hour, timePickerState.minute)
                    }
                    selectedTime?.let { viewModel.updateTime(it.hour, it.minute) }
                    showTimePicker = false
                }, enabled = !useTimeNumpad || numpadTime != null) {
                    Text(stringResource(R.string.alarm_edit_save_time), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.alarm_edit_keep_current), color = TextSecondary)
                }
            },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStatusChip(
                        label = stringResource(
                            if (state.is24HourFormat) R.string.alarm_edit_24_hour else R.string.alarm_edit_12_hour
                        ),
                        icon = Icons.Default.Schedule,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.alarm_edit_choose_time),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFilterChip(
                            label = stringResource(R.string.alarm_edit_clock_entry),
                            selected = !useTimeNumpad,
                            onClick = { useTimeNumpad = false },
                            modifier = Modifier.weight(1f)
                        )
                        AppFilterChip(
                            label = stringResource(R.string.alarm_edit_numpad_entry),
                            selected = useTimeNumpad,
                            onClick = {
                                if (!useTimeNumpad && timeNumpadDigits.isEmpty()) {
                                    timeNumpadDigits = formatAlarmNumpadDigits(
                                        state.hour, state.minute, state.is24HourFormat
                                    )
                                    timeNumpadIsPm = state.hour >= 12
                                }
                                useTimeNumpad = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (useTimeNumpad) {
                        AlarmTimeNumpad(
                            digits = timeNumpadDigits,
                            is24Hour = state.is24HourFormat,
                            isPm = timeNumpadIsPm,
                            onPeriodChange = { timeNumpadIsPm = it },
                            onDigit = { digit ->
                                if (timeNumpadDigits.length < 4) {
                                    timeNumpadDigits += digit
                                }
                            },
                            onDelete = { timeNumpadDigits = timeNumpadDigits.dropLast(1) },
                            onClear = { timeNumpadDigits = "" }
                        )
                    } else {
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
                    }
                }
            },
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun AlarmTimeNumpad(
    digits: String,
    is24Hour: Boolean,
    isPm: Boolean,
    onPeriodChange: (Boolean) -> Unit,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    val displayDigits = digits.padEnd(4, '–')
    val parsedTime = parseAlarmNumpadTime(digits, is24Hour, isPm)
    val keys = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', '0', '⌫')
    // v1.13.15: TalkBack reads the en-dash placeholder as dash fragments — describe
    // the entry state explicitly and announce updates politely.
    val entryDescription = if (digits.isEmpty()) {
        stringResource(R.string.alarm_edit_numpad_entry_empty_desc)
    } else {
        stringResource(
            R.string.alarm_edit_numpad_entry_desc,
            if (digits.length > 2) "${digits.take(2)}:${digits.drop(2)}" else digits
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "${displayDigits.take(2)}:${displayDigits.takeLast(2)}",
            style = ClockTimeLarge,
            color = if (digits.length == 4 && parsedTime == null) AccentRed else TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics {
                    contentDescription = entryDescription
                    liveRegion = LiveRegionMode.Polite
                }
        )
        Text(
            text = if (digits.length == 4 && parsedTime == null) {
                stringResource(
                    if (is24Hour) R.string.alarm_edit_numpad_invalid_24
                    else R.string.alarm_edit_numpad_invalid_12
                )
            } else {
                stringResource(R.string.alarm_edit_numpad_hint)
            },
            color = if (digits.length == 4 && parsedTime == null) AccentRed else TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        if (!is24Hour) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    label = stringResource(R.string.alarm_edit_am),
                    selected = !isPm,
                    onClick = { onPeriodChange(false) },
                    modifier = Modifier.weight(1f)
                )
                AppFilterChip(
                    label = stringResource(R.string.alarm_edit_pm),
                    selected = isPm,
                    onClick = { onPeriodChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        keys.chunked(3).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    OutlinedButton(
                        onClick = {
                            when (key) {
                                'C' -> onClear()
                                '⌫' -> onDelete()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        when (key) {
                            'C' -> Text(stringResource(R.string.alarm_edit_clear_short))
                            '⌫' -> Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.alarm_edit_delete_digit)
                            )
                            else -> Text(key.toString(), fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit
) {
    val days = listOf(
        DayOfWeek.MONDAY to stringResource(R.string.alarm_edit_day_monday_short),
        DayOfWeek.TUESDAY to stringResource(R.string.alarm_edit_day_tuesday_short),
        DayOfWeek.WEDNESDAY to stringResource(R.string.alarm_edit_day_wednesday_short),
        DayOfWeek.THURSDAY to stringResource(R.string.alarm_edit_day_thursday_short),
        DayOfWeek.FRIDAY to stringResource(R.string.alarm_edit_day_friday_short),
        DayOfWeek.SATURDAY to stringResource(R.string.alarm_edit_day_saturday_short),
        DayOfWeek.SUNDAY to stringResource(R.string.alarm_edit_day_sunday_short)
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
        state.specificDate.isNotBlank() -> stringResource(R.string.alarm_edit_schedule_specific, repeatSummary)
        state.smartAlarmEnabled -> stringResource(
            R.string.alarm_edit_schedule_smart,
            repeatSummary,
            state.smartAlarmWindowMinutes
        )
        else -> stringResource(R.string.alarm_edit_schedule_fixed, repeatSummary)
    }
    val scheduleSummary = if (state.timezonePolicy == Alarm.TIMEZONE_POLICY_FIXED) {
        stringResource(
            R.string.alarm_edit_schedule_zone,
            baseScheduleSummary,
            state.fixedTimezoneId.ifBlank { stringResource(R.string.alarm_edit_invalid_fixed_zone) }
        )
    } else {
        stringResource(R.string.alarm_edit_schedule_device_zone, baseScheduleSummary)
    }
    val wakeFeatures = listOfNotNull(
        stringResource(R.string.alarm_edit_feature_flash).takeIf { state.flashWake || state.flashlightStrobe },
        stringResource(R.string.alarm_edit_feature_sunrise).takeIf { state.sunriseSimulation },
        stringResource(R.string.alarm_edit_feature_announcement).takeIf { state.ttsEnabled },
        stringResource(R.string.alarm_edit_feature_routine).takeIf { state.morningRoutine.isNotBlank() }
    )
    val integrationCount = listOf(
        state.spotifyUri.isNotBlank(),
        state.hueEnabled,
        state.internetRadioUrl.isNotBlank(),
        state.guardianEnabled
    ).count { it }
    val advancedFeatures = listOfNotNull(
        stringResource(R.string.alarm_edit_feature_profile).takeIf { state.profileName.isNotBlank() },
        stringResource(R.string.alarm_edit_feature_shift).takeIf { state.shiftPattern.isNotBlank() },
        stringResource(R.string.alarm_edit_feature_solar).takeIf { state.solarOffsetMinutes != 0 },
        stringResource(R.string.alarm_edit_feature_wifi).takeIf { state.wifiDismissSsid.isNotBlank() }
    )
    val categories = listOf(
        AlarmEditorCategory(
            page = AlarmEditorPage.SOUND,
            title = stringResource(R.string.alarm_edit_page_sound),
            summary = stringResource(
                R.string.alarm_edit_sound_category_summary,
                state.soundSummary(),
                stringResource(
                    if (state.vibrationEnabled) R.string.alarm_edit_vibration_on else R.string.alarm_edit_vibration_off
                )
            ),
            icon = Icons.AutoMirrored.Filled.VolumeUp
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.DISMISS,
            title = stringResource(R.string.alarm_edit_page_dismiss),
            summary = stringResource(
                R.string.alarm_edit_dismiss_category_summary,
                state.challengeSummary(),
                state.snoozeDurationMinutes
            ),
            icon = Icons.Default.TaskAlt
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.SCHEDULE,
            title = stringResource(R.string.alarm_edit_page_schedule),
            summary = scheduleSummary,
            icon = Icons.Default.CalendarMonth
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.WAKE,
            title = stringResource(R.string.alarm_edit_page_wake),
            summary = wakeFeatures.takeIf { it.isNotEmpty() }?.joinToString(" • ")
                ?: stringResource(R.string.alarm_edit_standard_wake),
            icon = Icons.Default.WbSunny
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.INTEGRATIONS,
            title = stringResource(R.string.alarm_edit_page_integrations),
            summary = if (integrationCount == 0) {
                stringResource(R.string.alarm_edit_no_integrations)
            } else {
                pluralStringResource(R.plurals.alarm_edit_active_integrations, integrationCount, integrationCount)
            },
            icon = Icons.Default.Hub
        ),
        AlarmEditorCategory(
            page = AlarmEditorPage.ADVANCED,
            title = stringResource(R.string.alarm_edit_page_advanced),
            summary = advancedFeatures.takeIf { it.isNotEmpty() }?.joinToString(" • ")
                ?: stringResource(R.string.alarm_edit_using_defaults),
            icon = Icons.Default.Tune
        )
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSectionTitle(
            title = stringResource(R.string.alarm_edit_settings_title),
            description = stringResource(R.string.alarm_edit_settings_description)
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

private fun LazyListScope.SettingsSection(
    activePage: AlarmEditorPage,
    section: AlarmEditorSection,
    content: @Composable ColumnScope.() -> Unit
) {
    if (section.page != activePage) return
    item(key = "alarm-editor-${section.name.lowercase()}") {
        SettingsSectionContent(section, content)
    }
}

@Composable
private fun SettingsSectionContent(
    section: AlarmEditorSection,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSectionTitle(
            title = stringResource(section.titleRes),
            description = stringResource(section.descriptionRes)
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
    focusedPages: Set<AlarmEditorPage>? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val editorPage = LocalAlarmEditorPage.current
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
                    contentDescription = stringResource(
                        if (expanded) R.string.alarm_edit_collapse else R.string.alarm_edit_expand
                    ),
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
                title = stringResource(R.string.alarm_edit_challenge_chain),
                description = stringResource(R.string.alarm_edit_chain_picker_description)
            )

            if (draftChain.isEmpty()) {
                SettingsHint(
                    stringResource(R.string.alarm_edit_chain_picker_empty_hint),
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
                                            contentDescription = stringResource(R.string.alarm_edit_move_earlier, label),
                                            tint = if (index > 0) TextPrimary else TextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { draftChain = draftChain.moveItem(index, index + 1) },
                                        enabled = index < draftChain.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.alarm_edit_move_later, label),
                                            tint = if (index < draftChain.lastIndex) TextPrimary else TextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { draftChain = draftChain.filterNot { it == type } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.alarm_edit_remove_challenge, label),
                                            tint = AccentRed
                                        )
                                    }
                                }
                            } else {
                                TextButton(onClick = { draftChain = draftChain + type }) {
                                    Text(stringResource(R.string.alarm_edit_add), color = MaterialTheme.colorScheme.primary)
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
                    Text(
                        stringResource(R.string.alarm_edit_clear_chain),
                        color = if (draftChain.isNotEmpty()) AccentRed else TextMuted
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = TextSecondary)
                    }
                    Button(
                        onClick = { onApply(draftChain) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            stringResource(
                                if (draftChain.isEmpty()) R.string.alarm_edit_disable_chain else R.string.alarm_edit_use_chain
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
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
        isEmpty() -> stringResource(R.string.alarm_edit_repeat_once)
        size == orderedDays.size -> stringResource(R.string.alarm_edit_repeat_daily)
        this == weekdaySet -> stringResource(R.string.alarm_edit_repeat_weekdays)
        this == weekendSet -> stringResource(R.string.alarm_edit_repeat_weekends)
        else -> orderedDays
            .filter { it in this }
            .joinToString(", ") { day ->
                day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
            }
    }
}

@Composable
private fun String.toAlarmChallengeSummary(): String {
    val resource = when (this) {
        "NONE" -> R.string.alarm_edit_challenge_none
        "MATH_EASY" -> R.string.alarm_edit_challenge_math_easy
        "MATH_MEDIUM" -> R.string.alarm_edit_challenge_math_medium
        "MATH_HARD" -> R.string.alarm_edit_challenge_math_hard
        "SHAKE" -> R.string.alarm_edit_challenge_shake
        "SEQUENCE" -> R.string.alarm_edit_challenge_sequence
        "MEMORY_PATTERN" -> R.string.alarm_edit_challenge_memory
        "TYPING" -> R.string.alarm_edit_challenge_typing
        "VOICE_PHRASE" -> R.string.alarm_edit_challenge_voice
        "HANDWRITING" -> R.string.alarm_edit_challenge_handwriting
        "WALK_STEPS" -> R.string.alarm_edit_challenge_walk
        "NFC_SCAN" -> R.string.alarm_edit_challenge_nfc
        "BARCODE_SCAN" -> R.string.alarm_edit_challenge_barcode
        "PHOTO_MATCH" -> R.string.alarm_edit_challenge_photo
        "SQUAT" -> R.string.alarm_edit_challenge_squat
        "PUSH_UP" -> R.string.alarm_edit_challenge_pushup
        "PLANK_HOLD" -> R.string.alarm_edit_challenge_plank
        "WIFI_CONNECT" -> R.string.alarm_edit_challenge_wifi
        "MAZE" -> R.string.alarm_edit_challenge_maze
        "COUNT_SHEEP" -> R.string.alarm_edit_challenge_sheep
        "SIMON_SAYS" -> R.string.alarm_edit_challenge_simon
        "DATE_BACKWARDS" -> R.string.alarm_edit_challenge_date
        "STROOP" -> R.string.alarm_edit_challenge_stroop
        "ROCK_PAPER_SCISSORS" -> R.string.alarm_edit_challenge_rps
        "EMOJI_MEMORY" -> R.string.alarm_edit_challenge_emoji
        "TYPING_SPEED" -> R.string.alarm_edit_challenge_speed
        "WORDLE" -> R.string.alarm_edit_challenge_wordle
        "PVT" -> R.string.alarm_edit_challenge_pvt
        "SPOT_DIFFERENCE" -> R.string.alarm_edit_challenge_difference
        "CHESS_MATE" -> R.string.alarm_edit_challenge_chess
        "RSVP_READING" -> R.string.alarm_edit_challenge_rsvp
        else -> null
    }
    if (resource != null) return stringResource(resource)
    val readableCode = replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    return stringResource(R.string.alarm_edit_challenge_unknown, readableCode)
}

@Composable
private fun String.toAlarmChallengeDescription(): String = stringResource(
    when (this) {
        "MATH_EASY" -> R.string.alarm_edit_challenge_math_easy_description
        "MATH_MEDIUM" -> R.string.alarm_edit_challenge_math_medium_description
        "MATH_HARD" -> R.string.alarm_edit_challenge_math_hard_description
        "SHAKE" -> R.string.alarm_edit_challenge_shake_description
        "SEQUENCE" -> R.string.alarm_edit_challenge_sequence_description
        "MEMORY_PATTERN" -> R.string.alarm_edit_challenge_memory_description
        "TYPING" -> R.string.alarm_edit_challenge_typing_description
        "VOICE_PHRASE" -> R.string.alarm_edit_challenge_voice_description
        "HANDWRITING" -> R.string.alarm_edit_challenge_handwriting_description
        "WALK_STEPS" -> R.string.alarm_edit_challenge_walk_description
        "NFC_SCAN" -> R.string.alarm_edit_challenge_nfc_description
        "BARCODE_SCAN" -> R.string.alarm_edit_challenge_barcode_description
        "PHOTO_MATCH" -> R.string.alarm_edit_challenge_photo_description
        "SQUAT" -> R.string.alarm_edit_challenge_squat_description
        "PUSH_UP" -> R.string.alarm_edit_challenge_pushup_description
        "PLANK_HOLD" -> R.string.alarm_edit_challenge_plank_description
        "WIFI_CONNECT" -> R.string.alarm_edit_challenge_wifi_description
        "MAZE" -> R.string.alarm_edit_challenge_maze_description
        "COUNT_SHEEP" -> R.string.alarm_edit_challenge_sheep_description
        "SIMON_SAYS" -> R.string.alarm_edit_challenge_simon_description
        "DATE_BACKWARDS" -> R.string.alarm_edit_challenge_date_description
        "STROOP" -> R.string.alarm_edit_challenge_stroop_description
        "ROCK_PAPER_SCISSORS" -> R.string.alarm_edit_challenge_rps_description
        "EMOJI_MEMORY" -> R.string.alarm_edit_challenge_emoji_description
        "TYPING_SPEED" -> R.string.alarm_edit_challenge_speed_description
        "WORDLE" -> R.string.alarm_edit_challenge_wordle_description
        "PVT" -> R.string.alarm_edit_challenge_pvt_description
        "SPOT_DIFFERENCE" -> R.string.alarm_edit_challenge_difference_description
        "CHESS_MATE" -> R.string.alarm_edit_challenge_chess_description
        "RSVP_READING" -> R.string.alarm_edit_challenge_rsvp_description
        else -> R.string.alarm_edit_challenge_default_description
    }
)

private fun formatLocationDismissTarget(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

@Composable
private fun AlarmEditUiState.challengeSummary(): String {
    if (challengeChain.isNotBlank()) {
        val count = challengeChain.split(",")
            .map { it.trim() }
            .count { it.isNotEmpty() }
        if (count > 0) return pluralStringResource(R.plurals.alarm_edit_step_chain, count, count)
    }
    return challengeType.toAlarmChallengeSummary()
}

@Composable
private fun AlarmEditUiState.soundSummary(): String = stringResource(
    when {
        internetRadioUrl.isNotBlank() -> R.string.alarm_edit_internet_radio
        spotifyUri.isNotBlank() -> R.string.alarm_edit_spotify_short
        ringtoneUri == "silent" -> R.string.alarm_edit_silent_wake
        ringtoneUri.isBlank() -> R.string.alarm_edit_default_sound
        else -> R.string.alarm_edit_custom_tone
    }
)

@Composable
private fun shiftPatternDescription(pattern: ShiftPattern): String = stringResource(
    when (pattern.key) {
        "DDNNO" -> R.string.alarm_edit_shift_ddnno_description
        "FOUR_ON_FOUR_OFF" -> R.string.alarm_edit_shift_four_description
        "PANAMA" -> R.string.alarm_edit_shift_panama_description
        "DUPONT" -> R.string.alarm_edit_shift_dupont_description
        "PITMAN" -> R.string.alarm_edit_shift_pitman_description
        else -> R.string.alarm_edit_shift_default_description
    }
)

@Composable
private fun guardianEditHint(readiness: GuardianReadiness): String {
    val callPath = if (readiness.hasCallPhonePermission) {
        stringResource(R.string.alarm_edit_guardian_call_granted)
    } else {
        stringResource(R.string.alarm_edit_guardian_call_dialer)
    }
    return when (readiness.smsPath) {
        GuardianSmsPath.INACTIVE -> stringResource(R.string.alarm_edit_guardian_inactive)
        GuardianSmsPath.DIRECT_SMS ->
            stringResource(R.string.alarm_edit_guardian_direct_sms, callPath)
        GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION ->
            stringResource(R.string.alarm_edit_guardian_needs_sms, callPath)
        GuardianSmsPath.SMS_COMPOSER ->
            stringResource(R.string.alarm_edit_guardian_composer, callPath)
    }
}

@Composable
private fun alarmChallengeOptions(): List<Pair<String, String>> = listOf(
    "NONE" to stringResource(R.string.alarm_edit_challenge_none),
    "MATH_EASY" to stringResource(R.string.alarm_edit_challenge_math_easy),
    "MATH_MEDIUM" to stringResource(R.string.alarm_edit_challenge_math_medium),
    "MATH_HARD" to stringResource(R.string.alarm_edit_challenge_math_hard),
    "SHAKE" to stringResource(R.string.alarm_edit_challenge_shake),
    "SEQUENCE" to stringResource(R.string.alarm_edit_challenge_sequence),
    "MEMORY_PATTERN" to stringResource(R.string.alarm_edit_challenge_memory),
    "TYPING" to stringResource(R.string.alarm_edit_challenge_typing),
    "VOICE_PHRASE" to stringResource(R.string.alarm_edit_challenge_voice),
    "HANDWRITING" to stringResource(R.string.alarm_edit_challenge_handwriting),
    "WALK_STEPS" to stringResource(R.string.alarm_edit_challenge_walk),
    "NFC_SCAN" to stringResource(R.string.alarm_edit_challenge_nfc),
    "BARCODE_SCAN" to stringResource(R.string.alarm_edit_challenge_barcode),
    "PHOTO_MATCH" to stringResource(R.string.alarm_edit_challenge_photo),
    "SQUAT" to stringResource(R.string.alarm_edit_challenge_squat),
    "WIFI_CONNECT" to stringResource(R.string.alarm_edit_challenge_wifi),
    "MAZE" to stringResource(R.string.alarm_edit_challenge_maze),
    "COUNT_SHEEP" to stringResource(R.string.alarm_edit_challenge_sheep),
    "SIMON_SAYS" to stringResource(R.string.alarm_edit_challenge_simon),
    "DATE_BACKWARDS" to stringResource(R.string.alarm_edit_challenge_date),
    "STROOP" to stringResource(R.string.alarm_edit_challenge_stroop),
    "ROCK_PAPER_SCISSORS" to stringResource(R.string.alarm_edit_challenge_rps),
    "EMOJI_MEMORY" to stringResource(R.string.alarm_edit_challenge_emoji),
    "TYPING_SPEED" to stringResource(R.string.alarm_edit_challenge_speed),
    "WORDLE" to stringResource(R.string.alarm_edit_challenge_wordle),
    "PVT" to stringResource(R.string.alarm_edit_challenge_pvt),
    "SPOT_DIFFERENCE" to stringResource(R.string.alarm_edit_challenge_difference),
    "CHESS_MATE" to stringResource(R.string.alarm_edit_challenge_chess),
    "RSVP_READING" to stringResource(R.string.alarm_edit_challenge_rsvp),
    "PUSH_UP" to stringResource(R.string.alarm_edit_challenge_pushup),
    "PLANK_HOLD" to stringResource(R.string.alarm_edit_challenge_plank)
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

/**
 * v1.12.2 (roadmap N9): pick a short, human-friendly label for a ringtone
 * pool entry. We can't resolve content:// URIs to track titles without
 * touching ContentResolver per-render, so we trim aggressively at the
 * structural boundary instead:
 *   - "content://media/external/audio/media/12345" → "audio/12345"
 *   - "file:///storage/emulated/0/Music/sun.mp3" → "sun.mp3"
 * Anything pathologically long gets truncated with an ellipsis.
 */
private fun ringtoneShortName(uri: String, emptyLabel: String): String {
    val trimmed = uri.trim()
    if (trimmed.isEmpty()) return emptyLabel
    val fileName = trimmed
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { trimmed }
    val safe = fileName.take(28)
    return if (fileName.length > 28) "$safe…" else safe
}
