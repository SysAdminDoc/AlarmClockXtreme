package com.sysadmindoc.alarmclock.ui.alarmfiring

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeTimeTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun selectedTimeLaterTodayStaysOnToday() {
        val now = Instant.parse("2026-08-12T06:30:00Z").toEpochMilli()

        val target = nextSnoozeAtMillis(now, hour = 7, minute = 15, zoneId = zone)

        assertEquals("2026-08-12T07:15:00Z", Instant.ofEpochMilli(target).toString())
    }

    @Test
    fun selectedTimeAlreadyPassedMovesToTomorrow() {
        val now = Instant.parse("2026-08-12T23:45:00Z").toEpochMilli()

        val target = nextSnoozeAtMillis(now, hour = 7, minute = 0, zoneId = zone)

        assertEquals("2026-08-13T07:00:00Z", Instant.ofEpochMilli(target).toString())
    }
}
