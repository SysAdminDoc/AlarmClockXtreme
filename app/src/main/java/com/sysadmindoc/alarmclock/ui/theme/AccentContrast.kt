package com.sysadmindoc.alarmclock.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Contrast helpers for the user-chosen accent colour.
 *
 * The accent replaces `primary` wholesale, so anything painted on top of a
 * filled accent surface (button labels, checked switch thumbs, swatch
 * checkmarks) has to adapt with it. The palette runs from a mid blue to a
 * near-white mono, and the fixed near-white foreground the theme used to hand
 * out measured as low as 1.2:1 on the lighter presets, which made the label
 * invisible.
 */

/** WCAG 2.1 contrast ratio between two opaque colours. */
internal fun contrastRatio(foreground: Color, background: Color): Float {
    val a = foreground.luminance() + 0.05f
    val b = background.luminance() + 0.05f
    return if (a > b) a / b else b / a
}

/**
 * The readable foreground for text or an icon drawn on a filled [accent]
 * surface: whichever of the app's two extremes measures higher against it.
 */
internal fun accentForeground(accent: Color): Color =
    if (contrastRatio(SurfaceDark, accent) >= contrastRatio(TextPrimary, accent)) {
        SurfaceDark
    } else {
        TextPrimary
    }
