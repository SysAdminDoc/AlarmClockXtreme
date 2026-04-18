package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.util.SolarCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextAlarmCalculator private constructor(
    private val settingsProvider: () -> AppSettings
) {

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
        // v1.2.0: Date-specific alarm overrides repeat days
        if (alarm.specificDate.isNotBlank()) {
            try {
                val specificDate = LocalDate.parse(alarm.specificDate)
                val specificTime = solarTimeFor(alarm, specificDate, fromTime.zone)
                    ?: alarm.time
                val specificDateTime = ZonedDateTime.of(specificDate, specificTime, fromTime.zone)
                if (specificDateTime.isAfter(fromTime)) {
                    return specificDateTime.toInstant().toEpochMilli()
                }
                // Date is in the past — fall through to normal scheduling
            } catch (_: Exception) { /* Invalid date format, fall through */ }
        }

        val today = fromTime.toLocalDate()
        val todayTime = solarTimeFor(alarm, today, fromTime.zone)
            ?: alarm.time
        val todayAlarmDateTime = ZonedDateTime.of(today, todayTime, fromTime.zone)

        if (alarm.repeatDays.isEmpty()) {
            // One-shot alarm: today if in future, otherwise tomorrow
            return if (todayAlarmDateTime.isAfter(fromTime)) {
                todayAlarmDateTime.toInstant().toEpochMilli()
            } else {
                val tomorrow = today.plusDays(1)
                val tomorrowTime = solarTimeFor(alarm, tomorrow, fromTime.zone)
                    ?: alarm.time
                ZonedDateTime.of(tomorrow, tomorrowTime, fromTime.zone)
                    .toInstant().toEpochMilli()
            }
        }

        // Repeating alarm: find next matching day (solar time is recomputed per day
        // because sunrise/sunset drifts by minutes across the week).
        for (daysAhead in 0L..7L) {
            val candidateDate = today.plusDays(daysAhead)
            val dayOfWeek = candidateDate.dayOfWeek
            if (dayOfWeek in alarm.repeatDays) {
                val candidateTime = solarTimeFor(alarm, candidateDate, fromTime.zone)
                    ?: alarm.time
                val candidate = ZonedDateTime.of(candidateDate, candidateTime, fromTime.zone)
                if (daysAhead == 0L && !candidate.isAfter(fromTime)) {
                    continue  // Today's time already passed
                }
                return candidate.toInstant().toEpochMilli()
            }
        }

        // Fallback (shouldn't reach here with valid repeatDays)
        return todayAlarmDateTime.plusDays(1).toInstant().toEpochMilli()
    }

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
