package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentalNoiseBaselinePolicyTest {
    @Test
    fun classifiesQuietModerateAndLoudRmsLevels() {
        assertEquals(
            EnvironmentalNoiseLevel.QUIET,
            EnvironmentalNoiseBaselinePolicy.fromNormalizedRms(0.001f)?.level
        )
        assertEquals(
            EnvironmentalNoiseLevel.MODERATE,
            EnvironmentalNoiseBaselinePolicy.fromNormalizedRms(0.01f)?.level
        )
        assertEquals(
            EnvironmentalNoiseLevel.LOUD,
            EnvironmentalNoiseBaselinePolicy.fromNormalizedRms(0.05f)?.level
        )
    }

    @Test
    fun rejectsInvalidRmsValues() {
        assertNull(EnvironmentalNoiseBaselinePolicy.fromNormalizedRms(0f))
        assertNull(EnvironmentalNoiseBaselinePolicy.fromNormalizedRms(Float.NaN))
    }

    @Test
    fun notificationCopyFallsBackWhenNoBaselineExists() {
        assertEquals(
            EnvironmentalNoiseBaselinePolicy.DEFAULT_REMINDER_TEXT,
            EnvironmentalNoiseBaselinePolicy.notificationText(null)
        )
        assertTrue(
            EnvironmentalNoiseBaselinePolicy.notificationText(
                EnvironmentalNoiseBaseline(-30f, EnvironmentalNoiseLevel.LOUD)
            ).contains("loud")
        )
    }
}
