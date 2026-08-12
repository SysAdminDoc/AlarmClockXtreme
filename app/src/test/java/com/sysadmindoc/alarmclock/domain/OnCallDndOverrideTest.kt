package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnCallDndOverrideTest {

    @Test
    fun `override requires opt in policy access and total silence`() {
        assertTrue(OnCallDndOverride.shouldOverride(true, true, OnCallDndOverride.FILTER_TOTAL_SILENCE))
        assertFalse(OnCallDndOverride.shouldOverride(false, true, OnCallDndOverride.FILTER_TOTAL_SILENCE))
        assertFalse(OnCallDndOverride.shouldOverride(true, false, OnCallDndOverride.FILTER_TOTAL_SILENCE))
        assertFalse(OnCallDndOverride.shouldOverride(true, true, OnCallDndOverride.FILTER_ALARMS_ONLY))
    }

    @Test
    fun `restore only returns an untouched total silence choice`() {
        assertTrue(
            OnCallDndOverride.shouldRestore(
                OnCallDndOverride.FILTER_TOTAL_SILENCE,
                OnCallDndOverride.FILTER_ALARMS_ONLY
            )
        )
        assertFalse(OnCallDndOverride.shouldRestore(2, OnCallDndOverride.FILTER_ALARMS_ONLY))
        assertFalse(OnCallDndOverride.shouldRestore(OnCallDndOverride.FILTER_TOTAL_SILENCE, 2))
    }
}
