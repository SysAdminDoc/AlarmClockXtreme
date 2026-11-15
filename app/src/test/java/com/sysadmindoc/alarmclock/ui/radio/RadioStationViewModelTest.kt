package com.sysadmindoc.alarmclock.ui.radio

import com.sysadmindoc.alarmclock.data.remote.RadioBrowserApi
import com.sysadmindoc.alarmclock.data.remote.RadioStationDto
import com.sysadmindoc.alarmclock.data.repository.RadioStationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RadioStationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakePreview : RadioPreview {
        private val _state = MutableStateFlow<RadioPreview.State>(RadioPreview.State.Idle)
        override val state: StateFlow<RadioPreview.State> = _state
        val played = mutableListOf<String>()
        var stops = 0

        override fun play(url: String) {
            played += url
            _state.value = RadioPreview.State.Playing(url)
        }

        override fun stop() {
            stops++
            _state.value = RadioPreview.State.Idle
        }

        fun fail(url: String) {
            _state.value = RadioPreview.State.Failed(url)
        }
    }

    private class FakeApi(var down: Boolean = false) : RadioBrowserApi {
        val queries = mutableListOf<String?>()
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
            if (name != null) queries += name
            if (down) throw IOException("offline")
            val term = name ?: tag ?: return emptyList()
            return listOf(
                RadioStationDto(
                    stationUuid = "uuid-$term",
                    name = "Station $term",
                    url = "https://$term.example/live",
                    lastCheckOk = 1
                )
            )
        }

        override suspend fun countClick(stationUuid: String): Response<ResponseBody> {
            clicks += stationUuid
            return Response.success("{}".toResponseBody())
        }
    }

    private val api = FakeApi()
    private val preview = FakePreview()
    private val playlists = mutableMapOf<String, String>()
    private val viewModel = RadioStationViewModel(
        RadioStationRepository(listOf(api)) { url -> playlists[url] },
        preview
    )

    @Test
    fun `typing is debounced into one search for the final text`() = runTest(dispatcher) {
        viewModel.onQueryChange("j")
        viewModel.onQueryChange("ja")
        viewModel.onQueryChange("jaz")
        viewModel.onQueryChange("jazz")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("jazz"), api.queries)
        val state = viewModel.search.value
        assertEquals("jazz", state.searchedQuery)
        assertEquals(listOf("uuid-jazz"), state.results.map { it.id })
        assertFalse(state.searching)
    }

    @Test
    fun `one letter clears the list without asking the directory`() = runTest(dispatcher) {
        viewModel.onQueryChange("b")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(api.queries.isEmpty())
        assertNull(viewModel.search.value.searchedQuery)
    }

    @Test
    fun `an unreachable directory is reported and retry searches again`() = runTest(dispatcher) {
        api.down = true
        viewModel.onQueryChange("news")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.search.value.unreachable)

        api.down = false
        viewModel.searchNow()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.search.value.unreachable)
        assertEquals(listOf("uuid-news"), viewModel.search.value.results.map { it.id })
    }

    @Test
    fun `preview toggles per station and only one plays`() = runTest(dispatcher) {
        viewModel.togglePreview("https://a.example/live")
        viewModel.togglePreview("https://b.example/live")
        assertEquals(listOf("https://a.example/live", "https://b.example/live"), preview.played)

        viewModel.togglePreview("https://b.example/live")
        assertEquals(RadioPreview.State.Idle, preview.state.value)
    }

    @Test
    fun `tapping a failed station tries it again instead of stopping`() = runTest(dispatcher) {
        preview.fail("https://a.example/live")
        viewModel.togglePreview("https://a.example/live")
        assertEquals(listOf("https://a.example/live"), preview.played)
    }

    @Test
    fun `choosing a station stops the preview, reports the play and returns its stream`() = runTest(dispatcher) {
        viewModel.onQueryChange("jazz")
        dispatcher.scheduler.advanceUntilIdle()
        val station = viewModel.search.value.results.single()
        viewModel.togglePreview(station.streamUrl)

        val chosen = viewModel.choose(station)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://jazz.example/live", chosen)
        assertEquals(RadioPreview.State.Idle, preview.state.value)
        assertEquals(listOf("uuid-jazz"), api.clicks)
    }

    @Test
    fun `a pasted playlist link is replaced by the stream inside it`() = runTest(dispatcher) {
        playlists["https://radio.example/listen.pls"] = "[playlist]\nFile1=https://ice.example/live\n"

        viewModel.onStreamUrlChanged("https://radio.example/listen.pls")
        assertEquals(RadioPlaylistStatus.Reading, viewModel.playlist.value)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            RadioPlaylistStatus.Replaced("https://radio.example/listen.pls", "https://ice.example/live"),
            viewModel.playlist.value
        )

        // The editor then writes the stream back; the note stays up for it.
        viewModel.onStreamUrlChanged("https://ice.example/live")
        assertTrue(viewModel.playlist.value is RadioPlaylistStatus.Replaced)

        // Any other edit clears it.
        viewModel.onStreamUrlChanged("https://other.example/stream")
        assertEquals(RadioPlaylistStatus.None, viewModel.playlist.value)
    }

    @Test
    fun `an unreadable or http only playlist is reported, not applied`() = runTest(dispatcher) {
        viewModel.onStreamUrlChanged("https://radio.example/missing.m3u")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RadioPlaylistStatus.Unreadable, viewModel.playlist.value)

        playlists["https://radio.example/plain.m3u"] = "http://ice.example/plain\n"
        viewModel.onStreamUrlChanged("https://radio.example/plain.m3u")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RadioPlaylistStatus.OnlyInsecure, viewModel.playlist.value)
    }

    @Test
    fun `editing again before the lookup finishes cancels it`() = runTest(dispatcher) {
        playlists["https://radio.example/a.pls"] = "File1=https://ice.example/a\n"
        viewModel.onStreamUrlChanged("https://radio.example/a.pls")
        viewModel.onStreamUrlChanged("https://ice.example/typed-by-hand")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RadioPlaylistStatus.None, viewModel.playlist.value)
    }
}
