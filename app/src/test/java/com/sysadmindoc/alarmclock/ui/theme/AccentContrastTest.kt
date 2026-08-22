package com.sysadmindoc.alarmclock.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The accent presets offered in Settings > Personalization, kept in the same
 * order as `AccentColorPicker`. Any preset added there has to clear the same
 * bars, so the picker and this list are meant to stay in step.
 */
private val accentPresets = listOf(
    "blue" to Color(0xFF5B9EF4),
    "violet" to Color(0xFF8F73FF),
    "coral" to Color(0xFFFF6F8A),
    "amber" to Color(0xFFFFB347),
    "mint" to Color(0xFF5BD49A),
    "mono" to Color(0xFFE0E4EA),
    "default" to AccentBlue
)

class AccentContrastTest {
    @Test
    fun `every accent preset gets a readable button label`() {
        accentPresets.forEach { (name, accent) ->
            val ratio = contrastRatio(accentForeground(accent), accent)
            assertTrue(
                "Accent $name gives ${"%.2f".format(ratio)}:1 on a filled button, below WCAG AA",
                ratio >= 4.5f
            )
        }
    }

    @Test
    fun `every accent preset is readable as text on a card`() {
        accentPresets.forEach { (name, accent) ->
            val ratio = contrastRatio(accent, SurfaceCard)
            assertTrue(
                "Accent $name measures ${"%.2f".format(ratio)}:1 as label text on SurfaceCard",
                ratio >= 4.5f
            )
        }
    }

    @Test
    fun `contrast ratio is symmetric and bounded`() {
        assertTrue(contrastRatio(Color.White, Color.Black) > 20f)
        assertTrue(contrastRatio(Color.Black, Color.White) > 20f)
        assertTrue(contrastRatio(SurfaceCard, SurfaceCard) in 0.99f..1.01f)
    }

    @Test
    fun `expressive mode accents keep their foregrounds readable`() {
        listOf("snooze" to SnoozeYellow, "dismiss" to DismissGreen).forEach { (name, color) ->
            val ratio = contrastRatio(accentForeground(color), color)
            assertTrue(
                "$name gives ${"%.2f".format(ratio)}:1",
                ratio >= 4.5f
            )
        }
    }
}
