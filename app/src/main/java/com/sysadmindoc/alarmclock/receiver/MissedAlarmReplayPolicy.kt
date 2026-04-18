package com.sysadmindoc.alarmclock.receiver

/**
 * v1.5.2: Pure decision function for whether a missed alarm should be
 * replayed on the user's next unlock. Extracted out of
 * [MissedAlarmUnlockReceiver] so the logic can be unit-tested without
 * needing a real BroadcastReceiver / Hilt / DataStore wiring.
 *
 * The receiver deliberately holds no other policy — it just dispatches
 * side effects based on the result of this function.
 */
object MissedAlarmReplayPolicy {

    /** Hard ceiling on how recently the miss must have happened for us to
     *  bother replaying it. Past this point the user has likely moved on. */
    const val REPLAY_WINDOW_MS: Long = 10 * 60 * 1000L

    /**
     * @return true iff the caller should fire the replay.
     *
     * Accepts primitive arguments so tests don't need to build a full
     * [android.content.SharedPreferences] state. [nowMs] is injected so
     * tests control the wall clock.
     */
    fun shouldReplay(
        repeatMissedEnabled: Boolean,
        lastMissedAtMs: Long,
        lastMissedId: Long,
        alarmCurrentlyFiringId: Long,
        nowMs: Long
    ): Decision {
        if (!repeatMissedEnabled) return Decision.DROP_DISABLED
        if (lastMissedId <= 0L) return Decision.DROP_NO_RECORD
        if (lastMissedAtMs <= 0L) return Decision.DROP_NO_RECORD

        val age = nowMs - lastMissedAtMs
        // Half-open so the exact-boundary case doesn't straddle two alarms.
        if (age < 0 || age >= REPLAY_WINDOW_MS) return Decision.DROP_EXPIRED

        // Don't stack a replay on top of a live alarm — the foreground
        // service would fight itself for audio focus.
        if (alarmCurrentlyFiringId != -1L) return Decision.DROP_ALARM_LIVE

        return Decision.REPLAY
    }

    enum class Decision(val shouldReplay: Boolean, val shouldClearState: Boolean) {
        REPLAY(shouldReplay = true, shouldClearState = true),
        DROP_DISABLED(shouldReplay = false, shouldClearState = true),
        DROP_NO_RECORD(shouldReplay = false, shouldClearState = true),
        DROP_EXPIRED(shouldReplay = false, shouldClearState = true),
        DROP_ALARM_LIVE(shouldReplay = false, shouldClearState = true)
    }
}
