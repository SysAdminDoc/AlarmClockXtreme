package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.text.format.DateFormat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.BarcodeScanChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.MathChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.MazeChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.MemoryPatternChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.MemoryPhase
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.NfcScanChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.PhotoMatchChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.SequenceChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ShakeChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.SquatChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.TypingChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.WalkChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.CountSheepChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.DateBackwardsChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.SimonSaysChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.StroopChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.WifiChallengeView
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AlarmFiringScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onSnoozeCustom: (Int) -> Unit = { onSnooze() },
    onTakePhoto: () -> Unit = {},
    viewModel: AlarmFiringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // These two prefs gate optional UI surfaces below; collecting them with the
    // lifecycle keeps the firing screen reactive to a settings toggle made
    // mid-alarm (rare, but possible if user pulls down quick settings).
    val showQuotes by viewModel.showMotivationalQuotes.collectAsStateWithLifecycle()
    val flipToSnoozeEnabled by viewModel.flipToSnoozeEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    val currentDate by produceState(initialValue = LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(60_000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "alarmPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val challenge = state.challenge
    if (challenge is Challenge.MemoryPatternChallenge && state.memoryPhase == MemoryPhase.SHOWING) {
        LaunchedEffect(state.memoryPhase, state.wrongAttempts, state.currentChallengeIndex) {
            delay(challenge.showDurationMs)
            viewModel.onMemoryShowComplete()
        }
    }

    var showSnoozeOptions by remember { mutableStateOf(false) }
    var swipeHint by remember { mutableStateOf("") }
    var swipeCumulativeDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 200f
    val defaultSnoozeMinutes = state.alarm?.snoozeDurationMinutes ?: 10

    val timePattern = if (is24Hour) "HH:mm" else "h:mm"
    val timeText = currentTime.format(DateTimeFormatter.ofPattern(timePattern))
    val amPm = if (is24Hour) "" else currentTime.format(DateTimeFormatter.ofPattern("a"))
    val dateText = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val alarmLabel = state.alarm?.label?.takeIf { it.isNotBlank() } ?: "Alarm ringing"
    val stepLabel = if (state.totalChallenges > 1) {
        "Step ${state.currentChallengeIndex + 1} of ${state.totalChallenges}"
    } else {
        "Single-step dismissal"
    }
    val statusLine = when {
        state.canDismiss -> "Wake-up steps are complete. Dismiss now, or snooze if you need a short buffer."
        challenge == null -> "Swipe right or tap dismiss to stop the alarm."
        else -> challenge.statusDescription()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        SurfaceDark
                    )
                )
            )
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
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            AccentRed.copy(alpha = 0.08f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = alarmLabel,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = statusLine,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    AppStatusChip(
                        label = if (state.canDismiss) "Dismiss ready" else "Dismiss locked",
                        icon = if (state.canDismiss) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        color = if (state.canDismiss) DismissGreen else SnoozeYellow
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeText,
                            color = TextPrimary.copy(alpha = pulseAlpha),
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.scale(pulseScale)
                        )
                        if (amPm.isNotBlank()) {
                            Text(
                                text = amPm,
                                color = TextSecondary,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateText,
                            color = TextSecondary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stepLabel,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = if (state.totalChallenges > 1) stepLabel else "Wake-up check",
                        icon = Icons.Default.TaskAlt,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.wrongAttempts > 0) {
                        AppStatusChip(
                            label = "${state.wrongAttempts} retry${if (state.wrongAttempts == 1) "" else "ies"}",
                            icon = Icons.Default.WarningAmber,
                            color = AccentRed
                        )
                    }
                    AppStatusChip(
                        label = "Default snooze ${state.alarm?.snoozeDurationMinutes ?: 10} min",
                        icon = Icons.Default.Timer,
                        color = SnoozeYellow
                    )
                    // Only advertise flip-to-snooze when the user actually
                    // enabled the global setting — otherwise the chip lies.
                    if (flipToSnoozeEnabled) {
                        AppStatusChip(
                            label = "Flip to snooze",
                            icon = Icons.Default.Snooze,
                            color = TextMuted
                        )
                    }
                }

                if (showQuotes) {
                    state.motivationalQuote
                        .takeIf { it.isNotBlank() }
                        ?.let { quote ->
                            Text(
                                text = quote,
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                }
            }

            AppSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = !state.canDismiss
            ) {
                AppSectionTitle(
                    title = challenge.headline(),
                    description = challenge.supportingText()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state.canDismiss || challenge == null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AlarmOff,
                                    contentDescription = null,
                                    tint = DismissGreen.copy(alpha = pulseAlpha),
                                    modifier = Modifier
                                        .size(92.dp)
                                        .scale(pulseScale)
                                )
                                Text(
                                    text = "Alarm ready to dismiss",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "You’ve cleared every required step. Dismiss now or snooze if you need a short buffer.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
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
                                currentSteps = state.currentSteps,
                                walkStatus = state.walkStatus,
                                fallbackAllowed = state.walkFallbackAllowed,
                                onContinueWithoutSensor = viewModel::continueWalkChallengeWithoutSensor
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
                                scanStatus = state.barcodeScanStatus,
                                onCodeEntered = viewModel::onBarcodeDetected
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
                            SquatChallengeView(
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
                                currentSsid = state.wifiCurrentSsid,
                                wifiStatus = state.wifiStatus,
                                fallbackAllowed = state.wifiFallbackAllowed,
                                onContinueWithoutSsid = viewModel::continueWifiChallengeWithoutSsid
                            )
                        }

                        challenge is Challenge.CountSheepChallenge -> {
                            CountSheepChallengeView(
                                challenge = challenge,
                                tapped = state.sheepTapped,
                                wrongTaps = state.sheepWrongTaps,
                                onSheepTap = viewModel::onSheepTapped,
                                onGoatTap = viewModel::onGoatTapped
                            )
                        }

                        challenge is Challenge.SimonSaysChallenge -> {
                            SimonSaysChallengeView(
                                challenge = challenge,
                                playingIndex = state.simonPlayingIndex,
                                inputIndices = state.simonInputIndices,
                                errorFlash = state.simonErrorFlash,
                                onPadTap = viewModel::onSimonPadTap
                            )
                        }

                        challenge is Challenge.DateBackwardsChallenge -> {
                            DateBackwardsChallengeView(
                                challenge = challenge,
                                input = state.dateBackwardsInput,
                                onInputChange = viewModel::updateDateBackwardsInput,
                                onSubmit = viewModel::submitDateBackwards
                            )
                        }

                        challenge is Challenge.StroopChallenge -> {
                            StroopChallengeView(
                                challenge = challenge,
                                onPick = viewModel::onStroopPick
                            )
                        }
                    }
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))

                Text(
                    text = if (swipeHint.isBlank()) {
                        if (state.canDismiss) {
                            if (flipToSnoozeEnabled) {
                                "Swipe left to snooze or right to dismiss. Flip the phone over for a quick snooze."
                            } else {
                                "Swipe left to snooze or right to dismiss."
                            }
                        } else {
                            "Swipe left to snooze if you need a short reset. Dismiss unlocks once the wake-up task is complete."
                        }
                    } else {
                        swipeHint
                    },
                    color = when {
                        swipeHint.contains("Release") -> MaterialTheme.colorScheme.primary
                        state.canDismiss -> TextSecondary
                        else -> TextMuted
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                AppSectionTitle(
                    title = "Alarm controls",
                    description = if (state.canDismiss) {
                        "Choose the cleanest exit for this alarm now that the wake-up work is done."
                    } else {
                        "Snooze is always available. Dismiss unlocks as soon as the current wake-up step is complete."
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = "Swipe left to snooze",
                        icon = Icons.Default.Snooze,
                        color = SnoozeYellow
                    )
                    AppStatusChip(
                        label = if (state.canDismiss) "Swipe right to dismiss" else "Dismiss unlocks after challenge",
                        icon = if (state.canDismiss) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        color = if (state.canDismiss) DismissGreen else TextMuted
                    )
                }

                Button(
                    onClick = onDismiss,
                    enabled = state.canDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DismissGreen,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (state.canDismiss) "Dismiss alarm" else "Finish wake-up challenge",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        width = 1.5.dp
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SnoozeYellow
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Snooze for $defaultSnoozeMinutes min",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = { showSnoozeOptions = !showSnoozeOptions },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showSnoozeOptions) "Hide quick snooze options" else "Choose a different snooze length",
                        color = TextSecondary
                    )
                }

                if (showSnoozeOptions) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5, 15, 30).forEach { minutes ->
                            QuickSnoozeButton(
                                minutes = minutes,
                                isDefault = minutes == defaultSnoozeMinutes,
                                onClick = { onSnoozeCustom(minutes) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Challenge?.headline(): String = when (this) {
    null -> "Alarm ready"
    is Challenge.MathChallenge -> "Solve the final problem"
    is Challenge.ShakeChallenge -> "Shake yourself fully awake"
    is Challenge.SequenceChallenge -> "Finish the number sequence"
    is Challenge.MemoryPatternChallenge -> "Repeat the pattern"
    is Challenge.TypingChallenge -> "Type the wake-up phrase"
    is Challenge.WalkChallenge -> "Walk it off"
    is Challenge.NfcChallenge -> "Tap the registered NFC tag"
    is Challenge.BarcodeChallenge -> "Scan the registered code"
    is Challenge.PhotoMatchChallenge -> "Match the reference photo"
    is Challenge.SquatChallenge -> "Complete the movement check"
    is Challenge.MazeChallenge -> "Navigate out of the maze"
    is Challenge.WifiChallenge -> "Connect to the right network"
    is Challenge.CountSheepChallenge -> "Count the sheep"
    is Challenge.SimonSaysChallenge -> "Play back the pattern"
    is Challenge.DateBackwardsChallenge -> "Type the date backwards"
    is Challenge.StroopChallenge -> "Tap the ink color"
}

private fun Challenge?.supportingText(): String = when (this) {
    null -> "No extra challenge is required for this alarm."
    is Challenge.MathChallenge -> "A quick mental check before the alarm can be turned off."
    is Challenge.ShakeChallenge -> "Physical movement helps stop sleepy autopilot."
    is Challenge.SequenceChallenge -> "Tap in order without losing your place."
    is Challenge.MemoryPatternChallenge -> "Watch the tiles, then repeat them exactly."
    is Challenge.TypingChallenge -> "Typing the phrase confirms you are alert enough to finish."
    is Challenge.WalkChallenge -> "A few steps create enough movement to wake up properly."
    is Challenge.NfcChallenge -> "Use the tag you saved for this alarm."
    is Challenge.BarcodeChallenge -> "Scan the code you linked to this wake-up routine."
    is Challenge.PhotoMatchChallenge -> "Take a fresh photo that closely matches the saved reference."
    is Challenge.SquatChallenge -> "A short movement challenge helps you actually get moving."
    is Challenge.MazeChallenge -> "Stay focused and reach the exit."
    is Challenge.WifiChallenge -> "This alarm clears once you reconnect where you planned."
    is Challenge.CountSheepChallenge -> "A light-focus wake-up \u2014 tap only the sheep."
    is Challenge.SimonSaysChallenge -> "Watch the four-color sequence, then repeat it in order."
    is Challenge.DateBackwardsChallenge -> "Reading + typing cognitive gate \u2014 hard to do half-asleep."
    is Challenge.StroopChallenge -> "Classic interference test \u2014 pick the ink, ignore the word."
}

private fun Challenge?.statusDescription(): String = when (this) {
    is Challenge.MathChallenge -> "Solve the prompt below to unlock dismiss."
    is Challenge.ShakeChallenge -> "Keep moving until the shake target is complete."
    is Challenge.SequenceChallenge -> "Tap the numbers in ascending order."
    is Challenge.MemoryPatternChallenge -> "Memorize first, then repeat the pattern."
    is Challenge.TypingChallenge -> "Type the full phrase exactly as shown."
    is Challenge.WalkChallenge -> "Walk enough steps to prove you are up."
    is Challenge.NfcChallenge -> "Tap the saved NFC tag to continue."
    is Challenge.BarcodeChallenge -> "Scan the saved barcode or QR code."
    is Challenge.PhotoMatchChallenge -> "Take a matching photo to finish this wake-up step."
    is Challenge.SquatChallenge -> "Complete the required number of squats."
    is Challenge.MazeChallenge -> "Find the exit without hitting walls."
    is Challenge.WifiChallenge -> "Reconnect to the required Wi-Fi network."
    is Challenge.CountSheepChallenge -> "Tap every sheep; avoid the goats."
    is Challenge.SimonSaysChallenge -> "Wait for playback, then tap the pads in order."
    is Challenge.DateBackwardsChallenge -> "Type today's date reversed exactly."
    is Challenge.StroopChallenge -> "Tap the color the word is painted in."
    null -> "Swipe or tap dismiss when you're ready."
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

@Composable
private fun QuickSnoozeButton(
    minutes: Int,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDefault) SnoozeYellow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            contentColor = if (isDefault) SnoozeYellow else TextSecondary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$minutes min",
                fontWeight = FontWeight.SemiBold
            )
            if (isDefault) {
                AppStatusChip(
                    label = "Default",
                    color = SnoozeYellow
                )
            }
        }
    }
}
