package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.actigraphy.ActigraphySessionSummary
import com.sysadmindoc.alarmclock.data.local.ActigraphySessionDao
import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActigraphyRepository @Inject constructor(
    private val dao: ActigraphySessionDao
) {
    fun observeRecent(limit: Int = 10): Flow<List<ActigraphySession>> = dao.observeRecent(limit)

    suspend fun record(
        alarmId: Long,
        startedAt: Long,
        endedAt: Long,
        targetTime: Long,
        firedEarly: Boolean,
        summary: ActigraphySessionSummary
    ): Long {
        val session = ActigraphySession(
            alarmId = alarmId,
            startedAt = startedAt,
            endedAt = endedAt,
            targetTime = targetTime,
            totalMinutes = summary.totalMinutes,
            awakeMinutes = summary.awakeMinutes,
            lightMinutes = summary.lightMinutes,
            deepMinutes = summary.deepMinutes,
            averageSleepIndex = summary.averageSleepIndex,
            firedEarly = firedEarly,
            algorithm = summary.algorithm
        )
        val id = dao.insert(session)
        dao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
        return id
    }

    private companion object {
        const val RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
