package com.sysadmindoc.alarmclock.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary palette ────────────────────────────────────────────────────────
// A cooler, more sophisticated blue. Sharp on AMOLED, crisp on IPS, never
// neon. Tightened against the surface ladder below so primary type sits
// confidently without glowing.
val BluePrimary = Color(0xFF6FB7FF)
val BlueLight = Color(0xFF9BD0FF)
val BlueDark = Color(0xFF1F4E80)

// ─── Surface ladder ─────────────────────────────────────────────────────────
// A deliberate four-step ladder (no more drift). Each step adds ~6 luminance
// over the previous so backgrounds, sheets, cards, and chips stack
// predictably even with translucent overlays.
val SurfaceDark = Color(0xFF070B11)    // App background — deepest
val SurfaceMedium = Color(0xFF0F1721)  // Sheets / sticky regions
val SurfaceCard = Color(0xFF15202E)    // Card surfaces
val SurfaceLight = Color(0xFF1B2737)   // Elevated chips / hover wash

// ─── Hero accents ───────────────────────────────────────────────────────────
// Subtler hero gradient. The previous mix had three transitions in one band
// which fought the radial accent — now it's a single deep wash that lets the
// primary radial breathe.
val HeaderTop = Color(0xFF1F5FA0)
val HeaderBottom = Color(0xFF080D14)

// ─── Text ───────────────────────────────────────────────────────────────────
// TextPrimary stays just shy of pure white to soften AMOLED bloom. Secondary
// nudges cooler so the muted line-height between title and body reads cleanly
// in the dark surfaces.
val TextPrimary = Color(0xFFF1F5FB)
val TextSecondary = Color(0xFFA9BED8)
// Muted helper/caption text. Brightened from the old 0xFF6A819F so it clears
// WCAG AA (>=4.5:1) for body copy on the elevated surfaces it actually renders
// on — card bodies (SurfaceCard) and chips/tiles/inputs (SurfaceLight), where
// the old value measured 4.11:1 and 3.77:1. Still a clear step below
// TextSecondary, preserving the title/body/caption hierarchy.
val TextMuted = Color(0xFF7E93AE)

// ─── Semantic accents ───────────────────────────────────────────────────────
val AccentBlue = BluePrimary
val AccentRed = Color(0xFFFF7E7A)
val SnoozeYellow = Color(0xFFF5C96B)
val DismissGreen = Color(0xFF63D7AE)

// ─── Toggle colors ──────────────────────────────────────────────────────────
val ToggleOn = AccentBlue
val ToggleOff = Color(0xFF44566D)
val ToggleTrackOff = Color(0xFF22303F)

// ─── Border / overlay tokens ────────────────────────────────────────────────
// Two stroke weights and one hover wash, used everywhere. Keeping them in
// one place stops cards, chips, and inputs from drifting apart over time.
val BorderSubtle = Color(0x1FFFFFFF)   // 12% white — default card stroke
val BorderStrong = Color(0x33FFFFFF)   // 20% white — focused / selected
val OverlayHover = Color(0x0FFFFFFF)   // 6% white — hover / pressed wash

// ─── Night Clock ────────────────────────────────────────────────────────────
// The bedside surface is its own palette: darker than the app's own ladder so
// it does not light a room, and deliberately low-contrast except for the one
// thing a person needs to read when they cannot find their way out of it.
val NightClockTop = Color(0xFF02060D)
val NightClockBottom = Color(0xFF000000)
// The wash behind the date pill and the exit hint. 4% white on near-black.
val NightClockPanel = Color(0x0AFFFFFF)
// The exit hint. TextMuted at 0.58 measured 2.8:1 on that panel, under the
// 3:1 WCAG asks of non-text and large text alike, which is a bad place to be
// for the only instruction on the screen. NightClockContrastTest pins it.
val NightClockHint = Color(0xB87E93AE)
