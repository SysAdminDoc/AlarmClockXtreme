package com.sysadmindoc.alarmclock.data.actigraphy

enum class SmartWakeDecision {
    FIRE_EARLY,
    WAIT
}

object SmartWakeDecisionReason {
    const val FIRE_LIGHT_MOTION = "FIRE_LIGHT_MOTION"
    const val WAIT_INSUFFICIENT_DATA = "WAIT_INSUFFICIENT_DATA"
    const val WAIT_TOO_ACTIVE = "WAIT_TOO_ACTIVE"
    const val WAIT_DEEP_OR_STILL = "WAIT_DEEP_OR_STILL"
    const val WAIT_LIGHT_NOT_STABLE = "WAIT_LIGHT_NOT_STABLE"
    const val WAIT_FINAL_MINUTE = "WAIT_FINAL_MINUTE"
    const val WAIT_SERVICE_TIMEOUT = "WAIT_SERVICE_TIMEOUT"
    const val REACHED_TARGET = "REACHED_TARGET"
}

data class SmartWakeDecisionResult(
    val decision: SmartWakeDecision,
    val reasonCode: String,
    val confidence: Float,
    val observedMinutes: Int,
    val recentLightMinutes: Int,
    val recentAwakeMinutes: Int,
    val recentDeepMinutes: Int,
    val mode: String = SmartWakeDecisionEngine.MODE_CONSERVATIVE
)

object SmartWakeDecisionEngine {
    const val MODE_CONSERVATIVE = "CONSERVATIVE"
    private const val MIN_EPOCHS = 8
    private const val RECENT_WINDOW_MINUTES = 6
    private const val FINAL_MINUTE_MS = 60_000L

    fun decide(
        scoredEpochs: List<ActigraphyScoredEpoch>,
        sessionStartMs: Long,
        nowMs: Long,
        targetTimeMs: Long
    ): SmartWakeDecisionResult {
        if (nowMs >= targetTimeMs) {
            return wait(
                reason = SmartWakeDecisionReason.REACHED_TARGET,
                scoredEpochs = scoredEpochs
            )
        }
        if (targetTimeMs - nowMs <= FINAL_MINUTE_MS) {
            return wait(
                reason = SmartWakeDecisionReason.WAIT_FINAL_MINUTE,
                scoredEpochs = scoredEpochs
            )
        }
        if (
            scoredEpochs.size < MIN_EPOCHS ||
            !SmartWakeObservationGate.canConsiderEarlyFire(sessionStartMs, nowMs, targetTimeMs)
        ) {
            return wait(
                reason = SmartWakeDecisionReason.WAIT_INSUFFICIENT_DATA,
                scoredEpochs = scoredEpochs
            )
        }

        val recent = scoredEpochs.takeLast(RECENT_WINDOW_MINUTES)
        val light = recent.count { it.stage == ActigraphyStage.LIGHT }
        val awake = recent.count { it.stage == ActigraphyStage.AWAKE }
        val deep = recent.count { it.stage == ActigraphyStage.DEEP }
        val confidence = ((light - awake - (deep * 0.5f)) / recent.size)
            .coerceIn(0f, 1f)

        return when {
            light >= 4 && awake <= 1 && deep <= 2 -> SmartWakeDecisionResult(
                decision = SmartWakeDecision.FIRE_EARLY,
                reasonCode = SmartWakeDecisionReason.FIRE_LIGHT_MOTION,
                confidence = confidence,
                observedMinutes = scoredEpochs.size,
                recentLightMinutes = light,
                recentAwakeMinutes = awake,
                recentDeepMinutes = deep
            )
            awake > 1 -> SmartWakeDecisionResult(
                decision = SmartWakeDecision.WAIT,
                reasonCode = SmartWakeDecisionReason.WAIT_TOO_ACTIVE,
                confidence = confidence,
                observedMinutes = scoredEpochs.size,
                recentLightMinutes = light,
                recentAwakeMinutes = awake,
                recentDeepMinutes = deep
            )
            deep > light -> SmartWakeDecisionResult(
                decision = SmartWakeDecision.WAIT,
                reasonCode = SmartWakeDecisionReason.WAIT_DEEP_OR_STILL,
                confidence = confidence,
                observedMinutes = scoredEpochs.size,
                recentLightMinutes = light,
                recentAwakeMinutes = awake,
                recentDeepMinutes = deep
            )
            else -> SmartWakeDecisionResult(
                decision = SmartWakeDecision.WAIT,
                reasonCode = SmartWakeDecisionReason.WAIT_LIGHT_NOT_STABLE,
                confidence = confidence,
                observedMinutes = scoredEpochs.size,
                recentLightMinutes = light,
                recentAwakeMinutes = awake,
                recentDeepMinutes = deep
            )
        }
    }

    fun reachedTarget(scoredEpochs: List<ActigraphyScoredEpoch>): SmartWakeDecisionResult {
        return wait(
            reason = SmartWakeDecisionReason.REACHED_TARGET,
            scoredEpochs = scoredEpochs
        )
    }

    fun serviceTimeout(scoredEpochs: List<ActigraphyScoredEpoch>): SmartWakeDecisionResult {
        return wait(
            reason = SmartWakeDecisionReason.WAIT_SERVICE_TIMEOUT,
            scoredEpochs = scoredEpochs
        )
    }

    private fun wait(
        reason: String,
        scoredEpochs: List<ActigraphyScoredEpoch>
    ): SmartWakeDecisionResult {
        val recent = scoredEpochs.takeLast(RECENT_WINDOW_MINUTES)
        val light = recent.count { it.stage == ActigraphyStage.LIGHT }
        val awake = recent.count { it.stage == ActigraphyStage.AWAKE }
        val deep = recent.count { it.stage == ActigraphyStage.DEEP }
        return SmartWakeDecisionResult(
            decision = SmartWakeDecision.WAIT,
            reasonCode = reason,
            confidence = 0f,
            observedMinutes = scoredEpochs.size,
            recentLightMinutes = light,
            recentAwakeMinutes = awake,
            recentDeepMinutes = deep
        )
    }
}
