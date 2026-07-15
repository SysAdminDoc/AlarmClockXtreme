package com.sysadmindoc.alarmclock.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommuteHistoryStoreTest {
    private val now = 1_800_000_000_000L
    private lateinit var store: CommuteHistoryStore

    @Before
    fun setUp() {
        store = CommuteHistoryStore(ApplicationProvider.getApplicationContext<Context>())
        store.clear()
    }

    @Test
    fun learnedEstimateRequiresThreeFreshSamplesAndUsesConservativePercentile() {
        val samples = listOf(
            CommuteHistorySample(30, now - 3_000),
            CommuteHistorySample(40, now - 2_000)
        )
        assertNull(CommuteHistoryPolicy.estimate(samples, now))

        val estimate = CommuteHistoryPolicy.estimate(
            samples + CommuteHistorySample(50, now - 1_000),
            now
        )
        assertEquals(50, estimate?.minutes)
        assertEquals(3, estimate?.sampleCount)
    }

    @Test
    fun staleAndOutlierSamplesCannotPoisonEstimate() {
        var samples = listOf(
            CommuteHistorySample(30, now - 3_000),
            CommuteHistorySample(35, now - 2_000),
            CommuteHistorySample(40, now - 1_000),
            CommuteHistorySample(90, now - CommuteHistoryPolicy.RETENTION_MILLIS - 1)
        )
        samples = CommuteHistoryPolicy.addSample(samples, 200, now)
        samples = CommuteHistoryPolicy.addSample(samples, 600, now)

        assertEquals(3, samples.size)
        assertEquals(40, CommuteHistoryPolicy.estimate(samples, now)?.minutes)
    }

    @Test
    fun normalizedKeysHideLocationsWhileSeparatingRoutes() {
        val first = CommuteHistoryPolicy.routeKey(40.71281, -74.00601, " 123 MAIN St. ")
        val equivalent = CommuteHistoryPolicy.routeKey(40.71282, -74.00602, "123 main st")
        val otherDestination = CommuteHistoryPolicy.routeKey(40.71281, -74.00601, "500 Park Ave")
        val otherOrigin = CommuteHistoryPolicy.routeKey(41.0, -74.00601, "123 main st")

        assertEquals(first, equivalent)
        assertFalse(first.contains("main", ignoreCase = true))
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
        assertFalse(first == otherDestination)
        assertFalse(first == otherOrigin)
    }

    @Test
    fun storeCapsHistoryAndClearRemovesAllLearnedRoutes() {
        repeat(10) { index ->
            store.record(40.7, -74.0, "Office", 30 + index, now - 10 + index)
        }

        assertEquals(8, store.summary(now).sampleCount)
        assertEquals(1, store.summary(now).routeCount)
        assertTrue(store.estimate(40.7, -74.0, "Office", now) != null)

        store.clear()
        assertEquals(CommuteHistorySummary(0, 0), store.summary(now))
        assertNull(store.estimate(40.7, -74.0, "Office", now))
    }
}
