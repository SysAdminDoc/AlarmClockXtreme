package com.sysadmindoc.alarmclock.domain

/**
 * Reads the .pls and .m3u playlist links radio stations publish as their
 * "listen" address. Media3 plays the stream inside one, not the playlist
 * itself, so a pasted playlist link rang the fallback tone every morning.
 *
 * HLS (.m3u8) is not a playlist here: Media3's HLS module plays it directly.
 */
object RadioPlaylist {

    sealed interface Pick {
        data class Stream(val url: String) : Pick
        /** Every entry is plain http, which Android blocks at this targetSdk. */
        data object OnlyInsecure : Pick
        data object Empty : Pick
    }

    fun looksLikePlaylist(url: String): Boolean {
        val path = url.trim()
            .substringBefore('#')
            .substringBefore('?')
            .lowercase()
        return path.endsWith(".pls") || path.endsWith(".m3u")
    }

    fun entries(body: String): List<String> {
        val lines = body.removePrefix(BYTE_ORDER_MARK)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val isPls = lines.any { it.equals("[playlist]", ignoreCase = true) } ||
            lines.any { PLS_FILE_LINE.matches(it) }
        val candidates = if (isPls) {
            lines.mapNotNull { line -> PLS_FILE_LINE.matchEntire(line)?.groupValues?.get(1)?.trim() }
        } else {
            lines.filterNot { it.startsWith("#") }
        }
        return candidates.filter { candidate ->
            candidate.startsWith("https://", ignoreCase = true) ||
                candidate.startsWith("http://", ignoreCase = true)
        }
    }

    fun pick(body: String): Pick {
        val urls = entries(body)
        if (urls.isEmpty()) return Pick.Empty
        val secure = urls.firstOrNull { it.startsWith("https://", ignoreCase = true) }
            ?: return Pick.OnlyInsecure
        return Pick.Stream(secure)
    }

    private val BYTE_ORDER_MARK = Char(0xFEFF).toString()
    private val PLS_FILE_LINE = Regex("""^File\d+\s*=\s*(.+)$""", RegexOption.IGNORE_CASE)
}
