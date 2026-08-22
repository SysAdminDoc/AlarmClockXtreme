package com.sysadmindoc.alarmclock.util

import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A dozen screens used to write the 12/24-hour branch out by hand, so the same
 * alarm could render three ways on one phone. These cases pin the shape every
 * one of them now shares.
 */
class AlarmTimeFormatterTest {

    @Test
    fun `24-hour keeps the leading zero and drops the meridiem`() {
        assertEquals("06:30", AlarmTimeFormatter.format(6, 30, is24Hour = true, locale = Locale.US))
        assertEquals("00:05", AlarmTimeFormatter.format(0, 5, is24Hour = true, locale = Locale.US))
        assertEquals("23:59", AlarmTimeFormatter.format(23, 59, is24Hour = true, locale = Locale.US))
    }

    @Test
    fun `12-hour drops the leading zero and carries the meridiem`() {
        assertEquals("6:30 AM", AlarmTimeFormatter.format(6, 30, is24Hour = false, locale = Locale.US))
        // Midnight and noon are where a hand-rolled hour % 12 goes wrong.
        assertEquals("12:05 AM", AlarmTimeFormatter.format(0, 5, is24Hour = false, locale = Locale.US))
        assertEquals("12:00 PM", AlarmTimeFormatter.format(12, 0, is24Hour = false, locale = Locale.US))
        assertEquals("11:59 PM", AlarmTimeFormatter.format(23, 59, is24Hour = false, locale = Locale.US))
    }

    @Test
    fun `the meridiem follows the locale rather than a hardcoded AM`() {
        // BedtimeViewModel used to splice the literals "AM" and "PM" in, which
        // is the thing this whole helper exists to stop. French is not the
        // counter-example it looks like: CLDR gives fr-FR "AM" as well.
        // Japanese is where a spliced literal actually shows.
        assertEquals(
            "6:30 午前",
            AlarmTimeFormatter.format(6, 30, is24Hour = false, locale = Locale.JAPAN)
        )
    }

    @Test
    fun `a corrupt stored row renders instead of throwing`() {
        assertEquals("23:00", AlarmTimeFormatter.format(99, 0, is24Hour = true, locale = Locale.US))
        assertEquals("00:00", AlarmTimeFormatter.format(-4, -1, is24Hour = true, locale = Locale.US))
    }

    @Test
    fun `an epoch instant formats in the zone it is given`() {
        // 2026-08-22T13:45:00Z
        val epochMillis = 1_787_665_500_000L
        assertEquals(
            "13:45",
            AlarmTimeFormatter.format(
                epochMillis,
                is24Hour = true,
                zone = ZoneId.of("UTC"),
                locale = Locale.US
            )
        )
        assertEquals(
            "9:45 AM",
            AlarmTimeFormatter.format(
                epochMillis,
                is24Hour = false,
                zone = ZoneId.of("America/New_York"),
                locale = Locale.US
            )
        )
    }

    @Test
    fun `callers that need a date prefix compose the same time half`() {
        assertEquals("EEE, MMM d • HH:mm", "EEE, MMM d • " + AlarmTimeFormatter.pattern(true))
        assertEquals("MMM d, h:mm a", "MMM d, " + AlarmTimeFormatter.pattern(false))
    }
}
