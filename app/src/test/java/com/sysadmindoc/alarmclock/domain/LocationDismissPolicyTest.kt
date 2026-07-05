package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationDismissPolicyTest {

    @Test
    fun `target must be configured and valid`() {
        assertFalse(LocationDismissPolicy.hasTarget(0.0, 0.0))
        assertFalse(LocationDismissPolicy.hasTarget(91.0, -97.0))
        assertFalse(LocationDismissPolicy.hasTarget(32.0, -181.0))
        assertTrue(LocationDismissPolicy.hasTarget(32.7767, -96.7970))
    }

    @Test
    fun `radius is clamped to safe geofence bounds`() {
        assertEquals(25, LocationDismissPolicy.coerceRadius(1))
        assertEquals(100, LocationDismissPolicy.coerceRadius(100))
        assertEquals(5_000, LocationDismissPolicy.coerceRadius(50_000))
    }

    @Test
    fun `check stays locked inside the configured radius`() {
        val result = LocationDismissPolicy.check(
            targetLatitude = 32.7767,
            targetLongitude = -96.7970,
            radiusMeters = 250,
            currentLatitude = 32.7768,
            currentLongitude = -96.7971
        )

        assertNotNull(result)
        assertFalse(requireNotNull(result).outsideFence)
    }

    @Test
    fun `check unlocks outside the configured radius`() {
        val result = LocationDismissPolicy.check(
            targetLatitude = 32.7767,
            targetLongitude = -96.7970,
            radiusMeters = 250,
            currentLatitude = 32.7817,
            currentLongitude = -96.7970
        )

        assertNotNull(result)
        assertTrue(requireNotNull(result).outsideFence)
    }

    @Test
    fun `check rejects missing target or invalid current fix`() {
        assertNull(
            LocationDismissPolicy.check(
                targetLatitude = 0.0,
                targetLongitude = 0.0,
                radiusMeters = 100,
                currentLatitude = 32.0,
                currentLongitude = -96.0
            )
        )
        assertNull(
            LocationDismissPolicy.check(
                targetLatitude = 32.0,
                targetLongitude = -96.0,
                radiusMeters = 100,
                currentLatitude = -91.0,
                currentLongitude = -96.0
            )
        )
    }
}
