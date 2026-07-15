package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.PushUpChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.PlankHoldChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.TypingChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.VoicePhraseChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.HandwritingChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.WalkChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.CountSheepChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.DateBackwardsChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.SimonSaysChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.StroopChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.WifiChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.RockPaperScissorsChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.EmojiMemoryChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.TypingSpeedChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.WordleChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.PvtChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.SpotDifferenceChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChessMateChallengeView
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.RsvpReadingChallengeView
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.LocalMotionEnabled
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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
    val firingControlMode by viewModel.firingControlMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
    }
    val effectiveControlMode = if (
        firingControlMode == "hybrid" &&
        accessibilityManager?.isTouchExplorationEnabled == true
    ) "buttons" else firingControlMode
    val showSwipeControls = effectiveControlMode != "buttons"
    val showButtonControls = effectiveControlMode != "swipe"
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

    val motionEnabled = LocalMotionEnabled.current
    val pulseScale: Float
    val pulseAlpha: Float
    if (motionEnabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "alarmPulse")
        pulseScale = infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        ).value
        pulseAlpha = infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        ).value
    } else {
        pulseScale = 1f
        pulseAlpha = 1f
    }

    val challenge = state.challenge
    val locationDismissActive = state.alarm?.locationDismissEnabled == true &&
        !state.locationDismissReady &&
        !state.challengeBypassAvailable
    val locationDismissDistanceMeters = state.locationDismissDistanceMeters
    val locationDismissMessage = state.locationDismissStatus.ifBlank {
        "Leave the saved area to unlock dismissal. Snooze remains available."
    }
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
    val holdToDismissEnabled = state.alarm?.holdToDismissEnabled == true
    var customSnoozeMinutes by remember(defaultSnoozeMinutes) {
        mutableIntStateOf(defaultSnoozeMinutes.coerceIn(MIN_CUSTOM_SNOOZE_MINUTES, MAX_CUSTOM_SNOOZE_MINUTES))
    }
    val firingBackgroundUri = state.alarm
        ?.takeIf { it.firingBackgroundImageEnabled && it.firingBackgroundImageUri.isNotBlank() }
        ?.firingBackgroundImageUri
    val firingBackgroundBlurEnabled = state.alarm?.firingBackgroundBlurEnabled == true
    val firingBackgroundImage by produceState<ImageBitmap?>(initialValue = null, firingBackgroundUri) {
        value = null
        val uri = firingBackgroundUri ?: return@produceState
        value = loadFiringBackgroundImage(context, uri)
    }

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
        state.canDismiss && holdToDismissEnabled ->
            "Wake-up steps are complete. Hold Dismiss for 1.5 seconds, or right-swipe to snooze."
        state.canDismiss -> "Wake-up steps are complete. Swipe left to dismiss, or right to snooze."
        locationDismissActive && state.wakeChallengeReady -> locationDismissMessage
        challenge == null && holdToDismissEnabled -> "Hold Dismiss for 1.5 seconds to stop the alarm."
        challenge == null -> "Swipe left or tap dismiss to stop the alarm."
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
            .let { mod ->
                if (!showSwipeControls) return@let mod
                mod.pointerInput(state.canDismiss, holdToDismissEnabled) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeCumulativeDrag = 0f },
                    onDragEnd = {
                        if (swipeCumulativeDrag < -swipeThreshold && state.canDismiss) {
                            if (holdToDismissEnabled) {
                                swipeHint = "Hold the Dismiss button for 1.5 seconds."
                            } else {
                                onDismiss()
                                swipeHint = ""
                            }
                        } else if (swipeCumulativeDrag > swipeThreshold) {
                            onSnooze()
                            swipeHint = ""
                        } else {
                            swipeHint = ""
                        }
                        swipeCumulativeDrag = 0f
                    },
                    onDragCancel = {
                        swipeHint = ""
                        swipeCumulativeDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeCumulativeDrag += dragAmount
                        swipeHint = when {
                            swipeCumulativeDrag < -swipeThreshold / 2 && state.canDismiss && holdToDismissEnabled ->
                                "Hold Dismiss for 1.5 seconds"
                            swipeCumulativeDrag < -swipeThreshold / 2 && state.canDismiss -> "Release to dismiss"
                            swipeCumulativeDrag > swipeThreshold / 2 -> "Release to snooze"
                            swipeCumulativeDrag < -50 && state.canDismiss && holdToDismissEnabled ->
                                "Swipe-left is protected. Use Hold Dismiss."
                            swipeCumulativeDrag < -50 && state.canDismiss -> "Swipe left to dismiss"
                            swipeCumulativeDrag < -50 -> "Finish the wake-up step first"
                            swipeCumulativeDrag > 50 -> "Swipe right to snooze"
                            else -> ""
                        }
                    }
                )
            }
            }
    ) {
        if (firingBackgroundImage != null) {
            Image(
                bitmap = requireNotNull(firingBackgroundImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (firingBackgroundBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(24.dp)
                        } else {
                            Modifier
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SurfaceDark.copy(alpha = 0.52f),
                                SurfaceDark.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        }

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
                        label = when {
                            state.canDismiss && holdToDismissEnabled -> "Hold required"
                            state.canDismiss -> "Dismiss ready"
                            locationDismissActive && state.wakeChallengeReady -> "Location locked"
                            else -> "Dismiss locked"
                        },
                        icon = when {
                            state.canDismiss -> Icons.Default.CheckCircle
                            locationDismissActive && state.wakeChallengeReady -> Icons.Default.LocationOn
                            else -> Icons.Default.WarningAmber
                        },
                        color = if (state.canDismiss) DismissGreen else SnoozeYellow
                    )
                    if (state.challengeBypassRemainingSeconds > 0 && !state.canDismiss) {
                        AppStatusChip(
                            label = "Bypass in ${state.challengeBypassRemainingSeconds}s",
                            icon = Icons.Default.AccessTime,
                            color = TextMuted
                        )
                    }
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
                    if (state.firedEarlyForWeather) {
                        AppStatusChip(
                            label = "Fired early — ${state.weatherDescription ?: "weather"}",
                            icon = Icons.Default.AcUnit,
                            color = AccentBlue
                        )
                    } else if (state.weatherTemp != null) {
                        AppStatusChip(
                            label = "${state.weatherTemp} ${state.weatherDescription ?: ""}".trim(),
                            icon = Icons.Default.Cloud,
                            color = TextSecondary
                        )
                    }
                    AppStatusChip(
                        label = if (state.totalChallenges > 1) stepLabel else "Wake-up check",
                        icon = Icons.Default.TaskAlt,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.alarm?.locationDismissEnabled == true) {
                        AppStatusChip(
                            label = when {
                                state.locationDismissReady -> "Left saved place"
                                locationDismissDistanceMeters != null ->
                                    "${locationDismissDistanceMeters.toInt()} m from place"
                                else -> "Location lock"
                            },
                            icon = Icons.Default.LocationOn,
                            color = if (state.locationDismissReady) DismissGreen else SnoozeYellow
                        )
                    }
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
                    if (holdToDismissEnabled) {
                        AppStatusChip(
                            label = "Hold dismiss 1.5s",
                            icon = Icons.Default.AlarmOff,
                            color = DismissGreen
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
                    title = if (locationDismissActive && state.wakeChallengeReady) {
                        "Leave the saved place"
                    } else {
                        challenge.headline()
                    },
                    description = if (locationDismissActive && state.wakeChallengeReady) {
                        locationDismissMessage
                    } else {
                        challenge.supportingText()
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state.canDismiss -> {
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
                                    text = if (holdToDismissEnabled) {
                                        "You’ve cleared every required step. Hold the Dismiss button to finish, or snooze if you need a short buffer."
                                    } else {
                                        "You’ve cleared every required step. Dismiss now or snooze if you need a short buffer."
                                    },
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        locationDismissActive && state.wakeChallengeReady -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SnoozeYellow.copy(alpha = pulseAlpha),
                                    modifier = Modifier
                                        .size(92.dp)
                                        .scale(pulseScale)
                                )
                                Text(
                                    text = "Move outside the saved area",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = locationDismissMessage,
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

                        challenge is Challenge.VoicePhraseChallenge -> {
                            VoicePhraseChallengeView(
                                challenge = challenge,
                                transcript = state.voiceTranscript,
                                status = state.voiceStatus,
                                wrongAttempts = state.wrongAttempts,
                                onRecognized = viewModel::submitVoicePhrase
                            )
                        }

                        challenge is Challenge.HandwritingChallenge -> {
                            HandwritingChallengeView(
                                challenge = challenge,
                                status = state.handwritingStatus,
                                busy = state.handwritingBusy,
                                wrongAttempts = state.wrongAttempts,
                                onRecognize = viewModel::submitHandwriting,
                                onTypedFallback = viewModel::submitHandwritingFallback
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
                                currentSquats = state.squatCount,
                                exerciseStatus = state.exerciseStatus,
                                fallbackAllowed = state.exerciseFallbackAllowed,
                                onContinueWithoutSensor = viewModel::continueExerciseChallengeWithoutSensor
                            )
                        }

                        challenge is Challenge.PushUpChallenge -> {
                            PushUpChallengeView(
                                challenge = challenge,
                                currentPushUps = state.pushUpCount,
                                exerciseStatus = state.exerciseStatus,
                                fallbackAllowed = state.exerciseFallbackAllowed,
                                onContinueWithoutSensor = viewModel::continueExerciseChallengeWithoutSensor
                            )
                        }

                        challenge is Challenge.PlankHoldChallenge -> {
                            PlankHoldChallengeView(
                                challenge = challenge,
                                heldSeconds = state.plankHoldSeconds,
                                isActive = state.plankHoldActive,
                                onStart = viewModel::onPlankHoldStart,
                                onBreak = viewModel::onPlankHoldBreak
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

                        challenge is Challenge.RockPaperScissorsChallenge -> {
                            RockPaperScissorsChallengeView(
                                challenge = challenge,
                                playerWins = state.rpsPlayerWins,
                                computerWins = state.rpsComputerWins,
                                rounds = state.rpsRounds,
                                onPick = viewModel::onRpsPick
                            )
                        }

                        challenge is Challenge.EmojiMemoryChallenge -> {
                            EmojiMemoryChallengeView(
                                challenge = challenge,
                                phase = state.emojiMemoryPhase,
                                flippedIndices = state.emojiFlippedIndices,
                                matchedIndices = state.emojiMatchedIndices,
                                onCardFlip = viewModel::onEmojiCardFlip
                            )
                        }

                        challenge is Challenge.TypingSpeedChallenge -> {
                            TypingSpeedChallengeView(
                                challenge = challenge,
                                currentInput = state.typingSpeedInput,
                                onInputChanged = viewModel::onTypingSpeedInputChange,
                                onSubmit = viewModel::submitTypingSpeed,
                                wrongAttempts = state.wrongAttempts
                            )
                        }

                        challenge is Challenge.WordleChallenge -> {
                            WordleChallengeView(
                                challenge = challenge,
                                guesses = state.wordleGuesses,
                                currentInput = state.wordleCurrentInput,
                                gameOver = state.wordleGameOver,
                                onInputChanged = viewModel::updateWordleInput,
                                onSubmit = viewModel::submitWordleGuess
                            )
                        }
                        challenge is Challenge.PvtChallenge -> {
                            PvtChallengeView(
                                challenge = challenge,
                                trialIndex = state.pvtTrialIndex,
                                reactionTimes = state.pvtReactionTimes,
                                stimulusShown = state.pvtStimulusShown,
                                waiting = state.pvtWaiting,
                                lastReaction = state.pvtLastReaction,
                                failed = state.pvtFailed,
                                onTap = viewModel::onPvtReaction,
                                onFalseStart = viewModel::onPvtFalseStart,
                                onStartTrial = viewModel::startPvtTrial
                            )
                        }

                        challenge is Challenge.SpotDifferenceChallenge -> {
                            SpotDifferenceChallengeView(
                                challenge = challenge,
                                wrongAttempts = state.wrongAttempts,
                                onPick = viewModel::onSpotDifferencePick
                            )
                        }

                        challenge is Challenge.ChessMateChallenge -> {
                            ChessMateChallengeView(
                                challenge = challenge,
                                wrongAttempts = state.wrongAttempts,
                                onPick = viewModel::onChessMateMovePick
                            )
                        }

                        challenge is Challenge.RsvpReadingChallenge -> {
                            RsvpReadingChallengeView(
                                challenge = challenge,
                                wrongAttempts = state.wrongAttempts,
                                onPick = viewModel::onRsvpReadingChoice
                            )
                        }
                    }
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))

                if (showSwipeControls) {
                    Text(
                        text = if (swipeHint.isBlank()) {
                            if (state.canDismiss) {
                                if (holdToDismissEnabled && flipToSnoozeEnabled) {
                                    "Hold Dismiss for 1.5 seconds or swipe right to snooze. Flip the phone over for a quick snooze."
                                } else if (holdToDismissEnabled) {
                                    "Hold Dismiss for 1.5 seconds or swipe right to snooze."
                                } else if (flipToSnoozeEnabled) {
                                    "Swipe left to dismiss or right to snooze. Flip the phone over for a quick snooze."
                                } else {
                                    "Swipe left to dismiss or right to snooze."
                                }
                            } else if (locationDismissActive && state.wakeChallengeReady) {
                                "Swipe right to snooze if you need a short reset. Dismiss unlocks after you leave the saved place."
                            } else {
                                "Swipe right to snooze if you need a short reset. Dismiss unlocks once the wake-up task is complete."
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
            }

            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                AppSectionTitle(
                    title = "Alarm controls",
                    description = if (state.canDismiss) {
                        if (holdToDismissEnabled) {
                            "Hold to confirm dismissal. Snooze remains a quick action."
                        } else {
                            "Choose the cleanest exit for this alarm now that the wake-up work is done."
                        }
                    } else if (locationDismissActive && state.wakeChallengeReady) {
                        "Snooze remains available. Dismiss unlocks when location confirms you left the saved area."
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
                        label = when {
                            state.canDismiss && holdToDismissEnabled -> "Hold Dismiss to finish"
                            state.canDismiss && showSwipeControls -> "Swipe left to dismiss"
                            state.canDismiss -> "Tap to dismiss"
                            locationDismissActive && state.wakeChallengeReady -> "Dismiss unlocks after location"
                            else -> "Dismiss unlocks after challenge"
                        },
                        icon = if (state.canDismiss) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        color = if (state.canDismiss) DismissGreen else TextMuted
                    )
                    AppStatusChip(
                        label = if (showSwipeControls) "Swipe right to snooze" else "Tap to snooze",
                        icon = Icons.Default.Snooze,
                        color = SnoozeYellow
                    )
                }

                if (showButtonControls) {
                    if (holdToDismissEnabled) {
                        HoldToDismissButton(
                            enabled = state.canDismiss,
                            onDismiss = onDismiss
                        )
                    } else {
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when {
                                    state.canDismiss -> "Dismiss alarm"
                                    locationDismissActive && state.wakeChallengeReady -> "Leave saved place to dismiss"
                                    else -> "Finish wake-up challenge"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    LongPressSnoozeButton(
                        minutes = defaultSnoozeMinutes,
                        onClick = onSnooze,
                        onLongClick = {
                            customSnoozeMinutes = defaultSnoozeMinutes
                                .coerceIn(MIN_CUSTOM_SNOOZE_MINUTES, MAX_CUSTOM_SNOOZE_MINUTES)
                            showSnoozeOptions = true
                        }
                    )

                    Text(
                        text = "Long-press Snooze for exact minutes. Your saved default stays unchanged.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = { showSnoozeOptions = !showSnoozeOptions },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showSnoozeOptions) "Hide snooze choices" else "Choose preset or exact minutes",
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

                    SnoozeMinutePicker(
                        minutes = customSnoozeMinutes,
                        onMinutesChange = { minutes ->
                            customSnoozeMinutes = minutes.coerceIn(
                                MIN_CUSTOM_SNOOZE_MINUTES,
                                MAX_CUSTOM_SNOOZE_MINUTES
                            )
                        },
                        onSnooze = { onSnoozeCustom(customSnoozeMinutes) }
                    )
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
    is Challenge.VoicePhraseChallenge -> "Say the wake-up phrase"
    is Challenge.HandwritingChallenge -> "Draw the wake-up word"
    is Challenge.WalkChallenge -> "Walk it off"
    is Challenge.NfcChallenge -> "Tap the registered NFC tag"
    is Challenge.BarcodeChallenge -> "Scan the registered code"
    is Challenge.PhotoMatchChallenge -> "Match the reference photo"
    is Challenge.SquatChallenge -> "Complete the movement check"
    is Challenge.PushUpChallenge -> "Do the push-ups"
    is Challenge.PlankHoldChallenge -> "Hold the plank"
    is Challenge.MazeChallenge -> "Navigate out of the maze"
    is Challenge.WifiChallenge -> "Connect to the right network"
    is Challenge.CountSheepChallenge -> "Count the sheep"
    is Challenge.SimonSaysChallenge -> "Play back the pattern"
    is Challenge.DateBackwardsChallenge -> "Type the date backwards"
    is Challenge.StroopChallenge -> "Tap the ink color"
    is Challenge.RockPaperScissorsChallenge -> "Win at Rock Paper Scissors"
    is Challenge.EmojiMemoryChallenge -> "Match the emoji pairs"
    is Challenge.TypingSpeedChallenge -> "Type at speed"
    is Challenge.WordleChallenge -> "Solve the Wordle"
    is Challenge.PvtChallenge -> "React to the stimulus"
    is Challenge.SpotDifferenceChallenge -> "Spot the difference"
    is Challenge.ChessMateChallenge -> "Find mate in one"
    is Challenge.RsvpReadingChallenge -> "Read at speed"
}

private fun Challenge?.supportingText(): String = when (this) {
    null -> "No extra challenge is required for this alarm."
    is Challenge.MathChallenge -> "A quick mental check before the alarm can be turned off."
    is Challenge.ShakeChallenge -> "Physical movement helps stop sleepy autopilot."
    is Challenge.SequenceChallenge -> "Tap in order without losing your place."
    is Challenge.MemoryPatternChallenge -> "Watch the tiles, then repeat them exactly."
    is Challenge.TypingChallenge -> "Typing the phrase confirms you are alert enough to finish."
    is Challenge.VoicePhraseChallenge -> "Speaking the phrase adds a hands-free wake-up check."
    is Challenge.HandwritingChallenge -> "Writing the word gives your brain and hands a wake-up check."
    is Challenge.WalkChallenge -> "A few steps create enough movement to wake up properly."
    is Challenge.NfcChallenge -> "Use the tag you saved for this alarm."
    is Challenge.BarcodeChallenge -> "Scan the code you linked to this wake-up routine."
    is Challenge.PhotoMatchChallenge -> "Take a fresh photo that closely matches the saved reference."
    is Challenge.SquatChallenge -> "A short movement challenge helps you actually get moving."
    is Challenge.PushUpChallenge -> "Push-ups force upper-body engagement that resets sleepiness."
    is Challenge.PlankHoldChallenge -> "A sustained hold proves you are awake and in control."
    is Challenge.MazeChallenge -> "Stay focused and reach the exit."
    is Challenge.WifiChallenge -> "This alarm clears once you reconnect where you planned."
    is Challenge.CountSheepChallenge -> "A light-focus wake-up \u2014 tap only the sheep."
    is Challenge.SimonSaysChallenge -> "Watch the four-color sequence, then repeat it in order."
    is Challenge.DateBackwardsChallenge -> "Reading + typing cognitive gate \u2014 hard to do half-asleep."
    is Challenge.StroopChallenge -> "Classic interference test \u2014 pick the ink, ignore the word."
    is Challenge.RockPaperScissorsChallenge -> "Best-of-5 against the computer \u2014 first to 3 wins."
    is Challenge.EmojiMemoryChallenge -> "Memorise 8 pairs while they\u2019re face-up, then find them all."
    is Challenge.TypingSpeedChallenge -> "Groggy fingers slow you down \u2014 prove you can type at speed."
    is Challenge.WordleChallenge -> "Find the hidden 5-letter word in up to 6 guesses."
    is Challenge.PvtChallenge -> "Tap fast when the target appears \u2014 average under 500 ms."
    is Challenge.SpotDifferenceChallenge -> "Compare both grids and tap the one tile that changed."
    is Challenge.ChessMateChallenge -> "Pick the checkmate move from a small board position."
    is Challenge.RsvpReadingChallenge -> "Read a rapid word stream, then identify a word you saw."
}

private fun Challenge?.statusDescription(): String = when (this) {
    is Challenge.MathChallenge -> "Solve the prompt below to unlock dismiss."
    is Challenge.ShakeChallenge -> "Keep moving until the shake target is complete."
    is Challenge.SequenceChallenge -> "Tap the numbers in ascending order."
    is Challenge.MemoryPatternChallenge -> "Memorize first, then repeat the pattern."
    is Challenge.TypingChallenge -> "Type the full phrase exactly as shown."
    is Challenge.VoicePhraseChallenge -> "Say the phrase shown below, or use the typed fallback."
    is Challenge.HandwritingChallenge -> "Draw the displayed word, or use the typed fallback."
    is Challenge.WalkChallenge -> "Walk enough steps to prove you are up."
    is Challenge.NfcChallenge -> "Tap the saved NFC tag to continue."
    is Challenge.BarcodeChallenge -> "Scan the saved barcode or QR code."
    is Challenge.PhotoMatchChallenge -> "Take a matching photo to finish this wake-up step."
    is Challenge.SquatChallenge -> "Complete the required number of squats."
    is Challenge.PushUpChallenge -> "Complete the required push-ups with the phone on the floor."
    is Challenge.PlankHoldChallenge -> "Hold the phone level for the full duration."
    is Challenge.MazeChallenge -> "Find the exit without hitting walls."
    is Challenge.WifiChallenge -> "Reconnect to the required Wi-Fi network."
    is Challenge.CountSheepChallenge -> "Tap every sheep; avoid the goats."
    is Challenge.SimonSaysChallenge -> "Wait for playback, then tap the pads in order."
    is Challenge.DateBackwardsChallenge -> "Type today's date reversed exactly."
    is Challenge.StroopChallenge -> "Tap the color the word is painted in."
    is Challenge.RockPaperScissorsChallenge -> "Win 3 rounds to clear the alarm."
    is Challenge.EmojiMemoryChallenge -> "Flip pairs of matching emoji until all are found."
    is Challenge.TypingSpeedChallenge -> "Type the phrase fast and accurately to proceed."
    is Challenge.WordleChallenge -> "Enter a 5-letter guess and use the color clues."
    is Challenge.PvtChallenge -> "Tap the green square as fast as you can."
    is Challenge.SpotDifferenceChallenge -> "Tap the changed tile in the right-hand grid."
    is Challenge.ChessMateChallenge -> "Choose the mate-in-1 move."
    is Challenge.RsvpReadingChallenge -> "Watch the words, then pick the remembered word."
    null -> "Swipe or tap dismiss when you're ready."
}

private suspend fun loadFiringBackgroundImage(
    context: Context,
    uriString: String,
    maxSide: Int = 1600
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun calculateSampleSize(width: Int, height: Int, maxSide: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sampleSize = 1
    while (width / sampleSize > maxSide || height / sampleSize > maxSide) {
        sampleSize *= 2
    }
    return sampleSize
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private const val MIN_CUSTOM_SNOOZE_MINUTES = 1
private const val MAX_CUSTOM_SNOOZE_MINUTES = 120
private const val HOLD_TO_DISMISS_MS = 1_500L
private const val HOLD_PROGRESS_FRAME_MS = 16L

@Composable
private fun HoldToDismissButton(
    enabled: Boolean,
    onDismiss: () -> Unit
) {
    var isHolding by remember(enabled) { mutableStateOf(false) }
    var holdProgress by remember(enabled) { mutableFloatStateOf(0f) }

    LaunchedEffect(enabled, isHolding) {
        if (!enabled) {
            holdProgress = 0f
            return@LaunchedEffect
        }
        if (!isHolding) {
            holdProgress = 0f
            return@LaunchedEffect
        }

        var elapsedMs = 0L
        while (isHolding && elapsedMs < HOLD_TO_DISMISS_MS) {
            delay(HOLD_PROGRESS_FRAME_MS)
            elapsedMs += HOLD_PROGRESS_FRAME_MS
            holdProgress = (elapsedMs.toFloat() / HOLD_TO_DISMISS_MS).coerceIn(0f, 1f)
        }
        if (isHolding) {
            holdProgress = 1f
            isHolding = false
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (enabled) {
                    "Hold Dismiss for 1.5 seconds"
                } else {
                    "Finish the wake-up challenge before dismissing"
                }
                stateDescription = if (enabled) "Dismiss ready" else "Dismiss locked"
            }
            .pointerInput(enabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (enabled) {
                        isHolding = true
                        waitForUpOrCancellation()
                        isHolding = false
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) {
            DismissGreen.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) DismissGreen.copy(alpha = 0.82f) else TextMuted.copy(alpha = 0.24f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (holdProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(holdProgress)
                        .background(DismissGreen.copy(alpha = 0.24f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AlarmOff,
                    contentDescription = null,
                    tint = if (enabled) DismissGreen else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when {
                        !enabled -> "Finish wake-up challenge"
                        isHolding -> "Keep holding"
                        else -> "Hold 1.5s to dismiss"
                    },
                    color = if (enabled) DismissGreen else TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressSnoozeButton(
    minutes: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Snooze for $minutes minutes",
                onClick = onClick,
                onLongClickLabel = "Choose exact snooze length",
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        border = BorderStroke(1.5.dp, SnoozeYellow.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Snooze,
                contentDescription = null,
                tint = SnoozeYellow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Snooze for $minutes min",
                color = SnoozeYellow,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SnoozeMinutePicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    onSnooze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Exact snooze",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Applies once to this alarm.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "$minutes min",
                color = SnoozeYellow,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Slider(
            value = minutes.toFloat(),
            onValueChange = { onMinutesChange(it.roundToInt()) },
            valueRange = MIN_CUSTOM_SNOOZE_MINUTES.toFloat()..MAX_CUSTOM_SNOOZE_MINUTES.toFloat(),
            steps = MAX_CUSTOM_SNOOZE_MINUTES - MIN_CUSTOM_SNOOZE_MINUTES - 1,
            colors = SliderDefaults.colors(
                thumbColor = SnoozeYellow,
                activeTrackColor = SnoozeYellow,
                inactiveTrackColor = TextMuted.copy(alpha = 0.30f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMinutesChange(minutes - 1) },
                enabled = minutes > MIN_CUSTOM_SNOOZE_MINUTES
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease snooze minutes",
                    tint = if (minutes > MIN_CUSTOM_SNOOZE_MINUTES) TextSecondary else TextMuted
                )
            }
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, SnoozeYellow.copy(alpha = 0.74f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SnoozeYellow
                )
            ) {
                Text(
                    text = "Snooze $minutes min",
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(
                onClick = { onMinutesChange(minutes + 1) },
                enabled = minutes < MAX_CUSTOM_SNOOZE_MINUTES
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase snooze minutes",
                    tint = if (minutes < MAX_CUSTOM_SNOOZE_MINUTES) TextSecondary else TextMuted
                )
            }
        }
    }
}

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
        shape = RoundedCornerShape(12.dp),
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
