package com.sysadmindoc.alarmclock.ui.alarmfiring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshFiringScreenTest {

    private fun decide(
        incomingAlarmId: Long = 7L,
        incomingIsTest: Boolean = false,
        incomingFireId: String = "7:1000"
    ) = needsFreshFiringScreen(
        currentAlarmId = 7L,
        currentIsTest = false,
        currentFireId = "7:1000",
        incomingAlarmId = incomingAlarmId,
        incomingIsTest = incomingIsTest,
        incomingFireId = incomingFireId
    )

    @Test
    fun `the same fire coming back keeps the screen`() {
        assertFalse(decide())
    }

    @Test
    fun `a snooze of the same alarm firing again gets a fresh screen`() {
        assertTrue(decide(incomingFireId = "7:1600"))
    }

    @Test
    fun `a different alarm gets a fresh screen`() {
        assertTrue(decide(incomingAlarmId = 8L, incomingFireId = "8:1000"))
    }

    @Test
    fun `a real alarm replacing a test gets a fresh screen`() {
        assertTrue(decide(incomingIsTest = true))
    }

    @Test
    fun `a launch without a fire id keeps the screen`() {
        assertFalse(decide(incomingFireId = ""))
    }

    @Test
    fun `a launch without an alarm id keeps the screen`() {
        assertFalse(decide(incomingAlarmId = -1L, incomingFireId = "other"))
    }
}
