package com.sysadmindoc.alarmclock.data.support

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
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

    private fun diagnosticsText(
        sdkInt: Int,
        fullScreenIntentAllowed: Boolean?
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
        totalAlarms = 0,
        enabledAlarms = 0,
        nextTriggerTime = null,
        crashLogCount = 0,
        stats = AlarmStats()
    )
}
