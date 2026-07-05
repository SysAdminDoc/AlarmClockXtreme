package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChronotypeEstimatorTest {
    @Test
    fun encodesAndDecodesFiveBoundedAnswers() {
        val encoded = ChronotypeEstimator.encodeAnswers(listOf(0, null, 2, 4, 9))

        assertEquals("0,,2,4,", encoded)
        assertEquals(listOf(0, null, 2, 4, null), ChronotypeEstimator.decodeAnswers(encoded))
        assertEquals("4,,0,,", ChronotypeEstimator.sanitizeAnswers("4,99,0,nope,"))
    }

    @Test
    fun incompleteAnswersDoNotProduceTimingRecommendation() {
        val estimate = ChronotypeEstimator.estimate("0,1,2", sleepGoalMinutes = 480)

        assertFalse(estimate.isComplete)
        assertEquals(3, estimate.answeredCount)
        assertNull(estimate.category)
        assertNull(estimate.idealBedtimeMinutes)
        assertNull(estimate.idealWakeMinutes)
    }

    @Test
    fun estimatesEarlyAndLateTimingFromCompletedAnswers() {
        val early = ChronotypeEstimator.estimate("0,0,1,0,1", sleepGoalMinutes = 480)
        val late = ChronotypeEstimator.estimate("4,4,3,4,4", sleepGoalMinutes = 480)

        assertTrue(early.isComplete)
        assertEquals(ChronotypeCategory.EARLY, early.category)
        assertEquals(390, early.idealWakeMinutes)
        assertEquals(1350, early.idealBedtimeMinutes)

        assertTrue(late.isComplete)
        assertEquals(ChronotypeCategory.LATE, late.category)
        assertEquals(585, late.idealWakeMinutes)
        assertEquals(105, late.idealBedtimeMinutes)
    }

    @Test
    fun answerUpdaterPreservesExistingAnswers() {
        val updated = ChronotypeEstimator.withAnswer("0,,2,,4", questionIndex = 1, answerIndex = 3)

        assertEquals("0,3,2,,4", updated)
    }
}
