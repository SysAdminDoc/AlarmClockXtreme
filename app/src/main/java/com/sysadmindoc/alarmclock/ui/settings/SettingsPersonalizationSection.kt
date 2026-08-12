package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.ui.components.AppFeedbackCard
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.adaptive.shouldUseTwoPaneLayout
import com.sysadmindoc.alarmclock.data.backup.BackupExportWarning
import com.sysadmindoc.alarmclock.data.backup.BackupImportMode
import com.sysadmindoc.alarmclock.data.backup.BackupImportOptions
import com.sysadmindoc.alarmclock.data.backup.BackupImportPreview
import com.sysadmindoc.alarmclock.data.backup.FossifyImportErrorKind
import com.sysadmindoc.alarmclock.data.backup.FossifyImportException
import com.sysadmindoc.alarmclock.data.backup.FossifyImportPreview
import com.sysadmindoc.alarmclock.data.health.HealthConnectAvailability
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof
import com.sysadmindoc.alarmclock.data.support.SupportExportFile
import com.sysadmindoc.alarmclock.ui.permissions.PermissionRequestCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PersonalizationSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = stringResource(R.string.settings_personalization),
        description = stringResource(R.string.settings_personalization_description)
    ) {
        AccentColorPicker(
            currentHex = state.settings.accentColor,
            onPick = viewModel::updateAccentColor
        )

        SettingsToggle(
            label = stringResource(R.string.settings_motivational_quotes),
            checked = state.settings.showMotivationalQuotes,
            supportingText = stringResource(R.string.settings_motivational_quotes_description),
            onToggle = viewModel::toggleShowMotivationalQuotes
        )
        SettingsToggle(
            label = stringResource(R.string.settings_adaptive_difficulty),
            checked = state.settings.adaptiveDifficultyEnabled,
            supportingText = stringResource(R.string.settings_adaptive_difficulty_description),
            onToggle = viewModel::toggleAdaptiveDifficulty
        )

        // v1.4.0: Material You — respects the user's wallpaper palette on Android 12+.
        // On older devices the toggle is still persisted but has no visual effect,
        // so the help copy names the requirement rather than silently no-op'ing.
        SettingsToggle(
            label = stringResource(R.string.settings_dynamic_color),
            checked = state.settings.dynamicColorEnabled,
            supportingText = stringResource(R.string.settings_dynamic_color_description),
            onToggle = viewModel::toggleDynamicColor
        )

        SettingsToggle(
            label = stringResource(R.string.settings_expressive_surfaces),
            checked = state.settings.expressiveModeEnabled,
            supportingText = stringResource(R.string.settings_expressive_surfaces_description),
            onToggle = viewModel::toggleExpressiveMode
        )

        SettingsToggle(
            label = stringResource(R.string.settings_reduce_motion),
            checked = state.settings.reduceMotionAndFlashing,
            supportingText = stringResource(R.string.settings_reduce_motion_description),
            onToggle = viewModel::toggleReduceMotionAndFlashing
        )

        SettingsToggle(
            label = stringResource(R.string.settings_cover_to_snooze),
            checked = state.settings.coverToSnoozeEnabled,
            supportingText = stringResource(R.string.settings_cover_to_snooze_description),
            onToggle = viewModel::toggleCoverToSnooze
        )

        SettingsToggle(
            label = stringResource(R.string.settings_repeat_missed),
            checked = state.settings.repeatMissedAlarms,
            supportingText = stringResource(R.string.settings_repeat_missed_description),
            onToggle = viewModel::toggleRepeatMissed
        )

        var showLockMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_cancellation_lock), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_cancellation_lock_description),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showLockMenu = true }) {
                    Text(
                        if (state.settings.cancellationLockMinutes == 0) {
                            stringResource(R.string.settings_disabled)
                        } else {
                            stringResource(R.string.settings_minutes_short, state.settings.cancellationLockMinutes)
                        },
                        color = AccentBlue
                    )
                }
                DropdownMenu(expanded = showLockMenu, onDismissRequest = { showLockMenu = false }) {
                    listOf(0, 15, 30, 60).forEach { mins ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (mins == 0) {
                                        stringResource(R.string.settings_disabled)
                                    } else {
                                        stringResource(R.string.settings_lock_before_fire, mins)
                                    }
                                )
                            },
                            onClick = { viewModel.updateCancellationLockMinutes(mins); showLockMenu = false }
                        )
                    }
                }
            }
        }

        var showHoldThresholdMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_long_press_threshold), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_long_press_threshold_description),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showHoldThresholdMenu = true }) {
                    Text(
                        stringResource(
                            R.string.settings_hold_duration_short,
                            state.settings.holdToDismissMillis / 1000f
                        ),
                        color = AccentBlue
                    )
                }
                DropdownMenu(
                    expanded = showHoldThresholdMenu,
                    onDismissRequest = { showHoldThresholdMenu = false }
                ) {
                    listOf(500, 1_000, 1_500, 2_500, 4_000, 5_000).forEach { millis ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.settings_hold_duration_short,
                                        millis / 1000f
                                    )
                                )
                            },
                            onClick = {
                                viewModel.updateHoldToDismissMillis(millis)
                                showHoldThresholdMenu = false
                            }
                        )
                    }
                }
            }
        }

        var showFiringModeMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_firing_controls), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_firing_controls_description),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box {
                TextButton(onClick = { showFiringModeMenu = true }) {
                    Text(
                        when (state.settings.firingControlMode) {
                            "buttons" -> stringResource(R.string.settings_firing_buttons)
                            "swipe" -> stringResource(R.string.settings_firing_swipe)
                            else -> stringResource(R.string.settings_firing_hybrid)
                        },
                        color = AccentBlue
                    )
                }
                DropdownMenu(expanded = showFiringModeMenu, onDismissRequest = { showFiringModeMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_firing_hybrid_description)) },
                        onClick = { viewModel.updateFiringControlMode("hybrid"); showFiringModeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_firing_buttons_description)) },
                        onClick = { viewModel.updateFiringControlMode("buttons"); showFiringModeMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_firing_swipe_description)) },
                        onClick = { viewModel.updateFiringControlMode("swipe"); showFiringModeMenu = false }
                    )
                }
            }
        }

        BufferedSettingsTextField(
            value = state.settings.customTypingPhrases,
            onCommit = viewModel::updateCustomTypingPhrases,
            label = { Text(stringResource(R.string.settings_custom_typing_phrases), color = TextMuted) },
            placeholder = { Text(stringResource(R.string.settings_custom_typing_phrases_placeholder), color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6
        )
        Text(
            text = stringResource(R.string.settings_custom_typing_phrases_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        SettingsToggle(
            label = stringResource(R.string.settings_challenge_bypass),
            supportingText = stringResource(R.string.settings_challenge_bypass_description),
            checked = state.settings.challengeBypassEnabled,
            onToggle = { viewModel.updateChallengeBypassEnabled(it) }
        )
        if (state.settings.challengeBypassEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_bypass_delay), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(10, 30, 60, 120).forEach { secs ->
                        val selected = state.settings.challengeBypassDelaySeconds == secs
                        AppFilterChip(
                            selected = selected,
                            onClick = { viewModel.updateChallengeBypassDelay(secs) },
                            label = stringResource(R.string.settings_seconds_short, secs)
                        )
                    }
                }
            }
        }

        SettingsToggle(
            label = stringResource(R.string.settings_challenge_audio_ducking),
            supportingText = stringResource(R.string.settings_challenge_audio_ducking_description),
            checked = state.settings.challengeAudioDuckingEnabled,
            onToggle = viewModel::updateChallengeAudioDuckingEnabled
        )
        if (state.settings.challengeAudioDuckingEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_challenge_volume, state.settings.challengeAudioDuckPercent),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 35, 50, 65).forEach { percent ->
                        AppFilterChip(
                            selected = state.settings.challengeAudioDuckPercent == percent,
                            onClick = { viewModel.updateChallengeAudioDuckPercent(percent) },
                            label = stringResource(R.string.settings_percent, percent)
                        )
                    }
                }
            }
        }

        // v1.4.0: Pre-sleep checklist items, shown on the Bedtime tab.
        BufferedSettingsTextField(
            value = state.settings.bedtimeChecklist,
            onCommit = viewModel::updateBedtimeChecklist,
            label = { Text(stringResource(R.string.settings_bedtime_checklist), color = TextMuted) },
            placeholder = { Text(stringResource(R.string.settings_bedtime_checklist_placeholder), color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6
        )
        Text(
            text = stringResource(R.string.settings_bedtime_checklist_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AccentColorPicker(currentHex: String, onPick: (String) -> Unit) {
    // Six-swatch palette covers the most common requests (cool/warm/mono).
    // Listed in the order users tend to reach for them; the first one is
    // the historical default so users always have an obvious "reset" path.
    val palette = listOf(
        "#5B9EF4" to R.string.settings_accent_default_blue,
        "#7C5CFF" to R.string.settings_accent_violet,
        "#FF6F8A" to R.string.settings_accent_coral,
        "#FFB347" to R.string.settings_accent_amber,
        "#5BD49A" to R.string.settings_accent_mint,
        "#E0E4EA" to R.string.settings_accent_mono
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_accent_color),
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.settings_accent_color_description),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            palette.forEach { (hex, labelRes) ->
                val isSelected = hex.equals(currentHex, ignoreCase = true)
                val label = stringResource(labelRes)
                val accentContentDescription = stringResource(R.string.settings_accent_semantics, label)
                val accentStateDescription = stringResource(
                    if (isSelected) R.string.settings_selected else R.string.settings_not_selected
                )
                val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }
                    .getOrDefault(androidx.compose.ui.graphics.Color.Gray)
                val swatchShape = RoundedCornerShape(8.dp)
                val selectedIconTint = if (hex in lightAccentSwatches) SurfaceDark else TextPrimary
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(swatchShape)
                        .background(color)
                        .clickable(
                            role = Role.RadioButton,
                            onClickLabel = stringResource(R.string.settings_use_accent, label),
                            onClick = { onPick(hex) }
                        )
                        .semantics {
                            contentDescription = accentContentDescription
                            selected = isSelected
                            stateDescription = accentStateDescription
                        }
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = TextPrimary,
                                    shape = swatchShape
                                )
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = BorderSubtle,
                                    shape = swatchShape
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = selectedIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private val lightAccentSwatches = setOf("#FFB347", "#5BD49A", "#E0E4EA")
