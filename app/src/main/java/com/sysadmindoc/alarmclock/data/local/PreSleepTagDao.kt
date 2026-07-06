package com.sysadmindoc.alarmclock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sysadmindoc.alarmclock.data.local.entity.PreSleepTagEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PreSleepTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PreSleepTagEntry)

    @Query("DELETE FROM pre_sleep_tag_entries WHERE localDate = :localDate AND tagKey = :tagKey")
    suspend fun delete(localDate: String, tagKey: String)

    @Query("SELECT * FROM pre_sleep_tag_entries WHERE localDate = :localDate ORDER BY tagKey")
    fun observeForDate(localDate: String): Flow<List<PreSleepTagEntry>>

    @Query("SELECT * FROM pre_sleep_tag_entries WHERE localDate >= :fromLocalDate ORDER BY localDate DESC, tagKey")
    suspend fun getSince(fromLocalDate: String): List<PreSleepTagEntry>

    @Query("DELETE FROM pre_sleep_tag_entries WHERE localDate < :beforeLocalDate")
    suspend fun deleteOlderThan(beforeLocalDate: String)
}
