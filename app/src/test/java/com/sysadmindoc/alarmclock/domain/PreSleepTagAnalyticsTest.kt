package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.local.entity.PreSleepTagEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PreSleepTagAnalyticsTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun earlyMorningTagBelongsToPreviousNight() {
        val millis = LocalDateTime.of(2026, 7, 6, 2, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val tagDate = PreSleepTagAnalytics.tagDateFor(millis, zone)

        assertEquals("2026-07-05", tagDate.toString())
    }

    @Test
    fun correlationsCompareTaggedRestlessMinutesAgainstBaseline() {
        val tags = listOf(
            PreSleepTagEntry("2026-07-01", PreSleepTags.CAFFEINE, loggedAt = 1L),
            PreSleepTagEntry("2026-07-02", PreSleepTags.STRESS, loggedAt = 2L),
            PreSleepTagEntry("2026-07-03", PreSleepTags.CAFFEINE, loggedAt = 3L)
        )
        val sessions = listOf(
            session("2026-07-01T23:00:00", awake = 20, light = 30),
            session("2026-07-02T23:00:00", awake = 5, light = 15),
            session("2026-07-03T23:00:00", awake = 25, light = 35)
        )

        val correlations = PreSleepTagAnalytics.buildCorrelations(tags, sessions, zone)
        val caffeine = correlations.first { it.key == PreSleepTags.CAFFEINE }
        val alcohol = correlations.first { it.key == PreSleepTags.ALCOHOL }

        assertEquals(2, caffeine.loggedNights)
        assertEquals(2, caffeine.nightsWithSessions)
        assertEquals(55, caffeine.averageRestlessMinutes)
        assertEquals(43, caffeine.baselineRestlessMinutes)
        assertEquals(12, caffeine.deltaRestlessMinutes)
        assertEquals(0, alcohol.loggedNights)
    }

    private fun session(
        startedAt: String,
        awake: Int,
        light: Int
    ): ActigraphySession {
        val startMillis = LocalDateTime.parse(startedAt).atZone(zone).toInstant().toEpochMilli()
        return ActigraphySession(
            alarmId = 0L,
            startedAt = startMillis,
            endedAt = startMillis + 8L * 60L * 60L * 1000L,
            targetTime = startMillis,
            totalMinutes = 480,
            awakeMinutes = awake,
            lightMinutes = light,
            deepMinutes = 480 - awake - light,
            averageSleepIndex = 0.2f,
            firedEarly = false,
            algorithm = "test",
            decisionReason = "test",
            observedMinutesBeforeDecision = 480,
            smartWakeMode = "SONAR"
        )
    }
}
