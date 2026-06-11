package com.sysadmindoc.alarmclock.data.actigraphy

enum class ActigraphyStage {
    AWAKE,
    LIGHT,
    DEEP
}

data class ActigraphyEpoch(
    val startMillis: Long,
    val activityCount: Float
)

data class ActigraphyScoredEpoch(
    val epoch: ActigraphyEpoch,
    val sleepIndex: Float,
    val stage: ActigraphyStage
)

data class ActigraphySessionSummary(
    val totalMinutes: Int,
    val awakeMinutes: Int,
    val lightMinutes: Int,
    val deepMinutes: Int,
    val averageSleepIndex: Float,
    val algorithm: String = ActigraphySleepClassifier.ALGORITHM_VERSION
)

/**
 * Experimental phone-actigraphy bucketizer.
 *
 * The binary sleep/wake pass follows the 1-minute Cole-Kripke weighting shape,
 * but the input is phone accelerometer motion, not calibrated ActiGraph counts.
 * DEEP vs LIGHT is therefore a conservative local heuristic over epochs already
 * scored as sleep, not a clinical sleep-stage classifier.
 */
object ActigraphySleepClassifier {
    const val ALGORITHM_VERSION = "phone_cole_kripke_experimental_v1"

    private val weights = intArrayOf(106, 54, 58, 76, 230, 74, 67)
    private const val SCALE = 0.001f
    private const val WAKE_THRESHOLD = 1f
    private const val DEEP_SLEEP_INDEX = 0.36f
    private const val DEEP_ACTIVITY_COUNT = 2.4f

    fun classify(epochs: List<ActigraphyEpoch>): List<ActigraphyScoredEpoch> {
        if (epochs.isEmpty()) return emptyList()
        return epochs.mapIndexed { index, epoch ->
            val sleepIndex = sleepIndexFor(epochs, index)
            val stage = when {
                sleepIndex >= WAKE_THRESHOLD -> ActigraphyStage.AWAKE
                sleepIndex <= DEEP_SLEEP_INDEX && epoch.activityCount <= DEEP_ACTIVITY_COUNT ->
                    ActigraphyStage.DEEP
                else -> ActigraphyStage.LIGHT
            }
            ActigraphyScoredEpoch(epoch = epoch, sleepIndex = sleepIndex, stage = stage)
        }
    }

    fun summarizeScored(scored: List<ActigraphyScoredEpoch>): ActigraphySessionSummary {
        val awake = scored.count { it.stage == ActigraphyStage.AWAKE }
        val light = scored.count { it.stage == ActigraphyStage.LIGHT }
        val deep = scored.count { it.stage == ActigraphyStage.DEEP }
        val avg = if (scored.isEmpty()) 0f else scored.map { it.sleepIndex }.average().toFloat()
        return ActigraphySessionSummary(
            totalMinutes = scored.size,
            awakeMinutes = awake,
            lightMinutes = light,
            deepMinutes = deep,
            averageSleepIndex = avg
        )
    }

    fun summarize(epochs: List<ActigraphyEpoch>): ActigraphySessionSummary {
        return summarizeScored(classify(epochs))
    }

    fun phoneMotionToActivityCount(maxGravityDelta: Float): Float {
        return (maxGravityDelta.coerceAtLeast(0f) * 10f).coerceAtMost(300f)
    }

    private fun sleepIndexFor(epochs: List<ActigraphyEpoch>, index: Int): Float {
        val offsets = intArrayOf(-4, -3, -2, -1, 0, 1, 2)
        var weighted = 0f
        for (i in offsets.indices) {
            val value = epochs.getOrNull(index + offsets[i])?.activityCount ?: 0f
            weighted += weights[i] * value
        }
        return weighted * SCALE
    }
}
