package com.sysadmindoc.alarmclock.worker

import android.app.Application
import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class HueSunriseNotificationsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `pre Android 16 sunrise notification is an ongoing countdown`() {
        val notification = HueSunriseNotifications.buildCompat(
            context = context,
            alarmId = 42L,
            endWallClockMillis = 1_500_000L
        )

        assertEquals(1_500_000L, notification.`when`)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(Notification.CATEGORY_PROGRESS, notification.category)
    }

    @Test
    fun `alarm id maps to a stable bounded notification id`() {
        assertEquals(
            HueSunriseNotifications.notificationId(42L),
            HueSunriseNotifications.notificationId(100_042L)
        )
        assertTrue(HueSunriseNotifications.notificationId(42L) >= 800_000)
    }
}
