package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun MathChallengeView(
    challenge: Challenge.MathChallenge,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    var wrongFlash by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Choose the correct answer to unlock dismiss.")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (wrongFlash) AccentRed.copy(alpha = 0.14f) else SurfaceCard
            )
        ) {
            Text(
                text = challenge.expression,
                color = if (wrongFlash) AccentRed else TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            challenge.choices.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { choice ->
                        OutlinedButton(
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
                                .height(68.dp)
                                .semantics { contentDescription = "Answer: $choice" },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard.copy(alpha = 0.82f),
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(
                                text = choice.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (wrongFlash) {
            ChallengeNotice(
                text = "That one was off. Try the next option.",
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }
    }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash) {
            delay(400)
            wrongFlash = false
        }
    }
}

@Composable
fun ShakeChallengeView(
    challenge: Challenge.ShakeChallenge,
    currentShakes: Int
) {
    val progress = (currentShakes.toFloat() / challenge.requiredShakes).coerceIn(0f, 1f)
    val remaining = (challenge.requiredShakes - currentShakes).coerceAtLeast(0)

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
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("A strong shake helps break sleepy autopilot before the alarm unlocks.")

        ChallengeProgressHero(
            icon = Icons.Default.PhoneAndroid,
            accent = AccentBlue,
            progress = progress,
            statusLabel = "$currentShakes / ${challenge.requiredShakes} complete",
            summary = if (currentShakes == 0) "Start shaking to build momentum." else "$remaining shakes remaining."
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = "Shake your phone",
                tint = AccentBlue,
                modifier = Modifier
                    .size(42.dp)
                    .offset(x = shakeOffset.dp)
            )
        }
    }
}

@Composable
fun SequenceChallengeView(
    challenge: Challenge.SequenceChallenge,
    tappedIndices: Set<Int>,
    onTapNumber: (Int) -> Unit
) {
    val nextExpected = challenge.correctOrder.getOrNull(tappedIndices.size)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(
            if (nextExpected != null) {
                "Tap the numbers from lowest to highest. Next target: $nextExpected."
            } else {
                "Sequence complete."
            }
        )

        AppStatusChip(
            label = "${tappedIndices.size} of ${challenge.numbers.size} tapped",
            color = AccentBlue
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            challenge.numbers.withIndex().toList().chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (index, number) ->
                        val isTapped = index in tappedIndices
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(78.dp)
                                .clickable(enabled = !isTapped) { onTapNumber(index) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTapped) DismissGreen.copy(alpha = 0.22f) else SurfaceCard
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
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
}

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        AppStatusChip(
            label = when (phase) {
                MemoryPhase.SHOWING -> "Watch"
                MemoryPhase.INPUT -> "Repeat"
                MemoryPhase.WRONG -> "Retry"
            },
            color = when (phase) {
                MemoryPhase.SHOWING -> SnoozeYellow
                MemoryPhase.INPUT -> AccentBlue
                MemoryPhase.WRONG -> AccentRed
            }
        )

        ChallengeSupportText(
            text = when (phase) {
                MemoryPhase.SHOWING -> "Memorize the highlighted tiles before the pattern disappears."
                MemoryPhase.INPUT -> "Tap the same tiles in the same order."
                MemoryPhase.WRONG -> "The pattern will show again in a moment."
            },
            accent = when (phase) {
                MemoryPhase.WRONG -> AccentRed
                else -> TextSecondary
            }
        )

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
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isLit && phase == MemoryPhase.SHOWING -> AccentBlue.copy(alpha = 0.72f)
                                        isLit && phase == MemoryPhase.INPUT -> DismissGreen.copy(alpha = 0.48f)
                                        isLit && phase == MemoryPhase.WRONG -> AccentRed.copy(alpha = 0.48f)
                                        else -> SurfaceCard
                                    }
                                )
                                .clickable(enabled = phase == MemoryPhase.INPUT) { onTapTile(index) }
                        )
                    }
                }
            }
        }

        if (phase == MemoryPhase.INPUT) {
            AppStatusChip(
                label = "${tappedIndices.size} of ${challenge.pattern.size} matched",
                color = AccentBlue
            )
        }
    }
}

enum class MemoryPhase { SHOWING, INPUT, WRONG }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Type the same words and punctuation. Letter case does not matter.")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (wrongFlash) AccentRed.copy(alpha = 0.15f) else SurfaceCard
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = challenge.phrase,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(18.dp)
            )
        }

        OutlinedTextField(
            value = currentInput,
            onValueChange = onInputChanged,
            placeholder = { Text("Type the phrase above…", color = TextMuted) },
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        if (wrongAttempts > 0) {
            ChallengeNotice(
                text = "Not quite. Match the phrase exactly before trying again.",
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }

        Button(
            onClick = onSubmit,
            enabled = currentInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Check phrase", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WalkChallengeView(
    challenge: Challenge.WalkChallenge,
    currentSteps: Int
) {
    val progress = (currentSteps.toFloat() / challenge.requiredSteps).coerceIn(0f, 1f)
    val remaining = (challenge.requiredSteps - currentSteps).coerceAtLeast(0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Walking a few steps helps make sure you are genuinely up.")

        ChallengeProgressHero(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            accent = DismissGreen,
            progress = progress,
            statusLabel = "$currentSteps / ${challenge.requiredSteps} steps",
            summary = if (currentSteps == 0) "Start walking to build progress." else "$remaining steps remaining."
        )
    }
}

@Composable
fun NfcScanChallengeView(
    challenge: Challenge.NfcChallenge,
    scanStatus: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "nfcAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Tap the saved tag against the back of your phone to clear this step.")

        ChallengeIconPanel(accent = AccentBlue.copy(alpha = 0.12f)) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = "NFC scan",
                tint = AccentBlue.copy(alpha = pulseAlpha),
                modifier = Modifier.size(84.dp)
            )
        }

        if (scanStatus.isNotBlank()) {
            ChallengeNotice(
                text = scanStatus,
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }

        if (challenge.registeredTagId.isBlank()) {
            ChallengeNotice(
                text = "No tag is registered yet. Any NFC tag will work for now.",
                accent = SnoozeYellow,
                icon = Icons.Default.Nfc
            )
        }
    }
}

@Composable
fun BarcodeScanChallengeView(
    challenge: Challenge.BarcodeChallenge,
    scanStatus: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "barcodeScan")
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scanLine"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Point the camera at the saved barcode or QR code to unlock dismiss.")

        ChallengeIconPanel(accent = AccentBlue.copy(alpha = 0.12f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Barcode scan",
                    tint = AccentBlue,
                    modifier = Modifier.size(72.dp)
                )
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AccentBlue.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(lineProgress.coerceIn(0.18f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(AccentBlue)
                    )
                }
            }
        }

        if (scanStatus.isNotBlank()) {
            ChallengeNotice(
                text = scanStatus,
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }

        if (challenge.registeredValue.isBlank()) {
            ChallengeNotice(
                text = "No code is registered yet. Any barcode or QR code will work for now.",
                accent = SnoozeYellow,
                icon = Icons.Default.QrCodeScanner
            )
        }
    }
}

@Composable
fun PhotoMatchChallengeView(
    challenge: Challenge.PhotoMatchChallenge,
    photoMatchStatus: String,
    onTakePhoto: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Take a fresh photo from the saved location or angle to prove you made it there.")

        ChallengeIconPanel(accent = AccentBlue.copy(alpha = 0.12f)) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Take photo",
                tint = AccentBlue,
                modifier = Modifier.size(72.dp)
            )
        }

        if (photoMatchStatus.isNotBlank()) {
            ChallengeNotice(
                text = photoMatchStatus,
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }

        if (challenge.referencePhotoUri.isBlank()) {
            ChallengeNotice(
                text = "No reference photo is registered yet. Any photo will work for now.",
                accent = SnoozeYellow,
                icon = Icons.Default.PhotoCamera
            )
        }

        Button(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Open camera", modifier = Modifier.size(20.dp))
            Text(
                text = "Open camera",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun SquatChallengeView(
    challenge: Challenge.SquatChallenge,
    currentSquats: Int
) {
    val progress = (currentSquats.toFloat() / challenge.requiredSquats).coerceIn(0f, 1f)
    val remaining = (challenge.requiredSquats - currentSquats).coerceAtLeast(0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("A short movement burst makes it harder to crawl back into bed.")

        ChallengeProgressHero(
            icon = Icons.Default.FitnessCenter,
            accent = DismissGreen,
            progress = progress,
            statusLabel = "$currentSquats / ${challenge.requiredSquats} squats",
            summary = if (currentSquats == 0) "Start with one clean squat." else "$remaining squats remaining."
        )
    }
}

@Composable
fun MazeChallengeView(
    challenge: Challenge.MazeChallenge,
    currentPos: Int,
    onTapCell: (Int) -> Unit
) {
    val size = challenge.gridSize
    var invalidFlashIdx by remember { mutableStateOf(-1) }

    LaunchedEffect(invalidFlashIdx) {
        if (invalidFlashIdx >= 0) {
            delay(350)
            invalidFlashIdx = -1
        }
    }

    fun isAdjacent(from: Int, to: Int): Boolean {
        val fromRow = from / size
        val fromCol = from % size
        val toRow = to / size
        val toCol = to % size
        return (fromRow == toRow && kotlin.math.abs(fromCol - toCol) == 1) ||
               (fromCol == toCol && kotlin.math.abs(fromRow - toRow) == 1)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Tap adjacent cells only. Reach the exit without hitting the walls.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppStatusChip(label = "You", icon = Icons.Default.Person, color = AccentBlue)
            AppStatusChip(label = "Start", color = DismissGreen)
            AppStatusChip(label = "Exit", color = AccentRed)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until size) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until size) {
                        val idx = row * size + col
                        val isWall = idx in challenge.walls
                        val isStart = idx == challenge.startPos
                        val isEnd = idx == challenge.endPos
                        val isCurrent = idx == currentPos
                        val isInvalidFlash = idx == invalidFlashIdx

                        val bgColor = when {
                            isInvalidFlash -> AccentRed.copy(alpha = 0.38f)
                            isWall -> SurfaceDark
                            isCurrent -> AccentBlue
                            isStart -> DismissGreen.copy(alpha = 0.28f)
                            isEnd -> AccentRed.copy(alpha = 0.42f)
                            else -> SurfaceCard
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable(enabled = !isWall) {
                                    if (!isAdjacent(currentPos, idx)) {
                                        invalidFlashIdx = idx
                                    } else {
                                        onTapCell(idx)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCurrent -> Icon(Icons.Default.Person, contentDescription = "Your position", tint = TextPrimary, modifier = Modifier.size(22.dp))
                                isStart -> Text("S", color = DismissGreen, fontWeight = FontWeight.Bold)
                                isEnd -> Text("E", color = AccentRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (invalidFlashIdx >= 0) {
            ChallengeNotice(
                text = "Only adjacent cells are reachable. Tap a neighbor of your current position.",
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }
    }
}

@Composable
fun WifiChallengeView(
    challenge: Challenge.WifiChallenge,
    currentSsid: String
) {
    val isConnected = currentSsid.isNotBlank() &&
        (currentSsid == challenge.requiredSsid || challenge.requiredSsid.isBlank())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Reconnect to the planned Wi-Fi network before dismiss becomes available.")

        ChallengeIconPanel(
            accent = if (isConnected) DismissGreen.copy(alpha = 0.12f) else AccentBlue.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi",
                tint = if (isConnected) DismissGreen else AccentBlue,
                modifier = Modifier.size(76.dp)
            )
        }

        if (challenge.requiredSsid.isNotBlank()) {
            ChallengeNotice(
                text = "Required network: ${challenge.requiredSsid}",
                accent = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Wifi
            )
        }

        ChallengeNotice(
            text = if (currentSsid.isBlank()) "Not connected to Wi-Fi yet." else "Connected to: $currentSsid",
            accent = if (isConnected) DismissGreen else SnoozeYellow,
            icon = Icons.Default.Wifi
        )

        if (challenge.requiredSsid.isBlank()) {
            ChallengeNotice(
                text = "No network is specified yet. Any Wi-Fi connection will work for now.",
                accent = SnoozeYellow,
                icon = Icons.Default.Wifi
            )
        }
    }
}

@Composable
private fun ChallengeSupportText(
    text: String,
    accent: Color = TextSecondary
) {
    Text(
        text = text,
        color = accent,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
}

@Composable
private fun ChallengeNotice(
    text: String,
    accent: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
private fun ChallengeIconPanel(
    accent: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.22f),
                            accent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun ChallengeProgressHero(
    icon: ImageVector,
    accent: Color,
    progress: Float,
    statusLabel: String,
    summary: String,
    iconContent: @Composable (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(136.dp),
                color = accent,
                trackColor = SurfaceCard,
                strokeWidth = 10.dp
            )
            if (iconContent != null) {
                iconContent()
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = statusLabel,
                    tint = accent,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        AppStatusChip(
            label = statusLabel,
            icon = icon,
            color = accent
        )

        Text(
            text = summary,
            color = if (progress > 0f) accent else TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        if (progress > 0f) {
            Text(
                text = "${(progress * 100).toInt()}% complete",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// v1.4.0: Count-the-Sheep challenge. Sheep and goats drift across the screen.
// Tapping a sheep increments the count; tapping a goat decrements it.
@Composable
fun CountSheepChallengeView(
    challenge: Challenge.CountSheepChallenge,
    tapped: Int,
    wrongTaps: Int,
    onSheepTap: () -> Unit,
    onGoatTap: () -> Unit
) {
    val progress = (tapped.toFloat() / challenge.targetCount).coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "sheep-drift")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheep-drift-value"
    )

    // Stable arrangement per challenge instance: rows of sheep + a few goats interleaved.
    data class Creature(val emoji: String, val row: Int, val phase: Float, val isSheep: Boolean)
    val creatures = remember(challenge.targetCount) {
        val rng = kotlin.random.Random(challenge.targetCount * 131)
        val totalRows = 6
        val perRow = 4
        val out = mutableListOf<Creature>()
        repeat(totalRows) { row ->
            repeat(perRow) { col ->
                val isSheep = rng.nextFloat() < 0.75f
                out.add(
                    Creature(
                        emoji = if (isSheep) "\uD83D\uDC11" else "\uD83D\uDC10",
                        row = row,
                        phase = (col.toFloat() / perRow) + rng.nextFloat() * 0.05f,
                        isSheep = isSheep
                    )
                )
            }
        }
        out
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Tap every sheep. Skip the goats \u2014 they subtract from your count.")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.18f),
                            SurfaceDark
                        )
                    )
                )
        ) {
            val rowHeight = 40.dp
            creatures.forEach { c ->
                // Phase-shifted horizontal drift per creature
                val x = (((drift + c.phase) % 1f) * 1.4f) - 0.2f
                Box(
                    modifier = Modifier
                        .offset(
                            x = (x * 320f).dp,
                            y = (c.row * 40).dp + 8.dp
                        )
                        .size(rowHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { if (c.isSheep) onSheepTap() else onGoatTap() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = c.emoji, fontSize = 28.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            AppStatusChip(
                label = "Sheep $tapped / ${challenge.targetCount}",
                color = if (progress >= 1f) DismissGreen else AccentBlue
            )
            if (wrongTaps > 0) {
                AppStatusChip(
                    label = "Missed $wrongTaps",
                    color = AccentRed
                )
            }
        }
    }
}

// v1.5.0: Simon-says color-sequence challenge.
@Composable
fun SimonSaysChallengeView(
    challenge: Challenge.SimonSaysChallenge,
    playingIndex: Int,          // -1 when idle; otherwise the pad currently lit
    inputIndices: List<Int>,    // Positions the user has tapped so far
    errorFlash: Boolean,
    onPadTap: (Int) -> Unit
) {
    val colors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFF22C55E), // Green
        Color(0xFF3B82F6), // Blue
        Color(0xFFFACC15)  // Yellow
    )
    val names = listOf("Red", "Green", "Blue", "Yellow")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(
            text = "Watch the sequence, then tap it back in order. One wrong tap restarts the round.",
            accent = if (errorFlash) AccentRed else TextSecondary
        )

        // 2x2 color pad grid.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 2) {
                        val idx = row * 2 + col
                        val lit = idx == playingIndex
                        val alpha = if (lit) 1f else 0.45f
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors[idx].copy(alpha = alpha))
                                .clickable(enabled = playingIndex < 0) { onPadTap(idx) }
                                .semantics { contentDescription = "${names[idx]} pad" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (lit) {
                                Text("\u25CF", color = TextPrimary, fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }

        AppStatusChip(
            label = "${inputIndices.size} / ${challenge.sequence.size} correct",
            color = if (inputIndices.size == challenge.sequence.size) DismissGreen else AccentBlue
        )
    }
}

// v1.5.0: Type today's date backwards challenge.
@Composable
fun DateBackwardsChallengeView(
    challenge: Challenge.DateBackwardsChallenge,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(
            "Today is ${challenge.targetDate}. Type it reversed character-by-character."
        )

        Text(
            text = challenge.targetDate,
            color = TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "\u2192  ${challenge.expectedInput}",
            color = TextMuted,
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Type the reversed date", color = TextMuted) },
            singleLine = true,
            colors = appOutlinedTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSubmit,
            enabled = input.length == challenge.expectedInput.length,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Check")
        }
    }
}

// v1.5.0: Stroop-interference color-word test.
@Composable
fun StroopChallengeView(
    challenge: Challenge.StroopChallenge,
    onPick: (Int) -> Unit
) {
    val palette = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFF22C55E), // Green
        Color(0xFF3B82F6), // Blue
        Color(0xFFFACC15)  // Yellow
    )
    val names = listOf("Red", "Green", "Blue", "Yellow")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText("Tap the INK COLOR of the word, not the word itself.")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = challenge.wordLabel,
                color = palette[challenge.inkColorIndex],
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            challenge.choices.forEach { idx ->
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette[idx])
                        .clickable { onPick(idx) }
                        .semantics { contentDescription = "${names[idx]} choice" }
                )
            }
        }
    }
}
