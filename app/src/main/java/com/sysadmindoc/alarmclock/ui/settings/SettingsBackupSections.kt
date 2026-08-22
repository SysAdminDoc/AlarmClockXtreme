package com.sysadmindoc.alarmclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.ui.components.AppFeedbackCard
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.adaptive.shouldUseTwoPaneLayout
import com.sysadmindoc.alarmclock.data.backup.BackupExportWarning
import com.sysadmindoc.alarmclock.data.backup.BackupImportMode
import com.sysadmindoc.alarmclock.data.backup.BackupImportOptions
import com.sysadmindoc.alarmclock.data.backup.BackupImportPreview
import com.sysadmindoc.alarmclock.data.backup.FossifyImportErrorKind
import com.sysadmindoc.alarmclock.data.backup.FossifyImportException
import com.sysadmindoc.alarmclock.data.backup.FossifyImportPreview
import com.sysadmindoc.alarmclock.data.health.HealthConnectAvailability
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof
import com.sysadmindoc.alarmclock.data.support.SupportExportFile
import com.sysadmindoc.alarmclock.ui.permissions.PermissionRequestCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.worker.GuardianReadiness
import com.sysadmindoc.alarmclock.worker.GuardianSmsPath
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun BackupRestoreSection(viewModel: SettingsViewModel, is24HourFormat: Boolean) {
    val resources = LocalResources.current
    val unexpectedError = stringResource(R.string.settings_unexpected_error)
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var encryptedPassphrase by remember { mutableStateOf("") }
    var encryptedPassphraseConfirm by remember { mutableStateOf("") }
    var pendingExportWarning by remember { mutableStateOf<BackupExportWarning?>(null) }
    var pendingExportKind by remember { mutableStateOf<BackupExportKind?>(null) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }
    var pendingFossifyImport by remember { mutableStateOf<PendingFossifyImport?>(null) }
    var importEnabledAsDisabled by remember { mutableStateOf(false) }
    var importSettings by remember { mutableStateOf(true) }
    var keepIntegrations by remember { mutableStateOf(false) }
    var importPreviewBusy by remember { mutableStateOf(false) }
    val passphraseMismatch = encryptedPassphraseConfirm.isNotEmpty() &&
        encryptedPassphraseConfirm != encryptedPassphrase
    val encryptedExportEnabled = encryptedPassphrase.isNotBlank() &&
        encryptedPassphrase == encryptedPassphraseConfirm
    val encryptedImportEnabled = encryptedPassphrase.isNotBlank()
    val operationBusy = backupBusy || importPreviewBusy

    fun requestBackupImport(uri: Uri, encrypted: Boolean) {
        scope.launch {
            importPreviewBusy = true
            val passphrase = if (encrypted) encryptedPassphrase else ""
            try {
                val result = if (encrypted) {
                    viewModel.inspectEncryptedBackupImport(uri, passphrase)
                } else {
                    viewModel.inspectBackupImport(uri)
                }
                result
                    .onSuccess { preview ->
                        importEnabledAsDisabled = false
                        importSettings = preview.settingsIncluded
                        // Opt in explicitly: a file from anywhere but the user's
                        // own export would otherwise silently redirect alarm
                        // events and install a phone number to call.
                        keepIntegrations = false
                        pendingImport = PendingBackupImport(
                            uri = uri,
                            encrypted = encrypted,
                            passphrase = passphrase,
                            preview = preview
                        )
                    }
                    .onFailure { error ->
                        viewModel.showBackupResult(
                            backupFailureMessage(
                                if (encrypted) BackupStatusKind.EncryptedImportPreview else BackupStatusKind.ImportPreview,
                                error
                            )
                        )
                    }
            } catch (error: Exception) {
                viewModel.showBackupResult(
                    backupFailureMessage(
                        if (encrypted) BackupStatusKind.EncryptedImportPreview else BackupStatusKind.ImportPreview,
                        error
                    )
                )
            } finally {
                importPreviewBusy = false
            }
        }
    }

    fun confirmBackupImport(mode: BackupImportMode) {
        val pending = pendingImport ?: return
        val options = BackupImportOptions(
            mode = mode,
            importEnabledAsDisabled = importEnabledAsDisabled,
            importSettings = importSettings,
            keepIntegrationsAndContacts = importSettings && keepIntegrations
        )
        pendingImport = null
        if (pending.encrypted) {
            viewModel.importEncryptedBackup(pending.uri, pending.passphrase, options)
        } else {
            viewModel.importBackup(pending.uri, options)
        }
    }

    fun requestFossifyImport(uri: Uri) {
        scope.launch {
            importPreviewBusy = true
            try {
                viewModel.inspectFossifyImport(uri)
                    .onSuccess { preview -> pendingFossifyImport = PendingFossifyImport(uri, preview) }
                    .onFailure { error ->
                        // Fixed calm copy only — the raw exception detail stays in the log
                        // (see FossifyImportManager), never in a user-facing notice.
                        viewModel.showBackupResult(
                            resources.getString(fossifyPreviewFailureRes(error))
                        )
                    }
            } finally {
                importPreviewBusy = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { requestBackupImport(it, encrypted = false) } }

    val fossifyImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(::requestFossifyImport) }

    val encryptedExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportEncryptedBackup(it, encryptedPassphrase) } }

    val encryptedImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { requestBackupImport(it, encrypted = true) } }

    fun launchBackupExport(kind: BackupExportKind) {
        when (kind) {
            BackupExportKind.Plain -> exportLauncher.launch("alarmclock_backup.json")
            BackupExportKind.Encrypted -> encryptedExportLauncher.launch("alarmclock_backup_encrypted.json")
        }
    }

    fun requestBackupExport(kind: BackupExportKind) {
        scope.launch {
            val warning = runCatching {
                viewModel.inspectBackupExportWarning()
            }.getOrElse { error ->
                BackupExportWarning(
                    listOf(
                        resources.getString(
                            R.string.settings_backup_inspection_failed,
                            error.message ?: unexpectedError
                        )
                    )
                )
            }
            if (warning.shouldWarn) {
                pendingExportKind = kind
                pendingExportWarning = warning
            } else {
                launchBackupExport(kind)
            }
        }
    }

    SettingsGroup(
        title = stringResource(R.string.settings_backup_restore),
        description = stringResource(R.string.settings_backup_restore_description)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { requestBackupExport(BackupExportKind.Plain) },
                enabled = !operationBusy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.settings_export))
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                enabled = !operationBusy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.settings_import))
            }
        }

        Text(
            text = stringResource(R.string.settings_plain_backup_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))

        OutlinedButton(
            onClick = { fossifyImportLauncher.launch(arrayOf("application/json", "text/plain")) },
            enabled = !operationBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.settings_import_fossify))
        }
        Text(
            text = stringResource(R.string.settings_import_fossify_description),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(color = TextMuted.copy(alpha = 0.14f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_encrypted_backup),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_encrypted_backup_description),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = encryptedPassphrase,
                onValueChange = { encryptedPassphrase = it },
                label = { Text(stringResource(R.string.settings_passphrase)) },
                placeholder = { Text(stringResource(R.string.settings_passphrase_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape
            )
            OutlinedTextField(
                value = encryptedPassphraseConfirm,
                onValueChange = { encryptedPassphraseConfirm = it },
                label = { Text(stringResource(R.string.settings_confirm_passphrase)) },
                placeholder = { Text(stringResource(R.string.settings_confirm_passphrase_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = passphraseMismatch,
                supportingText = if (passphraseMismatch) {
                    {
                        Text(
                            stringResource(R.string.settings_passphrase_mismatch),
                            color = AccentRed
                        )
                    }
                } else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { requestBackupExport(BackupExportKind.Encrypted) },
                    enabled = encryptedExportEnabled && !operationBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_encrypt_export))
                }
                OutlinedButton(
                    onClick = { encryptedImportLauncher.launch(arrayOf("application/json", "*/*")) },
                    enabled = encryptedImportEnabled && !operationBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_decrypt_import))
                }
            }
        }
    }

    pendingExportWarning?.let { warning ->
        val kind = pendingExportKind ?: BackupExportKind.Plain
        BackupExportWarningDialog(
            warning = warning,
            encrypted = kind == BackupExportKind.Encrypted,
            onDismiss = {
                pendingExportWarning = null
                pendingExportKind = null
            },
            onContinue = {
                pendingExportWarning = null
                pendingExportKind = null
                launchBackupExport(kind)
            }
        )
    }

    pendingImport?.let { import ->
        BackupImportPreviewDialog(
            pendingImport = import,
            importEnabledAsDisabled = importEnabledAsDisabled,
            onImportEnabledAsDisabledChange = { importEnabledAsDisabled = it },
            importSettings = importSettings,
            onImportSettingsChange = { importSettings = it },
            keepIntegrations = keepIntegrations,
            onKeepIntegrationsChange = { keepIntegrations = it },
            onDismiss = { pendingImport = null },
            onImport = ::confirmBackupImport
        )
    }

    pendingFossifyImport?.let { pending ->
        FossifyImportPreviewDialog(
            pending = pending,
            is24HourFormat = is24HourFormat,
            onDismiss = { pendingFossifyImport = null },
            onImport = {
                pendingFossifyImport = null
                viewModel.importFossifyAlarms(pending.uri, pending.preview.fingerprint)
            }
        )
    }

    if (operationBusy) {
        AppSurfaceCard(highlighted = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(
                            if (importPreviewBusy) R.string.settings_inspecting_backup
                            else R.string.settings_backup_in_progress
                        ),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (importPreviewBusy) {
                            stringResource(R.string.settings_restore_choices_description)
                        } else {
                            stringResource(R.string.settings_backup_locked_description)
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    backupResult?.let { message ->
        val failed = isFailureStatusMessage(message)
        AppFeedbackCard(
            title = stringResource(
                if (failed) R.string.settings_backup_attention else R.string.settings_backup_complete
            ),
            message = message,
            icon = if (failed) Icons.Default.Warning else Icons.Default.Backup,
            color = if (failed) AccentRed else DismissGreen,
            onDismiss = viewModel::clearBackupResult
        )
    }
}

private enum class BackupExportKind {
    Plain,
    Encrypted
}

private data class PendingBackupImport(
    val uri: Uri,
    val encrypted: Boolean,
    val passphrase: String,
    val preview: BackupImportPreview
)

private data class PendingFossifyImport(
    val uri: Uri,
    val preview: FossifyImportPreview
)

/** Maps a sanitized Fossify inspect failure to its fixed user-facing copy. */
private fun fossifyPreviewFailureRes(error: Throwable): Int =
    when ((error as? FossifyImportException)?.kind) {
        FossifyImportErrorKind.UNREADABLE -> R.string.settings_fossify_preview_unreadable
        else -> R.string.settings_fossify_preview_not_export
    }

@Composable
private fun fossifyShortDayLabels(): Map<DayOfWeek, String> = mapOf(
    DayOfWeek.MONDAY to stringResource(R.string.alarm_edit_day_monday_short),
    DayOfWeek.TUESDAY to stringResource(R.string.alarm_edit_day_tuesday_short),
    DayOfWeek.WEDNESDAY to stringResource(R.string.alarm_edit_day_wednesday_short),
    DayOfWeek.THURSDAY to stringResource(R.string.alarm_edit_day_thursday_short),
    DayOfWeek.FRIDAY to stringResource(R.string.alarm_edit_day_friday_short),
    DayOfWeek.SATURDAY to stringResource(R.string.alarm_edit_day_saturday_short),
    DayOfWeek.SUNDAY to stringResource(R.string.alarm_edit_day_sunday_short)
)

@Composable
private fun FossifyImportPreviewDialog(
    pending: PendingFossifyImport,
    is24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    val preview = pending.preview
    val defaultAlarmLabel = stringResource(R.string.direct_boot_alarm_title)
    val dayLabels = fossifyShortDayLabels()
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(is24HourFormat, locale) {
        DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "h:mm a", locale)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restore, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_review_fossify_alarms)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        R.string.settings_fossify_preview_counts,
                        preview.alarmCount,
                        preview.invalidAlarmCount
                    ),
                    color = TextPrimary
                )
                Text(
                    stringResource(
                        R.string.settings_fossify_import_disabled_summary,
                        preview.sourceEnabledAlarmCount
                    ),
                    color = SnoozeYellow,
                    style = MaterialTheme.typography.bodySmall
                )
                if (preview.unreadableRingtoneCount > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.settings_fossify_unreadable_ringtones,
                            preview.unreadableRingtoneCount,
                            preview.unreadableRingtoneCount
                        ),
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                preview.alarms.take(5).forEach { alarm ->
                    val days = alarm.repeatDays.mapNotNull(dayLabels::get).joinToString(", ")
                    val daySummary = if (days.isBlank()) {
                        ""
                    } else {
                        stringResource(R.string.settings_fossify_days_suffix, days)
                    }
                    Text(
                        stringResource(
                            R.string.settings_fossify_alarm_summary,
                            LocalTime.of(alarm.hour, alarm.minute).format(timeFormatter),
                            alarm.label.ifBlank { defaultAlarmLabel },
                            daySummary
                        ),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (preview.alarmCount > 5) {
                    val remaining = preview.alarmCount - 5
                    Text(
                        pluralStringResource(R.plurals.settings_more_items, remaining, remaining),
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            if (preview.canImport) {
                TextButton(onClick = onImport) { Text(stringResource(R.string.settings_import_disabled)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun BackupImportPreviewDialog(
    pendingImport: PendingBackupImport,
    importEnabledAsDisabled: Boolean,
    onImportEnabledAsDisabledChange: (Boolean) -> Unit,
    importSettings: Boolean,
    onImportSettingsChange: (Boolean) -> Unit,
    keepIntegrations: Boolean,
    onKeepIntegrationsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onImport: (BackupImportMode) -> Unit
) {
    val preview = pendingImport.preview
    val unknownAppVersion = stringResource(R.string.settings_unknown)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (preview.canImport) Icons.Default.Backup else Icons.Default.Warning,
                contentDescription = null,
                tint = if (preview.canImport) MaterialTheme.colorScheme.primary else AccentRed
            )
        },
        title = { Text(stringResource(R.string.settings_review_backup)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_review_backup_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = preview.compatibilityStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview.canImport) TextPrimary else AccentRed
                )
                Text(
                    text = stringResource(
                        R.string.settings_backup_version,
                        preview.version,
                        preview.appVersion.ifBlank { unknownAppVersion }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatBackupExportedAt(preview.exportedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = stringResource(
                        R.string.settings_backup_alarm_counts,
                        preview.alarmCount,
                        preview.enabledAlarmCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (preview.invalidAlarmCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.settings_backup_invalid_rows,
                            preview.invalidAlarmCount,
                            preview.invalidAlarmCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = SnoozeYellow
                    )
                }
                Text(
                    text = if (preview.settingsIncluded) {
                        stringResource(R.string.settings_global_settings_restored)
                    } else {
                        stringResource(R.string.settings_global_settings_missing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (preview.privateDataCategories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_private_values_detected),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    preview.privateDataCategories.forEach { category ->
                        Text(
                            text = stringResource(R.string.settings_list_item, category),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                if (preview.canImport) {
                    BackupImportToggle(
                        checked = importEnabledAsDisabled,
                        onCheckedChange = onImportEnabledAsDisabledChange,
                        label = stringResource(R.string.settings_keep_restored_disabled)
                    )
                    if (preview.settingsIncluded) {
                        BackupImportToggle(
                            checked = importSettings,
                            onCheckedChange = onImportSettingsChange,
                            label = stringResource(R.string.settings_import_settings)
                        )
                        if (importSettings && preview.riskyImportValues.isNotEmpty()) {
                            BackupImportToggle(
                                checked = keepIntegrations,
                                onCheckedChange = onKeepIntegrationsChange,
                                label = stringResource(R.string.settings_keep_integrations)
                            )
                            Text(
                                text = stringResource(R.string.settings_keep_integrations_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = SnoozeYellow,
                                fontWeight = FontWeight.SemiBold
                            )
                            preview.riskyImportValues.forEach { value ->
                                Text(
                                    text = stringResource(R.string.settings_list_item, value),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_keep_integrations_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (preview.canImport) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onImport(BackupImportMode.Append) }) {
                        Text(stringResource(R.string.settings_append_alarms))
                    }
                    TextButton(onClick = { onImport(BackupImportMode.Replace) }) {
                        Text(stringResource(R.string.settings_replace_alarms))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(if (preview.canImport) R.string.cancel else R.string.settings_close)
                )
            }
        }
    )
}

/** One consent row in the import preview: switch on the left, label beside it. */
@Composable
private fun BackupImportToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = appSwitchColors()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun formatBackupExportedAt(exportedAt: Long): String {
    if (exportedAt <= 0L) return stringResource(R.string.settings_export_time_unknown)
    val locale = LocalConfiguration.current.locales[0]
    val formatted = runCatching {
        DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(exportedAt))
    }.getOrNull()
    return if (formatted == null) {
        stringResource(R.string.settings_export_time_unknown)
    } else {
        stringResource(R.string.settings_exported_at, formatted)
    }
}

@Composable
private fun BackupExportWarningDialog(
    warning: BackupExportWarning,
    encrypted: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SnoozeYellow)
        },
        title = {
            Text(
                text = stringResource(
                    if (encrypted) R.string.settings_encrypted_backup_private
                    else R.string.settings_plain_backup_private
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (encrypted) {
                        stringResource(R.string.settings_private_values_encrypted)
                    } else {
                        stringResource(R.string.settings_private_values_plain)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                warning.categories.forEach { category ->
                    Text(
                        text = stringResource(R.string.settings_list_item, category),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (encrypted) {
                        stringResource(R.string.settings_encrypted_backup_warning)
                    } else {
                        stringResource(R.string.settings_plain_backup_warning)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    stringResource(
                        if (encrypted) R.string.settings_export_encrypted_backup
                        else R.string.settings_export_plain_backup
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
