package com.sysadmindoc.alarmclock.data

import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeType
import com.sysadmindoc.alarmclock.ui.timer.TimerUiState
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertNull

class AlarmTest {

    @Test
    fun `time property returns correct LocalTime`() {
        val alarm = Alarm(hour = 14, minute = 30)
        assertEquals(LocalTime.of(14, 30), alarm.time)
    }

    // repeatLabel used to return English straight from the entity, so these
    // asserted the words. It hands back a resource id now, which is the only
    // form a Room entity can offer without a Context, so they assert the id.

    @Test
    fun `an alarm with no repeat days names itself Once`() {
        assertEquals(R.string.alarm_repeat_once, Alarm(repeatDays = emptySet()).repeatLabelRes)
    }

    @Test
    fun `every day is its own name, not a list of seven`() {
        assertEquals(
            R.string.alarm_repeat_every_day,
            Alarm(repeatDays = DayOfWeek.entries.toSet()).repeatLabelRes
        )
    }

    @Test
    fun `Monday to Friday is Weekdays`() {
        val alarm = Alarm(repeatDays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ))
        assertEquals(R.string.alarm_repeat_weekdays, alarm.repeatLabelRes)
    }

    @Test
    fun `Saturday and Sunday is Weekend`() {
        val alarm = Alarm(repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertEquals(R.string.alarm_repeat_weekend, alarm.repeatLabelRes)
    }

    @Test
    fun `an arbitrary set of days has no name, and lists the days instead`() {
        val alarm = Alarm(repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))

        // null is the signal to the caller that there is nothing to name.
        assertNull(alarm.repeatLabelRes)
        assertEquals("Mon, Wed", alarm.repeatDayNames(Locale.US))
        // The day names come from the locale, not from the enum constant.
        assertEquals("lun., mer.", alarm.repeatDayNames(Locale.FRANCE))
    }

    @Test
    fun `the support bundle gets a stable token, not the display label`() {
        // A supporter reading a diagnostics dump should see the same value
        // whatever language the phone was set to.
        assertEquals("ONCE", Alarm(repeatDays = emptySet()).repeatWireLabel)
        assertEquals("DAILY", Alarm(repeatDays = DayOfWeek.entries.toSet()).repeatWireLabel)
        assertEquals(
            "MON,WED",
            Alarm(repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)).repeatWireLabel
        )
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
        assertEquals(0, alarm.sortOrder)
        assertEquals("", alarm.shiftPattern)
        assertEquals("", alarm.shiftPatternStartDate)
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
            firingBackgroundImageEnabled = true,
            firingBackgroundImageUri = " content://background/" + "g".repeat(3_000),
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
            solarAnchor = "dusk",
            shiftPattern = "panama",
            shiftPatternStartDate = " 2026-07-06 ",
            sortOrder = -42
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
        assertTrue(sanitized.firingBackgroundImageEnabled)
        assertTrue(sanitized.firingBackgroundImageUri.startsWith("content://background/"))
        assertTrue(sanitized.firingBackgroundImageUri.length <= 2_048)
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
        assertEquals("PANAMA", sanitized.shiftPattern)
        assertEquals("2026-07-06", sanitized.shiftPatternStartDate)
        assertTrue(sanitized.isRecurringSchedule)
        assertEquals(0, sanitized.sortOrder)
    }

    @Test
    fun `invalid shift pattern anchor disables shift schedule`() {
        val sanitized = Alarm(
            shiftPattern = "DDNNO",
            shiftPatternStartDate = "not-a-date"
        ).sanitized()

        assertEquals("", sanitized.shiftPattern)
        assertEquals("", sanitized.shiftPatternStartDate)
        assertFalse(sanitized.isRecurringSchedule)
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
    fun `blank firing background uri disables image toggle`() {
        val sanitized = Alarm(
            firingBackgroundImageEnabled = true,
            firingBackgroundImageUri = "   "
        ).sanitized()

        assertFalse(sanitized.firingBackgroundImageEnabled)
        assertEquals("", sanitized.firingBackgroundImageUri)
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
