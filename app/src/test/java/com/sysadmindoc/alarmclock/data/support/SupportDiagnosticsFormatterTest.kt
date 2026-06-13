package com.sysadmindoc.alarmclock.data.support

import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SupportDiagnosticsFormatterTest {

    @Test
    fun `alarm diagnostics omit labels and raw integration values`() {
        val alarm = Alarm(
            id = 7,
            label = "Private appointment",
            ringtoneUri = "content://media/external/audio/media/123",
            ringtonePool = "content://media/external/audio/media/456",
            spotifyUri = "spotify:track:secret",
            internetRadioUrl = "https://example.com/private-stream",
            hueEnabled = true,
            guardianEnabled = true,
            guardianPhone = "+15555551212",
            wifiDismissSsid = "PrivateWifi",
            nfcTagId = "secret-tag"
        )

        val csv = SupportDiagnosticsFormatter.alarmCsv(
            listOf(SupportAlarmDiagnostic.from(alarm))
        )

        assertFalse(csv.contains("Private appointment"))
        assertFalse(csv.contains("content://"))
        assertFalse(csv.contains("spotify:"))
        assertFalse(csv.contains("example.com"))
        assertFalse(csv.contains("+15555551212"))
        assertFalse(csv.contains("PrivateWifi"))
        assertFalse(csv.contains("secret-tag"))
        assertTrue(csv.contains("hasCustomSound"))
        assertTrue(csv.contains("true"))
    }

    @Test
    fun `diagnostics expose full-screen alarm readiness status`() {
        val blocked = diagnosticsText(sdkInt = 34, fullScreenIntentAllowed = false)
        val unknown = diagnosticsText(sdkInt = 35, fullScreenIntentAllowed = null)
        val notApplicable = diagnosticsText(sdkInt = 33, fullScreenIntentAllowed = null)

        assertTrue(blocked.contains("- Full-screen alarm access: blocked"))
        assertTrue(unknown.contains("- Full-screen alarm access: unknown"))
        assertTrue(notApplicable.contains("- Full-screen alarm access: not_applicable"))
    }

    @Test
    fun `diagnostics expose guardian readiness status`() {
        val text = diagnosticsText(
            sdkInt = 35,
            fullScreenIntentAllowed = true,
            guardianReadiness = GuardianReadiness(
                enabledAlarmCount = 2,
                smsPath = GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION,
                hasSendSmsPermission = false,
                hasCallPhonePermission = true
            )
        )

        assertTrue(text.contains("- Guardian Angel alarms: 2"))
        assertTrue(text.contains("- Guardian SMS path: NEEDS_SEND_SMS_PERMISSION"))
        assertTrue(text.contains("- Guardian SEND_SMS granted: false"))
        assertTrue(text.contains("- Guardian CALL_PHONE granted: true"))
    }

    @Test
    fun `diagnostics expose aggregate smart wake fields only`() {
        val text = diagnosticsText(
            sdkInt = 35,
            fullScreenIntentAllowed = true,
            smartWakeSessionCount = 3,
            smartWakeFiredEarlyCount = 1,
            smartWakeLastDecisionReason = "WAIT_TOO_ACTIVE",
            smartWakeLastObservedMinutes = 12,
            smartWakeMode = "CONSERVATIVE"
        )

        assertTrue(text.contains("Smart wake summary"))
        assertTrue(text.contains("- Recent sessions: 3"))
        assertTrue(text.contains("- Fired early sessions: 1"))
        assertTrue(text.contains("- Last decision reason: WAIT_TOO_ACTIVE"))
        assertTrue(text.contains("- Last observed minutes: 12"))
        assertTrue(text.contains("per-minute local actigraphy motion buckets"))
    }

    @Test
    fun `incident diagnostics omit labels urls and secret-like reason text`() {
        val csv = SupportDiagnosticsFormatter.alarmIncidentCsv(
            listOf(
                AlarmIncidentEvent(
                    id = 9,
                    fireId = "fire://private-label",
                    alarmId = 7,
                    scheduledAt = 1_000L,
                    eventAt = 1_500L,
                    elapsedMs = 500L,
                    type = "activity launch",
                    status = "failed",
                    reasonCode = "https://secret.example/path?token=abc",
                    source = "AlarmService Private appointment",
                    sdkInt = 35,
                    standbyBucket = "ACTIVE (10)",
                    exactAlarmAllowed = "true",
                    notificationPermissionGranted = "true",
                    fullScreenIntentAllowed = "false",
                    batteryOptimizationsIgnored = "true",
                    algorithmVersion = ""
                )
            )
        )

        assertTrue(csv.contains("fireId"))
        assertTrue(csv.contains("ACTIVITY_LAUNCH"))
        assertTrue(csv.contains("NONE"))
        assertFalse(csv.contains("private-label"))
        assertFalse(csv.contains("Private appointment"))
        assertFalse(csv.contains("https://secret.example"))
        assertFalse(csv.contains("token=abc"))
    }

    // --- A12: Support Manifest ---

    @Test
    fun `manifest contains schema version and file list`() {
        val manifest = SupportDiagnosticsFormatter.manifestJson(
            generatedAt = Instant.EPOCH,
            appVersion = "1.14.0",
            versionCode = 82,
            flavor = "play",
            buildType = "debug",
            includedFiles = listOf("support_manifest.json", "readiness.json", "diagnostics.txt"),
            maxIncidentRows = 25,
            maxCrashLogs = 10,
            crashLogsScrubbed = true
        )

        assertTrue(manifest.contains("\"schemaVersion\": ${SupportDiagnosticsFormatter.SCHEMA_VERSION}"))
        assertTrue(manifest.contains("\"redactionPolicyVersion\": ${SupportDiagnosticsFormatter.REDACTION_POLICY_VERSION}"))
        assertTrue(manifest.contains("\"appVersion\": \"1.14.0\""))
        assertTrue(manifest.contains("\"versionCode\": 82"))
        assertTrue(manifest.contains("\"flavor\": \"play\""))
        assertTrue(manifest.contains("\"maxIncidentRows\": 25"))
        assertTrue(manifest.contains("\"maxCrashLogs\": 10"))
        assertTrue(manifest.contains("\"crashLogsScrubbed\": true"))
        assertTrue(manifest.contains("\"support_manifest.json\""))
        assertTrue(manifest.contains("\"readiness.json\""))
        assertTrue(manifest.contains("\"readinessFields\""))
        assertTrue(manifest.contains("\"alarmDiagnosticFields\""))
    }

    // --- A12: Readiness JSON ---

    @Test
    fun `readiness json contains all permission states`() {
        val json = SupportDiagnosticsFormatter.readinessJson(
            notificationPermissionGranted = true,
            exactAlarmsAllowed = true,
            fullScreenIntentAllowed = false,
            ignoringBatteryOptimizations = false,
            appStandbyBucket = "ACTIVE (10)",
            sdkInt = 35,
            guardianReadiness = GuardianReadiness(
                enabledAlarmCount = 1,
                smsPath = GuardianSmsPath.DIRECT_SMS,
                hasSendSmsPermission = true,
                hasCallPhonePermission = false
            )
        )

        assertTrue(json.contains("\"notificationPermissionGranted\": true"))
        assertTrue(json.contains("\"exactAlarmsAllowed\": true"))
        assertTrue(json.contains("\"fullScreenIntentAllowed\": false"))
        assertTrue(json.contains("\"batteryOptimizationsIgnored\": false"))
        assertTrue(json.contains("\"appStandbyBucket\": \"ACTIVE (10)\""))
        assertTrue(json.contains("\"sdkInt\": 35"))
        assertTrue(json.contains("\"guardianAlarmCount\": 1"))
        assertTrue(json.contains("\"guardianSmsPath\": \"DIRECT_SMS\""))
        assertTrue(json.contains("\"guardianSendSmsGranted\": true"))
        assertTrue(json.contains("\"guardianCallPhoneGranted\": false"))
    }

    @Test
    fun `readiness json handles not-applicable FSI on older API`() {
        val json = SupportDiagnosticsFormatter.readinessJson(
            notificationPermissionGranted = true,
            exactAlarmsAllowed = true,
            fullScreenIntentAllowed = null,
            ignoringBatteryOptimizations = true,
            appStandbyBucket = "ACTIVE (10)",
            sdkInt = 33,
            guardianReadiness = defaultGuardianReadiness()
        )
        assertTrue(json.contains("\"fullScreenIntentAllowed\": \"not_applicable\""))
    }

    @Test
    fun `readiness json handles unknown FSI on API 34 plus`() {
        val json = SupportDiagnosticsFormatter.readinessJson(
            notificationPermissionGranted = true,
            exactAlarmsAllowed = true,
            fullScreenIntentAllowed = null,
            ignoringBatteryOptimizations = true,
            appStandbyBucket = "RARE (40)",
            sdkInt = 34,
            guardianReadiness = defaultGuardianReadiness()
        )
        assertTrue(json.contains("\"fullScreenIntentAllowed\": \"unknown\""))
    }

    // --- A12: Smart Wake Summary JSON ---

    @Test
    fun `smart wake summary aggregates sessions`() {
        val sessions = listOf(
            testSession(firedEarly = true, totalMinutes = 30, observedMinutes = 10, reason = "LIGHT_SLEEP_DETECTED"),
            testSession(firedEarly = false, totalMinutes = 45, observedMinutes = 45, reason = "WINDOW_EXPIRED"),
            testSession(firedEarly = false, totalMinutes = 20, observedMinutes = 20, reason = "WINDOW_EXPIRED")
        )
        val json = SupportDiagnosticsFormatter.smartWakeSummaryJson(sessions)

        assertTrue(json.contains("\"sessionCount\": 3"))
        assertTrue(json.contains("\"firedEarlyCount\": 1"))
        assertTrue(json.contains("\"targetFireCount\": 2"))
        assertTrue(json.contains("\"totalRuntimeMinutes\": 95"))
        assertTrue(json.contains("\"averageObservedMinutes\": 25"))
        assertTrue(json.contains("\"latestDecisionReason\": \"LIGHT_SLEEP_DETECTED\""))
        assertTrue(json.contains("\"latestObservedMinutes\": 10"))
    }

    @Test
    fun `smart wake summary handles empty sessions`() {
        val json = SupportDiagnosticsFormatter.smartWakeSummaryJson(emptyList())

        assertTrue(json.contains("\"sessionCount\": 0"))
        assertTrue(json.contains("\"firedEarlyCount\": 0"))
        assertTrue(json.contains("\"totalRuntimeMinutes\": 0"))
        assertTrue(json.contains("\"latestDecisionReason\": null"))
        assertTrue(json.contains("\"latestObservedMinutes\": null"))
    }

    // --- A12: Crash Log Scrubber ---

    @Test
    fun `crash log scrubber removes URLs`() {
        val input = "Failed to load https://secret.example.com/api/v2/data?key=abc123"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("secret.example.com"))
        assertFalse(scrubbed.contains("key=abc123"))
        assertTrue(scrubbed.contains("[URL_REDACTED]"))
    }

    @Test
    fun `crash log scrubber removes content URIs`() {
        val input = "Failed: content://media/external/audio/media/12345"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("content://"))
        assertTrue(scrubbed.contains("[URI_REDACTED]"))
    }

    @Test
    fun `crash log scrubber removes file URIs`() {
        val input = "Cannot read file://storage/emulated/0/private/alarm.mp3"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("file://"))
        assertTrue(scrubbed.contains("[URI_REDACTED]"))
    }

    @Test
    fun `crash log scrubber removes phone numbers`() {
        val input = "Guardian contact: +1 (555) 123-4567 failed"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("555"))
        assertTrue(scrubbed.contains("[PHONE_REDACTED]"))
    }

    @Test
    fun `crash log scrubber removes email addresses`() {
        val input = "User: john.doe@example.com triggered crash"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("john.doe@example.com"))
        assertTrue(scrubbed.contains("[EMAIL_REDACTED]"))
    }

    @Test
    fun `crash log scrubber removes long hex tokens`() {
        val input = "Hue key: a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6 invalid"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"))
        assertTrue(scrubbed.contains("[TOKEN_REDACTED]"))
    }

    @Test
    fun `crash log scrubber removes api key patterns`() {
        val input = "api_key=mysecretkey123 failed auth"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertFalse(scrubbed.contains("mysecretkey123"))
        assertTrue(scrubbed.contains("[SECRET_REDACTED]"))
    }

    @Test
    fun `crash log scrubber preserves stack trace structure`() {
        val input = """Thread: main
Time: 2026-06-13_08-30-00-000
Version: 1.14.0 (82)
Device: Google Pixel 8
Android: 15 (API 35)
---
java.lang.NullPointerException: Attempt to invoke virtual method
	at com.sysadmindoc.alarmclock.service.AlarmService.startAlarm(AlarmService.kt:150)
	at com.sysadmindoc.alarmclock.service.AlarmService.onStartCommand(AlarmService.kt:80)"""
        val scrubbed = CrashLogScrubber.scrub(input)
        assertTrue(scrubbed.contains("NullPointerException"))
        assertTrue(scrubbed.contains("AlarmService.startAlarm"))
        assertTrue(scrubbed.contains("AlarmService.kt:150"))
    }

    @Test
    fun `crash log scrubber does not remove short digit sequences`() {
        val input = "API 35, line 150, alarm id 42"
        val scrubbed = CrashLogScrubber.scrub(input)
        assertEquals(input, scrubbed)
    }

    // --- Helpers ---

    private fun testSession(
        firedEarly: Boolean,
        totalMinutes: Int,
        observedMinutes: Int,
        reason: String,
        mode: String = "CONSERVATIVE"
    ) = ActigraphySession(
        alarmId = 1L,
        startedAt = 1_000_000L,
        endedAt = 2_000_000L,
        targetTime = 1_500_000L,
        totalMinutes = totalMinutes,
        awakeMinutes = 5,
        lightMinutes = 10,
        deepMinutes = totalMinutes - 15,
        averageSleepIndex = 0.8f,
        firedEarly = firedEarly,
        algorithm = "phone_cole_kripke_experimental_v1",
        decisionReason = reason,
        observedMinutesBeforeDecision = observedMinutes,
        smartWakeMode = mode
    )

    private fun defaultGuardianReadiness() = GuardianReadiness(
        enabledAlarmCount = 0,
        smsPath = GuardianSmsPath.INACTIVE,
        hasSendSmsPermission = false,
        hasCallPhonePermission = false
    )

    private fun diagnosticsText(
        sdkInt: Int,
        fullScreenIntentAllowed: Boolean?,
        smartWakeSessionCount: Int = 0,
        smartWakeFiredEarlyCount: Int = 0,
        smartWakeLastDecisionReason: String? = null,
        smartWakeLastObservedMinutes: Int? = null,
        smartWakeMode: String? = null,
        recentIncidentCount: Int = 0,
        latestIncidentType: String? = null,
        latestIncidentStatus: String? = null,
        latestIncidentReason: String? = null,
        guardianReadiness: GuardianReadiness = defaultGuardianReadiness()
    ): String = SupportDiagnosticsFormatter.diagnosticsText(
        generatedAt = Instant.EPOCH,
        appVersion = "test",
        versionCode = 1,
        flavor = "fdroid",
        buildType = "debug",
        packageName = "com.sysadmindoc.alarmclock",
        deviceManufacturer = "Test",
        deviceModel = "Device",
        androidRelease = "14",
        sdkInt = sdkInt,
        notificationPermissionGranted = true,
        exactAlarmsAllowed = true,
        fullScreenIntentAllowed = fullScreenIntentAllowed,
        ignoringBatteryOptimizations = true,
        appStandbyBucket = "ACTIVE (10)",
        guardianReadiness = guardianReadiness,
        totalAlarms = 0,
        enabledAlarms = 0,
        nextTriggerTime = null,
        crashLogCount = 0,
        smartWakeSessionCount = smartWakeSessionCount,
        smartWakeFiredEarlyCount = smartWakeFiredEarlyCount,
        smartWakeLastDecisionReason = smartWakeLastDecisionReason,
        smartWakeLastObservedMinutes = smartWakeLastObservedMinutes,
        smartWakeMode = smartWakeMode,
        recentIncidentCount = recentIncidentCount,
        latestIncidentType = latestIncidentType,
        latestIncidentStatus = latestIncidentStatus,
        latestIncidentReason = latestIncidentReason,
        stats = AlarmStats()
    )
}
