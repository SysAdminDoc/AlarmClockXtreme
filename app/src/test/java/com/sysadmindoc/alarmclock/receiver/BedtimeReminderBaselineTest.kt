package com.sysadmindoc.alarmclock.receiver

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.BedtimeNoiseBaselineSampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The bedtime reminder used to take its own microphone reading. A broadcast
 * receiver runs in the background, and from API 30 a background app is handed
 * silence by AudioRecord, so every reminder stored "the room is quiet" with a
 * fresh timestamp and buried whatever the Bedtime screen had measured.
 *
 * These assert on the notification text rather than on the stored file,
 * because the file cannot tell the two behaviours apart here: Robolectric's
 * AudioRecord returns no samples, so the old code's sample simply failed and
 * wrote nothing. What it did do was hand a null baseline to the message
 * chooser, which is visible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BedtimeReminderBaselineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("bedtime_noise_baseline", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun fireReminder(): Notification? {
        BedtimeReceiver().onReceive(context, Intent(BedtimeReceiver.ACTION_BEDTIME_REMINDER))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        return manager.allNotifications.lastOrNull()
    }

    @Test
    fun `the reminder repeats the last in-app measurement instead of taking one`() {
        // Written straight to the store rather than through the sampler: the
        // sampler is exactly the path this test is asserting nobody takes.
        context.getSharedPreferences("bedtime_noise_baseline", Context.MODE_PRIVATE)
            .edit()
            .putString("level", "LOUD")
            .putFloat("dbfs", -20f)
            .putLong("measured_at", 1_787_600_000_000L)
            .commit()

        val notification = fireReminder()

        assertNotNull(notification)
        assertEquals(
            context.getString(R.string.bedtime_room_loud_advice),
            notification!!.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        // Sampling from the background would have replaced the measurement.
        assertEquals(
            -20f,
            BedtimeNoiseBaselineSampler.readSnapshot(context).baseline?.dbfs
        )
    }

    @Test
    fun `with nothing measured yet the reminder says the plain wind-down line`() {
        val notification = fireReminder()

        assertNotNull(notification)
        assertEquals(
            context.getString(R.string.bedtime_reminder_default),
            notification!!.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(0L, BedtimeNoiseBaselineSampler.readSnapshot(context).measuredAtMillis)
    }
}
