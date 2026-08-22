package com.sysadmindoc.alarmclock.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.FileNotFoundException
import java.io.IOException
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BackupStatusCopyTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun successMessagesUsePlainHumanCounts() {
        assertEquals(
            "Backup exported: 1 alarm.",
            backupSuccessMessage(resources, BackupStatusKind.PlainExport, 1).text
        )
        assertEquals(
            "Backup imported: 3 alarms.",
            backupSuccessMessage(resources, BackupStatusKind.PlainImport, 3).text
        )
        assertEquals(
            "Encrypted backup exported: 2 alarms.",
            backupSuccessMessage(resources, BackupStatusKind.EncryptedExport, 2).text
        )
    }

    @Test
    fun failureMessagesAvoidRawExceptionDumping() {
        assertEquals(
            "Couldn’t preview encrypted backup. Check the passphrase and choose the encrypted backup again.",
            backupFailureMessage(
                resources,
                BackupStatusKind.EncryptedImportPreview,
                AEADBadTagException("mac check failed")
            ).text
        )
        assertEquals(
            "Couldn’t import backup. Choose a file location this device can still access.",
            backupFailureMessage(
                resources,
                BackupStatusKind.PlainImport,
                FileNotFoundException("/storage/raw/path")
            ).text
        )
        assertEquals(
            "Couldn’t export backup. Check storage access and try again.",
            backupFailureMessage(resources, BackupStatusKind.PlainExport, IOException("disk full")).text
        )
    }

    @Test
    fun successAndFailureAreDistinguishedWithoutReadingTheCopy() {
        // The classifier this replaced searched the finished message for
        // "Couldn't" and "failed", which stops working in any translation.
        assertTrue(
            backupFailureMessage(resources, BackupStatusKind.PlainExport, IOException("disk full")).isFailure
        )
        assertFalse(backupSuccessMessage(resources, BackupStatusKind.PlainExport, 2).isFailure)
    }
}
