package com.sysadmindoc.alarmclock.ui.timer

import android.app.AlarmManager
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
import org.junit.Assert.assertNull
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
        assertEquals("Restart", notification.actions.single().title.toString())
        assertEquals("Restart", notification.publicVersion.actions.single().title.toString())
        val restartIntent = shadowOf(notification.actions.single().actionIntent).savedIntent
        assertEquals(TimerAlarmService.ACTION_RESTART, restartIntent.action)
        assertEquals(5, restartIntent.getIntExtra(TimerAlarmService.EXTRA_TIMER_ID, -1))
    }

    @Test
    fun `single ringing timer notification offers restart`() {
        service.onStartCommand(fireIntent(4, "Tea"), 0, 1)

        val notification = service.buildNotification(hidePublicLabel = false)

        assertEquals(listOf("Stop", "Restart"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `restart action creates and schedules exactly one fresh timer without ui`() {
        TimerStore(context).upsert(
            PersistedTimerRecord(3, "Pasta", 60, 0, TimerState.FINISHED)
        )
        service.onStartCommand(fireIntent(3, "Pasta"), 0, 1)

        service.onStartCommand(restartIntent(3), 0, 2)
        service.onStartCommand(restartIntent(3), 0, 3)

        val records = TimerStore(context).loadRecords()
        val running = records.single()
        assertEquals(4, running.id)
        assertEquals("Pasta", running.label)
        assertEquals(60L, running.totalSeconds)
        assertEquals(TimerState.RUNNING, running.state)
        assertEquals(
            1,
            shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size
        )
        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNull(notifications.getNotification(TimerNotifications.notificationId(3)))
        assertNotNull(notifications.getNotification(TimerNotifications.notificationId(4)))
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
    fun `running timer fallback is ongoing private and counts down to elapsed deadline`() {
        val notification = TimerNotifications.buildRunningNotification(
            context = context,
            timer = PersistedTimerRecord(
                id = 8,
                label = "Medication",
                totalSeconds = 600,
                remainingMillis = 300_000,
                state = TimerState.RUNNING,
                endElapsedRealtime = 500_000L
            ),
            hidePublicLabel = true,
            nowElapsedRealtime = 200_000L,
            nowWallClockMillis = 1_000_000L
        )

        assertEquals(1_300_000L, notification.`when`)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals("Medication", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(
            "Timer",
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        )
    }

    @Test
    fun `posting and canceling a running timer owns its stable notification id`() {
        val timer = PersistedTimerRecord(
            id = 9,
            label = "Tea",
            totalSeconds = 60,
            remainingMillis = 60_000,
            state = TimerState.RUNNING,
            endElapsedRealtime = 260_000L
        )
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))

        TimerNotifications.postRunning(
            context = context,
            timer = timer,
            hidePublicLabel = false,
            nowElapsedRealtime = 200_000L,
            nowWallClockMillis = 1_000_000L
        )

        assertNotNull(manager.getNotification(TimerNotifications.notificationId(timer.id)))
        TimerNotifications.cancelTimer(context, timer.id)
        assertNull(manager.getNotification(TimerNotifications.notificationId(timer.id)))
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

    private fun restartIntent(id: Int): Intent =
        Intent(context, TimerAlarmService::class.java)
            .setAction(TimerAlarmService.ACTION_RESTART)
            .putExtra(TimerAlarmService.EXTRA_TIMER_ID, id)
}
