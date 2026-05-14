package com.sysadmindoc.alarmclock.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WakeStreakCalculatorTest {

    @Test
    fun keepsStreakAliveWhenTodayHasNotFiredYet() {
        val today = LocalDate.of(2026, 5, 14)
        val summary = WakeStreakCalculator.calculate(
            listOf("2026-05-13", "2026-05-12", "2026-05-11"),
            today = today
        )

        assertEquals(3, summary.currentDays)
        assertEquals(3, summary.bestDays)
        assertFalse(summary.includesToday)
        assertEquals(7, summary.nextGoal)
    }

    @Test
    fun includesTodayWhenDismissedToday() {
        val today = LocalDate.of(2026, 5, 14)
        val summary = WakeStreakCalculator.calculate(
            listOf("2026-05-14", "2026-05-13", "2026-05-12"),
            today = today
        )

        assertEquals(3, summary.currentDays)
        assertTrue(summary.includesToday)
    }

    @Test
    fun resetsCurrentAfterGapButKeepsBestRun() {
        val today = LocalDate.of(2026, 5, 14)
        val summary = WakeStreakCalculator.calculate(
            listOf("2026-05-11", "2026-05-10", "2026-05-09", "2026-05-01"),
            today = today
        )

        assertEquals(0, summary.currentDays)
        assertEquals(3, summary.bestDays)
        assertEquals(3, summary.nextGoal)
    }

    @Test
    fun ignoresMalformedAndDuplicateDates() {
        val today = LocalDate.of(2026, 5, 14)
        val summary = WakeStreakCalculator.calculate(
            listOf("bad", "2026-05-14", "2026-05-14", "2026-05-13"),
            today = today
        )

        assertEquals(2, summary.currentDays)
        assertEquals(2, summary.bestDays)
    }
}
