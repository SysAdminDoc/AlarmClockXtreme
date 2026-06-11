package com.sysadmindoc.alarmclock.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale
import java.util.UUID

@Entity(
    tableName = "alarm_incident_events",
    indices = [
        Index(value = ["alarmId"]),
        Index(value = ["fireId"]),
        Index(value = ["eventAt"])
    ]
)
data class AlarmIncidentEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fireId: String,
    val alarmId: Long,
    val scheduledAt: Long,
    val eventAt: Long,
    val elapsedMs: Long,
    val type: String,
    val status: String,
    val reasonCode: String,
    val source: String,
    val sdkInt: Int,
    val standbyBucket: String,
    val exactAlarmAllowed: String,
    val notificationPermissionGranted: String,
    val fullScreenIntentAllowed: String,
    val batteryOptimizationsIgnored: String,
    val algorithmVersion: String
) {
    fun sanitized(): AlarmIncidentEvent {
        return copy(
            fireId = sanitizeFireId(fireId),
            type = sanitizeToken(type, TYPE_UNKNOWN, maxLength = 48),
            status = sanitizeToken(status, STATUS_UNKNOWN, maxLength = 32),
            reasonCode = sanitizeReasonCode(reasonCode),
            source = sanitizeSource(source),
            standbyBucket = sanitizeToken(standbyBucket, VALUE_UNKNOWN, maxLength = 48),
            exactAlarmAllowed = sanitizeToken(exactAlarmAllowed, VALUE_UNKNOWN, maxLength = 24),
            notificationPermissionGranted = sanitizeToken(notificationPermissionGranted, VALUE_UNKNOWN, maxLength = 24),
            fullScreenIntentAllowed = sanitizeToken(fullScreenIntentAllowed, VALUE_UNKNOWN, maxLength = 24),
            batteryOptimizationsIgnored = sanitizeToken(batteryOptimizationsIgnored, VALUE_UNKNOWN, maxLength = 24),
            algorithmVersion = sanitizeToken(algorithmVersion, VALUE_NONE, maxLength = 64)
        )
    }

    companion object {
        const val TYPE_UNKNOWN = "UNKNOWN"
        const val TYPE_SCHEDULE = "SCHEDULE"
        const val TYPE_BROADCAST = "BROADCAST"
        const val TYPE_FOREGROUND_SERVICE = "FOREGROUND_SERVICE"
        const val TYPE_FOREGROUND_PROMOTION = "FOREGROUND_PROMOTION"
        const val TYPE_NOTIFICATION = "NOTIFICATION"
        const val TYPE_ACTIVITY_LAUNCH = "ACTIVITY_LAUNCH"
        const val TYPE_AUDIO = "AUDIO"
        const val TYPE_USER_ACTION = "USER_ACTION"
        const val TYPE_AUTO_SILENCE = "AUTO_SILENCE"
        const val TYPE_WAKE_CONFIRM = "WAKE_CONFIRM"

        const val STATUS_UNKNOWN = "UNKNOWN"
        const val STATUS_REQUESTED = "REQUESTED"
        const val STATUS_RECEIVED = "RECEIVED"
        const val STATUS_SUCCEEDED = "SUCCEEDED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SKIPPED = "SKIPPED"

        const val REASON_NONE = "NONE"
        const val VALUE_UNKNOWN = "UNKNOWN"
        const val VALUE_NOT_APPLICABLE = "NOT_APPLICABLE"
        const val VALUE_NONE = "NONE"

        fun fireIdFor(alarmId: Long, scheduledAt: Long): String {
            val safeAlarmId = alarmId.coerceAtLeast(0L)
            val safeScheduledAt = scheduledAt.coerceAtLeast(0L)
            return if (safeScheduledAt > 0L) {
                "alarm-$safeAlarmId-$safeScheduledAt"
            } else {
                "alarm-$safeAlarmId-${UUID.randomUUID()}"
            }
        }

        fun sanitizeToken(value: String, fallback: String, maxLength: Int): String {
            val normalized = value
                .trim()
                .uppercase(Locale.US)
                .map { char ->
                    when {
                        char.isLetterOrDigit() -> char
                        char == '_' || char == '-' || char == '.' -> char
                        else -> '_'
                    }
                }
                .joinToString("")
                .trim('_')
            return normalized
                .ifBlank { fallback }
                .take(maxLength)
        }

        private fun sanitizeFireId(value: String): String {
            val cleaned = value
                .trim()
                .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                .take(96)
            return if (cleaned.startsWith("alarm-")) {
                cleaned
            } else {
                fireIdFor(0L, 0L)
            }
        }

        private fun sanitizeReasonCode(value: String): String {
            if (value.any { it.isWhitespace() || it == ':' || it == '/' || it == '?' || it == '&' || it == '=' }) {
                return REASON_NONE
            }
            return sanitizeToken(value, REASON_NONE, maxLength = 80)
        }

        private fun sanitizeSource(value: String): String {
            if (value.any { it.isWhitespace() || it == ':' || it == '/' }) {
                return "unknown"
            }
            return value
                .trim()
                .filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
                .take(96)
                .ifBlank { "unknown" }
        }
    }
}
