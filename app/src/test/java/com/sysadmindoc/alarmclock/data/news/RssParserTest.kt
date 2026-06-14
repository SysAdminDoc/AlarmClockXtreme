package com.sysadmindoc.alarmclock.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RssParserTest {

    // Canonical instant parsed from an unambiguous ISO-8601 UTC timestamp; other
    // representations of the same moment must resolve to the same epoch millis.
    private val canonical = RssParser.parseDate("2026-06-14T08:30:00Z")

    @Test
    fun canonicalParses() {
        assertNotNull(canonical)
    }

    @Test
    fun rfc822RepresentationsMatchCanonical() {
        assertEquals(canonical, RssParser.parseDate("Sun, 14 Jun 2026 08:30:00 GMT"))
        assertEquals(canonical, RssParser.parseDate("Sun, 14 Jun 2026 08:30:00 +0000"))
        assertEquals(canonical, RssParser.parseDate("14 Jun 2026 08:30:00 GMT"))
    }

    @Test
    fun iso8601RepresentationsMatchCanonical() {
        assertEquals(canonical, RssParser.parseDate("2026-06-14T08:30:00+00:00"))
        assertEquals(canonical, RssParser.parseDate("2026-06-14T08:30:00.000Z"))
    }

    @Test
    fun timezoneOffsetIsApplied() {
        // 10:30 at +02:00 is the same instant as 08:30 UTC.
        assertEquals(canonical, RssParser.parseDate("2026-06-14T10:30:00+02:00"))
    }

    @Test
    fun returnsNullForBlankOrGarbage() {
        assertNull(RssParser.parseDate(""))
        assertNull(RssParser.parseDate("   "))
        assertNull(RssParser.parseDate("not a date"))
    }
}
