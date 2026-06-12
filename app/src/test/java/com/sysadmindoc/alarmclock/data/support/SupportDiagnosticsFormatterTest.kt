package com.sysadmindoc.alarmclock.data.support

import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
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
        guardianReadiness: GuardianReadiness = GuardianReadiness(
            enabledAlarmCount = 0,
            smsPath = GuardianSmsPath.INACTIVE,
            hasSendSmsPermission = false,
            hasCallPhonePermission = false
        )
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
