package com.sysadmindoc.alarmclock.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the OEM -> battery-guidance mapping: every aggressive OEM must have
 * actionable steps and a DontKillMyApp link, and unaffected devices must produce
 * no warning. This is the drift guard for the proactive reliability surface.
 */
class ManufacturerCompatTest {

    private val aggressiveOems = listOf(
        "samsung", "xiaomi", "redmi", "poco", "oneplus",
        "huawei", "honor", "oppo", "realme", "vivo", "iqoo"
    )

    @Test
    fun everyAggressiveOemHasCompleteGuidance() {
        aggressiveOems.forEach { oem ->
            val guidance = ManufacturerCompat.getGuidance(oem)
            assertNotNull("$oem should have guidance", guidance)
            requireNotNull(guidance)
            assertTrue("$oem guidance must have steps", guidance.steps.isNotEmpty())
            assertTrue("$oem title must be set", guidance.title.isNotBlank())
            assertTrue(
                "$oem must link to dontkillmyapp.com",
                guidance.dontKillMyAppUrl.startsWith("https://dontkillmyapp.com/")
            )
        }
    }

    @Test
    fun needsGuidanceMatchesGuidanceAvailability() {
        // The two must never drift: needsBatteryGuidance is derived from getGuidance.
        aggressiveOems.forEach { oem ->
            assertTrue("$oem should need guidance", ManufacturerCompat.needsBatteryGuidance(oem))
        }
    }

    @Test
    fun unaffectedDevicesProduceNoWarning() {
        listOf("google", "motorola", "nothing", "fairphone", "sony", "", "unknownvendor").forEach { oem ->
            assertNull("$oem should have no guidance", ManufacturerCompat.getGuidance(oem))
            assertFalse("$oem should not need guidance", ManufacturerCompat.needsBatteryGuidance(oem))
        }
    }

    @Test
    fun guidanceIsCaseInsensitive() {
        assertNotNull(ManufacturerCompat.getGuidance("Samsung"))
        assertNotNull(ManufacturerCompat.getGuidance("XIAOMI"))
        assertEquals("Samsung", ManufacturerCompat.getGuidance("samsung")?.manufacturer)
    }

    @Test
    fun colorOsVendorUrlMatchesVendor() {
        assertEquals("https://dontkillmyapp.com/oppo", ManufacturerCompat.getGuidance("oppo")?.dontKillMyAppUrl)
        assertEquals("https://dontkillmyapp.com/realme", ManufacturerCompat.getGuidance("realme")?.dontKillMyAppUrl)
    }
}
