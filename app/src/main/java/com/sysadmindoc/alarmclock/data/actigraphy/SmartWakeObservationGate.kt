package com.sysadmindoc.alarmclock.data.actigraphy

object SmartWakeObservationGate {
    const val MIN_OBSERVATION_MINUTES = 8L
    private const val MIN_OBSERVATION_MS = MIN_OBSERVATION_MINUTES * 60_000L

    fun canConsiderEarlyFire(
        sessionStartMs: Long,
        nowMs: Long,
        targetTimeMs: Long
    ): Boolean {
        if (sessionStartMs <= 0L || nowMs <= sessionStartMs || targetTimeMs <= sessionStartMs) {
            return false
        }
        val configuredWindowMs = targetTimeMs - sessionStartMs
        val requiredObservationMs = maxOf(MIN_OBSERVATION_MS, configuredWindowMs / 3L)
        return nowMs - sessionStartMs >= requiredObservationMs
    }
}
