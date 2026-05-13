package com.sysadmindoc.alarmclock.ui.templates

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
                title = "Quick-start templates",
                description = "Start with a thoughtful preset for common routines, then fine-tune the details afterward."
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = "${defaultTemplates.size} ready-made setups",
                    color = MaterialTheme.colorScheme.primary
                )
                AppStatusChip(
                    label = "Creates a draft alarm",
                    color = DismissGreen
                )
                AppStatusChip(
                    label = "Fully editable",
                    color = TextMuted
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(defaultTemplates, key = { it.name }) { template ->
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
                    text = template.name,
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
                    text = template.description,
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
                            label = templateChallengeLabel(template.challengeType),
                            color = SnoozeYellow
                        )
                    }
                    AppStatusChip(
                        label = "${template.snoozeDurationMinutes} min snooze",
                        icon = Icons.Default.Snooze,
                        color = AccentBlue
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Use template",
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun templateAccent(template: AlarmTemplate): Color = when {
    template.challengeType != "NONE" -> SnoozeYellow
    template.gradualVolumeSeconds >= 120 -> DismissGreen
    template.name.contains("Work", ignoreCase = true) -> AccentBlue
    else -> MaterialTheme.colorScheme.primary
}

private fun templateTimeLabel(template: AlarmTemplate): String {
    val isRelative = template.hour == 0 && template.minute > 0 && template.repeatDays.isEmpty()
    if (isRelative) {
        return "Now + ${template.minute} min"
    }

    val hour12 = when {
        template.hour == 0 -> 12
        template.hour > 12 -> template.hour - 12
        else -> template.hour
    }
    val suffix = if (template.hour < 12) "AM" else "PM"
    return "$hour12:${template.minute.toString().padStart(2, '0')} $suffix"
}

private fun templateRepeatLabel(repeatDays: Set<DayOfWeek>): String = when {
    repeatDays.isEmpty() -> "One-time"
    repeatDays.size == DayOfWeek.entries.size -> "Daily"
    repeatDays == setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ) -> "Weekdays"
    repeatDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    else -> "${repeatDays.size} days"
}

private fun templateWakeStyleLabel(template: AlarmTemplate): String = when {
    template.gradualVolumeSeconds == 0 -> "Instant ring"
    template.gradualVolumeSeconds >= 120 -> "Gentle wake"
    else -> "Balanced wake"
}

private fun templateChallengeLabel(challengeType: String): String = when (challengeType) {
    "MATH_EASY" -> "Easy math"
    "MATH_HARD" -> "Hard math"
    "SHAKE" -> "Shake phone"
    else -> challengeType
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
}

private fun templateIcon(template: AlarmTemplate): ImageVector = when {
    template.name.contains("Early", ignoreCase = true) -> Icons.Default.WbTwilight
    template.name.contains("Work", ignoreCase = true) -> Icons.Default.Work
    template.name.contains("Weekend", ignoreCase = true) -> Icons.Default.Weekend
    template.name.contains("Nap", ignoreCase = true) -> Icons.Default.Bedtime
    template.name.contains("Heavy", ignoreCase = true) -> Icons.Default.AlarmOn
    template.name.contains("Medication", ignoreCase = true) -> Icons.Default.MedicalServices
    else -> Icons.Default.Alarm
}
