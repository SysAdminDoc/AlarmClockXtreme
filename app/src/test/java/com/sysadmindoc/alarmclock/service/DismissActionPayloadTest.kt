package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The editor uses the same check the executor applies at dismiss time, so a
 * value that would be silently rejected is flagged while the field is still on
 * screen.
 */
class DismissActionPayloadTest {

    @Test
    fun `a blank payload is never acceptable for a real action`() {
        assertFalse(DismissActionExecutor.isAcceptablePayload("WEBHOOK", ""))
        assertFalse(DismissActionExecutor.isAcceptablePayload("BROADCAST", "   "))
        assertFalse(DismissActionExecutor.isAcceptablePayload("HUE_SCENE", ""))
    }

    @Test
    fun `webhook payloads must clear the same gate the executor uses`() {
        assertTrue(
            DismissActionExecutor.isAcceptablePayload("WEBHOOK", "https://hooks.example.com/acx")
        )
        assertFalse(
            DismissActionExecutor.isAcceptablePayload("WEBHOOK", "http://hooks.example.com/acx")
        )
        assertFalse(DismissActionExecutor.isAcceptablePayload("WEBHOOK", "not a url"))
    }

    @Test
    fun `broadcast actions accept an intent action and nothing else`() {
        assertTrue(
            DismissActionExecutor.isAcceptablePayload("BROADCAST", "com.example.ALARM_DISMISSED")
        )
        assertFalse(DismissActionExecutor.isAcceptablePayload("BROADCAST", "9lives"))
        assertFalse(DismissActionExecutor.isAcceptablePayload("BROADCAST", "has space"))
        assertFalse(
            DismissActionExecutor.isAcceptablePayload("BROADCAST", "x".repeat(201))
        )
    }

    @Test
    fun `hue scene payloads must parse into a scene target`() {
        assertTrue(DismissActionExecutor.isAcceptablePayload("HUE_SCENE", "AY6zPz1nEmDLxdI"))
        assertFalse(DismissActionExecutor.isAcceptablePayload("HUE_SCENE", " "))
    }

    @Test
    fun `an unknown action type is rejected rather than assumed harmless`() {
        assertFalse(DismissActionExecutor.isAcceptablePayload("SHELL", "rm -rf /"))
    }

    @Test
    fun `the type check is case and whitespace tolerant`() {
        assertTrue(
            DismissActionExecutor.isAcceptablePayload(" webhook ", "https://hooks.example.com/acx")
        )
    }
}
