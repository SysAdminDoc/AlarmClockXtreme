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
internal fun LazyListScope.alarmEditWakeSections(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel,
    onChooseBackground: () -> Unit,
    firingBackgroundStatus: String
) {
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
                    onClick = { onChooseBackground() }
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
}
