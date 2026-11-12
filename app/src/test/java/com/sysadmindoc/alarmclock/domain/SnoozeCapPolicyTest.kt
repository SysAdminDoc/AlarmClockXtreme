package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeCapPolicyTest {

    private fun alarm(
        maxSnoozeCount: Int = 3,
        challengeType: String = "NONE",
        challengeChain: String = "",
        locationDismissEnabled: Boolean = false
    ) = Alarm(
        id = 1L,
        hour = 7,
        minute = 0,
        maxSnoozeCount = maxSnoozeCount,
        challengeType = challengeType,
        challengeChain = challengeChain,
        locationDismissEnabled = locationDismissEnabled
    )

    @Test
    fun `snoozes below the cap just snooze`() {
        val plain = alarm()
        assertEquals(SnoozeCapOutcome.SNOOZE, SnoozeCapPolicy.outcomeFor(plain, 0))
        assertEquals(SnoozeCapOutcome.SNOOZE, SnoozeCapPolicy.outcomeFor(plain, 2))
    }

    @Test
    fun `an uncapped alarm never runs out`() {
        val uncapped = alarm(maxSnoozeCount = 0, challengeType = "TYPING")
        assertEquals(SnoozeCapOutcome.SNOOZE, SnoozeCapPolicy.outcomeFor(uncapped, 99))
        assertTrue(SnoozeCapPolicy.canSnooze(uncapped, 99))
        assertNull(SnoozeCapPolicy.snoozesRemaining(uncapped, 99))
    }

    @Test
    fun `a plain alarm still auto-dismisses once the cap is spent`() {
        assertEquals(SnoozeCapOutcome.AUTO_DISMISS, SnoozeCapPolicy.outcomeFor(alarm(), 3))
        assertEquals(SnoozeCapOutcome.AUTO_DISMISS, SnoozeCapPolicy.outcomeFor(alarm(), 4))
    }

    @Test
    fun `a challenge alarm is never auto-dismissed by the cap`() {
        listOf(
            alarm(challengeType = "TYPING"),
            alarm(challengeType = "MATH_EASY"),
            alarm(challengeType = "NONE", challengeChain = "SHAKE,TYPING")
        ).forEach { protectedAlarm ->
            assertEquals(
                "Cap must not dismiss ${protectedAlarm.challengeType}/${protectedAlarm.challengeChain}",
                SnoozeCapOutcome.REFUSE,
                SnoozeCapPolicy.outcomeFor(protectedAlarm, 3)
            )
            assertFalse(SnoozeCapPolicy.canSnooze(protectedAlarm, 3))
        }
    }

    @Test
    fun `location dismissal counts as a gate the cap must not skip`() {
        val located = alarm(locationDismissEnabled = true)

        assertTrue(SnoozeCapPolicy.hasDismissGate(located))
        assertEquals(SnoozeCapOutcome.REFUSE, SnoozeCapPolicy.outcomeFor(located, 3))
    }

    @Test
    fun `an alarm with nothing to satisfy has no gate`() {
        assertFalse(SnoozeCapPolicy.hasDismissGate(alarm()))
    }

    @Test
    fun `remaining count counts down and floors at zero`() {
        val capped = alarm(maxSnoozeCount = 2)
        assertEquals(2, SnoozeCapPolicy.snoozesRemaining(capped, 0))
        assertEquals(1, SnoozeCapPolicy.snoozesRemaining(capped, 1))
        assertEquals(0, SnoozeCapPolicy.snoozesRemaining(capped, 2))
        assertEquals(0, SnoozeCapPolicy.snoozesRemaining(capped, 7))
    }

    @Test
    fun `a corrupt negative snooze count is treated as none used`() {
        assertEquals(3, SnoozeCapPolicy.snoozesRemaining(alarm(), -5))
        assertEquals(SnoozeCapOutcome.SNOOZE, SnoozeCapPolicy.outcomeFor(alarm(), -5))
    }

    @Test
    fun `a cap of one leaves no second snooze on a challenge alarm`() {
        val strict = alarm(maxSnoozeCount = 1, challengeType = "TYPING")
        assertTrue(SnoozeCapPolicy.canSnooze(strict, 0))
        assertFalse(SnoozeCapPolicy.canSnooze(strict, 1))
    }

    @Test
    fun `snooze switched off refuses every request`() {
        listOf(
            alarm(maxSnoozeCount = 0),
            alarm(maxSnoozeCount = 3),
            alarm(maxSnoozeCount = 3, challengeType = "TYPING")
        ).forEach { base ->
            val noSnooze = base.copy(snoozeEnabled = false)
            listOf(-1, 0, 2, 3, 99).forEach { used ->
                assertEquals(SnoozeCapOutcome.REFUSE, SnoozeCapPolicy.outcomeFor(noSnooze, used))
                assertFalse(SnoozeCapPolicy.canSnooze(noSnooze, used))
                assertEquals(0, SnoozeCapPolicy.snoozesRemaining(noSnooze, used))
            }
        }
    }

    @Test
    fun `snooze switched off never turns a snooze press into a dismiss`() {
        // A plain capped alarm past its cap would auto-dismiss with snooze on.
        val spent = alarm(maxSnoozeCount = 1)
        assertEquals(SnoozeCapOutcome.AUTO_DISMISS, SnoozeCapPolicy.outcomeFor(spent, 1))
        assertEquals(
            SnoozeCapOutcome.REFUSE,
            SnoozeCapPolicy.outcomeFor(spent.copy(snoozeEnabled = false), 1)
        )
    }
}
