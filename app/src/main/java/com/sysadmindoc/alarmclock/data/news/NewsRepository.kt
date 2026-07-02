package com.sysadmindoc.alarmclock.data.news

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class NewsCacheEnvelope(
    val url: String,
    val fetchedAtMillis: Long,
    val items: List<NewsItem>
)

data class NewsFeedSnapshot(
    val items: List<NewsItem>,
    val fetchedAtMillis: Long,
    val isStale: Boolean,
    val refreshError: Throwable? = null
)

/**
 * Fetches an RSS/Atom feed and parses it into a list of [NewsItem]. The
 * surface is intentionally tiny: one suspending function that returns a
 * [Result] so the ViewModel can emit a clean error state without leaking
 * exception types into UI.
 *
 * Reuses the shared OkHttpClient (15 s timeouts, see NetworkModule) so the
 * News tab inherits the same network policy as weather and holiday calls.
 *
 * Keeps one compact last-good feed snapshot on disk. The cache is keyed by
 * feed URL, excluded from backup by the repo's existing include-only backup
 * rules, and is used only when a refresh fails.
 */
@Singleton
class NewsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    moshi: Moshi
) {
    private val cacheAdapter = moshi.adapter(NewsCacheEnvelope::class.java).indent("  ")
    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun fetchFeed(url: String): Result<NewsFeedSnapshot> = withContext(Dispatchers.IO) {
        try {
            val items = fetchNetworkFeed(url)
            val fetchedAt = System.currentTimeMillis()
            writeCache(NewsCacheEnvelope(url, fetchedAt, items))
            Result.success(
                NewsFeedSnapshot(
                    items = items,
                    fetchedAtMillis = fetchedAt,
                    isStale = false
                )
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val cached = readCache(url)
            if (cached != null) {
                Result.success(
                    NewsFeedSnapshot(
                        items = cached.items,
                        fetchedAtMillis = cached.fetchedAtMillis,
                        isStale = true,
                        refreshError = error
                    )
                )
            } else {
                Result.failure(error)
            }
        }
    }

    private fun fetchNetworkFeed(url: String): List<NewsItem> {
        val request = Request.Builder()
            .url(url)
            // Some publishers return HTML when the User-Agent is blank;
            // a plain UA flips them back to RSS. We don't impersonate a
            // browser because that would invite content-shape changes we
            // cannot parse; just identify the app honestly.
            .header("User-Agent", "AlarmClockXtreme/${BuildConfig.VERSION_NAME} (Android)")
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8")
            .build()

        return httpClient.newCall(request).execute().use { response ->
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

    private fun writeCache(envelope: NewsCacheEnvelope) {
        runCatching {
            cacheFile.writeText(cacheAdapter.toJson(envelope))
        }
    }

    private fun readCache(url: String): NewsCacheEnvelope? {
        return runCatching {
            if (!cacheFile.exists()) return null
            val envelope = cacheAdapter.fromJson(cacheFile.readText()) ?: return null
            val ageMs = System.currentTimeMillis() - envelope.fetchedAtMillis
            if (ageMs < 0 || ageMs > MAX_STALE_CACHE_MS || envelope.url != url) {
                return null
            }
            envelope
        }.getOrNull()
    }

    companion object {
        private const val CACHE_FILE_NAME = "news_last_good_v1.json"
        private const val MAX_STALE_CACHE_MS = 48L * 60 * 60 * 1000
    }
}
