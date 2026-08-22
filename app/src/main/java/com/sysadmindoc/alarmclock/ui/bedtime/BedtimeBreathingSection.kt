package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.domain.BreathingPattern
import com.sysadmindoc.alarmclock.domain.formatBreathingDuration
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

@Composable
internal fun BreathingExerciseSection(
    pattern: BreathingPattern,
    elapsedSeconds: Int,
    running: Boolean,
    onPatternSelected: (BreathingPattern) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = pattern.phaseAt(elapsedSeconds)
    val remainingSessionSeconds = (pattern.totalSeconds - elapsedSeconds).coerceAtLeast(0)

    AppSurfaceCard(
        modifier = modifier,
        highlighted = running
    ) {
        AppSectionTitle(
            title = stringResource(R.string.breathing_guided_breathing),
            description = stringResource(
                if (running) {
                    R.string.breathing_running_description
                } else {
                    R.string.breathing_idle_description
                }
            )
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BreathingPattern.entries.forEach { option ->
                AppFilterChip(
                    label = option.displayName,
                    selected = option == pattern,
                    onClick = { onPatternSelected(option) },
                    selectionSemantics = true
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard.copy(alpha = 0.72f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = stringResource(
                        R.string.breathing_cycle_progress,
                        phase.cycleNumber,
                        phase.cycleCount
                    ),
                    icon = Icons.Default.Schedule,
                    color = if (phase.completed) DismissGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(phase.labelRes),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    // "Inhale" / "Hold" / "Exhale" is the whole exercise; a
                    // screen-reader user needs it read as it changes.
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                )
                Text(
                    text = if (phase.completed) stringResource(R.string.bedtime_done) else "${phase.remainingSeconds}",
                    color = if (phase.completed) DismissGreen else SnoozeYellow,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(phase.cueRes),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.bedtime_left, formatBreathingDuration(remainingSessionSeconds)),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppFilterChip(
                label = if (running) stringResource(R.string.alarm_list_pause) else if (phase.completed) stringResource(R.string.timer_restart) else stringResource(R.string.challenge_start),
                selected = running,
                onClick = onToggleRunning,
                selectionSemantics = false,
                accessibilityLabel = if (running) stringResource(R.string.bedtime_pause_guided_breathing) else stringResource(R.string.bedtime_start_guided_breathing)
            )
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.stopwatch_reset), color = TextSecondary)
            }
        }
    }
}
