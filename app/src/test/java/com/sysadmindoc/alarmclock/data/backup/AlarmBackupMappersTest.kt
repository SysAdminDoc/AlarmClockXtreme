package com.sysadmindoc.alarmclock.data.backup

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Backup-import resilience: [AlarmBackup.toAlarmOrNull] must tolerate malformed
 * or out-of-range data from an old/edited/corrupt backup file and never produce
 * an invalid alarm (it sanitizes) or crash the whole import (it returns null on
 * a hard failure so the per-alarm loop can skip it).
 */
class AlarmBackupMappersTest {

    @Test
    fun roundTripPreservesCoreFields() {
        val alarm = Alarm(
            id = 5L,
            hour = 7,
            minute = 30,
            label = "Work",
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            challengeType = "MATH_EASY",
            volume = 80,
            firingBackgroundImageEnabled = true,
            firingBackgroundImageUri = "content://media/backgrounds/wake.jpg",
            firingBackgroundBlurEnabled = false,
            sortOrder = 4_000,
            shiftPattern = "DUPONT",
            shiftPatternStartDate = "2026-07-06",
            timezonePolicy = Alarm.TIMEZONE_POLICY_FIXED,
            fixedTimezoneId = "America/New_York"
        ).sanitized()

        val restored = alarm.toAlarmBackup().toAlarmOrNull()

        assertNotNull(restored)
        requireNotNull(restored)
        assertEquals(7, restored.hour)
        assertEquals(30, restored.minute)
        assertEquals("Work", restored.label)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), restored.repeatDays)
        assertEquals("MATH_EASY", restored.challengeType)
        assertEquals(80, restored.volume)
        assertEquals(true, restored.firingBackgroundImageEnabled)
        assertEquals("content://media/backgrounds/wake.jpg", restored.firingBackgroundImageUri)
        assertEquals(false, restored.firingBackgroundBlurEnabled)
        assertEquals(4_000, restored.sortOrder)
        assertEquals("DUPONT", restored.shiftPattern)
        assertEquals("2026-07-06", restored.shiftPatternStartDate)
        assertEquals(Alarm.TIMEZONE_POLICY_FIXED, restored.timezonePolicy)
        assertEquals("America/New_York", restored.fixedTimezoneId)
    }

    @Test
    fun malformedRepeatDaysAreDroppedCaseInsensitively() {
        val backup = Alarm(id = 1L, hour = 6, minute = 0).toAlarmBackup()
            .copy(repeatDays = listOf("MONDAY", "FUNDAY", " tuesday ", "", "WEDNESDAY"))

        val restored = backup.toAlarmOrNull()

        assertNotNull(restored)
        requireNotNull(restored)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
            restored.repeatDays
        )
    }

    @Test
    fun outOfRangeAndInvalidValuesAreSanitized() {
        val backup = Alarm(id = 2L, hour = 6, minute = 0).toAlarmBackup().copy(
            hour = 99,
            minute = -5,
            volume = 250,
            challengeType = "BOGUS_CHALLENGE",
            vibrationPattern = "weird"
        )

        val restored = backup.toAlarmOrNull()

        assertNotNull(restored)
        requireNotNull(restored)
        assertEquals(23, restored.hour)
        assertEquals(0, restored.minute)
        assertEquals(100, restored.volume)
        assertEquals("NONE", restored.challengeType)
        assertEquals("default", restored.vibrationPattern)
    }

    @Test
    fun emptyRepeatDaysYieldsOneShotAlarm() {
        val backup = Alarm(id = 3L, hour = 8, minute = 15).toAlarmBackup()
            .copy(repeatDays = emptyList())

        val restored = backup.toAlarmOrNull()

        assertNotNull(restored)
        requireNotNull(restored)
        assertTrue(restored.repeatDays.isEmpty())
    }

    @Test
    fun invalidChallengeChainEntriesAreFilteredOut() {
        val backup = Alarm(id = 4L, hour = 9, minute = 0).toAlarmBackup()
            .copy(challengeChain = "MATH_EASY,NONE,GARBAGE,SHAKE")

        val restored = backup.toAlarmOrNull()

        assertNotNull(restored)
        requireNotNull(restored)
        // sanitized() keeps only known challenge types and drops NONE.
        assertEquals("MATH_EASY,SHAKE", restored.challengeChain)
    }

    @Test
    fun invalidFixedTimezoneFallsBackToLocalOnImport() {
        val backup = Alarm(hour = 7, minute = 0).toAlarmBackup().copy(
            timezonePolicy = Alarm.TIMEZONE_POLICY_FIXED,
            fixedTimezoneId = "Invalid/Zone"
        )

        val restored = requireNotNull(backup.toAlarmOrNull())

        assertEquals(Alarm.TIMEZONE_POLICY_LOCAL, restored.timezonePolicy)
        assertEquals("", restored.fixedTimezoneId)
    }
}
