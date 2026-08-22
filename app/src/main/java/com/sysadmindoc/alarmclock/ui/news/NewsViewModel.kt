package com.sysadmindoc.alarmclock.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.news.NewsItem
import com.sysadmindoc.alarmclock.data.news.NewsRepository
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.inject.Inject

/**
 * Predefined feeds. The user can paste their own URL via Settings, but we
 * curate a short list of RSS-friendly publishers as quick-pick chips so the
 * tab is useful out of the box. All confirmed working with no auth in our
 * research pass — Reddit and X were excluded (auth-walled now).
 */
data class NewsFeedSource(
    val key: String,
    val label: String,
    val url: String,
)

val DEFAULT_NEWS_FEEDS = listOf(
    NewsFeedSource(
        key = "google_top",
        label = "Google News: Top",
        url = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"
    ),
    NewsFeedSource(
        key = "google_world",
        label = "Google News: World",
        url = "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-US&gl=US&ceid=US:en"
    ),
    NewsFeedSource(
        key = "google_tech",
        label = "Google News: Tech",
        url = "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en"
    ),
    NewsFeedSource(
        key = "bbc",
        label = "BBC: Top stories",
        url = "https://feeds.bbci.co.uk/news/rss.xml"
    ),
    NewsFeedSource(
        key = "npr",
        label = "NPR: News",
        url = "https://feeds.npr.org/1001/rss.xml"
    ),
    NewsFeedSource(
        key = "hn",
        label = "Hacker News: Front page",
        url = "https://hnrss.org/frontpage"
    ),
)

data class NewsUiState(
    val feeds: List<NewsFeedSource> = DEFAULT_NEWS_FEEDS,
    val activeFeedKey: String = DEFAULT_NEWS_FEEDS.first().key,
    val activeFeedUrl: String = DEFAULT_NEWS_FEEDS.first().url,
    val items: List<NewsItem> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdatedMillis: Long? = null,
    val isStale: Boolean = false,
    val staleMessage: String? = null,
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            // Pull a one-shot snapshot so the user's saved override (if any)
            // wins. We don't need to subscribe to settings — the news URL
            // doesn't change often enough to warrant live recomposition.
            val saved = preferencesManager.settings.firstOrNull()?.newsFeedUrl
            val matching = saved?.let { url ->
                DEFAULT_NEWS_FEEDS.firstOrNull { it.url == url }
            }
            if (matching != null) {
                _uiState.value = _uiState.value.copy(
                    activeFeedKey = matching.key,
                    activeFeedUrl = matching.url
                )
            } else if (!saved.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    activeFeedKey = "custom",
                    activeFeedUrl = saved
                )
            }
            refresh()
        }
    }

    fun selectFeed(key: String) {
        val feed = DEFAULT_NEWS_FEEDS.firstOrNull { it.key == key } ?: return
        if (feed.key == _uiState.value.activeFeedKey) return
        _uiState.value = _uiState.value.copy(
            activeFeedKey = feed.key,
            activeFeedUrl = feed.url,
            items = emptyList(),
            errorMessage = null,
            isStale = false,
            staleMessage = null,
        )
        viewModelScope.launch {
            preferencesManager.update { it.copy(newsFeedUrl = feed.url) }
        }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isFirstLoad = _uiState.value.items.isEmpty()
            _uiState.value = _uiState.value.copy(
                loading = isFirstLoad,
                refreshing = !isFirstLoad,
                errorMessage = null
            )
            val url = _uiState.value.activeFeedUrl
            repository.fetchFeed(url)
                .onSuccess { snapshot ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        items = snapshot.items,
                        errorMessage = null,
                        lastUpdatedMillis = snapshot.fetchedAtMillis,
                        isStale = snapshot.isStale,
                        staleMessage = if (snapshot.isStale) {
                            buildNewsStaleMessage(snapshot.refreshError)
                        } else {
                            null
                        },
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = newsLoadErrorMessage(error),
                        isStale = false,
                        staleMessage = null,
                    )
                }
        }
    }
}

private fun buildNewsStaleMessage(error: Throwable?): String {
    val reason = error?.let(::newsLoadErrorMessage)
        ?: "The feed could not be refreshed."
    return "Showing saved headlines. Use Refresh to try again. $reason"
}

internal fun newsLoadErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        error is UnknownHostException ->
            "Check your connection and try again."
        error is SocketTimeoutException ->
            "This feed took too long to respond. Try again later."
        error is SSLException ->
            "This feed could not be loaded securely. Choose another source."
        error is IllegalArgumentException ->
            "This feed URL is not valid. Choose another source in Settings."
        message.contains("HTTP 401") || message.contains("HTTP 403") ->
            "This feed requires access the app does not have. Choose another source."
        message.contains("HTTP", ignoreCase = true) ->
            "This feed source is not responding. Try another source or refresh later."
        message.contains("Empty response body", ignoreCase = true) ->
            "This source returned no feed content."
        error is IOException ->
            "The feed could not be reached. Check your connection and try again."
        else ->
            "This feed could not be read. Try another source or refresh later."
    }
}
