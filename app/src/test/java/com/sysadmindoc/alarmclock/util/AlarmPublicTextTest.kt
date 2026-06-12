package com.sysadmindoc.alarmclock.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPublicTextTest {

    @Test
    fun `required alarm label hides configured labels`() {
        assertEquals(
            "Alarm",
            AlarmPublicText.requiredAlarmLabel("Medication", hideLabel = true)
        )
    }

    @Test
    fun `optional alarm label preserves blank labels unless hidden`() {
        assertEquals("", AlarmPublicText.optionalAlarmLabel("", hideLabel = false))
        assertEquals("Alarm", AlarmPublicText.optionalAlarmLabel("", hideLabel = true))
    }

    @Test
    fun `firing notification uses time fallback only when labels are visible`() {
        assertEquals(
            "6:30 AM",
            AlarmPublicText.firingNotificationText("", "6:30 AM", hideLabel = false)
        )
        assertEquals(
            "Alarm",
            AlarmPublicText.firingNotificationText("Medication", "6:30 AM", hideLabel = true)
        )
    }

    @Test
    fun `wake confirmation omits private labels`() {
        assertEquals("Awake check: Gym", AlarmPublicText.wakeConfirmTitle("Gym", hideLabel = false))
        assertEquals("Are you awake?", AlarmPublicText.wakeConfirmTitle("Gym", hideLabel = true))
    }

    @Test
    fun `quick settings subtitle uses neutral copy when hidden`() {
        assertEquals("Doctor", AlarmPublicText.quickSettingsSubtitle("Doctor", hideLabel = false))
        assertEquals("Next alarm", AlarmPublicText.quickSettingsSubtitle("Doctor", hideLabel = true))
    }
}
