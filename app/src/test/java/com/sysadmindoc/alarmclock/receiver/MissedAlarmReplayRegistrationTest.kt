package com.sysadmindoc.alarmclock.receiver

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * USER_PRESENT and ACTION_POWER_DISCONNECTED are not exempt implicit
 * broadcasts, so a manifest-declared receiver never gets them on API 26+.
 * The repeat-missed-alarm replay net only works while the receiver stays
 * context-registered by the Application; this pins that registration.
 */
@RunWith(RobolectricTestRunner::class)
class MissedAlarmReplayRegistrationTest {

    @Test
    fun `replay receiver is context-registered for unlock and unplug`() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val actions = shadowOf(app).registeredReceivers
            .filter { it.broadcastReceiver is MissedAlarmUnlockReceiver }
            .flatMap { wrapper ->
                (0 until wrapper.intentFilter.countActions())
                    .map { wrapper.intentFilter.getAction(it) }
            }

        assertTrue(Intent.ACTION_USER_PRESENT in actions)
        assertTrue(Intent.ACTION_POWER_DISCONNECTED in actions)
    }
}
