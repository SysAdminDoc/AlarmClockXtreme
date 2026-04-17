package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-level guard around [WebhookService.isAllowedWebhookUrl] to make sure we
 * never silently send alarm event payloads via a non-http(s) scheme like
 * "javascript:" or "file://", and that bare junk (e.g. "not a url") never
 * reaches OkHttp.
 */
class WebhookUrlTest {

    private fun ok(url: String) = WebhookService.isAllowedWebhookUrl(url)

    @Test fun `accepts http url`() = assertTrue(ok("http://example.com/hook"))
    @Test fun `accepts https url`() = assertTrue(ok("https://example.com/hook"))
    @Test fun `accepts trimmed https url`() = assertTrue(ok("  https://example.com/x  "))

    @Test fun `rejects blank`() = assertFalse(ok(""))
    @Test fun `rejects whitespace only`() = assertFalse(ok("   "))
    @Test fun `rejects javascript scheme`() = assertFalse(ok("javascript:alert(1)"))
    @Test fun `rejects file scheme`() = assertFalse(ok("file:///etc/passwd"))
    @Test fun `rejects bare host`() = assertFalse(ok("example.com"))
    @Test fun `rejects garbage`() = assertFalse(ok("not a url"))
}
