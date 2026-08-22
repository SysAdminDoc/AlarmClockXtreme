package com.sysadmindoc.alarmclock.ui.alarmedit

import android.Manifest
import android.content.Context
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

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListScope.alarmEditDismissSections(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel,
    context: Context,
    onCaptureReferencePhoto: () -> Unit,
    photoReferenceStatus: String,
    requestLocationDismissTarget: () -> Unit,
    locationDismissStatus: String,
    onOpenChainPicker: () -> Unit
) {
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

        var showSnoozeLimitMenu by remember { mutableStateOf(false) }
        SettingsRow(label = stringResource(R.string.alarm_edit_snooze_limit)) {
            Box {
                SettingsValueButton(
                    label = snoozeLimitLabel(state.maxSnoozeCount),
                    onClick = { showSnoozeLimitMenu = true }
                )
                DropdownMenu(
                    expanded = showSnoozeLimitMenu,
                    onDismissRequest = { showSnoozeLimitMenu = false }
                ) {
                    listOf(0, 1, 2, 3, 5, 10).forEach { limit ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    snoozeLimitLabel(limit),
                                    color = if (limit == state.maxSnoozeCount) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        TextPrimary
                                    }
                                )
                            },
                            onClick = {
                                viewModel.updateMaxSnoozeCount(limit)
                                showSnoozeLimitMenu = false
                            }
                        )
                    }
                }
            }
        }
        SettingsHint(
            stringResource(R.string.alarm_edit_snooze_limit_hint),
            tone = HintTone.Neutral
        )
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
                    onClick = onCaptureReferencePhoto,
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
                    onClick = onOpenChainPicker
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
}

/**
 * Label for the per-alarm snooze cap. 0 is stored as "no limit".
 */
@Composable
private fun snoozeLimitLabel(limit: Int): String =
    if (limit <= 0) {
        stringResource(R.string.alarm_edit_snooze_limit_unlimited)
    } else {
        pluralStringResource(R.plurals.alarm_edit_snooze_limit_value, limit, limit)
    }
