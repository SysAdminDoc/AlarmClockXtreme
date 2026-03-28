package com.sysadmindoc.alarmclock.ui.alarmfiring

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.*
import com.sysadmindoc.alarmclock.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun AlarmFiringScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onSnoozeCustom: (Int) -> Unit = { onSnooze() },
    onTakePhoto: () -> Unit = {},
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

    // Swipe detection state
    var swipeHint by remember { mutableStateOf("") }
    var swipeCumulativeDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 200f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .pointerInput(state.canDismiss) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeCumulativeDrag = 0f },
                    onDragEnd = {
                        if (swipeCumulativeDrag > swipeThreshold && state.canDismiss) {
                            onDismiss()
                        } else if (swipeCumulativeDrag < -swipeThreshold) {
                            onSnooze()
                        }
                        swipeHint = ""
                        swipeCumulativeDrag = 0f
                    },
                    onDragCancel = {
                        swipeHint = ""
                        swipeCumulativeDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeCumulativeDrag += dragAmount
                        swipeHint = when {
                            swipeCumulativeDrag > swipeThreshold / 2 && state.canDismiss -> "Release to dismiss"
                            swipeCumulativeDrag < -swipeThreshold / 2 -> "Release to snooze"
                            swipeCumulativeDrag > 50 -> "Swipe right to dismiss"
                            swipeCumulativeDrag < -50 -> "Swipe left to snooze"
                            else -> ""
                        }
                    }
                )
            },
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

        // v1.2.0: Motivational quote
        val quote = state.motivationalQuote
        if (quote.isNotBlank()) {
            Text(
                text = quote,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }

        // v1.2.0: Mission chain progress
        if (state.totalChallenges > 1) {
            Text(
                text = "Challenge ${state.currentChallengeIndex + 1} of ${state.totalChallenges}",
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AlarmOff,
                            contentDescription = "Dismiss alarm",
                            tint = AccentRed.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(80.dp)
                        )
                        if (swipeHint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                swipeHint,
                                color = if (swipeHint.contains("Release")) AccentBlue else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = if (swipeHint.contains("Release")) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
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
                challenge is Challenge.TypingChallenge -> {
                    TypingChallengeView(
                        challenge = challenge,
                        currentInput = state.typingInput,
                        onInputChanged = viewModel::updateTypingInput,
                        onSubmit = viewModel::submitTyping,
                        wrongAttempts = state.wrongAttempts
                    )
                }
                challenge is Challenge.WalkChallenge -> {
                    WalkChallengeView(
                        challenge = challenge,
                        currentSteps = state.currentSteps
                    )
                }
                challenge is Challenge.NfcChallenge -> {
                    NfcScanChallengeView(
                        challenge = challenge,
                        scanStatus = state.nfcScanStatus
                    )
                }
                challenge is Challenge.BarcodeChallenge -> {
                    BarcodeScanChallengeView(
                        challenge = challenge,
                        scanStatus = state.barcodeScanStatus
                    )
                }
                challenge is Challenge.PhotoMatchChallenge -> {
                    PhotoMatchChallengeView(
                        challenge = challenge,
                        photoMatchStatus = state.photoMatchStatus,
                        onTakePhoto = onTakePhoto
                    )
                }
                challenge is Challenge.SquatChallenge -> {
                    com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.SquatChallengeView(
                        challenge = challenge,
                        currentSquats = state.squatCount
                    )
                }
                challenge is Challenge.MazeChallenge -> {
                    MazeChallengeView(
                        challenge = challenge,
                        currentPos = state.mazeCurrentPos,
                        onTapCell = viewModel::tapMazeCell
                    )
                }
                challenge is Challenge.WifiChallenge -> {
                    WifiChallengeView(
                        challenge = challenge,
                        currentSsid = state.wifiCurrentSsid
                    )
                }
            }
        }

        // Swipe hint area
        if (swipeHint.isBlank()) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Snooze, null, tint = SnoozeYellow.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Text(" Swipe left", color = TextMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Swipe right ", color = TextMuted, fontSize = 12.sp)
                    Icon(Icons.Default.AlarmOff, null, tint = AccentRed.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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

            // Snooze button with custom duration picker
            var showSnoozeOptions by remember { mutableStateOf(false) }
            val snoozeMins = state.alarm?.snoozeDurationMinutes ?: 10

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
                Text(
                    text = "SNOOZE ($snoozeMins MIN)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnoozeYellow
                )
            }

            // Custom snooze durations
            TextButton(onClick = { showSnoozeOptions = !showSnoozeOptions }) {
                Text(
                    if (showSnoozeOptions) "Hide options" else "Custom snooze...",
                    color = TextMuted, fontSize = 12.sp
                )
            }
            if (showSnoozeOptions) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 15, 30).forEach { mins ->
                        OutlinedButton(
                            onClick = { onSnoozeCustom(mins) },
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SnoozeYellow),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("${mins}m", fontSize = 13.sp, color = SnoozeYellow)
                        }
                    }
                }
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
