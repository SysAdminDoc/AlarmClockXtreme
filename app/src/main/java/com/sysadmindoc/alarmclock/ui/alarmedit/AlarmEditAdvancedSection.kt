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
internal fun LazyListScope.alarmEditAdvancedSection(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel
) {
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
}
