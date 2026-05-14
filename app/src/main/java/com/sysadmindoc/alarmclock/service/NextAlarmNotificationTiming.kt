package com.sysadmindoc.alarmclock.service

internal object NextAlarmNotificationTiming {
    private const val ONE_MINUTE_MS = 60_000L
    private const val MIN_REFRESH_MS = 1_000L
    private const val LIVE_UPDATE_WINDOW_MS = 2 * 60 * ONE_MINUTE_MS
    const val LIVE_UPDATE_PROGRESS_MAX = 1_000

    fun millisUntilNextRefresh(nowMillis: Long, triggerTimeMillis: Long): Long {
        val remaining = triggerTimeMillis - nowMillis
        if (remaining <= 0L) return 0L
        if (remaining <= ONE_MINUTE_MS) return remaining.coerceAtLeast(MIN_REFRESH_MS)

        val millisIntoDisplayedMinute = remaining % ONE_MINUTE_MS
        val untilDisplayedMinuteChanges = if (millisIntoDisplayedMinute == 0L) {
            MIN_REFRESH_MS
        } else {
            millisIntoDisplayedMinute + MIN_REFRESH_MS
        }
        return untilDisplayedMinuteChanges.coerceIn(MIN_REFRESH_MS, ONE_MINUTE_MS)
    }

    fun shouldUseLiveUpdate(nowMillis: Long, triggerTimeMillis: Long): Boolean {
        val remaining = triggerTimeMillis - nowMillis
        return remaining in 1L..LIVE_UPDATE_WINDOW_MS
    }

    fun liveUpdateProgress(nowMillis: Long, triggerTimeMillis: Long): Int {
        val remaining = (triggerTimeMillis - nowMillis).coerceIn(0L, LIVE_UPDATE_WINDOW_MS)
        val elapsed = LIVE_UPDATE_WINDOW_MS - remaining
        return ((elapsed * LIVE_UPDATE_PROGRESS_MAX) / LIVE_UPDATE_WINDOW_MS)
            .toInt()
            .coerceIn(0, LIVE_UPDATE_PROGRESS_MAX)
    }
}
