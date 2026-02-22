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
    val alarmsThisWeek: Int = 0,
    val dayOfWeekCounts: Map<DayOfWeek, Int> = emptyMap(),
    val dayOfWeekAvgResponseSec: Map<DayOfWeek, Int> = emptyMap()
)

@Singleton
class AlarmEventRepository @Inject constructor(
    private val dao: AlarmEventDao
) {
    fun observeRecent(limit: Int = 50): Flow<List<AlarmEvent>> = dao.observeRecent(limit)

    suspend fun record(event: AlarmEvent): Long = dao.insert(event)

    suspend fun getStats(): AlarmStats {
        val dismissed = dao.countByAction(AlarmEvent.ACTION_DISMISSED)
        val snoozed = dao.countByAction(AlarmEvent.ACTION_SNOOZED)
        val skipped = dao.countByAction(AlarmEvent.ACTION_SKIPPED)
        val missed = dao.countByAction(AlarmEvent.ACTION_MISSED)
        val avgDismissMs = dao.averageDismissTimeMs() ?: 0L
        val withSnooze = dao.countWithSnooze()
        val total = dismissed + snoozed + missed

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

        // Streak calculation
        val dates = dao.dismissDates()
        val streak = calculateStreak(dates)

        return AlarmStats(
            totalDismissed = dismissed,
            totalSnoozed = snoozed,
            totalSkipped = skipped,
            totalMissed = missed,
            averageDismissTimeSec = (avgDismissMs / 1000).toInt(),
            snoozeRate = if (total > 0) (withSnooze * 100 / total) else 0,
            currentStreak = streak,
            alarmsThisWeek = thisWeek,
            dayOfWeekCounts = dowCounts,
            dayOfWeekAvgResponseSec = dowAvg
        )
    }

    suspend fun clearHistory() = dao.deleteAll()

    private fun calculateStreak(dateStrings: List<String>): Int {
        if (dateStrings.isEmpty()) return 0
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        var streak = 0
        var expected = LocalDate.now()

        for (dateStr in dateStrings) {
            try {
                val date = LocalDate.parse(dateStr, formatter)
                if (date == expected) {
                    streak++
                    expected = expected.minusDays(1)
                } else if (date.isBefore(expected)) {
                    break
                }
            } catch (_: Exception) { break }
        }
        return streak
    }
}
