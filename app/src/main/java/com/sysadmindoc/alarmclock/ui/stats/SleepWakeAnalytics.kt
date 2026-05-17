package com.sysadmindoc.alarmclock.ui.stats

import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SleepWakeDayPoint(
    val date: LocalDate,
    val sleepMinutes: Long? = null,
    val dismissedCount: Int = 0,
    val snoozeCount: Int = 0,
    val challengeRetryCount: Int = 0,
    val averageResponseSec: Int? = null,
    val averageChallengeSolveSec: Int? = null
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
    val responseDeltaAfterShortSleepSec: Int? = null
) {
    val hasAnyData: Boolean
        get() = points.any { it.sleepMinutes != null || it.hasWakeData }

    val hasSleepWakeCorrelation: Boolean
        get() = points.any { it.sleepMinutes != null && it.averageResponseSec != null }
}

fun buildSleepWakeAnalytics(
    events: List<AlarmEvent>,
    sleepSessions: List<HealthConnectSleepSession>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId),
    days: Int = 14
): SleepWakeAnalytics {
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

        SleepWakeDayPoint(
            date = date,
            sleepMinutes = sleepByWakeDate[date]?.durationMinutes,
            dismissedCount = dismissedEvents.size,
            snoozeCount = maxOf(explicitSnoozes, terminalSnoozes),
            challengeRetryCount = dayEvents.sumOf { it.challengeRetryCount.coerceAtLeast(0) },
            averageResponseSec = responseTimes.averageOrNullMsToSec(),
            averageChallengeSolveSec = challengeSolveTimes.averageOrNullMsToSec()
        )
    }

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
        }
    )
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
