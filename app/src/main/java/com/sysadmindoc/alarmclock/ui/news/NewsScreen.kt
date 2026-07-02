package com.sysadmindoc.alarmclock.ui.news

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.news.NewsItem
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppIconSize
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppSkeletonBlock
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val activeFeedLabel = state.feeds.firstOrNull { it.key == state.activeFeedKey }?.label
        ?: "Custom feed"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // v1.8.1: pull-to-refresh — RSS readers expect this gesture and it
        // replaces the icon-only refresh button as the primary affordance.
        // The button stays in the hero actions slot for accessibility.
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    AlarmClockHeroHeader(
                        title = "News",
                        subtitle = "Headlines from your selected feed.",
                        overline = "Headlines",
                        badge = {
                            AppStatusChip(
                                label = activeFeedLabel,
                                icon = Icons.Default.RssFeed
                            )
                            state.lastUpdatedMillis?.let {
                                AppStatusChip(
                                    label = formatRelativeShort(it),
                                    icon = Icons.Default.Schedule
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = viewModel::refresh) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh feed",
                                    tint = TextPrimary
                                )
                            }
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.feeds.forEach { feed ->
                            AppFilterChip(
                                label = feed.label,
                                selected = feed.key == state.activeFeedKey,
                                onClick = { viewModel.selectFeed(feed.key) },
                                selectionSemantics = true,
                            )
                        }
                    }
                }

                if (state.isStale && !state.staleMessage.isNullOrBlank()) {
                    item {
                        Box(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            )
                        ) {
                            AppInlineNotice(
                                title = "Showing saved headlines",
                                message = state.staleMessage.orEmpty(),
                                icon = Icons.Default.Refresh,
                                color = SnoozeYellow
                            )
                        }
                    }
                }

                when {
                    state.loading -> {
                        items(count = 4, key = { "skeleton-$it" }) {
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 6.dp,
                                )
                            ) {
                                NewsCardSkeleton()
                            }
                        }
                    }

                    state.errorMessage != null -> {
                        item {
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp,
                                )
                            ) {
                                AppSurfaceCard {
                                    AppEmptyState(
                                        icon = Icons.Default.RssFeed,
                                        title = "Couldn't load this feed",
                                        description = state.errorMessage
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let { "Tap Refresh to try again, or pick a different source. $it" }
                                            ?: "Tap Refresh to try again, or pick a different source.",
                                    )
                                }
                            }
                        }
                    }

                    state.items.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp,
                                )
                            ) {
                                AppSurfaceCard {
                                    AppEmptyState(
                                        icon = Icons.Default.RssFeed,
                                        title = "No headlines yet",
                                        description = "Pick a feed from the chips above.",
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            Box(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 8.dp,
                                )
                            ) {
                                AppSectionTitle(title = "Top stories")
                            }
                        }

                        items(state.items, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 6.dp,
                                )
                            ) {
                                NewsCard(
                                    item = item,
                                    onClick = {
                                        if (item.link.isNotBlank()) {
                                            uriHandler.openUri(item.link)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    onClick: () -> Unit
) {
    val cleanedDescription = remember(item.description) {
        if (item.description.isBlank()) "" else stripHtml(item.description).trim()
    }
    val linkModifier = if (item.link.isNotBlank()) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(linkModifier)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
            )

            if (cleanedDescription.isNotBlank()) {
                Text(
                    text = cleanedDescription,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.source.isNotBlank()) {
                    AppStatusChip(
                        label = item.source,
                        icon = Icons.Default.RssFeed,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item.publishedAtMillis?.let {
                    AppStatusChip(
                        label = formatRelativeShort(it),
                        icon = Icons.Default.Schedule
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open article",
                    tint = TextMuted,
                    modifier = Modifier.size(AppIconSize.sm)
                )
            }
        }
    }
}

@Composable
private fun NewsCardSkeleton() {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppSkeletonBlock(modifier = Modifier.fillMaxWidth(0.78f), height = 18.dp)
            AppSkeletonBlock(modifier = Modifier.fillMaxWidth())
            AppSkeletonBlock(modifier = Modifier.fillMaxWidth(0.62f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSkeletonBlock(
                    modifier = Modifier.fillMaxWidth(0.32f),
                    height = 22.dp,
                    cornerRadius = 8.dp,
                )
                AppSkeletonBlock(
                    modifier = Modifier.fillMaxWidth(0.22f),
                    height = 22.dp,
                    cornerRadius = 8.dp,
                )
            }
        }
    }
}

private fun stripHtml(raw: String): String {
    return runCatching {
        Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT).toString()
    }.getOrDefault(raw)
}

private fun formatRelativeShort(epochMs: Long): String {
    val deltaMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(deltaMs)
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${TimeUnit.SECONDS.toMinutes(seconds)}m ago"
        seconds < 86_400 -> "${TimeUnit.SECONDS.toHours(seconds)}h ago"
        else -> "${TimeUnit.SECONDS.toDays(seconds)}d ago"
    }
}
