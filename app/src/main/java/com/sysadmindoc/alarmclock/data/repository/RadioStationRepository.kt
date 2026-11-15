package com.sysadmindoc.alarmclock.data.repository

import com.squareup.moshi.JsonDataException
import com.sysadmindoc.alarmclock.data.remote.RadioBrowserApi
import com.sysadmindoc.alarmclock.domain.RadioPlaylist
import com.sysadmindoc.alarmclock.domain.RadioStation
import com.sysadmindoc.alarmclock.domain.RadioStationFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException

/**
 * Station search for the alarm editor's internet radio card (#65), plus the
 * playlist lookup that turns a pasted .pls/.m3u link into the stream inside.
 *
 * [mirrors] are tried in order: one directory server being down must not look
 * like "no stations". [fetchText] reads a playlist body, or returns null when
 * the server answered with an error.
 */
class RadioStationRepository(
    private val mirrors: List<RadioBrowserApi>,
    private val fetchText: suspend (url: String) -> String?
) {

    sealed interface SearchResult {
        data class Stations(val stations: List<RadioStation>) : SearchResult
        /** Every directory server failed; the list would otherwise read as "nothing found". */
        data object Unreachable : SearchResult
    }

    sealed interface PlaylistResult {
        data object NotPlaylist : PlaylistResult
        data class Resolved(val streamUrl: String) : PlaylistResult
        data object OnlyInsecure : PlaylistResult
        data object Failed : PlaylistResult
    }

    suspend fun search(query: String): SearchResult {
        val term = query.trim()
        if (term.isEmpty()) return SearchResult.Stations(emptyList())
        for (api in mirrors) {
            try {
                // Name and genre in parallel: "BBC" is a name, "jazz" is mostly a tag.
                val (byName, byTag) = coroutineScope {
                    val name = async { api.searchStations(name = term) }
                    val tag = async { api.searchStations(tag = term) }
                    name.await() to tag.await()
                }
                return SearchResult.Stations(RadioStationFilter.merge(byName, byTag))
            } catch (_: IOException) {
                // Next mirror.
            } catch (_: HttpException) {
                // Next mirror.
            } catch (_: JsonDataException) {
                // A server mid-upgrade can answer with a different shape; next mirror.
            }
        }
        return SearchResult.Unreachable
    }

    /** Best effort: a failed count changes nothing for the user. */
    suspend fun countClick(station: RadioStation) {
        if (station.id.isEmpty()) return
        for (api in mirrors) {
            try {
                api.countClick(station.id).body()?.close()
                return
            } catch (_: IOException) {
                // Next mirror.
            } catch (_: HttpException) {
                // Next mirror.
            }
        }
    }

    suspend fun resolvePlaylist(url: String): PlaylistResult {
        var current = url.trim()
        if (!RadioPlaylist.looksLikePlaylist(current)) return PlaylistResult.NotPlaylist
        // A .pls pointing at an .m3u turns up now and then; two levels covers it
        // without following a loop forever.
        repeat(MAX_PLAYLIST_DEPTH) {
            if (!current.startsWith("https://", ignoreCase = true)) return PlaylistResult.OnlyInsecure
            val body = try {
                fetchText(current)
            } catch (_: IOException) {
                null
            } ?: return PlaylistResult.Failed
            when (val pick = RadioPlaylist.pick(body)) {
                RadioPlaylist.Pick.Empty -> return PlaylistResult.Failed
                RadioPlaylist.Pick.OnlyInsecure -> return PlaylistResult.OnlyInsecure
                is RadioPlaylist.Pick.Stream -> {
                    if (!RadioPlaylist.looksLikePlaylist(pick.url)) {
                        return PlaylistResult.Resolved(pick.url)
                    }
                    current = pick.url
                }
            }
        }
        return PlaylistResult.Failed
    }

    companion object {
        const val MAX_PLAYLIST_DEPTH = 2
        /** Playlists are a few hundred bytes. Anything bigger is a stream, not a list. */
        const val MAX_PLAYLIST_BYTES = 64 * 1024L
    }
}
