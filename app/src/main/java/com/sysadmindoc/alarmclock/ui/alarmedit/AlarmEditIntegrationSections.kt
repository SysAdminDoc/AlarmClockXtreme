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
internal fun LazyListScope.alarmEditIntegrationSections(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel,
    context: Context
) {
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
}
