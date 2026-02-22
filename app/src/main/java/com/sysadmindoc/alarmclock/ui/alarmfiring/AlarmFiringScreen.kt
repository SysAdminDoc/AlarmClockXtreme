package com.sysadmindoc.alarmclock.ui.alarmfiring

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.*
import com.sysadmindoc.alarmclock.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AlarmFiringScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    viewModel: AlarmFiringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "alarmPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Memory pattern auto-advance after show duration
    val challenge = state.challenge
    if (challenge is Challenge.MemoryPatternChallenge && state.memoryPhase == MemoryPhase.SHOWING) {
        LaunchedEffect(state.memoryPhase, state.wrongAttempts) {
            delay(challenge.showDurationMs)
            viewModel.onMemoryShowComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Current time
        val now = LocalTime.now()
        val hour12 = if (now.hour % 12 == 0) 12 else now.hour % 12
        val amPm = if (now.hour < 12) "AM" else "PM"

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$hour12:${String.format("%02d", now.minute)}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary.copy(alpha = pulseAlpha)
                )
                Text(
                    text = amPm,
                    fontSize = 24.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                )
            }

            val label = state.alarm?.label ?: ""
            Text(
                text = if (label.isNotBlank()) label else "ALARM",
                fontSize = 16.sp,
                letterSpacing = 4.sp,
                color = AccentBlue
            )
        }

        // Challenge area or standard dismiss
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.challengeSolved || challenge == null -> {
                    // Standard dismiss view (no challenge or challenge solved)
                    Icon(
                        Icons.Default.AlarmOff,
                        contentDescription = "Dismiss alarm",
                        tint = AccentRed.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(80.dp)
                    )
                }
                challenge is Challenge.MathChallenge -> {
                    MathChallengeView(
                        challenge = challenge,
                        onCorrect = { viewModel.submitMathAnswer(true) },
                        onWrong = { viewModel.submitMathAnswer(false) }
                    )
                }
                challenge is Challenge.ShakeChallenge -> {
                    ShakeChallengeView(
                        challenge = challenge,
                        currentShakes = state.shakeCount
                    )
                }
                challenge is Challenge.SequenceChallenge -> {
                    SequenceChallengeView(
                        challenge = challenge,
                        tappedIndices = state.sequenceTappedIndices,
                        onTapNumber = viewModel::tapSequenceNumber
                    )
                }
                challenge is Challenge.MemoryPatternChallenge -> {
                    MemoryPatternChallengeView(
                        challenge = challenge,
                        phase = state.memoryPhase,
                        tappedIndices = state.memoryTappedIndices,
                        onTapTile = viewModel::tapMemoryTile
                    )
                }
            }
        }

        // Action buttons
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dismiss button
            Button(
                onClick = onDismiss,
                enabled = state.canDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    disabledContainerColor = AccentRed.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (state.canDismiss) "DISMISS" else "SOLVE TO DISMISS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.canDismiss) TextPrimary else TextMuted
                )
            }

            // Snooze button (always available)
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    width = 2.dp
                ),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SnoozeYellow)
            ) {
                val snoozeMins = state.alarm?.snoozeDurationMinutes ?: 10
                Text(
                    text = "SNOOZE ($snoozeMins MIN)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnoozeYellow
                )
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
