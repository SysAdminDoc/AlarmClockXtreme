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
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import com.sysadmindoc.alarmclock.util.LocationHelper
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import com.sysadmindoc.alarmclock.worker.GuardianEscalationPolicy
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import java.time.DayOfWeek
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListScope.alarmEditScheduleSections(
    editorPage: AlarmEditorPage,
    state: AlarmEditUiState,
    viewModel: AlarmEditViewModel
) {
    // Schedule forecast
    SettingsSection(editorPage, AlarmEditorSection.UPCOMING) {
        if (state.forecastDates.isNotEmpty()) {
            state.forecastDates.forEach { entry ->
                val instant = java.time.Instant.ofEpochMilli(entry.timeMillis)
                val dt = instant.atZone(java.time.ZoneId.systemDefault())
                val dateStr = dt.format(
                    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                )
                val timeStr = dt.format(AlarmTimeFormatter.formatter(state.is24HourFormat))
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
}
