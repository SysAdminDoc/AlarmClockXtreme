package com.sysadmindoc.alarmclock.service

internal object BedtimeNotificationTiming {
    private const val ONE_MINUTE_MS = 60_000L
    private const val MIN_REFRESH_MS = 1_000L
    private const val COUNTDOWN_WINDOW_MS = 60 * ONE_MINUTE_MS
    private const val DAY_MS = 24 * 60 * ONE_MINUTE_MS
    const val LIVE_UPDATE_PROGRESS_MAX = 1_000

    fun shouldUseLiveUpdate(nowMillis: Long, reminderTimeMillis: Long): Boolean {
        val remaining = reminderTimeMillis - nowMillis
        return remaining in 1L..COUNTDOWN_WINDOW_MS
    }

    fun liveUpdateProgress(nowMillis: Long, reminderTimeMillis: Long): Int {
        val remaining = (reminderTimeMillis - nowMillis).coerceIn(0L, COUNTDOWN_WINDOW_MS)
        val elapsed = COUNTDOWN_WINDOW_MS - remaining
        return ((elapsed * LIVE_UPDATE_PROGRESS_MAX) / COUNTDOWN_WINDOW_MS)
            .toInt()
            .coerceIn(0, LIVE_UPDATE_PROGRESS_MAX)
    }

    fun millisUntilNextRefresh(nowMillis: Long, reminderTimeMillis: Long): Long {
        val remaining = reminderTimeMillis - nowMillis
        if (remaining <= 0L) return 0L
        if (remaining > COUNTDOWN_WINDOW_MS) {
            return remaining - COUNTDOWN_WINDOW_MS
        }
        if (remaining <= ONE_MINUTE_MS) return remaining.coerceAtLeast(MIN_REFRESH_MS)

        val millisIntoDisplayedMinute = remaining % ONE_MINUTE_MS
        val untilDisplayedMinuteChanges = if (millisIntoDisplayedMinute == 0L) {
            MIN_REFRESH_MS
        } else {
            millisIntoDisplayedMinute + MIN_REFRESH_MS
        }
        return untilDisplayedMinuteChanges.coerceIn(MIN_REFRESH_MS, ONE_MINUTE_MS)
    }

    fun firstCountdownUpdateAt(nowMillis: Long, reminderTimeMillis: Long): Long {
        val remaining = reminderTimeMillis - nowMillis
        return when {
            remaining <= 0L -> 0L
            remaining > COUNTDOWN_WINDOW_MS -> reminderTimeMillis - COUNTDOWN_WINDOW_MS
            else -> nowMillis + MIN_REFRESH_MS
        }
    }

    fun nextDailyReminderTime(previousReminderTimeMillis: Long, nowMillis: Long): Long {
        var next = if (previousReminderTimeMillis > 0L) {
            previousReminderTimeMillis + DAY_MS
        } else {
            nowMillis + DAY_MS
        }
        while (next <= nowMillis) {
            next += DAY_MS
        }
        return next
    }
}
