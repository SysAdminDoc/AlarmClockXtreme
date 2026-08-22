package com.sysadmindoc.alarmclock.ui.theme

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Long press anywhere to exit" is the only instruction on the Night Clock, and
 * it is the one thing someone needs to read when they cannot get out of a
 * screen that has deliberately dimmed everything else. It was TextMuted at 58%
 * over a 4% white wash on near-black, which measures 2.8:1: under the 3:1 floor
 * that WCAG 2.1 asks even of large text and non-text content.
 *
 * The Night Clock is meant to be dim, so this pins a floor rather than chasing
 * 4.5:1. If someone dims it further, they fail here rather than in the dark.
 */
class NightClockContrastTest {

    /** What the hint is actually painted on: the panel over the gradient's top. */
    private val hintBackground = NightClockPanel.compositeOver(NightClockTop)

    @Test
    fun `the exit hint clears the 3 to 1 floor`() {
        val ratio = contrastRatio(NightClockHint.compositeOver(hintBackground), hintBackground)

        assertTrue(
            "exit hint measured $ratio:1 against the night panel, floor is 3:1",
            ratio >= 3f
        )
    }

    @Test
    fun `the hint is still dimmer than the clock it sits under`() {
        // A fix that just made everything bright would defeat the screen.
        val hint = contrastRatio(NightClockHint.compositeOver(hintBackground), hintBackground)
        val clock = contrastRatio(TextPrimary.copy(alpha = 0.9f).compositeOver(NightClockTop), NightClockTop)

        assertTrue("hint $hint:1 should stay below the clock's $clock:1", hint < clock)
    }

    @Test
    fun `the gradient really is darker than the app's own background`() {
        // Night Clock has its own palette on purpose; if it ever inherits the
        // app surface ladder this stops being a bedside screen.
        assertTrue(NightClockTop.luminance() < SurfaceDark.luminance())
        assertTrue(NightClockBottom.luminance() <= NightClockTop.luminance())
    }
}
