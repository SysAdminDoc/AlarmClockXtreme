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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.theme.BorderStrong
import com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
import com.sysadmindoc.alarmclock.ui.theme.HeaderBottom
import com.sysadmindoc.alarmclock.ui.theme.HeaderTop
import com.sysadmindoc.alarmclock.ui.theme.LocalAppShapeTokens
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
val AppCardShape = RoundedCornerShape(8.dp)
val AppTileShape = RoundedCornerShape(8.dp)
val AppChipShape = RoundedCornerShape(8.dp)

// ─── Icon size scale ───────────────────────────────────────────────────────
// One scale, four steps. Replaces the 13 / 15 / 18 / 20 / 22 dp drift that
// crept across cards, chips, tiles, and metrics. Reach for the smallest
// step that still reads — Material's 22dp is the navigation default.
object AppIconSize {
    val xs: Dp = 14.dp   // chips, dense rows
    val sm: Dp = 18.dp   // inline labels, secondary actions
    val md: Dp = 22.dp   // primary actions, nav, card headers
    val lg: Dp = 32.dp   // hero illustration, empty-state icon
}

@Composable
fun AlarmClockHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    badge: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    /**
     * v1.9.0: when true, the hero renders with no background of its own.
     * Used by the Today tab so the dynamic time-of-day sky behind it shows
     * through. Other tabs keep the default blue header gradient.
     */
    transparent: Boolean = false,
) {
    // A premium hero header is a single confident gradient, not a stack of
    // washes. We keep one vertical fade (deep blue → app surface) plus a
    // single off-center accent radial that the primary color drives. The
    // earlier 4-stop gradient + nested overlay box created banding and
    // muddied the brand on AMOLED.
    Box(
        modifier = if (transparent) {
            modifier.fillMaxWidth()
        } else {
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HeaderTop.copy(alpha = 0.55f),
                            HeaderBottom
                        )
                    )
                )
        }
    ) {
        if (!transparent) {
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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                // v1.7.1: vertical 18dp → 12dp, gap 14dp → 10dp. The hero
                // takes ~70dp less now, so the first alarm card lands above
                // the fold on standard phones.
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val shapeTokens = LocalAppShapeTokens.current
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
        shape = shapeTokens.card,
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
    val shapeTokens = LocalAppShapeTokens.current
    Column(
        modifier = modifier
            .background(SurfaceLight, shapeTokens.tile)
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
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val shapeTokens = LocalAppShapeTokens.current
    val minHeight = if (onClick != null) 40.dp else 30.dp
    val horizontalPadding = if (onClick != null) 13.dp else 11.dp
    val chipContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = minHeight)
                .padding(horizontal = horizontalPadding, vertical = 6.dp),
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
    if (onClick != null) {
        Surface(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .semantics {
                    role = Role.Button
                    contentDescription = label
                },
            shape = shapeTokens.chip,
            color = color.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
            tonalElevation = 0.dp,
            onClick = onClick,
            content = chipContent
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shapeTokens.chip,
            color = color.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
            tonalElevation = 0.dp,
            content = chipContent
        )
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
    val shapeTokens = LocalAppShapeTokens.current
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
                .clip(shapeTokens.iconContainer)
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
fun AppFeedbackCard(
    message: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val shapeTokens = LocalAppShapeTokens.current
    AppSurfaceCard(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = listOfNotNull(title, message).joinToString(". ")
        },
        highlighted = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shapeTokens.iconContainer)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = message,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (onDismiss != null) {
                Surface(
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    shape = shapeTokens.chip,
                    color = SurfaceLight,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 0.dp,
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Dismiss",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppLoadingCard(
    modifier: Modifier = Modifier,
    height: Dp = 148.dp
) {
    val shapeTokens = LocalAppShapeTokens.current
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
                .clip(shapeTokens.chip)
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(40.dp)
                .alpha(alpha)
                .clip(shapeTokens.tile)
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .alpha(alpha)
                .clip(shapeTokens.chip)
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(11.dp)
                .alpha(alpha)
                .clip(shapeTokens.chip)
                .background(SurfaceLight)
        )
    }
}

/**
 * Filter chip that matches [AppStatusChip] geometry — same minHeight, same
 * shape, same accent treatment. Replaces raw Material `FilterChip` in
 * filter-row contexts so chip rows hold a single rhythm regardless of which
 * chip kind is rendered. Selected chips fill the accent at 16% with a
 * 32%-alpha border; unselected chips sit on `SurfaceLight` with the subtle
 * stroke. Tap-target is the full chip, not just the label.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val isSelected = selected
    val shapeTokens = LocalAppShapeTokens.current
    val accent = MaterialTheme.colorScheme.primary
    val containerColor = if (isSelected) accent.copy(alpha = 0.16f) else SurfaceLight
    val borderColor = if (isSelected) accent.copy(alpha = 0.32f) else BorderSubtle
    val labelColor = if (isSelected) accent else TextSecondary

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                this.selected = isSelected
                contentDescription = if (isSelected) "$label selected" else label
            },
        shape = shapeTokens.chip,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 38.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(AppIconSize.xs),
                )
            }
            Text(
                text = label,
                color = labelColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Reusable shimmering placeholder block. Use to compose skeleton rows for
 * lists (News, alarms-loading, etc.) so first-paint feels purposeful instead
 * of presenting a single spinner card. Width is a fraction of available
 * space so callers can vary widths between rows for a more natural rhythm.
 */
@Composable
fun AppSkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    cornerRadius: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton-block")
    val alpha by transition.animateFloat(
        initialValue = 0.36f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Spacer(
        modifier = modifier
            .height(height)
            .alpha(alpha)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceLight),
    )
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
    val shapeTokens = LocalAppShapeTokens.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = shapeTokens.bottomNav,
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
