package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeNoticeTone
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.DigitalInkChallengeRecognizer
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.DigitalInkRecognitionRequest
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.DigitalInkRecognitionResult
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.InkPoint
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.InkStroke
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The firing ViewModel had no test at all, which is how the voice and
 * handwriting notice tones shipped unverified: nothing said that a rejected
 * answer reads as a problem and an accepted one reads as success.
 *
 * The other thing pinned here is the fail-open rule. Dismissal stays locked
 * until the alarm row is loaded, so every path out of loading has to unlock it.
 * An alarm that cannot be turned off is worse than a challenge that gets
 * skipped.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmFiringViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repository: AlarmRepository
    private lateinit var recognizer: FakeInkRecognizer

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed = true)
        recognizer = FakeInkRecognizer()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a rejected voice answer reads as a problem`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "VOICE_PHRASE")
        advanceUntilIdle()

        viewModel.submitVoicePhrase("this is not the phrase at all")

        val state = viewModel.uiState.value
        assertEquals(ChallengeNoticeTone.PROBLEM, state.voiceStatusTone)
        assertFalse(state.challengeSolved)
        assertEquals(1, state.wrongAttempts)
        assertEquals(1, state.totalWrongAttempts)
        // The transcript is kept so the user can see what was heard.
        assertEquals("this is not the phrase at all", state.voiceTranscript)
    }

    @Test
    fun `an accepted voice answer reads as success and finishes the chain`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "VOICE_PHRASE")
        advanceUntilIdle()
        // The phrase is picked at random when the challenge is built, so it has
        // to be read back rather than assumed.
        val phrase = (viewModel.uiState.value.challenge as Challenge.VoicePhraseChallenge).phrase

        viewModel.submitVoicePhrase(phrase)

        val state = viewModel.uiState.value
        assertEquals(ChallengeNoticeTone.SUCCESS, state.voiceStatusTone)
        assertTrue(state.challengeSolved)
        assertEquals(0, state.totalWrongAttempts)
    }

    @Test
    fun `saying nothing at all is a problem, not a pass`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "VOICE_PHRASE")
        advanceUntilIdle()

        viewModel.submitVoicePhrase("   ")

        assertEquals(ChallengeNoticeTone.PROBLEM, viewModel.uiState.value.voiceStatusTone)
        assertFalse(viewModel.uiState.value.challengeSolved)
    }

    @Test
    fun `handwriting shows progress while the recogniser is working`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "HANDWRITING")
        advanceUntilIdle()
        var toneDuringRecognition: ChallengeNoticeTone? = null
        var busyDuringRecognition = false
        recognizer.onRecognize = {
            toneDuringRecognition = viewModel.uiState.value.handwritingStatusTone
            busyDuringRecognition = viewModel.uiState.value.handwritingBusy
        }
        recognizer.result = DigitalInkRecognitionResult(candidates = listOf("something else"))

        viewModel.submitHandwriting(strokes = listOf(stroke()), width = 100f, height = 100f)
        advanceUntilIdle()

        assertEquals(ChallengeNoticeTone.PROGRESS, toneDuringRecognition)
        assertTrue("the canvas should be locked while recognising", busyDuringRecognition)
        assertFalse("the canvas must unlock afterwards", viewModel.uiState.value.handwritingBusy)
    }

    @Test
    fun `handwriting the wrong word reads as a problem`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "HANDWRITING")
        advanceUntilIdle()
        recognizer.result = DigitalInkRecognitionResult(candidates = listOf("something else"))

        viewModel.submitHandwriting(strokes = listOf(stroke()), width = 100f, height = 100f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChallengeNoticeTone.PROBLEM, state.handwritingStatusTone)
        assertFalse(state.challengeSolved)
        assertEquals(1, state.wrongAttempts)
    }

    @Test
    fun `handwriting the right word reads as success`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "HANDWRITING")
        advanceUntilIdle()
        val target = (viewModel.uiState.value.challenge as Challenge.HandwritingChallenge).targetText
        recognizer.result = DigitalInkRecognitionResult(candidates = listOf(target))

        viewModel.submitHandwriting(strokes = listOf(stroke()), width = 100f, height = 100f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChallengeNoticeTone.SUCCESS, state.handwritingStatusTone)
        assertTrue(state.challengeSolved)
        assertFalse(state.handwritingBusy)
    }

    @Test
    fun `an unavailable recogniser is a problem the user can work around`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "HANDWRITING")
        advanceUntilIdle()
        recognizer.result = DigitalInkRecognitionResult(
            candidates = emptyList(),
            unavailableReason = "model not downloaded"
        )

        viewModel.submitHandwriting(strokes = listOf(stroke()), width = 100f, height = 100f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChallengeNoticeTone.PROBLEM, state.handwritingStatusTone)
        assertFalse(state.handwritingBusy)
        // Not counted against the user: the recogniser failed, they did not.
        assertEquals(0, state.wrongAttempts)

        // The typed fallback still has to work, or the alarm is unbeatable.
        val target = (state.challenge as Challenge.HandwritingChallenge).targetText
        viewModel.submitHandwritingFallback(target)
        assertEquals(ChallengeNoticeTone.SUCCESS, viewModel.uiState.value.handwritingStatusTone)
        assertTrue(viewModel.uiState.value.challengeSolved)
    }

    @Test
    fun `an empty canvas never reaches the recogniser`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "HANDWRITING")
        advanceUntilIdle()

        viewModel.submitHandwriting(
            strokes = listOf(InkStroke(points = listOf(InkPoint(1f, 1f, 0L)))),
            width = 100f,
            height = 100f
        )
        advanceUntilIdle()

        assertEquals(0, recognizer.calls)
        assertEquals(ChallengeNoticeTone.PROBLEM, viewModel.uiState.value.handwritingStatusTone)
    }

    @Test
    fun `a missing alarm row still unlocks dismissal`() = runTest(dispatcher) {
        coEvery { repository.getById(any()) } returns null

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(
            "dismissal stays locked, so the alarm would ring with no way to stop it",
            viewModel.uiState.value.alarmLoaded
        )
    }

    @Test
    fun `a failure while loading still unlocks dismissal`() = runTest(dispatcher) {
        coEvery { repository.getById(any()) } throws IllegalStateException("database gone")

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.alarmLoaded)
    }

    @Test
    fun `submitting the wrong kind of answer changes nothing`() = runTest(dispatcher) {
        val viewModel = viewModelFor(challengeType = "VOICE_PHRASE")
        advanceUntilIdle()
        val before = viewModel.uiState.value

        // The screen for another challenge type cannot reach into this one.
        viewModel.submitHandwriting(strokes = listOf(stroke()), width = 100f, height = 100f)
        viewModel.submitHandwritingFallback("anything")
        advanceUntilIdle()

        assertEquals(before, viewModel.uiState.value)
        assertEquals(0, recognizer.calls)
    }

    private fun stroke() = InkStroke(
        points = listOf(InkPoint(1f, 1f, 0L), InkPoint(20f, 30f, 12L), InkPoint(40f, 10f, 24L))
    )

    private fun viewModelFor(challengeType: String): AlarmFiringViewModel {
        coEvery { repository.getById(any()) } returns Alarm(
            id = 5L,
            hour = 7,
            minute = 0,
            label = "Wake",
            isEnabled = true,
            challengeType = challengeType
        )
        return viewModel()
    }

    private fun viewModel(): AlarmFiringViewModel {
        val preferencesManager: PreferencesManager = mockk(relaxed = true)
        every { preferencesManager.settings } returns flowOf(AppSettings())
        coEvery { preferencesManager.getCurrentSettings() } returns AppSettings()
        every { preferencesManager.getCachedSettings() } returns AppSettings()

        return AlarmFiringViewModel(
            appContext = context,
            savedStateHandle = SavedStateHandle(mapOf(AlarmScheduler.EXTRA_ALARM_ID to 5L)),
            repository = repository,
            eventRepository = mockk<AlarmEventRepository>(relaxed = true),
            preferencesManager = preferencesManager,
            weatherRepository = mockk<WeatherRepository>(relaxed = true),
            digitalInkChallengeRecognizer = recognizer
        )
    }

    private class FakeInkRecognizer : DigitalInkChallengeRecognizer {
        var result = DigitalInkRecognitionResult(candidates = emptyList())
        var calls = 0
        var onRecognize: (() -> Unit)? = null

        override suspend fun recognize(
            request: DigitalInkRecognitionRequest
        ): DigitalInkRecognitionResult {
            calls++
            onRecognize?.invoke()
            return result
        }
    }
}
