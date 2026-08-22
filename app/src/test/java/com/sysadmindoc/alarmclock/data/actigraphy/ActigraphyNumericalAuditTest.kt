package com.sysadmindoc.alarmclock.data.actigraphy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A numerical audit of the sleep classifier, written 2026-08-22 because the
 * maths had never been checked against the paper it names.
 *
 * The verdict is that the implementation is faithful. Cole-Kripke (1992) gives
 * the 1-minute automatic scoring as
 *
 *     D = 0.001 x (106*A-4 + 54*A-3 + 58*A-2 + 76*A-1 + 230*A0 + 74*A+1 + 67*A+2)
 *
 * with D >= 1 scored as wake, and that is exactly what the classifier computes,
 * including the sign and order of the offsets. These cases reproduce it from
 * hand-computed sums so a future edit to the weight table has to argue with
 * arithmetic rather than with a comment.
 *
 * What is NOT faithful, and is the honest limit of the feature, is the input.
 * Cole-Kripke takes calibrated ActiGraph counts, which integrate movement over
 * the epoch. This takes the largest 30-second peak in the minute, scaled by 10.
 * A peak cannot distinguish one sharp movement from sustained restlessness, so
 * the absolute threshold carries no clinical meaning even though the weighting
 * shape does.
 */
class ActigraphyNumericalAuditTest {

    private fun epochs(vararg counts: Float): List<ActigraphyEpoch> =
        counts.mapIndexed { i, c -> ActigraphyEpoch(startMillis = i * 60_000L, activityCount = c) }

    @Test
    fun `the weighted sum reproduces the published Cole-Kripke formula`() {
        val scored = ActigraphySleepClassifier.classify(
            epochs(0f, 0f, 0f, 0f, 10f, 20f, 30f, 0f, 0f)
        )

        // index 4 sees [0, 0, 0, 0, 10, 20, 30]
        // 0.001 * (230*10 + 74*20 + 67*30) = 5.790
        assertEquals(5.790f, scored[4].sleepIndex, 0.0005f)

        // index 6 sees [0, 0, 10, 20, 30, 0, 0]
        // 0.001 * (58*10 + 76*20 + 230*30) = 9.000
        assertEquals(9.000f, scored[6].sleepIndex, 0.0005f)

        // index 8 sees [10, 20, 30, 0, 0, -, -], the tail zero-padded
        // 0.001 * (106*10 + 54*20 + 58*30) = 3.880
        assertEquals(3.880f, scored[8].sleepIndex, 0.0005f)
    }

    @Test
    fun `the look-ahead weights are 74 then 67, not the other way round`() {
        // The two forward weights are close enough to swap unnoticed, and a swap
        // would shift every boundary between wake and sleep by a small amount
        // rather than breaking anything visibly.
        val forwardOne = ActigraphySleepClassifier.classify(epochs(0f, 100f, 0f))[0].sleepIndex
        val forwardTwo = ActigraphySleepClassifier.classify(epochs(0f, 0f, 100f))[0].sleepIndex

        assertEquals(7.4f, forwardOne, 0.0005f)
        assertEquals(6.7f, forwardTwo, 0.0005f)
    }

    @Test
    fun `the wake threshold lands at a known amount of phone movement`() {
        // A single isolated minute of movement contributes only its own weight,
        // so the threshold reduces to 0.001 * 230 * (delta * 10) >= 1, which is
        // a peak of 0.4348 m/s2: about 4.4% of gravity. Worth knowing, because
        // nothing else in the codebase states what "awake" costs in sensor units.
        fun stageForDelta(delta: Float): ActigraphyStage {
            val count = ActigraphySleepClassifier.phoneMotionToActivityCount(delta)
            return ActigraphySleepClassifier.classify(
                epochs(0f, 0f, 0f, 0f, count, 0f, 0f)
            )[4].stage
        }

        assertEquals(ActigraphyStage.AWAKE, stageForDelta(0.44f))
        assertTrue(stageForDelta(0.43f) != ActigraphyStage.AWAKE)
    }

    @Test
    fun `a minute is one epoch, which is what makes the summary minutes`() {
        // summarizeScored reports epoch counts as minutes. That is only true
        // because SmartAlarmService folds two 30-second windows into one epoch;
        // if that cadence ever changes, every duration on the Stats screen is
        // wrong by the same factor and nothing else would notice.
        val summary = ActigraphySleepClassifier.summarize(epochs(0f, 0f, 0f, 0f, 0f, 0f))

        assertEquals(6, summary.totalMinutes)
        assertEquals(6, summary.awakeMinutes + summary.lightMinutes + summary.deepMinutes)
    }

    @Test
    fun `a still night is deep and a restless one is not`() {
        val still = ActigraphySleepClassifier.summarize(epochs(*FloatArray(20) { 0f }))
        assertEquals(20, still.deepMinutes)
        assertEquals(0, still.awakeMinutes)

        val restless = ActigraphySleepClassifier.summarize(epochs(*FloatArray(20) { 8f }))
        assertEquals(0, restless.deepMinutes)
        assertEquals(20, restless.awakeMinutes)
    }
}
