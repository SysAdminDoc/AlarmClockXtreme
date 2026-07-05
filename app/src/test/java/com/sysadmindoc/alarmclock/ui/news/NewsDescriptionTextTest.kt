package com.sysadmindoc.alarmclock.ui.news

import org.junit.Assert.assertEquals
import org.junit.Test

class NewsDescriptionTextTest {

    @Test
    fun stripHtmlPreservesBoundariesAroundAdjacentLinks() {
        val raw = """
            National Mall fireworks reward those who had to sweat out a long wait
            <a href="https://example.com/post">The Washington Post</a><a href="https://example.com/more">See more headlines &amp; perspectives</a>
        """.trimIndent()

        assertEquals(
            "National Mall fireworks reward those who had to sweat out a long wait The Washington Post See more headlines & perspectives",
            stripHtml(raw)
        )
    }

    @Test
    fun stripHtmlCompactsWhitespaceAndDecodesEntities() {
        assertEquals(
            "Wake up & review morning headlines",
            stripHtml("<p>Wake up&nbsp;&amp;&nbsp;review</p><p>morning headlines</p>")
        )
    }
}
