@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.sysadmindoc.alarmclock.ui.components.AppTileShape
import androidx.compose.material.icons.Icons
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
import com.sysadmindoc.alarmclock.ui.components.AppTileGrid
import com.sysadmindoc.alarmclock.ui.theme.BlueLight
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceInset
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
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

        // The cards already show the current and target times, and the days
        // selector below shows the length, so no chip row repeats them. Two
        // per row: in a LazyRow the third card sat past the screen edge.
        AppTileGrid(
            tiles = listOf<@Composable (Modifier) -> Unit>(
                { tileModifier ->
                    BedtimeMetricCard(
                        title = stringResource(R.string.bedtime_jetlag_current_title),
                        value = formatClockMinute(plan.currentWakeMinutes, state.is24HourFormat),
                        icon = Icons.Default.Schedule,
                        accent = TextSecondary,
                        modifier = tileModifier,
                        helper = stringResource(
                            if (state.wakeTimeFormatted.isNotBlank()) {
                                R.string.bedtime_jetlag_helper_next_alarm
                            } else {
                                R.string.bedtime_jetlag_helper_sleep_target
                            }
                        )
                    )
                },
                { tileModifier ->
                    BedtimeMetricCard(
                        title = stringResource(R.string.bedtime_jetlag_target_title),
                        value = formatClockMinute(plan.targetWakeMinutes, state.is24HourFormat),
                        icon = Icons.Default.WbSunny,
                        accent = SnoozeYellow,
                        modifier = tileModifier,
                        helper = stringResource(R.string.bedtime_jetlag_helper_tap_to_edit),
                        onClick = onTargetWakeClick
                    )
                },
                { tileModifier ->
                    BedtimeMetricCard(
                        title = stringResource(R.string.bedtime_jetlag_daily_move_title),
                        value = formatJetLagShift(plan.dailyShiftMinutes),
                        icon = Icons.Default.Lightbulb,
                        accent = DismissGreen,
                        modifier = tileModifier,
                        // The value already says earlier or later, so the
                        // helper gives how many days the move repeats.
                        helper = if (plan.alreadyAligned) {
                            null
                        } else {
                            pluralStringResource(R.plurals.bedtime_jetlag_days, plan.adjustmentDays, plan.adjustmentDays)
                        }
                    )
                }
            )
        )

        Text(
            text = stringResource(R.string.bedtime_jetlag_direction),
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JetLagDayRow(
    day: JetLagDayPlan,
    is24Hour: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTileShape,
        color = SurfaceInset
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
            // Wraps instead of scrolling: the light ranges ran past the
            // card edge and the dim chip was off screen with no hint.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
    return AlarmTimeFormatter.format(normalized / 60, normalized % 60, is24Hour)
}
