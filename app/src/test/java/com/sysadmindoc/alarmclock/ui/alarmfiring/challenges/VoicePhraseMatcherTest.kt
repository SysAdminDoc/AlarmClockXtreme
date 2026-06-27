package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePhraseMatcherTest {
    @Test
    fun normalizesCasePunctuationAndFillerWords() {
        assertEquals(
            "rise and shine it is time",
            VoicePhraseMatcher.normalize("Um, Rise and shine--it is time!")
        )
    }

    @Test
    fun acceptsExactPhraseIgnoringCaseAndPunctuation() {
        assertTrue(
            VoicePhraseMatcher.matches(
                expected = "Wake up and be awesome",
                recognized = "wake up, and be awesome!"
            )
        )
    }

    @Test
    fun acceptsMinorSpeechRecognitionTranscriptionNoise() {
        assertTrue(
            VoicePhraseMatcher.matches(
                expected = "Rise and shine it is time to wake up",
                recognized = "rise and shine it's time to wake up"
            )
        )
    }

    @Test
    fun rejectsDifferentPhrase() {
        assertFalse(
            VoicePhraseMatcher.matches(
                expected = "Do not go back to sleep",
                recognized = "turn off the alarm"
            )
        )
    }
}
