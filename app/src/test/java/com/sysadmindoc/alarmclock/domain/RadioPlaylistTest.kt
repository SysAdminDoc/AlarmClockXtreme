package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #65: stations publish .pls and .m3u links as their "listen" address. Media3
 * can't play the playlist itself, so a pasted one rang the fallback tone.
 */
class RadioPlaylistTest {

    @Test
    fun `pls and m3u links are playlists, hls and plain streams are not`() {
        assertTrue(RadioPlaylist.looksLikePlaylist("https://radio.example/listen.pls"))
        assertTrue(RadioPlaylist.looksLikePlaylist("https://radio.example/LISTEN.M3U"))
        assertTrue(RadioPlaylist.looksLikePlaylist(" https://radio.example/tune.pls?sid=1#x "))
        assertFalse(RadioPlaylist.looksLikePlaylist("https://radio.example/live.m3u8"))
        assertFalse(RadioPlaylist.looksLikePlaylist("https://radio.example/stream.mp3"))
        assertFalse(RadioPlaylist.looksLikePlaylist("https://radio.example/stream?format=.pls"))
    }

    @Test
    fun `pls entries come from the FileN lines in order`() {
        val body = """
            [playlist]
            NumberOfEntries=2
            File1=https://ice1.example/stream
            Title1=Main
            Length1=-1
            File2=https://ice2.example/stream
            Version=2
        """.trimIndent()
        assertEquals(
            listOf("https://ice1.example/stream", "https://ice2.example/stream"),
            RadioPlaylist.entries(body)
        )
    }

    @Test
    fun `m3u entries skip comments and blank lines, with a byte order mark and CRLF`() {
        val body = Char(0xFEFF) + "#EXTM3U\r\n#EXTINF:-1,Station\r\n\r\nhttps://ice.example/aac\r\n"
        assertEquals(listOf("https://ice.example/aac"), RadioPlaylist.entries(body))
    }

    @Test
    fun `the first https entry wins over an earlier http one`() {
        val body = "[playlist]\nFile1=http://ice.example/mp3\nFile2=https://ice.example/mp3\n"
        assertEquals(RadioPlaylist.Pick.Stream("https://ice.example/mp3"), RadioPlaylist.pick(body))
    }

    @Test
    fun `a playlist of only http streams says so instead of looking empty`() {
        assertEquals(RadioPlaylist.Pick.OnlyInsecure, RadioPlaylist.pick("http://ice.example/a\nhttp://ice.example/b\n"))
    }

    @Test
    fun `an html error page or junk is empty`() {
        assertEquals(RadioPlaylist.Pick.Empty, RadioPlaylist.pick("<html><body>404</body></html>"))
        assertEquals(RadioPlaylist.Pick.Empty, RadioPlaylist.pick(""))
    }
}
