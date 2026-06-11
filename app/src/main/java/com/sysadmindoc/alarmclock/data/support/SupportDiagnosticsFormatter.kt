package com.sysadmindoc.alarmclock.data.support

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SupportAlarmDiagnostic(
    val id: Long,
    val enabled: Boolean,
    val time: String,
    val repeat: String,
    val nextTriggerTime: Long,
    val challengeType: String,
    val hasCustomSound: Boolean,
    val hasInternetRadio: Boolean,
    val hueEnabled: Boolean,
    val guardianEnabled: Boolean,
    val wakeConfirmEnabled: Boolean
) {
    companion object {
        fun from(alarm: Alarm): SupportAlarmDiagnostic {
            val sanitized = alarm.sanitized()
            return SupportAlarmDiagnostic(
                id = sanitized.id,
                enabled = sanitized.isEnabled,
                time = "%02d:%02d".format(sanitized.hour, sanitized.minute),
                repeat = sanitized.repeatLabel,
                nextTriggerTime = sanitized.nextTriggerTime,
                challengeType = sanitized.challengeType,
                hasCustomSound = sanitized.ringtoneUri.isNotBlank() ||
                    sanitized.ringtonePool.isNotBlank() ||
                    sanitized.spotifyUri.isNotBlank(),
                hasInternetRadio = sanitized.internetRadioUrl.isNotBlank(),
                hueEnabled = sanitized.hueEnabled,
                guardianEnabled = sanitized.guardianEnabled,
                wakeConfirmEnabled = sanitized.wakeConfirmEnabled
            )
        }
    }
}

object SupportDiagnosticsFormatter {
    fun alarmCsv(alarms: List<SupportAlarmDiagnostic>): String {
        return buildString {
            appendLine(
                listOf(
                    "id",
                    "enabled",
                    "time",
                    "repeat",
                    "nextTriggerTime",
                    "challengeType",
                    "hasCustomSound",
                    "hasInternetRadio",
                    "hueEnabled",
                    "guardianEnabled",
                    "wakeConfirmEnabled"
                ).joinToString(",")
            )
            alarms.forEach { alarm ->
                appendLine(
                    listOf(
                        alarm.id.toString(),
                        alarm.enabled.toString(),
                        csv(alarm.time),
                        csv(alarm.repeat),
                        alarm.nextTriggerTime.toString(),
                        csv(alarm.challengeType),
                        alarm.hasCustomSound.toString(),
                        alarm.hasInternetRadio.toString(),
                        alarm.hueEnabled.toString(),
                        alarm.guardianEnabled.toString(),
                        alarm.wakeConfirmEnabled.toString()
                    ).joinToString(",")
                )
            }
        }
    }

    fun diagnosticsText(
        generatedAt: Instant,
        appVersion: String,
        versionCode: Int,
        flavor: String,
        buildType: String,
        packageName: String,
        deviceManufacturer: String,
        deviceModel: String,
        androidRelease: String,
        sdkInt: Int,
        notificationPermissionGranted: Boolean,
        exactAlarmsAllowed: Boolean,
        fullScreenIntentAllowed: Boolean?,
        ignoringBatteryOptimizations: Boolean,
        appStandbyBucket: String,
        totalAlarms: Int,
        enabledAlarms: Int,
        nextTriggerTime: Long?,
        crashLogCount: Int,
        stats: AlarmStats
    ): String {
        val nextTrigger = nextTriggerTime?.takeIf { it > 0L }?.let(::formatEpochMillis) ?: "none"
        return buildString {
            appendLine("AlarmClockXtreme support diagnostics")
            appendLine("Generated: ${generatedAt}")
            appendLine()
            appendLine("App")
            appendLine("- Version: $appVersion ($versionCode)")
            appendLine("- Flavor/build: $flavor/$buildType")
            appendLine("- Package: $packageName")
            appendLine()
            appendLine("Device")
            appendLine("- Manufacturer: $deviceManufacturer")
            appendLine("- Model: $deviceModel")
            appendLine("- Android: $androidRelease (API $sdkInt)")
            appendLine()
            appendLine("Wake readiness")
            appendLine("- Notifications granted: $notificationPermissionGranted")
            appendLine("- Exact alarms allowed: $exactAlarmsAllowed")
            appendLine("- Full-screen alarm access: ${formatFullScreenIntentStatus(fullScreenIntentAllowed, sdkInt)}")
            appendLine("- Ignoring battery optimizations: $ignoringBatteryOptimizations")
            appendLine("- App standby bucket: $appStandbyBucket")
            appendLine()
            appendLine("Alarm summary")
            appendLine("- Total alarms: $totalAlarms")
            appendLine("- Enabled alarms: $enabledAlarms")
            appendLine("- Next trigger: $nextTrigger")
            appendLine()
            appendLine("History summary")
            appendLine("- Dismissed: ${stats.totalDismissed}")
            appendLine("- Snoozed: ${stats.totalSnoozed}")
            appendLine("- Skipped: ${stats.totalSkipped}")
            appendLine("- Missed: ${stats.totalMissed}")
            appendLine("- Average dismiss response seconds: ${stats.averageDismissTimeSec}")
            appendLine("- Snooze rate percent: ${stats.snoozeRate}")
            appendLine("- Current wake streak days: ${stats.currentStreak}")
            appendLine("- Best wake streak days: ${stats.bestStreak}")
            appendLine("- Alarms this week: ${stats.alarmsThisWeek}")
            appendLine("- Day counts: ${formatDayMap(stats.dayOfWeekCounts)}")
            appendLine("- Day average response seconds: ${formatDayMap(stats.dayOfWeekAvgResponseSec)}")
            appendLine()
            appendLine("Crash logs")
            appendLine("- Included files: $crashLogCount")
            appendLine()
            appendLine("Privacy note")
            appendLine("This bundle is generated locally and is not uploaded by the app.")
            appendLine("It omits alarm labels, custom media URIs, internet-radio URLs, Spotify URIs, Hue/webhook secrets, Wi-Fi/location/contact values, challenge reference values, and Health Connect records.")
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun formatEpochMillis(value: Long): String {
        return Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun formatFullScreenIntentStatus(value: Boolean?, sdkInt: Int): String = when (value) {
        true -> "allowed"
        false -> "blocked"
        null -> if (sdkInt >= 34) "unknown" else "not_applicable"
    }

    private fun <T> formatDayMap(values: Map<DayOfWeek, T>): String {
        if (values.isEmpty()) return "none"
        return values.entries
            .sortedBy { it.key.value }
            .joinToString(", ") { "${it.key.name}=${it.value}" }
    }
}
