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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.sysadmindoc.alarmclock.ui.radio.RadioPlaylistStatus
import com.sysadmindoc.alarmclock.ui.radio.RadioPreview
import com.sysadmindoc.alarmclock.ui.radio.RadioStationViewModel
import com.sysadmindoc.alarmclock.ui.radio.StopRadioPreviewWhenHidden
import com.sysadmindoc.alarmclock.ui.radio.streamUrl
import com.sysadmindoc.alarmclock.ui.theme.*
import com.sysadmindoc.alarmclock.util.LocationHelper
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import com.sysadmindoc.alarmclock.worker.GuardianEscalationPolicy
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import java.time.DayOfWeek
import java.util.Locale
import com.sysadmindoc.alarmclock.ui.components.AppButtonShape

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListScope.alarmEditSoundSections(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel,
    radioViewModel: RadioStationViewModel,
    onOpenRingtonePicker: () -> Unit,
    onOpenRadioSearch: () -> Unit
) {
    // Sound settings
    SettingsSection(editorPage, AlarmEditorSection.SOUND) {
        val hapticOnlyActive = state.overrideSystemVolume && state.volume == 0 && state.vibrationEnabled

        SettingsRow(label = stringResource(R.string.alarm_edit_partner_mode)) {
            OutlinedButton(
                onClick = viewModel::applyDontWakePartnerProfile,
                shape = AppButtonShape,
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
                color = MaterialTheme.colorScheme.primary
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
                    onClick = onOpenRingtonePicker
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
                        else -> editorDuration(state.gradualVolumeSeconds)
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
                                        else -> editorDuration(secs)
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

    // Internet radio replaces the ringtone, so it sits next to it (#65). It
    // used to be the eleventh card on Integrations, where nobody found it.
    SettingsSection(editorPage, AlarmEditorSection.RADIO) {
        InternetRadioContent(
            state = state,
            viewModel = viewModel,
            radioViewModel = radioViewModel,
            onOpenRadioSearch = onOpenRadioSearch
        )
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
                            else -> editorDuration(state.vibrationDelaySeconds)
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
}

@Composable
private fun ColumnScope.InternetRadioContent(
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel,
    radioViewModel: RadioStationViewModel,
    onOpenRadioSearch: () -> Unit
) {
    val playlistStatus by radioViewModel.playlist.collectAsStateWithLifecycle()
    val previewState by radioViewModel.previewState.collectAsStateWithLifecycle()
    val currentUrl by rememberUpdatedState(state.internetRadioUrl)
    val url = state.internetRadioUrl.trim()

    StopRadioPreviewWhenHidden(radioViewModel)
    LaunchedEffect(state.internetRadioUrl) {
        radioViewModel.onStreamUrlChanged(state.internetRadioUrl)
    }
    // Media3 plays the stream a .pls or .m3u points at, never the playlist
    // itself, so a pasted playlist link rang the fallback tone every time.
    LaunchedEffect(playlistStatus) {
        val replaced = playlistStatus as? RadioPlaylistStatus.Replaced ?: return@LaunchedEffect
        if (currentUrl.trim() == replaced.playlistUrl) {
            viewModel.updateInternetRadioUrl(replaced.streamUrl)
        }
    }

    val isHttps = url.startsWith("https://", ignoreCase = true)
    val previewActive = (previewState is RadioPreview.State.Loading || previewState is RadioPreview.State.Playing) &&
        previewState.streamUrl == url
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onOpenRadioSearch,
            modifier = Modifier.weight(1f),
            shape = AppButtonShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.radio_find_station), fontWeight = FontWeight.SemiBold)
        }
        if (isHttps && playlistStatus != RadioPlaylistStatus.Reading) {
            OutlinedButton(
                onClick = { radioViewModel.togglePreview(url) },
                shape = AppButtonShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (previewActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (previewActive) stringResource(R.string.radio_stop) else stringResource(R.string.radio_play),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Cleartext is blocked at this targetSdk, so an http stream silently
    // fell back to the default tone with nothing said about why.
    val radioNeedsHttps = url.isNotEmpty() && !isHttps
    OutlinedTextField(
        value = state.internetRadioUrl,
        onValueChange = viewModel::updateInternetRadioUrl,
        label = { Text(stringResource(R.string.alarm_edit_stream_url), color = TextMuted) },
        placeholder = { Text(stringResource(R.string.alarm_edit_default_ringtone_placeholder), color = TextMuted) },
        isError = radioNeedsHttps || playlistStatus == RadioPlaylistStatus.OnlyInsecure,
        colors = appOutlinedTextFieldColors(),
        shape = AppInputShape,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        singleLine = true
    )
    when (playlistStatus) {
        RadioPlaylistStatus.Reading -> SettingsHint(
            stringResource(R.string.alarm_edit_radio_playlist_reading),
            tone = HintTone.Neutral
        )
        is RadioPlaylistStatus.Replaced -> SettingsHint(
            stringResource(R.string.alarm_edit_radio_playlist_replaced),
            tone = HintTone.Neutral
        )
        RadioPlaylistStatus.OnlyInsecure -> SettingsHint(
            stringResource(R.string.alarm_edit_radio_playlist_insecure),
            tone = HintTone.Warning
        )
        RadioPlaylistStatus.Unreadable -> SettingsHint(
            stringResource(R.string.alarm_edit_radio_playlist_unreadable),
            tone = HintTone.Warning
        )
        RadioPlaylistStatus.None -> if (radioNeedsHttps) {
            SettingsHint(
                stringResource(R.string.alarm_edit_radio_https_required),
                tone = HintTone.Warning
            )
        }
    }
    val failed = previewState as? RadioPreview.State.Failed
    if (failed != null && failed.url == url) {
        SettingsHint(
            stringResource(R.string.radio_preview_failed_body),
            tone = HintTone.Warning
        )
    }
    SettingsHint(
        stringResource(R.string.alarm_edit_radio_hint),
        tone = HintTone.Neutral
    )
}
