package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                                .height(64.dp)
                                .semantics { contentDescription = "Answer: $choice" },
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

/**
 * F3: Typing challenge — type the displayed phrase exactly.
 */
@Composable
fun TypingChallengeView(
    challenge: Challenge.TypingChallenge,
    currentInput: String,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    wrongAttempts: Int
) {
    var wrongFlash by remember { mutableStateOf(false) }

    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts > 0) {
            wrongFlash = true
            delay(400)
            wrongFlash = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Type to dismiss", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (wrongFlash) AccentRed.copy(alpha = 0.15f) else SurfaceCard
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = challenge.phrase,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        OutlinedTextField(
            value = currentInput,
            onValueChange = onInputChanged,
            placeholder = { Text("Type the phrase above…", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = TextMuted,
                cursorColor = AccentBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        Button(
            onClick = onSubmit,
            enabled = currentInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * F4: Walk-steps challenge — walk N steps using the step counter sensor.
 */
@Composable
fun WalkChallengeView(
    challenge: Challenge.WalkChallenge,
    currentSteps: Int
) {
    val progress = (currentSteps.toFloat() / challenge.requiredSteps).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Walk to dismiss", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Icon(
            Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = "Walk steps",
            tint = DismissGreen,
            modifier = Modifier.size(72.dp)
        )

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(120.dp),
                color = DismissGreen,
                trackColor = SurfaceCard,
                strokeWidth = 8.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentSteps",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "/ ${challenge.requiredSteps}",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }
        }

        Text(
            text = if (currentSteps == 0) "Start walking!" else "${challenge.requiredSteps - currentSteps} steps remaining",
            color = if (currentSteps > 0) DismissGreen else TextMuted,
            fontSize = 16.sp
        )
    }
}

/**
 * F2: NFC scan challenge — tap the pre-registered NFC tag.
 */
@Composable
fun NfcScanChallengeView(
    challenge: Challenge.NfcChallenge,
    scanStatus: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "nfcAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Scan NFC tag to dismiss", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Icon(
            Icons.Default.Nfc,
            contentDescription = "NFC scan",
            tint = AccentBlue.copy(alpha = pulseAlpha),
            modifier = Modifier.size(96.dp)
        )

        Text(
            text = "Tap your registered NFC tag to the back of your phone",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        if (scanStatus.isNotBlank()) {
            Text(text = scanStatus, color = AccentRed, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        if (challenge.registeredTagId.isBlank()) {
            Text(
                "No tag registered — tap any NFC tag to dismiss",
                color = SnoozeYellow,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * F1: Barcode/QR scan challenge — scan the pre-registered barcode.
 */
@Composable
fun BarcodeScanChallengeView(
    challenge: Challenge.BarcodeChallenge,
    scanStatus: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "barcodeScan")
    val lineY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scanLine"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Scan barcode to dismiss", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = "Barcode scan",
            tint = AccentBlue,
            modifier = Modifier.size(96.dp)
        )

        Text(
            text = "Point the camera at your registered barcode or QR code",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        if (scanStatus.isNotBlank()) {
            Text(text = scanStatus, color = AccentRed, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        if (challenge.registeredValue.isBlank()) {
            Text(
                "No barcode registered — scan any code to dismiss",
                color = SnoozeYellow,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * F16: Photo match challenge — photograph the registered location to dismiss.
 */
@Composable
fun PhotoMatchChallengeView(
    challenge: Challenge.PhotoMatchChallenge,
    photoMatchStatus: String,
    onTakePhoto: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Photo match to dismiss", color = TextSecondary, fontSize = 14.sp, letterSpacing = 2.sp)

        Icon(
            Icons.Default.PhotoCamera,
            contentDescription = "Take photo",
            tint = AccentBlue,
            modifier = Modifier.size(80.dp)
        )

        Text(
            text = "Photograph the registered location to dismiss the alarm",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        if (photoMatchStatus.isNotBlank()) {
            Text(text = photoMatchStatus, color = AccentRed, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        if (challenge.referencePhotoUri.isBlank()) {
            Text(
                "No reference photo registered — take any photo to dismiss",
                color = SnoozeYellow,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Take Photo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
