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
import androidx.compose.ui.unit.sp
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

val AppCardShape = RoundedCornerShape(24.dp)
val AppCardBorderColor = TextMuted.copy(alpha = 0.16f)
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HeaderTop.copy(alpha = 0.95f),
                        SurfaceLight.copy(alpha = 0.96f),
                        HeaderBottom,
                        SurfaceDark
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                            Color.Transparent
                        )
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (badge != null) {
                Row(
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
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.animateContentSize(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ),
        shape = AppCardShape,
        border = BorderStroke(1.dp, AppCardBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                SurfaceMedium.copy(alpha = 0.98f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = action
            )
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
    Surface(
        modifier = modifier,
        shape = AppChipShape,
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
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
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
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
        initialValue = 0.52f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading-alpha"
    )

    AppSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(alpha)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(42.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceLight)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
        )
    }
}

@Composable
fun appOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = TextMuted.copy(alpha = 0.45f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = TextMuted,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = SurfaceCard.copy(alpha = 0.6f),
    unfocusedContainerColor = SurfaceCard.copy(alpha = 0.45f),
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted
)

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.primary,
    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
    uncheckedThumbColor = ToggleOff,
    uncheckedTrackColor = ToggleTrackOff
)

@Composable
fun BottomNavContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceMedium.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, AppCardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceLight.copy(alpha = 0.24f),
                            SurfaceMedium.copy(alpha = 0.98f)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}
