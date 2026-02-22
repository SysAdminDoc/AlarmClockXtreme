package com.sysadmindoc.alarmclock.ui.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.theme.*

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.state) {
            TimerState.IDLE -> TimerInputView(state, viewModel)
            TimerState.RUNNING, TimerState.PAUSED -> TimerCountdownView(state, viewModel)
            TimerState.FINISHED -> TimerFinishedView(state, viewModel)
        }
    }
}

@Composable
private fun TimerInputView(state: TimerUiState, viewModel: TimerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Time input display
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            TimeUnit(state.inputHours, "h")
            TimeUnit(state.inputMinutes, "m")
            TimeUnit(state.inputSeconds, "s")
        }

        // Presets row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultPresets.take(6).forEach { preset ->
                AssistChip(
                    onClick = { viewModel.selectPreset(preset) },
                    label = {
                        Text(
                            preset.label,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SurfaceCard
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Numpad
        NumPad(
            onDigit = viewModel::appendDigit,
            onDelete = viewModel::deleteDigit,
            onClear = viewModel::clearInput
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start button
        Button(
            onClick = viewModel::start,
            enabled = state.canStart,
            modifier = Modifier
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                disabledContainerColor = AccentBlue.copy(alpha = 0.3f)
            )
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TimerCountdownView(state: TimerUiState, viewModel: TimerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Circular progress ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = state.progress,
                animationSpec = tween(100),
                label = "progress"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )

                // Background ring
                drawArc(
                    color = SurfaceCard,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress ring
                drawArc(
                    color = AccentBlue,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Time display inside ring
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(
                        "%02d:%02d:%02d",
                        state.displayHours,
                        state.displayMinutes,
                        state.displaySeconds
                    ),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary
                )
                if (state.state == TimerState.PAUSED) {
                    Text("PAUSED", color = SnoozeYellow, fontSize = 14.sp, letterSpacing = 2.sp)
                }
            }
        }

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop
            OutlinedButton(
                onClick = viewModel::stop,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }

            // Pause/Resume
            Button(
                onClick = {
                    if (state.state == TimerState.RUNNING) viewModel.pause()
                    else viewModel.resume()
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(
                    if (state.state == TimerState.RUNNING) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (state.state == TimerState.RUNNING) "Pause" else "Resume",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TimerFinishedView(state: TimerUiState, viewModel: TimerViewModel) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.TimerOff,
            contentDescription = "Cancel timer",
            tint = AccentRed.copy(alpha = alpha),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TIME'S UP",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary.copy(alpha = alpha),
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = viewModel::dismissFinished,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("DISMISS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimeUnit(value: Int, unit: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = String.format("%02d", value),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = TextPrimary,
        )
        Text(
            text = unit,
            fontSize = 18.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 10.dp, start = 2.dp, end = 8.dp)
        )
    }
}

@Composable
private fun NumPad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    val keys = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -2, 0, -1)
    // -1 = delete, -2 = clear (00)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(keys) { key ->
            Box(
                modifier = Modifier
                    .aspectRatio(1.6f)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .clickable {
                        when (key) {
                            -1 -> onDelete()
                            -2 -> onClear()
                            else -> onDigit(key)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (key) {
                    -1 -> Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Delete",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    -2 -> Text("00", fontSize = 22.sp, color = TextPrimary)
                    else -> Text(
                        key.toString(),
                        fontSize = 24.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
