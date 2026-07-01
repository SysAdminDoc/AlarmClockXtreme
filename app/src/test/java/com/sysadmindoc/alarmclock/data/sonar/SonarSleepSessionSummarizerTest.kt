package com.sysadmindoc.alarmclock.data.sonar

import org.junit.Assert.assertEquals
import org.junit.Test

class SonarSleepSessionSummarizerTest {

    @Test
    fun noWindowsStillProducesLocalSessionSummary() {
        val summary = SonarSleepSessionSummarizer.summarize(
            startedAt = 0L,
            endedAt = 10_000L,
            stillWindows = 0,
            movementWindows = 0
        )

        assertEquals(1, summary.totalMinutes)
        assertEquals(0, summary.awakeMinutes)
        assertEquals(1, summary.lightMinutes)
        assertEquals(0, summary.deepMinutes)
        assertEquals(0.5f, summary.averageSleepIndex, 0.0001f)
        assertEquals(SonarSleepSessionSummarizer.ALGORITHM_VERSION, summary.algorithm)
    }

    @Test
    fun stillDominantSessionBecomesStillMotionBuckets() {
        val summary = SonarSleepSessionSummarizer.summarize(
            startedAt = 0L,
            endedAt = 10 * 60_000L,
            stillWindows = 80,
            movementWindows = 20
        )

        assertEquals(10, summary.totalMinutes)
        assertEquals(2, summary.awakeMinutes)
        assertEquals(0, summary.lightMinutes)
        assertEquals(8, summary.deepMinutes)
        assertEquals(0.2f, summary.averageSleepIndex, 0.0001f)
    }

    @Test
    fun mixedMovementKeepsASeparateLightBucket() {
        val summary = SonarSleepSessionSummarizer.summarize(
            startedAt = 0L,
            endedAt = 5 * 60_000L,
            stillWindows = 45,
            movementWindows = 55
        )

        assertEquals(5, summary.totalMinutes)
        assertEquals(2, summary.awakeMinutes)
        assertEquals(1, summary.lightMinutes)
        assertEquals(2, summary.deepMinutes)
        assertEquals(0.55f, summary.averageSleepIndex, 0.0001f)
    }
}
