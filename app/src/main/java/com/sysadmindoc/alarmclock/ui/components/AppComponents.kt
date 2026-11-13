@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.offset
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
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
import com.sysadmindoc.alarmclock.ui.theme.LocalMotionEnabled
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.ui.theme.ToggleOff
import com.sysadmindoc.alarmclock.ui.theme.ToggleTrackOff
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

// ─── Shared shape tokens ───────────────────────────────────────────────────
val AppCardShape = RoundedCornerShape(12.dp)
val AppTileShape = RoundedCornerShape(10.dp)
val AppChipShape = RoundedCornerShape(8.dp)
val AppInputShape = RoundedCornerShape(8.dp)
val AppButtonShape = RoundedCornerShape(10.dp)

/**
 * Widens a child by [bleed] on both sides, past its parent's padding. Settings
 * rows use it so the press highlight reaches the card edge, and scrolling chip
 * rows use it so chips slide off the screen edge instead of being cut at the
 * gutter (pad the scrolling content by the same amount to keep it aligned).
 */
fun Modifier.horizontalBleed(bleed: Dp): Modifier =
    if (bleed == 0.dp) this else layout { measurable, constraints ->
        val extra = bleed.roundToPx()
        val placeable = measurable.measure(constraints.offset(horizontal = extra * 2))
        val width = (placeable.width - extra * 2).coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, placeable.height) { placeable.place(-extra, 0) }
    }

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
    /**
     * Pushed pages (Settings categories, Stopwatch, Bedtime, Statistics) get a
     * leading back arrow here, so every page you can leave is left the same way.
     */
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    /**
     * 2 for a subtitle that changes with state (Stopwatch), so a one-line
     * state doesn't pull the page up under the finger that just tapped.
     */
    subtitleMinLines: Int = 1,
) {
    Box(
        modifier = if (transparent) {
            modifier.fillMaxWidth()
        } else {
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceMedium,
                            HeaderBottom
                        )
                    )
                )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backContentDescription
                                ?: stringResource(R.string.common_back),
                            tint = TextPrimary
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (onBack != null) Modifier.offset(x = (-12).dp) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!overline.isNullOrBlank()) {
                        Text(
                            text = overline,
                            color = TextMuted,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        // Two lines: one cut sentences off mid-word
                        // ("Start a precise running timer and mark la…").
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            minLines = subtitleMinLines,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (actions != null) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }

            if (badge != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
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
    // Pass the click here rather than as a clickable modifier: the modifier
    // sits outside the card's clip, so its ripple drew square corners.
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shapeTokens = LocalAppShapeTokens.current
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        SurfaceCard
    }
    val cardModifier = modifier.animateContentSize(
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
    )
    val body: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shapeTokens.card,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = body
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shapeTokens.card,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = body
        )
    }
}

/**
 * Equal-width tiles, [columns] to a row, every tile in a row as tall as the
 * tallest. A lone last tile takes the whole row. FlowRow with
 * fillMaxRowHeight made each row taller than its content because the row
 * height came from a measurement taken before the weights were applied;
 * Row's intrinsic height accounts for them.
 */
@Composable
fun AppTileGrid(
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    spacing: Dp = 12.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        tiles.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
            }
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
                Icon(icon, null, tint = accent, modifier = Modifier.size(AppIconSize.xs))
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
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    // The action used to sit on its own row under the title. On a card with no
    // description that left a visible empty band between the heading and the
    // content, which read as broken layout.
    //
    // The action is measured first, capped at half the width so a wide one
    // (two buttons, a pair of chips) can't squeeze the heading to nothing, and
    // pinned to the end. A Row with two weights split the width 50/50 instead,
    // which parked a short action like "Review" in the middle of the card.
    val gap = 12.dp
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            AppSectionTitleText(title, description)
            if (action != null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = action
                )
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val actionPlaceable = measurables.getOrNull(1)?.measure(
            Constraints(maxWidth = ((width - gapPx) / 2).coerceAtLeast(0))
        )
        val titleWidth = if (actionPlaceable == null) width else width - actionPlaceable.width - gapPx
        val titlePlaceable = measurables[0].measure(
            Constraints(minWidth = 0, maxWidth = titleWidth.coerceAtLeast(0))
        )
        val height = maxOf(titlePlaceable.height, actionPlaceable?.height ?: 0)
        layout(width, height) {
            titlePlaceable.placeRelative(0, (height - titlePlaceable.height) / 2)
            actionPlaceable?.placeRelative(
                width - actionPlaceable.width,
                (height - actionPlaceable.height) / 2
            )
        }
    }
}

@Composable
private fun AppSectionTitleText(title: String, description: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

/**
 * Material 3's text buttons default to a fully rounded shape, which shows up as
 * a pill on press and focus. Same parameters as [TextButton], app shape.
 */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = AppButtonShape,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
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
    val minHeight = if (onClick != null) 40.dp else 28.dp
    val horizontalPadding = if (onClick != null) 12.dp else 9.dp
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
                    modifier = Modifier.size(AppIconSize.xs)
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
            tonalElevation = 0.dp,
            onClick = onClick,
            content = chipContent
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shapeTokens.chip,
            color = color.copy(alpha = 0.10f),
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
        modifier = modifier.semantics(mergeDescendants = true) {
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
                    tonalElevation = 0.dp,
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(R.string.app_dismiss),
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
fun AppInlineNotice(
    message: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val shapeTokens = LocalAppShapeTokens.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = listOfNotNull(title, message).joinToString(". ")
            },
        shape = shapeTokens.tile,
        color = color.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(shapeTokens.iconContainer)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(AppIconSize.sm),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = message,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun AppLoadingCard(
    modifier: Modifier = Modifier,
    height: Dp = 148.dp,
    label: String = "Loading content"
) {
    val shapeTokens = LocalAppShapeTokens.current
    val alpha = if (LocalMotionEnabled.current) {
        rememberInfiniteTransition(label = "loading-card").animateFloat(
            initialValue = 0.42f,
            targetValue = 0.86f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loading-alpha"
        ).value
    } else {
        0.72f
    }

    AppSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = label
            }
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
 * stroke. Tap-target is the full chip, not just the label, and TalkBack gets
 * a real selected state instead of a hand-built label suffix.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    // Defaulting this to `selected` meant an unselected chip announced no state
    // at all, so a screen reader could not tell a filter chip from a button.
    selectionSemantics: Boolean = true,
    accessibilityLabel: String? = null,
) {
    val isSelected = selected
    val shapeTokens = LocalAppShapeTokens.current
    val accent = MaterialTheme.colorScheme.primary
    val containerColor = if (isSelected) accent.copy(alpha = 0.16f) else SurfaceLight
    val labelColor = if (isSelected) accent else TextSecondary
    // semantics {} is not a composable scope, so resolve this first.
    val selectionStateDescription = if (isSelected) {
        stringResource(R.string.settings_selected)
    } else {
        stringResource(R.string.settings_not_selected)
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                if (!accessibilityLabel.isNullOrBlank()) {
                    contentDescription = accessibilityLabel
                }
                if (selectionSemantics) {
                    this.selected = isSelected
                    stateDescription = selectionStateDescription
                }
            },
        shape = shapeTokens.chip,
        color = containerColor,
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 13.dp, vertical = 8.dp),
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
    val alpha = if (LocalMotionEnabled.current) {
        rememberInfiniteTransition(label = "skeleton-block").animateFloat(
            initialValue = 0.36f,
            targetValue = 0.78f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeleton-alpha",
        ).value
    } else {
        0.65f
    }
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

/**
 * Square-ended 4dp progress bar. M3's round caps and end dot turn a bar this
 * thin into a capsule, so both are switched off here. A null [progress] runs
 * the indeterminate animation.
 */
@Composable
fun AppProgressBar(
    progress: (() -> Float)?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = BorderSubtle,
) {
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier.height(4.dp),
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
    } else {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier.height(4.dp),
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

/** Sheet grab bar. The default rounds a 4dp bar with the theme's 8-12dp radius, which is a capsule. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheetDragHandle() {
    BottomSheetDefaults.DragHandle(shape = RectangleShape, color = TextMuted)
}

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    // Rides on the accent track, so it has to follow the accent's contrast.
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = ToggleOff,
    uncheckedTrackColor = ToggleTrackOff,
    uncheckedBorderColor = BorderSubtle,
    // M3's disabled defaults paint a dark surface-colored thumb that reads as
    // a broken control on these cards; a dimmed copy of the live state reads
    // as "locked" instead.
    disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
    disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
    disabledCheckedBorderColor = Color.Transparent,
    disabledUncheckedThumbColor = ToggleOff.copy(alpha = 0.38f),
    disabledUncheckedTrackColor = ToggleTrackOff.copy(alpha = 0.38f),
    disabledUncheckedBorderColor = BorderSubtle
)

/**
 * Edge-aligned bottom navigation. A single divider is enough separation from
 * content; removing the floating outlined shell keeps navigation subordinate
 * to the page.
 */
@Composable
fun BottomNavContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(0.dp),
        color = SurfaceMedium,
        shadowElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(color = BorderSubtle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                content()
            }
        }
    }
}
