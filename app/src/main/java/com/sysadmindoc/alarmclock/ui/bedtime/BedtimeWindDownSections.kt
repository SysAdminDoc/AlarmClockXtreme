package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

@Composable
internal fun SleepCycleOptionRow(index: Int, option: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard.copy(alpha = if (index == 0) 0.82f else 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppStatusChip(
                label = if (index == 0) "Best match" else "${index + 1}",
                color = if (index == 0) DismissGreen else MaterialTheme.colorScheme.primary
            )
            Text(
                text = option,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
internal fun WindDownChecklistSection(
    state: BedtimeUiState,
    onToggle: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier) {
        AppSectionTitle(
            title = "Wind-down checklist",
            description = "Tick each step as you settle in for the night.",
            action = {
                AppStatusChip(
                    label = "${state.bedtimeChecklistDone.size}/${state.bedtimeChecklist.size} done",
                    icon = Icons.Default.CheckCircle,
                    color = if (state.bedtimeChecklistDone.isEmpty()) TextMuted else DismissGreen
                )
            }
        )

        state.bedtimeChecklist.forEachIndexed { index, item ->
            val done = index in state.bedtimeChecklistDone
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    // toggleable, not clickable(role = Checkbox): the role alone
                    // says "checkbox" without ever saying whether it is ticked.
                    .toggleable(
                        value = done,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(index) }
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (done) DismissGreen.copy(alpha = 0.09f) else SurfaceCard.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Bedtime,
                        contentDescription = if (done) "Completed" else "Not done",
                        tint = if (done) DismissGreen else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = item,
                        color = if (done) TextMuted else TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    AppStatusChip(
                        label = if (done) "Done" else "Up next",
                        color = if (done) DismissGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (state.bedtimeChecklistDone.isNotEmpty()) {
            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Reset checklist", color = TextSecondary)
            }
        }
    }
}
