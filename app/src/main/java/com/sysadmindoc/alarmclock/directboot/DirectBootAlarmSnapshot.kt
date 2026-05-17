package com.sysadmindoc.alarmclock.directboot

import com.sysadmindoc.alarmclock.data.model.Alarm

data class DirectBootAlarmSnapshot(
    val alarmId: Long,
    val triggerTime: Long,
    val label: String,
    val timeLabel: String,
    val playDefaultSound: Boolean,
    val vibrationEnabled: Boolean,
    val updatedAt: Long,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun isSchedulable(now: Long): Boolean {
        return schemaVersion == CURRENT_SCHEMA_VERSION &&
            alarmId > 0L &&
            triggerTime > now
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        private const val MAX_TIME_LABEL_CHARS = 32

        fun fromAlarm(
            alarm: Alarm,
            triggerTime: Long,
            timeLabel: String,
            now: Long = System.currentTimeMillis()
        ): DirectBootAlarmSnapshot {
            val sanitized = alarm.sanitized()
            val mutedByVolume = sanitized.overrideSystemVolume && sanitized.volume <= 0
            return DirectBootAlarmSnapshot(
                alarmId = sanitized.id,
                triggerTime = triggerTime,
                label = "",
                timeLabel = timeLabel.take(MAX_TIME_LABEL_CHARS),
                playDefaultSound = sanitized.ringtoneUri != "silent" && !mutedByVolume,
                vibrationEnabled = sanitized.vibrationEnabled,
                updatedAt = now
            )
        }
    }
}
