package com.sysadmindoc.alarmclock.directboot

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectBootAlarmSnapshotTest {

    @Test
    fun `enabled future alarm snapshot is schedulable`() {
        val snapshot = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(id = 42, label = "Morning", ringtoneUri = ""),
            triggerTime = 2_000L,
            timeLabel = "7:00 AM",
            now = 1_000L
        )

        assertTrue(snapshot.isSchedulable(1_000L))
        assertEquals(42L, snapshot.alarmId)
        assertEquals("", snapshot.label)
        assertEquals("7:00 AM", snapshot.timeLabel)
        assertTrue(snapshot.playDefaultSound)
    }

    @Test
    fun `past or unsaved snapshot is not schedulable`() {
        val past = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(id = 42),
            triggerTime = 900L,
            timeLabel = "7:00 AM",
            now = 1_000L
        )
        val unsaved = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(id = 0),
            triggerTime = 2_000L,
            timeLabel = "7:00 AM",
            now = 1_000L
        )

        assertFalse(past.isSchedulable(1_000L))
        assertFalse(unsaved.isSchedulable(1_000L))
    }

    @Test
    fun `direct boot snapshot never preserves custom ringtone uri`() {
        val snapshot = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(
                id = 9,
                label = "Private media alarm",
                ringtoneUri = "content://media/external/audio/media/123"
            ),
            triggerTime = 2_000L,
            timeLabel = "7:00 AM",
            now = 1_000L
        )

        assertTrue(snapshot.playDefaultSound)
        assertEquals("", snapshot.label)
    }

    @Test
    fun `silent and muted alarms do not request direct boot audio`() {
        val silent = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(id = 1, ringtoneUri = "silent"),
            triggerTime = 2_000L,
            timeLabel = "7:00 AM",
            now = 1_000L
        )
        val muted = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(id = 2, overrideSystemVolume = true, volume = 0),
            triggerTime = 2_000L,
            timeLabel = "7:00 AM",
            now = 1_000L
        )

        assertFalse(silent.playDefaultSound)
        assertFalse(muted.playDefaultSound)
    }

    @Test
    fun `direct boot snapshot does not store alarm label and bounds display time`() {
        val label = "x".repeat(120)
        val snapshot = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(id = 3, label = label),
            triggerTime = 2_000L,
            timeLabel = "7:00 AM Eastern Standard Time",
            now = 1_000L
        )

        assertEquals("", snapshot.label)
        assertTrue(snapshot.timeLabel.length <= 32)
    }

    @Test
    fun `direct boot snapshot preserves fixed-zone schedule metadata`() {
        val snapshot = DirectBootAlarmSnapshot.fromAlarm(
            alarm = Alarm(
                id = 8,
                timezonePolicy = Alarm.TIMEZONE_POLICY_FIXED,
                fixedTimezoneId = "Asia/Tokyo"
            ),
            triggerTime = 2_000L,
            timeLabel = "8:00 PM",
            now = 1_000L
        )

        assertEquals(Alarm.TIMEZONE_POLICY_FIXED, snapshot.timezonePolicy)
        assertEquals("Asia/Tokyo", snapshot.fixedTimezoneId)
    }
}
