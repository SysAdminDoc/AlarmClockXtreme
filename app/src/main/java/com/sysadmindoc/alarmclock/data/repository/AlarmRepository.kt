package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.local.AlarmDao
import com.sysadmindoc.alarmclock.data.model.Alarm
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dao: AlarmDao
) {
    fun observeAll(): Flow<List<Alarm>> = dao.observeAll()
    fun observeEnabled(): Flow<List<Alarm>> = dao.observeEnabled()
    fun observeNextAlarm(): Flow<Alarm?> = dao.observeNextAlarm()

    suspend fun getById(id: Long): Alarm? = dao.getById(id)
    suspend fun getEnabled(): List<Alarm> = dao.getEnabled()
    suspend fun getNextAlarm(): Alarm? = dao.getNextAlarm()
    suspend fun getAll(): List<Alarm> = dao.getAll()

    suspend fun save(alarm: Alarm): Long = dao.insert(alarm)
    suspend fun update(alarm: Alarm) = dao.update(alarm)
    suspend fun delete(alarm: Alarm) = dao.delete(alarm)
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean, nextTrigger: Long) =
        dao.setEnabled(id, enabled, nextTrigger)

    suspend fun updateNextTrigger(id: Long, nextTrigger: Long) =
        dao.updateNextTrigger(id, nextTrigger)
}
