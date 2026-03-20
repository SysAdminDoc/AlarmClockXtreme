package com.sysadmindoc.alarmclock.ui.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
        // Active timers section
        if (state.activeTimers.isNotEmpty()) {
            ActiveTimersList(
                timers = state.activeTimers,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
                onDismiss = viewModel::dismissFinished,
                modifier = Modifier.weight(
                    if (state.activeTimers.size > 1) 0.5f else 0.4f
                )
            )
            HorizontalDivider(color = SurfaceCard, thickness = 1.dp)
        }

        // Input area (always visible to start more timers)
        TimerInputView(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.weight(if (state.activeTimers.isEmpty()) 1f else 0.6f)
        )
    }
}

@Composable
private fun ActiveTimersList(
    timers: List<TimerInstance>,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onStop: (Int) -> Unit,
    onDismiss: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(timers, key = { it.id }) { timer ->
            ActiveTimerCard(
                timer = timer,
                onPause = { onPause(timer.id) },
                onResume = { onResume(timer.id) },
                onStop = { onStop(timer.id) },
                onDismiss = { onDismiss(timer.id) }
            )
        }
    }
}

@Composable
private fun ActiveTimerCard(
    timer: TimerInstance,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    val isFinished = timer.state == TimerState.FINISHED

    // Pulse for finished timers
    val pulseAlpha = if (isFinished) {
        val infiniteTransition = rememberInfiniteTransition(label = "timerPulse${timer.id}")
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse${timer.id}"
        ).value
    } else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished) AccentRed.copy(alpha = 0.15f) else SurfaceMedium
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress ring (mini)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = timer.progress,
                    animationSpec = tween(100),
                    label = "progress${timer.id}"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )
                    drawArc(
                        color = SurfaceCard,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    if (!isFinished) {
                        drawArc(
                            color = AccentBlue,
                            startAngle = -90f, sweepAngle = animatedProgress * 360f, useCenter = false,
                            topLeft = topLeft, size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                if (isFinished) {
                    Icon(Icons.Default.TimerOff, null, tint = AccentRed.copy(alpha = pulseAlpha), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Timer info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    timer.label.ifBlank { "Timer" },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = if (isFinished) "TIME'S UP" else String.format(
                        "%02d:%02d:%02d",
                        timer.displayHours, timer.displayMinutes, timer.displaySeconds
                    ),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = if (isFinished) AccentRed.copy(alpha = pulseAlpha) else TextPrimary
                )
                if (timer.state == TimerState.PAUSED) {
                    Text("PAUSED", color = SnoozeYellow, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }

            // Controls
            if (isFinished) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("DISMISS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Stop
                IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Stop, "Stop", tint = AccentRed, modifier = Modifier.size(20.dp))
                }
                // Pause/Resume
                IconButton(
                    onClick = { if (timer.state == TimerState.RUNNING) onPause() else onResume() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (timer.state == TimerState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timer.state == TimerState.RUNNING) "Pause" else "Resume",
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerInputView(state: TimerUiState, viewModel: TimerViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(if (state.activeTimers.isEmpty()) 24.dp else 12.dp))

        if (state.activeTimers.isNotEmpty()) {
            Text("Add another timer", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Time input display
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(vertical = if (state.activeTimers.isEmpty()) 16.dp else 8.dp)
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
