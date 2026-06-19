package com.sysadmindoc.alarmclock.data

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class AlarmShareCodecTest {

    @Test
    fun deepLinkRoundTripPreservesPortableAlarmFields() {
        val alarm = Alarm(
            id = 42,
            hour = 6,
            minute = 35,
            label = "Gym",
            isEnabled = true,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ringtoneUri = "content://tone",
            vibrationEnabled = false,
            volume = 72,
            snoozeDurationMinutes = 8,
            challengeType = "SIMON_SAYS",
            group = "Training",
            challengeChain = "SIMON_SAYS,STROOP",
            progressiveSnooze = true,
            backupSoundEnabled = true,
            guardianEnabled = true,
            guardianPhone = "+15551234567",
            hardwareButtonAction = "SNOOZE",
            dismissAtRingtoneEnd = true,
            ringtonePool = "content://a,content://b",
            solarOffsetMinutes = -20,
            solarAnchor = "SUNRISE",
            nextTriggerTime = 123_456L
        )

        val link = AlarmShareCodec.createDeepLink(alarm)
        val token = link.substringAfter("${AlarmShareCodec.DATA_PARAM}=")

        val decoded = AlarmShareCodec.decodeToken(token).getOrThrow()

        assertEquals(6, decoded.hour)
        assertEquals(35, decoded.minute)
        assertEquals("Gym", decoded.label)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), decoded.repeatDays)
        assertEquals("content://tone", decoded.ringtoneUri)
        assertFalse(decoded.vibrationEnabled)
        assertEquals(72, decoded.volume)
        assertEquals(8, decoded.snoozeDurationMinutes)
        assertEquals("SIMON_SAYS", decoded.challengeType)
        assertEquals("Training", decoded.group)
        assertEquals("SIMON_SAYS,STROOP", decoded.challengeChain)
        assertTrue(decoded.progressiveSnooze)
        assertTrue(decoded.backupSoundEnabled)
        assertTrue(decoded.guardianEnabled)
        assertEquals("+15551234567", decoded.guardianPhone)
        assertEquals("SNOOZE", decoded.hardwareButtonAction)
        assertTrue(decoded.dismissAtRingtoneEnd)
        assertEquals("content://a,content://b", decoded.ringtonePool)
        assertEquals(-20, decoded.solarOffsetMinutes)
        assertEquals("SUNRISE", decoded.solarAnchor)
    }

    @Test
    fun prepareImportedAlarmDisablesSharedAlarmAndClearsRuntimeIdentity() {
        val imported = AlarmShareCodec.prepareImportedAlarm(
            alarm = Alarm(
                id = 99,
                label = "",
                isEnabled = true,
                createdAt = 1L,
                nextTriggerTime = 123_456L
            ),
            nowMillis = 5_000L
        )

        assertEquals(0L, imported.id)
        assertEquals("Shared alarm", imported.label)
        assertFalse(imported.isEnabled)
        assertEquals(5_000L, imported.createdAt)
        assertEquals(0L, imported.nextTriggerTime)
    }

    @Test
    fun stripRiskyImportedFieldsClearsPrivateReferences() {
        val stripped = AlarmShareCodec.stripRiskyImportedFields(
            Alarm(
                ringtoneUri = "content://tone",
                spotifyUri = "spotify:track:abc",
                nfcTagId = "nfc-secret",
                barcodeValue = "barcode-secret",
                photoMatchUri = "content://photo",
                hueEnabled = true,
                guardianEnabled = true,
                guardianPhone = "+15551234567",
                locationDismissEnabled = true,
                locationDismissLat = 39.0,
                locationDismissLng = -77.0,
                locationDismissRadius = 250,
                wifiDismissSsid = "Home WiFi",
                internetRadioUrl = "https://radio.example/stream",
                morningRoutine = "Medication\nCoffee",
                ringtonePool = "content://a,content://b",
                challengeType = "BARCODE_SCAN",
                challengeChain = "NFC_SCAN,MATH_EASY,WIFI_CONNECT"
            )
        )

        assertEquals("", stripped.ringtoneUri)
        assertEquals("", stripped.spotifyUri)
        assertEquals("", stripped.nfcTagId)
        assertEquals("", stripped.barcodeValue)
        assertEquals("", stripped.photoMatchUri)
        assertFalse(stripped.hueEnabled)
        assertFalse(stripped.guardianEnabled)
        assertEquals("", stripped.guardianPhone)
        assertFalse(stripped.locationDismissEnabled)
        assertEquals(0.0, stripped.locationDismissLat, 0.0)
        assertEquals(0.0, stripped.locationDismissLng, 0.0)
        assertEquals(100, stripped.locationDismissRadius)
        assertEquals("", stripped.wifiDismissSsid)
        assertEquals("", stripped.internetRadioUrl)
        assertEquals("", stripped.morningRoutine)
        assertEquals("", stripped.ringtonePool)
        assertEquals("NONE", stripped.challengeType)
        assertEquals("MATH_EASY", stripped.challengeChain)
    }

    @Test
    fun decodeTokenRejectsInvalidPayload() {
        assertTrue(AlarmShareCodec.decodeToken("not a token").isFailure)
    }

    @Test
    fun decodeTokenRejectsBlankInput() {
        assertTrue(AlarmShareCodec.decodeToken("").isFailure)
        assertTrue(AlarmShareCodec.decodeToken("   ").isFailure)
    }

    @Test
    fun decodeTokenRejectsOversizedToken() {
        // Build a synthetic 32 KB base64-shaped token; the size guard should
        // reject it before Moshi or the Base64 decoder is involved, so a
        // hostile deep-link can't OOM the process by handing us megabytes.
        val oversized = buildString {
            repeat(32 * 1024) { append('A') }
        }
        val result = AlarmShareCodec.decodeToken(oversized)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "Expected oversize message, got: $msg",
            msg.contains("maximum size", ignoreCase = true)
        )
    }

    @Test
    fun tokenStorageKeyIsStableAndDoesNotExposeRawToken() {
        val token = AlarmShareCodec.encodeToken(Alarm(label = "Private appointment"))

        val key = AlarmShareCodec.tokenStorageKey(token)

        assertEquals(key, AlarmShareCodec.tokenStorageKey(token))
        assertFalse(key.contains(token))
        assertTrue(key.startsWith("${token.length}:"))
        assertTrue(key.length < 80)
        assertFalse(key == AlarmShareCodec.tokenStorageKey("${token}x"))
    }
}
