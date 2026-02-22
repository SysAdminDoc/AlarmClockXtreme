package com.sysadmindoc.alarmclock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Core alarm entity stored in Room database.
 * Maps directly to the alarm list UI and scheduling engine.
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int = 9,
    val minute: Int = 0,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val ringtoneUri: String = "",           // Empty = device default
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 2,        // 0=off, 1=gentle, 2=intense
    val volume: Int = 100,                  // 0-100
    val overrideSystemVolume: Boolean = true,
    val gradualVolumeSeconds: Int = 60,     // Fade-in duration in seconds
    val snoozeDurationMinutes: Int = 10,
    val maxSnoozeCount: Int = 3,            // 0 = unlimited
    val showOnLockScreen: Boolean = true,
    val challengeType: String = "NONE",     // ChallengeType enum name
    val createdAt: Long = System.currentTimeMillis(),
    val nextTriggerTime: Long = 0           // Epoch millis of next scheduled fire
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)

    val repeatLabel: String get() = when {
        repeatDays.isEmpty() -> "Once"
        repeatDays.size == 7 -> "Every day"
        repeatDays == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) -> "Weekdays"
        repeatDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekend"
        else -> repeatDays.sortedBy { it.value }
            .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
    }
}
