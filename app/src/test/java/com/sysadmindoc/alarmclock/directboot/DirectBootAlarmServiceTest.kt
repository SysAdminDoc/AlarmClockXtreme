package com.sysadmindoc.alarmclock.directboot

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.service.AlarmService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// A plain Application: the ring before first unlock needs nothing the app's
// startup provides.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DirectBootAlarmServiceTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val controller = Robolectric.buildService(DirectBootAlarmService::class.java).create()

    @After
    fun tearDown() {
        controller.destroy()
        DirectBootAlarmCache.clear(context)
    }

    @Test
    fun `the ring before first unlock carries its full-screen intent and is colorized`() {
        // Colorized foreground-service posts stay out of Android 16's auto-grouping,
        // which marks grouped posts silent. This pins the flags, not SystemUI.
        val start = Intent(context, DirectBootAlarmService::class.java)
            .setAction(DirectBootAlarmService.ACTION_START)
            .putExtra(DirectBootAlarmReceiver.EXTRA_ALARM_ID, 5L)
            .putExtra(DirectBootAlarmReceiver.EXTRA_TRIGGER_TIME, 1_700_000_000_000L)
            .putExtra(DirectBootAlarmReceiver.EXTRA_TIME_LABEL, "7:00 AM")
            .putExtra(DirectBootAlarmReceiver.EXTRA_PLAY_DEFAULT_SOUND, false)
            .putExtra(DirectBootAlarmReceiver.EXTRA_VIBRATION_ENABLED, false)

        controller.get().onStartCommand(start, 0, 1)

        val notification = shadowOf(controller.get()).lastForegroundNotification
        assertNotNull(notification)
        assertNotNull(notification.fullScreenIntent)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_COLORIZED))
        assertEquals(AlarmService.ALARM_NOTIFICATION_COLOR, notification.color)
    }
}
