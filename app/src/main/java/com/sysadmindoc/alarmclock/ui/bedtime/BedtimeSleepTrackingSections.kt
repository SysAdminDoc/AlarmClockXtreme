package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.data.health.HealthConnectAvailability
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

@Composable
internal fun PreSleepTagSection(
    state: BedtimeUiState,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = state.preSleepTags.any { it.selected }
    AppSurfaceCard(
        modifier = modifier,
        highlighted = hasSelection
    ) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_tracking_pre_sleep_factors),
            description = stringResource(R.string.bedtime_tag_the_signals_that_may_shape, state.preSleepTagDateLabel)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.preSleepTags.forEach { tag ->
                AppFilterChip(
                    label = tag.label,
                    selected = tag.selected,
                    onClick = { onToggle(tag.key) },
                    leadingIcon = if (tag.selected) Icons.Default.CheckCircle else Icons.Default.Add,
                    accessibilityLabel = "${tag.label}: ${tag.helper}"
                )
            }
        }

        if (state.preSleepCorrelations.isNotEmpty()) {
            HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
            PreSleepCorrelationChart(items = state.preSleepCorrelations)
        } else {
            Text(
                text = stringResource(R.string.bedtime_tracking_local_chart_appears_after_tagged),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PreSleepCorrelationChart(items: List<PreSleepCorrelationItem>) {
    val maxDelta = items.mapNotNull { it.deltaMinutes?.let(::abs) }.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegend("More restless", SnoozeYellow)
            ChartLegend("Calmer", DismissGreen)
        }
        items.forEach { item ->
            val delta = item.deltaMinutes
            val color = when {
                delta == null -> TextMuted
                delta > 0 -> SnoozeYellow
                delta < 0 -> DismissGreen
                else -> TextMuted
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.averageRestlessMinutes?.let { "${it}m avg" } ?: stringResource(R.string.bedtime_no_sleep_data),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(SurfaceCard.copy(alpha = 0.54f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val ratio = delta?.let { (abs(it).toFloat() / maxDelta).coerceIn(0.08f, 1f) } ?: 0f
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(8.dp)
                                .background(color, RoundedCornerShape(8.dp))
                        )
                    }
                }
                Text(
                    text = "${item.deltaLabel} - ${item.nightsLabel}",
                    color = color.copy(alpha = if (delta == null) 0.78f else 1f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(6.dp))
        )
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun SonarSleepTrackingSection(
    state: BedtimeUiState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = state.sonarTrackingActive
    ) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_tracking_sonar_sleep_tracking),
            description = if (state.sonarTrackingActive) {
                "Experimental overnight movement monitoring is running locally."
            } else {
                "Start a local ultrasonic movement session from Bedtime when you want extra context."
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = if (state.sonarTrackingActive) stringResource(R.string.settings_active) else stringResource(R.string.alarm_edit_off),
                        icon = if (state.sonarTrackingActive) Icons.Default.CheckCircle else Icons.Default.GraphicEq,
                        color = if (state.sonarTrackingActive) DismissGreen else TextMuted
                    )
                    AppStatusChip(
                        label = stringResource(R.string.bedtime_tracking_no_audio_saved),
                        icon = Icons.Default.NightsStay,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = state.sonarTrackingStatus,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.sonarLastSessionLabel.isNotBlank()) {
                    Text(
                        text = state.sonarLastSessionLabel,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.snoreTimeline.isNotEmpty()) {
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.snoreTimeline.forEach { event ->
                            SnoreTimelineRow(event = event)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            AppFilterChip(
                label = if (state.sonarTrackingActive) stringResource(R.string.notif_timer_stop_action) else stringResource(R.string.challenge_start),
                selected = state.sonarTrackingActive,
                onClick = onToggle,
                selectionSemantics = false,
                accessibilityLabel = if (state.sonarTrackingActive) {
                    "Stop sleep-motion tracking"
                } else {
                    "Start sleep-motion tracking"
                }
            )
        }
    }
}

@Composable
private fun SnoreTimelineRow(event: SnoreTimelineItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = SnoozeYellow.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = SnoozeYellow,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${event.timeLabel} - ${event.durationLabel}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = event.intensityLabel,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun HealthConnectSleepSection(
    summary: HealthConnectSleepSummary,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = summary.permissionGranted && summary.hasRecentSession
    ) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_tracking_health_connect_sleep),
            description = when {
                summary.availability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
                    stringResource(R.string.bedtime_update_health_connect_before_recent_sleep)
                summary.availability == HealthConnectAvailability.UNAVAILABLE ->
                    stringResource(R.string.settings_health_unavailable_description)
                !summary.permissionGranted ->
                    stringResource(R.string.bedtime_grant_read_sleep_in_settings_to)
                summary.hasRecentSession ->
                    stringResource(R.string.bedtime_recent_sessions_stay_local_and_help)
                else ->
                    stringResource(R.string.bedtime_read_sleep_is_granted_but_no)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppStatusChip(
                label = if (summary.permissionGranted) stringResource(R.string.bedtime_read_sleep_granted) else stringResource(R.string.settings_health_permission_needed),
                icon = if (summary.permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                color = if (summary.permissionGranted) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = stringResource(R.string.bedtime_sessions, summary.sessionsRead),
                icon = Icons.Default.Bedtime,
                color = if (summary.sessionsRead > 0) MaterialTheme.colorScheme.primary else TextMuted
            )
        }
        if (summary.hasRecentSession) {
            Text(
                text = stringResource(
                    R.string.bedtime_last_session,
                    formatSleepMinutes(summary.lastSessionDurationMinutes)
                ),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleepStageChip("Light", summary.lightStageMinutes)
                SleepStageChip("Deep", summary.deepStageMinutes)
                SleepStageChip("REM", summary.remStageMinutes)
                SleepStageChip("Awake", summary.awakeStageMinutes)
            }
        }
        summary.errorMessage?.let { error ->
            AppInlineNotice(
                title = stringResource(R.string.settings_health_attention),
                message = error,
                icon = Icons.Default.Warning,
                color = SnoozeYellow
            )
        }
    }
}

@Composable
private fun SleepStageChip(label: String, minutes: Long) {
    AppStatusChip(
        label = "$label ${formatSleepMinutes(minutes)}",
        icon = Icons.Default.NightsStay,
        color = if (minutes > 0) MaterialTheme.colorScheme.primary else TextMuted
    )
}

private fun formatSleepMinutes(minutes: Long?): String {
    val value = minutes ?: return "0m"
    val hours = value / 60
    val mins = value % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
