package com.sysadmindoc.alarmclock.receiver

import com.sysadmindoc.alarmclock.receiver.MissedAlarmReplayPolicy.Decision
import com.sysadmindoc.alarmclock.receiver.MissedAlarmReplayPolicy.ACTION_POWER_DISCONNECTED
import com.sysadmindoc.alarmclock.receiver.MissedAlarmReplayPolicy.ACTION_USER_PRESENT
import com.sysadmindoc.alarmclock.receiver.MissedAlarmReplayPolicy.REPLAY_WINDOW_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.5.2: Unit coverage for the repeat-missed-alarm replay window logic
 * that was inlined inside [MissedAlarmUnlockReceiver] through v1.5.1.
 */
class MissedAlarmReplayPolicyTest {

    private val now = 10_000_000L
    private val id = 42L

    @Test
    fun `replay triggers include unlock and power disconnect`() {
        assertTrue(MissedAlarmReplayPolicy.isReplayTrigger(ACTION_USER_PRESENT))
        assertTrue(MissedAlarmReplayPolicy.isReplayTrigger(ACTION_POWER_DISCONNECTED))
        assertFalse(MissedAlarmReplayPolicy.isReplayTrigger("android.intent.action.ACTION_POWER_CONNECTED"))
        assertFalse(MissedAlarmReplayPolicy.isReplayTrigger(null))
    }

    @Test
    fun `incident source identifies the replay trigger`() {
        assertEquals(
            "MissedAlarmUnlockReceiver",
            MissedAlarmReplayPolicy.sourceForTrigger(ACTION_USER_PRESENT)
        )
        assertEquals(
            "MissedAlarmPowerDisconnectReceiver",
            MissedAlarmReplayPolicy.sourceForTrigger(ACTION_POWER_DISCONNECTED)
        )
        assertEquals(
            "MissedAlarmUnlockReceiver",
            MissedAlarmReplayPolicy.sourceForTrigger("android.intent.action.SCREEN_ON")
        )
    }

    @Test
    fun `replays when inside window, feature on, no live alarm`() {
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = now - 60_000L, // 1 min ago
            lastMissedId = id,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.REPLAY, decision)
        assertTrue(decision.shouldReplay)
        assertTrue(decision.shouldClearState)
    }

    @Test
    fun `drops when feature disabled`() {
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = false,
            lastMissedAtMs = now - 60_000L,
            lastMissedId = id,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.DROP_DISABLED, decision)
        assertFalse(decision.shouldReplay)
        assertTrue(decision.shouldClearState) // purge stale state even if off
    }

    @Test
    fun `drops when no recorded id`() {
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = now - 60_000L,
            lastMissedId = -1L,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.DROP_NO_RECORD, decision)
    }

    @Test
    fun `drops when no recorded timestamp`() {
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = 0L,
            lastMissedId = id,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.DROP_NO_RECORD, decision)
    }

    @Test
    fun `drops when outside window`() {
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = now - (REPLAY_WINDOW_MS + 1_000L),
            lastMissedId = id,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.DROP_EXPIRED, decision)
    }

    @Test
    fun `drops at exact boundary because window is half-open`() {
        // v1.5.1 changed the range from `..` to `until` so a second unlock
        // landing exactly on the 10-minute boundary can't replay a miss
        // that is about to be overwritten by the next alarm.
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = now - REPLAY_WINDOW_MS,
            lastMissedId = id,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.DROP_EXPIRED, decision)
    }

    @Test
    fun `drops if timestamp is in the future (clock drift)`() {
        // lastMissedAt after now — system clock jumped back. Treat as
        // expired rather than trying to replay.
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = now + 60_000L,
            lastMissedId = id,
            alarmCurrentlyFiringId = -1L,
            nowMs = now
        )
        assertEquals(Decision.DROP_EXPIRED, decision)
    }

    @Test
    fun `drops if another alarm is currently firing`() {
        val decision = MissedAlarmReplayPolicy.shouldReplay(
            repeatMissedEnabled = true,
            lastMissedAtMs = now - 60_000L,
            lastMissedId = id,
            alarmCurrentlyFiringId = 99L,
            nowMs = now
        )
        assertEquals(Decision.DROP_ALARM_LIVE, decision)
        assertFalse(decision.shouldReplay)
    }

    @Test
    fun `every decision requests state clear so stale state cannot persist`() {
        Decision.entries.forEach { d ->
            assertTrue(
                "Decision $d should always ask caller to clear state",
                d.shouldClearState
            )
        }
    }
}
