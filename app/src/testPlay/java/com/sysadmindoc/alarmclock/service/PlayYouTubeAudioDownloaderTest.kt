package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.service.PlayYouTubeAudioDownloader.Companion.isLikelyYouTubeUrl
import com.sysadmindoc.alarmclock.service.PlayYouTubeAudioDownloader.Companion.sanitizeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayYouTubeAudioDownloaderTest {

    @Test
    fun urlValidatorAcceptsCanonicalForms() {
        assertTrue(isLikelyYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(isLikelyYouTubeUrl("https://youtube.com/watch?v=abc"))
        assertTrue(isLikelyYouTubeUrl("https://m.youtube.com/watch?v=abc"))
        assertTrue(isLikelyYouTubeUrl("https://music.youtube.com/watch?v=abc"))
        assertTrue(isLikelyYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(isLikelyYouTubeUrl("https://youtube.com/shorts/abc"))
        assertTrue(isLikelyYouTubeUrl("http://youtube.com/watch?v=abc"))
        assertTrue(isLikelyYouTubeUrl("https://www.youtube-nocookie.com/embed/abc"))
    }

    @Test
    fun urlValidatorRejectsObviouslyHostileInput() {
        assertFalse(isLikelyYouTubeUrl(""))
        assertFalse(isLikelyYouTubeUrl("youtube.com/watch?v=abc")) // no scheme
        assertFalse(isLikelyYouTubeUrl("javascript:alert(1)"))
        assertFalse(isLikelyYouTubeUrl("https://evil.com/watch?v=abc"))
        assertFalse(isLikelyYouTubeUrl("ftp://youtube.com/watch?v=abc"))
        assertFalse(isLikelyYouTubeUrl("https://youtube.com.evil.com/x"))
        assertFalse(isLikelyYouTubeUrl("https://youtube.com/watch?v=abc --netrc-cmd=calc"))
        assertFalse(isLikelyYouTubeUrl("https://youtube.com/watch?v=abc\n--netrc-cmd=calc"))
    }

    @Test
    fun nameSanitizerReplacesUnsafeChars() {
        assertEquals("morning-rain", sanitizeName("Morning Rain"))
        assertEquals("track-1-best-of-2024", sanitizeName("Track #1: Best of 2024!"))
    }

    @Test
    fun nameSanitizerCollapsesWhitespaceAndDrops() {
        assertEquals("a-b", sanitizeName("a   b"))
        assertEquals("a-b", sanitizeName("  a / b  "))
    }

    @Test
    fun nameSanitizerCapsLength() {
        val long = "a".repeat(200)
        assertTrue(sanitizeName(long).length <= 80)
    }

    @Test
    fun nameSanitizerLowercases() {
        assertEquals("loud-bell", sanitizeName("LOUD BELL"))
    }
}
