package com.sysadmindoc.alarmclock.ui.stopwatch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.sysadmindoc.alarmclock.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Circular stopwatch display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            // Animated seconds hand
            val secondsFraction = (state.elapsedMillis % 60000) / 60000f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )

                // Outer ring - track
                drawArc(
                    color = SurfaceCard,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc (seconds hand progress)
                if (state.state != StopwatchState.IDLE) {
                    drawArc(
                        color = AccentBlue,
                        startAngle = -90f,
                        sweepAngle = secondsFraction * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Seconds hand dot
                    val angle = Math.toRadians((-90.0 + secondsFraction * 360.0))
                    val dotX = center.x + (radius * cos(angle)).toFloat()
                    val dotY = center.y + (radius * sin(angle)).toFloat()
                    drawCircle(
                        color = AccentBlue,
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                // Tick marks
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
                        color = if (i % 5 == 0) TextSecondary else TextMuted.copy(alpha = 0.3f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // Time display
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

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state.state) {
                StopwatchState.IDLE -> {
                    Button(
                        onClick = viewModel::start,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                StopwatchState.RUNNING -> {
                    // Lap
                    OutlinedButton(
                        onClick = viewModel::lap,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Lap")
                    }

                    // Pause
                    Button(
                        onClick = viewModel::pause,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                StopwatchState.PAUSED -> {
                    // Reset
                    OutlinedButton(
                        onClick = viewModel::reset,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }

                    // Resume
                    Button(
                        onClick = viewModel::resume,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lap list
        if (state.laps.isNotEmpty()) {
            HorizontalDivider(color = SurfaceCard, thickness = 1.dp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(state.laps) { lap ->
                    LapRow(lap)
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Lap ${lap.number}",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.width(64.dp)
        )

        Text(
            text = formatMillis(lap.splitMillis),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatMillis(lap.totalMillis),
            color = TextMuted,
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
