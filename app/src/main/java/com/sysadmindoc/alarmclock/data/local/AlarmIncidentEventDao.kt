package com.sysadmindoc.alarmclock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmIncidentEventDao {
    @Insert
    suspend fun insert(event: AlarmIncidentEvent): Long

    @Query("SELECT * FROM alarm_incident_events ORDER BY eventAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 25): Flow<List<AlarmIncidentEvent>>

    @Query("SELECT * FROM alarm_incident_events ORDER BY eventAt DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 25): List<AlarmIncidentEvent>

    @Query("DELETE FROM alarm_incident_events WHERE eventAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)

    @Query(
        """
        DELETE FROM alarm_incident_events
        WHERE id NOT IN (
            SELECT id FROM alarm_incident_events
            ORDER BY eventAt DESC, id DESC
            LIMIT :maxRows
        )
        """
    )
    suspend fun trimToLatest(maxRows: Int)

    @Query("DELETE FROM alarm_incident_events")
    suspend fun deleteAll()
}
