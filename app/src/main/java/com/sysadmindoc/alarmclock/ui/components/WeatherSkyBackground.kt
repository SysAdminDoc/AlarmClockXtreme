package com.sysadmindoc.alarmclock.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cyclone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.theme.SkyGradient
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.LocalMotionEnabled
import com.sysadmindoc.alarmclock.ui.theme.TimeOfDaySky
import com.sysadmindoc.alarmclock.ui.theme.WeatherSkyOverrides
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

/**
 * Computes the live sky gradient by ticking once a minute and re-evaluating
 * the time-of-day fraction. Returns the *base* gradient; storm and tornado
 * overrides are applied separately so each layer can be animated independently.
 */
@Composable
fun rememberSkyGradient(
    sunrise: LocalTime?,
    sunset: LocalTime?,
    weatherCode: Int?,
    tornadoActive: Boolean,
): SkyGradient {
    // Defensive defaults for the polar / no-location case. 6 AM / 8 PM is
    // close enough to mid-latitude average that the keyframe table still
    // produces sensible colors.
    val effectiveSunrise = sunrise ?: LocalTime.of(6, 0)
    val effectiveSunset = sunset ?: LocalTime.of(20, 0)

    val nowMinute by produceState(initialValue = LocalTime.now()) {
        // Tick at the top of every minute. We don't need second-by-second
        // accuracy — the keyframes are minutes-scale.
        while (true) {
            val now = LocalTime.now()
            value = now
            val secondsToNextMinute = 60 - now.second
            delay(secondsToNextMinute * 1000L)
        }
    }

    return remember(nowMinute, effectiveSunrise, effectiveSunset, weatherCode, tornadoActive) {
        when {
            tornadoActive -> WeatherSkyOverrides.TORNADO_SKY
            WeatherSkyOverrides.isStorm(weatherCode) -> {
                val t = TimeOfDaySky.computeT(nowMinute, effectiveSunrise, effectiveSunset)
                if (TimeOfDaySky.isDeepNight(t)) WeatherSkyOverrides.STORM_NIGHT
                else WeatherSkyOverrides.STORM_DAY
            }

            else -> {
                val t = TimeOfDaySky.computeT(nowMinute, effectiveSunrise, effectiveSunset)
                TimeOfDaySky.gradientForT(t)
            }
        }
    }
}

/**
 * Whole-screen weather backdrop. Stacks four layers:
 *   1. The base sky gradient (time-of-day or weather override)
 *   2. A long fade band from the gradient bottom toward [SurfaceDark] so
 *      cards below the hero sit on app surface, not on a vivid sky color.
 *   3. Lightning flashes when the active weather is a thunderstorm (any t).
 *   4. A rotating tornado funnel + warning chip when [tornadoActive] is true.
 *
 * The actual screen content is rendered on top via [content], on a
 * transparent column — the cards still bring their own backgrounds.
 */
@Composable
fun WeatherSkyBackground(
    sunrise: LocalTime?,
    sunset: LocalTime?,
    weatherCode: Int?,
    tornadoActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sky = rememberSkyGradient(sunrise, sunset, weatherCode, tornadoActive)
    val isStorm = WeatherSkyOverrides.isStorm(weatherCode)

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: sky gradient. Three stops mapped vertically so the
        // top-of-screen reads as the zenith and the bottom as the horizon.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to sky.top,
                        0.45f to sky.mid,
                        1f to sky.bot,
                    ),
                ),
        )

        // Layer 2: long fade to SurfaceDark so the screen below the hero
        // returns to the app's neutral surface. Without this, alarm cards
        // and metric tiles sit on a vivid blue sky and lose contrast.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.85f to SurfaceDark.copy(alpha = 0.95f),
                        1f to SurfaceDark,
                    ),
                ),
        )

        // Layer 3: lightning. Gated behind storm conditions; the flash is
        // brighter at night because the dark sky benefits from it most.
        if ((isStorm || tornadoActive) && LocalMotionEnabled.current) {
            LightningOverlay(
                modifier = Modifier.fillMaxSize(),
                intensity = if (tornadoActive) 1.4f else 1f,
            )
        }

        // Layer 4: tornado warning visual. A rotating funnel silhouette in
        // the upper-third of the screen + an unmistakable banner.
        if (tornadoActive) {
            TornadoOverlay(modifier = Modifier.fillMaxSize())
        }

        // Layer 5: actual content.
        content()
    }
}

/**
 * Subtle lightning flash. We don't emit one-shot flashes via a
 * coroutine + animation choreography because that's surprisingly hard to
 * get to feel right under recomposition; instead we drive the whole effect
 * off a single infinite transition that reads "flash, settle, dark, dark,
 * dark, flash". Multiplicatively driven by [intensity] so tornado mode
 * pushes the flashes hotter.
 */
@Composable
private fun LightningOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(intensity) {
        // Random "every 4-9 seconds, sometimes a double-strike" rhythm.
        val rng = Random(System.currentTimeMillis())
        while (true) {
            val gap = rng.nextInt(4_000, 9_000).toLong()
            delay(gap)
            flash(intensity) { flashAlpha = it }
            // ~30% chance of an immediate aftershock.
            if (rng.nextFloat() < 0.3f) {
                delay(180L)
                flash(intensity * 0.7f) { flashAlpha = it }
            }
        }
    }

    Box(
        modifier = modifier
            .alpha(flashAlpha)
            .background(Color.White),
    )
}

/** One flash: ramp up fast, decay slow. Linear is fine — flashes are short. */
private suspend fun flash(intensity: Float, set: (Float) -> Unit) {
    val peak = (0.28f * intensity).coerceIn(0f, 0.55f)
    // 60ms ramp up
    val rampSteps = 4
    repeat(rampSteps) { i ->
        set(peak * (i + 1) / rampSteps)
        delay(15L)
    }
    // 220ms decay
    val decaySteps = 11
    repeat(decaySteps) { i ->
        set(peak * (1f - (i + 1) / decaySteps.toFloat()))
        delay(20L)
    }
    set(0f)
}

/**
 * Tornado warning visual. A slowly-rotating funnel cloud silhouette in the
 * upper-third of the screen plus a red TORNADO WARNING banner pinned to
 * the top. The funnel is intentionally stylized — a Canvas-drawn spiral —
 * because a photoreal tornado would clash with the rest of the UI.
 */
@Composable
private fun TornadoOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        FunnelCloud(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp)
                .size(240.dp)
                .alpha(0.32f)
                .align(Alignment.TopCenter),
        )

        // Top warning banner. Pinned beneath the status bar with a margin
        // that matches the hero's status-bar inset.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFB7271A),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Cyclone,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.weather_tornado_warning),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Stylized funnel cloud: a Canvas of nested concentric arcs that rotate at
 * different rates so the silhouette appears to swirl. Three opacity layers
 * stack over each other to suggest depth.
 */
@Composable
private fun FunnelCloud(modifier: Modifier = Modifier) {
    val rotation: Float
    val drift: Float
    if (LocalMotionEnabled.current) {
        val transition = rememberInfiniteTransition(label = "funnel")
        rotation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "funnel-rotation",
        ).value
        drift = transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4_500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "funnel-drift",
        ).value
    } else {
        rotation = 0f
        drift = 0f
    }

    Canvas(modifier = modifier.rotate(rotation)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        // Funnel: wide at top (cloud) tapering to point at bottom (touchdown).
        val path = Path().apply {
            moveTo(cx - w * 0.45f, 0f)
            quadraticTo(cx + drift * 30f, h * 0.35f, cx - w * 0.04f, h)
            lineTo(cx + w * 0.04f, h)
            quadraticTo(cx + drift * 30f, h * 0.35f, cx + w * 0.45f, 0f)
            close()
        }
        drawPath(path = path, color = Color(0xFF1F1408))
        // Inner stroke band suggesting rotation.
        for (i in 0..6) {
            val frac = i / 6f
            val y = h * frac
            val width = w * (0.45f - 0.40f * frac)
            drawLine(
                color = Color(0xFF3A2912).copy(alpha = 0.35f + sin(frac * 6f + drift) * 0.1f),
                start = Offset(cx - width, y),
                end = Offset(cx + width, y),
                strokeWidth = 4f,
            )
        }
    }
}
