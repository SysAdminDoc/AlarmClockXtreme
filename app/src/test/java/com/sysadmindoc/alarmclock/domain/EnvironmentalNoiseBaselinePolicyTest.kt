package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        // The policy hands back resource ids rather than sentences now, so
        // this asserts the routing instead of the English. Matching on
        // "loud" was the thing that would have broken the moment the copy
        // moved into a translated strings.xml.
        assertEquals(
            EnvironmentalNoiseBaselinePolicy.DEFAULT_REMINDER_TEXT_RES,
            EnvironmentalNoiseBaselinePolicy.notificationTextRes(null)
        )
        assertEquals(
            R.string.bedtime_room_loud_advice,
            EnvironmentalNoiseBaselinePolicy.notificationTextRes(
                EnvironmentalNoiseBaseline(-30f, EnvironmentalNoiseLevel.LOUD)
            )
        )
        assertEquals(
            R.string.bedtime_room_quiet_advice,
            EnvironmentalNoiseBaselinePolicy.notificationTextRes(
                EnvironmentalNoiseBaseline(-60f, EnvironmentalNoiseLevel.QUIET)
            )
        )
        assertEquals(
            R.string.bedtime_noise_level_moderate,
            EnvironmentalNoiseBaselinePolicy.levelLabelRes(EnvironmentalNoiseLevel.MODERATE)
        )
    }
}
