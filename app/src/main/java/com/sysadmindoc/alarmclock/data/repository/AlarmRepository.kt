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

    suspend fun save(alarm: Alarm): Long {
        val sanitized = alarm.sanitized()
        val ordered = if (sanitized.id == 0L && sanitized.sortOrder == 0) {
            sanitized.copy(sortOrder = nextSortOrder())
        } else {
            sanitized
        }
        return dao.insert(ordered)
    }

    suspend fun importDisabledAtomically(alarms: List<Alarm>): List<Long> =
        dao.insertAllWithStableOrder(
            alarms.map {
                it.copy(id = 0L, isEnabled = false, nextTriggerTime = 0L).sanitized()
            }
        )
    suspend fun update(alarm: Alarm) = dao.update(alarm.sanitized())
    suspend fun delete(alarm: Alarm) = dao.delete(alarm)
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean, nextTrigger: Long) =
        dao.setEnabled(id, enabled, nextTrigger)

    suspend fun updateNextTrigger(id: Long, nextTrigger: Long) =
        dao.updateNextTrigger(id, nextTrigger)

    suspend fun nextSortOrder(): Int = dao.maxSortOrder() + AlarmDao.SORT_ORDER_STEP

    suspend fun updateSortOrders(idsInOrder: List<Long>) =
        dao.updateSortOrders(idsInOrder)
}
