package com.sysadmindoc.alarmclock.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The "rings in" text on the notification, widget, alarm list and editor
 * takes its units from string resources, so a translation changes them too
 * (#54). The arithmetic is covered by [NextAlarmCalculatorTest].
 */
@RunWith(RobolectricTestRunner::class)
class NextAlarmCalculatorLocaleTest {

    private val calculator = NextAlarmCalculator()
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun unitsComeFromTheAppsStrings() {
        val inTwoDaysFiveMinutes = System.currentTimeMillis() + 2 * DAY + 5 * MINUTE + 30_000L
        assertEquals("2d 5m", calculator.formatRemaining(context, inTwoDaysFiveMinutes))
    }

    @Test
    fun pastAndSubMinuteUseTheirOwnStrings() {
        assertEquals("now", calculator.formatRemaining(context, System.currentTimeMillis() - MINUTE))
        assertEquals("<1m", calculator.formatRemaining(context, System.currentTimeMillis() + 10_000L))
    }

    private companion object {
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE
    }
}
