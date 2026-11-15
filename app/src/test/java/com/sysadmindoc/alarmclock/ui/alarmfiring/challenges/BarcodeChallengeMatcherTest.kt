package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeChallengeMatcherTest {
    @Test
    fun theSameTextMatches() {
        assertTrue(BarcodeChallengeMatcher.matches("ACX-51 kitchen fridge", "ACX-51 kitchen fridge"))
    }

    @Test
    fun spacesAroundEitherSideAreIgnored() {
        assertTrue(BarcodeChallengeMatcher.matches("  4006381333931\n", "4006381333931"))
        assertTrue(BarcodeChallengeMatcher.matches("4006381333931", " 4006381333931 "))
    }

    @Test
    fun anyOtherDifferenceIsAMiss() {
        assertFalse(BarcodeChallengeMatcher.matches("acx-51 kitchen fridge", "ACX-51 kitchen fridge"))
        assertFalse(BarcodeChallengeMatcher.matches("ACX-51 kitchen  fridge", "ACX-51 kitchen fridge"))
        assertFalse(BarcodeChallengeMatcher.matches("4006381333930", "4006381333931"))
        assertFalse(BarcodeChallengeMatcher.matches("400638133393", "4006381333931"))
        assertFalse(BarcodeChallengeMatcher.matches("", "4006381333931"))
    }
}
