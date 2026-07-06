package com.sysadmindoc.alarmclock.domain

import kotlin.math.log10

data class SnoreEventCandidate(
    val startedAt: Long,
    val endedAt: Long,
    val peakDb: Float,
    val averageDb: Float,
    val windowCount: Int
) {
    val durationMillis: Long = (endedAt - startedAt).coerceAtLeast(0L)
}

class SnoreEventDetector(
    private val thresholdDb: Float = DEFAULT_THRESHOLD_DB,
    private val minDurationMs: Long = DEFAULT_MIN_DURATION_MS,
    private val mergeGapMs: Long = DEFAULT_MERGE_GAP_MS
) {
    private var active: MutableSnoreEvent? = null

    fun acceptWindow(
        windowStartedAt: Long,
        windowDurationMs: Long,
        rms: Float
    ): SnoreEventCandidate? {
        val windowEndedAt = windowStartedAt + windowDurationMs.coerceAtLeast(1L)
        val db = estimatedDbFromRms(rms)
        if (db >= thresholdDb) {
            val current = active
            if (current == null) {
                active = MutableSnoreEvent(
                    startedAt = windowStartedAt,
                    endedAt = windowEndedAt,
                    lastAboveThresholdAt = windowEndedAt,
                    peakDb = db,
                    totalDb = db,
                    windowCount = 1
                )
            } else {
                current.endedAt = windowEndedAt
                current.lastAboveThresholdAt = windowEndedAt
                current.peakDb = maxOf(current.peakDb, db)
                current.totalDb += db
                current.windowCount++
            }
            return null
        }

        val current = active ?: return null
        current.endedAt = windowEndedAt
        if (windowEndedAt - current.lastAboveThresholdAt < mergeGapMs) return null

        active = null
        return current.toCandidate()
    }

    fun flush(): SnoreEventCandidate? {
        val current = active ?: return null
        active = null
        return current.toCandidate()
    }

    private fun MutableSnoreEvent.toCandidate(): SnoreEventCandidate? {
        val eventEndedAt = lastAboveThresholdAt.coerceAtLeast(startedAt)
        val duration = eventEndedAt - startedAt
        if (duration < minDurationMs || windowCount <= 0) return null
        return SnoreEventCandidate(
            startedAt = startedAt,
            endedAt = eventEndedAt,
            peakDb = peakDb,
            averageDb = totalDb / windowCount,
            windowCount = windowCount
        )
    }

    private data class MutableSnoreEvent(
        val startedAt: Long,
        var endedAt: Long,
        var lastAboveThresholdAt: Long,
        var peakDb: Float,
        var totalDb: Float,
        var windowCount: Int
    )

    companion object {
        const val DEFAULT_THRESHOLD_DB = 60f
        const val DEFAULT_MIN_DURATION_MS = 400L
        const val DEFAULT_MERGE_GAP_MS = 1_500L

        fun estimatedDbFromRms(rms: Float): Float {
            if (rms <= 0f || rms.isNaN()) return 0f
            return (REFERENCE_DB + 20f * log10(rms.coerceAtLeast(MIN_RMS))).coerceIn(0f, 120f)
        }

        private const val REFERENCE_DB = 114f
        private const val MIN_RMS = 0.000001f
    }
}
