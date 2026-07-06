package com.sysadmindoc.alarmclock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sysadmindoc.alarmclock.data.local.entity.SnoreEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreEventDao {
    @Insert
    suspend fun insertAll(events: List<SnoreEvent>): List<Long>

    @Query("SELECT * FROM snore_events ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 12): Flow<List<SnoreEvent>>

    @Query("SELECT * FROM snore_events ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 12): List<SnoreEvent>

    @Query("DELETE FROM snore_events WHERE endedAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}
