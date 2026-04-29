package com.sysadmindoc.alarmclock.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches an RSS/Atom feed and parses it into a list of [NewsItem]. The
 * surface is intentionally tiny — one suspending function that returns a
 * [Result] so the ViewModel can emit a clean error state without leaking
 * exception types into UI.
 *
 * Reuses the shared OkHttpClient (15 s timeouts, see NetworkModule) so the
 * News tab inherits the same network policy as weather and holiday calls.
 *
 * No caching layer — RSS is small (BBC ~28 KB, Google News ~126 KB, NPR
 * ~14 KB), and the user's pull-to-refresh signal is the right invalidation
 * trigger. If we ever hit rate limits we'll add an LRU + ETag check.
 */
@Singleton
class NewsRepository @Inject constructor(
    private val httpClient: OkHttpClient,
) {

    suspend fun fetchFeed(url: String): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                // Some publishers return HTML when the User-Agent is blank;
                // a plain UA flips them back to RSS. We don't impersonate a
                // browser — that would invite content-shape changes we can't
                // parse — just identify the app honestly.
                .header("User-Agent", "AlarmClockXtreme/1.8.0 (Android)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Feed returned HTTP ${response.code}")
                }
                val body = response.body ?: error("Empty response body")
                body.byteStream().use { stream ->
                    RssParser.parse(stream, defaultSource = "")
                        .sortedByDescending { it.publishedAtMillis ?: 0L }
                }
            }
        }
    }
}
