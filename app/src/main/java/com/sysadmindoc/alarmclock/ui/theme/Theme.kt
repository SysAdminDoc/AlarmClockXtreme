package com.sysadmindoc.alarmclock.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAccentColor = compositionLocalOf { AccentBlue }

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = TextPrimary,
    primaryContainer = BlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = BlueLight,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceLight,
    onSecondaryContainer = TextPrimary,
    tertiary = SnoozeYellow,
    onTertiary = SurfaceDark,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceMedium,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = TextPrimary,
    outline = TextMuted,
    outlineVariant = SurfaceLight,
    surfaceTint = BluePrimary,
)

@Composable
fun AlarmClockXtremeTheme(
    accentColorHex: String? = null,
    content: @Composable () -> Unit
) {
    val accent = if (accentColorHex != null && accentColorHex.startsWith("#")) {
        try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (_: Exception) { AccentBlue }
    } else AccentBlue

    val colorScheme = DarkColorScheme.copy(
        primary = accent,
        secondary = accent,
        surfaceTint = accent
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = SurfaceDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalAccentColor provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
