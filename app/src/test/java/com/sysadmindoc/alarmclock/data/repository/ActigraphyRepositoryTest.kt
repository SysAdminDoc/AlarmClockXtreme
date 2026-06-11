package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.actigraphy.ActigraphySessionSummary
import com.sysadmindoc.alarmclock.data.local.ActigraphySessionDao
import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActigraphyRepositoryTest {

    @Test
    fun recordPrunesSessionsOlderThanThirtyDays() = runBlocking {
        val dao = FakeActigraphySessionDao()
        val repository = ActigraphyRepository(dao)
        val beforeRecord = System.currentTimeMillis()

        val id = repository.record(
            alarmId = 7L,
            startedAt = beforeRecord - 10 * 60_000L,
            endedAt = beforeRecord,
            targetTime = beforeRecord + 5 * 60_000L,
            firedEarly = false,
            summary = ActigraphySessionSummary(
                totalMinutes = 10,
                awakeMinutes = 1,
                lightMinutes = 4,
                deepMinutes = 5,
                averageSleepIndex = 0.42f
            )
        )
        val afterRecord = System.currentTimeMillis()
        val retentionMs = 30L * 24L * 60L * 60L * 1000L
        val deletedBefore = dao.deletedBeforeMs

        assertEquals(1L, id)
        assertEquals(1, dao.inserted.size)
        assertEquals(7L, dao.inserted.single().alarmId)
        assertNotNull(deletedBefore)
        assertTrue(deletedBefore!! >= beforeRecord - retentionMs)
        assertTrue(deletedBefore <= afterRecord - retentionMs)
    }

    private class FakeActigraphySessionDao : ActigraphySessionDao {
        val inserted = mutableListOf<ActigraphySession>()
        var deletedBeforeMs: Long? = null
        private var nextId = 1L

        override suspend fun insert(session: ActigraphySession): Long {
            val id = nextId++
            inserted += session.copy(id = id)
            return id
        }

        override fun observeRecent(limit: Int): Flow<List<ActigraphySession>> {
            return flowOf(recent(limit))
        }

        override suspend fun getRecent(limit: Int): List<ActigraphySession> {
            return recent(limit)
        }

        override suspend fun deleteOlderThan(beforeMs: Long) {
            deletedBeforeMs = beforeMs
            inserted.removeAll { it.endedAt < beforeMs }
        }

        private fun recent(limit: Int): List<ActigraphySession> {
            return inserted.sortedByDescending { it.endedAt }.take(limit)
        }
    }
}
