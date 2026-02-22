package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextAlarmCalculator @Inject constructor() {

    /**
     * Calculate the next trigger time in epoch millis for an alarm.
     * If alarm has repeat days, finds the next matching day.
     * If no repeat days, schedules for today if time hasn't passed, otherwise tomorrow.
     */
    fun calculate(alarm: Alarm, fromTime: ZonedDateTime = ZonedDateTime.now()): Long {
        val alarmTime = LocalTime.of(alarm.hour, alarm.minute)
        val today = fromTime.toLocalDate()
        val todayAlarmDateTime = ZonedDateTime.of(today, alarmTime, fromTime.zone)

        if (alarm.repeatDays.isEmpty()) {
            // One-shot alarm: today if in future, otherwise tomorrow
            return if (todayAlarmDateTime.isAfter(fromTime)) {
                todayAlarmDateTime.toInstant().toEpochMilli()
            } else {
                todayAlarmDateTime.plusDays(1).toInstant().toEpochMilli()
            }
        }

        // Repeating alarm: find next matching day
        for (daysAhead in 0L..7L) {
            val candidate = todayAlarmDateTime.plusDays(daysAhead)
            val dayOfWeek = candidate.dayOfWeek
            if (dayOfWeek in alarm.repeatDays) {
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
     * Format remaining time until alarm as human-readable string.
     * e.g. "2d 13h 57m"
     */
    fun formatRemaining(triggerTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = triggerTimeMillis - now
        if (diff <= 0) return "now"

        val days = diff / (24 * 60 * 60 * 1000)
        val hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            append("${minutes}m")
        }.trim()
    }
}
