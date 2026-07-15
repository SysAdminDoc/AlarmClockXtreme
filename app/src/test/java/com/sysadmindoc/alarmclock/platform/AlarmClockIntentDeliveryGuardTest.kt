package com.sysadmindoc.alarmclock.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmClockIntentDeliveryGuardTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("alarm_clock_intent_delivery", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun immediateDuplicateIsRejectedButLaterIntentIsAccepted() {
        val guard = AlarmClockIntentDeliveryGuard(context)

        assertTrue(guard.claim("same-request", nowElapsed = 10_000L))
        assertTrue(!guard.claim("same-request", nowElapsed = 12_000L))
        assertTrue(guard.claim("same-request", nowElapsed = 16_000L))
    }

    @Test
    fun elapsedRealtimeResetAfterRebootDoesNotSuppressRequest() {
        val guard = AlarmClockIntentDeliveryGuard(context)

        assertTrue(guard.claim("same-request", nowElapsed = 50_000L))
        assertTrue(guard.claim("same-request", nowElapsed = 1_000L))
    }
}
