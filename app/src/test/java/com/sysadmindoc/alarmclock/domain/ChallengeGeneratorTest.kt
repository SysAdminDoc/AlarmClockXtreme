package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.*
import org.junit.Assert.*
import org.junit.Test

class ChallengeGeneratorTest {

    @Test
    fun `math easy generates valid challenge`() {
        repeat(20) {
            val challenge = ChallengeGenerator.generate(ChallengeType.MATH_EASY) as Challenge.MathChallenge
            assertEquals(ChallengeType.MATH_EASY, challenge.type)
            assertTrue("Answer should be in choices", challenge.answer in challenge.choices)
            assertEquals("Should have 4 choices", 4, challenge.choices.size)
            assertTrue("Expression should end with ?", challenge.expression.endsWith("?"))
        }
    }

    @Test
    fun `math medium generates valid challenge`() {
        repeat(20) {
            val challenge = ChallengeGenerator.generate(ChallengeType.MATH_MEDIUM) as Challenge.MathChallenge
            assertEquals(ChallengeType.MATH_MEDIUM, challenge.type)
            assertTrue("Answer should be in choices", challenge.answer in challenge.choices)
            assertEquals(4, challenge.choices.size)
        }
    }

    @Test
    fun `math hard generates valid challenge`() {
        repeat(20) {
            val challenge = ChallengeGenerator.generate(ChallengeType.MATH_HARD) as Challenge.MathChallenge
            assertEquals(ChallengeType.MATH_HARD, challenge.type)
            assertTrue("Answer should be in choices", challenge.answer in challenge.choices)
            assertEquals(4, challenge.choices.size)
        }
    }

    @Test
    fun `math choices are unique`() {
        repeat(50) {
            val challenge = ChallengeGenerator.generate(ChallengeType.MATH_EASY) as Challenge.MathChallenge
            val uniqueChoices = challenge.choices.toSet()
            assertEquals("All choices should be unique", challenge.choices.size, uniqueChoices.size)
        }
    }

    @Test
    fun `shake challenge has correct defaults`() {
        val challenge = ChallengeGenerator.generate(ChallengeType.SHAKE) as Challenge.ShakeChallenge
        assertEquals(ChallengeType.SHAKE, challenge.type)
        assertEquals(30, challenge.requiredShakes)
        assertEquals(0, challenge.currentShakes)
    }

    @Test
    fun `sequence challenge generates 6 unique numbers`() {
        repeat(10) {
            val challenge = ChallengeGenerator.generate(ChallengeType.SEQUENCE) as Challenge.SequenceChallenge
            assertEquals(6, challenge.numbers.size)
            assertEquals(6, challenge.numbers.toSet().size) // All unique
            assertEquals(challenge.numbers.sorted(), challenge.correctOrder)
        }
    }

    @Test
    fun `memory pattern generates valid grid positions`() {
        repeat(10) {
            val challenge = ChallengeGenerator.generate(ChallengeType.MEMORY_PATTERN) as Challenge.MemoryPatternChallenge
            assertEquals(3, challenge.gridSize)
            assertEquals(4, challenge.pattern.size)
            assertTrue("All indices should be in 0-8 range",
                challenge.pattern.all { it in 0 until 9 })
            assertEquals(4, challenge.pattern.toSet().size) // All unique positions
        }
    }

    @Test
    fun `none type generates placeholder`() {
        val challenge = ChallengeGenerator.generate(ChallengeType.NONE)
        assertTrue(challenge is Challenge.MathChallenge)
        assertEquals(ChallengeType.NONE, challenge.type)
    }
}
