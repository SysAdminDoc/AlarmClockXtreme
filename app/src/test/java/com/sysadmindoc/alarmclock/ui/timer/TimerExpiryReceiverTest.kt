package com.sysadmindoc.alarmclock.ui.timer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The persisted RUNNING -> FINISHED transition is the single-delivery claim.
 * Whether a ViewModel exists or not, the first expiry starts the alert service
 * and a duplicate AlarmManager delivery is ignored.
 */
@RunWith(RobolectricTestRunner::class)
class TimerExpiryReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun persistRunningTimer(id: Int) {
        TimerStore(context).upsert(
            PersistedTimerRecord(
                id = id,
                label = "Tea",
                totalSeconds = 60L,
                remainingMillis = 0L,
                state = TimerState.RUNNING,
                endElapsedRealtime = SystemClock.elapsedRealtime()
            )
        )
    }

    private fun fireExpiry(id: Int) {
        TimerExpiryReceiver().onReceive(
            context,
            Intent(TimerAlarmScheduler.ACTION_TIMER_EXPIRED)
                .putExtra(TimerAlarmScheduler.EXTRA_TIMER_ID, id)
        )
    }

    @After
    fun tearDown() {
        TimerStore(context).replace(emptyList())
    }

    @Test
    fun `expiry starts the ringing foreground service`() {
        persistRunningTimer(1)

        fireExpiry(1)

        val started = shadowOf(context as Application).nextStartedService
        assertEquals(TimerAlarmService::class.java.name, started?.component?.className)
        assertEquals(TimerAlarmService.ACTION_FIRED, started?.action)
    }

    @Test
    fun `duplicate expiry delivery does not start a second service`() {
        persistRunningTimer(2)

        fireExpiry(2)
        val first = shadowOf(context as Application).nextStartedService
        fireExpiry(2)

        assertEquals(TimerAlarmService.ACTION_FIRED, first?.action)
        assertNull(shadowOf(context as Application).nextStartedService)
    }
}
