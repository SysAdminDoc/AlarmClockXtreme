package com.sysadmindoc.alarmclock.data.backup

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExportWarningTest {
    @Test
    fun `default settings and alarms do not warn`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(),
            alarms = listOf(Alarm())
        )

        assertFalse(warning.shouldWarn)
        assertTrue(warning.categories.isEmpty())
    }

    @Test
    fun `settings secrets and custom feeds are disclosed before export`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(
                webhookUrl = "https://example.com/hook?token=abc",
                hueBridgeIp = "192.168.1.50",
                hueApiKey = "bridge-secret",
                hueLightIds = "1,2",
                newsFeedUrl = "https://example.com/private-feed.xml?key=abc"
            ),
            alarms = emptyList()
        )

        assertTrue(warning.shouldWarn)
        assertTrue(warning.categories.contains("Webhook endpoint URL"))
        assertTrue(warning.categories.contains("Philips Hue bridge details and API key"))
        assertTrue(warning.categories.contains("Custom news feed URL"))
    }

    @Test
    fun `saved weather location is disclosed before export`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(
                locationName = "Dallas, Texas, United States",
                lastKnownLatitude = 32.78,
                lastKnownLongitude = -96.8
            ),
            alarms = emptyList()
        )

        assertTrue(warning.shouldWarn)
        assertTrue(warning.categories.contains("Saved weather location"))
    }

    @Test
    fun `alarm local references and challenge secrets are disclosed before export`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(),
            alarms = listOf(
                Alarm(
                    ringtoneUri = "content://media/external/audio/media/42",
                    ringtonePool = "android.resource://system/alarm,C:\\Music\\local.mp3",
                    photoMatchUri = "file:///storage/emulated/0/Pictures/wake.jpg",
                    internetRadioUrl = "https://example.com/stream.m3u8?token=abc",
                    wifiDismissSsid = "Home WiFi",
                    guardianPhone = "+15551234567",
                    locationDismissEnabled = true,
                    locationDismissLat = 32.0,
                    locationDismissLng = -96.0,
                    nfcTagId = "04:a1:b2",
                    barcodeValue = "private-code"
                )
            )
        )

        assertTrue(warning.shouldWarn)
        assertTrue(warning.categories.contains("Internet radio stream URLs"))
        assertTrue(warning.categories.contains("Device-local ringtone or photo URIs"))
        assertTrue(warning.categories.contains("Wi-Fi, location, or guardian contact details"))
        assertTrue(warning.categories.contains("NFC or barcode challenge values"))
    }
}
