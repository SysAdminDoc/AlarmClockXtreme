package com.sysadmindoc.alarmclock.ui.news

import com.sysadmindoc.alarmclock.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.UnknownHostException

/**
 * The mapping only. The function returns a resource id now, so the wording
 * lives in strings.xml and what matters here is which message each failure
 * picks, and that none of them is the raw exception.
 */
class NewsLoadErrorMessageTest {

    @Test
    fun networkErrorsUsePlainLanguage() {
        assertEquals(
            R.string.news_error_no_connection,
            newsLoadErrorMessage(
                UnknownHostException("Unable to resolve host \"feeds.example.com\"")
            )
        )
    }

    @Test
    fun httpErrorsDoNotExposeRawStatusText() {
        assertEquals(
            R.string.news_error_unresponsive,
            newsLoadErrorMessage(IllegalStateException("Feed returned HTTP 500"))
        )
    }

    @Test
    fun parserErrorsDoNotExposeExceptionDetails() {
        assertEquals(
            R.string.news_error_unreadable,
            newsLoadErrorMessage(
                RuntimeException("org.xmlpull.v1.XmlPullParserException: expected START_TAG")
            )
        )
    }

    @Test
    fun anAuthWalledFeedIsCalledOutSeparatelyFromAnUnreachableOne() {
        assertEquals(
            R.string.news_error_forbidden,
            newsLoadErrorMessage(IllegalStateException("Feed returned HTTP 403"))
        )
    }
}
