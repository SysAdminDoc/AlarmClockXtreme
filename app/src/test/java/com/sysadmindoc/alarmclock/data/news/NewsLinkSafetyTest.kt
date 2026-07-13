package com.sysadmindoc.alarmclock.data.news

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * News feed links come from an untrusted, user-configurable RSS/Atom source and
 * are handed to `ACTION_VIEW`. Only plain web URLs may be opened.
 */
class NewsLinkSafetyTest {

    @Test
    fun `plain http and https links are openable`() {
        assertTrue(isSafeNewsLink("https://example.com/article"))
        assertTrue(isSafeNewsLink("http://news.example.org/story?id=5"))
        assertTrue(isSafeNewsLink("  https://example.com/leading-space  "))
        assertTrue(isSafeNewsLink("HTTPS://Example.com/Mixed-Case"))
    }

    @Test
    fun `dangerous and non-web schemes are rejected`() {
        assertFalse(isSafeNewsLink("javascript:alert(1)"))
        assertFalse(isSafeNewsLink("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isSafeNewsLink("file:///data/data/com.sysadmindoc.alarmclock/x"))
        assertFalse(isSafeNewsLink("content://media/external/x"))
        assertFalse(isSafeNewsLink("market://details?id=x"))
        assertFalse(isSafeNewsLink("tel:+15555550123"))
        assertFalse(isSafeNewsLink(""))
        assertFalse(isSafeNewsLink("example.com"))
    }

    @Test
    fun `whitespace-smuggled second scheme is rejected`() {
        assertFalse(isSafeNewsLink("https://ok.com\njavascript:alert(1)"))
        assertFalse(isSafeNewsLink("https://ok.com\tintent:x"))
    }
}
