package com.sysadmindoc.alarmclock.data.repository

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The dashboard's calendar strip used to hardcode "h:mm a", so a phone set to
 * 24-hour time still read "6:30 AM" there while every other surface read
 * "06:30". The row takes the preference now because it has no settings of its
 * own to read.
 */
class CalendarEventTimeFormatTest {

    private val zone = ZoneId.of("UTC")
    private var previousZone: TimeZone? = null
    private var previousLocale: Locale? = null

    @Before
    fun pinZoneAndLocale() {
        previousZone = TimeZone.getDefault()
        previousLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreZoneAndLocale() {
        previousZone?.let { TimeZone.setDefault(it) }
        previousLocale?.let { Locale.setDefault(it) }
    }

    private fun event(
        start: LocalDateTime,
        end: LocalDateTime,
        allDay: Boolean = false
    ) = CalendarEvent(
        id = 1L,
        title = "Team sync",
        startTime = start.atZone(zone).toInstant().toEpochMilli(),
        endTime = end.atZone(zone).toInstant().toEpochMilli(),
        allDay = allDay,
        location = "",
        calendarColor = 0
    )

    @Test
    fun `24-hour keeps the leading zero and drops the meridiem`() {
        val row = event(LocalDateTime.of(2026, 8, 22, 6, 30), LocalDateTime.of(2026, 8, 22, 7, 15))

        assertEquals("06:30", row.startFormatted(is24Hour = true))
        assertEquals("07:15", row.endFormatted(is24Hour = true))
    }

    @Test
    fun `12-hour still carries the meridiem`() {
        val row = event(LocalDateTime.of(2026, 8, 22, 6, 30), LocalDateTime.of(2026, 8, 22, 13, 5))

        assertEquals("6:30 AM", row.startFormatted(is24Hour = false))
        assertEquals("1:05 PM", row.endFormatted(is24Hour = false))
    }

    @Test
    fun `an all-day event has no time of its own to show`() {
        // The caller names it, because "All day" is copy and this is a data
        // class with no Context.
        val row = event(
            LocalDateTime.of(2026, 8, 22, 0, 0),
            LocalDateTime.of(2026, 8, 23, 0, 0),
            allDay = true
        )

        assertEquals("", row.startFormatted(is24Hour = true))
        assertEquals("", row.endFormatted(is24Hour = false))
    }

    @Test
    fun `midnight and noon are where a hand-rolled hour goes wrong`() {
        val midnight = event(
            LocalDateTime.of(2026, 8, 22, 0, 5),
            LocalDateTime.of(2026, 8, 22, 12, 0)
        )

        assertEquals("12:05 AM", midnight.startFormatted(is24Hour = false))
        assertEquals("12:00 PM", midnight.endFormatted(is24Hour = false))
        assertEquals("00:05", midnight.startFormatted(is24Hour = true))
    }
}
