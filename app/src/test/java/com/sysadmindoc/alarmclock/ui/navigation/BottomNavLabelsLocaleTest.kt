package com.sysadmindoc.alarmclock.ui.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The tab labels were plain Kotlin strings, so the bottom bar stayed English
 * after the rest of the app switched to Japanese.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BottomNavLabelsLocaleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun labels() = bottomNavItems.map { context.getString(it.labelRes) }

    @Test
    fun `tab labels read in English by default`() {
        assertEquals(listOf("Today", "Alarms", "Timer", "World", "News", "Settings"), labels())
    }

    @Test
    @Config(qualifiers = "ja")
    fun `tab labels follow a Japanese locale`() {
        assertEquals(listOf("今日", "アラーム", "タイマー", "世界時計", "ニュース", "設定"), labels())
    }
}
