package com.sysadmindoc.alarmclock.ui.stats

import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class SleepWakeDayPoint(
    val date: LocalDate,
    val sleepMinutes: Long? = null,
    val dismissedCount: Int = 0,
    val snoozeCount: Int = 0,
    val challengeRetryCount: Int = 0,
    val averageResponseSec: Int? = null,
    val averageChallengeSolveSec: Int? = null,
    /**
     * Duration-only composite sleep score (0-100): how close the night's sleep
     * came to the user's nightly need. A full v2 will fold in efficiency,
     * regularity, and stage balance once those signals are available.
     */
    val sleepScore: Int? = null
) {
    val hasWakeData: Boolean
        get() = dismissedCount > 0 ||
            snoozeCount > 0 ||
            challengeRetryCount > 0 ||
            averageResponseSec != null ||
            averageChallengeSolveSec != null
}

data class SleepWakeAnalytics(
    val points: List<SleepWakeDayPoint> = emptyList(),
    val averageSleepMinutes: Long? = null,
    val averageResponseSec: Int? = null,
    val totalSnoozes: Int = 0,
    val totalChallengeRetries: Int = 0,
    val shortSleepAverageResponseSec: Int? = null,
    val restedSleepAverageResponseSec: Int? = null,
    val responseDeltaAfterShortSleepSec: Int? = null,
    /** The user's nightly sleep need, in minutes, used to score nights and debt. */
    val sleepNeedMinutes: Long = DEFAULT_SLEEP_NEED_MINUTES,
    /** Mean of the per-night [SleepWakeDayPoint.sleepScore] values, when any exist. */
    val averageSleepScore: Int? = null,
    /** The score of the most recent night with sleep data. */
    val latestSleepScore: Int? = null,
    /**
     * Rolling sleep-debt over the window: accumulated shortfall against
     * [sleepNeedMinutes]. Nights longer than the need pay debt back down, but it
     * never drops below zero or exceeds a one-week cap.
     */
    val sleepDebtMinutes: Long = 0L
) {
    val hasAnyData: Boolean
        get() = points.any { it.sleepMinutes != null || it.hasWakeData }

    val hasSleepWakeCorrelation: Boolean
        get() = points.any { it.sleepMinutes != null && it.averageResponseSec != null }

    val hasSleepScore: Boolean
        get() = points.any { it.sleepScore != null }
}

fun buildSleepWakeAnalytics(
    events: List<AlarmEvent>,
    sleepSessions: List<HealthConnectSleepSession>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId),
    days: Int = 14,
    sleepNeedMinutes: Long = DEFAULT_SLEEP_NEED_MINUTES
): SleepWakeAnalytics {
    val needMinutes = sleepNeedMinutes.coerceIn(MIN_SLEEP_NEED_MINUTES, MAX_SLEEP_NEED_MINUTES)
    val safeDays = days.coerceAtLeast(1)
    val dates = (safeDays - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val dateSet = dates.toSet()

    val sleepByWakeDate = sleepSessions
        .mapNotNull { session ->
            val wakeDate = Instant.ofEpochMilli(session.endMillis).atZone(zoneId).toLocalDate()
            if (wakeDate in dateSet) wakeDate to session else null
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, sessions) -> sessions.maxByOrNull { it.durationMinutes } }

    val eventsByDate = events
        .mapNotNull { event ->
            val eventDate = Instant.ofEpochMilli(event.firedAt).atZone(zoneId).toLocalDate()
            if (eventDate in dateSet) eventDate to event else null
        }
        .groupBy({ it.first }, { it.second })

    val points = dates.map { date ->
        val dayEvents = eventsByDate[date].orEmpty()
        val dismissedEvents = dayEvents.filter { it.action == AlarmEvent.ACTION_DISMISSED }
        val responseTimes = dismissedEvents
            .map { it.responseTimeMs }
            .filter { it > 0L }
        val challengeSolveTimes = dismissedEvents
            .map { it.challengeSolveTimeMs }
            .filter { it > 0L }
        val explicitSnoozes = dayEvents.count { it.action == AlarmEvent.ACTION_SNOOZED }
        val terminalSnoozes = dayEvents
            .filter { it.action != AlarmEvent.ACTION_SNOOZED }
            .sumOf { it.snoozeCount.coerceAtLeast(0) }

        val nightSleepMinutes = sleepByWakeDate[date]?.durationMinutes
        SleepWakeDayPoint(
            date = date,
            sleepMinutes = nightSleepMinutes,
            dismissedCount = dismissedEvents.size,
            snoozeCount = maxOf(explicitSnoozes, terminalSnoozes),
            challengeRetryCount = dayEvents.sumOf { it.challengeRetryCount.coerceAtLeast(0) },
            averageResponseSec = responseTimes.averageOrNullMsToSec(),
            averageChallengeSolveSec = challengeSolveTimes.averageOrNullMsToSec(),
            sleepScore = sleepScoreFor(nightSleepMinutes, needMinutes)
        )
    }

    // Rolling sleep debt: walk the window oldest-first, accumulating shortfall
    // against the nightly need. Surplus nights pay it down; it is floored at 0
    // and capped at a week so a long history can't show an alarming number.
    val maxDebt = needMinutes * SLEEP_DEBT_CAP_NIGHTS
    var runningDebt = 0L
    points.forEach { point ->
        val minutes = point.sleepMinutes ?: return@forEach
        runningDebt = (runningDebt + (needMinutes - minutes)).coerceIn(0L, maxDebt)
    }

    val sleepScores = points.mapNotNull { it.sleepScore }
    val sleepDurations = points.mapNotNull { it.sleepMinutes }
    val responses = points.mapNotNull { it.averageResponseSec }
    val shortSleepResponses = points
        .filter { (it.sleepMinutes ?: Long.MAX_VALUE) < SHORT_SLEEP_MINUTES }
        .mapNotNull { it.averageResponseSec }
    val restedSleepResponses = points
        .filter { (it.sleepMinutes ?: 0L) >= RESTED_SLEEP_MINUTES }
        .mapNotNull { it.averageResponseSec }
    val shortAverage = shortSleepResponses.averageOrNullInt()
    val restedAverage = restedSleepResponses.averageOrNullInt()

    return SleepWakeAnalytics(
        points = points,
        averageSleepMinutes = sleepDurations.averageOrNullLong(),
        averageResponseSec = responses.averageOrNullInt(),
        totalSnoozes = points.sumOf { it.snoozeCount },
        totalChallengeRetries = points.sumOf { it.challengeRetryCount },
        shortSleepAverageResponseSec = shortAverage,
        restedSleepAverageResponseSec = restedAverage,
        responseDeltaAfterShortSleepSec = if (shortAverage != null && restedAverage != null) {
            shortAverage - restedAverage
        } else {
            null
        },
        sleepNeedMinutes = needMinutes,
        averageSleepScore = sleepScores.averageOrNullInt(),
        latestSleepScore = points.lastOrNull { it.sleepScore != null }?.sleepScore,
        sleepDebtMinutes = runningDebt
    )
}

/**
 * Duration-only composite sleep score: 100 when the night meets or exceeds the
 * user's nightly need, scaling linearly toward 0 as sleep falls short. Returns
 * null when there is no sleep duration to score.
 */
fun sleepScoreFor(sleepMinutes: Long?, needMinutes: Long): Int? {
    if (sleepMinutes == null || needMinutes <= 0L) return null
    val ratio = sleepMinutes.toDouble() / needMinutes.toDouble()
    return (ratio * 100.0).roundToInt().coerceIn(0, 100)
}

private fun List<Long>.averageOrNullMsToSec(): Int? {
    return if (isEmpty()) null else (average() / 1000.0).toInt()
}

private fun List<Int>.averageOrNullInt(): Int? {
    return if (isEmpty()) null else average().toInt()
}

private fun List<Long>.averageOrNullLong(): Long? {
    return if (isEmpty()) null else average().toLong()
}

private const val SHORT_SLEEP_MINUTES = 6 * 60L
private const val RESTED_SLEEP_MINUTES = 7 * 60L

/** Default nightly sleep need (8h) when the user has not set a goal. */
const val DEFAULT_SLEEP_NEED_MINUTES = 8 * 60L
private const val MIN_SLEEP_NEED_MINUTES = 60L
private const val MAX_SLEEP_NEED_MINUTES = 16 * 60L
/** Cap rolling sleep debt at one week's worth of need to keep the figure sane. */
private const val SLEEP_DEBT_CAP_NIGHTS = 7L
