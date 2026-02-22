package com.sysadmindoc.alarmclock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records each alarm event for statistics tracking.
 * Stores when alarms fired, how they were dismissed, and response times.
 */
@Entity(tableName = "alarm_events")
data class AlarmEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val alarmLabel: String = "",
    val scheduledTime: Long,          // When the alarm was supposed to fire
    val firedAt: Long,                // When the alarm actually fired
    val action: String,               // DISMISSED, SNOOZED, SKIPPED, MISSED
    val actionAt: Long = 0,           // When the user took action
    val challengeType: String = "NONE",
    val challengeSolveTimeMs: Long = 0, // How long to solve the challenge
    val snoozeCount: Int = 0,         // How many times snoozed before dismiss
    val dayOfWeek: Int = 0            // 1=Monday..7=Sunday for day-of-week stats
) {
    val responseTimeMs: Long get() = if (actionAt > 0 && firedAt > 0) actionAt - firedAt else 0

    companion object {
        const val ACTION_DISMISSED = "DISMISSED"
        const val ACTION_SNOOZED = "SNOOZED"
        const val ACTION_SKIPPED = "SKIPPED"
        const val ACTION_MISSED = "MISSED"
    }
}
