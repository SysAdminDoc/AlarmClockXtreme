package com.sysadmindoc.alarmclock.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The one place that decides what a clock time looks like.
 *
 * A dozen call sites each carried their own `if (is24Hour) "HH:mm" else
 * "h:mm a"`, two of them pinned to `Locale.US` and one hand-rolling the split
 * with literal "AM"/"PM", so the same alarm could render three different ways
 * on one phone. Anything that wants a date in front of the time builds its
 * pattern from [pattern] instead of writing the time half out again.
 */
object AlarmTimeFormatter {

    /** 24-hour clock. */
    const val PATTERN_24H: String = "HH:mm"

    /** 12-hour clock with the meridiem attached. */
    const val PATTERN_12H: String = "h:mm a"

    fun pattern(is24Hour: Boolean): String = if (is24Hour) PATTERN_24H else PATTERN_12H

    fun formatter(
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern(is24Hour), locale)

    /**
     * Formats a wall-clock hour and minute. Both are clamped rather than
     * thrown on: these come from stored alarm rows, and a corrupt row should
     * still render something instead of taking the screen down with it.
     */
    fun format(
        hour: Int,
        minute: Int,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        .format(formatter(is24Hour, locale))

    fun format(
        epochMillis: Long,
        is24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .format(formatter(is24Hour, locale))
}
