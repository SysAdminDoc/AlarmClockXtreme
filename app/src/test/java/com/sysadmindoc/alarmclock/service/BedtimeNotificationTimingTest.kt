package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class BedtimeNotificationTimingTest {

    @Test
    fun liveUpdateStartsOnlyInsideFinalHour() {
        val now = 1_000_000L

        assertEquals(false, BedtimeNotificationTiming.shouldUseLiveUpdate(now, now + 61 * 60_000L))
        assertEquals(true, BedtimeNotificationTiming.shouldUseLiveUpdate(now, now + 60 * 60_000L))
        assertEquals(false, BedtimeNotificationTiming.shouldUseLiveUpdate(now, now))
    }

    @Test
    fun progressCountsTowardReminderTime() {
        val now = 1_000_000L
        val oneHour = now + 60 * 60_000L
        val halfHour = now + 30 * 60_000L

        assertEquals(0, BedtimeNotificationTiming.liveUpdateProgress(now, oneHour))
        assertEquals(500, BedtimeNotificationTiming.liveUpdateProgress(now, halfHour))
        assertEquals(
            BedtimeNotificationTiming.LIVE_UPDATE_PROGRESS_MAX,
            BedtimeNotificationTiming.liveUpdateProgress(now, now)
        )
    }

    @Test
    fun firstCountdownUpdateWaitsUntilFinalHour() {
        val now = 1_000_000L
        val reminder = now + 90 * 60_000L

        assertEquals(reminder - 60 * 60_000L, BedtimeNotificationTiming.firstCountdownUpdateAt(now, reminder))
    }

    @Test
    fun firstCountdownUpdateStartsImmediatelyWhenAlreadyInsideFinalHour() {
        val now = 1_000_000L
        val reminder = now + 45 * 60_000L

        assertEquals(now + 1_000L, BedtimeNotificationTiming.firstCountdownUpdateAt(now, reminder))
    }

    @Test
    fun refreshesWhenRemainingMinuteLabelDrops() {
        val now = 1_000_000L
        val reminder = now + 9 * 60_000L + 30_000L

        assertEquals(31_000L, BedtimeNotificationTiming.millisUntilNextRefresh(now, reminder))
    }

    @Test
    fun nextDailyReminderSkipsPastNow() {
        val now = 100_000L
        val staleReminder = now - 2 * 24 * 60 * 60_000L

        assertEquals(now + 24 * 60 * 60_000L, BedtimeNotificationTiming.nextDailyReminderTime(staleReminder, now))
    }
}
