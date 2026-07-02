package com.sysadmindoc.alarmclock.ui.adaptive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutPolicyTest {
    @Test
    fun compactWidthsUseSinglePane() {
        assertFalse(shouldUseTwoPaneLayout(411f))
        assertFalse(shouldUseTwoPaneLayout(839.9f))
    }

    @Test
    fun expandedWidthsUseTwoPane() {
        assertTrue(shouldUseTwoPaneLayout(840f))
        assertTrue(shouldUseTwoPaneLayout(1280f))
    }
}
