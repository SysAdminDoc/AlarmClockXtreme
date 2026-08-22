package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm

/** What a snooze request should do once the per-alarm cap comes into play. */
enum class SnoozeCapOutcome {
    /** Snooze normally. */
    SNOOZE,

    /** The cap is spent and the alarm has no dismiss challenge, so stop it. */
    AUTO_DISMISS,

    /** The cap is spent but the alarm is challenge-protected, so keep ringing. */
    REFUSE
}

/**
 * Decides what the snooze cap means for a given alarm.
 *
 * The cap used to record a dismiss and stop the alarm unconditionally, which
 * handed challenge-protected alarms a way out that never involved solving the
 * challenge: tap Snooze once more than the cap and the alarm is gone. Where a
 * challenge is configured, exhausting the cap now takes the snooze away
 * instead of taking the alarm away.
 */
object SnoozeCapPolicy {

    /**
     * Whether something has to be satisfied before this alarm may be dismissed.
     *
     * Location dismissal counts: `FiringUiState.canDismiss` gates on it exactly
     * like a challenge, so letting the snooze cap auto-dismiss such an alarm
     * would be the same escape hatch by another name.
     */
    fun hasDismissGate(alarm: Alarm): Boolean =
        alarm.challengeType.trim().uppercase() != "NONE" ||
            alarm.challengeChain.isNotBlank() ||
            alarm.locationDismissEnabled

    /** True when [alarm] limits snoozes at all (0 means unlimited). */
    fun isCapped(alarm: Alarm): Boolean = alarm.maxSnoozeCount > 0

    /**
     * Snoozes still available after [snoozeCount] have been used, or null when
     * the alarm is uncapped.
     */
    fun snoozesRemaining(alarm: Alarm, snoozeCount: Int): Int? =
        if (!isCapped(alarm)) null
        else (alarm.maxSnoozeCount - snoozeCount.coerceAtLeast(0)).coerceAtLeast(0)

    /** True when the user may still snooze after [snoozeCount] snoozes. */
    fun canSnooze(alarm: Alarm, snoozeCount: Int): Boolean =
        outcomeFor(alarm, snoozeCount) != SnoozeCapOutcome.REFUSE

    fun outcomeFor(alarm: Alarm, snoozeCount: Int): SnoozeCapOutcome {
        val next = snoozeCount.coerceAtLeast(0) + 1
        if (!isCapped(alarm) || next <= alarm.maxSnoozeCount) return SnoozeCapOutcome.SNOOZE
        return if (hasDismissGate(alarm)) {
            SnoozeCapOutcome.REFUSE
        } else {
            SnoozeCapOutcome.AUTO_DISMISS
        }
    }
}
