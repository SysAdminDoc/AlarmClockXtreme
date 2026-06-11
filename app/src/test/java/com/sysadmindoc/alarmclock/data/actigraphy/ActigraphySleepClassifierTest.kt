package com.sysadmindoc.alarmclock.data.actigraphy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActigraphySleepClassifierTest {

    @Test
    fun emptyInputProducesEmptyClassificationAndZeroSummary() {
        assertTrue(ActigraphySleepClassifier.classify(emptyList()).isEmpty())

        val summary = ActigraphySleepClassifier.summarize(emptyList())

        assertEquals(0, summary.totalMinutes)
        assertEquals(0, summary.awakeMinutes)
        assertEquals(0, summary.lightMinutes)
        assertEquals(0, summary.deepMinutes)
        assertEquals(0f, summary.averageSleepIndex, 0.0001f)
    }

    @Test
    fun stillEpochsBecomeDeepMotionBuckets() {
        val epochs = List(5) { index ->
            ActigraphyEpoch(startMillis = index * 60_000L, activityCount = 0f)
        }

        val scored = ActigraphySleepClassifier.classify(epochs)
        val summary = ActigraphySleepClassifier.summarize(epochs)

        assertTrue(scored.all { it.stage == ActigraphyStage.DEEP })
        assertEquals(5, summary.totalMinutes)
        assertEquals(0, summary.awakeMinutes)
        assertEquals(0, summary.lightMinutes)
        assertEquals(5, summary.deepMinutes)
        assertEquals(0f, summary.averageSleepIndex, 0.0001f)
    }

    @Test
    fun activeEpochsBecomeAwakeMotionBuckets() {
        val epochs = List(7) { index ->
            ActigraphyEpoch(startMillis = index * 60_000L, activityCount = 300f)
        }

        val scored = ActigraphySleepClassifier.classify(epochs)
        val summary = ActigraphySleepClassifier.summarize(epochs)

        assertTrue(scored.all { it.stage == ActigraphyStage.AWAKE })
        assertEquals(7, summary.totalMinutes)
        assertEquals(7, summary.awakeMinutes)
        assertEquals(0, summary.lightMinutes)
        assertEquals(0, summary.deepMinutes)
        assertTrue(summary.averageSleepIndex > 1f)
    }

    @Test
    fun mixedMotionProducesAwakeLightAndStillMotionBuckets() {
        val epochs = listOf(0f, 0f, 0f, 0f, 5f, 0f, 0f).mapIndexed { index, activity ->
            ActigraphyEpoch(startMillis = index * 60_000L, activityCount = activity)
        }

        val summary = ActigraphySleepClassifier.summarize(epochs)

        assertEquals(7, summary.totalMinutes)
        assertEquals(1, summary.awakeMinutes)
        assertEquals(2, summary.lightMinutes)
        assertEquals(4, summary.deepMinutes)
    }

    @Test
    fun boundaryWeightingZeroPadsMissingNeighborEpochs() {
        val scored = ActigraphySleepClassifier.classify(
            listOf(
                ActigraphyEpoch(startMillis = 0L, activityCount = 10f),
                ActigraphyEpoch(startMillis = 60_000L, activityCount = 0f),
                ActigraphyEpoch(startMillis = 120_000L, activityCount = 0f)
            )
        )

        assertEquals(2.3f, scored[0].sleepIndex, 0.0001f)
        assertEquals(0.76f, scored[1].sleepIndex, 0.0001f)
        assertEquals(0.58f, scored[2].sleepIndex, 0.0001f)
        assertEquals(ActigraphyStage.AWAKE, scored[0].stage)
        assertEquals(ActigraphyStage.LIGHT, scored[1].stage)
        assertEquals(ActigraphyStage.LIGHT, scored[2].stage)
    }

    @Test
    fun phoneMotionToActivityCountClampsSensorNoiseToClassifierRange() {
        assertEquals(0f, ActigraphySleepClassifier.phoneMotionToActivityCount(-1f), 0.0001f)
        assertEquals(5f, ActigraphySleepClassifier.phoneMotionToActivityCount(0.5f), 0.0001f)
        assertEquals(300f, ActigraphySleepClassifier.phoneMotionToActivityCount(50f), 0.0001f)
    }

    @Test
    fun summarizeScoredCountsBucketsAndAveragesSleepIndex() {
        val scored = listOf(
            ActigraphyScoredEpoch(
                epoch = ActigraphyEpoch(startMillis = 0L, activityCount = 0f),
                sleepIndex = 1.2f,
                stage = ActigraphyStage.AWAKE
            ),
            ActigraphyScoredEpoch(
                epoch = ActigraphyEpoch(startMillis = 60_000L, activityCount = 2f),
                sleepIndex = 0.5f,
                stage = ActigraphyStage.LIGHT
            ),
            ActigraphyScoredEpoch(
                epoch = ActigraphyEpoch(startMillis = 120_000L, activityCount = 0f),
                sleepIndex = 0.1f,
                stage = ActigraphyStage.DEEP
            )
        )

        val summary = ActigraphySleepClassifier.summarizeScored(scored)

        assertEquals(3, summary.totalMinutes)
        assertEquals(1, summary.awakeMinutes)
        assertEquals(1, summary.lightMinutes)
        assertEquals(1, summary.deepMinutes)
        assertEquals(0.6f, summary.averageSleepIndex, 0.0001f)
    }
}
