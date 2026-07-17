package com.sysadmindoc.alarmclock.ui.settings

import android.content.res.Resources
import com.sysadmindoc.alarmclock.R
import java.io.FileNotFoundException
import java.io.IOException
import javax.crypto.AEADBadTagException

internal enum class BackupStatusKind {
    PlainExport,
    EncryptedExport,
    PlainImport,
    EncryptedImport,
    ImportPreview,
    EncryptedImportPreview,
    SupportExport
}

internal fun backupSuccessMessage(resources: Resources, kind: BackupStatusKind, count: Int): String {
    val alarmCount = resources.getQuantityString(R.plurals.settings_backup_alarm_count, count, count)
    return when (kind) {
        BackupStatusKind.PlainExport -> "Backup exported: $alarmCount."
        BackupStatusKind.EncryptedExport -> "Encrypted backup exported: $alarmCount."
        BackupStatusKind.PlainImport -> "Backup imported: $alarmCount."
        BackupStatusKind.EncryptedImport -> "Encrypted backup imported: $alarmCount."
        BackupStatusKind.ImportPreview,
        BackupStatusKind.EncryptedImportPreview,
        BackupStatusKind.SupportExport -> "Backup complete."
    }
}

internal fun backupFailureMessage(kind: BackupStatusKind, cause: Throwable? = null): String {
    val prefix = when (kind) {
        BackupStatusKind.PlainExport -> "Couldn't export backup."
        BackupStatusKind.EncryptedExport -> "Couldn't export encrypted backup."
        BackupStatusKind.PlainImport -> "Couldn't import backup."
        BackupStatusKind.EncryptedImport -> "Couldn't import encrypted backup."
        BackupStatusKind.ImportPreview -> "Couldn't preview backup."
        BackupStatusKind.EncryptedImportPreview -> "Couldn't preview encrypted backup."
        BackupStatusKind.SupportExport -> "Couldn't create support bundle."
    }
    return "$prefix ${backupRecoveryHint(cause)}"
}

internal fun isFailureStatusMessage(message: String): Boolean {
    return message.startsWith("Couldn't", ignoreCase = true) ||
        message.contains("needs attention", ignoreCase = true) ||
        message.contains("failed", ignoreCase = true)
}

private fun backupRecoveryHint(cause: Throwable?): String {
    val message = cause?.message.orEmpty()
    return when {
        cause is AEADBadTagException ||
            message.contains("passphrase", ignoreCase = true) ||
            message.contains("decrypt", ignoreCase = true) ->
            "Check the passphrase and choose the encrypted backup again."
        cause is SecurityException ->
            "Grant file access and try again."
        cause is FileNotFoundException ->
            "Choose a file location this device can still access."
        cause is IOException ->
            "Check storage access and try again."
        message.contains("version", ignoreCase = true) ->
            "Choose a backup from a supported app version."
        message.contains("json", ignoreCase = true) ||
            message.contains("malformed", ignoreCase = true) ||
            message.contains("parse", ignoreCase = true) ->
            "Choose a valid AlarmClockXtreme backup file."
        else ->
            "Check the file or destination and try again."
    }
}
