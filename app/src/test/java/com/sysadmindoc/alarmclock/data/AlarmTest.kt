package com.sysadmindoc.alarmclock.data

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeType
import com.sysadmindoc.alarmclock.ui.timer.TimerUiState
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AlarmTest {

    @Test
    fun `time property returns correct LocalTime`() {
        val alarm = Alarm(hour = 14, minute = 30)
        assertEquals(LocalTime.of(14, 30), alarm.time)
    }

    @Test
    fun `repeatLabel returns Once for empty days`() {
        val alarm = Alarm(repeatDays = emptySet())
        assertEquals("Once", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns Every day for all days`() {
        val alarm = Alarm(repeatDays = DayOfWeek.entries.toSet())
        assertEquals("Every day", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns Weekdays for Monday-Friday`() {
        val alarm = Alarm(repeatDays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ))
        assertEquals("Weekdays", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns Weekend for Saturday-Sunday`() {
        val alarm = Alarm(repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertEquals("Weekend", alarm.repeatLabel)
    }

    @Test
    fun `repeatLabel returns custom day names for partial selection`() {
        val alarm = Alarm(repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertTrue(alarm.repeatLabel.contains("Mon"))
        assertTrue(alarm.repeatLabel.contains("Wed"))
    }

    @Test
    fun `default alarm has sensible defaults`() {
        val alarm = Alarm()
        assertEquals(9, alarm.hour)
        assertEquals(0, alarm.minute)
        assertEquals("", alarm.label)
        assertTrue(alarm.isEnabled)
        assertTrue(alarm.vibrationEnabled)
        assertEquals(100, alarm.volume)
        assertEquals(10, alarm.snoozeDurationMinutes)
        assertEquals("NONE", alarm.challengeType)
        assertFalse(alarm.holdToDismissEnabled)
    }

    @Test
    fun `alarm with midnight time`() {
        val alarm = Alarm(hour = 0, minute = 0)
        assertEquals(LocalTime.MIDNIGHT, alarm.time)
    }

    @Test
    fun `alarm with max time`() {
        val alarm = Alarm(hour = 23, minute = 59)
        assertEquals(LocalTime.of(23, 59), alarm.time)
    }

    @Test
    fun `sanitized clamps invalid values and normalizes noisy text`() {
        val alarm = Alarm(
            hour = 99,
            minute = 99,
            label = "  Morning run  ",
            vibrationIntensity = 99,
            volume = -10,
            snoozeDurationMinutes = 0,
            maxSnoozeCount = 99,
            group = "  Fitness  ",
            walkStepsRequired = 0,
            wakeConfirmDelayMinutes = 0,
            smartAlarmWindowMinutes = 999,
            specificDate = "not-a-date",
            profileName = "  Daily  ",
            ringtoneUri = " content://tone/" + "r".repeat(3_000),
            nfcTagId = " n".repeat(200),
            barcodeValue = " b".repeat(600),
            spotifyUri = " spotify:track:" + "s".repeat(3_000),
            photoMatchUri = " content://photo/" + "p".repeat(3_000),
            guardianPhone = " +1555" + "9".repeat(80),
            guardianDelaySec = 1,
            locationDismissRadius = 1,
            wifiDismissSsid = " WiFi-" + "x".repeat(120),
            internetRadioUrl = " https://radio.example/" + "i".repeat(3_000),
            challengeType = "not-real",
            vibrationPattern = "buzzstorm",
            challengeChain = "MATH_EASY, ,NOT_REAL,STROOP,NONE",
            morningRoutine = (1..20).joinToString("\n") { " step-$it " + "m".repeat(120) },
            hardwareButtonAction = "volume-up",
            ringtonePool = (1..25).joinToString(",") { " tone://$it " } + ",tone://1",
            solarOffsetMinutes = 9999,
            solarAnchor = "dusk"
        )

        val sanitized = alarm.sanitized()

        assertEquals(23, sanitized.hour)
        assertEquals(59, sanitized.minute)
        assertEquals("Morning run", sanitized.label)
        assertEquals(2, sanitized.vibrationIntensity)
        assertEquals(0, sanitized.volume)
        assertEquals(1, sanitized.snoozeDurationMinutes)
        assertEquals(20, sanitized.maxSnoozeCount)
        assertEquals("NONE", sanitized.challengeType)
        assertEquals("Fitness", sanitized.group)
        assertEquals("default", sanitized.vibrationPattern)
        assertEquals(1, sanitized.walkStepsRequired)
        assertEquals(1, sanitized.wakeConfirmDelayMinutes)
        assertEquals(60, sanitized.smartAlarmWindowMinutes)
        assertEquals("MATH_EASY,STROOP", sanitized.challengeChain)
        assertEquals("", sanitized.specificDate)
        assertEquals("Daily", sanitized.profileName)
        assertTrue(sanitized.ringtoneUri.startsWith("content://tone/"))
        assertTrue(sanitized.ringtoneUri.length <= 2_048)
        assertTrue(sanitized.nfcTagId.length <= 128)
        assertTrue(sanitized.barcodeValue.length <= 512)
        assertTrue(sanitized.spotifyUri.length <= 2_048)
        assertTrue(sanitized.photoMatchUri.length <= 2_048)
        assertEquals(40, sanitized.guardianPhone.length)
        assertEquals(30, sanitized.guardianDelaySec)
        assertEquals(25, sanitized.locationDismissRadius)
        assertTrue(sanitized.wifiDismissSsid.length <= 64)
        assertTrue(sanitized.internetRadioUrl.startsWith("https://radio.example/"))
        assertTrue(sanitized.internetRadioUrl.length <= 2_048)
        val routineItems = sanitized.morningRoutine.lines()
        assertEquals(12, routineItems.size)
        assertTrue(routineItems.all { it.length <= 80 })
        assertEquals("NONE", sanitized.hardwareButtonAction)
        assertEquals(20, sanitized.ringtonePool.split(",").size)
        assertEquals("tone://1", sanitized.ringtonePool.substringBefore(","))
        assertEquals(720, sanitized.solarOffsetMinutes)
        assertEquals("SUNRISE", sanitized.solarAnchor)
    }

    @Test
    fun `timer ui canStart ignores all-zero input`() {
        assertFalse(TimerUiState(inputDigits = "000000").canStart)
        assertTrue(TimerUiState(inputDigits = "15").canStart)
    }

    @Test
    fun `every ChallengeType survives sanitized round-trip`() {
        // Regression guard for the v1.6.0 bug where ROCK_PAPER_SCISSORS,
        // EMOJI_MEMORY, TYPING_SPEED, and WORDLE were defined in the enum
        // but missing from VALID_CHALLENGE_TYPES, causing sanitized() to
        // silently rewrite them to "NONE" on every backup / share / DataStore
        // round-trip. If a future ChallengeType is added but the whitelist
        // isn't updated, this test fails fast.
        ChallengeType.entries.forEach { type ->
            val alarm = Alarm(challengeType = type.name).sanitized()
            assertEquals(
                "ChallengeType.${type.name} must survive Alarm.sanitized() round-trip — " +
                    "add ${type.name} to Alarm.VALID_CHALLENGE_TYPES",
                type.name,
                alarm.challengeType
            )
        }
    }

    @Test
    fun `weatherEarlyMinutes clamped to 0-60`() {
        assertEquals(0, Alarm(weatherEarlyMinutes = -5).sanitized().weatherEarlyMinutes)
        assertEquals(15, Alarm(weatherEarlyMinutes = 15).sanitized().weatherEarlyMinutes)
        assertEquals(60, Alarm(weatherEarlyMinutes = 120).sanitized().weatherEarlyMinutes)
    }

    @Test
    fun `every ChallengeType is preserved when used in a challenge chain`() {
        // Companion check: the chain sanitizer also reads VALID_CHALLENGE_TYPES,
        // so it must accept every non-NONE type.
        val nonNoneTypes = ChallengeType.entries.filter { it != ChallengeType.NONE }
        val chainInput = nonNoneTypes.joinToString(",") { it.name }
        val sanitized = Alarm(challengeChain = chainInput).sanitized()
        nonNoneTypes.forEach { type ->
            assertTrue(
                "Challenge chain dropped ${type.name} — sanitizer whitelist is stale",
                sanitized.challengeChain.contains(type.name)
            )
        }
    }
}
