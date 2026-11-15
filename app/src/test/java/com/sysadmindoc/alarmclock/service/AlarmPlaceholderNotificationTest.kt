package com.sysadmindoc.alarmclock.service

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.app.Notification
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager

/**
 * The service's first post for a ring is the placeholder. SystemUI decides
 * on a full-screen launch only when a notification is added, not when it's
 * updated, so the placeholder has to carry the ringing screen itself.
 *
 * AlarmService is a Hilt service and the unit tests have no Hilt harness, so
 * this pins the builder startForegroundWithPlaceholder() posts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AlarmPlaceholderNotificationTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `the first post for a ring carries the ringing screen as its full-screen intent`() {
        val firing = AlarmFireDismissContract.firingPendingIntent(context, ALARM_ID, SCHEDULED_AT, FIRE_ID)

        val placeholder = AlarmService.placeholderNotification(context, firing, appOpen = false)

        assertSame(firing, placeholder.fullScreenIntent)
        assertSame(firing, placeholder.contentIntent)
        assertEquals(AlarmService.CHANNEL_ALARM, placeholder.channelId)
        assertEquals(Notification.CATEGORY_ALARM, placeholder.category)
        assertEquals(Notification.VISIBILITY_PUBLIC, placeholder.visibility)
        assertTrue(placeholder.flags and Notification.FLAG_ONGOING_EVENT != 0)

        val opened = shadowOf(firing).savedIntent
        assertEquals(AlarmFiringActivity::class.java.name, opened.component?.className)
        assertEquals(ALARM_ID, opened.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L))
        assertEquals(SCHEDULED_AT, opened.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L))
        assertEquals(FIRE_ID, opened.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID))
    }

    @Test
    fun `the first post asks to be colorized`() {
        // Android 16 r1 marks every auto-grouped notification FLAG_SILENT and
        // SystemUI then drops its full-screen intent. A colorized foreground
        // service notification is never auto-grouped. The framework adds the
        // foreground flag at startForeground(), the app supplies the request.
        val firing = AlarmFireDismissContract.firingPendingIntent(context, ALARM_ID, SCHEDULED_AT, FIRE_ID)

        val placeholder = AlarmService.placeholderNotification(context, firing, appOpen = false)

        assertTrue(placeholder.extras.getBoolean(Notification.EXTRA_COLORIZED))
        assertEquals(AlarmService.ALARM_NOTIFICATION_COLOR, placeholder.color)
    }

    @Test
    fun `with the app open the first post leaves the full-screen intent off`() {
        // A heads-up card with one stays over the ringing screen until it's
        // swiped away, and the service opens that screen itself.
        val firing = AlarmFireDismissContract.firingPendingIntent(context, ALARM_ID, SCHEDULED_AT, FIRE_ID)

        val placeholder = AlarmService.placeholderNotification(context, firing, appOpen = true)

        assertNull(placeholder.fullScreenIntent)
        assertSame(firing, placeholder.contentIntent)
    }

    @Test
    fun `the labelled post follows the same rule`() {
        val firing = AlarmFireDismissContract.firingPendingIntent(context, ALARM_ID, SCHEDULED_AT, FIRE_ID)
        fun post(appOpen: Boolean) = with(AlarmService) {
            NotificationCompat.Builder(context, AlarmService.CHANNEL_ALARM)
                .setSmallIcon(R.drawable.ic_alarm)
                .setRingingScreen(firing, appOpen)
                .build()
        }

        assertSame(firing, post(appOpen = false).fullScreenIntent)
        assertNull(post(appOpen = true).fullScreenIntent)
        assertSame(firing, post(appOpen = true).contentIntent)
    }

    @Test
    fun `the app counts as open only while one of its screens is in front`() {
        assertTrue(appOnScreenAt(RunningAppProcessInfo.IMPORTANCE_FOREGROUND))
        // The ringing service alone, and the app in front of a dark or locked screen.
        assertFalse(appOnScreenAt(RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE))
        assertFalse(appOnScreenAt(RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING))
        assertFalse(appOnScreenAt(RunningAppProcessInfo.IMPORTANCE_CACHED))
    }

    @After
    fun restoreProcesses() {
        ShadowActivityManager.reset()
    }

    private fun appOnScreenAt(importance: Int): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        shadowOf(activityManager).setProcesses(
            listOf(
                RunningAppProcessInfo().apply {
                    pid = Process.myPid()
                    processName = context.packageName
                    this.importance = importance
                }
            )
        )
        return AlarmService.appOnScreen()
    }

    private companion object {
        const val ALARM_ID = 42L
        const val SCHEDULED_AT = 1_700_000_000_000L
        const val FIRE_ID = "42-1700000000000"
    }
}
