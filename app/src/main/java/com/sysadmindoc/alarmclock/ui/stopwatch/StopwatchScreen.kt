package com.sysadmindoc.alarmclock.ui.stopwatch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import com.sysadmindoc.alarmclock.R
import androidx.compose.ui.res.stringResource

@Composable
fun StopwatchScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Reset is immediate, per the project's no-confirmation-dialogs rule, so
    // the safety net is an undo rather than a prompt.
    val onReset: () -> Unit = {
        viewModel.reset()
        if (viewModel.canUndoReset) {
            scope.launch {
                val action = snackbarHostState.showSnackbar(
                    message = "Stopwatch reset",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (action == SnackbarResult.ActionPerformed) {
                    viewModel.undoReset()
                } else {
                    viewModel.clearUndo()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        AlarmClockHeroHeader(
            title = stringResource(R.string.nav_stopwatch),
            subtitle = when (state.state) {
                StopwatchState.IDLE -> "Start a precise running timer and mark laps whenever you need a split."
                StopwatchState.RUNNING -> "Timing live. Mark laps as the session unfolds."
                StopwatchState.PAUSED -> "Paused in place. Resume when you are ready or reset for a clean run."
            },
            overline = "Timing",
            badge = {
                AppStatusChip(
                    label = when (state.state) {
                        StopwatchState.IDLE -> "Ready"
                        StopwatchState.RUNNING -> "Running"
                        StopwatchState.PAUSED -> "Paused"
                    },
                    icon = when (state.state) {
                        StopwatchState.IDLE -> Icons.Default.Speed
                        StopwatchState.RUNNING -> Icons.Default.PlayArrow
                        StopwatchState.PAUSED -> Icons.Default.Pause
                    },
                    color = when (state.state) {
                        StopwatchState.IDLE -> MaterialTheme.colorScheme.primary
                        StopwatchState.RUNNING -> DismissGreen
                        StopwatchState.PAUSED -> SnoozeYellow
                    }
                )
                AppStatusChip(
                    label = "${state.laps.size} laps",
                    icon = Icons.Default.Flag,
                    color = if (state.laps.isEmpty()) TextMuted else MaterialTheme.colorScheme.primary,
                    // Start, pause, resume and reset all change only the chip
                    // above and the numbers, neither of which announced.
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                )
            },
            actions = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.stopwatch_close_stopwatch), tint = TextMuted)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                AppSectionTitle(
                    title = stringResource(R.string.stopwatch_current_run),
                    description = stringResource(R.string.stopwatch_high_contrast_display_built_quick)
                )

                StopwatchDial(state = state)

                ControlsRow(state = state, viewModel = viewModel, onReset = onReset)
            }

            if (state.laps.isEmpty()) {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    AppEmptyState(
                        icon = Icons.Default.Flag,
                        title = stringResource(R.string.stopwatch_no_laps_recorded_yet),
                        description = stringResource(R.string.stopwatch_tap_lap_while_stopwatch_running)
                    )
                }
            } else {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    AppSectionTitle(
                        title = stringResource(R.string.stopwatch_lap_history),
                        description = stringResource(R.string.stopwatch_best_slowest_splits_are_highlighted)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.laps) { lap ->
                            LapRow(lap)
                            if (lap != state.laps.last()) {
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
                            }
                        }
                    }
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun StopwatchDial(state: StopwatchUiState) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            val secondsFraction = (state.elapsedMillis % 60000) / 60000f
            val accent = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )

                drawArc(
                    color = SurfaceCard,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (state.state != StopwatchState.IDLE) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = secondsFraction * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val angle = Math.toRadians((-90.0 + secondsFraction * 360.0))
                    val dotX = center.x + (radius * cos(angle)).toFloat()
                    val dotY = center.y + (radius * sin(angle)).toFloat()
                    drawCircle(
                        color = accent,
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                for (i in 0 until 60) {
                    val tickAngle = Math.toRadians(-90.0 + i * 6.0)
                    val tickLength = if (i % 5 == 0) 12.dp.toPx() else 6.dp.toPx()
                    val outerRadius = radius - strokeWidth
                    val innerRadius = outerRadius - tickLength
                    val startX = center.x + (outerRadius * cos(tickAngle)).toFloat()
                    val startY = center.y + (outerRadius * sin(tickAngle)).toFloat()
                    val endX = center.x + (innerRadius * cos(tickAngle)).toFloat()
                    val endY = center.y + (innerRadius * sin(tickAngle)).toFloat()
                    drawLine(
                        color = if (i % 5 == 0) TextSecondary else TextMuted.copy(alpha = 0.28f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.hours > 0) {
                        String.format("%d:%02d:%02d", state.hours, state.minutes, state.seconds)
                    } else {
                        String.format("%02d:%02d", state.minutes, state.seconds)
                    },
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary
                )
                Text(
                    text = String.format(".%02d", state.centiseconds),
                    fontSize = 24.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ControlsRow(
    state: StopwatchUiState,
    viewModel: StopwatchViewModel,
    onReset: () -> Unit
) {
    when (state.state) {
        StopwatchState.IDLE -> {
            Button(
                onClick = viewModel::start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.stopwatch_start_stopwatch),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stopwatch_start_stopwatch),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        StopwatchState.RUNNING -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StopwatchSecondaryButton(
                    label = stringResource(R.string.stopwatch_lap),
                    icon = Icons.Default.Flag,
                    onClick = viewModel::lap,
                    modifier = Modifier.weight(1f)
                )
                StopwatchPrimaryButton(
                    label = stringResource(R.string.alarm_list_pause),
                    icon = Icons.Default.Pause,
                    onClick = viewModel::pause,
                    modifier = Modifier.weight(1.35f)
                )
            }
        }

        StopwatchState.PAUSED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StopwatchSecondaryButton(
                    label = stringResource(R.string.stopwatch_reset),
                    icon = Icons.Default.Refresh,
                    onClick = onReset,
                    accent = AccentRed,
                    modifier = Modifier.weight(1f)
                )
                StopwatchPrimaryButton(
                    label = stringResource(R.string.stopwatch_resume),
                    icon = Icons.Default.PlayArrow,
                    onClick = viewModel::resume,
                    modifier = Modifier.weight(1.35f)
                )
            }
        }
    }
}

@Composable
private fun LapRow(lap: Lap) {
    val textColor = when {
        lap.isBest -> DismissGreen
        lap.isWorst -> AccentRed
        else -> TextPrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = when {
            lap.isBest -> DismissGreen.copy(alpha = 0.1f)
            lap.isWorst -> AccentRed.copy(alpha = 0.09f)
            else -> SurfaceCard.copy(alpha = 0.72f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.width(84.dp)) {
                Text(
                    text = stringResource(R.string.stopwatch_lap_number, lap.number),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when {
                        lap.isBest -> "Best split"
                        lap.isWorst -> "Slowest split"
                        else -> "Split"
                    },
                    color = when {
                        lap.isBest -> DismissGreen
                        lap.isWorst -> AccentRed
                        else -> TextMuted
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatMillis(lap.splitMillis),
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.stopwatch_split_time),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatMillis(lap.totalMillis),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.stopwatch_total),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StopwatchPrimaryButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StopwatchSecondaryButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = TextSecondary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}

private fun formatMillis(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    val seconds = (millis % 60000) / 1000
    val centis = (millis % 1000) / 10

    return if (hours > 0) {
        String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, centis)
    } else {
        String.format("%02d:%02d.%02d", minutes, seconds, centis)
    }
}
