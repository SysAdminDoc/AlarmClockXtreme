package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeEngineUpdateResultTest {
    @Test
    fun `updated result names the refreshed engine version`() {
        val result = YouTubeEngineUpdateResult(
            state = YouTubeEngineUpdateState.Updated,
            beforeVersionName = "2024.11.16",
            afterVersionName = "2026.06.09"
        )

        assertEquals("Downloader engine updated to 2026.06.09.", result.userMessage())
    }

    @Test
    fun `current result confirms no update was needed`() {
        val result = YouTubeEngineUpdateResult(
            state = YouTubeEngineUpdateState.AlreadyCurrent,
            beforeVersionName = "2026.06.09",
            afterVersionName = "2026.06.09"
        )

        assertEquals("Downloader engine is current (2026.06.09).", result.userMessage())
    }
}
