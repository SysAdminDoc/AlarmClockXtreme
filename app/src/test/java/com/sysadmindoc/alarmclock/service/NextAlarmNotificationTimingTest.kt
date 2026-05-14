package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NextAlarmNotificationTimingTest {

    @Test
    fun refreshesWhenRemainingMinuteLabelDrops() {
        val now = 1_000_000L
        val trigger = now + 9 * 60_000L + 30_000L

        assertEquals(31_000L, NextAlarmNotificationTiming.millisUntilNextRefresh(now, trigger))
    }

    @Test
    fun refreshesShortlyAfterExactMinuteBoundary() {
        val now = 1_000_000L
        val trigger = now + 2 * 60_000L

        assertEquals(1_000L, NextAlarmNotificationTiming.millisUntilNextRefresh(now, trigger))
    }

    @Test
    fun waitsUntilFireWhenAlreadySubMinute() {
        val now = 1_000_000L
        val trigger = now + 45_000L

        assertEquals(45_000L, NextAlarmNotificationTiming.millisUntilNextRefresh(now, trigger))
    }

    @Test
    fun returnsZeroForExpiredAlarm() {
        val now = 1_000_000L
        val trigger = now - 1L

        assertEquals(0L, NextAlarmNotificationTiming.millisUntilNextRefresh(now, trigger))
    }

    @Test
    fun liveUpdateStartsOnlyInsideTwoHourWindow() {
        val now = 1_000_000L

        assertEquals(false, NextAlarmNotificationTiming.shouldUseLiveUpdate(now, now + 3 * 60 * 60_000L))
        assertEquals(true, NextAlarmNotificationTiming.shouldUseLiveUpdate(now, now + 2 * 60 * 60_000L))
        assertEquals(false, NextAlarmNotificationTiming.shouldUseLiveUpdate(now, now))
    }

    @Test
    fun liveUpdateProgressCountsTowardFireTime() {
        val now = 1_000_000L
        val twoHours = now + 2 * 60 * 60_000L
        val oneHour = now + 60 * 60_000L
        val nowTrigger = now

        assertEquals(0, NextAlarmNotificationTiming.liveUpdateProgress(now, twoHours))
        assertEquals(500, NextAlarmNotificationTiming.liveUpdateProgress(now, oneHour))
        assertEquals(NextAlarmNotificationTiming.LIVE_UPDATE_PROGRESS_MAX, NextAlarmNotificationTiming.liveUpdateProgress(now, nowTrigger))
    }
}
