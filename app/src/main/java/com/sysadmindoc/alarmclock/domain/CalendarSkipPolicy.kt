package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Decides whether an alarm's calendar tag skips a given day.
 *
 * A day is skipped when any event covering it has a title containing the tag,
 * ignoring case. Two details make this more than a title search:
 *
 *  - All-day events are stored as UTC midnight to UTC midnight, so they are
 *    read by their UTC date. Converting them through the device zone would
 *    move a Tuesday holiday onto Monday in UTC-8 and keep it off Tuesday
 *    morning in UTC+9.
 *  - Events that span several days, timed or all-day, cover every one of
 *    those days, so the query has to look for overlap rather than events
 *    starting on the day.
 */
object CalendarSkipPolicy {

    fun isActive(tag: String): Boolean = tag.isNotBlank()

    fun shouldSkip(
        date: LocalDate,
        tag: String,
        events: List<CalendarEvent>,
        zone: ZoneId
    ): Boolean {
        val needle = tag.trim()
        if (needle.isEmpty()) return false
        return events.any { event ->
            event.title.contains(needle, ignoreCase = true) && covers(event, date, zone)
        }
    }

    /** True when [event] takes up any part of [date] in [zone]. */
    fun covers(event: CalendarEvent, date: LocalDate, zone: ZoneId): Boolean {
        if (event.allDay) {
            val first = utcDate(event.startTime)
            val endExclusive = utcDate(event.endTime).takeIf { it.isAfter(first) }
                ?: first.plusDays(1)
            return !date.isBefore(first) && date.isBefore(endExclusive)
        }
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        if (event.endTime <= event.startTime) {
            return event.startTime in dayStart until dayEnd
        }
        return event.startTime < dayEnd && event.endTime > dayStart
    }

    /**
     * The span to ask the calendar provider about for [date]: the local day
     * plus the UTC day, so an all-day event stored in UTC is never cut off by
     * the device zone. Returned as start inclusive, end exclusive millis.
     */
    fun queryWindow(date: LocalDate, zone: ZoneId): Pair<Long, Long> {
        val localStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val localEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val utcStart = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val utcEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return minOf(localStart, utcStart) to maxOf(localEnd, utcEnd)
    }

    private fun utcDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
