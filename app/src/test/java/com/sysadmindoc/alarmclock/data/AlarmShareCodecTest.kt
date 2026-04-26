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
    fun decodeTokenRejectsInvalidPayload() {
        assertTrue(AlarmShareCodec.decodeToken("not a token").isFailure)
    }
}
