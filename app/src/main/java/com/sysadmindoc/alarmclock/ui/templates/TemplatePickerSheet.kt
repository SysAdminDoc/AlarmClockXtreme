package com.sysadmindoc.alarmclock.ui.templates

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import com.sysadmindoc.alarmclock.ui.alarmedit.toAlarmChallengeSummary
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerSheet(
    onSelect: (AlarmTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceMedium,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextMuted)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppSectionTitle(
                title = stringResource(R.string.template_quick_start_templates),
                description = stringResource(R.string.template_start_thoughtful_preset_common_routines)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = stringResource(R.string.templates_ready_made_setups, defaultTemplates.size),
                    color = MaterialTheme.colorScheme.primary
                )
                AppStatusChip(
                    label = stringResource(R.string.template_creates_draft_alarm),
                    color = DismissGreen
                )
                AppStatusChip(
                    label = stringResource(R.string.template_fully_editable),
                    color = TextMuted
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(defaultTemplates, key = { it.key }) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onSelect(template) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: AlarmTemplate,
    onClick: () -> Unit
) {
    val accent = templateAccent(template)

    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        highlighted = template.challengeType != "NONE" || template.gradualVolumeSeconds >= 120,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = templateIcon(template),
                    contentDescription = null,
                    tint = accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(template.nameRes),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = templateTimeLabel(template),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = stringResource(template.descriptionRes),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = templateRepeatLabel(template.repeatDays),
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppStatusChip(
                        label = templateWakeStyleLabel(template),
                        color = DismissGreen
                    )
                    if (template.challengeType != "NONE") {
                        AppStatusChip(
                            label = template.challengeType.toAlarmChallengeSummary(),
                            color = SnoozeYellow
                        )
                    }
                    AppStatusChip(
                        label = stringResource(R.string.templates_min_snooze, template.snoozeDurationMinutes),
                        icon = Icons.Default.Snooze,
                        color = AccentBlue
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun templateAccent(template: AlarmTemplate): Color = when {
    template.challengeType != "NONE" -> SnoozeYellow
    template.gradualVolumeSeconds >= 120 -> DismissGreen
    template.key == "work_alarm" -> AccentBlue
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun templateTimeLabel(template: AlarmTemplate): String {
    val isRelative = template.hour == 0 && template.minute > 0 && template.repeatDays.isEmpty()
    if (isRelative) {
        return stringResource(R.string.template_time_now_plus, template.minute)
    }

    val hour12 = when {
        template.hour == 0 -> 12
        template.hour > 12 -> template.hour - 12
        else -> template.hour
    }
    val suffix = if (template.hour < 12) "AM" else "PM"
    return "$hour12:${template.minute.toString().padStart(2, '0')} $suffix"
}

@Composable
private fun templateRepeatLabel(repeatDays: Set<DayOfWeek>): String = when {
    repeatDays.isEmpty() -> stringResource(R.string.template_repeat_one_time)
    repeatDays.size == DayOfWeek.entries.size -> stringResource(R.string.template_repeat_daily)
    repeatDays == setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ) -> stringResource(R.string.template_repeat_weekdays)
    repeatDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) ->
        stringResource(R.string.template_repeat_weekends)
    else -> pluralStringResource(R.plurals.template_repeat_days, repeatDays.size, repeatDays.size)
}

@Composable
private fun templateWakeStyleLabel(template: AlarmTemplate): String = when {
    template.gradualVolumeSeconds == 0 -> stringResource(R.string.template_wake_instant)
    template.gradualVolumeSeconds >= 120 -> stringResource(R.string.template_wake_gentle)
    else -> stringResource(R.string.template_wake_balanced)
}

private fun templateIcon(template: AlarmTemplate): ImageVector = when (template.key) {
    "early_bird" -> Icons.Default.WbTwilight
    "work_alarm" -> Icons.Default.Work
    "weekend_sleep_in" -> Icons.Default.Weekend
    "power_nap" -> Icons.Default.Bedtime
    "heavy_sleeper" -> Icons.Default.AlarmOn
    "medication_reminder" -> Icons.Default.MedicalServices
    else -> Icons.Default.Alarm
}
