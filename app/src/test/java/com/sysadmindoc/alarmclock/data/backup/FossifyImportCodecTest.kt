package com.sysadmindoc.alarmclock.data.backup

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FossifyImportCodecTest {
    @Test
    fun `maps documented Fossify time weekdays label vibration and sound fields`() {
        val parsed = FossifyImportCodec.parse(
            """
            {
              "alarms": [
                {
                  "time": 27000000,
                  "days": 65,
                  "label": "Morning shift",
                  "isEnabled": true,
                  "vibrate": true,
                  "soundName": "Chime",
                  "soundUri": "content://media/external/audio/7"
                },
                {"timeInMinutes": 1385, "enabled": false}
              ]
            }
            """.trimIndent()
        )

        assertEquals(0, parsed.invalidAlarmCount)
        assertEquals(2, parsed.alarms.size)
        with(parsed.alarms[0]) {
            assertEquals(7, hour)
            assertEquals(30, minute)
            assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), repeatDays)
            assertEquals("Morning shift", label)
            assertTrue(vibrationEnabled)
            assertTrue(sourceWasEnabled)
            assertEquals("Chime", soundName)
            assertEquals("content://media/external/audio/7", soundUri)
        }
        assertEquals(23, parsed.alarms[1].hour)
        assertEquals(5, parsed.alarms[1].minute)
    }

    @Test
    fun `invalid rows are disclosed and valid rows remain previewable`() {
        val parsed = FossifyImportCodec.parse(
            """{"alarms":[{"time":3600000},{"time":-1},{"time":86400000},"bad"]}"""
        )

        assertEquals(1, parsed.alarms.size)
        assertEquals(3, parsed.invalidAlarmCount)
    }

    @Test
    fun `mapper always disables imports and replaces unreadable sound with default`() {
        val candidate = FossifyAlarmCandidate(
            hour = 6,
            minute = 45,
            label = "Imported",
            repeatDays = setOf(DayOfWeek.FRIDAY),
            vibrationEnabled = true,
            sourceWasEnabled = true,
            soundName = "Private file",
            soundUri = "file:///private/ringtone.mp3"
        )

        val alarm = FossifyImportMapper.toAlarm(candidate, readableRingtoneUri = "")

        assertFalse(alarm.isEnabled)
        assertEquals(0L, alarm.nextTriggerTime)
        assertEquals("", alarm.ringtoneUri)
        assertEquals("Fossify", alarm.profileName)
    }

    @Test
    fun `rejects missing schema oversized files and alarm floods`() {
        assertThrows(IllegalArgumentException::class.java) { FossifyImportCodec.parse("{}") }
        val alarms = List(FossifyImportCodec.MAX_ALARMS + 1) { "{\"time\":60}" }.joinToString(",")
        assertThrows(IllegalArgumentException::class.java) {
            FossifyImportCodec.parse("{\"alarms\":[$alarms]}")
        }
        val oversized = "{\"alarms\":[],\"padding\":\"" +
            "x".repeat(FossifyImportCodec.MAX_BYTES) + "\"}"
        assertThrows(IllegalArgumentException::class.java) { FossifyImportCodec.parse(oversized) }
    }
}
