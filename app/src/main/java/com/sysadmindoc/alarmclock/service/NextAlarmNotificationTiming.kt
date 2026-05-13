package com.sysadmindoc.alarmclock.service

internal object NextAlarmNotificationTiming {
    private const val ONE_MINUTE_MS = 60_000L
    private const val MIN_REFRESH_MS = 1_000L

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
}
