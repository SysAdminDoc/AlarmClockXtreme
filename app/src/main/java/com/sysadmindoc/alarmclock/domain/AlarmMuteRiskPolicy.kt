package com.sysadmindoc.alarmclock.domain

/**
 * Pure decision for whether the current Do-Not-Disturb / Zen state can silence
 * an alarm.
 *
 * `USAGE_ALARM` streams bypass every normal DND filter (Priority, Alarms-only),
 * so those never mute the alarm. The one filter that also silences alarms is
 * total silence (`NotificationManager.INTERRUPTION_FILTER_NONE`). When the user
 * has left the device in that mode, an armed alarm can fail to make a sound —
 * a real, easily-missed cause of "my alarm didn't go off".
 *
 * Kept Android-free so it can be unit-tested with plain ints; the caller reads
 * `NotificationManager.getCurrentInterruptionFilter()` and passes it in.
 */
object AlarmMuteRiskPolicy {

    /** Mirrors `NotificationManager.INTERRUPTION_FILTER_NONE` (total silence). */
    const val FILTER_TOTAL_SILENCE = 3

    fun alarmsMutedByDnd(currentInterruptionFilter: Int): Boolean =
        currentInterruptionFilter == FILTER_TOTAL_SILENCE
}
