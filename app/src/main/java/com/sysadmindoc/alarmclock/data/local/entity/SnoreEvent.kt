package com.sysadmindoc.alarmclock.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snore_events",
    indices = [
        Index(value = ["sessionStartedAt"]),
        Index(value = ["startedAt"])
    ]
)
data class SnoreEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionStartedAt: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val peakDb: Float,
    val averageDb: Float,
    val windowCount: Int,
    val source: String
)
