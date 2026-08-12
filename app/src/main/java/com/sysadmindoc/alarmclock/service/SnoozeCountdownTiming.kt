package com.sysadmindoc.alarmclock.service

/** Refresh policy for the ongoing notification shown while an alarm is snoozed. */
internal object SnoozeCountdownTiming {
    private const val ONE_MINUTE_MS = 60_000L
    private const val MIN_REFRESH_MS = 1_000L

    /** Android 16's ProgressStyle surface is useful throughout the final two minutes. */
    const val LIVE_UPDATE_WINDOW_MS = 2 * ONE_MINUTE_MS

    fun millisUntilNextRefresh(nowMillis: Long, snoozeAtMillis: Long): Long {
        val remaining = snoozeAtMillis - nowMillis
        if (remaining <= 0L) return 0L
        if (remaining > LIVE_UPDATE_WINDOW_MS) {
            return (remaining - LIVE_UPDATE_WINDOW_MS).coerceAtLeast(MIN_REFRESH_MS)
        }

        if (remaining <= ONE_MINUTE_MS) {
            return remaining.coerceAtLeast(MIN_REFRESH_MS)
        }

        val intoCurrentMinute = remaining % ONE_MINUTE_MS
        return (intoCurrentMinute + MIN_REFRESH_MS)
            .coerceIn(MIN_REFRESH_MS, ONE_MINUTE_MS)
    }
}
