package com.sysadmindoc.alarmclock.data.news

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Aggregator feeds end each headline with " - Publisher" while the card already
 * shows the publisher under the title.
 */
class NewsDisplayTitleTest {

    private fun item(title: String, source: String) =
        NewsItem(id = "1", title = title, link = "https://example.com", description = "", source = source, publishedAtMillis = null)

    @Test
    fun `trailing publisher suffix is dropped`() {
        assertEquals(
            "Outlook outage hits email",
            item("Outlook outage hits email - ABC News", "ABC News").displayTitle
        )
        assertEquals(
            "Storm moves east",
            item("Storm moves east – Reuters ", "reuters").displayTitle
        )
        assertEquals(
            "Rates hold",
            item("Rates hold | The Wire", "The Wire").displayTitle
        )
    }

    @Test
    fun `titles without the suffix are left alone`() {
        assertEquals("Mid-term results - analysis", item("Mid-term results - analysis", "ABC News").displayTitle)
        assertEquals("Plain headline", item("Plain headline", "").displayTitle)
        // A title that is only the publisher name stays as it is rather than going blank.
        assertEquals(" - ABC News", item(" - ABC News", "ABC News").displayTitle)
    }
}
