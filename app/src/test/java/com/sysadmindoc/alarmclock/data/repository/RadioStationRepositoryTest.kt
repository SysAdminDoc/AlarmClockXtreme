package com.sysadmindoc.alarmclock.data.repository

import com.squareup.moshi.JsonDataException
import com.sysadmindoc.alarmclock.data.remote.RadioBrowserApi
import com.sysadmindoc.alarmclock.data.remote.RadioStationDto
import com.sysadmindoc.alarmclock.data.repository.RadioStationRepository.PlaylistResult
import com.sysadmindoc.alarmclock.data.repository.RadioStationRepository.SearchResult
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RadioStationRepositoryTest {

    private class FakeApi(
        private val failure: Exception? = null,
        private val byName: List<RadioStationDto> = emptyList(),
        private val byTag: List<RadioStationDto> = emptyList()
    ) : RadioBrowserApi {
        val searches = mutableListOf<Pair<String?, String?>>()
        val clicks = mutableListOf<String>()

        override suspend fun searchStations(
            name: String?,
            tag: String?,
            limit: Int,
            hideBroken: Boolean,
            httpsOnly: Boolean,
            order: String,
            reverse: Boolean
        ): List<RadioStationDto> {
            searches += name to tag
            assertTrue("https filter must be asked of the server", httpsOnly)
            failure?.let { throw it }
            return if (name != null) byName else byTag
        }

        override suspend fun countClick(stationUuid: String): Response<ResponseBody> {
            clicks += stationUuid
            failure?.let { throw it }
            return Response.success("{}".toResponseBody())
        }
    }

    private fun station(uuid: String, url: String) = RadioStationDto(
        stationUuid = uuid,
        name = "Station $uuid",
        url = url,
        lastCheckOk = 1
    )

    private fun repo(vararg mirrors: RadioBrowserApi, fetch: suspend (String) -> String? = { null }) =
        RadioStationRepository(mirrors.toList(), fetch)

    @Test
    fun `search asks by name and by genre and merges them`() = runTest {
        val api = FakeApi(
            byName = listOf(station("a", "https://a.example/live")),
            byTag = listOf(station("b", "https://b.example/live"))
        )
        val result = repo(api).search("  jazz ")

        assertEquals(setOf("jazz" to null, null to "jazz"), api.searches.toSet())
        assertEquals(listOf("a", "b"), (result as SearchResult.Stations).stations.map { it.id })
    }

    @Test
    fun `a dead mirror falls through to the next one`() = runTest {
        val dead = FakeApi(failure = IOException("connection refused"))
        val broken = FakeApi(
            failure = HttpException(Response.error<Any>(503, "".toResponseBody()))
        )
        val odd = FakeApi(failure = JsonDataException("unexpected shape"))
        val live = FakeApi(byName = listOf(station("a", "https://a.example/live")))

        val result = repo(dead, broken, odd, live).search("bbc")

        assertEquals(listOf("a"), (result as SearchResult.Stations).stations.map { it.id })
    }

    @Test
    fun `every mirror down is unreachable, not an empty result`() = runTest {
        val result = repo(
            FakeApi(failure = IOException("a")),
            FakeApi(failure = IOException("b"))
        ).search("bbc")
        assertEquals(SearchResult.Unreachable, result)
    }

    @Test
    fun `a blank query asks nothing of the network`() = runTest {
        val api = FakeApi()
        assertEquals(SearchResult.Stations(emptyList()), repo(api).search("   "))
        assertTrue(api.searches.isEmpty())
    }

    @Test
    fun `a failed click count is swallowed and tries the next mirror`() = runTest {
        val dead = FakeApi(failure = IOException("down"))
        val live = FakeApi()
        val chosen = com.sysadmindoc.alarmclock.domain.RadioStation(
            id = "abc", name = "A", streamUrl = "https://a.example", country = "", codec = "", bitrate = 0, tags = emptyList()
        )
        repo(dead, live).countClick(chosen)
        assertEquals(listOf("abc"), dead.clicks)
        assertEquals(listOf("abc"), live.clicks)
    }

    @Test
    fun `a pls link resolves to its first https stream`() = runTest {
        val fetched = mutableListOf<String>()
        val repository = repo(FakeApi()) { url ->
            fetched += url
            "[playlist]\nFile1=http://ice.example/plain\nFile2=https://ice.example/secure\n"
        }
        assertEquals(
            PlaylistResult.Resolved("https://ice.example/secure"),
            repository.resolvePlaylist(" https://radio.example/listen.pls ")
        )
        assertEquals(listOf("https://radio.example/listen.pls"), fetched)
    }

    @Test
    fun `a pls pointing at an m3u is followed one level`() = runTest {
        val repository = repo(FakeApi()) { url ->
            when (url) {
                "https://radio.example/listen.pls" -> "File1=https://radio.example/inner.m3u\n"
                "https://radio.example/inner.m3u" -> "#EXTM3U\nhttps://ice.example/live\n"
                else -> null
            }
        }
        assertEquals(
            PlaylistResult.Resolved("https://ice.example/live"),
            repository.resolvePlaylist("https://radio.example/listen.pls")
        )
    }

    @Test
    fun `a playlist loop gives up instead of following it forever`() = runTest {
        var fetches = 0
        val repository = repo(FakeApi()) {
            fetches++
            "https://radio.example/again.m3u\n"
        }
        assertEquals(PlaylistResult.Failed, repository.resolvePlaylist("https://radio.example/start.m3u"))
        assertEquals(RadioStationRepository.MAX_PLAYLIST_DEPTH, fetches)
    }

    @Test
    fun `playlist outcomes other than a stream`() = runTest {
        val repository = repo(FakeApi()) { url ->
            when {
                url.contains("plain") -> "http://ice.example/a\n"
                url.contains("down") -> throw IOException("timeout")
                else -> null
            }
        }
        assertEquals(PlaylistResult.NotPlaylist, repository.resolvePlaylist("https://ice.example/stream.mp3"))
        assertEquals(PlaylistResult.OnlyInsecure, repository.resolvePlaylist("http://radio.example/listen.pls"))
        assertEquals(PlaylistResult.OnlyInsecure, repository.resolvePlaylist("https://radio.example/plain.pls"))
        assertEquals(PlaylistResult.Failed, repository.resolvePlaylist("https://radio.example/down.pls"))
        assertEquals(PlaylistResult.Failed, repository.resolvePlaylist("https://radio.example/missing.pls"))
    }
}
