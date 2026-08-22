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

/**
 * A finished backup operation, ready to show.
 *
 * [isFailure] is carried rather than derived: the classifier this replaced read
 * the message back looking for "Couldn't" and "failed", which only ever worked
 * while the copy was English.
 */
data class BackupStatusMessage(
    val text: String,
    val isFailure: Boolean
)

internal fun backupSuccessMessage(
    resources: Resources,
    kind: BackupStatusKind,
    count: Int
): BackupStatusMessage {
    val alarmCount = resources.getQuantityString(R.plurals.settings_backup_alarm_count, count, count)
    val text = when (kind) {
        BackupStatusKind.PlainExport ->
            resources.getString(R.string.settings_backup_exported, alarmCount)
        BackupStatusKind.EncryptedExport ->
            resources.getString(R.string.settings_backup_encrypted_exported, alarmCount)
        BackupStatusKind.PlainImport ->
            resources.getString(R.string.settings_backup_imported, alarmCount)
        BackupStatusKind.EncryptedImport ->
            resources.getString(R.string.settings_backup_encrypted_imported, alarmCount)
        BackupStatusKind.ImportPreview,
        BackupStatusKind.EncryptedImportPreview,
        BackupStatusKind.SupportExport ->
            resources.getString(R.string.settings_backup_all_done)
    }
    return BackupStatusMessage(text, isFailure = false)
}

internal fun backupFailureMessage(
    resources: Resources,
    kind: BackupStatusKind,
    cause: Throwable? = null
): BackupStatusMessage {
    val prefix = when (kind) {
        BackupStatusKind.PlainExport -> R.string.settings_backup_export_failed
        BackupStatusKind.EncryptedExport -> R.string.settings_backup_encrypted_export_failed
        BackupStatusKind.PlainImport -> R.string.settings_backup_import_failed
        BackupStatusKind.EncryptedImport -> R.string.settings_backup_encrypted_import_failed
        BackupStatusKind.ImportPreview -> R.string.settings_backup_preview_failed
        BackupStatusKind.EncryptedImportPreview -> R.string.settings_backup_encrypted_preview_failed
        BackupStatusKind.SupportExport -> R.string.settings_backup_support_bundle_failed
    }
    val text = resources.getString(
        R.string.settings_backup_failure_with_hint,
        resources.getString(prefix),
        resources.getString(backupRecoveryHint(cause))
    )
    return BackupStatusMessage(text, isFailure = true)
}

private fun backupRecoveryHint(cause: Throwable?): Int {
    val message = cause?.message.orEmpty()
    return when {
        cause is AEADBadTagException ||
            message.contains("passphrase", ignoreCase = true) ||
            message.contains("decrypt", ignoreCase = true) ->
            R.string.settings_backup_hint_passphrase
        cause is SecurityException -> R.string.settings_backup_hint_permission
        cause is FileNotFoundException -> R.string.settings_backup_hint_missing_file
        cause is IOException -> R.string.settings_backup_hint_storage
        message.contains("version", ignoreCase = true) -> R.string.settings_backup_hint_version
        message.contains("json", ignoreCase = true) ||
            message.contains("malformed", ignoreCase = true) ||
            message.contains("parse", ignoreCase = true) ->
            R.string.settings_backup_hint_malformed
        else -> R.string.settings_backup_hint_generic
    }
}
