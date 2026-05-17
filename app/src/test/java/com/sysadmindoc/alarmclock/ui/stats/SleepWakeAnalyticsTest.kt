package com.sysadmindoc.alarmclock.ui.stats

import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SleepWakeAnalyticsTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun pairsSleepEndingDateWithWakeFriction() {
        val today = LocalDate.of(2026, 5, 17)
        val events = listOf(
            dismissed(
                firedAt = millis("2026-05-16T07:00:00"),
                responseMs = 180_000,
                snoozeCount = 2,
                retryCount = 3,
                solveMs = 90_000
            ),
            snoozed(firedAt = millis("2026-05-16T07:05:00")),
            dismissed(
                firedAt = millis("2026-05-17T07:00:00"),
                responseMs = 60_000,
                snoozeCount = 0,
                retryCount = 0,
                solveMs = 30_000
            )
        )
        val sleeps = listOf(
            sleep(start = "2026-05-15T23:30:00", end = "2026-05-16T05:00:00"),
            sleep(start = "2026-05-16T22:30:00", end = "2026-05-17T06:30:00")
        )

        val analytics = buildSleepWakeAnalytics(
            events = events,
            sleepSessions = sleeps,
            zoneId = zone,
            today = today,
            days = 2
        )

        assertEquals(2, analytics.points.size)
        assertEquals(405L, analytics.averageSleepMinutes)
        assertEquals(120, analytics.averageResponseSec)
        assertEquals(2, analytics.totalSnoozes)
        assertEquals(3, analytics.totalChallengeRetries)

        val shortSleepDay = analytics.points.first { it.date == LocalDate.of(2026, 5, 16) }
        assertEquals(330L, shortSleepDay.sleepMinutes)
        assertEquals(180, shortSleepDay.averageResponseSec)
        assertEquals(2, shortSleepDay.snoozeCount)
        assertEquals(3, shortSleepDay.challengeRetryCount)
        assertEquals(90, shortSleepDay.averageChallengeSolveSec)

        val restedDay = analytics.points.first { it.date == LocalDate.of(2026, 5, 17) }
        assertEquals(480L, restedDay.sleepMinutes)
        assertEquals(60, restedDay.averageResponseSec)
        assertEquals(120, analytics.responseDeltaAfterShortSleepSec)
        assertTrue(analytics.hasSleepWakeCorrelation)
    }

    @Test
    fun keepsWakeOnlyAnalyticsWhenHealthConnectHasNoSessions() {
        val analytics = buildSleepWakeAnalytics(
            events = listOf(
                dismissed(
                    firedAt = millis("2026-05-17T07:00:00"),
                    responseMs = 45_000,
                    snoozeCount = 1,
                    retryCount = 2
                )
            ),
            sleepSessions = emptyList(),
            zoneId = zone,
            today = LocalDate.of(2026, 5, 17),
            days = 1
        )

        assertTrue(analytics.hasAnyData)
        assertEquals(null, analytics.averageSleepMinutes)
        assertEquals(45, analytics.averageResponseSec)
        assertEquals(1, analytics.totalSnoozes)
        assertEquals(2, analytics.totalChallengeRetries)
        assertEquals(false, analytics.hasSleepWakeCorrelation)
        assertNotNull(analytics.points.single().averageResponseSec)
    }

    private fun dismissed(
        firedAt: Long,
        responseMs: Long,
        snoozeCount: Int,
        retryCount: Int,
        solveMs: Long = 0L
    ): AlarmEvent {
        return AlarmEvent(
            alarmId = firedAt,
            scheduledTime = firedAt,
            firedAt = firedAt,
            action = AlarmEvent.ACTION_DISMISSED,
            actionAt = firedAt + responseMs,
            challengeSolveTimeMs = solveMs,
            challengeRetryCount = retryCount,
            snoozeCount = snoozeCount,
            dayOfWeek = 1
        )
    }

    private fun snoozed(firedAt: Long): AlarmEvent {
        return AlarmEvent(
            alarmId = firedAt,
            scheduledTime = firedAt,
            firedAt = firedAt,
            action = AlarmEvent.ACTION_SNOOZED,
            actionAt = firedAt + 10_000,
            dayOfWeek = 1
        )
    }

    private fun sleep(start: String, end: String): HealthConnectSleepSession {
        val startMs = millis(start)
        val endMs = millis(end)
        return HealthConnectSleepSession(
            startMillis = startMs,
            endMillis = endMs,
            durationMinutes = (endMs - startMs) / 60_000
        )
    }

    private fun millis(dateTime: String): Long {
        return LocalDateTime.parse(dateTime)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }
}
