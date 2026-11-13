package com.sysadmindoc.alarmclock.ui.alarmedit

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.model.ShiftPattern
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastRingCountTest {

    @Test
    fun `one-time alarm lists a single upcoming ring`() {
        assertEquals(1, forecastRingCount(Alarm(hour = 16, minute = 50)))
    }

    @Test
    fun `date-specific one-off lists a single upcoming ring`() {
        assertEquals(1, forecastRingCount(Alarm(hour = 7, minute = 0, specificDate = "2026-12-24")))
    }

    @Test
    fun `repeating alarm lists a week of rings`() {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        assertEquals(7, forecastRingCount(Alarm(hour = 6, minute = 0, repeatDays = weekdays)))
    }

    @Test
    fun `shift pattern alarm lists a week of rings`() {
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            shiftPattern = ShiftPattern.presets.first().key,
            shiftPatternStartDate = "2026-11-01"
        )
        assertEquals(7, forecastRingCount(alarm))
    }
}
