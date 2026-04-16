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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        AlarmClockHeroHeader(
            title = "Stopwatch",
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
                    color = if (state.laps.isEmpty()) TextMuted else MaterialTheme.colorScheme.primary
                )
            },
            actions = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close stopwatch", tint = TextMuted)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                AppSectionTitle(
                    title = "Current run",
                    description = "A high-contrast display built for quick glances."
                )

                StopwatchDial(state = state)

                ControlsRow(state = state, viewModel = viewModel)
            }

            if (state.laps.isEmpty()) {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    AppEmptyState(
                        icon = Icons.Default.Flag,
                        title = "No laps recorded yet",
                        description = "Tap Lap while the stopwatch is running to capture split times and compare pace."
                    )
                }
            } else {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    AppSectionTitle(
                        title = "Lap history",
                        description = "Best and slowest splits are highlighted automatically."
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
private fun ControlsRow(state: StopwatchUiState, viewModel: StopwatchViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.state) {
            StopwatchState.IDLE -> {
                Button(
                    onClick = viewModel::start,
                    modifier = Modifier.size(78.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start stopwatch", modifier = Modifier.size(34.dp))
                }
            }

            StopwatchState.RUNNING -> {
                OutlinedButton(
                    onClick = viewModel::lap,
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = "Mark lap")
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = viewModel::pause,
                    modifier = Modifier.size(78.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause stopwatch", modifier = Modifier.size(34.dp))
                }
            }

            StopwatchState.PAUSED -> {
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset stopwatch")
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = viewModel::resume,
                    modifier = Modifier.size(78.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume stopwatch", modifier = Modifier.size(34.dp))
                }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            Text(
                text = "Lap ${lap.number}",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = when {
                    lap.isBest -> "Best"
                    lap.isWorst -> "Slowest"
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

        Text(
            text = formatMillis(lap.splitMillis),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatMillis(lap.totalMillis),
            color = TextSecondary,
            fontSize = 14.sp
        )
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
