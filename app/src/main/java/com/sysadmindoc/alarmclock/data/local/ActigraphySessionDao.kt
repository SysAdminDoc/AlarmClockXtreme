package com.sysadmindoc.alarmclock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import kotlinx.coroutines.flow.Flow

@Dao
interface ActigraphySessionDao {
    @Insert
    suspend fun insert(session: ActigraphySession): Long

    @Query("SELECT * FROM actigraphy_sessions ORDER BY endedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<ActigraphySession>>

    @Query("SELECT * FROM actigraphy_sessions ORDER BY endedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<ActigraphySession>

    @Query("SELECT * FROM actigraphy_sessions WHERE startedAt >= :sinceMs ORDER BY startedAt DESC")
    suspend fun getSince(sinceMs: Long): List<ActigraphySession>

    @Query("DELETE FROM actigraphy_sessions WHERE endedAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}
