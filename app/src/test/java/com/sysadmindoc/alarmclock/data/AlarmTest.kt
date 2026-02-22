package com.sysadmindoc.alarmclock.data

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AlarmTest {

    @Test
    fun `time property returns correct LocalTime`() {
        val alarm = Alarm(hour = 14, minute = 30)
        assertEquals(LocalTime.of(14, 30), alarm.time)
    }

    @Test
    fun `repeatLabel returns Once for empty days`() {
        val alarm = Alarm(repeatDays = emptySet())
        assertEquals("Once", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns Every day for all days`() {
        val alarm = Alarm(repeatDays = DayOfWeek.entries.toSet())
        assertEquals("Every day", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns Weekdays for Monday-Friday`() {
        val alarm = Alarm(repeatDays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ))
        assertEquals("Weekdays", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns Weekend for Saturday-Sunday`() {
        val alarm = Alarm(repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertEquals("Weekend", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns custom day names for partial selection`() {
        val alarm = Alarm(repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertTrue(alarm.repeatLabel.contains("Mon"))
        assertTrue(alarm.repeatLabel.contains("Wed"))
    }

    @Test
    fun `default alarm has sensible defaults`() {
        val alarm = Alarm()
        assertEquals(9, alarm.hour)
        assertEquals(0, alarm.minute)
        assertEquals("", alarm.label)
        assertTrue(alarm.isEnabled)
        assertTrue(alarm.vibrationEnabled)
        assertEquals(100, alarm.volume)
        assertEquals(10, alarm.snoozeDurationMinutes)
        assertEquals("NONE", alarm.challengeType)
    }

    @Test
    fun `alarm with midnight time`() {
        val alarm = Alarm(hour = 0, minute = 0)
        assertEquals(LocalTime.MIDNIGHT, alarm.time)
    }

    @Test
    fun `alarm with max time`() {
        val alarm = Alarm(hour = 23, minute = 59)
        assertEquals(LocalTime.of(23, 59), alarm.time)
    }
}
