package com.sysadmindoc.alarmclock.ui.news

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
                        subtitle = buildList {
                            add(activeFeedLabel)
                            state.lastUpdatedMillis?.let { add(formatRelativeShort(it)) }
                        }.joinToString(" · "),
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
                            val selected = feed.key == state.activeFeedKey
                            Column(
                                modifier = Modifier
                                    .clickable(role = Role.Button) {
                                        viewModel.selectFeed(feed.key)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = feed.label.substringAfter("— ", feed.label),
                                    color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(3.dp)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                )
                            }
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
                        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 0.dp,
                                )
                            ) {
                                Column {
                                    NewsCard(
                                        item = item,
                                        onClick = {
                                            if (item.hasOpenableLink) {
                                                runCatching { uriHandler.openUri(item.link) }
                                            }
                                        },
                                    )
                                    if (index < state.items.lastIndex) {
                                        HorizontalDivider(color = TextMuted.copy(alpha = 0.18f))
                                    }
                                }
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
    val meaningfulDescription = remember(item.title, cleanedDescription) {
        val titleLead = item.title
            .substringBefore(" - ")
            .substringBefore(" – ")
            .normalizeNewsComparisonText()
        val descriptionLead = cleanedDescription.normalizeNewsComparisonText()
        if (titleLead.length >= 24 && descriptionLead.startsWith(titleLead)) {
            ""
        } else {
            cleanedDescription
                .replaceFirst(
                    Regex("^${Regex.escape(item.title)}\\s*", RegexOption.IGNORE_CASE),
                    ""
                )
                .trim()
                .takeIf { it.length >= 24 }
                .orEmpty()
        }
    }
    val linkModifier = if (item.hasOpenableLink) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(linkModifier)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = item.title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
        )

        if (meaningfulDescription.isNotBlank()) {
            Text(
                text = meaningfulDescription,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = buildList {
                    if (item.source.isNotBlank()) add(item.source)
                    item.publishedAtMillis?.let { add(formatRelativeShort(it)) }
                }.joinToString(" · "),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open article",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppIconSize.sm)
            )
        }
    }
}

@Composable
private fun NewsCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppSkeletonBlock(modifier = Modifier.fillMaxWidth(0.78f), height = 18.dp)
        AppSkeletonBlock(modifier = Modifier.fillMaxWidth())
        AppSkeletonBlock(modifier = Modifier.fillMaxWidth(0.62f))
        AppSkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.28f)
                .padding(top = 4.dp),
            height = 14.dp,
            cornerRadius = 6.dp,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            color = TextMuted.copy(alpha = 0.18f)
        )
    }
}

private val textBeforeTagBoundaryRegex = Regex("(?<=[\\p{L}\\p{N}\\p{Punct}])\\s*(?=<[A-Za-z][^>]*>)")
private val adjacentTagBoundaryRegex = Regex("(?<=>)\\s*(?=<[A-Za-z][^>]*>)")
private val tagBeforeTextBoundaryRegex = Regex("(?<=>)\\s*(?=[\\p{L}\\p{N}])")
private val htmlTagRegex = Regex("<[^>]+>")
private val htmlEntityRegex = Regex("&(#x?[0-9A-Fa-f]+|[A-Za-z][A-Za-z0-9]+);")
private val whitespaceRegex = Regex("\\s+")
private val comparisonPunctuationRegex = Regex("[^\\p{L}\\p{N}]+")
private val namedHtmlEntities = mapOf(
    "amp" to "&",
    "apos" to "'",
    "gt" to ">",
    "hellip" to "...",
    "laquo" to "\"",
    "ldquo" to "\"",
    "lsquo" to "'",
    "lt" to "<",
    "mdash" to "-",
    "nbsp" to " ",
    "ndash" to "-",
    "quot" to "\"",
    "raquo" to "\"",
    "rdquo" to "\"",
    "rsquo" to "'",
)

internal fun stripHtml(raw: String): String {
    val textWithBoundaries = raw
        .replace(textBeforeTagBoundaryRegex, " ")
        .replace(adjacentTagBoundaryRegex, " ")
        .replace(tagBeforeTextBoundaryRegex, " ")
        .replace(htmlTagRegex, " ")
    return decodeHtmlEntities(textWithBoundaries)
        .replace('\u00A0', ' ')
        .replace(whitespaceRegex, " ")
        .trim()
}

private fun String.normalizeNewsComparisonText(): String =
    lowercase()
        .replace(comparisonPunctuationRegex, " ")
        .trim()

private fun decodeHtmlEntities(raw: String): String =
    htmlEntityRegex.replace(raw) { match ->
        val entity = match.groupValues[1]
        when {
            entity.startsWith("#x", ignoreCase = true) ->
                entity.drop(2).toIntOrNull(16)?.toCodePointString()

            entity.startsWith("#") ->
                entity.drop(1).toIntOrNull()?.toCodePointString()

            else -> namedHtmlEntities[entity.lowercase()]
        } ?: match.value
    }

private fun Int.toCodePointString(): String? =
    takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }

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
