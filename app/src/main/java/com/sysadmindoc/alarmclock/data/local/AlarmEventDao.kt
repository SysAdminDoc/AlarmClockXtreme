package com.sysadmindoc.alarmclock.data.local

import androidx.room.*
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmEventDao {

    @Insert
    suspend fun insert(event: AlarmEvent): Long

    @Query("SELECT * FROM alarm_events ORDER BY firedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<AlarmEvent>>

    @Query("SELECT * FROM alarm_events ORDER BY firedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<AlarmEvent>

    @Query("SELECT * FROM alarm_events WHERE firedAt >= :sinceMs ORDER BY firedAt DESC")
    suspend fun getSince(sinceMs: Long): List<AlarmEvent>

    // Stats: total counts by action
    @Query("SELECT COUNT(*) FROM alarm_events WHERE action = :action")
    suspend fun countByAction(action: String): Int

    // Stats: average response time for dismissals
    @Query("SELECT AVG(actionAt - firedAt) FROM alarm_events WHERE action = 'DISMISSED' AND actionAt > 0 AND firedAt > 0")
    suspend fun averageDismissTimeMs(): Long?

    // Stats: snooze rate (% of events that included at least one snooze)
    @Query("SELECT COUNT(*) FROM alarm_events WHERE action IN ('DISMISSED', 'MISSED', 'SKIPPED') AND snoozeCount > 0")
    suspend fun countWithSnooze(): Int

    // Stats: events per day of week (1=Monday..7=Sunday)
    @Query("SELECT dayOfWeek, COUNT(*) as cnt FROM alarm_events GROUP BY dayOfWeek ORDER BY dayOfWeek")
    suspend fun countByDayOfWeek(): List<DayOfWeekCount>

    // Stats: average response time per day of week
    @Query("SELECT dayOfWeek, AVG(actionAt - firedAt) as avgMs FROM alarm_events WHERE action = 'DISMISSED' AND actionAt > 0 GROUP BY dayOfWeek ORDER BY dayOfWeek")
    suspend fun avgResponseByDayOfWeek(): List<DayOfWeekAvg>

    // Stats: events in last N days
    @Query("SELECT COUNT(*) FROM alarm_events WHERE firedAt > :sinceMs")
    suspend fun countSince(sinceMs: Long): Int

    // Streak: consecutive days with at least one dismissed alarm
    @Query("SELECT DISTINCT date(firedAt / 1000, 'unixepoch', 'localtime') as d FROM alarm_events WHERE action = 'DISMISSED' ORDER BY d DESC")
    suspend fun dismissDates(): List<String>

    @Query("SELECT AVG(snoozeCount) FROM alarm_events WHERE alarmId = :alarmId AND action IN ('DISMISSED', 'MISSED') AND firedAt > :sinceMs")
    suspend fun avgSnoozeCountForAlarm(alarmId: Long, sinceMs: Long): Double?

    @Query("SELECT COUNT(*) FROM alarm_events WHERE alarmId = :alarmId AND firedAt > :sinceMs")
    suspend fun countForAlarm(alarmId: Long, sinceMs: Long): Int

    @Query("SELECT AVG(actionAt - firedAt) FROM alarm_events WHERE alarmId = :alarmId AND action = 'DISMISSED' AND actionAt > 0 AND firedAt > :sinceMs")
    suspend fun avgDismissTimeForAlarm(alarmId: Long, sinceMs: Long): Long?

    @Query("SELECT COUNT(*) FROM alarm_events WHERE alarmId = :alarmId AND action = 'MISSED' AND firedAt > :sinceMs")
    suspend fun missedCountForAlarm(alarmId: Long, sinceMs: Long): Int

    @Query("DELETE FROM alarm_events")
    suspend fun deleteAll()
}

data class DayOfWeekCount(val dayOfWeek: Int, val cnt: Int)
data class DayOfWeekAvg(val dayOfWeek: Int, val avgMs: Long)
