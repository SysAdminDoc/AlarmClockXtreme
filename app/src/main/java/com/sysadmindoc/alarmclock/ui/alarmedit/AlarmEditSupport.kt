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

@Composable
internal fun AlarmTimeNumpad(
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
internal fun DaySelector(
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
internal fun AlarmEditorCategoryOverview(
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
                        // Cards wrap their content now, so a multi-column row
                        // has to equalise heights itself or a two-line summary
                        // next to a one-line one leaves the shorter card short.
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { category ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
            .fillMaxHeight()
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

internal fun LazyListScope.SettingsSection(
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
internal fun SettingsSectionContent(
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
internal fun CollapsibleGroup(
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
internal fun SettingsRow(
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
internal fun SettingsValueButton(
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

internal enum class HintTone {
    Neutral,
    Warning,
    Danger
}

@Composable
internal fun SettingsHint(
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
internal fun ChallengeChainPickerSheet(
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
internal fun Set<DayOfWeek>.toAlarmRepeatSummary(): String {
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
internal fun String.toAlarmChallengeSummary(): String {
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
internal fun String.toAlarmChallengeDescription(): String = stringResource(
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

internal fun formatLocationDismissTarget(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

@Composable
internal fun AlarmEditUiState.challengeSummary(): String {
    if (challengeChain.isNotBlank()) {
        val count = challengeChain.split(",")
            .map { it.trim() }
            .count { it.isNotEmpty() }
        if (count > 0) return pluralStringResource(R.plurals.alarm_edit_step_chain, count, count)
    }
    return challengeType.toAlarmChallengeSummary()
}

@Composable
internal fun AlarmEditUiState.soundSummary(): String = stringResource(
    when {
        internetRadioUrl.isNotBlank() -> R.string.alarm_edit_internet_radio
        spotifyUri.isNotBlank() -> R.string.alarm_edit_spotify_short
        ringtoneUri == "silent" -> R.string.alarm_edit_silent_wake
        ringtoneUri.isBlank() -> R.string.alarm_edit_default_sound
        else -> R.string.alarm_edit_custom_tone
    }
)

@Composable
internal fun shiftPatternDescription(pattern: ShiftPattern): String = stringResource(
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
internal fun guardianEditHint(readiness: GuardianReadiness): String {
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
internal fun alarmChallengeOptions(): List<Pair<String, String>> = listOf(
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

internal fun String.toChallengeChainList(): List<String> = split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

internal fun List<String>.toChallengeChainValue(): String = joinToString(",")

internal fun List<String>.moveItem(fromIndex: Int, toIndex: Int): List<String> {
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
internal fun ringtoneShortName(uri: String, emptyLabel: String): String {
    val trimmed = uri.trim()
    if (trimmed.isEmpty()) return emptyLabel
    val fileName = trimmed
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { trimmed }
    val safe = fileName.take(28)
    return if (fileName.length > 28) "$safe…" else safe
}
