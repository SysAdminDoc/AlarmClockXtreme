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
internal fun IntegrationsSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = stringResource(R.string.settings_webhook_integrations),
        description = stringResource(R.string.settings_webhook_description)
    ) {
        SettingsToggle(
            label = stringResource(R.string.settings_enable_webhook),
            checked = state.settings.webhookEnabled,
            supportingText = stringResource(R.string.settings_enable_webhook_description),
            onToggle = viewModel::toggleWebhook
        )

        BufferedSettingsTextField(
            value = state.settings.webhookUrl,
            onCommit = viewModel::updateWebhookUrl,
            label = { Text(stringResource(R.string.settings_webhook_url)) },
            placeholder = { Text(stringResource(R.string.settings_webhook_url_placeholder)) },
            enabled = state.settings.webhookEnabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SettingsToggle(
            label = stringResource(R.string.settings_webhook_include_labels),
            checked = state.settings.webhookIncludeLabel,
            supportingText = stringResource(R.string.settings_webhook_labels_description),
            enabled = state.settings.webhookEnabled,
            onToggle = viewModel::toggleWebhookLabelSharing
        )

        BufferedSettingsTextField(
            value = state.settings.webhookSigningSecret,
            onCommit = viewModel::updateWebhookSigningSecret,
            label = { Text(stringResource(R.string.settings_signing_secret)) },
            placeholder = { Text(stringResource(R.string.settings_signing_secret_placeholder)) },
            enabled = state.settings.webhookEnabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Warn if the user pasted a plain-http endpoint. ACX intentionally
        // keeps app-wide cleartext traffic disabled, so these endpoints cannot
        // be treated as reliable on current Android.
        val urlLower = state.settings.webhookUrl.trim().lowercase()
        val plainHttpWarning = state.settings.webhookEnabled &&
                urlLower.startsWith("http://")
        val localWebhookPermissionMissing = state.settings.webhookEnabled &&
            LocalNetworkPermission.isRuntimeRequired() &&
            LocalNetworkPermission.isLikelyLocalEndpoint(state.settings.webhookUrl) &&
            !state.hasLocalNetworkPermission

        if (plainHttpWarning) {
            AppInlineNotice(
                title = stringResource(R.string.settings_webhook_blocked),
                message = stringResource(R.string.settings_webhook_blocked_description),
                icon = Icons.Default.Warning,
                color = AccentRed
            )
        }
        if (localWebhookPermissionMissing) {
            AppInlineNotice(
                title = stringResource(R.string.settings_local_network_needed),
                message = stringResource(R.string.settings_webhook_network_description),
                icon = Icons.Default.Link,
                color = SnoozeYellow
            )
        }

        val lastDeliveryStatus = formatWebhookDeliveryStatus(state.settings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.webhookTestResult
                    ?: lastDeliveryStatus
                    ?: stringResource(R.string.settings_webhook_payload_description),
                color = when {
                    state.isWebhookTesting -> MaterialTheme.colorScheme.primary
                    state.webhookTestResult?.contains("OK") == true -> DismissGreen
                    state.webhookTestResult == null && lastDeliveryStatus?.contains("OK") == true -> DismissGreen
                    state.webhookTestResult == null && lastDeliveryStatus != null -> AccentRed
                    state.webhookTestResult != null -> AccentRed
                    else -> TextMuted
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(
                onClick = viewModel::testWebhook,
                enabled = state.settings.webhookEnabled &&
                    state.settings.webhookUrl.isNotBlank() &&
                    !localWebhookPermissionMissing &&
                    !state.isWebhookTesting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isWebhookTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    stringResource(
                        if (state.isWebhookTesting) R.string.settings_testing else R.string.settings_test
                    )
                )
            }
        }

        val deliveryLog = state.settings.webhookDeliveryLog
        if (deliveryLog.isNotBlank()) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.settings_recent_deliveries),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.size(4.dp))
            deliveryLog.lineSequence().filter { it.isNotBlank() }.take(8).forEach { line ->
                Text(
                    text = formatWebhookLogLine(line),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWebhookLogLineSuccess(line)) DismissGreen else AccentRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (state.settings.webhookEnabled) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.settings_recent_deliveries),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.settings_webhook_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Delivery-log lines are stored by WebhookService as
 * "<ISO instant> <event wire name> <OK|failed>[ (code)][: Reason]", so the
 * third whitespace token is the structured status — never key success off a
 * substring match (a failure reason could legitimately contain "OK").
 */
private fun isWebhookLogLineSuccess(line: String): Boolean =
    line.split(' ').getOrNull(2) == "OK"

/**
 * Render a stored "<ISO instant> <status>" delivery-log line as a friendly
 * local time. Falls back to the raw line if the leading token isn't an instant.
 */
@Composable
private fun formatWebhookLogLine(line: String): String {
    val spaceIdx = line.indexOf(' ')
    if (spaceIdx <= 0) return line
    val locale = LocalConfiguration.current.locales[0]
    val instantPart = line.substring(0, spaceIdx)
    val rest = line.substring(spaceIdx + 1)
    val local = runCatching {
        val local = Instant.parse(instantPart)
            .atZone(ZoneId.systemDefault())
            .format(
                DateTimeFormatter.ofLocalizedDateTime(
                    java.time.format.FormatStyle.MEDIUM,
                    java.time.format.FormatStyle.SHORT
                ).withLocale(locale)
            )
        local
    }.getOrNull() ?: return line
    return stringResource(R.string.settings_webhook_log_line, local, rest)
}

@Composable
internal fun HolidaysSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsGroup(
        title = stringResource(R.string.settings_public_holidays),
        description = stringResource(R.string.settings_public_holidays_description)
    ) {
        SettingsToggle(
            label = stringResource(R.string.settings_skip_holidays),
            checked = state.settings.holidayAutoSkipEnabled,
            supportingText = stringResource(R.string.settings_skip_holidays_description),
            onToggle = viewModel::toggleHolidayAutoSkip
        )
        BufferedSettingsTextField(
            value = state.settings.holidayCountryCode,
            onCommit = viewModel::updateHolidayCountryCode,
            transformInput = { newValue ->
                newValue
                    .filter(Char::isLetter)
                    .uppercase(Locale.US)
                    .take(2)
            },
            label = { Text(stringResource(R.string.settings_country_code)) },
            placeholder = { Text(stringResource(R.string.settings_country_code_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            )
        )
        Text(
            text = if (state.settings.holidayAutoSkipEnabled) {
                stringResource(R.string.settings_holiday_enabled_description)
            } else {
                stringResource(R.string.settings_holiday_disabled_description)
            },
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun PhilipsHueSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showForgetCertificateDialog by remember { mutableStateOf(false) }
    val localNetworkPermissionMissing = LocalNetworkPermission.isRuntimeRequired() &&
        state.settings.hueBridgeIp.isNotBlank() &&
        !state.hasLocalNetworkPermission
    SettingsGroup(
        title = stringResource(R.string.settings_hue_sunrise),
        description = stringResource(R.string.settings_hue_sunrise_description)
    ) {
        BufferedSettingsTextField(
            value = state.settings.hueBridgeIp,
            onCommit = viewModel::updateHueBridgeIp,
            label = { Text(stringResource(R.string.settings_hue_ip)) },
            placeholder = { Text(stringResource(R.string.settings_hue_ip_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        BufferedSettingsTextField(
            value = state.settings.hueApiKey,
            onCommit = viewModel::updateHueApiKey,
            label = { Text(stringResource(R.string.settings_hue_api_key)) },
            placeholder = { Text(stringResource(R.string.settings_hue_api_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        BufferedSettingsTextField(
            value = state.settings.hueLightIds,
            onCommit = viewModel::updateHueLightIds,
            label = { Text(stringResource(R.string.settings_hue_light_ids)) },
            placeholder = { Text(stringResource(R.string.settings_hue_light_ids_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SettingsToggle(
            label = stringResource(R.string.settings_hue_legacy),
            checked = state.settings.hueLegacyHttpEnabled,
            supportingText = stringResource(R.string.settings_hue_legacy_description),
            onToggle = viewModel::toggleHueLegacyHttp
        )
        if (state.settings.hueBridgeCertFingerprint.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_hue_cert_pinned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = state.settings.hueBridgeCertFingerprint.take(16) + "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                TextButton(onClick = { showForgetCertificateDialog = true }) {
                    Text(stringResource(R.string.settings_forget))
                }
            }
        }
        if (localNetworkPermissionMissing) {
            AppInlineNotice(
                title = stringResource(R.string.settings_local_network_needed),
                message = stringResource(R.string.settings_hue_network_description),
                icon = Icons.Default.Link,
                color = SnoozeYellow
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.hueTestResult ?: stringResource(R.string.settings_hue_test_description),
                color = when {
                    state.isHueTesting -> MaterialTheme.colorScheme.primary
                    state.hueTestResult?.contains("reachable") == true -> DismissGreen
                    state.hueTestResult != null -> AccentRed
                    else -> TextMuted
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(
                onClick = viewModel::testHue,
                enabled = state.settings.hueBridgeIp.isNotBlank() &&
                    state.settings.hueApiKey.isNotBlank() &&
                    !localNetworkPermissionMissing &&
                    !state.isHueTesting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isHueTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    stringResource(
                        if (state.isHueTesting) R.string.settings_testing else R.string.settings_test
                    )
                )
            }
        }
    }
    if (showForgetCertificateDialog) {
        AlertDialog(
            onDismissRequest = { showForgetCertificateDialog = false },
            title = { Text(stringResource(R.string.settings_hue_forget_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_hue_forget_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgetCertificateDialog = false
                        viewModel.clearHueCertificatePin()
                    }
                ) {
                    Text(stringResource(R.string.settings_hue_forget_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetCertificateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * v1.13.2 (roadmap X1): Play builds request only Health Connect READ_SLEEP and
 * read recent sleep-session summaries for foreground Bedtime/Stats surfaces.
 * F-Droid keeps the preference for backup compatibility without shipping the
 * SDK or permission request path.
 */
@Composable
internal fun HealthConnectSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onRequestPermissions: (() -> Unit)?
) {
    val isPlayFlavor = com.sysadmindoc.alarmclock.BuildConfig.FLAVOR == "play"
    val summary = state.healthConnectSleepSummary
    AppSurfaceCard {
        AppSectionTitle(
            title = stringResource(R.string.settings_health_connect),
            description = if (isPlayFlavor) {
                healthConnectDescription(state.settings.healthConnectEnabled, summary)
            } else {
                stringResource(R.string.settings_health_fdroid_description)
            }
        )
        SettingsToggle(
            label = stringResource(R.string.settings_health_read_sleep),
            checked = state.settings.healthConnectEnabled,
            supportingText = if (isPlayFlavor) {
                stringResource(R.string.settings_health_read_sleep_description)
            } else {
                stringResource(R.string.settings_health_fdroid_permission)
            },
            onToggle = { enabled ->
                if (enabled && isPlayFlavor && !summary.permissionGranted && onRequestPermissions != null) {
                    onRequestPermissions()
                } else {
                    viewModel.updateHealthConnectEnabled(enabled)
                }
            }
        )
        if (isPlayFlavor) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppStatusChip(
                    label = when (summary.availability) {
                        HealthConnectAvailability.AVAILABLE -> stringResource(R.string.settings_health_sdk_available)
                        HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> stringResource(R.string.settings_health_update_needed)
                        HealthConnectAvailability.UNAVAILABLE -> stringResource(R.string.settings_health_unavailable)
                        HealthConnectAvailability.NOT_INCLUDED -> stringResource(R.string.settings_health_not_included)
                    },
                    icon = if (summary.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (summary.isAvailable) DismissGreen else SnoozeYellow
                )
                AppStatusChip(
                    label = stringResource(
                        if (summary.permissionGranted) R.string.settings_health_permission_granted
                        else R.string.settings_health_permission_needed
                    ),
                    icon = if (summary.permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    color = if (summary.permissionGranted) DismissGreen else SnoozeYellow
                )
            }
            if (summary.permissionGranted) {
                Text(
                    text = if (summary.hasRecentSession) {
                        stringResource(
                            R.string.settings_health_last_session,
                            formatSleepMinutes(summary.lastSessionDurationMinutes),
                            summary.sessionsRead
                        )
                    } else {
                        stringResource(R.string.settings_health_no_recent_sessions)
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            summary.errorMessage?.let { error ->
                AppInlineNotice(
                    title = stringResource(R.string.settings_health_attention),
                    message = error,
                    icon = Icons.Default.Warning,
                    color = SnoozeYellow
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onRequestPermissions?.invoke() },
                    enabled = onRequestPermissions != null && summary.isAvailable,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        stringResource(
                            if (summary.permissionGranted) R.string.settings_health_review_access
                            else R.string.settings_health_grant_access
                        )
                    )
                }
                OutlinedButton(
                    onClick = viewModel::refreshHealthConnectSleep,
                    enabled = summary.isAvailable,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Bedtime, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_refresh))
                }
            }
        }
    }
}

@Composable
private fun healthConnectDescription(
    enabled: Boolean,
    summary: HealthConnectSleepSummary
): String = when {
    summary.availability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
        stringResource(R.string.settings_health_update_description)
    summary.availability == HealthConnectAvailability.UNAVAILABLE ->
        stringResource(R.string.settings_health_unavailable_description)
    !enabled ->
        stringResource(R.string.settings_health_opt_in_description)
    !summary.permissionGranted ->
        stringResource(R.string.settings_health_grant_description)
    summary.hasRecentSession ->
        stringResource(R.string.settings_health_available_description)
    else ->
        stringResource(R.string.settings_health_empty_description)
}

@Composable
private fun formatSleepMinutes(minutes: Long?): String {
    val value = minutes ?: return stringResource(R.string.settings_health_no_session)
    val hours = value / 60
    val mins = value % 60
    return when {
        hours > 0 && mins > 0 -> stringResource(R.string.settings_hours_minutes_short, hours, mins)
        hours > 0 -> stringResource(R.string.settings_hours_short, hours)
        else -> stringResource(R.string.settings_minutes_compact, mins)
    }
}

/**
 * v1.2.0 personalization controls. Until this audit pass these settings
 * (`accentColor`, `showMotivationalQuotes`, `adaptiveDifficultyEnabled`,
 * `customTypingPhrases`) lived in DataStore + the backup payload but had no
 * UI surface — users couldn't change them.
 */
@Composable
internal fun ConnectionsSection(state: SettingsUiState) {
    data class ConnectionInfo(
        val name: String,
        val enabled: Boolean,
        val domain: String,
        val dataSent: String,
        val offlineFallback: String
    )

    val connections = buildList {
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_weather),
            enabled = state.settings.showWeatherOnDashboard,
            domain = "api.open-meteo.com",
            dataSent = stringResource(R.string.settings_connection_location_data),
            offlineFallback = stringResource(R.string.settings_connection_cached_forecast)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_air_quality),
            enabled = state.settings.showWeatherOnDashboard,
            domain = "air-quality-api.open-meteo.com",
            dataSent = stringResource(R.string.settings_connection_location_data),
            offlineFallback = stringResource(R.string.settings_connection_hidden_unavailable)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_nws),
            enabled = state.settings.showWeatherOnDashboard,
            domain = "api.weather.gov",
            dataSent = stringResource(R.string.settings_connection_us_location_data),
            offlineFallback = stringResource(R.string.settings_connection_no_alerts)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_public_holidays),
            enabled = state.settings.holidayAutoSkipEnabled,
            domain = "date.nager.at",
            dataSent = stringResource(R.string.settings_country_code),
            offlineFallback = stringResource(R.string.settings_connection_cached_holidays)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_radar),
            enabled = state.settings.showRadarEmbed,
            domain = "embed.windy.com",
            dataSent = stringResource(R.string.settings_connection_embed_location),
            offlineFallback = stringResource(R.string.settings_connection_radar_hidden)
        ))
        add(ConnectionInfo(
            name = stringResource(R.string.settings_connection_news),
            enabled = state.settings.showNewsTab,
            domain = state.settings.newsFeedUrl
                .removePrefix("https://").removePrefix("http://")
                .substringBefore("/").ifBlank { stringResource(R.string.settings_connection_user_configured) },
            dataSent = stringResource(R.string.settings_connection_feed_data),
            offlineFallback = stringResource(R.string.settings_connection_empty_feed)
        ))
        if (state.settings.webhookEnabled) {
            add(ConnectionInfo(
                name = stringResource(R.string.settings_connection_webhook),
                enabled = true,
                domain = state.settings.webhookUrl
                    .removePrefix("https://").removePrefix("http://")
                    .substringBefore("/").ifBlank { stringResource(R.string.settings_connection_not_configured) },
                dataSent = stringResource(R.string.settings_connection_webhook_data),
                offlineFallback = stringResource(R.string.settings_connection_events_dropped)
            ))
        }
        if (state.settings.hueBridgeIp.isNotBlank()) {
            add(ConnectionInfo(
                name = stringResource(R.string.settings_connection_hue),
                enabled = true,
                domain = stringResource(R.string.settings_connection_lan_domain, state.settings.hueBridgeIp),
                dataSent = stringResource(R.string.settings_connection_hue_data),
                offlineFallback = stringResource(R.string.settings_connection_sunrise_skipped)
            ))
        }
        add(ConnectionInfo(
            name = stringResource(R.string.settings_health_connect),
            enabled = state.settings.healthConnectEnabled,
            domain = stringResource(R.string.settings_connection_on_device),
            dataSent = stringResource(R.string.settings_connection_health_data),
            offlineFallback = stringResource(R.string.settings_connection_always_local)
        ))
    }

    SettingsGroup(
        title = stringResource(R.string.settings_connections_data),
        description = stringResource(R.string.settings_connections_data_description)
    ) {
        connections.forEach { conn ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (conn.enabled) SurfaceLight.copy(alpha = 0.58f)
                    else SurfaceLight.copy(alpha = 0.28f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conn.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (conn.enabled) TextPrimary else TextMuted
                        )
                        AppStatusChip(
                            label = stringResource(
                                if (conn.enabled) R.string.settings_active else R.string.settings_off
                            ),
                            color = if (conn.enabled) DismissGreen else TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conn.domain,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.settings_connection_sends, conn.dataSent),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = stringResource(R.string.settings_connection_offline, conn.offlineFallback),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
