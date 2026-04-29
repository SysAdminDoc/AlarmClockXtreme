package com.sysadmindoc.alarmclock.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.theme.BorderStrong
import com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
import com.sysadmindoc.alarmclock.ui.theme.HeaderBottom
import com.sysadmindoc.alarmclock.ui.theme.HeaderTop
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.ui.theme.ToggleOff
import com.sysadmindoc.alarmclock.ui.theme.ToggleTrackOff

// ─── Shared shape tokens ───────────────────────────────────────────────────
val AppCardShape = RoundedCornerShape(20.dp)
val AppTileShape = RoundedCornerShape(16.dp)
val AppChipShape = RoundedCornerShape(999.dp)

@Composable
fun AlarmClockHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    badge: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    // A premium hero header is a single confident gradient, not a stack of
    // washes. We keep one vertical fade (deep blue → app surface) plus a
    // single off-center accent radial that the primary color drives. The
    // earlier 4-stop gradient + nested overlay box created banding and
    // muddied the brand on AMOLED.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HeaderTop.copy(alpha = 0.55f),
                        HeaderBottom
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        radius = 720f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!overline.isNullOrBlank()) {
                    Text(
                        text = overline.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (badge != null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = badge
                )
            }

            if (content != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    // One container, one stroke, one soft sheen. The previous version stacked
    // a vertical white wash AND a radial accent on top of the base color,
    // which made cards feel busy and inconsistent on dark surfaces.
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        SurfaceCard
    }
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    } else {
        BorderSubtle
    }

    Card(
        modifier = modifier.animateContentSize(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ),
        shape = AppCardShape,
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (highlighted) 3.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (highlighted) 0.04f else 0.02f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/**
 * Compact metric tile — replaces the ad-hoc Surface treatments scattered
 * across Dashboard / Stats / Bedtime so every "small data card" looks the
 * same. Optional leading icon plus a value/label pair.
 */
@Composable
fun AppMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = TextMuted
) {
    Column(
        modifier = modifier
            .background(SurfaceLight, AppTileShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = value.ifBlank { "—" },
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AppSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (action != null) 10.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (action != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = action
                )
            }
        }
    }
}

@Composable
fun AppStatusChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // Slightly tighter, slightly more confident: subtler fill, color-matched
    // border (was hard-coded primary alpha), and SemiBold label so chips read
    // as deliberate metadata rather than free-floating UI clutter.
    Surface(
        modifier = modifier,
        shape = AppChipShape,
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 30.dp)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.20f),
                            accent.copy(alpha = 0.04f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        if (footer != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = footer
            )
        }
    }
}

@Composable
fun AppLoadingCard(
    modifier: Modifier = Modifier,
    height: Dp = 148.dp
) {
    val transition = rememberInfiniteTransition(label = "loading-card")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading-alpha"
    )

    AppSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.32f)
                .height(16.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(40.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(11.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLight)
        )
    }
}

@Composable
fun appOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = BorderSubtle,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = TextMuted,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = TextMuted,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = TextMuted,
    focusedSupportingTextColor = TextSecondary,
    unfocusedSupportingTextColor = TextMuted,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorCursorColor = MaterialTheme.colorScheme.error,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    errorTrailingIconColor = MaterialTheme.colorScheme.error,
    focusedContainerColor = SurfaceLight,
    unfocusedContainerColor = SurfaceLight.copy(alpha = 0.8f),
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted
)

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = TextPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = ToggleOff,
    uncheckedTrackColor = ToggleTrackOff,
    uncheckedBorderColor = BorderSubtle
)

/**
 * Floating bottom-nav container. Restrained: a single surface, a single
 * stroke, a faint inner sheen at the very top to suggest depth without the
 * old radial+gradient sandwich.
 */
@Composable
fun BottomNavContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceMedium,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

// Backwards-compat: legacy constant name still referenced from a few screens.
val AppCardBorderColor: Color = BorderSubtle
