package com.sysadmindoc.alarmclock.ui.alarmfiring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
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
    // F3: Typing challenge
    val typingInput: String = "",
    // F4: Walk-steps challenge
    val currentSteps: Int = 0,
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
    val motivationalQuote: String = ""
) {
    val requiresChallenge: Boolean get() {
        val type = alarm?.challengeType ?: "NONE"
        return type != "NONE"
    }
    val canDismiss: Boolean get() = !requiresChallenge || challengeSolved
}

@HiltViewModel
class AlarmFiringViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val eventRepository: com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>(AlarmScheduler.EXTRA_ALARM_ID) ?: -1

    private val _uiState = MutableStateFlow(FiringUiState())
    val uiState: StateFlow<FiringUiState> = _uiState.asStateFlow()

    // v1.2.0: Challenge chain list built from alarm config
    private var challengeChainTypes: List<ChallengeType> = emptyList()
    private var currentAlarm: Alarm? = null

    init {
        loadAlarm()
    }

    private fun loadAlarm() {
        viewModelScope.launch {
            val alarm = repository.getById(alarmId) ?: return@launch
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
            challengeChainTypes = chainTypes

            val firstChallenge = if (chainTypes.isNotEmpty()) {
                buildChallengeForType(chainTypes[0], alarm)
            } else {
                null
            }

            val quote = MOTIVATIONAL_QUOTES.random()

            _uiState.value = FiringUiState(
                alarm = alarm,
                challenge = firstChallenge,
                challengeSolved = chainTypes.isEmpty(),
                totalChallenges = maxOf(chainTypes.size, 1),
                currentChallengeIndex = 0,
                motivationalQuote = quote
            )
        }
    }

    private fun buildChallengeForType(type: ChallengeType, alarm: Alarm): Challenge? = when (type) {
        ChallengeType.NONE -> null
        ChallengeType.WALK_STEPS -> Challenge.WalkChallenge(requiredSteps = alarm.walkStepsRequired)
        ChallengeType.NFC_SCAN -> Challenge.NfcChallenge(registeredTagId = alarm.nfcTagId)
        ChallengeType.BARCODE_SCAN -> Challenge.BarcodeChallenge(registeredValue = alarm.barcodeValue)
        ChallengeType.PHOTO_MATCH -> Challenge.PhotoMatchChallenge(referencePhotoUri = alarm.photoMatchUri)
        ChallengeType.SQUAT -> Challenge.SquatChallenge(requiredSquats = 10)
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
            currentSteps = 0,
            sequenceTappedIndices = emptySet(),
            memoryPhase = MemoryPhase.SHOWING,
            memoryTappedIndices = emptySet(),
            typingInput = "",
            wrongAttempts = 0,
            nfcScanStatus = "",
            barcodeScanStatus = "",
            photoMatchStatus = ""
        )
    }

    // Math challenge - check answer
    fun submitMathAnswer(correct: Boolean) {
        if (correct) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1
            )
        }
    }

    // Shake challenge - update count from sensor
    fun updateShakeCount(count: Int) {
        val challenge = _uiState.value.challenge as? Challenge.ShakeChallenge ?: return
        _uiState.value = _uiState.value.copy(shakeCount = count)
        if (count >= challenge.requiredShakes) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
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
                _uiState.value = _uiState.value.copy(challengeSolved = true)
            }
        } else {
            // Wrong - reset
            _uiState.value = _uiState.value.copy(
                sequenceTappedIndices = emptySet(),
                wrongAttempts = _uiState.value.wrongAttempts + 1
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
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        } else {
            _uiState.value = _uiState.value.copy(
                wrongAttempts = _uiState.value.wrongAttempts + 1
            )
        }
    }

    // F4: Walk-steps challenge
    fun updateStepCount(steps: Int) {
        val challenge = _uiState.value.challenge as? Challenge.WalkChallenge ?: return
        _uiState.value = _uiState.value.copy(currentSteps = steps)
        if (steps >= challenge.requiredSteps) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        }
    }

    // F2: NFC scan challenge
    fun onNfcTagDetected(tagId: String) {
        val challenge = _uiState.value.challenge as? Challenge.NfcChallenge ?: return
        if (challenge.registeredTagId.isBlank()) {
            // No tag registered — skip challenge
            _uiState.value = _uiState.value.copy(challengeSolved = true)
            return
        }
        if (tagId.equals(challenge.registeredTagId, ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        } else {
            _uiState.value = _uiState.value.copy(nfcScanStatus = "Wrong tag — try the registered tag")
        }
    }

    // F1: Barcode/QR scan challenge
    fun onBarcodeDetected(value: String) {
        val challenge = _uiState.value.challenge as? Challenge.BarcodeChallenge ?: return
        if (challenge.registeredValue.isBlank()) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
            return
        }
        if (value == challenge.registeredValue) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        } else {
            _uiState.value = _uiState.value.copy(barcodeScanStatus = "Wrong code — scan the registered barcode")
        }
    }

    // v1.2.0: Squat challenge
    fun updateSquatCount(count: Int) {
        val challenge = _uiState.value.challenge as? Challenge.SquatChallenge ?: return
        _uiState.value = _uiState.value.copy(squatCount = count)
        if (count >= challenge.requiredSquats) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        }
    }

    // F16: Photo match challenge
    fun onPhotoTaken(similarityScore: Float) {
        // Score 0.0–1.0; fire solved if >= 0.65
        if (similarityScore >= 0.65f) {
            _uiState.value = _uiState.value.copy(challengeSolved = true)
        } else {
            _uiState.value = _uiState.value.copy(
                photoMatchStatus = "Not a match — try again (${(similarityScore * 100).toInt()}% similar)",
                wrongAttempts = _uiState.value.wrongAttempts + 1
            )
        }
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
                _uiState.value = _uiState.value.copy(challengeSolved = true)
            }
        } else {
            // Wrong tile - show pattern again and reset
            _uiState.value = _uiState.value.copy(
                memoryPhase = MemoryPhase.WRONG,
                memoryTappedIndices = emptySet(),
                wrongAttempts = _uiState.value.wrongAttempts + 1
            )
            // After a delay the screen should transition back to SHOWING
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(memoryPhase = MemoryPhase.SHOWING)
            }
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
