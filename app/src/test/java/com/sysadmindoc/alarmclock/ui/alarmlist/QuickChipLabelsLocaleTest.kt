package com.sysadmindoc.alarmclock.ui.alarmlist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.ui.timer.defaultPresets
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The quick-alarm chips ("10 min", "1 hour"), the timer presets and the alarm
 * card's options button were literals, so they stayed English in Japanese.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class QuickChipLabelsLocaleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun quickAlarmLabels() = listOf(10, 30, 60, 120).map { minutes ->
        if (minutes < 60) {
            context.getString(R.string.alarmlist_min, minutes)
        } else {
            context.resources.getQuantityString(R.plurals.alarmlist_hours, minutes / 60, minutes / 60)
        }
    }

    private fun timerPresetLabels() =
        defaultPresets.map { context.getString(R.string.timer_preset_minutes, (it.seconds / 60).toInt()) }

    @Test
    fun `chips read in English by default`() {
        assertEquals(listOf("10 min", "30 min", "1 hour", "2 hours"), quickAlarmLabels())
        assertEquals(listOf("1 min", "3 min", "5 min", "10 min", "15 min", "30 min"), timerPresetLabels())
        assertEquals("Alarm options", context.getString(R.string.alarmlist_alarm_options))
    }

    @Test
    @Config(qualifiers = "ja")
    fun `chips follow a Japanese locale`() {
        assertEquals(listOf("10 分", "30 分", "1 時間", "2 時間"), quickAlarmLabels())
        assertEquals(listOf("1 分", "3 分", "5 分", "10 分", "15 分", "30 分"), timerPresetLabels())
        assertEquals("アラームのオプション", context.getString(R.string.alarmlist_alarm_options))
    }
}
