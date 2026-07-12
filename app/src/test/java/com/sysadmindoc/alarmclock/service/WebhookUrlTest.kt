package com.sysadmindoc.alarmclock.service

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
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
    private val payloadAdapter = Moshi.Builder()
        .build()
        .adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )

    @Test fun `rejects http url`() = assertFalse(ok("http://example.com/hook"))
    @Test fun `accepts https url`() = assertTrue(ok("https://example.com/hook"))
    @Test fun `accepts trimmed https url`() = assertTrue(ok("  https://example.com/x  "))

    @Test fun `rejects blank`() = assertFalse(ok(""))
    @Test fun `rejects whitespace only`() = assertFalse(ok("   "))
    @Test fun `rejects javascript scheme`() = assertFalse(ok("javascript:alert(1)"))
    @Test fun `rejects file scheme`() = assertFalse(ok("file:///etc/passwd"))
    @Test fun `rejects bare host`() = assertFalse(ok("example.com"))
    @Test fun `rejects garbage`() = assertFalse(ok("not a url"))

    @Test
    fun `signature headers use timestamped HMAC over raw body`() {
        val headers = WebhookService.buildSignatureHeaders(
            signingSecret = " secret ",
            timestampEpochSeconds = 1_700_000_000L,
            body = """{"event":"test"}"""
        )

        assertEquals("1700000000", headers!!.timestamp)
        assertEquals(
            "v1=e6a22eb66e93669c75e7a035a110d9a2ccfa7cdef62d0ecb361671b92718ee9f",
            headers.signature
        )
    }

    @Test
    fun `blank signing secret omits signature headers`() {
        assertEquals(
            null,
            WebhookService.buildSignatureHeaders(
                signingSecret = "  ",
                timestampEpochSeconds = 1_700_000_000L,
                body = """{"event":"test"}"""
            )
        )
    }

    @Test
    fun `signature timestamp freshness allows five minute skew only`() {
        val now = 1_700_000_000_000L

        assertTrue(WebhookService.isSignatureTimestampFresh(1_700_000_000L, now))
        assertTrue(WebhookService.isSignatureTimestampFresh(1_699_999_700L, now))
        assertTrue(WebhookService.isSignatureTimestampFresh(1_700_000_300L, now))
        assertFalse(WebhookService.isSignatureTimestampFresh(1_699_999_699L, now))
        assertFalse(WebhookService.isSignatureTimestampFresh(1_700_000_301L, now))
        assertFalse(WebhookService.isSignatureTimestampFresh(0L, now))
    }

    @Test
    fun `delivery status redacts endpoint and labels`() {
        assertEquals(
            "alarm_fired OK (204)",
            WebhookService.buildDeliveryStatus(
                event = WebhookEvent.AlarmFired,
                successful = true,
                code = 204,
                failure = null
            )
        )
        assertEquals(
            "alarm_missed failed: IllegalStateException",
            WebhookService.buildDeliveryStatus(
                event = WebhookEvent.AlarmMissed,
                successful = false,
                code = null,
                failure = IllegalStateException("https://example.com/secret")
            )
        )
    }

    @Test
    fun `payload includes stable schema fields`() {
        val payload = payloadAdapter.fromJson(
            WebhookService.buildPayloadJson(
                event = WebhookEvent.AlarmFired,
                alarmId = 42,
                label = "Early flight",
                displayTime = "5:30 AM",
                includeLabel = true,
                scheduledForMillis = 1_800_000L,
                fireId = "fire-42",
                occurredAtMillis = 1_700_000_000_000L,
                eventId = "event-1"
            )
        )!!

        assertEquals(1.0, payload["schemaVersion"])
        assertEquals("alarm_fired", payload["event"])
        assertEquals("event-1", payload["eventId"])
        assertEquals("2023-11-14T22:13:20Z", payload["occurredAt"])
        assertEquals(42.0, payload["alarmId"])
        assertEquals("1970-01-01T00:30:00Z", payload["scheduledFor"])
        assertEquals("5:30 AM", payload["displayTime"])
        assertEquals(true, payload["labelIncluded"])
        assertEquals("Early flight", payload["label"])
        assertEquals("fire-42", payload["fireId"])
    }

    @Test
    fun `payload omits label value when label sharing is disabled`() {
        val payload = payloadAdapter.fromJson(
            WebhookService.buildPayloadJson(
                event = WebhookEvent.AlarmMissed,
                alarmId = 7,
                label = "Medical appointment",
                displayTime = "7:00 AM",
                includeLabel = false,
                scheduledForMillis = null,
                fireId = null,
                occurredAtMillis = 1_700_000_000_000L,
                eventId = "event-2"
            )
        )!!

        assertEquals("alarm_missed", payload["event"])
        assertEquals(false, payload["labelIncluded"])
        assertFalse(payload.containsKey("label"))
        assertTrue(payload.containsKey("scheduledFor"))
        assertEquals(null, payload["scheduledFor"])
        assertFalse(payload.containsKey("fireId"))
    }

    @Test
    fun `test payload respects label sharing setting`() {
        val withLabel = payloadAdapter.fromJson(
            WebhookService.buildTestPayloadJson(
                includeLabel = true,
                occurredAtMillis = 1_700_000_000_000L,
                eventId = "test-1"
            )
        )!!
        val withoutLabel = payloadAdapter.fromJson(
            WebhookService.buildTestPayloadJson(
                includeLabel = false,
                occurredAtMillis = 1_700_000_000_000L,
                eventId = "test-2"
            )
        )!!

        assertEquals("test", withLabel["event"])
        assertEquals(true, withLabel["labelIncluded"])
        assertEquals("Test Alarm", withLabel["label"])
        assertEquals("test", withoutLabel["event"])
        assertEquals(false, withoutLabel["labelIncluded"])
        assertFalse(withoutLabel.containsKey("label"))
    }

    @Test
    fun `event enum exposes documented event names`() {
        assertEquals("alarm_fired", WebhookEvent.AlarmFired.wireName)
        assertEquals("alarm_snoozed", WebhookEvent.AlarmSnoozed.wireName)
        assertEquals("alarm_dismissed", WebhookEvent.AlarmDismissed.wireName)
        assertEquals("alarm_missed", WebhookEvent.AlarmMissed.wireName)
        assertEquals("alarm_skipped", WebhookEvent.AlarmSkipped.wireName)
        assertEquals("test", WebhookEvent.Test.wireName)
    }

    @Test
    fun `only fired and missed are wake-critical`() {
        assertTrue(WebhookEvent.AlarmFired.isWakeCritical)
        assertTrue(WebhookEvent.AlarmMissed.isWakeCritical)
        assertFalse(WebhookEvent.AlarmSnoozed.isWakeCritical)
        assertFalse(WebhookEvent.AlarmDismissed.isWakeCritical)
        assertFalse(WebhookEvent.AlarmSkipped.isWakeCritical)
        assertFalse(WebhookEvent.Test.isWakeCritical)
    }

    @Test
    fun `fromWireName round-trips and rejects unknown`() {
        assertEquals(WebhookEvent.AlarmFired, WebhookEvent.fromWireName("alarm_fired"))
        assertEquals(WebhookEvent.AlarmMissed, WebhookEvent.fromWireName("alarm_missed"))
        assertEquals(null, WebhookEvent.fromWireName("nope"))
        assertEquals(null, WebhookEvent.fromWireName(null))
    }

    @Test
    fun `delivery log prepends newest first and caps length`() {
        var log = ""
        log = WebhookService.prependDeliveryLogLine(log, "line1", maxLines = 3)
        log = WebhookService.prependDeliveryLogLine(log, "line2", maxLines = 3)
        log = WebhookService.prependDeliveryLogLine(log, "line3", maxLines = 3)
        log = WebhookService.prependDeliveryLogLine(log, "line4", maxLines = 3)
        assertEquals(listOf("line4", "line3", "line2"), log.lines())
    }

    @Test
    fun `delivery log ignores blank prior lines`() {
        val log = WebhookService.prependDeliveryLogLine("\n\nold\n", "new", maxLines = 5)
        assertEquals(listOf("new", "old"), log.lines())
    }
}
