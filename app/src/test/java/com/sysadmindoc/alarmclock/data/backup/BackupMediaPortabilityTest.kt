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

    @Test
    fun `an authority that merely starts with ours is not ours`() {
        // The check used startsWith on the application id with no separator, so
        // any authority beginning with those characters was accepted. The
        // everyday case is the debug build: its own provider is
        // "<id>.debug.fileprovider", and a debug backup restored into release
        // kept a URI release cannot open, which is the silent alarm this is
        // supposed to prevent.
        listOf(
            "content://${BuildConfig.APPLICATION_ID}.evil.com/audio/1",
            "content://${BuildConfig.APPLICATION_ID}malware/audio/1",
            "android.resource://${BuildConfig.APPLICATION_ID}.other/raw/chime"
        ).forEach {
            assertFalse("$it is not this app", BackupManager.isPortableMediaUri(it))
        }
    }

    @Test
    fun `our own sub-authorities still count as ours`() {
        assertTrue(
            BackupManager.isPortableMediaUri(
                "content://${BuildConfig.APPLICATION_ID}.fileprovider/tones/x.mp3"
            )
        )
    }

    @Test
    fun `an authority that looks like the media store but is not gets dropped`() {
        listOf(
            "content://media_gallery/audio/1",
            "content://settings_backup/system/alarm_alert",
            "not-a-uri",
            "content:///audio/1"
        ).forEach {
            assertFalse("$it should not be treated as portable", BackupManager.isPortableMediaUri(it))
        }
    }

    @Test
    fun `an uppercase scheme is still recognised`() {
        assertTrue(BackupManager.isPortableMediaUri("CONTENT://MEDIA/internal/audio/media/42"))
    }
}
