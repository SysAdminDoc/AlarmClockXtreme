package com.sysadmindoc.alarmclock.ui.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.repository.RadioStationRepository
import com.sysadmindoc.alarmclock.data.repository.RadioStationRepository.PlaylistResult
import com.sysadmindoc.alarmclock.data.repository.RadioStationRepository.SearchResult
import com.sysadmindoc.alarmclock.domain.RadioPlaylist
import com.sysadmindoc.alarmclock.domain.RadioStation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadioSearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    /** The query the shown [results] belong to; null before the first search finishes. */
    val searchedQuery: String? = null,
    val results: List<RadioStation> = emptyList(),
    val unreachable: Boolean = false
)

sealed interface RadioPlaylistStatus {
    data object None : RadioPlaylistStatus
    data object Reading : RadioPlaylistStatus
    data class Replaced(val playlistUrl: String, val streamUrl: String) : RadioPlaylistStatus
    data object OnlyInsecure : RadioPlaylistStatus
    data object Unreadable : RadioPlaylistStatus
}

/**
 * Internet radio for the alarm editor (#65): the station search sheet, the
 * preview player, and the lookup that swaps a pasted .pls/.m3u link for the
 * stream inside it. Scoped to the editor screen, so the sheet and the card
 * share one preview and only one station ever plays.
 */
@HiltViewModel
class RadioStationViewModel @Inject constructor(
    private val repository: RadioStationRepository,
    private val preview: RadioPreview
) : ViewModel() {

    private val _search = MutableStateFlow(RadioSearchUiState())
    val search: StateFlow<RadioSearchUiState> = _search.asStateFlow()

    val previewState: StateFlow<RadioPreview.State> = preview.state

    private val _playlist = MutableStateFlow<RadioPlaylistStatus>(RadioPlaylistStatus.None)
    val playlist: StateFlow<RadioPlaylistStatus> = _playlist.asStateFlow()

    private var searchJob: Job? = null
    private var playlistJob: Job? = null

    fun onQueryChange(query: String) {
        _search.update { it.copy(query = query) }
        launchSearch(query, debounceMillis = SEARCH_DEBOUNCE_MS)
    }

    /** The keyboard's search key, or Retry after the directory was unreachable. */
    fun searchNow() {
        launchSearch(_search.value.query, debounceMillis = 0)
    }

    private fun launchSearch(query: String, debounceMillis: Long) {
        searchJob?.cancel()
        val term = query.trim()
        if (term.length < MIN_QUERY_CHARS) {
            _search.update {
                it.copy(searching = false, searchedQuery = null, results = emptyList(), unreachable = false)
            }
            return
        }
        searchJob = viewModelScope.launch {
            if (debounceMillis > 0) delay(debounceMillis)
            _search.update { it.copy(searching = true, unreachable = false) }
            when (val result = repository.search(term)) {
                is SearchResult.Stations -> _search.update {
                    it.copy(searching = false, searchedQuery = term, results = result.stations, unreachable = false)
                }
                SearchResult.Unreachable -> _search.update {
                    it.copy(searching = false, searchedQuery = term, results = emptyList(), unreachable = true)
                }
            }
        }
    }

    fun togglePreview(url: String) {
        val current = preview.state.value
        val active = current is RadioPreview.State.Loading || current is RadioPreview.State.Playing
        if (active && current.streamUrl == url) {
            preview.stop()
        } else {
            preview.play(url)
        }
    }

    fun stopPreview() {
        preview.stop()
    }

    /** The station's stream is what gets saved; the click count only helps the directory's ranking. */
    fun choose(station: RadioStation): String {
        preview.stop()
        viewModelScope.launch { repository.countClick(station) }
        return station.streamUrl
    }

    /**
     * Called whenever the editor's stream address changes. A playlist link is
     * read after a short pause and, when it holds an https stream, reported as
     * [RadioPlaylistStatus.Replaced] for the editor to apply.
     */
    fun onStreamUrlChanged(url: String) {
        val trimmed = url.trim()
        val shown = _playlist.value
        // The address the editor just put in place of the playlist; keep the note up.
        if (shown is RadioPlaylistStatus.Replaced && shown.streamUrl == trimmed) return
        playlistJob?.cancel()
        if (!RadioPlaylist.looksLikePlaylist(trimmed)) {
            _playlist.value = RadioPlaylistStatus.None
            return
        }
        _playlist.value = RadioPlaylistStatus.Reading
        playlistJob = viewModelScope.launch {
            delay(PLAYLIST_DEBOUNCE_MS)
            _playlist.value = when (val result = repository.resolvePlaylist(trimmed)) {
                is PlaylistResult.Resolved -> RadioPlaylistStatus.Replaced(trimmed, result.streamUrl)
                PlaylistResult.OnlyInsecure -> RadioPlaylistStatus.OnlyInsecure
                PlaylistResult.Failed -> RadioPlaylistStatus.Unreadable
                PlaylistResult.NotPlaylist -> RadioPlaylistStatus.None
            }
        }
    }

    override fun onCleared() {
        preview.stop()
        super.onCleared()
    }

    companion object {
        const val MIN_QUERY_CHARS = 2
        const val SEARCH_DEBOUNCE_MS = 450L
        const val PLAYLIST_DEBOUNCE_MS = 600L
    }
}
