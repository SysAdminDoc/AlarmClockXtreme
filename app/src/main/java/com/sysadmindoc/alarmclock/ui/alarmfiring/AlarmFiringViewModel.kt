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
    val wrongAttempts: Int = 0
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
    private val repository: AlarmRepository
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>(AlarmScheduler.EXTRA_ALARM_ID) ?: -1

    private val _uiState = MutableStateFlow(FiringUiState())
    val uiState: StateFlow<FiringUiState> = _uiState.asStateFlow()

    init {
        loadAlarm()
    }

    private fun loadAlarm() {
        viewModelScope.launch {
            val alarm = repository.getById(alarmId) ?: return@launch
            val challengeType = try {
                ChallengeType.valueOf(alarm.challengeType)
            } catch (_: Exception) {
                ChallengeType.NONE
            }

            val challenge = if (challengeType != ChallengeType.NONE) {
                ChallengeGenerator.generate(challengeType)
            } else null

            _uiState.value = FiringUiState(
                alarm = alarm,
                challenge = challenge,
                challengeSolved = challengeType == ChallengeType.NONE
            )
        }
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

    // Memory pattern challenge
    fun onMemoryShowComplete() {
        _uiState.value = _uiState.value.copy(memoryPhase = MemoryPhase.INPUT)
    }

    fun tapMemoryTile(index: Int) {
        val challenge = _uiState.value.challenge as? Challenge.MemoryPatternChallenge ?: return
        if (_uiState.value.memoryPhase != MemoryPhase.INPUT) return

        val tapped = _uiState.value.memoryTappedIndices

        if (index in challenge.pattern) {
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
}
