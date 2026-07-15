package com.sysadmindoc.alarmclock.ui.alarmfiring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeAudioDuckingTest {

    @Test
    fun `ducking is active only for an enabled unsolved challenge`() {
        assertTrue(shouldDuckAlarmForChallenge(true, true, false))
        assertFalse(shouldDuckAlarmForChallenge(false, true, false))
        assertFalse(shouldDuckAlarmForChallenge(true, false, false))
        assertFalse(shouldDuckAlarmForChallenge(true, true, true))
    }
}
