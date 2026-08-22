package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import androidx.compose.ui.platform.LocalResources
import androidx.annotation.StringRes
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.LocalMotionEnabled
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import com.sysadmindoc.alarmclock.R
import androidx.compose.ui.res.stringResource

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
        ChallengeSupportText(stringResource(R.string.challenge_choose_correct_answer_unlock_dismiss))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
                        // Resolved before the semantics lambda, which is not a
                        // composable scope.
                        val answerLabel =
                            stringResource(R.string.challenge_answer_choice, choice)
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
                                .semantics { contentDescription = answerLabel },
                            shape = RoundedCornerShape(10.dp),
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
                text = stringResource(R.string.challenge_one_was_off_try_next),
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

    val shakeOffset = if (LocalMotionEnabled.current) {
        rememberInfiniteTransition(label = "shake").animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shakeAnim"
        ).value
    } else {
        0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_strong_shake_helps_break_sleepy))

        ChallengeProgressHero(
            icon = Icons.Default.PhoneAndroid,
            accent = AccentBlue,
            progress = progress,
            statusLabel = stringResource(R.string.alarmfiring_complete, currentShakes, challenge.requiredShakes),
            summary = if (currentShakes == 0) stringResource(R.string.alarmfiring_start_shaking_to_build_momentum) else stringResource(R.string.alarmfiring_shakes_remaining, remaining)
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = stringResource(R.string.challenge_shake_phone),
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
            label = stringResource(R.string.alarmfiring_of_tapped, tappedIndices.size, challenge.numbers.size),
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
                            shape = RoundedCornerShape(10.dp),
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
                MemoryPhase.SHOWING -> stringResource(R.string.alarmfiring_watch)
                MemoryPhase.INPUT -> stringResource(R.string.share_import_repeat)
                MemoryPhase.WRONG -> stringResource(R.string.dashboard_retry)
            },
            color = when (phase) {
                MemoryPhase.SHOWING -> SnoozeYellow
                MemoryPhase.INPUT -> AccentBlue
                MemoryPhase.WRONG -> AccentRed
            }
        )

        ChallengeSupportText(
            text = when (phase) {
                MemoryPhase.SHOWING -> stringResource(R.string.alarmfiring_memorize_the_highlighted_tiles_before_the)
                MemoryPhase.INPUT -> stringResource(R.string.alarmfiring_tap_the_same_tiles_in_the)
                MemoryPhase.WRONG -> stringResource(R.string.alarmfiring_the_pattern_will_show_again_in)
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
                                .clip(RoundedCornerShape(10.dp))
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
                label = stringResource(R.string.alarmfiring_of_matched, tappedIndices.size, challenge.pattern.size),
                color = AccentBlue
            )
        }
    }
}

enum class MemoryPhase { SHOWING, INPUT, WRONG }

// v1.6.0: Emoji memory challenge phases
enum class EmojiMemoryPhase { REVEALING, INPUT }

// v1.6.0: Rock-paper-scissors support types
enum class RpsChoice(val emoji: String, val label: String) {
    ROCK("\uD83E\uDEA8", "Rock"),
    PAPER("\uD83D\uDCC4", "Paper"),
    SCISSORS("\u2702\uFE0F", "Scissors")
}

enum class RpsOutcome { WIN, LOSE, DRAW }

data class RpsRoundResult(
    val playerChoice: RpsChoice,
    val computerChoice: RpsChoice,
    val outcome: RpsOutcome
)

// v1.6.0: Wordle letter evaluation state
enum class WordleLetterState { CORRECT, PRESENT, ABSENT }

fun computeWordleTileStates(guess: String, target: String): List<WordleLetterState> {
    val result = MutableList(5) { WordleLetterState.ABSENT }
    val targetCounts = target.groupingBy { it }.eachCount().toMutableMap()
    // First pass: mark correct (exact position match)
    for (i in 0 until 5) {
        if (i < guess.length && guess[i] == target[i]) {
            result[i] = WordleLetterState.CORRECT
            targetCounts[guess[i]] = (targetCounts[guess[i]] ?: 1) - 1
        }
    }
    // Second pass: mark present (letter in word but wrong position)
    for (i in 0 until 5) {
        if (result[i] == WordleLetterState.CORRECT) continue
        if (i >= guess.length) continue
        val remaining = targetCounts[guess[i]] ?: 0
        if (remaining > 0) {
            result[i] = WordleLetterState.PRESENT
            targetCounts[guess[i]] = remaining - 1
        }
    }
    return result
}

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
        ChallengeSupportText(stringResource(R.string.challenge_type_same_words_punctuation_letter))

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.padding(18.dp)
            )
        }

        OutlinedTextField(
            value = currentInput,
            onValueChange = onInputChanged,
            placeholder = { Text(stringResource(R.string.challenge_type_phrase_above), color = TextMuted) },
            colors = appOutlinedTextFieldColors(),
            shape = AppInputShape,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        if (wrongAttempts > 0) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_not_quite_match_phrase_exactly),
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
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.challenge_check_phrase), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VoicePhraseChallengeView(
    challenge: Challenge.VoicePhraseChallenge,
    transcript: String,
    status: String,
    wrongAttempts: Int,
    onRecognized: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // The recogniser listener below is a plain callback, so it needs its own handle.
    val resources = LocalResources.current
    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isListening by remember { mutableStateOf(false) }
    var localStatus by remember(challenge.phrase) { mutableStateOf("") }
    var typedFallback by remember(challenge.phrase) { mutableStateOf("") }
    var wrongFlash by remember { mutableStateOf(false) }
    val latestOnRecognized by rememberUpdatedState(onRecognized)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
        localStatus = if (granted) {
            "Microphone ready. Start listening again."
        } else {
            "Microphone permission was denied. Type the phrase below to finish."
        }
    }

    val speechRecognizer = remember(speechAvailable) {
        if (speechAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val listenIntent = remember(challenge.phrase) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, challenge.phrase)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    DisposableEffect(speechRecognizer, listenIntent) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                localStatus = "Listening. Say the phrase shown below."
            }

            override fun onBeginningOfSpeech() {
                localStatus = "Speech detected."
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                localStatus = "Checking what was heard..."
            }

            override fun onError(error: Int) {
                isListening = false
                localStatus = resources.getString(speechErrorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val recognized = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                latestOnRecognized(recognized)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    localStatus = "Hearing: $partial"
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            runCatching { speechRecognizer?.cancel() }
            runCatching { speechRecognizer?.destroy() }
        }
    }

    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts > 0) {
            wrongFlash = true
            delay(400)
            wrongFlash = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_say_phrase_clearly_offline_recognition)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (wrongFlash) AccentRed.copy(alpha = 0.15f) else SurfaceCard
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = challenge.phrase,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(18.dp)
            )
        }

        Button(
            onClick = {
                when {
                    !speechAvailable -> {
                        localStatus = "Android speech recognition is unavailable here. Type the phrase below."
                    }
                    !hasRecordPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    else -> {
                        runCatching {
                            isListening = true
                            localStatus = "Starting microphone..."
                            speechRecognizer?.startListening(listenIntent)
                        }.onFailure {
                            isListening = false
                            localStatus = "Could not start speech recognition. Type the phrase below."
                        }
                    }
                }
            },
            enabled = !isListening,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (isListening) stringResource(R.string.alarmfiring_listening) else stringResource(R.string.alarmfiring_start_listening),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isListening) {
            OutlinedButton(
                onClick = {
                    runCatching { speechRecognizer?.stopListening() }
                    isListening = false
                    localStatus = "Stopped listening."
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.challenge_stop_listening))
            }
        }

        val visibleStatus = status.ifBlank { localStatus }
        if (visibleStatus.isNotBlank()) {
            ChallengeNotice(
                text = visibleStatus,
                accent = if (status.startsWith("Heard") || status.startsWith("No phrase")) {
                    AccentRed
                } else {
                    SnoozeYellow
                },
                icon = Icons.Default.WarningAmber
            )
        } else if (transcript.isNotBlank()) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_heard_transcript, transcript),
                accent = TextSecondary,
                icon = Icons.Default.PhoneAndroid
            )
        }

        OutlinedTextField(
            value = typedFallback,
            onValueChange = { typedFallback = it },
            placeholder = { Text(stringResource(R.string.challenge_typed_fallback_phrase), color = TextMuted) },
            colors = appOutlinedTextFieldColors(),
            shape = AppInputShape,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (typedFallback.isNotBlank()) onRecognized(typedFallback)
            })
        )

        OutlinedButton(
            onClick = { onRecognized(typedFallback) },
            enabled = typedFallback.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.challenge_check_typed_phrase))
        }
    }
}

@StringRes
private fun speechErrorMessage(error: Int): Int = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> R.string.alarmfiring_the_microphone_had_an_audio_error
    SpeechRecognizer.ERROR_CLIENT -> R.string.alarmfiring_speech_recognition_stopped_try_again_or
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
        R.string.alarmfiring_microphone_permission_is_missing_grant_it
    SpeechRecognizer.ERROR_NETWORK -> R.string.alarmfiring_speech_recognition_network_error_offline_fallback
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.alarmfiring_speech_recognition_timed_out_try_again
    SpeechRecognizer.ERROR_NO_MATCH -> R.string.alarmfiring_no_matching_speech_was_detected_say
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.alarmfiring_speech_recognition_is_busy_wait_a
    SpeechRecognizer.ERROR_SERVER -> R.string.alarmfiring_speech_service_error_type_the_phrase
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.alarmfiring_no_speech_was_heard_try_again
    else -> R.string.alarmfiring_speech_recognition_failed_try_again_or
}

@Composable
fun HandwritingChallengeView(
    challenge: Challenge.HandwritingChallenge,
    status: String,
    busy: Boolean,
    wrongAttempts: Int,
    onRecognize: (List<InkStroke>, Float, Float) -> Unit,
    onTypedFallback: (String) -> Unit
) {
    var strokes by remember(challenge.targetText) { mutableStateOf<List<InkStroke>>(emptyList()) }
    var currentStroke by remember(challenge.targetText) { mutableStateOf<List<InkPoint>>(emptyList()) }
    var typedFallback by remember(challenge.targetText) { mutableStateOf("") }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_draw_word_box_recognition_runs)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (wrongFlash) AccentRed.copy(alpha = 0.15f) else SurfaceCard
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = challenge.targetText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            )
        }

        // Resolved outside the semantics lambda, which is not composable.
        val padLabel = stringResource(R.string.challenge_drawing_pad_for, challenge.targetText)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark.copy(alpha = 0.78f))
                .semantics { contentDescription = padLabel }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(challenge.targetText, busy) {
                        if (busy) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(
                                    InkPoint(offset.x, offset.y, SystemClock.uptimeMillis())
                                )
                            },
                            onDragEnd = {
                                if (currentStroke.size >= 2) {
                                    strokes = strokes + InkStroke(currentStroke)
                                }
                                currentStroke = emptyList()
                            },
                            onDragCancel = { currentStroke = emptyList() },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + InkPoint(
                                    x = change.position.x,
                                    y = change.position.y,
                                    timestampMillis = SystemClock.uptimeMillis()
                                )
                            }
                        )
                    }
            ) {
                fun drawInk(stroke: InkStroke, alpha: Float) {
                    val points = stroke.points
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
                    }
                    drawPath(
                        path = path,
                        color = AccentBlue.copy(alpha = alpha),
                        style = Stroke(
                            width = 8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                strokes.forEach { drawInk(it, alpha = 0.95f) }
                if (currentStroke.isNotEmpty()) {
                    drawInk(InkStroke(currentStroke), alpha = 0.72f)
                }
            }

            if (strokes.isEmpty() && currentStroke.isEmpty()) {
                Text(
                    text = stringResource(R.string.challenge_write_target, challenge.targetText),
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    strokes = emptyList()
                    currentStroke = emptyList()
                },
                enabled = !busy && (strokes.isNotEmpty() || currentStroke.isNotEmpty()),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.alarm_edit_clear_short))
            }
            Button(
                onClick = {
                    onRecognize(
                        strokes,
                        canvasSize.width.toFloat(),
                        canvasSize.height.toFloat()
                    )
                },
                enabled = !busy && strokes.isNotEmpty() && canvasSize != IntSize.Zero,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (busy) stringResource(R.string.alarmfiring_checking) else stringResource(R.string.alarmfiring_check_drawing))
            }
        }

        if (busy) {
            CircularProgressIndicator(color = AccentBlue)
        }

        if (status.isNotBlank()) {
            ChallengeNotice(
                text = status,
                accent = when {
                    status.endsWith("matched.") -> DismissGreen
                    status.startsWith("Checking") -> SnoozeYellow
                    else -> AccentRed
                },
                icon = Icons.Default.WarningAmber
            )
        }

        OutlinedTextField(
            value = typedFallback,
            onValueChange = { typedFallback = it },
            placeholder = { Text(stringResource(R.string.challenge_typed_fallback_word), color = TextMuted) },
            colors = appOutlinedTextFieldColors(),
            shape = AppInputShape,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (typedFallback.isNotBlank()) onTypedFallback(typedFallback)
            })
        )

        OutlinedButton(
            onClick = { onTypedFallback(typedFallback) },
            enabled = typedFallback.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.challenge_check_typed_word))
        }
    }
}

@Composable
fun WalkChallengeView(
    challenge: Challenge.WalkChallenge,
    currentSteps: Int,
    walkStatus: String,
    fallbackAllowed: Boolean,
    onContinueWithoutSensor: () -> Unit
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
        ChallengeSupportText(stringResource(R.string.challenge_walking_few_steps_helps_make))

        ChallengeProgressHero(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            accent = DismissGreen,
            progress = progress,
            statusLabel = stringResource(R.string.alarmfiring_steps, currentSteps, challenge.requiredSteps),
            summary = if (currentSteps == 0) stringResource(R.string.alarmfiring_start_walking_to_build_progress) else stringResource(R.string.alarmfiring_steps_remaining, remaining)
        )

        if (walkStatus.isNotBlank()) {
            ChallengeNotice(
                text = walkStatus,
                accent = SnoozeYellow,
                icon = Icons.Default.WarningAmber
            )
        }

        if (fallbackAllowed) {
            OutlinedButton(
                onClick = onContinueWithoutSensor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.challenge_continue_without_step_count))
            }
        }
    }
}

@Composable
fun NfcScanChallengeView(
    challenge: Challenge.NfcChallenge,
    scanStatus: String
) {
    val pulseAlpha = if (LocalMotionEnabled.current) {
        rememberInfiniteTransition(label = "nfcPulse").animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "nfcAlpha"
        ).value
    } else {
        1f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_tap_saved_tag_against_back))

        ChallengeIconPanel(accent = AccentBlue.copy(alpha = 0.12f)) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = stringResource(R.string.alarm_edit_challenge_nfc),
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
                text = stringResource(R.string.challenge_no_tag_registered_yet_any),
                accent = SnoozeYellow,
                icon = Icons.Default.Nfc
            )
        }
    }
}

@Composable
fun BarcodeScanChallengeView(
    challenge: Challenge.BarcodeChallenge,
    scanStatus: String,
    onCodeEntered: (String) -> Unit
) {
    var codeInput by remember(challenge.registeredValue) { mutableStateOf("") }
    val lineProgress = if (LocalMotionEnabled.current) {
        rememberInfiniteTransition(label = "barcodeScan").animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
            label = "scanLine"
        ).value
    } else {
        1f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_enter_saved_barcode_qr_payload))

        ChallengeIconPanel(accent = AccentBlue.copy(alpha = 0.12f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = stringResource(R.string.alarm_edit_challenge_barcode),
                    tint = AccentBlue,
                    modifier = Modifier.size(72.dp)
                )
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentBlue.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(lineProgress.coerceIn(0.18f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                text = stringResource(R.string.challenge_no_code_registered_yet_continue),
                accent = SnoozeYellow,
                icon = Icons.Default.QrCodeScanner
            )
            Button(
                onClick = { onCodeEntered("") },
                colors = ButtonDefaults.buttonColors(containerColor = DismissGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.challenge_continue_without_saved_code))
            }
        } else {
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text(stringResource(R.string.alarm_edit_barcode_value)) },
                singleLine = true,
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val trimmed = codeInput.trim()
                        if (trimmed.isNotEmpty()) onCodeEntered(trimmed)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onCodeEntered(codeInput.trim()) },
                enabled = codeInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DismissGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.challenge_submit_code))
            }
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
        ChallengeSupportText(stringResource(R.string.challenge_take_fresh_photo_saved_location))

        ChallengeIconPanel(accent = AccentBlue.copy(alpha = 0.12f)) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.challenge_take_photo),
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
                text = stringResource(R.string.challenge_no_reference_photo_registered_yet),
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
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = stringResource(R.string.challenge_open_camera), modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.challenge_open_camera),
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
    currentSquats: Int,
    exerciseStatus: String = "",
    fallbackAllowed: Boolean = false,
    onContinueWithoutSensor: () -> Unit = {}
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
        ChallengeSupportText(stringResource(R.string.challenge_short_movement_burst_makes_harder))

        ChallengeProgressHero(
            icon = Icons.Default.FitnessCenter,
            accent = DismissGreen,
            progress = progress,
            statusLabel = stringResource(R.string.alarmfiring_squats, currentSquats, challenge.requiredSquats),
            summary = if (currentSquats == 0) stringResource(R.string.alarmfiring_start_with_one_clean_squat) else stringResource(R.string.alarmfiring_squats_remaining, remaining)
        )

        ExerciseSensorFallback(exerciseStatus, fallbackAllowed, "Continue without squat count", onContinueWithoutSensor)
    }
}

@Composable
fun PushUpChallengeView(
    challenge: Challenge.PushUpChallenge,
    currentPushUps: Int,
    exerciseStatus: String = "",
    fallbackAllowed: Boolean = false,
    onContinueWithoutSensor: () -> Unit = {}
) {
    val progress = (currentPushUps.toFloat() / challenge.requiredPushUps).coerceIn(0f, 1f)
    val remaining = (challenge.requiredPushUps - currentPushUps).coerceAtLeast(0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_place_phone_face_down_floor))

        ChallengeProgressHero(
            icon = Icons.Default.FitnessCenter,
            accent = AccentRed,
            progress = progress,
            statusLabel = stringResource(R.string.alarmfiring_push_ups, currentPushUps, challenge.requiredPushUps),
            summary = if (currentPushUps == 0) stringResource(R.string.alarmfiring_start_your_first_push_up) else stringResource(R.string.alarmfiring_push_ups_remaining, remaining)
        )

        ExerciseSensorFallback(exerciseStatus, fallbackAllowed, "Continue without push-up count", onContinueWithoutSensor)
    }
}

/** Shared "no motion sensor" notice + escape hatch for squat/push-up challenges. */
@Composable
private fun ExerciseSensorFallback(
    status: String,
    fallbackAllowed: Boolean,
    buttonLabel: String,
    onContinueWithoutSensor: () -> Unit
) {
    if (status.isNotBlank()) {
        ChallengeNotice(
            text = status,
            accent = SnoozeYellow,
            icon = Icons.Default.WarningAmber
        )
    }
    if (fallbackAllowed) {
        OutlinedButton(
            onClick = onContinueWithoutSensor,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(buttonLabel)
        }
    }
}

@Composable
fun PlankHoldChallengeView(
    challenge: Challenge.PlankHoldChallenge,
    heldSeconds: Int,
    isActive: Boolean,
    onStart: () -> Unit,
    onBreak: () -> Unit
) {
    val progress = (heldSeconds.toFloat() / challenge.requiredSeconds).coerceIn(0f, 1f)
    val remaining = (challenge.requiredSeconds - heldSeconds).coerceAtLeast(0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_hold_phone_level_face_down))

        ChallengeProgressHero(
            icon = Icons.Default.FitnessCenter,
            accent = AccentBlue,
            progress = progress,
            statusLabel = stringResource(R.string.alarmfiring_seconds, heldSeconds, challenge.requiredSeconds),
            summary = if (heldSeconds == 0) stringResource(R.string.alarmfiring_tap_start_and_hold_position) else stringResource(R.string.alarmfiring_seconds_remaining, remaining)
        )

        if (!isActive) {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(stringResource(R.string.challenge_start_plank))
            }
        } else {
            OutlinedButton(
                onClick = onBreak,
                border = BorderStroke(1.dp, AccentRed)
            ) {
                Text(stringResource(R.string.challenge_i_broke_form), color = AccentRed)
            }
            Text(
                text = stringResource(R.string.challenge_timer_running_hold_steady),
                color = AccentBlue,
                style = MaterialTheme.typography.bodySmall
            )
        }
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
        ChallengeSupportText(stringResource(R.string.challenge_tap_adjacent_cells_only_reach))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppStatusChip(label = stringResource(R.string.challenge_you), icon = Icons.Default.Person, color = AccentBlue)
            AppStatusChip(label = stringResource(R.string.challenge_start), color = DismissGreen)
            AppStatusChip(label = stringResource(R.string.challenge_exit), color = AccentRed)
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
                                isCurrent -> Icon(Icons.Default.Person, contentDescription = stringResource(R.string.challenge_position), tint = TextPrimary, modifier = Modifier.size(22.dp))
                                isStart -> Text(stringResource(R.string.challenge_s), color = DismissGreen, fontWeight = FontWeight.Bold)
                                isEnd -> Text(stringResource(R.string.challenge_e), color = AccentRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (invalidFlashIdx >= 0) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_only_adjacent_cells_are_reachable),
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }
    }
}

@Composable
fun WifiChallengeView(
    challenge: Challenge.WifiChallenge,
    currentSsid: String,
    wifiStatus: String
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
        ChallengeSupportText(stringResource(R.string.challenge_reconnect_planned_wi_fi_network))

        ChallengeIconPanel(
            accent = if (isConnected) DismissGreen.copy(alpha = 0.12f) else AccentBlue.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = stringResource(R.string.challenge_wi_fi),
                tint = if (isConnected) DismissGreen else AccentBlue,
                modifier = Modifier.size(76.dp)
            )
        }

        if (challenge.requiredSsid.isNotBlank()) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_required_network, challenge.requiredSsid),
                accent = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Wifi
            )
        }

        ChallengeNotice(
            text = if (currentSsid.isBlank()) stringResource(R.string.alarmfiring_not_connected_to_wi_fi_yet) else stringResource(R.string.alarmfiring_connected_to, currentSsid),
            accent = if (isConnected) DismissGreen else SnoozeYellow,
            icon = Icons.Default.Wifi
        )

        if (challenge.requiredSsid.isBlank()) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_no_network_specified_yet_any),
                accent = SnoozeYellow,
                icon = Icons.Default.Wifi
            )
        }

        if (wifiStatus.isNotBlank()) {
            ChallengeNotice(
                text = wifiStatus,
                accent = SnoozeYellow,
                icon = Icons.Default.WarningAmber
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
        shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.18f),
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        // Announce progress to TalkBack so non-visual users hear each increment
        // (rep counts, held seconds, steps) without watching the ring.
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            stateDescription = statusLabel
        }
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
                text = stringResource(R.string.alarmfiring_complete_2, (progress * 100).toInt()),
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
    val drift = if (LocalMotionEnabled.current) {
        rememberInfiniteTransition(label = "sheep-drift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sheep-drift-value"
        ).value
    } else {
        0f
    }

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
                .clip(RoundedCornerShape(12.dp))
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
                        .clip(RoundedCornerShape(12.dp))
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
                label = stringResource(
                    R.string.challenge_sheep_progress,
                    tapped,
                    challenge.targetCount
                ),
                color = if (progress >= 1f) DismissGreen else AccentBlue
            )
            if (wrongTaps > 0) {
                AppStatusChip(
                    label = stringResource(R.string.challenge_sheep_missed, wrongTaps),
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
        AccentRed,
        DismissGreen,
        AccentBlue,
        SnoozeYellow
    )
    val names = listOf("Red", "Green", "Blue", "Yellow")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(
            text = stringResource(R.string.challenge_watch_sequence_then_tap_back),
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
                        val padDescription = stringResource(R.string.alarmfiring_pad, names[idx])
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors[idx].copy(alpha = alpha))
                                .clickable(enabled = playingIndex < 0) { onPadTap(idx) }
                                .semantics { contentDescription = padDescription },
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
            label = stringResource(R.string.alarmfiring_correct, inputIndices.size, challenge.sequence.size),
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
            placeholder = { Text(stringResource(R.string.challenge_type_reversed_date), color = TextMuted) },
            singleLine = true,
            colors = appOutlinedTextFieldColors(),
            shape = AppInputShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSubmit,
            enabled = input.length == challenge.expectedInput.length,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.challenge_check_date))
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
        AccentRed,
        DismissGreen,
        AccentBlue,
        SnoozeYellow
    )
    val names = listOf("Red", "Green", "Blue", "Yellow")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_tap_ink_color_word_not))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
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
                val choiceDescription = stringResource(R.string.alarmfiring_choice, names[idx])
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette[idx])
                        .clickable { onPick(idx) }
                        .semantics { contentDescription = choiceDescription }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// v1.6.0: Rock-Paper-Scissors
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RockPaperScissorsChallengeView(
    challenge: Challenge.RockPaperScissorsChallenge,
    playerWins: Int,
    computerWins: Int,
    rounds: List<RpsRoundResult>,
    onPick: (RpsChoice) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.challenge_rps_you, playerWins),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DismissGreen
            )
            Text(
                text = "  —  ",
                style = MaterialTheme.typography.headlineSmall,
                color = TextMuted
            )
            Text(
                text = stringResource(R.string.challenge_rps_cpu, computerWins),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AccentRed
            )
        }

        val lastRound = rounds.lastOrNull()
        if (lastRound != null) {
            val outcomeText = when (lastRound.outcome) {
                RpsOutcome.WIN  -> stringResource(R.string.challenge_rps_you_won)
                RpsOutcome.LOSE -> stringResource(R.string.challenge_rps_computer_won)
                RpsOutcome.DRAW -> stringResource(R.string.challenge_rps_draw)
            }
            val outcomeColor = when (lastRound.outcome) {
                RpsOutcome.WIN  -> DismissGreen
                RpsOutcome.LOSE -> AccentRed
                RpsOutcome.DRAW -> SnoozeYellow
            }
            ChallengeNotice(
                text = "${lastRound.playerChoice.emoji} vs ${lastRound.computerChoice.emoji} \u2014 $outcomeText",
                accent = outcomeColor,
                icon = Icons.Default.WarningAmber
            )
        }

        ChallengeSupportText("First to ${challenge.requiredWins} wins. Pick your move:")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RpsChoice.entries.forEach { choice ->
                OutlinedButton(
                    onClick = { onPick(choice) },
                    modifier = Modifier.weight(1f).height(72.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = choice.emoji, fontSize = 24.sp)
                        Text(
                            text = choice.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// v1.6.0: Emoji Memory
// ─────────────────────────────────────────────────────────────────────────────

private val EMOJI_MEMORY_SYMBOLS = listOf(
    "\uD83D\uDC36", // dog
    "\uD83D\uDC31", // cat
    "\uD83D\uDC38", // frog
    "\uD83D\uDC2C", // dolphin
    "\uD83E\uDD8A", // fox
    "\uD83D\uDC27", // penguin
    "\uD83E\uDD81", // lion
    "\uD83D\uDC2E"  // cow
)

@Composable
fun EmojiMemoryChallengeView(
    challenge: Challenge.EmojiMemoryChallenge,
    phase: EmojiMemoryPhase,
    flippedIndices: Set<Int>,
    matchedIndices: Set<Int>,
    onCardFlip: (Int) -> Unit
) {
    var displayCountdown by remember { mutableStateOf((challenge.revealDurationMs / 1000).toInt()) }
    LaunchedEffect(phase) {
        if (phase == EmojiMemoryPhase.REVEALING) {
            displayCountdown = (challenge.revealDurationMs / 1000).toInt()
            while (displayCountdown > 0) {
                delay(1000)
                displayCountdown--
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        when (phase) {
            EmojiMemoryPhase.REVEALING ->
                ChallengeNotice(
                    text = stringResource(R.string.challenge_emoji_memorise, displayCountdown),
                    accent = SnoozeYellow,
                    icon = Icons.Default.WarningAmber
                )
            EmojiMemoryPhase.INPUT -> {
                val pairs = matchedIndices.size / 2
                ChallengeSupportText("$pairs of 8 pairs found. Tap two cards to match them.")
            }
        }

        val cols = 4
        val rows = challenge.cards.size / cols
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until rows).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0 until cols).forEach { col ->
                        val idx = row * cols + col
                        val isFaceUp = phase == EmojiMemoryPhase.REVEALING ||
                                idx in matchedIndices || idx in flippedIndices
                        val isMatched = idx in matchedIndices
                        val bgColor = when {
                            isMatched -> DismissGreen.copy(alpha = 0.25f)
                            isFaceUp  -> SurfaceCard
                            else      -> SurfaceDark
                        }
                        val borderColor = when {
                            isMatched                 -> DismissGreen
                            isFaceUp && !isMatched    -> AccentBlue
                            else                      -> TextMuted.copy(alpha = 0.3f)
                        }
                        Card(
                            modifier = Modifier
                                .size(62.dp)
                                .clickable(
                                    enabled = phase == EmojiMemoryPhase.INPUT &&
                                            !isMatched && idx !in flippedIndices
                                ) { onCardFlip(idx) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Box(
                                modifier = Modifier.size(62.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFaceUp) {
                                    Text(
                                        text = EMOJI_MEMORY_SYMBOLS[challenge.cards[idx]],
                                        fontSize = 26.sp
                                    )
                                } else {
                                    Text(
                                        text = "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// v1.6.0: Typing Speed
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TypingSpeedChallengeView(
    challenge: Challenge.TypingSpeedChallenge,
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        ChallengeSupportText(
            "Type the phrase at ${challenge.minWpm}+ wpm with at most ${challenge.maxErrors} word error(s)."
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u201C${challenge.phrase}\u201D",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (wrongAttempts > 0) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_too_slow_too_many_word),
                accent = if (wrongFlash) AccentRed else AccentRed.copy(alpha = 0.75f),
                icon = Icons.Default.WarningAmber
            )
        }

        OutlinedTextField(
            value = currentInput,
            onValueChange = onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.challenge_type_phrase)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            colors = appOutlinedTextFieldColors(),
            shape = AppInputShape,
            singleLine = false,
            maxLines = 3
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = currentInput.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text(stringResource(R.string.challenge_check_speed), color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// v1.6.0: Wordle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WordleChallengeView(
    challenge: Challenge.WordleChallenge,
    guesses: List<String>,
    currentInput: String,
    gameOver: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        val triesLeft = challenge.maxGuesses - guesses.size
        ChallengeSupportText("Guess the 5-letter word \u2014 $triesLeft tr${if (triesLeft == 1) "y" else stringResource(R.string.alarmfiring_ies)} left.")

        if (gameOver) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_wordle_reveal, challenge.target),
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0 until challenge.maxGuesses).forEach { row ->
                val guess = guesses.getOrNull(row)
                val tileStates = if (guess != null) computeWordleTileStates(guess, challenge.target) else null
                val isCurrentRow = row == guesses.size && !gameOver

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0 until 5).forEach { col ->
                        val guessedCh = guess?.getOrNull(col) ?: ' '
                        val displayCh = if (isCurrentRow) currentInput.getOrNull(col) ?: ' ' else guessedCh
                        val state = tileStates?.getOrNull(col)
                        val bgColor = when (state) {
                            WordleLetterState.CORRECT -> DismissGreen
                            WordleLetterState.PRESENT -> SnoozeYellow
                            WordleLetterState.ABSENT  -> SurfaceCard
                            null -> if (isCurrentRow && displayCh != ' ') AccentBlue.copy(alpha = 0.2f) else SurfaceDark
                        }
                        val borderColor = when {
                            state != null -> Color.Transparent
                            isCurrentRow  -> AccentBlue.copy(alpha = 0.6f)
                            else          -> TextMuted.copy(alpha = 0.3f)
                        }
                        // Letter state is also signalled by color (green/yellow/dim),
                        // so expose it to TalkBack / colorblind users as text instead of
                        // relying on the tile background alone.
                        val tileDescription = when {
                            displayCh == ' ' -> stringResource(R.string.alarmfiring_empty)
                            state == WordleLetterState.CORRECT -> stringResource(R.string.alarmfiring_correct_position, displayCh)
                            state == WordleLetterState.PRESENT -> stringResource(R.string.alarmfiring_in_the_word_wrong_position, displayCh)
                            state == WordleLetterState.ABSENT -> stringResource(R.string.alarmfiring_not_in_the_word, displayCh)
                            else -> "$displayCh"
                        }
                        Card(
                            modifier = Modifier
                                .size(48.dp)
                                .clearAndSetSemantics { contentDescription = tileDescription },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (displayCh != ' ') {
                                    Text(
                                        text = displayCh.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state != null) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!gameOver) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { onInputChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.alarmfiring_5_letter_word)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (currentInput.length == 5) onSubmit() }),
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape,
                singleLine = true
            )
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = currentInput.length == 5,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(stringResource(R.string.challenge_submit_guess), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PvtChallengeView(
    challenge: Challenge.PvtChallenge,
    trialIndex: Int,
    reactionTimes: List<Long>,
    stimulusShown: Boolean,
    waiting: Boolean,
    lastReaction: Long?,
    failed: Boolean,
    onTap: () -> Unit,
    onFalseStart: () -> Unit,
    onStartTrial: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.challenge_reaction_test),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(
                R.string.challenge_pvt_trial,
                trialIndex + 1,
                challenge.totalTrials
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        if (reactionTimes.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.challenge_pvt_average,
                    reactionTimes.average().toLong().toInt()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    color = when {
                        failed -> AccentRed
                        stimulusShown -> DismissGreen
                        waiting -> SurfaceCard
                        else -> SurfaceCard
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable {
                    when {
                        stimulusShown -> onTap()
                        waiting -> onFalseStart()
                        trialIndex < challenge.totalTrials && !failed -> onStartTrial()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    failed -> stringResource(R.string.alarmfiring_too_slow_ntry_again)
                    stimulusShown -> stringResource(R.string.alarmfiring_tap)
                    waiting -> stringResource(R.string.alarmfiring_wait)
                    lastReaction != null && lastReaction >= 0 -> "${lastReaction} ms"
                    lastReaction != null && lastReaction < 0 -> stringResource(R.string.alarmfiring_too_early)
                    else -> stringResource(R.string.alarmfiring_tap_to_start)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = when {
                    stimulusShown -> Color.White
                    failed -> Color.White
                    else -> TextPrimary
                },
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
        Text(stringResource(R.string.challenge_tap_green_square_as_fast),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SpotDifferenceChallengeView(
    challenge: Challenge.SpotDifferenceChallenge,
    wrongAttempts: Int,
    onPick: (Int) -> Unit
) {
    val palette = listOf(
        AccentBlue,
        DismissGreen,
        SnoozeYellow,
        AccentRed,
        MaterialTheme.colorScheme.primary,
        TextSecondary
    )
    val changedTiles = remember(challenge) {
        challenge.baseTiles.toMutableList().also { it[challenge.changedIndex] = challenge.changedTile }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_find_one_tile_changed_right))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DifferenceGrid(
                title = stringResource(R.string.challenge_original),
                modifier = Modifier.weight(1f),
                gridSize = challenge.gridSize,
                tiles = challenge.baseTiles,
                palette = palette,
                enabled = false,
                onPick = {}
            )
            DifferenceGrid(
                title = stringResource(R.string.challenge_changed),
                modifier = Modifier.weight(1f),
                gridSize = challenge.gridSize,
                tiles = changedTiles,
                palette = palette,
                enabled = true,
                onPick = onPick
            )
        }

        if (wrongAttempts > 0) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_tile_matches_look_single_color),
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }
    }
}

@Composable
private fun DifferenceGrid(
    title: String,
    modifier: Modifier = Modifier,
    gridSize: Int,
    tiles: List<Int>,
    palette: List<Color>,
    enabled: Boolean,
    onPick: (Int) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            tiles.chunked(gridSize).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEachIndexed { columnIndex, tile ->
                        val index = rowIndex * gridSize + columnIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(palette[tile % palette.size].copy(alpha = 0.9f))
                                .clickable(enabled = enabled) { onPick(index) }
                                .semantics {
                                    contentDescription = if (enabled) {
                                        "Changed grid tile ${index + 1}"
                                    } else {
                                        "Original grid tile ${index + 1}"
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChessMateChallengeView(
    challenge: Challenge.ChessMateChallenge,
    wrongAttempts: Int,
    onPick: (String) -> Unit
) {
    val puzzle = challenge.puzzle
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppStatusChip(label = stringResource(R.string.alarmfiring_to_move, puzzle.sideToMove), color = AccentBlue)
        Text(
            puzzle.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        ChessBoard(puzzle.board)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            puzzle.choices.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { move ->
                        OutlinedButton(
                            onClick = { onPick(move) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard.copy(alpha = 0.84f),
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(move, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (wrongAttempts > 0) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_not_mate_yet_find_move),
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        } else {
            Text(
                puzzle.explanation,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChessBoard(board: List<String>) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        board.chunked(8).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEachIndexed { columnIndex, piece ->
                    val light = (rowIndex + columnIndex) % 2 == 0
                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .background(
                                if (light) SurfaceCard else SurfaceCard.copy(alpha = 0.52f),
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (piece == ".") "" else piece,
                            color = if (piece.firstOrNull()?.isUpperCase() == true) TextPrimary else AccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RsvpReadingChallengeView(
    challenge: Challenge.RsvpReadingChallenge,
    wrongAttempts: Int,
    onPick: (String) -> Unit
) {
    var wordIndex by remember(challenge) { mutableStateOf(-1) }
    var showChoices by remember(challenge) { mutableStateOf(false) }

    LaunchedEffect(challenge) {
        showChoices = false
        wordIndex = -1
        delay(500)
        challenge.words.indices.forEach { index ->
            wordIndex = index
            delay(challenge.wordIntervalMs)
        }
        wordIndex = -1
        showChoices = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChallengeSupportText(stringResource(R.string.challenge_read_rapid_word_stream_then))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    showChoices -> stringResource(R.string.alarmfiring_which_word_appeared)
                    wordIndex >= 0 -> challenge.words[wordIndex]
                    else -> stringResource(R.string.alarmfiring_get_ready)
                },
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        if (showChoices) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                challenge.choices.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { choice ->
                            OutlinedButton(
                                onClick = { onPick(choice) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceCard.copy(alpha = 0.84f),
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text(choice, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            AppStatusChip(label = stringResource(R.string.challenge_rapid_reading), color = SnoozeYellow)
        }

        if (wrongAttempts > 0) {
            ChallengeNotice(
                text = stringResource(R.string.challenge_word_was_not_stream_pick),
                accent = AccentRed,
                icon = Icons.Default.WarningAmber
            )
        }
    }
}
