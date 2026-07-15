package com.sysadmindoc.alarmclock.receiver

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootHealthCheckPolicyTest {

    @Test
    fun `boot and app update trigger immediate health checks`() {
        assertTrue(shouldCheckAlarmHealthAfter(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(shouldCheckAlarmHealthAfter(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertFalse(shouldCheckAlarmHealthAfter(Intent.ACTION_LOCKED_BOOT_COMPLETED))
        assertFalse(shouldCheckAlarmHealthAfter(Intent.ACTION_TIME_CHANGED))
    }
}
