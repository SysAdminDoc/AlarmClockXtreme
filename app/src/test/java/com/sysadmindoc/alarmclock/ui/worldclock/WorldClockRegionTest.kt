package com.sysadmindoc.alarmclock.ui.worldclock

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The card line shows the part of the zone id just above the city, so a long
 * id never gets cut down to a bare "America/".
 */
class WorldClockRegionTest {

    @Test
    fun `two part id shows the continent`() {
        assertEquals("Europe", worldClockRegion("Europe/London"))
    }

    @Test
    fun `three part id shows the country`() {
        assertEquals("Argentina", worldClockRegion("America/Argentina/Buenos_Aires"))
    }

    @Test
    fun `underscores become spaces`() {
        assertEquals("North Dakota", worldClockRegion("America/North_Dakota/New_Salem"))
    }

    @Test
    fun `id without a slash is shown as is`() {
        assertEquals("UTC", worldClockRegion("UTC"))
    }
}
