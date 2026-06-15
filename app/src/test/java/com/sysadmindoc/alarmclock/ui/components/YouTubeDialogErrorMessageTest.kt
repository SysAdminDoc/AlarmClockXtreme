package com.sysadmindoc.alarmclock.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.UnknownHostException

class YouTubeDialogErrorMessageTest {

    @Test
    fun networkErrorsUsePlainRecoveryCopy() {
        assertEquals(
            "Check your connection and try again.",
            youTubeDialogErrorMessage(
                UnknownHostException("Unable to resolve host \"youtube.com\""),
                YouTubeDialogAction.Search
            )
        )
    }

    @Test
    fun httpErrorsDoNotExposeRawStatusText() {
        val message = youTubeDialogErrorMessage(
            IllegalStateException("HTTP 403 Forbidden"),
            YouTubeDialogAction.Preview
        )

        assertEquals(
            "YouTube is blocking this request right now. Try updating the downloader engine or try again later.",
            message
        )
        assertFalse(message.contains("403"))
        assertFalse(message.contains("Forbidden"))
    }

    @Test
    fun extractorErrorsSuggestEngineUpdateWithoutRawException() {
        val message = youTubeDialogErrorMessage(
            RuntimeException("ExtractorError: player response is invalid"),
            YouTubeDialogAction.Download
        )

        assertEquals(
            "YouTube changed how this video is served. Update the downloader engine and try again.",
            message
        )
        assertFalse(message.contains("ExtractorError"))
        assertFalse(message.contains("player response"))
    }

    @Test
    fun unavailableBuildMessageIsClear() {
        assertEquals(
            "YouTube downloads are not available in this build.",
            youTubeDialogErrorMessage(
                IllegalStateException("YouTube download is not available in this build"),
                YouTubeDialogAction.Download
            )
        )
    }
}
