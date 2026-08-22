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
            description = if (running) {
                "Follow the count and keep the phone nearby while you settle down."
            } else {
                "Run a short 4-7-8 or box-breathing reset before sleep."
            }
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
                    text = phase.label,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    // "Inhale" / "Hold" / "Exhale" is the whole exercise; a
                    // screen-reader user needs it read as it changes.
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                )
                Text(
                    text = if (phase.completed) "Done" else "${phase.remainingSeconds}",
                    color = if (phase.completed) DismissGreen else SnoozeYellow,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = phase.cue,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${formatBreathingDuration(remainingSessionSeconds)} left",
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
                label = if (running) "Pause" else if (phase.completed) "Restart" else "Start",
                selected = running,
                onClick = onToggleRunning,
                selectionSemantics = false,
                accessibilityLabel = if (running) "Pause guided breathing" else "Start guided breathing"
            )
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.stopwatch_reset), color = TextSecondary)
            }
        }
    }
}
