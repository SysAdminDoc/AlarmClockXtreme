package com.sysadmindoc.alarmclock.data.local

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        return days.joinToString(",") { it.value.toString() }
    }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { DayOfWeek.of(it.trim().toInt()) }.toSet()
    }
}
