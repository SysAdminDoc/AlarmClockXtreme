package com.sysadmindoc.alarmclock.service

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.local.AlarmDatabase
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class NextAlarmNotifierLeadTest {

    private lateinit var context: Context
    private lateinit var database: AlarmDatabase
    private lateinit var preferences: PreferencesManager
    private lateinit var notifier: NextAlarmNotifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = PreferencesManager(context)
        notifier = NextAlarmNotifier(
            context,
            AlarmRepository(database.alarmDao()),
            NextAlarmCalculator(AppSettings()),
            preferences
        )
    }

    @After
    fun tearDown() {
        notifier.stopObserving()
        runBlocking { preferences.update { it.copy(nextAlarmNotificationLeadMinutes = 0) } }
        database.close()
    }

    private fun showWakeups() =
        shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.filter { alarm ->
            alarm.operation?.let { shadowOf(it).savedIntent.component?.className } ==
                NextAlarmNotificationReceiver::class.java.name
        }

    private fun nextAlarmNotificationPosted() =
        shadowOf(context.getSystemService(NotificationManager::class.java))
            .getNotification(NextAlarmNotifier.NOTIFICATION_ID_PERSISTENT) != null

    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail("timed out waiting for $what")
            Thread.sleep(20)
        }
    }

    @Test
    fun `going back to Always drops the pending show wakeup`() {
        val trigger = System.currentTimeMillis() + 2 * 60 * 60_000L
        runBlocking {
            database.alarmDao().insert(Alarm(hour = 6, minute = 0, isEnabled = true, nextTriggerTime = trigger))
            preferences.update { it.copy(nextAlarmNotificationLeadMinutes = 10) }
        }

        notifier.startObserving()
        waitFor("the 10-minute show wakeup") { showWakeups().isNotEmpty() }
        assertEquals(trigger - 10 * 60_000L, showWakeups().single().triggerAtTime)
        assertTrue(!nextAlarmNotificationPosted())

        runBlocking { preferences.update { it.copy(nextAlarmNotificationLeadMinutes = 0) } }
        waitFor("the next-alarm notification") { nextAlarmNotificationPosted() }
        waitFor("the stale show wakeup to go") { showWakeups().isEmpty() }
    }
}
