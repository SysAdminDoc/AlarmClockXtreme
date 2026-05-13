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
}
