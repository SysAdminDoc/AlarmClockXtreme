package com.sysadmindoc.alarmclock.ui.alarmedit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.lazy.LazyListState
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
import kotlinx.coroutines.CancellationException

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
    // Backup sound sits with the rest of the anti-snooze behaviour, directly
    // under the snooze settings it modifies.
    ANTI_SNOOZE(AlarmEditorPage.DISMISS, R.string.alarm_edit_anti_snooze, R.string.alarm_edit_section_anti_snooze_description),
    UPCOMING(AlarmEditorPage.SCHEDULE, R.string.alarm_edit_upcoming_dates, R.string.alarm_edit_section_upcoming_description),
    DISMISS_CHALLENGE(AlarmEditorPage.DISMISS, R.string.dismiss_challenge, R.string.alarm_edit_section_challenge_description),
    // Directly after the challenge picker: a chain replaces the single
    // challenge, which was not discoverable with a card in between.
    CHAIN(AlarmEditorPage.DISMISS, R.string.alarm_edit_mission_chain, R.string.alarm_edit_section_chain_description),
    LOCATION(AlarmEditorPage.DISMISS, R.string.alarm_edit_location_lock, R.string.alarm_edit_section_location_description),
    WAKE_EFFECTS(AlarmEditorPage.WAKE, R.string.alarm_edit_wake_effects, R.string.alarm_edit_section_wake_effects_description),
    ANNOUNCEMENT(AlarmEditorPage.WAKE, R.string.alarm_edit_announcement, R.string.alarm_edit_section_announcement_description),
    WAKE_CONFIRM(AlarmEditorPage.WAKE, R.string.alarm_edit_wake_confirmation, R.string.alarm_edit_section_wake_confirmation_description),
    SMART_ALARM(AlarmEditorPage.SCHEDULE, R.string.alarm_edit_smart_alarm, R.string.alarm_edit_section_smart_alarm_description),
    HOLIDAYS(AlarmEditorPage.SCHEDULE, R.string.alarm_edit_holidays, R.string.alarm_edit_section_holidays_description),
    SPOTIFY(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_spotify, R.string.alarm_edit_section_spotify_description),
    HUE(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_hue, R.string.alarm_edit_section_hue_description),
    SUNRISE(AlarmEditorPage.WAKE, R.string.alarm_edit_sunrise, R.string.alarm_edit_section_sunrise_description),
    RADIO(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_internet_radio, R.string.alarm_edit_section_radio_description),
    GUARDIAN(AlarmEditorPage.INTEGRATIONS, R.string.alarm_edit_guardian, R.string.alarm_edit_section_guardian_description),
    ROUTINE(AlarmEditorPage.WAKE, R.string.alarm_edit_morning_routine, R.string.alarm_edit_section_routine_description),
    ADVANCED(AlarmEditorPage.ADVANCED, R.string.alarm_edit_advanced, R.string.alarm_edit_section_advanced_description)
}

internal val LocalAlarmEditorPage = staticCompositionLocalOf { AlarmEditorPage.OVERVIEW }

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
    // One saved position per page. A single shared state meant returning from
    // a category scrolled the overview back to the top, so working down the
    // list lost your place every time.
    var overviewScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var overviewScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    val editorScrollState = remember(editorPage) {
        if (editorPage == AlarmEditorPage.OVERVIEW) {
            LazyListState(overviewScrollIndex, overviewScrollOffset)
        } else {
            LazyListState()
        }
    }
    if (editorPage == AlarmEditorPage.OVERVIEW) {
        LaunchedEffect(editorScrollState) {
            snapshotFlow {
                editorScrollState.firstVisibleItemIndex to
                    editorScrollState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                overviewScrollIndex = index
                overviewScrollOffset = offset
            }
        }
    }
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
    PredictiveBackHandler(enabled = !state.notFound) { progress ->
        try {
            progress.collect { }
            requestNavigateBack()
        } catch (_: CancellationException) {
            // The user cancelled the gesture; keep the editor open.
        }
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
                                color = LocalContentColor.current
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
            when (editorPage) {
                AlarmEditorPage.OVERVIEW -> alarmEditOverviewSections(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel,
                    onEditTime = {
                        // v1.13.15: sticky numpad mode opens prefilled with the
                        // current time instead of empty digits + disabled Save.
                        timeNumpadDigits = if (useTimeNumpad) {
                            formatAlarmNumpadDigits(state.hour, state.minute, state.is24HourFormat)
                        } else {
                            ""
                        }
                        timeNumpadIsPm = state.hour >= 12
                        showTimePicker = true
                    },
                    onSelectPage = { page -> editorPageName = page.name }
                )
                AlarmEditorPage.SOUND -> alarmEditSoundSections(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel,
                    onOpenRingtonePicker = { showRingtonePicker = true }
                )
                AlarmEditorPage.DISMISS -> alarmEditDismissSections(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel,
                    context = context,
                    onCaptureReferencePhoto = captureReferencePhoto,
                    photoReferenceStatus = photoReferenceStatus,
                    requestLocationDismissTarget = requestLocationDismissTarget,
                    locationDismissStatus = locationDismissStatus,
                    onOpenChainPicker = { showChainPicker = true }
                )
                AlarmEditorPage.SCHEDULE -> alarmEditScheduleSections(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel
                )
                AlarmEditorPage.WAKE -> alarmEditWakeSections(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel,
                    onChooseBackground = {
                        firingBackgroundImageLauncher.launch(arrayOf("image/*"))
                    },
                    firingBackgroundStatus = firingBackgroundStatus
                )
                AlarmEditorPage.INTEGRATIONS -> alarmEditIntegrationSections(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel,
                    context = context
                )
                AlarmEditorPage.ADVANCED -> alarmEditAdvancedSection(
                    editorPage = editorPage,
                    state = state,
                    viewModel = viewModel
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
