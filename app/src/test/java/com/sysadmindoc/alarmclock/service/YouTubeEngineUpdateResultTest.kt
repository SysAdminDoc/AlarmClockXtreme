package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The result carries a message id and its argument now, not a finished
 * sentence: it is a data class with no Context, and only the download dialog
 * can render it in the reader's language. These cases used to assert the
 * English, which is the assertion that keeps passing after a translation
 * breaks the feature.
 */
class YouTubeEngineUpdateResultTest {

    @Test
    fun `updated result names the refreshed engine version`() {
        val result = YouTubeEngineUpdateResult(
            state = YouTubeEngineUpdateState.Updated,
            beforeVersionName = "2024.11.16",
            afterVersionName = "2026.06.09"
        )

        assertEquals(R.string.youtube_engine_updated_to, result.userMessageRes)
    }

    @Test
    fun `an update with no version to report falls back to the plain message`() {
        val result = YouTubeEngineUpdateResult(
            state = YouTubeEngineUpdateState.Updated,
            beforeVersionName = "2024.11.16",
            afterVersionName = null
        )

        // The version is the format argument, so its absence has to pick a
        // different resource rather than leave a "%1$s" on screen.
        assertEquals(R.string.youtube_engine_updated, result.userMessageRes)
    }

    @Test
    fun `current result confirms no update was needed`() {
        val result = YouTubeEngineUpdateResult(
            state = YouTubeEngineUpdateState.AlreadyCurrent,
            beforeVersionName = "2026.06.09",
            afterVersionName = "2026.06.09"
        )

        assertEquals(R.string.youtube_engine_current, result.userMessageRes)
    }

    @Test
    fun `current result with no version reported still says so`() {
        val result = YouTubeEngineUpdateResult(
            state = YouTubeEngineUpdateState.AlreadyCurrent,
            beforeVersionName = null,
            afterVersionName = null
        )

        assertEquals(R.string.youtube_engine_already_current, result.userMessageRes)
    }
}
