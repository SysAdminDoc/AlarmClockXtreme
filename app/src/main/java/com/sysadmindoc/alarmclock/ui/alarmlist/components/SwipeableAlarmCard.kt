package com.sysadmindoc.alarmclock.ui.alarmlist.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

/**
 * Wraps content with a SwipeToDismiss gesture.
 * Swiping right-to-left reveals a red delete background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAlarmCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { it * 0.55f }
    )

    // v1.7.5: rememberSwipeToDismissBoxState persists offset via Saver, so a
    // gesture interrupted mid-swipe (back press, tab navigation, etc.) can
    // leave the card stuck visually offset on next composition. Snap back
    // to Settled once on first composition so a partial drag never persists
    // across navigation.
    LaunchedEffect(Unit) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val willDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val isSwiping = dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val color by animateColorAsState(
                if (willDelete) AccentRed.copy(alpha = 0.86f) else Color.Transparent,
                label = stringResource(R.string.swipe_swipe_bg)
            )
            val scale by animateFloatAsState(
                if (willDelete) 1.05f else 0.86f,
                label = "icon_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                // v1.7.5: Only render the delete affordance when the user is
                // actively swiping. Otherwise the "Swipe to delete" text and
                // trash icon bleed through any semi-transparent foreground —
                // which is exactly how disabled alarm cards (alpha 0.55)
                // render — and look like a stuck swipe state.
                if (isSwiping) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (willDelete) stringResource(R.string.alarmlist_release_to_delete) else stringResource(R.string.alarmlist_swipe_to_delete),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.swipe_undo_available_right_after_removal),
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.alarm_list_delete),
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .scale(scale)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        ) {
            content()
        }
    }
}
