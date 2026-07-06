package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.local.ActigraphySessionDao
import com.sysadmindoc.alarmclock.data.local.PreSleepTagDao
import com.sysadmindoc.alarmclock.data.local.entity.PreSleepTagEntry
import com.sysadmindoc.alarmclock.domain.PreSleepTagAnalytics
import com.sysadmindoc.alarmclock.domain.PreSleepTagCorrelation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreSleepTagRepository @Inject constructor(
    private val dao: PreSleepTagDao,
    private val actigraphySessionDao: ActigraphySessionDao
) {
    fun observeForDate(localDate: LocalDate): Flow<List<PreSleepTagEntry>> {
        return dao.observeForDate(localDate.toString())
    }

    suspend fun setTag(
        localDate: LocalDate,
        tagKey: String,
        selected: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (selected) {
            dao.upsert(
                PreSleepTagEntry(
                    localDate = localDate.toString(),
                    tagKey = tagKey,
                    loggedAt = nowMillis
                )
            )
        } else {
            dao.delete(localDate.toString(), tagKey)
        }
        dao.deleteOlderThan(localDate.minusDays(RETENTION_DAYS).toString())
    }

    suspend fun readCorrelations(
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PreSleepTagCorrelation> {
        val fromDate = today.minusDays(RETENTION_DAYS - 1)
        val tags = dao.getSince(fromDate.toString())
        val fromStartMillis = fromDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val sessions = actigraphySessionDao.getSince(fromStartMillis)
        return PreSleepTagAnalytics.buildCorrelations(tags, sessions, zoneId)
    }

    private companion object {
        const val RETENTION_DAYS = 30L
    }
}
