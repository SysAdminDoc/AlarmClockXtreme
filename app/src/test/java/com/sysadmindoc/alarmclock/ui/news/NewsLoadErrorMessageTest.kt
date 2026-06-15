package com.sysadmindoc.alarmclock.ui.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.UnknownHostException

class NewsLoadErrorMessageTest {

    @Test
    fun networkErrorsUsePlainLanguage() {
        assertEquals(
            "Check your connection and try again.",
            newsLoadErrorMessage(
                UnknownHostException("Unable to resolve host \"feeds.example.com\"")
            )
        )
    }

    @Test
    fun httpErrorsDoNotExposeRawStatusText() {
        val message = newsLoadErrorMessage(IllegalStateException("Feed returned HTTP 500"))

        assertEquals(
            "This feed source is not responding. Try another source or refresh later.",
            message
        )
        assertFalse(message.contains("HTTP"))
    }

    @Test
    fun parserErrorsDoNotExposeExceptionDetails() {
        val message = newsLoadErrorMessage(
            RuntimeException("org.xmlpull.v1.XmlPullParserException: expected START_TAG")
        )

        assertEquals(
            "This feed could not be read. Try another source or refresh later.",
            message
        )
        assertFalse(message.contains("XmlPullParserException"))
    }
}
