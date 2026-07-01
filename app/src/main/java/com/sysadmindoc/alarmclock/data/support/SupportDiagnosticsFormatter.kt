package com.sysadmindoc.alarmclock.data.support

import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
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

object CrashLogScrubber {
    private val URL_PATTERN = Regex("""https?://[^\s"')\]]+""", RegexOption.IGNORE_CASE)
    private val CONTENT_URI_PATTERN = Regex("""content://[^\s"')\]]+""", RegexOption.IGNORE_CASE)
    private val FILE_URI_PATTERN = Regex("""file://[^\s"')\]]+""", RegexOption.IGNORE_CASE)
    private val PHONE_PATTERN = Regex("""\+?\d[\d\s\-()]{6,15}\d""")
    private val EMAIL_PATTERN = Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}""")
    private val HEX_TOKEN_PATTERN = Regex("""(?<![a-zA-Z0-9])[0-9a-fA-F]{32,}(?![a-zA-Z0-9])""")
    private val API_KEY_PATTERN = Regex("""(?:api[_\-]?key|token|secret|password|credential|authorization)[=:\s"']+[^\s"']+""", RegexOption.IGNORE_CASE)

    fun scrub(text: String): String {
        var result = text
        result = URL_PATTERN.replace(result, "[URL_REDACTED]")
        result = CONTENT_URI_PATTERN.replace(result, "[URI_REDACTED]")
        result = FILE_URI_PATTERN.replace(result, "[URI_REDACTED]")
        result = EMAIL_PATTERN.replace(result, "[EMAIL_REDACTED]")
        result = API_KEY_PATTERN.replace(result, "[SECRET_REDACTED]")
        result = HEX_TOKEN_PATTERN.replace(result, "[TOKEN_REDACTED]")
        result = scrubPhoneNumbers(result)
        return result
    }

    private fun scrubPhoneNumbers(text: String): String {
        return PHONE_PATTERN.replace(text) { match ->
            val digits = match.value.count { it.isDigit() }
            if (digits >= 7) "[PHONE_REDACTED]" else match.value
        }
    }
}

object SupportDiagnosticsFormatter {
    const val SCHEMA_VERSION = 3
    const val REDACTION_POLICY_VERSION = 1

    private val READINESS_FIELDS = listOf(
        "notificationPermissionGranted",
        "exactAlarmsAllowed",
        "fullScreenIntentAllowed",
        "localNetworkPermissionGranted",
        "batteryOptimizationsIgnored",
        "appStandbyBucket",
        "testAlarmCompleted",
        "testAlarmScheduledAt",
        "testAlarmFiredAt",
        "testAlarmCompletedAt",
        "testAlarmLatencyMs",
        "testAlarmNotificationPermissionGranted",
        "testAlarmFullScreenIntentRequested",
        "testAlarmActivityLaunchSucceeded",
        "testAlarmLegacyCompleted"
    )

    private val ALARM_DIAGNOSTIC_FIELDS = listOf(
        "id", "enabled", "time", "repeat", "nextTriggerTime", "challengeType",
        "hasCustomSound", "hasInternetRadio", "hueEnabled", "guardianEnabled",
        "wakeConfirmEnabled"
    )

    fun manifestJson(
        generatedAt: Instant,
        appVersion: String,
        versionCode: Int,
        flavor: String,
        buildType: String,
        includedFiles: List<String>,
        maxIncidentRows: Int,
        maxCrashLogs: Int,
        crashLogsScrubbed: Boolean
    ): String = buildString {
        appendLine("{")
        appendLine("""  "schemaVersion": $SCHEMA_VERSION,""")
        appendLine("""  "redactionPolicyVersion": $REDACTION_POLICY_VERSION,""")
        appendLine("""  "generatedAt": "${generatedAt}",""")
        appendLine("""  "appVersion": "${jsonEscape(appVersion)}",""")
        appendLine("""  "versionCode": $versionCode,""")
        appendLine("""  "flavor": "${jsonEscape(flavor)}",""")
        appendLine("""  "buildType": "${jsonEscape(buildType)}",""")
        appendLine("""  "maxIncidentRows": $maxIncidentRows,""")
        appendLine("""  "maxCrashLogs": $maxCrashLogs,""")
        appendLine("""  "crashLogsScrubbed": $crashLogsScrubbed,""")
        appendLine("""  "includedFiles": [${includedFiles.joinToString(", ") { "\"${jsonEscape(it)}\"" }}],""")
        appendLine("""  "readinessFields": [${READINESS_FIELDS.joinToString(", ") { "\"$it\"" }}],""")
        appendLine("""  "alarmDiagnosticFields": [${ALARM_DIAGNOSTIC_FIELDS.joinToString(", ") { "\"$it\"" }}]""")
        appendLine("}")
    }

    fun readinessJson(
        notificationPermissionGranted: Boolean,
        exactAlarmsAllowed: Boolean,
        fullScreenIntentAllowed: Boolean?,
        localNetworkPermissionGranted: Boolean?,
        ignoringBatteryOptimizations: Boolean,
        appStandbyBucket: String,
        sdkInt: Int,
        guardianReadiness: GuardianReadiness,
        testAlarmProof: TestAlarmProof = TestAlarmProof()
    ): String = buildString {
        appendLine("{")
        appendLine("""  "notificationPermissionGranted": $notificationPermissionGranted,""")
        appendLine("""  "exactAlarmsAllowed": $exactAlarmsAllowed,""")
        appendLine("""  "fullScreenIntentAllowed": ${formatFsiJson(fullScreenIntentAllowed, sdkInt)},""")
        appendLine("""  "localNetworkPermissionGranted": ${formatLocalNetworkJson(localNetworkPermissionGranted, sdkInt)},""")
        appendLine("""  "batteryOptimizationsIgnored": $ignoringBatteryOptimizations,""")
        appendLine("""  "appStandbyBucket": "${jsonEscape(appStandbyBucket)}",""")
        appendLine("""  "sdkInt": $sdkInt,""")
        appendLine("""  "guardianAlarmCount": ${guardianReadiness.enabledAlarmCount},""")
        appendLine("""  "guardianSmsPath": "${guardianReadiness.smsPath.name}",""")
        appendLine("""  "guardianSendSmsGranted": ${guardianReadiness.hasSendSmsPermission},""")
        appendLine("""  "guardianCallPhoneGranted": ${guardianReadiness.hasCallPhonePermission},""")
        appendLine("""  "testAlarmCompleted": ${testAlarmProof.hasDetailedCompletion},""")
        appendLine("""  "testAlarmScheduledAt": ${jsonEpochMillisOrNull(testAlarmProof.scheduledAt)},""")
        appendLine("""  "testAlarmFiredAt": ${jsonEpochMillisOrNull(testAlarmProof.firedAt)},""")
        appendLine("""  "testAlarmCompletedAt": ${jsonEpochMillisOrNull(testAlarmProof.completedAt)},""")
        appendLine("""  "testAlarmLatencyMs": ${testAlarmProof.latencyMs?.toString() ?: "null"},""")
        appendLine("""  "testAlarmNotificationPermissionGranted": ${testAlarmProof.notificationPermissionGranted},""")
        appendLine("""  "testAlarmFullScreenIntentRequested": ${testAlarmProof.fullScreenIntentRequested},""")
        appendLine("""  "testAlarmActivityLaunchSucceeded": ${testAlarmProof.activityLaunchSucceeded},""")
        appendLine("""  "testAlarmLegacyCompleted": ${testAlarmProof.legacyCompleted}""")
        appendLine("}")
    }

    fun smartWakeSummaryJson(sessions: List<ActigraphySession>): String = buildString {
        val sessionCount = sessions.size
        val firedEarlyCount = sessions.count { it.firedEarly }
        val targetFireCount = sessions.count { !it.firedEarly }
        val totalRuntimeMinutes = sessions.sumOf { it.totalMinutes }
        val avgObservedMinutes = if (sessions.isNotEmpty()) {
            sessions.sumOf { it.observedMinutesBeforeDecision } / sessions.size
        } else 0
        val latest = sessions.firstOrNull()

        appendLine("{")
        appendLine("""  "sessionCount": $sessionCount,""")
        appendLine("""  "firedEarlyCount": $firedEarlyCount,""")
        appendLine("""  "targetFireCount": $targetFireCount,""")
        appendLine("""  "totalRuntimeMinutes": $totalRuntimeMinutes,""")
        appendLine("""  "averageObservedMinutes": $avgObservedMinutes,""")
        appendLine("""  "latestDecisionReason": ${jsonStringOrNull(latest?.decisionReason)},""")
        appendLine("""  "latestObservedMinutes": ${latest?.observedMinutesBeforeDecision ?: "null"},""")
        appendLine("""  "latestMode": ${jsonStringOrNull(latest?.smartWakeMode)}""")
        appendLine("}")
    }

    fun alarmIncidentCsv(incidents: List<AlarmIncidentEvent>): String {
        return buildString {
            appendLine(
                listOf(
                    "id",
                    "fireId",
                    "alarmId",
                    "scheduledAt",
                    "eventAt",
                    "elapsedMs",
                    "type",
                    "status",
                    "reasonCode",
                    "source",
                    "sdkInt",
                    "standbyBucket",
                    "exactAlarmAllowed",
                    "notificationPermissionGranted",
                    "fullScreenIntentAllowed",
                    "batteryOptimizationsIgnored",
                    "algorithmVersion"
                ).joinToString(",")
            )
            incidents.forEach { raw ->
                val incident = raw.sanitized()
                appendLine(
                    listOf(
                        incident.id.toString(),
                        csv(incident.fireId),
                        incident.alarmId.toString(),
                        incident.scheduledAt.toString(),
                        incident.eventAt.toString(),
                        incident.elapsedMs.toString(),
                        csv(incident.type),
                        csv(incident.status),
                        csv(incident.reasonCode),
                        csv(incident.source),
                        incident.sdkInt.toString(),
                        csv(incident.standbyBucket),
                        csv(incident.exactAlarmAllowed),
                        csv(incident.notificationPermissionGranted),
                        csv(incident.fullScreenIntentAllowed),
                        csv(incident.batteryOptimizationsIgnored),
                        csv(incident.algorithmVersion)
                    ).joinToString(",")
                )
            }
        }
    }

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
        localNetworkPermissionGranted: Boolean?,
        ignoringBatteryOptimizations: Boolean,
        appStandbyBucket: String,
        guardianReadiness: GuardianReadiness,
        testAlarmProof: TestAlarmProof = TestAlarmProof(),
        totalAlarms: Int,
        enabledAlarms: Int,
        nextTriggerTime: Long?,
        crashLogCount: Int,
        smartWakeSessionCount: Int,
        smartWakeFiredEarlyCount: Int,
        smartWakeLastDecisionReason: String?,
        smartWakeLastObservedMinutes: Int?,
        smartWakeMode: String?,
        recentIncidentCount: Int,
        latestIncidentType: String?,
        latestIncidentStatus: String?,
        latestIncidentReason: String?,
        stats: AlarmStats,
        ytEngineBundledVersion: String = "",
        ytEngineActiveVersion: String = "",
        ytEngineLastUpdateMs: Long = 0,
        ytEngineLastUpdateStatus: String = "",
        ytEngineLastUpdateSource: String = "",
        ytEngineLastFailureReason: String = "",
        showWeatherOnDashboard: Boolean = false,
        holidayAutoSkipEnabled: Boolean = false,
        showRadarEmbed: Boolean = false,
        showNewsTab: Boolean = false,
        webhookEnabled: Boolean = false,
        hueBridgeConfigured: Boolean = false,
        healthConnectEnabled: Boolean = false
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
            appendLine("- Local network access: ${formatLocalNetworkStatus(localNetworkPermissionGranted, sdkInt)}")
            appendLine("- Ignoring battery optimizations: $ignoringBatteryOptimizations")
            appendLine("- App standby bucket: $appStandbyBucket")
            appendLine("- Guardian Angel alarms: ${guardianReadiness.enabledAlarmCount}")
            appendLine("- Guardian SMS path: ${guardianReadiness.smsPath.name}")
            appendLine("- Guardian SEND_SMS granted: ${guardianReadiness.hasSendSmsPermission}")
            appendLine("- Guardian CALL_PHONE granted: ${guardianReadiness.hasCallPhonePermission}")
            appendLine("- Test alarm completed: ${testAlarmProof.hasDetailedCompletion}")
            appendLine("- Test alarm scheduled: ${formatOptionalEpoch(testAlarmProof.scheduledAt)}")
            appendLine("- Test alarm fired: ${formatOptionalEpoch(testAlarmProof.firedAt)}")
            appendLine("- Test alarm dismissed: ${formatOptionalEpoch(testAlarmProof.completedAt)}")
            appendLine("- Test alarm latency: ${testAlarmProof.latencyMs?.let(::formatLatencyMs) ?: "none"}")
            appendLine("- Test alarm notification permission: ${testAlarmProof.notificationPermissionGranted}")
            appendLine("- Test alarm full-screen requested: ${testAlarmProof.fullScreenIntentRequested}")
            appendLine("- Test alarm direct activity launch: ${testAlarmProof.activityLaunchSucceeded}")
            appendLine("- Test alarm legacy completion: ${testAlarmProof.legacyCompleted}")
            appendLine()
            appendLine("Alarm summary")
            appendLine("- Total alarms: $totalAlarms")
            appendLine("- Enabled alarms: $enabledAlarms")
            appendLine("- Next trigger: $nextTrigger")
            appendLine()
            appendLine("Sleep motion summary")
            appendLine("- Recent sessions: $smartWakeSessionCount")
            appendLine("- Smart-wake fired early sessions: $smartWakeFiredEarlyCount")
            appendLine("- Last decision reason: ${smartWakeLastDecisionReason ?: "none"}")
            appendLine("- Last observed minutes: ${smartWakeLastObservedMinutes?.toString() ?: "none"}")
            appendLine("- Decision mode: ${smartWakeMode ?: "none"}")
            appendLine()
            appendLine("Incident summary")
            appendLine("- Recent incidents: $recentIncidentCount")
            appendLine("- Latest type: ${latestIncidentType ?: "none"}")
            appendLine("- Latest status: ${latestIncidentStatus ?: "none"}")
            appendLine("- Latest reason: ${latestIncidentReason ?: "none"}")
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
            appendLine("YouTube engine")
            appendLine("- Bundled version: ${ytEngineBundledVersion.ifBlank { "unknown" }}")
            appendLine("- Active version: ${ytEngineActiveVersion.ifBlank { "unknown" }}")
            if (ytEngineActiveVersion.isNotBlank() && ytEngineActiveVersion < com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader.MIN_SAFE_VERSION) {
                appendLine("- WARNING: engine is below minimum safe version ${com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader.MIN_SAFE_VERSION}")
            }
            if (ytEngineLastUpdateMs > 0) {
                appendLine("- Last update: ${java.time.Instant.ofEpochMilli(ytEngineLastUpdateMs)}")
                appendLine("- Last update status: $ytEngineLastUpdateStatus")
                appendLine("- Last update source: $ytEngineLastUpdateSource")
            }
            if (ytEngineLastFailureReason.isNotBlank()) {
                appendLine("- Last failure: $ytEngineLastFailureReason")
            }
            appendLine()
            appendLine("Connections")
            appendLine("- Weather (Open-Meteo): ${if (showWeatherOnDashboard) "active" else "off"}")
            appendLine("- Public holidays (Nager.Date): ${if (holidayAutoSkipEnabled) "active" else "off"}")
            appendLine("- Radar (Windy embed): ${if (showRadarEmbed) "active" else "off"}")
            appendLine("- News feed: ${if (showNewsTab) "active" else "off"}")
            appendLine("- Webhook: ${if (webhookEnabled) "active" else "off"}")
            appendLine("- Hue bridge: ${if (hueBridgeConfigured) "configured" else "off"}")
            appendLine("- Health Connect: ${if (healthConnectEnabled) "active" else "off"}")
            appendLine()
            appendLine("Crash logs")
            appendLine("- Included files: $crashLogCount")
            appendLine()
            appendLine("Privacy note")
            appendLine("This bundle is generated locally and is not uploaded by the app.")
            appendLine("It omits alarm labels, custom media URIs, internet-radio URLs, Spotify URIs, Hue/webhook secrets, Wi-Fi/location/contact values, challenge reference values, Health Connect records, raw audio, and per-minute local motion buckets.")
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun jsonStringOrNull(value: String?): String =
        if (value != null) "\"${jsonEscape(value)}\"" else "null"

    private fun jsonEpochMillisOrNull(value: Long): String =
        value.takeIf { it > 0L }?.let { "\"${formatEpochMillis(it)}\"" } ?: "null"

    private fun formatOptionalEpoch(value: Long): String =
        value.takeIf { it > 0L }?.let(::formatEpochMillis) ?: "none"

    private fun formatLatencyMs(value: Long): String =
        if (value < 1_000L) "${value}ms" else "${(value / 1000.0).formatOneDecimal()}s"

    private fun formatEpochMillis(value: Long): String {
        return Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun Double.formatOneDecimal(): String =
        String.format(java.util.Locale.US, "%.1f", this)

    private fun formatFullScreenIntentStatus(value: Boolean?, sdkInt: Int): String = when (value) {
        true -> "allowed"
        false -> "blocked"
        null -> if (sdkInt >= 34) "unknown" else "not_applicable"
    }

    private fun formatFsiJson(value: Boolean?, sdkInt: Int): String = when (value) {
        true -> "true"
        false -> "false"
        null -> "\"${if (sdkInt >= 34) "unknown" else "not_applicable"}\""
    }

    private fun formatLocalNetworkStatus(value: Boolean?, sdkInt: Int): String = when (value) {
        true -> "granted"
        false -> "denied"
        null -> if (sdkInt >= 37) "unknown" else "not_applicable"
    }

    private fun formatLocalNetworkJson(value: Boolean?, sdkInt: Int): String = when (value) {
        true -> "true"
        false -> "false"
        null -> "\"${if (sdkInt >= 37) "unknown" else "not_applicable"}\""
    }

    private fun <T> formatDayMap(values: Map<DayOfWeek, T>): String {
        if (values.isEmpty()) return "none"
        return values.entries
            .sortedBy { it.key.value }
            .joinToString(", ") { "${it.key.name}=${it.value}" }
    }
}
