package com.sysadmindoc.alarmclock.data.repository

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import com.sysadmindoc.alarmclock.data.local.AlarmIncidentEventDao
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmIncidentRepositoryTest {

    @Test
    fun recordPrunesOldAndExcessIncidentRows() = runBlocking {
        val dao = FakeAlarmIncidentEventDao()
        val repository = AlarmIncidentRepository(dao, MinimalContext())
        val beforeRecord = System.currentTimeMillis()

        val id = repository.record(
            alarmId = 7L,
            fireId = "",
            scheduledAt = beforeRecord - 90_000L,
            eventAt = beforeRecord,
            type = AlarmIncidentEvent.TYPE_SCHEDULE,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "SET_ALARM_CLOCK",
            source = "test"
        )
        val afterRecord = System.currentTimeMillis()
        val retentionMs = 30L * 24L * 60L * 60L * 1000L

        assertEquals(1L, id)
        assertEquals(1, dao.inserted.size)
        assertTrue(dao.inserted.single().fireId.startsWith("alarm-7-"))
        assertNotNull(dao.deletedBeforeMs)
        assertTrue(dao.deletedBeforeMs!! >= beforeRecord - retentionMs)
        assertTrue(dao.deletedBeforeMs!! <= afterRecord - retentionMs)
        assertEquals(100, dao.trimmedToMaxRows)
    }

    @Test
    fun recordFailureIsSwallowedAndDoesNotPrune() = runBlocking {
        val dao = FakeAlarmIncidentEventDao(failInserts = true)
        val repository = AlarmIncidentRepository(dao, MinimalContext())

        val id = repository.record(
            alarmId = 7L,
            fireId = "alarm-7-test",
            scheduledAt = 1_000L,
            eventAt = 1_500L,
            type = AlarmIncidentEvent.TYPE_AUDIO,
            status = AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = "MEDIA_PLAYER_FAILED",
            source = "test"
        )

        assertNull(id)
        assertTrue(dao.inserted.isEmpty())
        assertNull(dao.deletedBeforeMs)
        assertNull(dao.trimmedToMaxRows)
    }

    @Test
    fun clearHistoryDeletesOnlyIncidentRepositoryRows() = runBlocking {
        val dao = FakeAlarmIncidentEventDao()
        val repository = AlarmIncidentRepository(dao, MinimalContext())
        dao.insert(
            AlarmIncidentEvent(
                fireId = "alarm-7-test",
                alarmId = 7L,
                scheduledAt = 1_000L,
                eventAt = 1_500L,
                elapsedMs = 500L,
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "MEDIA_PLAYER_FAILED",
                source = "test",
                sdkInt = 35,
                standbyBucket = "ACTIVE_10",
                exactAlarmAllowed = "TRUE",
                notificationPermissionGranted = "TRUE",
                fullScreenIntentAllowed = "TRUE",
                batteryOptimizationsIgnored = "TRUE",
                algorithmVersion = AlarmIncidentEvent.VALUE_NONE
            )
        )

        repository.clearHistory()

        assertTrue(dao.inserted.isEmpty())
        assertTrue(dao.deleteAllCalled)
        assertFalse(dao.deleteOlderThanCalled)
    }

    private class MinimalContext : ContextWrapper(null) {
        override fun getPackageName(): String = "com.sysadmindoc.alarmclock"

        override fun getSystemService(name: String): Any? = null

        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return PackageManager.PERMISSION_GRANTED
        }
    }

    private class FakeAlarmIncidentEventDao(
        private val failInserts: Boolean = false
    ) : AlarmIncidentEventDao {
        val inserted = mutableListOf<AlarmIncidentEvent>()
        var deletedBeforeMs: Long? = null
        var trimmedToMaxRows: Int? = null
        var deleteAllCalled: Boolean = false
        var deleteOlderThanCalled: Boolean = false
        private var nextId = 1L

        override suspend fun insert(event: AlarmIncidentEvent): Long {
            if (failInserts) error("insert failed")
            val id = nextId++
            inserted += event.copy(id = id)
            return id
        }

        override fun observeRecent(limit: Int): Flow<List<AlarmIncidentEvent>> {
            return flowOf(recent(limit))
        }

        override suspend fun getRecent(limit: Int): List<AlarmIncidentEvent> {
            return recent(limit)
        }

        override suspend fun countByOccurrenceAndType(
            alarmId: Long,
            scheduledAt: Long,
            type: String
        ): Int = inserted.count {
            it.alarmId == alarmId && it.scheduledAt == scheduledAt && it.type == type
        }

        override suspend fun deleteOlderThan(beforeMs: Long) {
            deleteOlderThanCalled = true
            deletedBeforeMs = beforeMs
            inserted.removeAll { it.eventAt < beforeMs }
        }

        override suspend fun trimToLatest(maxRows: Int) {
            trimmedToMaxRows = maxRows
            val keepIds = recent(maxRows).map { it.id }.toSet()
            inserted.removeAll { it.id !in keepIds }
        }

        override suspend fun deleteAll() {
            deleteAllCalled = true
            inserted.clear()
        }

        private fun recent(limit: Int): List<AlarmIncidentEvent> {
            return inserted.sortedWith(
                compareByDescending<AlarmIncidentEvent> { it.eventAt }
                    .thenByDescending { it.id }
            ).take(limit)
        }
    }
}
