package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoreEventDetectorTest {
    @Test
    fun estimatesDecibelsFromNormalizedRms() {
        val db = SnoreEventDetector.estimatedDbFromRms(0.002f)

        assertEquals(60f, db, 0.1f)
        assertEquals(0f, SnoreEventDetector.estimatedDbFromRms(0f), 0.0f)
    }

    @Test
    fun ignoresShortLoudClicks() {
        val detector = SnoreEventDetector()

        assertNull(detector.acceptWindow(0L, 50L, 0.01f))
        assertNull(detector.acceptWindow(50L, 1_600L, 0.0001f))
    }

    @Test
    fun emitsMergedBurstAfterQuietGap() {
        val detector = SnoreEventDetector()
        repeat(5) { index ->
            assertNull(detector.acceptWindow(index * 100L, 100L, 0.01f + index * 0.001f))
        }
        assertNull(detector.acceptWindow(500L, 1_000L, 0.0001f))

        val event = detector.acceptWindow(1_500L, 600L, 0.0001f)

        requireNotNull(event)
        assertEquals(0L, event.startedAt)
        assertEquals(500L, event.endedAt)
        assertEquals(500L, event.durationMillis)
        assertEquals(5, event.windowCount)
        assertTrue(event.peakDb > event.averageDb)
    }

    @Test
    fun flushesActiveBurstAtSessionEnd() {
        val detector = SnoreEventDetector()
        repeat(4) { index ->
            detector.acceptWindow(index * 100L, 100L, 0.01f)
        }

        val event = detector.flush()

        requireNotNull(event)
        assertEquals(400L, event.durationMillis)
        assertEquals(4, event.windowCount)
    }
}
