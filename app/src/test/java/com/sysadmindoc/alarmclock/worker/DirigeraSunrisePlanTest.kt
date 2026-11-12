package com.sysadmindoc.alarmclock.worker

import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirigeraSunrisePlanTest {

    @Test
    fun hueBrightnessMapsOntoTheHubLevelScale() {
        assertEquals(1, DirigeraSunrisePlan.lightLevel(HueSunriseRampPlan.MIN_BRIGHTNESS))
        assertEquals(50, DirigeraSunrisePlan.lightLevel(127))
        assertEquals(100, DirigeraSunrisePlan.lightLevel(HueSunriseRampPlan.MAX_BRIGHTNESS))
        assertEquals(1, DirigeraSunrisePlan.lightLevel(0))
    }

    @Test
    fun colorStartsAtTheWarmestWhiteAndEndsAtNeutral() {
        // DIRIGERA lights often report the range back to front.
        assertEquals(2202, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MIN_BRIGHTNESS, 4000, 2202))
        assertEquals(4000, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MAX_BRIGHTNESS, 4000, 2202))
        assertEquals(2202, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MIN_BRIGHTNESS, 2202, 4000))
    }

    @Test
    fun colorNeverGoesBeyondWhatTheLightOrNeutralAllows() {
        assertEquals(2700, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MAX_BRIGHTNESS, 2202, 2700))
        assertEquals(4000, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MAX_BRIGHTNESS, 2700, 6500))
        assertEquals(4500, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MIN_BRIGHTNESS, 4500, 6500))
        assertEquals(4500, DirigeraSunrisePlan.kelvin(HueSunriseRampPlan.MAX_BRIGHTNESS, 4500, 6500))
    }

    @Test
    fun colorWarmsUpSteadilyAcrossTheRamp() {
        val kelvins = (HueSunriseRampPlan.MIN_BRIGHTNESS..HueSunriseRampPlan.MAX_BRIGHTNESS)
            .map { DirigeraSunrisePlan.kelvin(it, 4000, 2202) }

        assertTrue(kelvins.zipWithNext().all { (before, after) -> after >= before })
    }

    @Test
    fun eachStepFadesOverItsOwnLengthUpToTheHubLimit() {
        val minute = 60_000L
        assertEquals(30_000L, DirigeraSunrisePlan.transitionMillis(0L, 10 * minute))
        assertEquals(60_000L, DirigeraSunrisePlan.transitionMillis(0L, 30 * minute))
        assertEquals(0L, DirigeraSunrisePlan.transitionMillis(5 * minute, 5 * minute))
        assertEquals(0L, DirigeraSunrisePlan.transitionMillis(5 * minute, 0L))
    }

    @Test
    fun onlyHubsWithAnAddressACredentialAndLightsCount() {
        val hue = AppSettings(hueBridgeIp = "192.168.1.2", hueApiKey = "key", hueLightIds = "1,2")
        val dirigera = AppSettings(
            dirigeraHubIp = "192.168.1.50",
            dirigeraAccessToken = "tok.en",
            dirigeraLightIds = "aaaa-1111_1"
        )

        assertEquals(emptyList<SunriseLightHub>(), SunriseLightHub.configured(AppSettings()))
        assertEquals(listOf(SunriseLightHub.HUE), SunriseLightHub.configured(hue))
        assertEquals(listOf(SunriseLightHub.DIRIGERA), SunriseLightHub.configured(dirigera))
        assertEquals(
            listOf(SunriseLightHub.HUE, SunriseLightHub.DIRIGERA),
            SunriseLightHub.configured(
                hue.copy(
                    dirigeraHubIp = dirigera.dirigeraHubIp,
                    dirigeraAccessToken = dirigera.dirigeraAccessToken,
                    dirigeraLightIds = dirigera.dirigeraLightIds
                )
            )
        )
        assertEquals(
            "paired but no lights picked yet",
            emptyList<SunriseLightHub>(),
            SunriseLightHub.configured(dirigera.copy(dirigeraLightIds = ""))
        )
    }
}
