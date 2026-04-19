package com.sysadmindoc.alarmclock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalAccentColor = compositionLocalOf { AccentBlue }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val parsedAccent = if (accentColorHex != null && accentColorHex.startsWith("#")) {
        try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (_: Exception) { AccentBlue }
    } else AccentBlue

    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = if (supportsDynamic) {
        // Material You: derive the palette from the user's wallpaper. Keep the
        // app's deep-dark surfaces so the identity of the dark theme isn't lost.
        dynamicDarkColorScheme(context).copy(
            background = SurfaceDark,
            surface = SurfaceMedium,
            surfaceVariant = SurfaceCard
        )
    } else {
        DarkColorScheme.copy(
            primary = parsedAccent,
            secondary = parsedAccent,
            surfaceTint = parsedAccent
        )
    }
    val accent = if (supportsDynamic) colorScheme.primary else parsedAccent

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // `view.context as Activity` would crash in any non-Activity host
            // (preview, ContextWrapper from a service, etc.). Bail safely.
            val window = (view.context as? Activity)?.window ?: return@SideEffect

            // v1.5.2: The direct `statusBarColor` / `navigationBarColor`
            // setters were deprecated in Android 15 (API 35) because edge-
            // to-edge is now enforced system-wide and the host activities
            // already call `enableEdgeToEdge()`. Keep the setters for
            // older OS versions so nav-bar contrast stays correct below
            // the transparent region, but suppress the deprecation noise
            // and skip the call on API 35+ where it is a no-op.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = SurfaceDark.toArgb()
            }

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
            shapes = AppShapes,
            content = content
        )
    }
}
