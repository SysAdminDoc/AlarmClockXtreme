package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class VacationAlarmPolicyTest {

    private val zone = ZoneId.of("UTC")
    private val calculator = NextAlarmCalculator()

    @Test
    fun `repeating alarm inside vacation range resumes after vacation ends`() {
        val settings = vacationSettings(
            start = ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, zone),
            end = ZonedDateTime.of(2026, 6, 10, 23, 59, 0, 0, zone)
        )
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            repeatDays = DayOfWeek.entries.toSet()
        )
        val initialTrigger = ZonedDateTime.of(2026, 6, 2, 7, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

        val adjusted = VacationAlarmPolicy.adjustTrigger(
            alarm = alarm,
            initialTriggerTime = initialTrigger,
            settings = settings,
            zone = zone,
            calculateFrom = calculator::calculate
        )

        val adjustedDateTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(adjusted.triggerTime),
            zone
        )
        assertTrue(adjusted.skippedByVacation)
        assertEquals(ZonedDateTime.of(2026, 6, 11, 7, 0, 0, 0, zone), adjustedDateTime)
    }

    @Test
    fun `one shot alarm inside vacation range is not skipped`() {
        val settings = vacationSettings(
            start = ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, zone),
            end = ZonedDateTime.of(2026, 6, 10, 23, 59, 0, 0, zone)
        )
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = emptySet())
        val initialTrigger = ZonedDateTime.of(2026, 6, 2, 7, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

        val adjusted = VacationAlarmPolicy.adjustTrigger(
            alarm = alarm,
            initialTriggerTime = initialTrigger,
            settings = settings,
            zone = zone,
            calculateFrom = calculator::calculate
        )

        assertFalse(adjusted.skippedByVacation)
        assertEquals(initialTrigger, adjusted.triggerTime)
    }

    @Test
    fun `vacation is active only inside configured window`() {
        val settings = vacationSettings(
            start = ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, zone),
            end = ZonedDateTime.of(2026, 6, 10, 23, 59, 0, 0, zone)
        )

        assertFalse(
            VacationAlarmPolicy.isActive(
                settings,
                ZonedDateTime.of(2026, 5, 31, 23, 59, 0, 0, zone).toMillis()
            )
        )
        assertTrue(
            VacationAlarmPolicy.isActive(
                settings,
                ZonedDateTime.of(2026, 6, 5, 12, 0, 0, 0, zone).toMillis()
            )
        )
        assertFalse(
            VacationAlarmPolicy.isActive(
                settings,
                ZonedDateTime.of(2026, 6, 11, 0, 0, 0, 0, zone).toMillis()
            )
        )
    }

    private fun vacationSettings(start: ZonedDateTime, end: ZonedDateTime): AppSettings {
        return AppSettings(
            vacationModeEnabled = true,
            vacationStartMillis = start.toMillis(),
            vacationEndMillis = end.toMillis()
        )
    }

    private fun ZonedDateTime.toMillis(): Long = toInstant().toEpochMilli()
}
