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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.sysadmindoc.alarmclock.ui.components.AppLoadingCard
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.util.concurrent.TimeUnit

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                AlarmClockHeroHeader(
                    title = "News",
                    subtitle = "Top stories pulled live from public RSS feeds. No accounts, no tracking.",
                    overline = "Headlines",
                    badge = {
                        AppStatusChip(
                            label = state.feeds.firstOrNull { it.key == state.activeFeedKey }?.label
                                ?: "Custom feed",
                            icon = Icons.Default.RssFeed
                        )
                        state.lastUpdatedMillis?.let {
                            AppStatusChip(
                                label = "Updated ${formatRelativeShort(it)}",
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
                        FilterChip(
                            selected = feed.key == state.activeFeedKey,
                            onClick = { viewModel.selectFeed(feed.key) },
                            label = { Text(feed.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                labelColor = TextSecondary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                }
            }

            when {
                state.loading -> {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AppLoadingCard()
                        }
                    }
                }

                state.errorMessage != null -> {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            AppSurfaceCard {
                                AppEmptyState(
                                    icon = Icons.Default.RssFeed,
                                    title = "Couldn't load this feed",
                                    description = state.errorMessage ?: "",
                                    footer = {
                                        AssistChip(
                                            onClick = viewModel::refresh,
                                            label = { Text("Try again") },
                                            colors = AssistChipDefaults.assistChipColors(
                                                labelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                state.items.isEmpty() -> {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            AppSurfaceCard {
                                AppEmptyState(
                                    icon = Icons.Default.RssFeed,
                                    title = "Nothing to show yet",
                                    description = "Pick a feed from the chips above or pull to refresh."
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) {
                            AppSectionTitle(
                                title = "Top stories",
                                description = "Tap any headline to read in your browser."
                            )
                        }
                    }

                    items(state.items, key = { it.id }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            NewsCard(
                                item = item,
                                onClick = {
                                    if (item.link.isNotBlank()) uriHandler.openUri(item.link)
                                }
                            )
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

    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
                        label = formatRelativeLong(it),
                        icon = Icons.Default.Schedule
                    )
                }
                Spacer(modifier = Modifier.size(0.dp))
                Box(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open article",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
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

private fun formatRelativeLong(epochMs: Long): String = formatRelativeShort(epochMs)
