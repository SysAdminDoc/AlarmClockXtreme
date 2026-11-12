package com.sysadmindoc.alarmclock.domain

/**
 * Turns whatever a person pastes into the Spotify field into the form Spotify
 * actually acts on.
 *
 * The share sheet hands out `https://open.spotify.com/playlist/<id>?si=<token>`.
 * That web link opens Spotify but frequently lands on the page without
 * starting playback, which is exactly what issues #50 and #53 described. The
 * `spotify:playlist:<id>` scheme is the form that autoplays, so anything
 * recognisably Spotify is rewritten to it before the alarm fires.
 *
 * `spotify.link` short links are the one exception: the id in the path is
 * opaque and only Spotify's own servers can expand it, so there is nothing to
 * rewrite offline. They are accepted and passed through as https, which the
 * Spotify app resolves itself.
 */
object SpotifyUriNormalizer {

    /** Spotify entity types that can be opened by URI. */
    private val ENTITY_TYPES = setOf(
        "track", "album", "playlist", "artist", "show", "episode", "collection"
    )

    private val WEB_HOSTS = setOf("open.spotify.com", "play.spotify.com")
    private const val SHORT_LINK_HOST = "spotify.link"

    /** Spotify base62 ids are alphanumeric. Length varies by entity type. */
    private val ID_PATTERN = Regex("^[A-Za-z0-9]+$")

    /**
     * Returns the value that should be stored and launched, or null when
     * [raw] is not a Spotify target at all.
     *
     * A blank input is not an error, it means "no Spotify ringtone", so it
     * normalises to an empty string rather than null.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("spotify:", ignoreCase = true)) return trimmed

        val withoutScheme = trimmed
            .dropPrefix("https://")
            .dropPrefix("http://")
            .dropPrefix("www.")

        val hostEnd = withoutScheme.indexOfFirst { it == '/' || it == '?' || it == '#' }
        val host = (if (hostEnd < 0) withoutScheme else withoutScheme.take(hostEnd)).lowercase()
        val rest = if (hostEnd < 0) "" else withoutScheme.drop(hostEnd)

        if (host == SHORT_LINK_HOST) {
            val path = rest.substringBefore('?').substringBefore('#')
            if (path.trim('/').isEmpty()) return null
            return "https://$SHORT_LINK_HOST$path"
        }
        if (host !in WEB_HOSTS) return null

        val segments = rest
            .substringBefore('?')
            .substringBefore('#')
            .split('/')
            .filter { it.isNotBlank() }
        // Locale prefixes ("/intl-de/track/<id>") and the legacy
        // "/user/<name>/playlist/<id>" shape both leave the pair we want at the
        // end of the path.
        val idIndex = segments.indexOfLast { it in ENTITY_TYPES } + 1
        if (idIndex <= 0 || idIndex >= segments.size) return null
        val type = segments[idIndex - 1]
        val id = segments[idIndex]
        if (!ID_PATTERN.matches(id)) return null
        return "spotify:$type:$id"
    }

    /** True when [raw] is blank or something Spotify can open. */
    fun isAcceptable(raw: String): Boolean = normalize(raw) != null

    private fun String.dropPrefix(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) drop(prefix.length) else this
}
