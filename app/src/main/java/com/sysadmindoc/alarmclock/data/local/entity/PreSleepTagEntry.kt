package com.sysadmindoc.alarmclock.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "pre_sleep_tag_entries",
    primaryKeys = ["localDate", "tagKey"],
    indices = [
        Index(value = ["localDate"]),
        Index(value = ["tagKey"])
    ]
)
data class PreSleepTagEntry(
    val localDate: String,
    val tagKey: String,
    val loggedAt: Long
)
