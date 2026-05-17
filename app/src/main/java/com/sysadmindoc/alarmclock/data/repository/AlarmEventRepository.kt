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

    suspend fun record(event: AlarmEvent): Long = dao.insert(event)

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

    suspend fun clearHistory() = dao.deleteAll()
}
