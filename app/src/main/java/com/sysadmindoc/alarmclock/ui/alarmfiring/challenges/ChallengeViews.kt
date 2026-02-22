package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Math challenge - show expression, tap correct answer from 4 choices.
 */
@Composable
fun MathChallengeView(
    challenge: Challenge.MathChallenge,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    var wrongFlash by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "Solve to dismiss",
            color = TextSecondary,
            fontSize = 14.sp,
            letterSpacing = 2.sp
        )

        // Expression
        Text(
            text = challenge.expression,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = if (wrongFlash) AccentRed else TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Answer grid (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in challenge.choices.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { choice ->
                        Button(
                            onClick = {
                                if (choice == challenge.answer) {
                                    onCorrect()
                                } else {
                                    wrongFlash = true
                                    onWrong()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceCard
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = choice.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Reset wrong flash
    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(400)
            wrongFlash = false
        }
    }
}

/**
 * Shake challenge - shake the phone N times.
 */
@Composable
fun ShakeChallengeView(
    challenge: Challenge.ShakeChallenge,
    currentShakes: Int
) {
    val progress = (currentShakes.toFloat() / challenge.requiredShakes).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeAnim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Shake to dismiss", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Phone icon with shake animation
        Icon(
            Icons.Default.PhoneAndroid,
            contentDescription = "Shake your phone",
            tint = AccentBlue,
            modifier = Modifier
                .size(80.dp)
                .offset(x = shakeOffset.dp)
        )

        // Progress ring
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(120.dp),
                color = AccentBlue,
                trackColor = SurfaceCard,
                strokeWidth = 8.dp
            )
            Text(
                text = "$currentShakes / ${challenge.requiredShakes}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Text(
            text = "Keep shaking!",
            color = if (currentShakes > 0) AccentBlue else TextMuted,
            fontSize = 16.sp
        )
    }
}

/**
 * Sequence challenge - tap numbers in ascending order.
 */
@Composable
fun SequenceChallengeView(
    challenge: Challenge.SequenceChallenge,
    tappedIndices: Set<Int>,
    onTapNumber: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Tap in ascending order", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Text(
            text = "${tappedIndices.size} / ${challenge.numbers.size}",
            color = AccentBlue,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Number grid (2x3)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in challenge.numbers.withIndex().toList().chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (index, number) ->
                        val isTapped = index in tappedIndices
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isTapped) DismissGreen.copy(alpha = 0.3f)
                                    else SurfaceCard
                                )
                                .clickable(enabled = !isTapped) { onTapNumber(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = number.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTapped) DismissGreen else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Memory pattern challenge - memorize lit tiles, then tap them back.
 */
@Composable
fun MemoryPatternChallengeView(
    challenge: Challenge.MemoryPatternChallenge,
    phase: MemoryPhase,
    tappedIndices: Set<Int>,
    onTapTile: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = when (phase) {
                MemoryPhase.SHOWING -> "Memorize the pattern"
                MemoryPhase.INPUT -> "Tap the tiles you saw"
                MemoryPhase.WRONG -> "Wrong! Try again..."
            },
            color = when (phase) {
                MemoryPhase.SHOWING -> SnoozeYellow
                MemoryPhase.INPUT -> TextSecondary
                MemoryPhase.WRONG -> AccentRed
            },
            fontSize = 14.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3x3 grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until challenge.gridSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until challenge.gridSize) {
                        val index = row * challenge.gridSize + col
                        val isLit = when (phase) {
                            MemoryPhase.SHOWING -> index in challenge.pattern
                            MemoryPhase.INPUT -> index in tappedIndices
                            MemoryPhase.WRONG -> index in challenge.pattern
                        }

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isLit && phase == MemoryPhase.SHOWING -> AccentBlue.copy(alpha = 0.7f)
                                        isLit && phase == MemoryPhase.INPUT -> DismissGreen.copy(alpha = 0.5f)
                                        isLit && phase == MemoryPhase.WRONG -> AccentRed.copy(alpha = 0.5f)
                                        else -> SurfaceCard
                                    }
                                )
                                .clickable(enabled = phase == MemoryPhase.INPUT) {
                                    onTapTile(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
            }
        }

        if (phase == MemoryPhase.INPUT) {
            Text(
                "${tappedIndices.size} / ${challenge.pattern.size}",
                color = AccentBlue,
                fontSize = 16.sp
            )
        }
    }
}

enum class MemoryPhase { SHOWING, INPUT, WRONG }
