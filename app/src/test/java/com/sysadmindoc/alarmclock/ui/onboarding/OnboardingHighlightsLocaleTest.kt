package com.sysadmindoc.alarmclock.ui.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The two highlight lines on each welcome page were plain Kotlin strings, so a
 * first run in Japanese showed English under every Japanese title.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class OnboardingHighlightsLocaleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun highlights() = onboardingPages.flatMap { page -> page.highlightRes.map(context::getString) }

    @Test
    fun `highlights read in English by default`() {
        val lines = highlights()
        assertEquals(8, lines.size)
        assertEquals("Exact alarms designed to survive Doze mode", lines.first())
        assertEquals("Data stays on your device unless you configure an integration", lines.last())
    }

    @Test
    @Config(qualifiers = "ja")
    fun `highlights follow a Japanese locale`() {
        val lines = highlights()
        assertEquals(8, lines.size)
        assertEquals("Dozeモード中でも鳴るように作られた正確なアラーム", lines.first())
        lines.forEach { line ->
            assertTrue(line, line.any { it.code > 0x2E80 })
        }
    }
}
