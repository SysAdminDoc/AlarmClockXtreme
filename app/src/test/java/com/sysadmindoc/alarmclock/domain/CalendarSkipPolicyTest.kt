package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSkipPolicyTest {

    private val losAngeles = ZoneId.of("America/Los_Angeles")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val monday = LocalDate.of(2027, 1, 11)
    private val tuesday = LocalDate.of(2027, 1, 12)
    private val wednesday = LocalDate.of(2027, 1, 13)
    private val thursday = LocalDate.of(2027, 1, 14)

    @Test
    fun allDayEventSkipsItsOwnDateWestOfUtc() {
        // Stored as Tuesday 00:00Z to Wednesday 00:00Z, which is Monday 16:00
        // to Tuesday 16:00 in Los Angeles. Read through the device zone it
        // would land on Monday.
        val holiday = allDay("#holiday", tuesday, wednesday)

        assertTrue(CalendarSkipPolicy.shouldSkip(tuesday, "#holiday", listOf(holiday), losAngeles))
        assertFalse(CalendarSkipPolicy.shouldSkip(monday, "#holiday", listOf(holiday), losAngeles))
        assertFalse(CalendarSkipPolicy.shouldSkip(wednesday, "#holiday", listOf(holiday), losAngeles))
    }

    @Test
    fun allDayEventSkipsItsOwnDateEastOfUtc() {
        // Tuesday 09:00 to Wednesday 09:00 in Tokyo; Wednesday morning must
        // still ring.
        val holiday = allDay("#holiday", tuesday, wednesday)

        assertTrue(CalendarSkipPolicy.shouldSkip(tuesday, "#holiday", listOf(holiday), tokyo))
        assertFalse(CalendarSkipPolicy.shouldSkip(wednesday, "#holiday", listOf(holiday), tokyo))
        assertFalse(CalendarSkipPolicy.shouldSkip(monday, "#holiday", listOf(holiday), tokyo))
    }

    @Test
    fun multiDayAllDayEventSkipsEveryDateItCovers() {
        val trip = allDay("Ski trip #holiday", monday, thursday)

        listOf(monday, tuesday, wednesday).forEach { date ->
            assertTrue("$date", CalendarSkipPolicy.shouldSkip(date, "#holiday", listOf(trip), losAngeles))
        }
        assertFalse(CalendarSkipPolicy.shouldSkip(thursday, "#holiday", listOf(trip), losAngeles))
        assertFalse(
            CalendarSkipPolicy.shouldSkip(monday.minusDays(1), "#holiday", listOf(trip), losAngeles)
        )
    }

    @Test
    fun multiDayTimedEventSkipsEveryLocalDayItTouches() {
        val conference = timed(
            "Conference #holiday",
            LocalDateTime.of(2027, 1, 11, 20, 0),
            LocalDateTime.of(2027, 1, 13, 8, 0),
            tokyo
        )

        listOf(monday, tuesday, wednesday).forEach { date ->
            assertTrue("$date", CalendarSkipPolicy.shouldSkip(date, "#holiday", listOf(conference), tokyo))
        }
        assertFalse(CalendarSkipPolicy.shouldSkip(thursday, "#holiday", listOf(conference), tokyo))
    }

    @Test
    fun timedEventEndingAtMidnightDoesNotTouchTheNextDay() {
        val lateShift = timed(
            "#holiday",
            LocalDateTime.of(2027, 1, 11, 22, 0),
            LocalDateTime.of(2027, 1, 12, 0, 0),
            losAngeles
        )

        assertTrue(CalendarSkipPolicy.shouldSkip(monday, "#holiday", listOf(lateShift), losAngeles))
        assertFalse(CalendarSkipPolicy.shouldSkip(tuesday, "#holiday", listOf(lateShift), losAngeles))
    }

    @Test
    fun allDayEventWithNoLengthStillCoversItsDate() {
        val marker = allDay("#holiday", tuesday, tuesday)

        assertTrue(CalendarSkipPolicy.shouldSkip(tuesday, "#holiday", listOf(marker), tokyo))
        assertFalse(CalendarSkipPolicy.shouldSkip(wednesday, "#holiday", listOf(marker), tokyo))
    }

    @Test
    fun tagMatchIgnoresCaseAndSurroundingSpaces() {
        val event = allDay("Company HOLIDAY party", tuesday, wednesday)

        assertTrue(CalendarSkipPolicy.shouldSkip(tuesday, "holiday", listOf(event), losAngeles))
        assertTrue(CalendarSkipPolicy.shouldSkip(tuesday, "  Holiday ", listOf(event), losAngeles))
        assertFalse(CalendarSkipPolicy.shouldSkip(tuesday, "vacation", listOf(event), losAngeles))
    }

    @Test
    fun blankTagNeverSkips() {
        val event = allDay("Anything at all", tuesday, wednesday)

        assertFalse(CalendarSkipPolicy.isActive(""))
        assertFalse(CalendarSkipPolicy.isActive("   "))
        assertTrue(CalendarSkipPolicy.isActive("#off"))
        assertFalse(CalendarSkipPolicy.shouldSkip(tuesday, "", listOf(event), losAngeles))
        assertFalse(CalendarSkipPolicy.shouldSkip(tuesday, "   ", listOf(event), losAngeles))
    }

    @Test
    fun noEventsNeverSkips() {
        assertFalse(CalendarSkipPolicy.shouldSkip(tuesday, "#holiday", emptyList(), losAngeles))
    }

    @Test
    fun queryWindowCoversBothTheLocalAndTheUtcDay() {
        val west = CalendarSkipPolicy.queryWindow(tuesday, losAngeles)
        assertEquals(millis(tuesday.atStartOfDay(), ZoneOffset.UTC), west.first)
        assertEquals(millis(wednesday.atStartOfDay(), losAngeles), west.second)

        val east = CalendarSkipPolicy.queryWindow(tuesday, tokyo)
        assertEquals(millis(tuesday.atStartOfDay(), tokyo), east.first)
        assertEquals(millis(wednesday.atStartOfDay(), ZoneOffset.UTC), east.second)
    }

    private fun allDay(title: String, first: LocalDate, endExclusive: LocalDate) = CalendarEvent(
        id = 1L,
        title = title,
        startTime = millis(first.atStartOfDay(), ZoneOffset.UTC),
        endTime = millis(endExclusive.atStartOfDay(), ZoneOffset.UTC),
        allDay = true,
        location = "",
        calendarColor = 0
    )

    private fun timed(
        title: String,
        start: LocalDateTime,
        end: LocalDateTime,
        zone: ZoneId
    ) = CalendarEvent(
        id = 2L,
        title = title,
        startTime = millis(start, zone),
        endTime = millis(end, zone),
        allDay = false,
        location = "",
        calendarColor = 0
    )

    private fun millis(at: LocalDateTime, zone: ZoneId): Long =
        at.atZone(zone).toInstant().toEpochMilli()
}
