@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.sysadmindoc.alarmclock.R
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
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.LocalMotionEnabled
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun AlarmFiringScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onSnoozeCustom: (Int) -> Unit = { onSnooze() },
    onSnoozeUntil: (Long) -> Unit = { onSnooze() },
    onTakePhoto: () -> Unit = {},
    viewModel: AlarmFiringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // These two prefs gate optional UI surfaces below; collecting them with the
    // lifecycle keeps the firing screen reactive to a settings toggle made
    // mid-alarm (rare, but possible if user pulls down quick settings).
    val showQuotes by viewModel.showMotivationalQuotes.collectAsStateWithLifecycle()
    val flipToSnoozeEnabled by viewModel.flipToSnoozeEnabled.collectAsStateWithLifecycle()
    val holdToDismissMillis by viewModel.holdToDismissMillis.collectAsStateWithLifecycle()
    val holdDurationSeconds = holdToDismissMillis / 1000f
    val firingControlMode by viewModel.firingControlMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
    }
    val effectiveControlMode = if (
        firingControlMode == "hybrid" &&
        accessibilityManager?.isTouchExplorationEnabled == true
    ) stringResource(R.string.alarmfiring_buttons) else firingControlMode
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
        stringResource(R.string.firing_leave_area_hint)
    }
    if (challenge is Challenge.MemoryPatternChallenge && state.memoryPhase == MemoryPhase.SHOWING) {
        LaunchedEffect(state.memoryPhase, state.wrongAttempts, state.currentChallengeIndex) {
            delay(challenge.showDurationMs)
            viewModel.onMemoryShowComplete()
        }
    }

    var showSnoozeOptions by remember { mutableStateOf(false) }
    var showSnoozeUntilPicker by remember { mutableStateOf(false) }
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
    val dateText = currentDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val alarmLabel = state.alarm?.label?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.notif_alarm_ringing)
    val stepLabel = if (state.totalChallenges > 1) {
        stringResource(
            R.string.firing_step_of,
            state.currentChallengeIndex + 1,
            state.totalChallenges
        )
    } else {
        stringResource(R.string.firing_single_step)
    }
    val statusLine = when {
        state.canDismiss && holdToDismissEnabled ->
            stringResource(R.string.firing_status_hold_or_snooze, holdDurationSeconds)
        state.canDismiss -> stringResource(R.string.firing_status_swipe_or_snooze)
        locationDismissActive && state.wakeChallengeReady -> locationDismissMessage
        challenge == null && holdToDismissEnabled ->
            stringResource(R.string.firing_status_hold_to_stop, holdDurationSeconds)
        challenge == null -> stringResource(R.string.firing_status_swipe_to_stop)
        else -> challenge.statusDescription()
    }
    val holdButtonHint = stringResource(R.string.firing_hold_button_hint, holdDurationSeconds)
    val holdDismissHint = stringResource(R.string.firing_hold_dismiss_short, holdDurationSeconds)
    val releaseDismissHint = stringResource(R.string.firing_release_dismiss)
    val releaseSnoozeHint = stringResource(R.string.firing_release_snooze)
    val noSnoozesLeftHint = stringResource(R.string.firing_snoozes_none_left)
    val swipeProtectedHint = stringResource(R.string.firing_swipe_protected)
    val swipeLeftHint = stringResource(R.string.firing_swipe_left_dismiss)
    val finishStepHint = stringResource(R.string.firing_finish_step_first)
    val swipeRightHint = stringResource(R.string.firing_swipe_right_snooze)

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
                mod.pointerInput(state.canDismiss, holdToDismissEnabled, state.snoozeAllowed) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeCumulativeDrag = 0f },
                    onDragEnd = {
                        if (swipeCumulativeDrag < -swipeThreshold && state.canDismiss) {
                            if (holdToDismissEnabled) {
                                swipeHint = holdButtonHint
                            } else {
                                onDismiss()
                                swipeHint = ""
                            }
                        } else if (swipeCumulativeDrag > swipeThreshold) {
                            if (state.snoozeAllowed) {
                                onSnooze()
                                swipeHint = ""
                            } else {
                                swipeHint = noSnoozesLeftHint
                            }
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
                                holdDismissHint
                            swipeCumulativeDrag < -swipeThreshold / 2 && state.canDismiss ->
                                releaseDismissHint
                            swipeCumulativeDrag > swipeThreshold / 2 ->
                                releaseSnoozeHint
                            swipeCumulativeDrag < -50 && state.canDismiss && holdToDismissEnabled ->
                                swipeProtectedHint
                            swipeCumulativeDrag < -50 && state.canDismiss ->
                                swipeLeftHint
                            swipeCumulativeDrag < -50 -> finishStepHint
                            swipeCumulativeDrag > 50 -> swipeRightHint
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
                            state.canDismiss && holdToDismissEnabled -> stringResource(R.string.firing_hold_required)
                            state.canDismiss -> stringResource(R.string.firing_dismiss_ready)
                            locationDismissActive && state.wakeChallengeReady ->
                                stringResource(R.string.firing_location_locked)
                            else -> stringResource(R.string.firing_dismiss_locked)
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
                            label = stringResource(
                                R.string.firing_bypass_in_seconds,
                                state.challengeBypassRemainingSeconds
                            ),
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
                            label = stringResource(
                                R.string.firing_weather_early,
                                state.weatherDescription ?: stringResource(R.string.firing_weather_fallback)
                            ),
                            icon = Icons.Default.AcUnit,
                            color = AccentBlue
                        )
                    } else if (state.weatherTemp != null) {
                        AppStatusChip(
                            label = stringResource(
                                R.string.firing_weather_summary,
                                state.weatherTemp.orEmpty(),
                                state.weatherDescription.orEmpty()
                            ).trim(),
                            icon = Icons.Default.Cloud,
                            color = TextSecondary
                        )
                    }
                    AppStatusChip(
                        label = if (state.totalChallenges > 1) {
                            stepLabel
                        } else {
                            stringResource(R.string.firing_wakeup_check)
                        },
                        icon = Icons.Default.TaskAlt,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.alarm?.locationDismissEnabled == true) {
                        AppStatusChip(
                            label = when {
                                state.locationDismissReady -> stringResource(R.string.firing_left_saved_place)
                                locationDismissDistanceMeters != null ->
                                    stringResource(
                                        R.string.firing_distance_from_place,
                                        locationDismissDistanceMeters.toInt()
                                    )
                                else -> stringResource(R.string.firing_location_lock)
                            },
                            icon = Icons.Default.LocationOn,
                            color = if (state.locationDismissReady) DismissGreen else SnoozeYellow
                        )
                    }
                    if (state.wrongAttempts > 0) {
                        AppStatusChip(
                            label = pluralStringResource(
                                R.plurals.firing_retry_count,
                                state.wrongAttempts,
                                state.wrongAttempts
                            ),
                            icon = Icons.Default.WarningAmber,
                            color = AccentRed
                        )
                    }
                    AppStatusChip(
                        label = stringResource(
                            R.string.firing_default_snooze_minutes,
                            state.alarm?.snoozeDurationMinutes ?: 10
                        ),
                        icon = Icons.Default.Timer,
                        color = SnoozeYellow
                    )
                    // Only advertise flip-to-snooze when the user actually
                    // enabled the global setting — otherwise the chip lies.
                    if (flipToSnoozeEnabled) {
                        AppStatusChip(
                            label = stringResource(R.string.firing_flip_to_snooze),
                            icon = Icons.Default.Snooze,
                            color = TextMuted
                        )
                    }
                    if (holdToDismissEnabled) {
                        AppStatusChip(
                            label = stringResource(R.string.firing_hold_dismiss, holdDurationSeconds),
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
                        stringResource(R.string.firing_leave_place_title)
                    } else {
                        challenge.headline()
                    },
                    description = if (locationDismissActive && state.wakeChallengeReady) {
                        locationDismissMessage
                    } else {
                        challenge.supportingText()
                    }
                )

                // When the Wi-Fi check cannot run, the challenge is swapped for
                // a solvable one; say why rather than leaving an unexplained
                // maths problem on a Wi-Fi alarm.
                val challengeNotice = when {
                    state.challengeNotice.isNotBlank() -> state.challengeNotice
                    state.wifiStatus.isNotBlank() && challenge !is Challenge.WifiChallenge ->
                        state.wifiStatus
                    else -> ""
                }
                if (challengeNotice.isNotBlank()) {
                    Text(
                        text = challengeNotice,
                        color = SnoozeYellow,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                                    text = stringResource(R.string.firing_ready_title),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = if (holdToDismissEnabled) {
                                        stringResource(R.string.firing_ready_hold_description)
                                    } else {
                                        stringResource(R.string.firing_ready_description)
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
                                    text = stringResource(R.string.firing_move_outside),
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
                                statusTone = state.voiceStatusTone,
                                wrongAttempts = state.wrongAttempts,
                                onRecognized = viewModel::submitVoicePhrase
                            )
                        }

                        challenge is Challenge.HandwritingChallenge -> {
                            HandwritingChallengeView(
                                challenge = challenge,
                                status = state.handwritingStatus,
                                statusTone = state.handwritingStatusTone,
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
                                wifiStatus = state.wifiStatus
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
                                    stringResource(
                                        R.string.firing_swipe_hold_flip_hint,
                                        holdDurationSeconds
                                    )
                                } else if (holdToDismissEnabled) {
                                    stringResource(R.string.firing_swipe_hold_hint, holdDurationSeconds)
                                } else if (flipToSnoozeEnabled) {
                                    stringResource(R.string.firing_swipe_flip_hint)
                                } else {
                                    stringResource(R.string.firing_swipe_hint)
                                }
                            } else if (locationDismissActive && state.wakeChallengeReady) {
                                stringResource(R.string.firing_location_snooze_hint)
                            } else if (!state.snoozeAllowed) {
                                noSnoozesLeftHint
                            } else {
                                stringResource(R.string.firing_challenge_snooze_hint)
                            }
                        } else {
                            swipeHint
                        },
                        color = when {
                            swipeCumulativeDrag > swipeThreshold / 2 ||
                                (swipeCumulativeDrag < -swipeThreshold / 2 &&
                                    state.canDismiss && !holdToDismissEnabled) ->
                                MaterialTheme.colorScheme.primary
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
                    title = stringResource(R.string.firing_controls_title),
                    description = if (state.canDismiss) {
                        if (holdToDismissEnabled) {
                            stringResource(R.string.firing_controls_hold_description)
                        } else {
                            stringResource(R.string.firing_controls_ready_description)
                        }
                    } else if (locationDismissActive && state.wakeChallengeReady) {
                        stringResource(R.string.firing_controls_location_description)
                    } else {
                        stringResource(R.string.firing_controls_locked_description)
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
                            state.canDismiss && holdToDismissEnabled ->
                                stringResource(R.string.firing_hold_to_finish)
                            state.canDismiss && showSwipeControls ->
                                stringResource(R.string.firing_swipe_left_dismiss)
                            state.canDismiss -> stringResource(R.string.firing_tap_dismiss)
                            locationDismissActive && state.wakeChallengeReady ->
                                stringResource(R.string.firing_dismiss_after_location)
                            else -> stringResource(R.string.firing_dismiss_after_challenge)
                        },
                        icon = if (state.canDismiss) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        color = if (state.canDismiss) DismissGreen else TextMuted
                    )
                    AppStatusChip(
                        label = when {
                            !state.snoozeAllowed ->
                                stringResource(R.string.firing_snoozes_none_left)
                            showSwipeControls ->
                                stringResource(R.string.firing_swipe_right_snooze)
                            else -> stringResource(R.string.firing_tap_snooze)
                        },
                        icon = Icons.Default.Snooze,
                        color = if (state.snoozeAllowed) SnoozeYellow else TextMuted
                    )
                }

                if (showButtonControls) {
                    if (holdToDismissEnabled) {
                        HoldToDismissButton(
                            enabled = state.canDismiss,
                            durationMillis = holdToDismissMillis.toLong(),
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
                                    state.canDismiss -> stringResource(R.string.dismiss_alarm)
                                    locationDismissActive && state.wakeChallengeReady ->
                                        stringResource(R.string.firing_leave_place_to_dismiss)
                                    else -> stringResource(R.string.firing_finish_challenge)
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    LongPressSnoozeButton(
                        minutes = defaultSnoozeMinutes,
                        enabled = state.snoozeAllowed,
                        onClick = onSnooze,
                        onLongClick = {
                            customSnoozeMinutes = defaultSnoozeMinutes
                                .coerceIn(MIN_CUSTOM_SNOOZE_MINUTES, MAX_CUSTOM_SNOOZE_MINUTES)
                            showSnoozeOptions = true
                        }
                    )

                    val snoozesLeft = state.snoozesRemaining
                    Text(
                        text = when {
                            !state.snoozeAllowed ->
                                stringResource(R.string.firing_snoozes_none_left)
                            // Zero remaining on an alarm with no dismiss gate
                            // still snoozes; it just ends the alarm. Saying
                            // "0 snoozes left" beside a live button would read
                            // as a contradiction, so fall through to the hint.
                            snoozesLeft != null && snoozesLeft > 0 -> pluralStringResource(
                                R.plurals.firing_snoozes_left,
                                snoozesLeft,
                                snoozesLeft
                            )
                            else -> stringResource(R.string.firing_snooze_hint)
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = { showSnoozeOptions = !showSnoozeOptions },
                        enabled = state.snoozeAllowed,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (showSnoozeOptions) {
                                stringResource(R.string.firing_hide_snooze_choices)
                            } else {
                                stringResource(R.string.firing_choose_snooze)
                            },
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

                        OutlinedButton(
                            onClick = { showSnoozeUntilPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, SnoozeYellow.copy(alpha = 0.74f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SnoozeYellow)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.firing_choose_snooze_until))
                        }
                    }
                }
            }
        }
    }

    if (showSnoozeUntilPicker) {
        val initialTarget = remember(showSnoozeUntilPicker, defaultSnoozeMinutes) {
            LocalTime.now().plusMinutes(defaultSnoozeMinutes.toLong())
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialTarget.hour,
            initialMinute = initialTarget.minute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showSnoozeUntilPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSnoozeUntil(
                            nextSnoozeAtMillis(
                                nowMillis = System.currentTimeMillis(),
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )
                        )
                        showSnoozeUntilPicker = false
                    }
                ) {
                    Text(stringResource(R.string.firing_snooze_until_save), color = SnoozeYellow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnoozeUntilPicker = false }) {
                    Text(stringResource(R.string.alarm_edit_keep_current), color = TextSecondary)
                }
            },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStatusChip(
                        label = stringResource(R.string.firing_snooze_until_title),
                        icon = Icons.Default.AccessTime,
                        color = SnoozeYellow
                    )
                    Text(
                        stringResource(R.string.firing_snooze_until_hint),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(containerColor = SurfaceCard)
                )
            },
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

internal fun nextSnoozeAtMillis(
    nowMillis: Long,
    hour: Int,
    minute: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    val today = now.toLocalDate().atTime(safeHour, safeMinute).atZone(zoneId).toInstant().toEpochMilli()
    return if (today > nowMillis) {
        today
    } else {
        now.toLocalDate().plusDays(1)
            .atTime(safeHour, safeMinute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}

@Composable
private fun Challenge?.headline(): String = stringResource(
    when (this) {
        null -> R.string.firing_challenge_none_title
        is Challenge.MathChallenge -> R.string.firing_challenge_math_title
        is Challenge.ShakeChallenge -> R.string.firing_challenge_shake_title
        is Challenge.SequenceChallenge -> R.string.firing_challenge_sequence_title
        is Challenge.MemoryPatternChallenge -> R.string.firing_challenge_memory_title
        is Challenge.TypingChallenge -> R.string.firing_challenge_typing_title
        is Challenge.VoicePhraseChallenge -> R.string.firing_challenge_voice_title
        is Challenge.HandwritingChallenge -> R.string.firing_challenge_handwriting_title
        is Challenge.WalkChallenge -> R.string.firing_challenge_walk_title
        is Challenge.NfcChallenge -> R.string.firing_challenge_nfc_title
        is Challenge.BarcodeChallenge -> R.string.firing_challenge_barcode_title
        is Challenge.PhotoMatchChallenge -> R.string.firing_challenge_photo_title
        is Challenge.SquatChallenge -> R.string.firing_challenge_squat_title
        is Challenge.PushUpChallenge -> R.string.firing_challenge_pushup_title
        is Challenge.PlankHoldChallenge -> R.string.firing_challenge_plank_title
        is Challenge.MazeChallenge -> R.string.firing_challenge_maze_title
        is Challenge.WifiChallenge -> R.string.firing_challenge_wifi_title
        is Challenge.CountSheepChallenge -> R.string.firing_challenge_sheep_title
        is Challenge.SimonSaysChallenge -> R.string.firing_challenge_simon_title
        is Challenge.DateBackwardsChallenge -> R.string.firing_challenge_date_title
        is Challenge.StroopChallenge -> R.string.firing_challenge_stroop_title
        is Challenge.RockPaperScissorsChallenge -> R.string.firing_challenge_rps_title
        is Challenge.EmojiMemoryChallenge -> R.string.firing_challenge_emoji_title
        is Challenge.TypingSpeedChallenge -> R.string.firing_challenge_speed_title
        is Challenge.WordleChallenge -> R.string.firing_challenge_wordle_title
        is Challenge.PvtChallenge -> R.string.firing_challenge_pvt_title
        is Challenge.SpotDifferenceChallenge -> R.string.firing_challenge_difference_title
        is Challenge.ChessMateChallenge -> R.string.firing_challenge_chess_title
        is Challenge.RsvpReadingChallenge -> R.string.firing_challenge_rsvp_title
    }
)

@Composable
private fun Challenge?.supportingText(): String = stringResource(
    when (this) {
        null -> R.string.firing_challenge_none_support
        is Challenge.MathChallenge -> R.string.firing_challenge_math_support
        is Challenge.ShakeChallenge -> R.string.firing_challenge_shake_support
        is Challenge.SequenceChallenge -> R.string.firing_challenge_sequence_support
        is Challenge.MemoryPatternChallenge -> R.string.firing_challenge_memory_support
        is Challenge.TypingChallenge -> R.string.firing_challenge_typing_support
        is Challenge.VoicePhraseChallenge -> R.string.firing_challenge_voice_support
        is Challenge.HandwritingChallenge -> R.string.firing_challenge_handwriting_support
        is Challenge.WalkChallenge -> R.string.firing_challenge_walk_support
        is Challenge.NfcChallenge -> R.string.firing_challenge_nfc_support
        is Challenge.BarcodeChallenge -> R.string.firing_challenge_barcode_support
        is Challenge.PhotoMatchChallenge -> R.string.firing_challenge_photo_support
        is Challenge.SquatChallenge -> R.string.firing_challenge_squat_support
        is Challenge.PushUpChallenge -> R.string.firing_challenge_pushup_support
        is Challenge.PlankHoldChallenge -> R.string.firing_challenge_plank_support
        is Challenge.MazeChallenge -> R.string.firing_challenge_maze_support
        is Challenge.WifiChallenge -> R.string.firing_challenge_wifi_support
        is Challenge.CountSheepChallenge -> R.string.firing_challenge_sheep_support
        is Challenge.SimonSaysChallenge -> R.string.firing_challenge_simon_support
        is Challenge.DateBackwardsChallenge -> R.string.firing_challenge_date_support
        is Challenge.StroopChallenge -> R.string.firing_challenge_stroop_support
        is Challenge.RockPaperScissorsChallenge -> R.string.firing_challenge_rps_support
        is Challenge.EmojiMemoryChallenge -> R.string.firing_challenge_emoji_support
        is Challenge.TypingSpeedChallenge -> R.string.firing_challenge_speed_support
        is Challenge.WordleChallenge -> R.string.firing_challenge_wordle_support
        is Challenge.PvtChallenge -> R.string.firing_challenge_pvt_support
        is Challenge.SpotDifferenceChallenge -> R.string.firing_challenge_difference_support
        is Challenge.ChessMateChallenge -> R.string.firing_challenge_chess_support
        is Challenge.RsvpReadingChallenge -> R.string.firing_challenge_rsvp_support
    }
)

@Composable
private fun Challenge?.statusDescription(): String = stringResource(
    when (this) {
        is Challenge.MathChallenge -> R.string.firing_challenge_math_status
        is Challenge.ShakeChallenge -> R.string.firing_challenge_shake_status
        is Challenge.SequenceChallenge -> R.string.firing_challenge_sequence_status
        is Challenge.MemoryPatternChallenge -> R.string.firing_challenge_memory_status
        is Challenge.TypingChallenge -> R.string.firing_challenge_typing_status
        is Challenge.VoicePhraseChallenge -> R.string.firing_challenge_voice_status
        is Challenge.HandwritingChallenge -> R.string.firing_challenge_handwriting_status
        is Challenge.WalkChallenge -> R.string.firing_challenge_walk_status
        is Challenge.NfcChallenge -> R.string.firing_challenge_nfc_status
        is Challenge.BarcodeChallenge -> R.string.firing_challenge_barcode_status
        is Challenge.PhotoMatchChallenge -> R.string.firing_challenge_photo_status
        is Challenge.SquatChallenge -> R.string.firing_challenge_squat_status
        is Challenge.PushUpChallenge -> R.string.firing_challenge_pushup_status
        is Challenge.PlankHoldChallenge -> R.string.firing_challenge_plank_status
        is Challenge.MazeChallenge -> R.string.firing_challenge_maze_status
        is Challenge.WifiChallenge -> R.string.firing_challenge_wifi_status
        is Challenge.CountSheepChallenge -> R.string.firing_challenge_sheep_status
        is Challenge.SimonSaysChallenge -> R.string.firing_challenge_simon_status
        is Challenge.DateBackwardsChallenge -> R.string.firing_challenge_date_status
        is Challenge.StroopChallenge -> R.string.firing_challenge_stroop_status
        is Challenge.RockPaperScissorsChallenge -> R.string.firing_challenge_rps_status
        is Challenge.EmojiMemoryChallenge -> R.string.firing_challenge_emoji_status
        is Challenge.TypingSpeedChallenge -> R.string.firing_challenge_speed_status
        is Challenge.WordleChallenge -> R.string.firing_challenge_wordle_status
        is Challenge.PvtChallenge -> R.string.firing_challenge_pvt_status
        is Challenge.SpotDifferenceChallenge -> R.string.firing_challenge_difference_status
        is Challenge.ChessMateChallenge -> R.string.firing_challenge_chess_status
        is Challenge.RsvpReadingChallenge -> R.string.firing_challenge_rsvp_status
        null -> R.string.firing_challenge_none_status
    }
)

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
private const val HOLD_PROGRESS_FRAME_MS = 16L

@Composable
private fun HoldToDismissButton(
    enabled: Boolean,
    durationMillis: Long,
    onDismiss: () -> Unit
) {
    var isHolding by remember(enabled) { mutableStateOf(false) }
    var holdProgress by remember(enabled) { mutableFloatStateOf(0f) }
    val holdDescription = stringResource(
        if (enabled) R.string.firing_hold_accessibility else R.string.firing_challenge_before_dismiss,
        durationMillis / 1000f
    )
    val holdStateDescription = stringResource(
        if (enabled) R.string.firing_dismiss_ready else R.string.firing_dismiss_locked
    )

    LaunchedEffect(enabled, isHolding, durationMillis) {
        if (!enabled) {
            holdProgress = 0f
            return@LaunchedEffect
        }
        if (!isHolding) {
            holdProgress = 0f
            return@LaunchedEffect
        }

        var elapsedMs = 0L
        while (isHolding && elapsedMs < durationMillis) {
            delay(HOLD_PROGRESS_FRAME_MS)
            elapsedMs += HOLD_PROGRESS_FRAME_MS
            holdProgress = (elapsedMs.toFloat() / durationMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)
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
                contentDescription = holdDescription
                stateDescription = holdStateDescription
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
                        !enabled -> stringResource(R.string.firing_finish_challenge)
                        isHolding -> stringResource(R.string.firing_keep_holding)
                        else -> stringResource(
                            R.string.firing_hold_to_dismiss_short,
                            durationMillis / 1000f
                        )
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
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val snoozeLabel = stringResource(R.string.firing_snooze_for_minutes, minutes)
    val exactSnoozeLabel = stringResource(R.string.firing_choose_snooze)
    val tint = if (enabled) SnoozeYellow else TextMuted
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = snoozeLabel,
                onClick = onClick,
                onLongClickLabel = exactSnoozeLabel,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        border = BorderStroke(1.5.dp, tint.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Snooze,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = snoozeLabel,
                color = tint,
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
                    text = stringResource(R.string.firing_exact_snooze),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.firing_exact_snooze_hint),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = stringResource(R.string.firing_minutes_short, minutes),
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
                    contentDescription = stringResource(R.string.firing_decrease_snooze),
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
                    text = stringResource(R.string.firing_snooze_minutes, minutes),
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(
                onClick = { onMinutesChange(minutes + 1) },
                enabled = minutes < MAX_CUSTOM_SNOOZE_MINUTES
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.firing_increase_snooze),
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
                text = stringResource(R.string.firing_minutes_short, minutes),
                fontWeight = FontWeight.SemiBold
            )
            if (isDefault) {
                AppStatusChip(
                    label = stringResource(R.string.firing_default),
                    color = SnoozeYellow
                )
            }
        }
    }
}
