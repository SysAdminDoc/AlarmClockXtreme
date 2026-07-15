package com.sysadmindoc.alarmclock.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPolicyTest {
    @Test
    fun appPreferenceAndAndroidAnimationScaleBothGateMotion() {
        assertTrue(MotionPolicy.allowsMotion(reduceMotionAndFlashing = false, animatorDurationScale = 1f))
        assertFalse(MotionPolicy.allowsMotion(reduceMotionAndFlashing = true, animatorDurationScale = 1f))
        assertFalse(MotionPolicy.allowsMotion(reduceMotionAndFlashing = false, animatorDurationScale = 0f))
    }
}
