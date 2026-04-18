package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmCalculatorTest {

    private lateinit var calculator: NextAlarmCalculator

    @Before
    fun setup() {
        calculator = NextAlarmCalculator()
    }

    @Test
    fun `calculate returns future time for non-repeating alarm`() {
        val alarm = Alarm(hour = 7, minute = 30, repeatDays = emptySet())
        val result = calculator.calculate(alarm)
        assertTrue("Trigger time should be in the future", result > System.currentTimeMillis())
    }

    @Test
    fun `calculate returns future time for repeating alarm`() {
        val alarm = Alarm(
            hour = 6, minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        val result = calculator.calculate(alarm)
        assertTrue("Trigger time should be in the future", result > System.currentTimeMillis())
    }

    @Test
    fun `calculate with everyday returns within 24 hours`() {
        val now = ZonedDateTime.now()
        val futureTime = now.plusHours(1)
        val alarm = Alarm(
            hour = futureTime.hour, minute = futureTime.minute,
            repeatDays = DayOfWeek.entries.toSet()
        )
        val result = calculator.calculate(alarm)
        val diff = result - System.currentTimeMillis()
        assertTrue("Should be within ~24 hours", diff < 25 * 60 * 60 * 1000L)
        assertTrue("Should be in the future", diff > 0)
    }

    @Test
    fun `calculate with fromTime skips past specified time`() {
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = DayOfWeek.entries.toSet())
        val now = ZonedDateTime.now()
        val result = calculator.calculate(alarm, now)
        assertTrue("Should be after now", result > now.toInstant().toEpochMilli())
    }

    @Test
    fun `formatRemaining returns reasonable string`() {
        val in1Hour = System.currentTimeMillis() + 60 * 60 * 1000L
        val result = calculator.formatRemaining(in1Hour)
        assertFalse("Should not be empty", result.isEmpty())
        assertTrue("Should contain time units", result.contains("h") || result.contains("m"))
    }

    @Test
    fun `formatRemaining for past time returns now`() {
        val pastTime = System.currentTimeMillis() - 60_000
        val result = calculator.formatRemaining(pastTime)
        assertEquals("now", result)
    }

    @Test
    fun `one-shot alarm schedules for tomorrow if time passed`() {
        val now = ZonedDateTime.now()
        val pastTime = now.minusHours(1)
        val alarm = Alarm(hour = pastTime.hour, minute = pastTime.minute, repeatDays = emptySet())
        val result = calculator.calculate(alarm, now)
        val diff = result - now.toInstant().toEpochMilli()
        // Should be ~23 hours from now
        assertTrue("Should be roughly 23h ahead", diff > 22 * 60 * 60 * 1000L)
        assertTrue("Should be less than 25h", diff < 25 * 60 * 60 * 1000L)
    }

    @Test
    fun `formatRemaining shows under-1m for sub-minute deltas`() {
        // Inside the same minute, none of d/h/m would be > 0 — historically this
        // produced a misleading "0m" label. We now render "<1m" instead.
        val in10Sec = System.currentTimeMillis() + 10_000L
        val result = calculator.formatRemaining(in10Sec)
        assertEquals("<1m", result)
    }

    @Test
    fun `formatRemaining drops zero hours but keeps minutes`() {
        // Multi-day diff with zero-hour component should not render "Xd m" with empty hours.
        val twoDaysFiveMin = System.currentTimeMillis() +
                2 * 24 * 60 * 60 * 1000L + 5 * 60 * 1000L
        val result = calculator.formatRemaining(twoDaysFiveMin)
        assertTrue("Should contain 2d", result.contains("2d"))
        assertTrue("Should contain 5m", result.contains("5m"))
    }

    @Test
    fun `specific date in future overrides repeat days`() {
        val now = ZonedDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1)
        val alarm = Alarm(
            hour = 7, minute = 0,
            repeatDays = setOf(DayOfWeek.SUNDAY),
            specificDate = tomorrow.toString()
        )
        val result = calculator.calculate(alarm, now)
        val resultDate = java.time.Instant.ofEpochMilli(result)
            .atZone(now.zone).toLocalDate()
        assertEquals("Specific date should win over repeatDays", tomorrow, resultDate)
    }

    @Test
    fun `specific date in past falls through to repeat days`() {
        val now = ZonedDateTime.now()
        val yesterday = now.toLocalDate().minusDays(1)
        val alarm = Alarm(
            hour = 7, minute = 0,
            repeatDays = DayOfWeek.entries.toSet(),
            specificDate = yesterday.toString()
        )
        val result = calculator.calculate(alarm, now)
        // Expired specificDate must NOT keep firing the alarm forever in the past.
        assertTrue("Past specificDate should fall through to repeatDays scheduling",
            result > now.toInstant().toEpochMilli())
    }

    @Test
    fun `malformed specific date is ignored`() {
        val alarm = Alarm(
            hour = 9, minute = 0,
            repeatDays = DayOfWeek.entries.toSet(),
            specificDate = "not-a-date"
        )
        val result = calculator.calculate(alarm)
        assertTrue("Should still produce a future trigger", result > System.currentTimeMillis())
    }

    @Test
    fun `calculate clamps invalid alarm time instead of crashing`() {
        val fromTime = ZonedDateTime.of(2026, 1, 1, 22, 30, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(hour = 99, minute = 99, repeatDays = emptySet())

        val result = calculator.calculate(alarm, fromTime)
        val resultDateTime = Instant.ofEpochMilli(result).atZone(fromTime.zone)

        assertEquals(23, resultDateTime.hour)
        assertEquals(59, resultDateTime.minute)
        assertEquals(fromTime.toLocalDate(), resultDateTime.toLocalDate())
    }
}
