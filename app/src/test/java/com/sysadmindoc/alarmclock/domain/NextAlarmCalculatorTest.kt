package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    fun `expired one-shot specific date returns no trigger`() {
        val now = ZonedDateTime.of(2026, 4, 19, 9, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            specificDate = now.toLocalDate().minusDays(1).toString()
        )

        val result = calculator.calculate(alarm, now)

        assertEquals("Expired one-shot date alarm should not roll to tomorrow", 0L, result)
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

    @Test
    fun `DDNNO shift pattern skips off day`() {
        val fromTime = ZonedDateTime.of(2026, 7, 10, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            shiftPattern = "DDNNO",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(fromTime.toLocalDate().plusDays(1), resultDate)
    }

    @Test
    fun `four on four off pattern schedules next work block`() {
        val fromTime = ZonedDateTime.of(2026, 7, 10, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            shiftPattern = "FOUR_ON_FOUR_OFF",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(LocalDate.of(2026, 7, 14), resultDate)
    }

    @Test
    fun `Panama pattern keeps two two three cadence`() {
        val fromTime = ZonedDateTime.of(2026, 7, 8, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            shiftPattern = "PANAMA",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(LocalDate.of(2026, 7, 10), resultDate)
    }

    @Test
    fun `DuPont pattern handles long off block`() {
        val fromTime = ZonedDateTime.of(2026, 7, 27, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            shiftPattern = "DUPONT",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(LocalDate.of(2026, 8, 3), resultDate)
    }

    @Test
    fun `Pitman pattern supports pattern-only recurring alarm`() {
        val fromTime = ZonedDateTime.of(2026, 7, 8, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            repeatDays = emptySet(),
            shiftPattern = "PITMAN",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(LocalDate.of(2026, 7, 10), resultDate)
    }

    @Test
    fun `specific date overrides shift pattern when date is future`() {
        val fromTime = ZonedDateTime.of(2026, 7, 8, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            specificDate = "2026-07-10",
            shiftPattern = "FOUR_ON_FOUR_OFF",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(LocalDate.of(2026, 7, 10), resultDate)
    }

    @Test
    fun `expired specific date falls through to shift pattern`() {
        val fromTime = ZonedDateTime.of(2026, 7, 10, 5, 0, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            repeatDays = emptySet(),
            specificDate = "2026-07-08",
            shiftPattern = "DDNNO",
            shiftPatternStartDate = "2026-07-06"
        )

        val result = calculator.calculate(alarm, fromTime)
        val resultDate = Instant.ofEpochMilli(result).atZone(fromTime.zone).toLocalDate()

        assertEquals(LocalDate.of(2026, 7, 11), resultDate)
    }

    // --- DST policy ---------------------------------------------------------

    @Test
    fun `spring-forward gap alarm fires at gap end not an hour later`() {
        // America/New_York springs forward 2026-03-08: 02:00 -> 03:00, so 02:30
        // does not exist. ZonedDateTime.of would silently drift it to 03:30;
        // policy is to fire at the gap end (03:00) so the alarm goes off as soon
        // as a valid clock time exists.
        val ny = ZoneId.of("America/New_York")
        val fromTime = ZonedDateTime.of(LocalDate.of(2026, 3, 8), LocalTime.of(0, 0), ny)
        val alarm = Alarm(hour = 2, minute = 30, repeatDays = emptySet())

        val result = calculator.calculate(alarm, fromTime)
        val resultLocal = Instant.ofEpochMilli(result).atZone(ny).toLocalDateTime()

        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 0), resultLocal)
    }

    @Test
    fun `fall-back overlap alarm uses the earlier offset`() {
        // America/New_York falls back 2026-11-01: 02:00 -> 01:00, so 01:30
        // occurs twice. Policy keeps the first occurrence (EDT, -04:00).
        val ny = ZoneId.of("America/New_York")
        val fromTime = ZonedDateTime.of(LocalDate.of(2026, 11, 1), LocalTime.of(0, 0), ny)
        val alarm = Alarm(hour = 1, minute = 30, repeatDays = emptySet())

        val result = calculator.calculate(alarm, fromTime)
        val resultZoned = Instant.ofEpochMilli(result).atZone(ny)

        assertEquals(LocalTime.of(1, 30), resultZoned.toLocalTime())
        assertEquals(java.time.ZoneOffset.ofHours(-4), resultZoned.offset)
    }
}
