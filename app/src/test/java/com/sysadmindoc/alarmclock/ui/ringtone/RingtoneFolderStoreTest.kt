package com.sysadmindoc.alarmclock.ui.ringtone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingtoneFolderStoreTest {

    @Test
    fun `accepts audio mime types and known audio extensions`() {
        assertTrue(RingtoneFolderStore.isSupportedAudioDocument("alarm.bin", "audio/mpeg"))
        assertTrue(RingtoneFolderStore.isSupportedAudioDocument("alarm.M4A", "application/octet-stream"))
        assertTrue(RingtoneFolderStore.isSupportedAudioDocument("tone.ogg", null))
    }

    @Test
    fun `rejects folders images and unknown documents`() {
        assertFalse(RingtoneFolderStore.isSupportedAudioDocument("Pictures", "vnd.android.document/directory"))
        assertFalse(RingtoneFolderStore.isSupportedAudioDocument("cover.jpg", "image/jpeg"))
        assertFalse(RingtoneFolderStore.isSupportedAudioDocument("notes.txt", "text/plain"))
    }
}
