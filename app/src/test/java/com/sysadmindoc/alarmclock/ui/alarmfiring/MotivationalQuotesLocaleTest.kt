package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The firing-screen quotes only existed in English, so a Japanese phone with
 * quotes turned on showed English under a Japanese clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MotivationalQuotesLocaleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun quotes() = context.resources.getStringArray(R.array.motivational_quotes).toList()

    @Test
    fun `quotes read in English by default`() {
        val quotes = quotes()
        assertEquals(15, quotes.size)
        assertEquals("The secret of getting ahead is getting started.", quotes.first())
    }

    @Test
    @Config(qualifiers = "ja")
    fun `quotes follow a Japanese locale`() {
        val quotes = quotes()
        assertEquals(15, quotes.size)
        assertEquals("前に進む秘訣は、まず始めることです。", quotes.first())
        assertEquals(emptyList<String>(), quotes.filter { it.none { c -> c.code > 0x2E80 } })
    }
}
