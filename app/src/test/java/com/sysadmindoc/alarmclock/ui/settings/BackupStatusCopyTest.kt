package com.sysadmindoc.alarmclock.ui.settings

import java.io.FileNotFoundException
import java.io.IOException
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupStatusCopyTest {
    @Test
    fun successMessagesUsePlainHumanCounts() {
        assertEquals("Backup exported: 1 alarm.", backupSuccessMessage(BackupStatusKind.PlainExport, 1))
        assertEquals("Backup imported: 3 alarms.", backupSuccessMessage(BackupStatusKind.PlainImport, 3))
        assertEquals(
            "Encrypted backup exported: 2 alarms.",
            backupSuccessMessage(BackupStatusKind.EncryptedExport, 2)
        )
    }

    @Test
    fun failureMessagesAvoidRawExceptionDumping() {
        assertEquals(
            "Couldn't preview encrypted backup. Check the passphrase and choose the encrypted backup again.",
            backupFailureMessage(BackupStatusKind.EncryptedImportPreview, AEADBadTagException("mac check failed"))
        )
        assertEquals(
            "Couldn't import backup. Choose a file location this device can still access.",
            backupFailureMessage(BackupStatusKind.PlainImport, FileNotFoundException("/storage/raw/path"))
        )
        assertEquals(
            "Couldn't export backup. Check storage access and try again.",
            backupFailureMessage(BackupStatusKind.PlainExport, IOException("disk full"))
        )
    }

    @Test
    fun statusClassifierHandlesNewAndLegacyCopy() {
        assertTrue(isFailureStatusMessage("Couldn't export backup. Check storage access and try again."))
        assertTrue(isFailureStatusMessage("Export failed: old message"))
        assertFalse(isFailureStatusMessage("Backup exported: 2 alarms."))
    }
}
