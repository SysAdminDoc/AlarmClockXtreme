package com.sysadmindoc.alarmclock.data.backup

import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A backup names providers on the device that wrote it. Anything outside the
 * media store, the settings provider and this app's own files is a URI the
 * importing device holds no grant for: the alarm would fall silent at fire
 * time, and the file would have disclosed which apps the exporter had.
 */
class BackupMediaPortabilityTest {

    @Test
    fun `system and own-app media survive an import`() {
        listOf(
            "",
            "silent",
            "content://media/internal/audio/media/42",
            "content://media/external/audio/media/1701",
            "content://settings/system/alarm_alert",
            "android.resource://${BuildConfig.APPLICATION_ID}/raw/chime",
            "content://${BuildConfig.APPLICATION_ID}.fileprovider/tones/custom.mp3"
        ).forEach {
            assertTrue("$it should survive an import", BackupManager.isPortableMediaUri(it))
        }
    }

    @Test
    fun `another app's provider does not`() {
        listOf(
            "content://com.other.app/audio/1",
            "content://com.android.externalstorage.documents/document/primary%3Atone.mp3",
            "file:///storage/emulated/0/Music/tone.mp3",
            "android.resource://com.other.app/raw/chime"
        ).forEach {
            assertFalse("$it should not survive an import", BackupManager.isPortableMediaUri(it))
        }
    }

    @Test
    fun `an unusable ringtone imports as the device default, not as a dead URI`() {
        val imported = BackupManager.withoutUnportableMedia(
            Alarm(
                id = 0,
                hour = 7,
                minute = 0,
                ringtoneUri = "content://com.other.app/audio/1",
                photoMatchUri = "content://com.other.app/images/9",
                firingBackgroundImageEnabled = true,
                firingBackgroundImageUri = "content://com.other.app/images/12",
                ringtonePool = "content://media/internal/audio/media/42,content://com.other.app/audio/2"
            )
        )

        // Blank means "device default", which is the whole point: the alarm
        // still rings.
        assertEquals("", imported.ringtoneUri)
        assertEquals("", imported.photoMatchUri)
        assertEquals("", imported.firingBackgroundImageUri)
        // A background flag with no image behind it would render nothing.
        assertFalse(imported.firingBackgroundImageEnabled)
        // The pool keeps what this device can actually play.
        assertEquals("content://media/internal/audio/media/42", imported.ringtonePool)
    }

    @Test
    fun `media this device can open is left exactly as it was`() {
        val alarm = Alarm(
            id = 0,
            hour = 6,
            minute = 30,
            ringtoneUri = "content://media/internal/audio/media/42",
            ringtonePool = "content://media/internal/audio/media/1,content://media/internal/audio/media/2"
        )

        assertEquals(alarm, BackupManager.withoutUnportableMedia(alarm))
    }

    @Test
    fun `the preview names what it is about to drop`() {
        val warnings = BackupManager.unportableMediaWarnings(
            listOf(
                Alarm(id = 0, hour = 7, minute = 0, ringtoneUri = "content://com.other.app/audio/1"),
                Alarm(id = 0, hour = 8, minute = 0, ringtoneUri = "content://media/internal/audio/media/42")
            )
        )

        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("com.other.app"))
    }
}
