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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import com.sysadmindoc.alarmclock.util.LocationHelper
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import com.sysadmindoc.alarmclock.worker.GuardianEscalationPolicy
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import java.time.DayOfWeek
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListScope.alarmEditOverviewSections(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel,
    onEditTime: () -> Unit,
    onSelectPage: (AlarmEditorPage) -> Unit
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
                .clickable(role = Role.Button, onClick = onEditTime)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.is24HourFormat) {
                Text(
                    text = AlarmTimeFormatter.format(state.hour, state.minute, is24Hour = true),
                    style = ClockTimeLarge,
                    color = TextPrimary
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    val hour12 = if (state.hour % 12 == 0) 12 else state.hour % 12
                    val locale = LocalConfiguration.current.locales[0]
                    val amPm = java.time.LocalTime.of(state.hour, state.minute)
                        .format(java.time.format.DateTimeFormatter.ofPattern("a"))
                    Text(
                        text = String.format(locale, "%d:%02d", hour12, state.minute),
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
                    onSelect = onSelectPage
                )
        }
    }
}
