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
    fun readinessMarksFdroidMissingSmsAsActionable() {
        val readiness = GuardianEscalationPolicy.readiness(
            flavor = GuardianEscalationPolicy.FDROID_FLAVOR,
            enabledAlarmCount = 2,
            hasSendSmsPermission = false,
            hasCallPhonePermission = false
        )

        assertEquals(2, readiness.enabledAlarmCount)
        assertEquals(GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION, readiness.smsPath)
        assertTrue(readiness.needsSmsPermission)
        assertTrue(readiness.needsCallPermission)
        assertTrue(readiness.needsUserAction)
        assertFalse(readiness.hasCallPhonePermission)
    }

    @Test
    fun readinessMarksPlayAsComposerOnly() {
        val readiness = GuardianEscalationPolicy.readiness(
            flavor = "play",
            enabledAlarmCount = 1,
            hasSendSmsPermission = false,
            hasCallPhonePermission = true
        )

        assertEquals(GuardianSmsPath.SMS_COMPOSER, readiness.smsPath)
        assertFalse(readiness.needsSmsPermission)
        assertFalse(readiness.needsCallPermission)
        assertFalse(readiness.needsUserAction)
        assertTrue(readiness.hasCallPhonePermission)
    }

    @Test
    fun readinessIsInactiveWithoutGuardianAlarms() {
        val readiness = GuardianEscalationPolicy.readiness(
            flavor = GuardianEscalationPolicy.FDROID_FLAVOR,
            enabledAlarmCount = 0,
            hasSendSmsPermission = true,
            hasCallPhonePermission = true
        )

        assertEquals(GuardianSmsPath.INACTIVE, readiness.smsPath)
        assertFalse(readiness.hasEnabledAlarms)
        assertFalse(readiness.needsCallPermission)
        assertFalse(readiness.needsUserAction)
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
