package com.sysadmindoc.alarmclock.data.support

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
