package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmMuteRiskPolicyTest {

    // NotificationManager filter constants.
    private val filterUnknown = 0
    private val filterAll = 1
    private val filterPriority = 2
    private val filterNone = 3
    private val filterAlarms = 4

    @Test
    fun totalSilenceMutesAlarms() {
        assertTrue(AlarmMuteRiskPolicy.alarmsMutedByDnd(filterNone))
        assertTrue(AlarmMuteRiskPolicy.alarmsMutedByDnd(AlarmMuteRiskPolicy.FILTER_TOTAL_SILENCE))
    }

    @Test
    fun otherFiltersLetAlarmsThrough() {
        // USAGE_ALARM bypasses all of these.
        assertFalse(AlarmMuteRiskPolicy.alarmsMutedByDnd(filterUnknown))
        assertFalse(AlarmMuteRiskPolicy.alarmsMutedByDnd(filterAll))
        assertFalse(AlarmMuteRiskPolicy.alarmsMutedByDnd(filterPriority))
        assertFalse(AlarmMuteRiskPolicy.alarmsMutedByDnd(filterAlarms))
    }
}
