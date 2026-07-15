package com.sysadmindoc.alarmclock.ui.timer

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TimerAlarmServiceTest {
    private lateinit var context: Context
    private lateinit var controller: ServiceController<TimerAlarmService>
    private lateinit var service: TimerAlarmService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("timer_state", Context.MODE_PRIVATE).edit().clear().commit()
        controller = Robolectric.buildService(TimerAlarmService::class.java).create()
        service = controller.get()
    }

    @After
    fun tearDown() {
        controller.destroy()
        TimerStore(context).replace(emptyList())
    }

    @Test
    fun `simultaneous and duplicate expiries share one foreground alert`() {
        service.onStartCommand(fireIntent(1, "Tea"), 0, 1)
        service.onStartCommand(fireIntent(1, "Tea"), 0, 2)
        service.onStartCommand(fireIntent(2, "Rice"), 0, 3)

        val notification = shadowOf(service).lastForegroundNotification

        assertNotNull(notification)
        assertEquals(
            "2 timers finished",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(TimerAlarmService.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
    }

    @Test
    fun `foreground alert keeps private label but publishes generic lock screen content`() {
        service.onStartCommand(fireIntent(4, "Medication"), 0, 1)

        val notification = service.buildNotification(hidePublicLabel = true)

        assertEquals(
            "Medication",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion)
        assertEquals(
            "Timer",
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.publicVersion.visibility)
    }

    @Test
    fun `passive finished notification uses the same public label policy`() {
        val notification = TimerNotifications.buildFinishedNotification(
            context = context,
            timerId = 5,
            label = "Laundry",
            hidePublicLabel = true
        )

        assertEquals(
            "Laundry",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion)
        assertEquals(
            "Timer",
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.publicVersion.visibility)
    }

    @Test
    fun `labels remain public when privacy control is disabled`() {
        val notification = TimerNotifications.buildFinishedNotification(
            context = context,
            timerId = 6,
            label = "Laundry",
            hidePublicLabel = false
        )

        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertEquals(
            "Laundry",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
    }

    @Test
    fun `public label policy fails closed before preferences load`() {
        assertTrue(PreferencesManager(context).shouldHideLabelsOnPublicSurfaces())
    }

    @Test
    fun `auto stop preserves finished state and leaves a passive notification`() {
        TimerStore(context).upsert(
            PersistedTimerRecord(3, "Pasta", 60, 0, TimerState.FINISHED)
        )
        service.onStartCommand(fireIntent(3, "Pasta"), 0, 1)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(3))

        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(TimerState.FINISHED, TimerStore(context).loadRecords().single().state)
        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNotNull(notifications.getNotification(7_003))
    }

    private fun fireIntent(id: Int, label: String): Intent =
        Intent(context, TimerAlarmService::class.java)
            .setAction(TimerAlarmService.ACTION_FIRED)
            .putExtra(TimerAlarmService.EXTRA_TIMER_ID, id)
            .putExtra(TimerAlarmService.EXTRA_LABEL, label)
}
