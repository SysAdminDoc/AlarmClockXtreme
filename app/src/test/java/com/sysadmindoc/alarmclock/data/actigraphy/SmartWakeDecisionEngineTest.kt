package com.sysadmindoc.alarmclock.data.actigraphy

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartWakeDecisionEngineTest {

    @Test
    fun waitsWhenObservationIsInsufficient() {
        val result = decide(
            stages = List(7) { ActigraphyStage.LIGHT },
            nowMinutes = 10
        )

        assertEquals(SmartWakeDecision.WAIT, result.decision)
        assertEquals(SmartWakeDecisionReason.WAIT_INSUFFICIENT_DATA, result.reasonCode)
    }

    @Test
    fun firesWhenRecentWindowIsConservativeLightMotion() {
        val result = decide(
            stages = listOf(
                ActigraphyStage.DEEP,
                ActigraphyStage.DEEP,
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.DEEP
            ),
            nowMinutes = 12
        )

        assertEquals(SmartWakeDecision.FIRE_EARLY, result.decision)
        assertEquals(SmartWakeDecisionReason.FIRE_LIGHT_MOTION, result.reasonCode)
        assertEquals(5, result.recentLightMinutes)
        assertEquals(0, result.recentAwakeMinutes)
        assertEquals(1, result.recentDeepMinutes)
    }

    @Test
    fun waitsWhenRecentWindowIsTooActive() {
        val result = decide(
            stages = listOf(
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.AWAKE,
                ActigraphyStage.AWAKE,
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.DEEP
            ),
            nowMinutes = 12
        )

        assertEquals(SmartWakeDecision.WAIT, result.decision)
        assertEquals(SmartWakeDecisionReason.WAIT_TOO_ACTIVE, result.reasonCode)
    }

    @Test
    fun waitsWhenRecentWindowIsDeepOrStillDominant() {
        val result = decide(
            stages = listOf(
                ActigraphyStage.LIGHT,
                ActigraphyStage.LIGHT,
                ActigraphyStage.DEEP,
                ActigraphyStage.DEEP,
                ActigraphyStage.DEEP,
                ActigraphyStage.LIGHT,
                ActigraphyStage.DEEP,
                ActigraphyStage.DEEP
            ),
            nowMinutes = 12
        )

        assertEquals(SmartWakeDecision.WAIT, result.decision)
        assertEquals(SmartWakeDecisionReason.WAIT_DEEP_OR_STILL, result.reasonCode)
    }

    @Test
    fun waitsDuringFinalMinuteForScheduledAlarmPath() {
        val result = decide(
            stages = List(10) { ActigraphyStage.LIGHT },
            nowMinutes = 29,
            nowExtraMs = 1_000L
        )

        assertEquals(SmartWakeDecision.WAIT, result.decision)
        assertEquals(SmartWakeDecisionReason.WAIT_FINAL_MINUTE, result.reasonCode)
    }

    @Test
    fun reportsReachedTargetWhenTargetTimeHasArrived() {
        val result = decide(
            stages = List(10) { ActigraphyStage.LIGHT },
            nowMinutes = 30
        )

        assertEquals(SmartWakeDecision.WAIT, result.decision)
        assertEquals(SmartWakeDecisionReason.REACHED_TARGET, result.reasonCode)
    }

    @Test
    fun reportsServiceTimeoutAsNonFatalWaitReason() {
        val result = SmartWakeDecisionEngine.serviceTimeout(
            scoredEpochs = List(9) { index ->
                ActigraphyScoredEpoch(
                    epoch = ActigraphyEpoch(startMillis = index * 60_000L, activityCount = 0f),
                    sleepIndex = 0.3f,
                    stage = ActigraphyStage.DEEP
                )
            }
        )

        assertEquals(SmartWakeDecision.WAIT, result.decision)
        assertEquals(SmartWakeDecisionReason.WAIT_SERVICE_TIMEOUT, result.reasonCode)
        assertEquals(9, result.observedMinutes)
    }

    private fun decide(
        stages: List<ActigraphyStage>,
        nowMinutes: Int,
        nowExtraMs: Long = 0L
    ): SmartWakeDecisionResult {
        val start = 1_000L
        return SmartWakeDecisionEngine.decide(
            scoredEpochs = stages.mapIndexed { index, stage ->
                ActigraphyScoredEpoch(
                    epoch = ActigraphyEpoch(startMillis = start + index * 60_000L, activityCount = 0f),
                    sleepIndex = 0.5f,
                    stage = stage
                )
            },
            sessionStartMs = start,
            nowMs = start + nowMinutes * 60_000L + nowExtraMs,
            targetTimeMs = start + 30 * 60_000L
        )
    }
}
