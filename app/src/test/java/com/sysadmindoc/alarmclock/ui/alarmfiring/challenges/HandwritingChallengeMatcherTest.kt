package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingChallengeMatcherTest {
    @Test
    fun `matches exact candidate ignoring case`() {
        assertTrue(
            HandwritingChallengeMatcher.matches(
                expected = "AWAKE",
                candidates = listOf("awake")
            )
        )
    }

    @Test
    fun `matches later candidate`() {
        assertTrue(
            HandwritingChallengeMatcher.matches(
                expected = "ALARM",
                candidates = listOf("alone", "alarm", "alert")
            )
        )
    }

    @Test
    fun `normalizes punctuation and spaces`() {
        assertTrue(
            HandwritingChallengeMatcher.matches(
                expected = "SUNRISE",
                candidates = listOf("sun rise", "sunrise!")
            )
        )
    }

    @Test
    fun `rejects blank and different candidates`() {
        assertFalse(HandwritingChallengeMatcher.matches("READY", emptyList()))
        assertFalse(HandwritingChallengeMatcher.matches("READY", listOf("", "red")))
    }
}
