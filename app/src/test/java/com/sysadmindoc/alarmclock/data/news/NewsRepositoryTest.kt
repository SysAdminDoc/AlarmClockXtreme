package com.sysadmindoc.alarmclock.data.news

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NewsRepositoryTest {
    private lateinit var context: Context
    private val moshi = Moshi.Builder().build()
    private val feedUrl = "https://feeds.example/news.xml"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.resolve("news_last_good_v1.json").delete()
    }

    @Test
    fun fetchFeedReturnsStaleDiskCacheWhenRefreshFails() = runTest {
        val writer = NewsRepository(
            context = context,
            httpClient = rssClient(sampleRss(title = "Fresh headline")),
            moshi = moshi
        )

        val fresh = writer.fetchFeed(feedUrl).getOrThrow()

        assertFalse(fresh.isStale)
        assertEquals("Fresh headline", fresh.items.single().title)

        val offline = NewsRepository(
            context = context,
            httpClient = failingClient(),
            moshi = moshi
        ).fetchFeed(feedUrl).getOrThrow()

        assertTrue(offline.isStale)
        assertEquals("Fresh headline", offline.items.single().title)
        assertTrue(offline.refreshError is UnknownHostException)
    }

    @Test
    fun fetchFeedDoesNotServeCacheForDifferentFeedUrl() = runTest {
        NewsRepository(
            context = context,
            httpClient = rssClient(sampleRss(title = "Cached headline")),
            moshi = moshi
        ).fetchFeed(feedUrl).getOrThrow()

        val result = NewsRepository(
            context = context,
            httpClient = failingClient(),
            moshi = moshi
        ).fetchFeed("https://feeds.example/other.xml")

        assertTrue(result.isFailure)
    }

    private fun rssClient(body: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/rss+xml".toMediaType()))
                    .build()
            }
            .build()

    private fun failingClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { throw UnknownHostException("offline") }
            .build()

    private fun sampleRss(title: String): String = """
        <?xml version="1.0" encoding="UTF-8" ?>
        <rss version="2.0">
          <channel>
            <title>Example News</title>
            <item>
              <title>$title</title>
              <link>https://example.com/story</link>
              <description>Summary</description>
              <pubDate>Thu, 02 Jul 2026 12:00:00 GMT</pubDate>
              <guid>story-1</guid>
            </item>
          </channel>
        </rss>
    """.trimIndent()
}
