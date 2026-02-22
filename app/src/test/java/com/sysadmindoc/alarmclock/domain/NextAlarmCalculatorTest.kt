package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
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
}
