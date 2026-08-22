package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.local.AlarmEventDao
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class AlarmStats(
    val totalDismissed: Int = 0,
    val totalSnoozed: Int = 0,
    val totalSkipped: Int = 0,
    val totalMissed: Int = 0,
    val averageDismissTimeSec: Int = 0,
    val snoozeRate: Int = 0,             // Percentage of alarms that got snoozed
    val currentStreak: Int = 0,          // Consecutive days with dismissed alarm
    val bestStreak: Int = 0,
    val streakIncludesToday: Boolean = false,
    val nextStreakGoal: Int = 3,
    val alarmsThisWeek: Int = 0,
    val dayOfWeekCounts: Map<DayOfWeek, Int> = emptyMap(),
    val dayOfWeekAvgResponseSec: Map<DayOfWeek, Int> = emptyMap()
)

data class WakeStreakSummary(
    val currentDays: Int = 0,
    val bestDays: Int = 0,
    val includesToday: Boolean = false,
    val nextGoal: Int = 3
)

internal object WakeStreakCalculator {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val goals = listOf(3, 7, 14, 30, 60, 100)

    fun calculate(dateStrings: List<String>, today: LocalDate = LocalDate.now()): WakeStreakSummary {
        val dates = dateStrings
            .mapNotNull { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }
            .distinct()
            .sortedDescending()
        if (dates.isEmpty()) return WakeStreakSummary()

        val includesToday = today in dates
        var expected = if (includesToday) today else today.minusDays(1)
        var current = 0

        for (date in dates) {
            if (date.isAfter(expected)) continue
            if (date == expected) {
                current++
                expected = expected.minusDays(1)
            } else if (date.isBefore(expected)) {
                break
            }
        }

        var best = 0
        var run = 0
        var previous: LocalDate? = null
        dates.sorted().forEach { date ->
            run = if (previous == null || date == previous?.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }

        val nextGoal = goals.firstOrNull { it > current } ?: (((current / 50) + 1) * 50)
        return WakeStreakSummary(
            currentDays = current,
            bestDays = best,
            includesToday = includesToday,
            nextGoal = nextGoal
        )
    }
}

@Singleton
class AlarmEventRepository @Inject constructor(
    private val dao: AlarmEventDao
) {
    fun observeRecent(limit: Int = 50): Flow<List<AlarmEvent>> = dao.observeRecent(limit)

    suspend fun record(event: AlarmEvent): Long {
        val id = dao.insert(event)
        prune()
        return id
    }

    /**
     * Keeps the history bounded. Stats never look further back than a year, and
     * the table previously only ever grew.
     */
    private suspend fun prune() {
        dao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
        dao.trimToLatest(MAX_ROWS)
    }

    suspend fun getSince(sinceMs: Long): List<AlarmEvent> = dao.getSince(sinceMs)

    suspend fun getStats(): AlarmStats {
        val dismissed = dao.countByAction(AlarmEvent.ACTION_DISMISSED)
        val snoozed = dao.countByAction(AlarmEvent.ACTION_SNOOZED)
        val skipped = dao.countByAction(AlarmEvent.ACTION_SKIPPED)
        val missed = dao.countByAction(AlarmEvent.ACTION_MISSED)
        val avgDismissMs = dao.averageDismissTimeMs() ?: 0L
        val withSnooze = dao.countWithSnooze()
        val total = dismissed + skipped + missed

        // Week stats
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val thisWeek = dao.countSince(weekAgo)

        // Day of week breakdown (filter invalid values - DayOfWeek.of requires 1-7)
        val dowCounts = dao.countByDayOfWeek()
            .filter { it.dayOfWeek in 1..7 }
            .associate { DayOfWeek.of(it.dayOfWeek) to it.cnt }
        val dowAvg = dao.avgResponseByDayOfWeek()
            .filter { it.dayOfWeek in 1..7 }
            .associate { DayOfWeek.of(it.dayOfWeek) to (it.avgMs / 1000).toInt() }

        // Streak calculation. A streak remains alive through yesterday so the
        // badge does not reset before today's alarm has had a chance to fire.
        val dates = dao.dismissDates()
        val streak = WakeStreakCalculator.calculate(dates)

        return AlarmStats(
            totalDismissed = dismissed,
            totalSnoozed = snoozed,
            totalSkipped = skipped,
            totalMissed = missed,
            averageDismissTimeSec = (avgDismissMs / 1000).toInt(),
            snoozeRate = if (total > 0) (withSnooze * 100 / total).coerceIn(0, 100) else 0,
            currentStreak = streak.currentDays,
            bestStreak = streak.bestDays,
            streakIncludesToday = streak.includesToday,
            nextStreakGoal = streak.nextGoal,
            alarmsThisWeek = thisWeek,
            dayOfWeekCounts = dowCounts,
            dayOfWeekAvgResponseSec = dowAvg
        )
    }

    suspend fun avgSnoozeCountForAlarm(alarmId: Long, lookbackDays: Int = 14): Double? {
        val sinceMs = System.currentTimeMillis() - lookbackDays * 86_400_000L
        return dao.avgSnoozeCountForAlarm(alarmId, sinceMs)
    }

    data class PerAlarmStats(
        val fireCount: Int,
        val avgSnoozesPerFire: Double,
        val avgDismissTimeSec: Long,
        val missedCount: Int
    )

    suspend fun getPerAlarmStats(alarmId: Long, lookbackDays: Int = 30): PerAlarmStats {
        val sinceMs = System.currentTimeMillis() - lookbackDays * 86_400_000L
        return PerAlarmStats(
            fireCount = dao.countForAlarm(alarmId, sinceMs),
            avgSnoozesPerFire = dao.avgSnoozeCountForAlarm(alarmId, sinceMs) ?: 0.0,
            avgDismissTimeSec = (dao.avgDismissTimeForAlarm(alarmId, sinceMs) ?: 0L) / 1000,
            missedCount = dao.missedCountForAlarm(alarmId, sinceMs)
        )
    }

    suspend fun clearHistory() = dao.deleteAll()

    companion object {
        /** Stats never reach further back than this. */
        const val RETENTION_MS = 365L * 24L * 60L * 60L * 1000L

        /** Hard ceiling for a user who fires far more alarms than a year's worth. */
        const val MAX_ROWS = 5_000
    }
}
