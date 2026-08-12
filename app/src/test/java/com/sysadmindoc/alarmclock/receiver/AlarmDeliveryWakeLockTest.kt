package com.sysadmindoc.alarmclock.receiver

import android.content.Context
import android.os.PowerManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Test

class AlarmDeliveryWakeLockTest {
    @Test
    fun acquireUsesPartialWakeLockWithBoundedTimeout() {
        val context = mockk<Context>()
        val powerManager = mockk<PowerManager>()
        val wakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
        every { context.getSystemService(PowerManager::class.java) } returns powerManager
        every {
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlarmClockXtreme::AlarmDeliveryWakeLock"
            )
        } returns wakeLock
        every { wakeLock.setReferenceCounted(false) } just runs

        assertSame(wakeLock, AlarmDeliveryWakeLock.acquire(context))

        verify { wakeLock.setReferenceCounted(false) }
        verify { wakeLock.acquire(AlarmDeliveryWakeLock.TIMEOUT_MS) }
    }
}
