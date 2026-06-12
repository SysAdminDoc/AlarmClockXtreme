package com.sysadmindoc.alarmclock.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianEscalationPolicyTest {
    @Test
    fun fdroidWithPermissionCanSendDirectSms() {
        assertTrue(
            GuardianEscalationPolicy.canSendDirectSms(
                flavor = GuardianEscalationPolicy.FDROID_FLAVOR,
                hasSendSmsPermission = true
            )
        )
    }

    @Test
    fun playNeverUsesDirectSmsEvenIfPermissionGranted() {
        assertFalse(
            GuardianEscalationPolicy.canSendDirectSms(
                flavor = "play",
                hasSendSmsPermission = true
            )
        )
    }

    @Test
    fun missingPermissionUsesComposerPath() {
        assertFalse(
            GuardianEscalationPolicy.canSendDirectSms(
                flavor = GuardianEscalationPolicy.FDROID_FLAVOR,
                hasSendSmsPermission = false
            )
        )
    }

    @Test
    fun sanitisePhoneKeepsTelUriSafeChars() {
        assertEquals(
            "+1555-*123#",
            GuardianEscalationPolicy.sanitisePhone("+1 (555) abc-*123#")
        )
    }

    @Test
    fun sanitisePhoneRejectsUnusableInput() {
        assertNull(GuardianEscalationPolicy.sanitisePhone("call me"))
        assertNull(GuardianEscalationPolicy.sanitisePhone("12"))
    }

    @Test
    fun buildMessageIncludesLabel() {
        val message = GuardianEscalationPolicy.buildMessage("Work alarm")

        assertTrue(message.contains("Work alarm"))
        assertTrue(message.contains("was not dismissed"))
    }
}
