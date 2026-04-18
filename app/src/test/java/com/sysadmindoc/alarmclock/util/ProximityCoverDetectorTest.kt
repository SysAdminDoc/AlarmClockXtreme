package com.sysadmindoc.alarmclock.util

import com.sysadmindoc.alarmclock.util.ProximityCoverDetector.Companion.DEFAULT_MAX_RANGE_CM
import com.sysadmindoc.alarmclock.util.ProximityCoverDetector.Companion.computeThreshold
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v1.5.2: Pin the clamp behaviour of the cover-to-snooze threshold.
 * Some OEM proximity drivers report `maximumRange` as 0 or microscopic
 * values; without clamping, every sample would look covered (or never)
 * depending on rounding.
 */
class ProximityCoverDetectorTest {

    @Test
    fun `typical 5cm sensor yields 2_5cm threshold`() {
        assertEquals(2.5f, computeThreshold(5f), 0.0001f)
    }

    @Test
    fun `8cm long-range sensor yields 4cm threshold`() {
        assertEquals(4f, computeThreshold(8f), 0.0001f)
    }

    @Test
    fun `driver reporting 0 falls back to default`() {
        assertEquals(DEFAULT_MAX_RANGE_CM * 0.5f, computeThreshold(0f), 0.0001f)
    }

    @Test
    fun `driver reporting microscopic range falls back to default`() {
        // Some buggy drivers report 0.1 or similar. These values would
        // produce a threshold of 0.05 cm (50 micrometres) which no
        // proximity sample will ever report.
        assertEquals(DEFAULT_MAX_RANGE_CM * 0.5f, computeThreshold(0.1f), 0.0001f)
        assertEquals(DEFAULT_MAX_RANGE_CM * 0.5f, computeThreshold(0.5f), 0.0001f)
    }

    @Test
    fun `driver reporting just above floor uses sensor value`() {
        // 0.51 is silly but above the 0.5 cutoff, so the helper trusts it.
        assertEquals(0.255f, computeThreshold(0.51f), 0.0001f)
    }

    @Test
    fun `driver reporting negative range falls back to default`() {
        assertEquals(DEFAULT_MAX_RANGE_CM * 0.5f, computeThreshold(-1f), 0.0001f)
    }
}
