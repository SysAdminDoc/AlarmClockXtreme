@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.JetLagDayPlan
import com.sysadmindoc.alarmclock.domain.JetLagDirection
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.BlueLight
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun JetLagPlannerSection(
    state: BedtimeUiState,
    onTargetWakeClick: () -> Unit,
    onDaysChange: (Int) -> Unit,
    onDirectionChange: (JetLagDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val plan = state.jetLagPlan
    AppSurfaceCard(
        modifier = modifier,
        highlighted = !plan.alreadyAligned
    ) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_jetlag_title),
            description = stringResource(
                if (plan.alreadyAligned) {
                    R.string.bedtime_jetlag_aligned_description
                } else {
                    R.string.bedtime_jetlag_shift_description
                }
            )
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = stringResource(
                    R.string.bedtime_jetlag_current_chip,
                    formatClockMinute(plan.currentWakeMinutes, state.is24HourFormat)
                ),
                icon = Icons.Default.Schedule,
                color = TextMuted
            )
            AppStatusChip(
                label = stringResource(
                    R.string.bedtime_jetlag_target_chip,
                    formatClockMinute(plan.targetWakeMinutes, state.is24HourFormat)
                ),
                icon = Icons.Default.WbSunny,
                color = MaterialTheme.colorScheme.primary
            )
            AppStatusChip(
                label = pluralStringResource(
                    R.plurals.bedtime_jetlag_days,
                    plan.adjustmentDays,
                    plan.adjustmentDays
                ),
                icon = Icons.Default.CheckCircle,
                color = if (plan.alreadyAligned) TextMuted else DismissGreen
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                BedtimeMetricCard(
                    title = stringResource(R.string.bedtime_jetlag_current_title),
                    value = formatClockMinute(plan.currentWakeMinutes, state.is24HourFormat),
                    icon = Icons.Default.Schedule,
                    accent = TextSecondary,
                    modifier = Modifier.width(144.dp),
                    helper = stringResource(
                        if (state.wakeTimeFormatted.isNotBlank()) {
                            R.string.bedtime_jetlag_helper_next_alarm
                        } else {
                            R.string.bedtime_jetlag_helper_sleep_target
                        }
                    )
                )
            }
            item {
                BedtimeMetricCard(
                    title = stringResource(R.string.bedtime_jetlag_target_title),
                    value = formatClockMinute(plan.targetWakeMinutes, state.is24HourFormat),
                    icon = Icons.Default.WbSunny,
                    accent = SnoozeYellow,
                    modifier = Modifier.width(144.dp),
                    helper = stringResource(R.string.bedtime_jetlag_helper_tap_to_edit),
                    onClick = onTargetWakeClick
                )
            }
            item {
                BedtimeMetricCard(
                    title = stringResource(R.string.bedtime_jetlag_daily_move_title),
                    value = formatJetLagShift(plan.dailyShiftMinutes),
                    icon = Icons.Default.Lightbulb,
                    accent = DismissGreen,
                    modifier = Modifier.width(168.dp),
                    helper = stringResource(plan.resolvedDirection.labelRes)
                )
            }
        }

        Text(
            text = stringResource(R.string.bedtime_jetlag_direction),
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JetLagDirection.entries.forEach { direction ->
                AppFilterChip(
                    label = stringResource(direction.labelRes),
                    selected = state.jetLagDirection == direction,
                    onClick = { onDirectionChange(direction) },
                    selectionSemantics = true
                )
            }
        }

        Text(
            text = stringResource(R.string.bedtime_jetlag_adjustment_days),
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Backup restore can persist any 1..14 value; surface non-preset
            // values as an extra selected chip so the state is always visible.
            val presetDays = listOf(2, 3, 4, 5, 7, 10)
            val dayOptions = if (plan.adjustmentDays in presetDays) {
                presetDays
            } else {
                (presetDays + plan.adjustmentDays).sorted()
            }
            dayOptions.forEach { days ->
                AppFilterChip(
                    label = pluralStringResource(R.plurals.bedtime_jetlag_days, days, days),
                    selected = plan.adjustmentDays == days,
                    onClick = { onDaysChange(days) },
                    selectionSemantics = true
                )
            }
        }

        HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            plan.days.forEach { day ->
                JetLagDayRow(day = day, is24Hour = state.is24HourFormat)
            }
        }
    }
}

@Composable
private fun JetLagDayRow(
    day: JetLagDayPlan,
    is24Hour: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bedtime_jetlag_day_number, day.dayNumber),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.bedtime_jetlag_wake_chip,
                        formatClockMinute(day.wakeMinutes, is24Hour)
                    ),
                    color = SnoozeYellow,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = stringResource(
                        R.string.bedtime_jetlag_bed_chip,
                        formatClockMinute(day.bedtimeMinutes, is24Hour)
                    ),
                    icon = Icons.Default.NightsStay,
                    color = BlueLight
                )
                AppStatusChip(
                    label = stringResource(
                        R.string.bedtime_jetlag_bright_chip,
                        formatTimeRange(day.brightLightStartMinutes, day.brightLightEndMinutes, is24Hour)
                    ),
                    icon = Icons.Default.WbSunny,
                    color = SnoozeYellow
                )
                AppStatusChip(
                    label = stringResource(
                        R.string.bedtime_jetlag_dim_chip,
                        formatTimeRange(day.dimLightStartMinutes, day.dimLightEndMinutes, is24Hour)
                    ),
                    icon = Icons.Default.Lightbulb,
                    color = TextMuted
                )
            }
        }
    }
}

private val JetLagDirection.labelRes: Int
    get() = when (this) {
        JetLagDirection.AUTO -> R.string.bedtime_jetlag_direction_auto
        JetLagDirection.ADVANCE -> R.string.bedtime_jetlag_direction_earlier
        JetLagDirection.DELAY -> R.string.bedtime_jetlag_direction_later
    }

@Composable
private fun formatJetLagShift(minutes: Int): String {
    if (minutes == 0) return stringResource(R.string.bedtime_jetlag_no_shift)
    val absolute = abs(minutes)
    val hours = absolute / 60
    val mins = absolute % 60
    val duration = when {
        hours > 0 && mins > 0 -> stringResource(R.string.bedtime_jetlag_duration_hours_minutes, hours, mins)
        hours > 0 -> stringResource(R.string.bedtime_jetlag_duration_hours, hours)
        else -> stringResource(R.string.bedtime_jetlag_duration_minutes, mins)
    }
    return stringResource(
        if (minutes < 0) R.string.bedtime_jetlag_shift_earlier else R.string.bedtime_jetlag_shift_later,
        duration
    )
}

@Composable
private fun formatTimeRange(startMinutes: Int, endMinutes: Int, is24Hour: Boolean): String =
    stringResource(
        R.string.bedtime_jetlag_time_range,
        formatClockMinute(startMinutes, is24Hour),
        formatClockMinute(endMinutes, is24Hour)
    )

private fun formatClockMinute(minutes: Int, is24Hour: Boolean): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return LocalTime.of(normalized / 60, normalized % 60)
        .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
