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
 * A timer that expires while the app process is dead must ring, not fail
 * silently. The expiry receiver starts [TimerAlarmService] only when no live
 * ViewModel is present (killed process); when the UI is alive it defers to the
 * ViewModel's own sound and just posts the notification.
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
        TimerAlertState.setUiAlive(false)
        TimerStore(context).replace(emptyList())
    }

    @Test
    fun `killed-process expiry starts the ringing foreground service`() {
        TimerAlertState.setUiAlive(false)
        persistRunningTimer(1)

        fireExpiry(1)

        val started = shadowOf(context as Application).nextStartedService
        assertEquals(TimerAlarmService::class.java.name, started?.component?.className)
        assertEquals(TimerAlarmService.ACTION_FIRED, started?.action)
    }

    @Test
    fun `expiry with a live UI does not start the service`() {
        TimerAlertState.setUiAlive(true)
        persistRunningTimer(2)

        fireExpiry(2)

        assertNull(shadowOf(context as Application).nextStartedService)
    }
}
