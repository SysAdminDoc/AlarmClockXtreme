package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyUriNormalizerTest {

    @Test
    fun `share links become the spotify scheme`() {
        assertEquals(
            "spotify:track:6rqhFgbbKwnb9MLmUQDhG6",
            SpotifyUriNormalizer.normalize("https://open.spotify.com/track/6rqhFgbbKwnb9MLmUQDhG6")
        )
        assertEquals(
            "spotify:album:2noRn2Aes5aoNVsU6iWThc",
            SpotifyUriNormalizer.normalize("https://open.spotify.com/album/2noRn2Aes5aoNVsU6iWThc")
        )
        assertEquals(
            "spotify:playlist:6uu7E334TPSuHXcZOx6Nz5",
            SpotifyUriNormalizer.normalize("https://open.spotify.com/playlist/6uu7E334TPSuHXcZOx6Nz5")
        )
    }

    /** The exact link from issue #53, share token and all. */
    @Test
    fun `the share token and other query parameters are dropped`() {
        assertEquals(
            "spotify:playlist:6uu7E334TPSuHXcZOx6Nz5",
            SpotifyUriNormalizer.normalize(
                "https://open.spotify.com/playlist/6uu7E334TPSuHXcZOx6Nz5" +
                    "?si=900RM90jT_WmIFQCredgRg&utm_source=copy-link&pi=yRHfeljGSK-ZV"
            )
        )
    }

    @Test
    fun `scheme host case and surrounding whitespace do not matter`() {
        assertEquals(
            "spotify:track:6rqhFgbbKwnb9MLmUQDhG6",
            SpotifyUriNormalizer.normalize("  HTTPS://OPEN.SPOTIFY.COM/track/6rqhFgbbKwnb9MLmUQDhG6  ")
        )
        assertEquals(
            "spotify:track:6rqhFgbbKwnb9MLmUQDhG6",
            SpotifyUriNormalizer.normalize("http://www.open.spotify.com/track/6rqhFgbbKwnb9MLmUQDhG6")
        )
        assertEquals(
            "spotify:track:6rqhFgbbKwnb9MLmUQDhG6",
            SpotifyUriNormalizer.normalize("open.spotify.com/track/6rqhFgbbKwnb9MLmUQDhG6")
        )
    }

    @Test
    fun `locale prefixed and legacy user paths still resolve`() {
        assertEquals(
            "spotify:track:6rqhFgbbKwnb9MLmUQDhG6",
            SpotifyUriNormalizer.normalize("https://open.spotify.com/intl-de/track/6rqhFgbbKwnb9MLmUQDhG6")
        )
        assertEquals(
            "spotify:playlist:6uu7E334TPSuHXcZOx6Nz5",
            SpotifyUriNormalizer.normalize("https://open.spotify.com/user/someone/playlist/6uu7E334TPSuHXcZOx6Nz5")
        )
        assertEquals(
            "spotify:playlist:6uu7E334TPSuHXcZOx6Nz5",
            SpotifyUriNormalizer.normalize("https://play.spotify.com/playlist/6uu7E334TPSuHXcZOx6Nz5")
        )
    }

    @Test
    fun `an already canonical uri is left alone`() {
        assertEquals(
            "spotify:playlist:6uu7E334TPSuHXcZOx6Nz5",
            SpotifyUriNormalizer.normalize("spotify:playlist:6uu7E334TPSuHXcZOx6Nz5")
        )
        assertEquals(
            "spotify:user:someone:playlist:6uu7E334TPSuHXcZOx6Nz5",
            SpotifyUriNormalizer.normalize(" spotify:user:someone:playlist:6uu7E334TPSuHXcZOx6Nz5 ")
        )
    }

    /**
     * The id in a spotify.link path is opaque; only Spotify's own servers can
     * expand it. There is nothing to rewrite offline, so it is passed through
     * as https and the Spotify app follows the redirect itself.
     */
    @Test
    fun `short links are accepted and passed through`() {
        assertEquals(
            "https://spotify.link/abc123XYZ",
            SpotifyUriNormalizer.normalize("https://spotify.link/abc123XYZ")
        )
        assertNull(SpotifyUriNormalizer.normalize("https://spotify.link/"))
    }

    @Test
    fun `anything that is not spotify is rejected`() {
        assertNull(SpotifyUriNormalizer.normalize("https://example.com/playlist/6uu7E334TPSuHXcZOx6Nz5"))
        assertNull(SpotifyUriNormalizer.normalize("https://open.spotify.com.evil.test/track/abc"))
        assertNull(SpotifyUriNormalizer.normalize("https://music.youtube.com/watch?v=abc"))
        assertNull(SpotifyUriNormalizer.normalize("just some words"))
        assertNull(SpotifyUriNormalizer.normalize("https://open.spotify.com/"))
        assertNull(SpotifyUriNormalizer.normalize("https://open.spotify.com/track/"))
        assertNull(SpotifyUriNormalizer.normalize("https://open.spotify.com/nonsense/6rqhFgbbKwnb9MLmUQDhG6"))
    }

    @Test
    fun `blank is not an error`() {
        assertEquals("", SpotifyUriNormalizer.normalize(""))
        assertEquals("", SpotifyUriNormalizer.normalize("   "))
        assertTrue(SpotifyUriNormalizer.isAcceptable(""))
        assertFalse(SpotifyUriNormalizer.isAcceptable("https://example.com/x"))
    }

    @Test
    fun `sanitizing an alarm canonicalises the pasted link`() {
        val saved = Alarm(
            id = 1L,
            hour = 7,
            minute = 0,
            spotifyUri = "https://open.spotify.com/playlist/6uu7E334TPSuHXcZOx6Nz5?si=abc"
        ).sanitized()
        assertEquals("spotify:playlist:6uu7E334TPSuHXcZOx6Nz5", saved.spotifyUri)
    }

    @Test
    fun `sanitizing drops a value spotify cannot open`() {
        val saved = Alarm(
            id = 1L,
            hour = 7,
            minute = 0,
            spotifyUri = "https://example.com/not-spotify"
        ).sanitized()
        assertEquals("", saved.spotifyUri)
    }
}
