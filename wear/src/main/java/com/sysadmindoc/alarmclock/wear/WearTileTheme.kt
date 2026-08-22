package com.sysadmindoc.alarmclock.wear

import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.types.LayoutColor

/**
 * The phone app's palette, restated for the tile.
 *
 * The tile was drawing the stock ProtoLayout scheme, so it sat in the carousel
 * looking like a different product from the app that feeds it. The `:wear`
 * module cannot see `:app`'s Compose colour tokens, so the values below are
 * copied from `app/ui/theme/Color.kt` and have to be kept in step by hand.
 * WearTileThemeTest pins them so a silent drift shows up as a failure rather
 * than as a tile that quietly stops matching.
 */
internal object WearTileTheme {

    // app/ui/theme/Color.kt, surface ladder.
    const val SURFACE_DARK: Int = 0xFF070B11.toInt()
    const val SURFACE_MEDIUM: Int = 0xFF0F1721.toInt()
    const val SURFACE_CARD: Int = 0xFF15202E.toInt()
    const val SURFACE_LIGHT: Int = 0xFF1B2737.toInt()

    // Primary palette and text ladder.
    const val BLUE_PRIMARY: Int = 0xFF6FB7FF.toInt()
    const val TEXT_PRIMARY: Int = 0xFFF1F5FB.toInt()
    const val TEXT_SECONDARY: Int = 0xFFA9BED8.toInt()

    // Semantic accents. Snooze yellow is the tile's secondary because the
    // tile's own secondary action is snooze.
    const val SNOOZE_YELLOW: Int = 0xFFF5C96B.toInt()
    const val ACCENT_RED: Int = 0xFFFF7E7A.toInt()

    // BorderSubtle in the app is 12% white, which a tile cannot composite the
    // same way, so the outline uses the opaque toggle track instead.
    const val OUTLINE: Int = 0xFF22303F.toInt()

    fun colorScheme(): ColorScheme = ColorScheme(
        primary = LayoutColor(BLUE_PRIMARY),
        onPrimary = LayoutColor(SURFACE_DARK),
        primaryContainer = LayoutColor(SURFACE_LIGHT),
        onPrimaryContainer = LayoutColor(TEXT_PRIMARY),
        secondary = LayoutColor(SNOOZE_YELLOW),
        onSecondary = LayoutColor(SURFACE_DARK),
        secondaryContainer = LayoutColor(SURFACE_CARD),
        onSecondaryContainer = LayoutColor(TEXT_PRIMARY),
        surfaceContainerLow = LayoutColor(SURFACE_MEDIUM),
        surfaceContainer = LayoutColor(SURFACE_CARD),
        surfaceContainerHigh = LayoutColor(SURFACE_LIGHT),
        onSurface = LayoutColor(TEXT_PRIMARY),
        onSurfaceVariant = LayoutColor(TEXT_SECONDARY),
        outline = LayoutColor(OUTLINE),
        outlineVariant = LayoutColor(SURFACE_LIGHT),
        background = LayoutColor(SURFACE_DARK),
        onBackground = LayoutColor(TEXT_PRIMARY),
        error = LayoutColor(ACCENT_RED),
        onError = LayoutColor(SURFACE_DARK),
    )
}
