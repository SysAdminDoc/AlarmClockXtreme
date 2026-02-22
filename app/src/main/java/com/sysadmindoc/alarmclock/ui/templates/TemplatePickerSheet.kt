package com.sysadmindoc.alarmclock.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.*

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
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Alarm Templates",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(defaultTemplates) { template ->
                    TemplateCard(template = template, onClick = { onSelect(template) })
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: AlarmTemplate, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = templateIcon(template),
                contentDescription = template.name,
                tint = AccentBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    template.description,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                // Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (template.repeatDays.isNotEmpty()) {
                        Tag(when {
                            template.repeatDays.size == 7 -> "Daily"
                            template.repeatDays.size == 5 -> "Weekdays"
                            template.repeatDays.size == 2 -> "Weekends"
                            else -> "${template.repeatDays.size} days"
                        })
                    }
                    if (template.challengeType != "NONE") {
                        Tag(template.challengeType.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })
                    }
                    if (template.gradualVolumeSeconds > 60) {
                        Tag("Gentle")
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select template",
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = AccentBlue.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = AccentBlue,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun templateIcon(template: AlarmTemplate): ImageVector = when {
    template.name.contains("Early") -> Icons.Default.WbTwilight
    template.name.contains("Work") -> Icons.Default.Work
    template.name.contains("Weekend") -> Icons.Default.Weekend
    template.name.contains("Nap") -> Icons.Default.Bedtime
    template.name.contains("Heavy") -> Icons.Default.AlarmOn
    template.name.contains("Medication") -> Icons.Default.MedicalServices
    else -> Icons.Default.Alarm
}
