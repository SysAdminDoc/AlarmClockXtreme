package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.model.ShiftPattern
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.util.SolarCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextAlarmCalculator private constructor(
    private val settingsProvider: () -> AppSettings
) {
    private companion object {
        const val MAX_RECURRING_SEARCH_DAYS = 370L
    }

    /**
     * v1.5.1: Settings are read via a non-suspend cached snapshot exposed by
     * [PreferencesManager.getCachedSettings]. The previous `runBlocking`
     * implementation could ANR the main thread when called from ViewModel
     * `combine` blocks while DataStore was slow (first launch, large prefs,
     * managed profile edge cases).
     */
    @Inject
    constructor(preferencesManager: PreferencesManager) : this(
        settingsProvider = { preferencesManager.getCachedSettings() }
    )

    /** Test-friendly constructor — supply settings inline. */
    constructor(settings: AppSettings) : this(settingsProvider = { settings })

    /** Convenience for tests + default-construction sites that don't need solar math. */
    constructor() : this(settingsProvider = { AppSettings() })

    /**
     * Calculate the next trigger time in epoch millis for an alarm.
     * If alarm has repeat days, finds the next matching day.
     * If no repeat days, schedules for today if time hasn't passed, otherwise tomorrow.
     *
     * v1.5.0: When [Alarm.solarOffsetMinutes] is non-zero, the fixed clock
     * time is overridden with `sunrise-or-sunset + offset` at the user's
     * last known location. Falls back to the clock time if no location is
     * known or the location hits a polar day/night.
     */
    fun calculate(alarm: Alarm, fromTime: ZonedDateTime = ZonedDateTime.now()): Long {
        val scheduleZone = alarm.schedulingZone(fromTime.zone)
        val scheduleTime = fromTime.withZoneSameInstant(scheduleZone)
        // v1.2.0: Date-specific alarm overrides repeat days
        if (alarm.specificDate.isNotBlank()) {
            try {
                val specificDate = LocalDate.parse(alarm.specificDate)
                val specificTime = solarTimeFor(alarm, specificDate, scheduleZone)
                    ?: alarm.time
                val specificDateTime = resolveZoned(specificDate, specificTime, scheduleZone)
                if (specificDateTime.isAfter(scheduleTime)) {
                    return specificDateTime.toInstant().toEpochMilli()
                }
                // A one-shot date-specific alarm has expired. Repeating alarms
                // are allowed to fall through so their repeat-day schedule can
                // resume after the one-off date has passed.
                if (!alarm.isRecurringSchedule) return 0L
            } catch (_: Exception) { /* Invalid date format, fall through */ }
        }

        val today = scheduleTime.toLocalDate()
        val shiftSchedule = alarm.shiftScheduleOrNull()
        val todayTime = solarTimeFor(alarm, today, scheduleZone)
            ?: alarm.time
        val todayAlarmDateTime = resolveZoned(today, todayTime, scheduleZone)

        if (alarm.repeatDays.isEmpty() && shiftSchedule == null) {
            // One-shot alarm: today if in future, otherwise tomorrow
            return if (todayAlarmDateTime.isAfter(scheduleTime)) {
                todayAlarmDateTime.toInstant().toEpochMilli()
            } else {
                val tomorrow = today.plusDays(1)
                val tomorrowTime = solarTimeFor(alarm, tomorrow, scheduleZone)
                    ?: alarm.time
                resolveZoned(tomorrow, tomorrowTime, scheduleZone)
                    .toInstant().toEpochMilli()
            }
        }

        // Repeating alarm: find next matching day (solar time is recomputed per day
        // because sunrise/sunset drifts by minutes across the week).
        for (daysAhead in 0L..MAX_RECURRING_SEARCH_DAYS) {
            val candidateDate = today.plusDays(daysAhead)
            val dayOfWeek = candidateDate.dayOfWeek
            val matchesRepeatDays = alarm.repeatDays.isEmpty() || dayOfWeek in alarm.repeatDays
            val matchesShiftPattern = shiftSchedule == null ||
                shiftSchedule.pattern.isWorkDate(shiftSchedule.startDate, candidateDate)
            if (matchesRepeatDays && matchesShiftPattern) {
                val candidateTime = solarTimeFor(alarm, candidateDate, scheduleZone)
                    ?: alarm.time
                val candidate = resolveZoned(candidateDate, candidateTime, scheduleZone)
                if (daysAhead == 0L && !candidate.isAfter(scheduleTime)) {
                    continue  // Today's time already passed
                }
                return candidate.toInstant().toEpochMilli()
            }
        }

        return 0L
    }

    /**
     * Resolve a wall-clock (date, time) to a concrete instant with an explicit
     * DST policy instead of letting [ZonedDateTime.of] decide silently.
     *
     * - Spring-forward gap (the requested wall time doesn't exist, e.g. 02:30
     *   on a US spring-forward day): fire at the instant the skipped hour ends,
     *   so the alarm goes off as soon as a valid clock time exists rather than
     *   drifting a full gap-length (~1h) past the requested time, which is what
     *   `ZonedDateTime.of` does by default.
     * - Fall-back overlap (the wall time happens twice): keep the earlier
     *   offset — the first occurrence — which is also `ZonedDateTime.of`'s
     *   default. Made explicit here so the choice is deliberate and testable.
     */
    private fun resolveZoned(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime {
        val local = LocalDateTime.of(date, time)
        val transition = zone.rules.getTransition(local)
        return if (transition != null && transition.isGap) {
            transition.instant.atZone(zone)
        } else {
            ZonedDateTime.of(date, time, zone)
        }
    }

    private fun Alarm.shiftScheduleOrNull(): ShiftSchedule? {
        val pattern = ShiftPattern.fromKey(shiftPattern) ?: return null
        val startDate = runCatching { LocalDate.parse(shiftPatternStartDate) }.getOrNull()
            ?: return null
        return ShiftSchedule(pattern, startDate)
    }

    private data class ShiftSchedule(
        val pattern: ShiftPattern,
        val startDate: LocalDate
    )

    /**
     * Returns the solar-adjusted LocalTime for the alarm on [date], or null
     * if the alarm isn't using solar offset, location isn't known, or the
     * day is polar day/night. Caller falls back to the fixed hour/minute.
     *
     * v1.5.1: Uses a non-suspend settings snapshot to avoid blocking the
     * thread this calculator runs on (often Dispatchers.Main in
     * ViewModel `combine` blocks).
     */
    private fun solarTimeFor(alarm: Alarm, date: LocalDate, zone: ZoneId): LocalTime? {
        if (alarm.solarOffsetMinutes == 0) return null

        val settings = settingsProvider()
        val lat = settings.lastKnownLatitude
        val lng = settings.lastKnownLongitude
        // Treat (0, 0) as "unset" — the DataStore default for unknown location.
        // The 1° penalty around Null Island is accepted: a user legitimately at
        // (0, 0) will simply fall back to clock time.
        if (lat == 0.0 && lng == 0.0) return null

        val anchor = if (alarm.solarAnchor.equals("SUNSET", ignoreCase = true)) {
            SolarCalculator.sunset(date, lat, lng, zone)
        } else {
            SolarCalculator.sunrise(date, lat, lng, zone)
        } ?: return null

        return anchor.plusMinutes(alarm.solarOffsetMinutes.toLong())
    }

    /**
     * Format remaining time until alarm as human-readable string.
     * e.g. "2d 13h 57m". For sub-minute remainders we render "<1m" so the user
     * never sees the misleading "0m" label that the previous formatter produced
     * in the last 60 seconds before fire.
     */
    fun formatRemaining(triggerTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = triggerTimeMillis - now
        if (diff <= 0) return "now"

        val days = diff / (24 * 60 * 60 * 1000)
        val hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)

        if (days == 0L && hours == 0L && minutes == 0L) return "<1m"

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || (days == 0L && hours == 0L)) append("${minutes}m")
        }.trim()
    }
}
