package com.sysadmindoc.alarmclock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actigraphy_sessions")
data class ActigraphySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val targetTime: Long,
    val totalMinutes: Int,
    val awakeMinutes: Int,
    val lightMinutes: Int,
    val deepMinutes: Int,
    val averageSleepIndex: Float,
    val firedEarly: Boolean,
    val algorithm: String,
    val decisionReason: String,
    val observedMinutesBeforeDecision: Int,
    val smartWakeMode: String
)
