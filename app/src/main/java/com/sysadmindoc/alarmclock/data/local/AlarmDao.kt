package com.sysadmindoc.alarmclock.data.local

import androidx.room.*
import com.sysadmindoc.alarmclock.data.model.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY sortOrder ASC, hour ASC, minute ASC, id ASC")
    fun observeAll(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY nextTriggerTime ASC")
    fun observeEnabled(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabled(): List<Alarm>

    @Query("SELECT * FROM alarms ORDER BY sortOrder ASC, hour ASC, minute ASC, id ASC")
    suspend fun getAll(): List<Alarm>

    // Only consider alarms with a real future trigger so the "next alarm" surface
    // (notification, widget, dashboard) doesn't latch onto a stale trigger from a
    // disabled-then-re-enabled alarm whose nextTriggerTime hasn't been recomputed yet.
    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND nextTriggerTime > 0 ORDER BY nextTriggerTime ASC LIMIT 1")
    suspend fun getNextAlarm(): Alarm?

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND nextTriggerTime > 0 ORDER BY nextTriggerTime ASC LIMIT 1")
    fun observeNextAlarm(): Flow<Alarm?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: Alarm): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<Alarm>): List<Long>

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alarms SET isEnabled = :enabled, nextTriggerTime = :nextTrigger WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, nextTrigger: Long)

    @Query("UPDATE alarms SET nextTriggerTime = :nextTrigger WHERE id = :id")
    suspend fun updateNextTrigger(id: Long, nextTrigger: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM alarms")
    suspend fun maxSortOrder(): Int

    @Query("UPDATE alarms SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun updateSortOrders(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id ->
            updateSortOrder(id, (index + 1) * SORT_ORDER_STEP)
        }
    }

    @Transaction
    suspend fun insertAllWithStableOrder(alarms: List<Alarm>): List<Long> {
        val start = maxSortOrder()
        return insertAll(
            alarms.mapIndexed { index, alarm ->
                alarm.copy(sortOrder = start + (index + 1) * SORT_ORDER_STEP)
            }
        )
    }

    companion object {
        const val SORT_ORDER_STEP = 1_000
    }
}
