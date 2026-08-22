package com.sysadmindoc.alarmclock.wear

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The tile's palette is a hand-copy of `app/ui/theme/Color.kt`, because the
 * `:wear` module cannot see `:app`. Nothing stops the phone tokens moving
 * without the tile following, so these pin the copies: a failure here means
 * someone changed one side and this file is the other side.
 *
 * Robolectric because `ColorScheme`'s defaults initialise ProtoLayout's
 * `ColorTokens`, which reads `android.graphics.Color` and dies with
 * `ExceptionInInitializerError` on a bare JVM.
 */
@RunWith(RobolectricTestRunner::class)
class WearTileThemeTest {

    @Test
    fun `the scheme paints the app surface, not the stock ProtoLayout one`() {
        val scheme = WearTileTheme.colorScheme()

        assertEquals(0xFF070B11.toInt(), scheme.background.staticArgb)
        assertEquals(0xFF070B11.toInt(), scheme.onPrimary.staticArgb)
        assertEquals(0xFFF1F5FB.toInt(), scheme.onBackground.staticArgb)
        assertEquals(0xFF6FB7FF.toInt(), scheme.primary.staticArgb)
    }

    @Test
    fun `the surface ladder keeps the app's four steps in order`() {
        val scheme = WearTileTheme.colorScheme()

        assertEquals(0xFF0F1721.toInt(), scheme.surfaceContainerLow.staticArgb)
        assertEquals(0xFF15202E.toInt(), scheme.surfaceContainer.staticArgb)
        assertEquals(0xFF1B2737.toInt(), scheme.surfaceContainerHigh.staticArgb)
    }

    @Test
    fun `snooze yellow and the error red carry over as the accents`() {
        val scheme = WearTileTheme.colorScheme()

        assertEquals(0xFFF5C96B.toInt(), scheme.secondary.staticArgb)
        assertEquals(0xFFFF7E7A.toInt(), scheme.error.staticArgb)
    }

    @Test
    fun `every colour is opaque`() {
        // A tile has no window behind it to composite against, so a token
        // copied over with an alpha channel (BorderSubtle is 12% white in the
        // app) would render as a hole rather than as a subtle stroke.
        val scheme = WearTileTheme.colorScheme()
        listOf(
            "primary" to scheme.primary,
            "onPrimary" to scheme.onPrimary,
            "primaryContainer" to scheme.primaryContainer,
            "onPrimaryContainer" to scheme.onPrimaryContainer,
            "secondary" to scheme.secondary,
            "onSecondary" to scheme.onSecondary,
            "secondaryContainer" to scheme.secondaryContainer,
            "onSecondaryContainer" to scheme.onSecondaryContainer,
            "surfaceContainerLow" to scheme.surfaceContainerLow,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "onSurface" to scheme.onSurface,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "outline" to scheme.outline,
            "outlineVariant" to scheme.outlineVariant,
            "background" to scheme.background,
            "onBackground" to scheme.onBackground,
            "error" to scheme.error,
            "onError" to scheme.onError,
        ).forEach { (name, color) ->
            assertEquals(
                "$name must be fully opaque",
                0xFF,
                (color.staticArgb ushr 24) and 0xFF
            )
        }
    }
}
