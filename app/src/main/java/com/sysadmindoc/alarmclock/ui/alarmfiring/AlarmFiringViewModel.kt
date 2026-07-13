package com.sysadmindoc.alarmclock.ui.alarmfiring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.LocationDismissPolicy
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FiringUiState(
    val alarm: Alarm? = null,
    val challenge: Challenge? = null,
    val challengeSolved: Boolean = false,
    val shakeCount: Int = 0,
    val sequenceTappedIndices: Set<Int> = emptySet(),
    val memoryPhase: MemoryPhase = MemoryPhase.SHOWING,
    val memoryTappedIndices: Set<Int> = emptySet(),
    val wrongAttempts: Int = 0,
    val totalWrongAttempts: Int = 0,
    val challengeStartedAtMillis: Long = 0L,
    // F3: Typing challenge
    val typingInput: String = "",
    // Voice phrase challenge
    val voiceTranscript: String = "",
    val voiceStatus: String = "",
    // Handwriting challenge
    val handwritingStatus: String = "",
    val handwritingBusy: Boolean = false,
    // F4: Walk-steps challenge
    val currentSteps: Int = 0,
    val walkStatus: String = "",
    val walkFallbackAllowed: Boolean = false,
    // F2: NFC scan status
    val nfcScanStatus: String = "",
    // F1: Barcode scan status
    val barcodeScanStatus: String = "",
    // F16: Photo match status
    val photoMatchStatus: String = "",
    // v1.2.0: Mission chaining
    val currentChallengeIndex: Int = 0,
    val totalChallenges: Int = 1,
    // v1.2.0: Squat challenge
    val squatCount: Int = 0,
    // v1.2.0: Motivational quote
    val motivationalQuote: String = "",
    // v1.2.0: Maze challenge
    val mazeCurrentPos: Int = 0,
    // v1.2.0: Wi-Fi challenge
    val wifiCurrentSsid: String = "",
    val wifiStatus: String = "",
    val wifiFallbackAllowed: Boolean = false,
    // v1.4.0: Count-the-Sheep challenge
    val sheepTapped: Int = 0,
    val sheepWrongTaps: Int = 0,
    // v1.5.0: Simon-says state
    val simonPlayingIndex: Int = -1,       // -1 = idle; otherwise the lit pad
    val simonInputIndices: List<Int> = emptyList(),
    val simonErrorFlash: Boolean = false,
    // v1.5.0: Date-backwards input buffer
    val dateBackwardsInput: String = "",
    // v1.6.0: RPS challenge
    val rpsPlayerWins: Int = 0,
    val rpsComputerWins: Int = 0,
    val rpsRounds: List<RpsRoundResult> = emptyList(),
    // v1.6.0: Emoji memory challenge
    val emojiMemoryPhase: EmojiMemoryPhase = EmojiMemoryPhase.REVEALING,
    val emojiFlippedIndices: Set<Int> = emptySet(),
    val emojiMatchedIndices: Set<Int> = emptySet(),
    // v1.6.0: Typing speed challenge
    val typingSpeedInput: String = "",
    val typingSpeedStartTime: Long = 0L,
    // v1.6.0: Wordle challenge
    val wordleGuesses: List<String> = emptyList(),
    val wordleCurrentInput: String = "",
    val wordleGameOver: Boolean = false,
    // PVT challenge
    val pvtTrialIndex: Int = 0,
    val pvtReactionTimes: List<Long> = emptyList(),
    val pvtStimulusShown: Boolean = false,
    val pvtStimulusShownAt: Long = 0L,
    val pvtWaiting: Boolean = false,
    val pvtLastReaction: Long? = null,
    val pvtFailed: Boolean = false,
    // v1.15.0: Push-up challenge
    val pushUpCount: Int = 0,
    // v1.15.0: Plank hold challenge
    val plankHoldSeconds: Int = 0,
    val plankHoldActive: Boolean = false,
    // Sensor-backed exercise (squat/push-up) fallback when no accelerometer is present.
    val exerciseStatus: String = "",
    val exerciseFallbackAllowed: Boolean = false,
    val challengeBypassAvailable: Boolean = false,
    val challengeBypassRemainingSeconds: Int = -1,
    val locationDismissReady: Boolean = false,
    val locationDismissDistanceMeters: Float? = null,
    val locationDismissStatus: String = "",
    val weatherTemp: String? = null,
    val weatherDescription: String? = null,
    val firedEarlyForWeather: Boolean = false
) {
    val requiresLocationDismiss: Boolean get() = alarm?.locationDismissEnabled == true
    val requiresChallenge: Boolean get() {
        val type = alarm?.challengeType ?: "NONE"
        return challenge != null ||
            alarm?.challengeChain?.isNotBlank() == true ||
            type != "NONE" ||
            requiresLocationDismiss
    }
    val wakeChallengeReady: Boolean get() = challengeSolved || challenge == null || challengeBypassAvailable
    val canDismiss: Boolean get() {
        val locationReady = !requiresLocationDismiss || locationDismissReady || challengeBypassAvailable
        return wakeChallengeReady && locationReady
    }
}

@HiltViewModel
class AlarmFiringViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val eventRepository: com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository,
    private val preferencesManager: com.sysadmindoc.alarmclock.data.preferences.PreferencesManager,
    private val weatherRepository: com.sysadmindoc.alarmclock.data.repository.WeatherRepository,
    private val digitalInkChallengeRecognizer: DigitalInkChallengeRecognizer
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>(AlarmScheduler.EXTRA_ALARM_ID) ?: -1

    private val _uiState = MutableStateFlow(FiringUiState())
    val uiState: StateFlow<FiringUiState> = _uiState.asStateFlow()

    /**
     * v1.5.1: One-shot signal that the firing activity should close itself
     * (alarm row disappeared between schedule and fire — DB corruption,
     * manual row delete, etc.). Without this the activity would render a
     * blank firing screen with no user feedback and no way to clear it.
     */
    private val _finish = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finishEvents: SharedFlow<Unit> = _finish.asSharedFlow()

    /** Exposes the global flip-to-snooze preference so the Activity only registers
     *  the orientation listener when the user has actually opted in. */
    val flipToSnoozeEnabled: StateFlow<Boolean> = preferencesManager.settings
        .map { it.flipToSnoozeEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Whether the user has opted into motivational quotes on the firing screen. */
    val showMotivationalQuotes: StateFlow<Boolean> = preferencesManager.settings
        .map { it.showMotivationalQuotes }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** v1.4.0: Whether "cover the phone" snooze is enabled globally. */
    val coverToSnoozeEnabled: StateFlow<Boolean> = preferencesManager.settings
        .map { it.coverToSnoozeEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val firingControlMode: StateFlow<String> = preferencesManager.settings
        .map { it.firingControlMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "hybrid")

    // v1.2.0: Challenge chain list built from alarm config
    private var challengeChainTypes: List<ChallengeType> = emptyList()
    private var currentAlarm: Alarm? = null

    init {
        loadAlarm()
    }

    private fun loadAlarm() {
        viewModelScope.launch {
            // v1.5.1: Signal the activity to finish if the row disappeared
            // between schedule and fire. Also sanitise the row so corrupt
            // challengeType / vibrationPattern don't make it into the UI.
            val alarm = repository.getById(alarmId)?.sanitized() ?: run {
                _finish.tryEmit(Unit)
                return@launch
            }
            currentAlarm = alarm
            val challengeType = try {
                ChallengeType.valueOf(alarm.challengeType)
            } catch (_: Exception) {
                ChallengeType.NONE
            }

            // v1.2.0: Build challenge chain or single challenge
            val chainTypes = if (alarm.challengeChain.isNotBlank()) {
                alarm.challengeChain.split(",").mapNotNull { name ->
                    try { ChallengeType.valueOf(name.trim()) } catch (_: Exception) { null }
                }
            } else if (challengeType != ChallengeType.NONE) {
                listOf(challengeType)
            } else {
                emptyList()
            }
            // v1.2.0: Adaptive difficulty — escalate if user snoozes a lot
            // Read recent events to decide if we should bump difficulty
            val adaptiveDifficultyEnabled = preferencesManager.getCurrentSettings().adaptiveDifficultyEnabled
            val adaptedChain = if (adaptiveDifficultyEnabled && chainTypes.isNotEmpty()) {
                try {
                    val recentStats = eventRepository.getStats()
                    if (recentStats.snoozeRate > 50) {
                        chainTypes.map { type ->
                            when (type) {
                                ChallengeType.MATH_EASY -> ChallengeType.MATH_MEDIUM
                                ChallengeType.MATH_MEDIUM -> ChallengeType.MATH_HARD
                                else -> type
                            }
                        }
                    } else chainTypes
                } catch (_: Exception) { chainTypes }
            } else chainTypes
            challengeChainTypes = adaptedChain

            val firstChallenge = if (adaptedChain.isNotEmpty()) {
                buildChallengeForType(adaptedChain[0], alarm)
            } else {
                null
            }
            val hasLocationDismissTarget = LocationDismissPolicy.hasTarget(
                alarm.locationDismissLat,
                alarm.locationDismissLng
            )
            val locationDismissActive = alarm.locationDismissEnabled && hasLocationDismissTarget

            val quote = MOTIVATIONAL_QUOTES.random()

            val weatherSettings = preferencesManager.getCachedSettings()
            val haveLocation = weatherSettings.lastKnownLatitude != 0.0 || weatherSettings.lastKnownLongitude != 0.0
            val cached = weatherRepository.getCachedWeather(
                latitude = weatherSettings.lastKnownLatitude.takeIf { haveLocation },
                longitude = weatherSettings.lastKnownLongitude.takeIf { haveLocation }
            )
            val weatherTemp = cached?.current?.temperature?.let { temp ->
                val unit = cached.currentUnits?.temperature ?: ""
                "${temp.toInt()}$unit"
            }
            val weatherDesc = cached?.current?.weatherCode?.let { code ->
                com.sysadmindoc.alarmclock.data.remote.WeatherCodes.describe(code)
            }

            _uiState.value = FiringUiState(
                alarm = alarm,
                challenge = firstChallenge,
                challengeSolved = adaptedChain.isEmpty(),
                challengeStartedAtMillis = if (firstChallenge != null) System.currentTimeMillis() else 0L,
                totalChallenges = maxOf(adaptedChain.size, 1),
                currentChallengeIndex = 0,
                motivationalQuote = quote,
                mazeCurrentPos = (firstChallenge as? Challenge.MazeChallenge)?.startPos ?: 0,
                locationDismissReady = !locationDismissActive,
                locationDismissStatus = when {
                    !alarm.locationDismissEnabled -> ""
                    !hasLocationDismissTarget -> "No saved place is set, so location dismissal is not locked."
                    else -> "Waiting for a location fix. Leave the saved ${LocationDismissPolicy.coerceRadius(alarm.locationDismissRadius)} m area to unlock dismiss."
                },
                weatherTemp = weatherTemp,
                weatherDescription = weatherDesc,
                firedEarlyForWeather = alarm.weatherEarlyMinutes > 0 && weatherDesc != null &&
                    com.sysadmindoc.alarmclock.domain.AlarmScheduler.isSnowOrIceCode(
                        cached?.current?.weatherCode ?: 0
                    )
            )
            // Start Simon sequence playback when Simon is the very first challenge.
            if (firstChallenge is Challenge.SimonSaysChallenge) {
                playSimonSequence(firstChallenge)
            }
            // Start emoji reveal when emoji memory is the very first challenge.
            if (firstChallenge is Challenge.EmojiMemoryChallenge) {
                startEmojiReveal(firstChallenge)
            }
            if (firstChallenge != null) {
                launchChallengeBypassTimer()
            } else if (locationDismissActive) {
                launchChallengeBypassTimer()
            }
        }
    }

    private fun launchChallengeBypassTimer() {
        viewModelScope.launch {
            val settings = preferencesManager.getCachedSettings()
            if (!settings.challengeBypassEnabled) return@launch
            val delaySec = settings.challengeBypassDelaySeconds
            for (remaining in delaySec downTo 1) {
                _uiState.value = _uiState.value.copy(challengeBypassRemainingSeconds = remaining)
                kotlinx.coroutines.delay(1000)
                if (_uiState.value.canDismiss) return@launch
            }
            _uiState.value = _uiState.value.copy(
                challengeBypassAvailable = true,
                challengeBypassRemainingSeconds = 0
            )
        }
    }

    private fun buildChallengeForType(type: ChallengeType, alarm: Alarm): Challenge? = when (type) {
        ChallengeType.NONE -> null
        ChallengeType.WALK_STEPS -> Challenge.WalkChallenge(requiredSteps = alarm.walkStepsRequired)
        ChallengeType.NFC_SCAN -> Challenge.NfcChallenge(registeredTagId = alarm.nfcTagId)
        ChallengeType.BARCODE_SCAN -> Challenge.BarcodeChallenge(registeredValue = alarm.barcodeValue)
        ChallengeType.PHOTO_MATCH -> Challenge.PhotoMatchChallenge(referencePhotoUri = alarm.photoMatchUri)
        ChallengeType.SQUAT -> Challenge.SquatChallenge(requiredSquats = alarm.requiredSquats)
        ChallengeType.PUSH_UP -> Challenge.PushUpChallenge(requiredPushUps = 10)
        ChallengeType.PLANK_HOLD -> Challenge.PlankHoldChallenge(requiredSeconds = 30)
        else -> ChallengeGenerator.generate(type)
    }

    fun proceedToNextChallenge() {
        val nextIndex = _uiState.value.currentChallengeIndex + 1
        val alarm = currentAlarm ?: return
        if (nextIndex >= challengeChainTypes.size) {
            // All challenges complete
            _uiState.value = _uiState.value.copy(challengeSolved = true)
            return
        }
        val nextChallenge = buildChallengeForType(challengeChainTypes[nextIndex], alarm)
        _uiState.value = _uiState.value.copy(
            currentChallengeIndex = nextIndex,
            challenge = nextChallenge,
            challengeSolved = false,
            shakeCount = 0,
            squatCount = 0,
            pushUpCount = 0,
            plankHoldSeconds = 0,
            plankHoldActive = false,
            exerciseStatus = "",
            exerciseFallbackAllowed = false,
            currentSteps = 0,
            walkStatus = "",
            walkFallbackAllowed = false,
            sequenceTappedIndices = emptySet(),
            memoryPhase = MemoryPhase.SHOWING,
            memoryTappedIndices = emptySet(),
            typingInput = "",
            voiceTranscript = "",
            voiceStatus = "",
            handwritingStatus = "",
            handwritingBusy = false,
            wrongAttempts = 0,
            nfcScanStatus = "",
            barcodeScanStatus = "",
            photoMatchStatus = "",
            mazeCurrentPos = (nextChallenge as? Challenge.MazeChallenge)?.startPos ?: 0,
            wifiCurrentSsid = "",
            wifiStatus = "",
            wifiFallbackAllowed = false,
            sheepTapped = 0,
            sheepWrongTaps = 0,
            simonPlayingIndex = -1,
            simonInputIndices = emptyList(),
            simonErrorFlash = false,
            dateBackwardsInput = "",
            rpsPlayerWins = 0,
            rpsComputerWins = 0,
            rpsRounds = emptyList(),
            emojiMemoryPhase = EmojiMemoryPhase.REVEALING,
            emojiFlippedIndices = emptySet(),
            emojiMatchedIndices = emptySet(),
            typingSpeedInput = "",
            typingSpeedStartTime = 0L,
            wordleGuesses = emptyList(),
            wordleCurrentInput = "",
            wordleGameOver = false,
            pvtTrialIndex = 0,
            pvtReactionTimes = emptyList(),
            pvtStimulusShown = false,
            pvtStimulusShownAt = 0L,
            pvtWaiting = false,
            pvtLastReaction = null,
            pvtFailed = false
        )
        // Kick off Simon playback whenever the new challenge is Simon-says.
        if (nextChallenge is Challenge.SimonSaysChallenge) {
            playSimonSequence(nextChallenge)
        }
        // Kick off emoji reveal when the new challenge is emoji memory.
        if (nextChallenge is Challenge.EmojiMemoryChallenge) {
            startEmojiReveal(nextChallenge)
        }
    }

    // Math challenge - check answer
    fun submitMathAnswer(correct: Boolean) {
        if (correct) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // Shake challenge - update count from sensor
    fun updateShakeCount(count: Int) {
        val challenge = _uiState.value.challenge as? Challenge.ShakeChallenge ?: return
        _uiState.value = _uiState.value.copy(shakeCount = count)
        if (count >= challenge.requiredShakes) {
            proceedToNextChallenge()
        }
    }

    // Sequence challenge - tap a number
    fun tapSequenceNumber(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.SequenceChallenge ?: return
        val tapped = _uiState.value.sequenceTappedIndices
        val nextExpectedIndex = tapped.size
        val tappedNumber = challenge.numbers[index]
        val expectedNumber = challenge.correctOrder[nextExpectedIndex]

        if (tappedNumber == expectedNumber) {
            val newTapped = tapped + index
            _uiState.value = _uiState.value.copy(sequenceTappedIndices = newTapped)
            if (newTapped.size == challenge.numbers.size) {
                proceedToNextChallenge()
            }
        } else {
            // Wrong - reset
            _uiState.value = _uiState.value.copy(
                sequenceTappedIndices = emptySet(),
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // F3: Typing challenge
    fun updateTypingInput(text: String) {
        _uiState.value = _uiState.value.copy(typingInput = text)
    }

    fun submitTyping() {
        val challenge = _uiState.value.challenge as? Challenge.TypingChallenge ?: return
        if (_uiState.value.typingInput.trim().equals(challenge.phrase, ignoreCase = true)) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    fun submitVoicePhrase(recognizedText: String) {
        val challenge = _uiState.value.challenge as? Challenge.VoicePhraseChallenge ?: return
        val cleanTranscript = recognizedText.trim()
        if (VoicePhraseMatcher.matches(challenge.phrase, cleanTranscript)) {
            _uiState.value = _uiState.value.copy(
                voiceTranscript = cleanTranscript,
                voiceStatus = "Voice phrase matched."
            )
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                voiceTranscript = cleanTranscript,
                voiceStatus = if (cleanTranscript.isBlank()) {
                    "No phrase was detected. Try again or use the typed fallback."
                } else {
                    "Heard \"$cleanTranscript\". Say the phrase shown below."
                },
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    fun submitHandwriting(strokes: List<InkStroke>, width: Float, height: Float) {
        val challenge = _uiState.value.challenge as? Challenge.HandwritingChallenge ?: return
        if (strokes.none { it.points.size >= 2 }) {
            markHandwritingWrong("Draw the word before checking it.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                handwritingBusy = true,
                handwritingStatus = "Checking handwriting..."
            )
            val result = digitalInkChallengeRecognizer.recognize(
                DigitalInkRecognitionRequest(
                    strokes = strokes,
                    canvasWidth = width,
                    canvasHeight = height,
                    expectedText = challenge.targetText
                )
            )
            when {
                !result.isAvailable -> _uiState.value = _uiState.value.copy(
                    handwritingBusy = false,
                    handwritingStatus = result.unavailableReason
                        ?: "Handwriting recognition is unavailable. Type the word instead."
                )
                HandwritingChallengeMatcher.matches(challenge.targetText, result.candidates) -> {
                    _uiState.value = _uiState.value.copy(
                        handwritingBusy = false,
                        handwritingStatus = "Handwriting matched."
                    )
                    proceedToNextChallenge()
                }
                else -> markHandwritingWrong(
                    message = result.candidates.firstOrNull()?.let { candidate ->
                        "Recognized \"$candidate\". Draw ${challenge.targetText} again."
                    } ?: "No handwriting match. Draw ${challenge.targetText} again."
                )
            }
        }
    }

    fun submitHandwritingFallback(text: String) {
        val challenge = _uiState.value.challenge as? Challenge.HandwritingChallenge ?: return
        val cleanText = text.trim()
        if (HandwritingChallengeMatcher.matches(challenge.targetText, listOf(cleanText))) {
            _uiState.value = _uiState.value.copy(handwritingStatus = "Typed word matched.")
            proceedToNextChallenge()
        } else {
            markHandwritingWrong(
                if (cleanText.isBlank()) {
                    "Type the displayed word or draw it again."
                } else {
                    "Typed \"$cleanText\". Match ${challenge.targetText} exactly."
                }
            )
        }
    }

    private fun markHandwritingWrong(message: String) {
        if (_uiState.value.challenge !is Challenge.HandwritingChallenge) return
        _uiState.value = _uiState.value.copy(
            handwritingBusy = false,
            handwritingStatus = message,
            wrongAttempts = _uiState.value.wrongAttempts + 1,
            totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
        )
    }

    // F4: Walk-steps challenge
    fun updateStepCount(steps: Int) {
        val challenge = _uiState.value.challenge as? Challenge.WalkChallenge ?: return
        _uiState.value = _uiState.value.copy(currentSteps = steps)
        if (steps >= challenge.requiredSteps) {
            proceedToNextChallenge()
        }
    }

    fun onWalkChallengeUnavailable(message: String) {
        if (_uiState.value.challenge !is Challenge.WalkChallenge) return
        _uiState.value = _uiState.value.copy(
            walkStatus = message,
            walkFallbackAllowed = true
        )
    }

    fun continueWalkChallengeWithoutSensor() {
        if (_uiState.value.challenge !is Challenge.WalkChallenge) return
        if (!_uiState.value.walkFallbackAllowed) return
        proceedToNextChallenge()
    }

    // F2: NFC scan challenge
    fun onNfcTagDetected(tagId: String) {
        val challenge = _uiState.value.challenge as? Challenge.NfcChallenge ?: return
        if (challenge.registeredTagId.isBlank()) {
            // No tag registered — skip challenge
            proceedToNextChallenge()
            return
        }
        if (tagId.equals(challenge.registeredTagId, ignoreCase = true)) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                nfcScanStatus = "Wrong tag - try the registered tag",
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // F1: Barcode/QR scan challenge
    fun onBarcodeDetected(value: String) {
        val challenge = _uiState.value.challenge as? Challenge.BarcodeChallenge ?: return
        if (challenge.registeredValue.isBlank()) {
            proceedToNextChallenge()
            return
        }
        if (value == challenge.registeredValue) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                barcodeScanStatus = "Wrong code - scan the registered barcode",
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // v1.2.0: Maze challenge - move to adjacent cell
    fun tapMazeCell(cellIndex: Int) {
        val challenge = _uiState.value.challenge as? Challenge.MazeChallenge ?: return
        val currentPos = _uiState.value.mazeCurrentPos
        val size = challenge.gridSize

        // Check adjacency (up/down/left/right only)
        val currentRow = currentPos / size
        val currentCol = currentPos % size
        val targetRow = cellIndex / size
        val targetCol = cellIndex % size
        val isAdjacent = (kotlin.math.abs(currentRow - targetRow) + kotlin.math.abs(currentCol - targetCol)) == 1

        if (!isAdjacent || cellIndex in challenge.walls) return

        _uiState.value = _uiState.value.copy(mazeCurrentPos = cellIndex)
        if (cellIndex == challenge.endPos) {
            proceedToNextChallenge()
        }
    }

    // v1.2.0: Wi-Fi challenge - update current SSID
    fun updateWifiSsid(ssid: String) {
        val challenge = _uiState.value.challenge as? Challenge.WifiChallenge ?: return
        _uiState.value = _uiState.value.copy(
            wifiCurrentSsid = ssid,
            wifiStatus = "",
            wifiFallbackAllowed = false
        )
        if (challenge.requiredSsid.isBlank() && ssid.isNotBlank()) {
            proceedToNextChallenge()
        } else if (ssid == challenge.requiredSsid) {
            proceedToNextChallenge()
        }
    }

    fun onWifiChallengeUnavailable(message: String) {
        if (_uiState.value.challenge !is Challenge.WifiChallenge) return
        _uiState.value = _uiState.value.copy(
            wifiStatus = message,
            wifiFallbackAllowed = true
        )
    }

    fun continueWifiChallengeWithoutSsid() {
        if (_uiState.value.challenge !is Challenge.WifiChallenge) return
        if (!_uiState.value.wifiFallbackAllowed) return
        proceedToNextChallenge()
    }

    fun onLocationDismissLocation(latitude: Double, longitude: Double) {
        val alarm = currentAlarm ?: _uiState.value.alarm ?: return
        if (!alarm.locationDismissEnabled) return
        val result = LocationDismissPolicy.check(
            targetLatitude = alarm.locationDismissLat,
            targetLongitude = alarm.locationDismissLng,
            radiusMeters = alarm.locationDismissRadius,
            currentLatitude = latitude,
            currentLongitude = longitude
        )
        if (result == null) {
            _uiState.value = _uiState.value.copy(
                locationDismissReady = true,
                locationDismissDistanceMeters = null,
                locationDismissStatus = "No saved place is set, so location dismissal is not locked."
            )
            return
        }

        val distance = result.distanceMeters.toInt()
        val remaining = (result.radiusMeters - distance).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(
            locationDismissReady = result.outsideFence,
            locationDismissDistanceMeters = result.distanceMeters,
            locationDismissStatus = if (result.outsideFence) {
                "Location confirmed: ${distance} m from the saved place. Dismiss is unlocked."
            } else {
                "Still inside the saved area: ${distance} m away. Move about ${remaining} m farther to unlock dismiss."
            }
        )
    }

    fun onLocationDismissUnavailable(message: String) {
        val alarm = currentAlarm ?: _uiState.value.alarm ?: return
        if (!alarm.locationDismissEnabled) return
        _uiState.value = _uiState.value.copy(
            locationDismissReady = false,
            locationDismissStatus = message
        )
    }

    // v1.2.0: Squat challenge
    fun updateSquatCount(count: Int) {
        val challenge = _uiState.value.challenge as? Challenge.SquatChallenge ?: return
        _uiState.value = _uiState.value.copy(squatCount = count)
        if (count >= challenge.requiredSquats) {
            proceedToNextChallenge()
        }
    }

    // v1.15.0: Push-up challenge
    fun updatePushUpCount(count: Int) {
        val challenge = _uiState.value.challenge as? Challenge.PushUpChallenge ?: return
        _uiState.value = _uiState.value.copy(pushUpCount = count)
        if (count >= challenge.requiredPushUps) {
            proceedToNextChallenge()
        }
    }

    /**
     * Sensor-backed exercise challenges (squat/push-up) have no accelerometer on
     * some devices. Without a fallback the alarm would be undismissable, so
     * surface a "Continue without sensor" affordance exactly like the walk/wifi
     * challenges do.
     */
    fun onExerciseChallengeUnavailable(message: String) {
        val challenge = _uiState.value.challenge
        if (challenge !is Challenge.SquatChallenge && challenge !is Challenge.PushUpChallenge) return
        _uiState.value = _uiState.value.copy(
            exerciseStatus = message,
            exerciseFallbackAllowed = true
        )
    }

    fun continueExerciseChallengeWithoutSensor() {
        val challenge = _uiState.value.challenge
        if (challenge !is Challenge.SquatChallenge && challenge !is Challenge.PushUpChallenge) return
        if (!_uiState.value.exerciseFallbackAllowed) return
        proceedToNextChallenge()
    }

    // v1.15.0: Plank hold challenge — accumulate seconds while the phone is held level
    private var plankJob: kotlinx.coroutines.Job? = null

    fun onPlankHoldStart() {
        if (_uiState.value.challenge !is Challenge.PlankHoldChallenge) return
        if (_uiState.value.plankHoldActive) return
        _uiState.value = _uiState.value.copy(plankHoldActive = true)
        plankJob?.cancel()
        plankJob = viewModelScope.launch {
            val challenge = _uiState.value.challenge as? Challenge.PlankHoldChallenge ?: return@launch
            var elapsed = _uiState.value.plankHoldSeconds
            while (elapsed < challenge.requiredSeconds) {
                kotlinx.coroutines.delay(1000)
                if (!_uiState.value.plankHoldActive) return@launch
                elapsed++
                _uiState.value = _uiState.value.copy(plankHoldSeconds = elapsed)
                if (elapsed >= challenge.requiredSeconds) {
                    proceedToNextChallenge()
                    return@launch
                }
            }
        }
    }

    fun onPlankHoldBreak() {
        plankJob?.cancel()
        _uiState.value = _uiState.value.copy(plankHoldActive = false)
    }

    // v1.5.0: Simon-says — play the sequence then accept taps
    private var simonJob: kotlinx.coroutines.Job? = null
    private fun playSimonSequence(challenge: Challenge.SimonSaysChallenge) {
        simonJob?.cancel()
        simonJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            for (idx in challenge.sequence) {
                _uiState.value = _uiState.value.copy(simonPlayingIndex = idx)
                kotlinx.coroutines.delay(challenge.showDurationMs)
                _uiState.value = _uiState.value.copy(simonPlayingIndex = -1)
                kotlinx.coroutines.delay(200)
            }
            // Playback done — state already idle (playingIndex = -1).
        }
    }

    // v1.6.0: Emoji memory — reveal all cards briefly then flip face-down
    private var emojiMemoryJob: kotlinx.coroutines.Job? = null
    private fun startEmojiReveal(challenge: Challenge.EmojiMemoryChallenge) {
        emojiMemoryJob?.cancel()
        emojiMemoryJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(emojiMemoryPhase = EmojiMemoryPhase.REVEALING)
            kotlinx.coroutines.delay(challenge.revealDurationMs)
            _uiState.value = _uiState.value.copy(
                emojiMemoryPhase = EmojiMemoryPhase.INPUT,
                emojiFlippedIndices = emptySet()
            )
        }
    }

    fun onSimonPadTap(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.SimonSaysChallenge ?: return
        if (_uiState.value.simonPlayingIndex >= 0) return // ignore taps during playback
        val nextExpected = challenge.sequence.getOrNull(_uiState.value.simonInputIndices.size) ?: return
        if (index == nextExpected) {
            val updated = _uiState.value.simonInputIndices + index
            _uiState.value = _uiState.value.copy(simonInputIndices = updated)
            if (updated.size == challenge.sequence.size) {
                proceedToNextChallenge()
            }
        } else {
            _uiState.value = _uiState.value.copy(
                simonInputIndices = emptyList(),
                simonErrorFlash = true,
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
            // Flash briefly then replay the sequence from the start.
            viewModelScope.launch {
                kotlinx.coroutines.delay(700)
                _uiState.value = _uiState.value.copy(simonErrorFlash = false)
                playSimonSequence(challenge)
            }
        }
    }

    // v1.5.0: Date-backwards typing
    fun updateDateBackwardsInput(input: String) {
        _uiState.value = _uiState.value.copy(dateBackwardsInput = input)
    }
    fun submitDateBackwards() {
        val challenge = _uiState.value.challenge as? Challenge.DateBackwardsChallenge ?: return
        if (_uiState.value.dateBackwardsInput.trim() == challenge.expectedInput) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // v1.5.0: Stroop color-word pick
    fun onStroopPick(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.StroopChallenge ?: return
        if (index == challenge.inkColorIndex) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // v1.6.0: Rock-paper-scissors
    fun onRpsPick(choice: RpsChoice) {
        val challenge = _uiState.value.challenge as? Challenge.RockPaperScissorsChallenge ?: return
        val computerChoice = RpsChoice.entries.random()
        val outcome = when {
            choice == computerChoice -> RpsOutcome.DRAW
            (choice == RpsChoice.ROCK    && computerChoice == RpsChoice.SCISSORS) ||
            (choice == RpsChoice.SCISSORS && computerChoice == RpsChoice.PAPER)   ||
            (choice == RpsChoice.PAPER   && computerChoice == RpsChoice.ROCK)     -> RpsOutcome.WIN
            else -> RpsOutcome.LOSE
        }
        val newRound = RpsRoundResult(choice, computerChoice, outcome)
        val newPlayerWins   = _uiState.value.rpsPlayerWins   + if (outcome == RpsOutcome.WIN)  1 else 0
        val newComputerWins = _uiState.value.rpsComputerWins + if (outcome == RpsOutcome.LOSE) 1 else 0
        _uiState.value = _uiState.value.copy(
            rpsRounds       = _uiState.value.rpsRounds + newRound,
            rpsPlayerWins   = newPlayerWins,
            rpsComputerWins = newComputerWins
        )
        when {
            newPlayerWins >= challenge.requiredWins -> proceedToNextChallenge()
            newComputerWins >= challenge.requiredWins -> _uiState.value = _uiState.value.copy(
                rpsPlayerWins   = 0,
                rpsComputerWins = 0,
                rpsRounds       = emptyList(),
                wrongAttempts   = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // v1.6.0: Emoji memory card flip
    fun onEmojiCardFlip(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.EmojiMemoryChallenge ?: return
        val state = _uiState.value
        if (state.emojiMemoryPhase != EmojiMemoryPhase.INPUT) return
        if (index in state.emojiMatchedIndices || index in state.emojiFlippedIndices) return
        // Only allow flipping if fewer than 2 cards are currently face-up
        if (state.emojiFlippedIndices.size >= 2) return
        val newFlipped = state.emojiFlippedIndices + index
        _uiState.value = state.copy(emojiFlippedIndices = newFlipped)
        if (newFlipped.size == 2) {
            val (a, b) = newFlipped.toList()
            if (challenge.cards[a] == challenge.cards[b]) {
                val newMatched = state.emojiMatchedIndices + a + b
                _uiState.value = _uiState.value.copy(
                    emojiFlippedIndices = emptySet(),
                    emojiMatchedIndices = newMatched
                )
                if (newMatched.size == challenge.cards.size) proceedToNextChallenge()
            } else {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1000)
                    _uiState.value = _uiState.value.copy(
                        emojiFlippedIndices = emptySet(),
                        wrongAttempts = _uiState.value.wrongAttempts + 1,
                        totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
                    )
                }
            }
        }
    }

    // v1.6.0: Typing speed challenge
    fun onTypingSpeedInputChange(text: String) {
        val state = _uiState.value
        val newStart = if (state.typingSpeedStartTime == 0L && text.isNotEmpty()) {
            System.currentTimeMillis()
        } else {
            state.typingSpeedStartTime
        }
        _uiState.value = state.copy(typingSpeedInput = text, typingSpeedStartTime = newStart)
    }

    fun submitTypingSpeed() {
        val challenge = _uiState.value.challenge as? Challenge.TypingSpeedChallenge ?: return
        val state = _uiState.value
        val input = state.typingSpeedInput.trim()
        val targetWords = challenge.phrase.split(" ").map { it.lowercase().filter { c -> c.isLetter() } }
        val inputWords  = input.split(" ").map { it.lowercase().filter { c -> c.isLetter() } }
        // Count missing AND extra words as errors, and only credit words up to
        // the target length toward WPM — otherwise padding the input with junk
        // words inflates the numerator and defeats the speed gate.
        val errors = targetWords.zip(inputWords).count { (t, i) -> t != i } +
                kotlin.math.abs(targetWords.size - inputWords.size)
        val elapsedMs = maxOf(if (state.typingSpeedStartTime > 0L) System.currentTimeMillis() - state.typingSpeedStartTime else 1L, 1L)
        val countedWords = inputWords.size.coerceAtMost(targetWords.size)
        val wpm = (countedWords.toLong() * 60000L / elapsedMs).toInt()
        if (wpm >= challenge.minWpm && errors <= challenge.maxErrors) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                typingSpeedInput = "",
                typingSpeedStartTime = 0L,
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    // v1.6.0: Wordle challenge
    fun updateWordleInput(text: String) {
        if (_uiState.value.wordleGameOver) return
        _uiState.value = _uiState.value.copy(
            wordleCurrentInput = text.uppercase().filter { it.isLetter() }.take(5)
        )
    }

    fun submitWordleGuess() {
        val challenge = _uiState.value.challenge as? Challenge.WordleChallenge ?: return
        val state = _uiState.value
        if (state.wordleGameOver) return
        val guess = state.wordleCurrentInput
        if (guess.length != 5) return
        val newGuesses = state.wordleGuesses + guess
        val won = guess == challenge.target
        val outOfGuesses = newGuesses.size >= challenge.maxGuesses && !won
        _uiState.value = state.copy(
            wordleGuesses = newGuesses,
            wordleCurrentInput = "",
            wordleGameOver = outOfGuesses
        )
        if (won) {
            proceedToNextChallenge()
        } else if (outOfGuesses) {
            // Reset with a fresh word after showing the answer briefly
            viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                val newChallenge = ChallengeGenerator.generate(ChallengeType.WORDLE)
                        as? Challenge.WordleChallenge ?: return@launch
                _uiState.value = _uiState.value.copy(
                    challenge = newChallenge,
                    wordleGuesses = emptyList(),
                    wordleCurrentInput = "",
                    wordleGameOver = false,
                    wrongAttempts = _uiState.value.wrongAttempts + 1,
                    totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
                )
            }
        }
    }

    // v1.4.0: Count-the-Sheep challenge
    fun onSheepTapped() {
        val challenge = _uiState.value.challenge as? Challenge.CountSheepChallenge ?: return
        val next = _uiState.value.sheepTapped + 1
        _uiState.value = _uiState.value.copy(sheepTapped = next)
        if (next >= challenge.targetCount) {
            proceedToNextChallenge()
        }
    }

    fun onGoatTapped() {
        if (_uiState.value.challenge !is Challenge.CountSheepChallenge) return
        _uiState.value = _uiState.value.copy(
            sheepTapped = maxOf(0, _uiState.value.sheepTapped - 1),
            sheepWrongTaps = _uiState.value.sheepWrongTaps + 1,
            wrongAttempts = _uiState.value.wrongAttempts + 1,
            totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
        )
    }

    // F16: Photo match challenge
    fun onPhotoTaken(similarityScore: Float) {
        // Score 0.0–1.0; fire solved if >= 0.65
        if (similarityScore >= 0.65f) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                photoMatchStatus = "Not a match — try again (${(similarityScore * 100).toInt()}% similar)",
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    fun onPhotoCaptureUnavailable(message: String) {
        if (_uiState.value.challenge !is Challenge.PhotoMatchChallenge) return
        _uiState.value = _uiState.value.copy(photoMatchStatus = message)
    }

    // Memory pattern challenge
    fun onMemoryShowComplete() {
        _uiState.value = _uiState.value.copy(memoryPhase = MemoryPhase.INPUT)
    }

    fun tapMemoryTile(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.MemoryPatternChallenge ?: return
        if (_uiState.value.memoryPhase != MemoryPhase.INPUT) return

        val tapped = _uiState.value.memoryTappedIndices
        val nextExpectedIndex = tapped.size

        // Validate the tile matches the next expected position in the pattern sequence
        if (nextExpectedIndex < challenge.pattern.size && index == challenge.pattern[nextExpectedIndex]) {
            val newTapped = tapped + index
            _uiState.value = _uiState.value.copy(memoryTappedIndices = newTapped)
            if (newTapped.size == challenge.pattern.size) {
                proceedToNextChallenge()
            }
        } else {
            // Wrong tile - show pattern again and reset
            _uiState.value = _uiState.value.copy(
                memoryPhase = MemoryPhase.WRONG,
                memoryTappedIndices = emptySet(),
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
            // After a delay the screen should transition back to SHOWING
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(memoryPhase = MemoryPhase.SHOWING)
            }
        }
    }

    fun startPvtTrial() {
        _uiState.value = _uiState.value.copy(pvtWaiting = true, pvtStimulusShown = false, pvtLastReaction = null)
        viewModelScope.launch {
            val delayMs = (2000L..8000L).random()
            kotlinx.coroutines.delay(delayMs)
            if (_uiState.value.pvtWaiting) {
                _uiState.value = _uiState.value.copy(
                    pvtStimulusShown = true,
                    pvtStimulusShownAt = android.os.SystemClock.elapsedRealtime()
                )
            }
        }
    }

    fun onPvtReaction() {
        val state = _uiState.value
        val challenge = state.challenge as? Challenge.PvtChallenge ?: return
        if (!state.pvtStimulusShown) return
        val reactionMs = android.os.SystemClock.elapsedRealtime() - state.pvtStimulusShownAt
        val reactions = state.pvtReactionTimes + reactionMs
        val trialIndex = state.pvtTrialIndex + 1
        _uiState.value = state.copy(
            pvtReactionTimes = reactions,
            pvtTrialIndex = trialIndex,
            pvtStimulusShown = false,
            pvtWaiting = false,
            pvtLastReaction = reactionMs
        )
        if (trialIndex >= challenge.totalTrials) {
            val avg = reactions.average().toLong()
            if (avg <= challenge.maxAverageMs) {
                proceedToNextChallenge()
            } else {
                _uiState.value = _uiState.value.copy(
                    pvtFailed = true,
                    wrongAttempts = _uiState.value.wrongAttempts + 1,
                    totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
                )
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2500)
                    _uiState.value = _uiState.value.copy(
                        pvtTrialIndex = 0,
                        pvtReactionTimes = emptyList(),
                        pvtStimulusShown = false,
                        pvtWaiting = false,
                        pvtLastReaction = null,
                        pvtFailed = false
                    )
                }
            }
        }
    }

    fun onPvtFalseStart() {
        _uiState.value = _uiState.value.copy(
            pvtWaiting = false,
            pvtStimulusShown = false,
            pvtLastReaction = -1L,
            wrongAttempts = _uiState.value.wrongAttempts + 1,
            totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
        )
    }

    fun onSpotDifferencePick(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.SpotDifferenceChallenge ?: return
        if (index == challenge.changedIndex) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    fun onChessMateMovePick(move: String) {
        val challenge = _uiState.value.challenge as? Challenge.ChessMateChallenge ?: return
        if (move == challenge.puzzle.answer) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    fun onRsvpReadingChoice(choice: String) {
        val challenge = _uiState.value.challenge as? Challenge.RsvpReadingChallenge ?: return
        if (choice.equals(challenge.answer, ignoreCase = true)) {
            proceedToNextChallenge()
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1,
                totalWrongAttempts = _uiState.value.totalWrongAttempts + 1
            )
        }
    }

    companion object {
        private val MOTIVATIONAL_QUOTES = listOf(
            "The secret of getting ahead is getting started.",
            "Today is a new beginning. Make the most of it.",
            "Your future is created by what you do today.",
            "Rise up, start fresh, see the bright opportunity in each new day.",
            "Every morning brings new potential.",
            "Do something today that your future self will thank you for.",
            "The only way to do great work is to love what you do.",
            "Believe you can and you are halfway there.",
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "The best time for new beginnings is now.",
            "You are never too old to set another goal or to dream a new dream.",
            "What you do today can improve all your tomorrows.",
            "Start where you are. Use what you have. Do what you can.",
            "It does not matter how slowly you go as long as you do not stop.",
            "Act as if what you do makes a difference. It does."
        )
    }
}
