package com.sysadmindoc.alarmclock.data.local

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String {
        if (days.isNullOrEmpty()) return ""
        // Stable order so two equal sets always serialise to the same string
        // (helps observers de-dupe even if Set iteration order differs).
        return days.sortedBy { it.value }.joinToString(",") { it.value.toString() }
    }

    @TypeConverter
    fun toDayOfWeekSet(value: String?): Set<DayOfWeek> {
        if (value.isNullOrBlank()) return emptySet()
        // Trim whitespace, drop empties, ignore anything outside 1..7. A previously
        // corrupt cell ("1,,3" or "8") is silently sanitised rather than crashing
        // the alarm list query.
        return value.split(",")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { part ->
                runCatching {
                    val day = part.toInt()
                    if (day in 1..7) DayOfWeek.of(day) else null
                }.getOrNull()
            }
            .toSet()
    }
}
