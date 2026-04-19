package com.sysadmindoc.alarmclock.ui.timer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        AlarmClockHeroHeader(
            title = if (state.activeTimers.isEmpty()) "Timer" else "${state.activeTimers.size} timer${if (state.activeTimers.size == 1) "" else "s"} running",
            subtitle = if (state.activeTimers.isEmpty()) {
                "Build a countdown in seconds and start it without digging through menus."
            } else {
                "Stack multiple countdowns and keep every one of them visible."
            },
            overline = "Countdowns",
            badge = {
                if (state.activeTimers.any { it.state == TimerState.FINISHED }) {
                    AppStatusChip(label = "Attention needed", icon = Icons.Default.TimerOff, color = AccentRed)
                } else if (state.activeTimers.any { it.state == TimerState.PAUSED }) {
                    AppStatusChip(label = "Paused timer", icon = Icons.Default.Pause, color = SnoozeYellow)
                } else {
                    AppStatusChip(label = "Ready to start", icon = Icons.Default.Timer)
                }
            }
        )

        if (state.activeTimers.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppSectionTitle(
                        title = "Active timers",
                        description = "Pause, resume, or dismiss countdowns without losing context."
                    )
                }
                items(state.activeTimers, key = { it.id }) { timer ->
                    ActiveTimerCard(
                        timer = timer,
                        onPause = { viewModel.pause(timer.id) },
                        onResume = { viewModel.resume(timer.id) },
                        onStop = { viewModel.stop(timer.id) },
                        onDismiss = { viewModel.dismissFinished(timer.id) }
                    )
                }
            }
        }

        TimerInputView(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.weight(if (state.activeTimers.isEmpty()) 1f else 0.5f)
        )
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
    val transition = rememberInfiniteTransition(label = "timer-finished")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.52f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer-pulse"
    )

    AppSurfaceCard(highlighted = isFinished) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerProgressRing(timer = timer, pulseAlpha = pulseAlpha)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        timer.label.ifBlank { "Timer" },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isFinished) "Time’s up" else String.format(
                            "%02d:%02d:%02d",
                            timer.displayHours,
                            timer.displayMinutes,
                            timer.displaySeconds
                        ),
                        color = if (isFinished) AccentRed else TextPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (timer.state == TimerState.PAUSED) {
                        AppStatusChip(label = "Paused", icon = Icons.Default.Pause, color = SnoozeYellow)
                    }
                }
            }

            if (isFinished) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Dismiss")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, "Stop timer", tint = AccentRed)
                    }
                    IconButton(onClick = { if (timer.state == TimerState.RUNNING) onPause() else onResume() }) {
                        Icon(
                            if (timer.state == TimerState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (timer.state == TimerState.RUNNING) "Pause timer" else "Resume timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerProgressRing(timer: TimerInstance, pulseAlpha: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = timer.progress,
        animationSpec = tween(100),
        label = "timer-progress"
    )
    val accent = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2
            )
            drawArc(
                color = SurfaceCard,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (timer.state != TimerState.FINISHED) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Icon(
            imageVector = if (timer.state == TimerState.FINISHED) Icons.Default.TimerOff else Icons.Default.Timer,
            contentDescription = null,
            tint = if (timer.state == TimerState.FINISHED) AccentRed.copy(alpha = pulseAlpha) else TextMuted
        )
    }
}

@Composable
private fun TimerInputView(state: TimerUiState, viewModel: TimerViewModel, modifier: Modifier = Modifier) {
    AppSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AppSectionTitle(
            title = "Build a timer",
            description = if (state.activeTimers.isEmpty()) {
                "Enter a duration or choose a preset to get moving quickly."
            } else {
                "Start another countdown without interrupting the ones already running."
            }
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TimeUnit(state.inputHours, "h")
            TimeUnit(state.inputMinutes, "m")
            TimeUnit(state.inputSeconds, "s")
        }

        if (state.inputDigits.isNotBlank()) {
            AppStatusChip(
                label = String.format(
                    "%02d:%02d:%02d selected",
                    state.inputHours,
                    state.inputMinutes,
                    state.inputSeconds
                ),
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.inputDigits.isBlank()) {
                    "Use the keypad below or start from a preset."
                } else {
                    "Tap 00 for quick entries, or clear and start fresh."
                },
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (state.inputDigits.isNotBlank()) {
                TextButton(onClick = viewModel::clearInput) {
                    Text("Clear", color = TextMuted)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultPresets.forEach { preset ->
                AssistChip(
                    onClick = { viewModel.selectPreset(preset) },
                    label = { Text(preset.label, color = TextPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCard),
                    border = null
                )
            }
        }

        NumPad(
            onDigit = viewModel::appendDigit,
            onDoubleZero = viewModel::appendDoubleZero,
            onDelete = viewModel::deleteDigit
        )

        Button(
            onClick = viewModel::start,
            enabled = state.canStart,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(76.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Start timer", modifier = Modifier.size(34.dp))
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
            color = TextPrimary
        )
        Text(
            text = unit,
            fontSize = 18.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp, end = 10.dp)
        )
    }
}

@Composable
private fun NumPad(
    onDigit: (Int) -> Unit,
    onDoubleZero: () -> Unit,
    onDelete: () -> Unit
) {
    val keys = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -2, 0, -1)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    val accent = when (key) {
                        -1 -> AccentRed
                        -2 -> MaterialTheme.colorScheme.primary
                        else -> TextPrimary
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.12f)
                            .clickable(role = Role.Button) {
                                when (key) {
                                    -1 -> onDelete()
                                    -2 -> onDoubleZero()
                                    else -> onDigit(key)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (key < 0) SurfaceCard.copy(alpha = 0.88f) else SurfaceMedium,
                        border = BorderStroke(
                            1.dp,
                            if (key < 0) accent.copy(alpha = 0.22f) else TextMuted.copy(alpha = 0.12f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = if (key < 0) accent.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                -1 -> Icon(Icons.AutoMirrored.Filled.Backspace, "Delete digit", tint = accent)
                                -2 -> Text("00", color = accent, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                                else -> Text(key.toString(), color = TextPrimary, fontSize = 24.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
