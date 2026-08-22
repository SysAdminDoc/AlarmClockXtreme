package com.sysadmindoc.alarmclock.ui.news

import android.content.res.Resources
import com.sysadmindoc.alarmclock.R
import androidx.annotation.StringRes
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
    @StringRes val labelRes: Int,
    /** Tab caption. Its own field so no separator has to survive translation. */
    @StringRes val shortLabelRes: Int,
    val url: String,
)

val DEFAULT_NEWS_FEEDS = listOf(
    NewsFeedSource(
        key = "google_top",
        labelRes = R.string.news_feed_google_top,
        shortLabelRes = R.string.news_feed_google_top_short,
        url = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"
    ),
    NewsFeedSource(
        key = "google_world",
        labelRes = R.string.news_feed_google_world,
        shortLabelRes = R.string.news_feed_google_world_short,
        url = "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-US&gl=US&ceid=US:en"
    ),
    NewsFeedSource(
        key = "google_tech",
        labelRes = R.string.news_feed_google_tech,
        shortLabelRes = R.string.news_feed_google_tech_short,
        url = "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en"
    ),
    NewsFeedSource(
        key = "bbc",
        labelRes = R.string.news_feed_bbc,
        shortLabelRes = R.string.news_feed_bbc_short,
        url = "https://feeds.bbci.co.uk/news/rss.xml"
    ),
    NewsFeedSource(
        key = "npr",
        labelRes = R.string.news_feed_npr,
        shortLabelRes = R.string.news_feed_npr_short,
        url = "https://feeds.npr.org/1001/rss.xml"
    ),
    NewsFeedSource(
        key = "hn",
        labelRes = R.string.news_feed_hn,
        shortLabelRes = R.string.news_feed_hn_short,
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
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context,
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
                            buildNewsStaleMessage(appContext.resources, snapshot.refreshError)
                        } else {
                            null
                        },
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = appContext.getString(newsLoadErrorMessage(error)),
                        isStale = false,
                        staleMessage = null,
                    )
                }
        }
    }
}

private fun buildNewsStaleMessage(resources: Resources, error: Throwable?): String {
    val reason = resources.getString(
        error?.let(::newsLoadErrorMessage) ?: R.string.news_error_generic
    )
    return resources.getString(R.string.news_stale_headlines, reason)
}

/**
 * The message id for [error]. An id rather than the text: this runs in the
 * ViewModel, where the Resources to read it from belongs to the caller.
 */
@StringRes
internal fun newsLoadErrorMessage(error: Throwable): Int {
    val message = error.message.orEmpty()
    return when {
        error is UnknownHostException ->
            R.string.news_error_no_connection
        error is SocketTimeoutException ->
            R.string.news_error_timeout
        error is SSLException ->
            R.string.news_error_tls
        error is IllegalArgumentException ->
            R.string.news_error_invalid_url
        message.contains("HTTP 401") || message.contains("HTTP 403") ->
            R.string.news_error_forbidden
        message.contains("HTTP", ignoreCase = true) ->
            R.string.news_error_unresponsive
        message.contains("Empty response body", ignoreCase = true) ->
            R.string.news_error_empty
        error is IOException ->
            R.string.news_error_io
        else ->
            R.string.news_error_unreadable
    }
}
