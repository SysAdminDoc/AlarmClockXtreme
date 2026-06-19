package com.sysadmindoc.alarmclock.ui.ringtone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingtonePickerSheetTest {

    @Test
    fun enumerationWarningMentionsYouTubeOnlyWhenAvailable() {
        assertTrue(ringtoneEnumerationWarning(youTubeAvailable = true).contains("YouTube"))
        assertFalse(ringtoneEnumerationWarning(youTubeAvailable = false).contains("YouTube"))
    }
}
