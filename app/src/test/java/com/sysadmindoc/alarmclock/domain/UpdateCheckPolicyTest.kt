package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckPolicyTest {

    @Test
    fun `a newer release tag is an update`() {
        assertTrue(UpdateCheckPolicy.isNewer("v1.16.0", "1.15.35"))
        assertTrue(UpdateCheckPolicy.isNewer("v1.15.36", "1.15.35"))
        assertTrue(UpdateCheckPolicy.isNewer("v2.0", "1.99.99"))
        // Numeric, not string, comparison: 10 sorts after 9.
        assertTrue(UpdateCheckPolicy.isNewer("v1.15.10", "1.15.9"))
    }

    @Test
    fun `the same or an older tag is not an update`() {
        assertFalse(UpdateCheckPolicy.isNewer("v1.16.0", "1.16.0"))
        assertFalse(UpdateCheckPolicy.isNewer("1.16", "1.16.0"))
        assertFalse(UpdateCheckPolicy.isNewer("v1.15.35", "1.16.0"))
    }

    @Test
    fun `a malformed tag never counts as an update`() {
        listOf(null, "", "latest", "v", "v1.16.0-beta", "release-1.17", "v1..2", "v1.2.3.4.5", "v9999999.0")
            .forEach { tag ->
                assertFalse("tag $tag", UpdateCheckPolicy.isNewer(tag, "1.15.35"))
            }
        assertNull(UpdateCheckPolicy.parse("v1.16.0-beta"))
    }

    @Test
    fun `an unparseable running version never nags`() {
        assertFalse(UpdateCheckPolicy.isNewer("v9.0.0", "1.16.0-debug"))
    }

    @Test
    fun `the release link is built from the tag`() {
        assertEquals("1.16.0", UpdateCheckPolicy.displayVersion("v1.16.0"))
        assertEquals(
            "https://github.com/SysAdminDoc/AlarmClockXtreme/releases/tag/v1.16.0",
            UpdateCheckPolicy.releasePageUrl("v1.16.0")
        )
        // A tag published without the v lives at its own path, not at /tag/v1.16.0.
        assertEquals(
            "https://github.com/SysAdminDoc/AlarmClockXtreme/releases/tag/1.16.0",
            UpdateCheckPolicy.releasePageUrl("1.16.0")
        )
        assertEquals(
            "https://github.com/SysAdminDoc/AlarmClockXtreme/releases/tag/",
            UpdateCheckPolicy.releasePageUrl("../../evil")
        )
    }
}
