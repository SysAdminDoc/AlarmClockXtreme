package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LongPressThresholdTest {
    @Test
    fun `coerces hold duration to accessible safe bounds`() {
        assertEquals(LongPressThreshold.MIN_MILLIS, LongPressThreshold.coerceMillis(1))
        assertEquals(1_500, LongPressThreshold.coerceMillis(1_500))
        assertEquals(LongPressThreshold.MAX_MILLIS, LongPressThreshold.coerceMillis(10_000))
    }
}
