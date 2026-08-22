package com.sysadmindoc.alarmclock.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.components.AppChipShape
import com.sysadmindoc.alarmclock.ui.components.AppFeedbackCard
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

@Composable
fun SharedAlarmImportScreen(
    alarm: Alarm,
    onCancel: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: SharedAlarmImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val riskyFields = remember(alarm) { alarm.sharedImportRiskLabels() }
    var stripRiskyFields by remember(alarm) { mutableStateOf(riskyFields.isNotEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.share_import_discard_shared_alarm),
                    tint = TextPrimary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.share_import_review_shared_alarm),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.share_import_saved_imports_stay_off_until),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AppSurfaceCard {
            Text(
                text = alarm.label.ifBlank { "Shared alarm" },
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            SharedImportDetailRow(label = stringResource(R.string.share_import_time), value = alarm.formatSharedImportTime())
            SharedImportDetailRow(label = stringResource(R.string.share_import_repeat), value = alarm.repeatLabel)
            SharedImportDetailRow(label = stringResource(R.string.share_import_challenge), value = alarm.challengeSummary())
            SharedImportDetailRow(label = stringResource(R.string.alarm_edit_sound), value = alarm.soundSummary())
            SharedImportDetailRow(label = stringResource(R.string.share_import_status), value = "Saved off until reviewed")
        }

        AppSurfaceCard(highlighted = riskyFields.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.share_import_private_references),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AppStatusChip(
                    label = if (riskyFields.isEmpty()) "Clean" else "Review",
                    icon = if (riskyFields.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Security,
                    color = if (riskyFields.isEmpty()) DismissGreen else SnoozeYellow
                )
            }
            if (riskyFields.isEmpty()) {
                Text(
                    text = stringResource(R.string.share_import_no_contact_location_wi_fi),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    riskyFields.forEach { label ->
                        AppStatusChip(
                            label = label,
                            color = SnoozeYellow
                        )
                    }
                }
                PrivateReferenceToggle(
                    checked = stripRiskyFields,
                    onCheckedChange = { stripRiskyFields = it }
                )
                Text(
                    text = stringResource(R.string.share_import_sanitizing_keeps_wake_time_repeat),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        uiState.error?.let { error ->
            AppFeedbackCard(
                title = stringResource(R.string.share_import_import_could_not_saved),
                message = error,
                icon = Icons.Default.Warning,
                color = AccentRed
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving,
                shape = AppChipShape
            ) {
                Text(stringResource(R.string.share_import_discard))
            }
            Button(
                onClick = {
                    viewModel.saveDraft(
                        alarm = alarm,
                        stripRiskyFields = stripRiskyFields,
                        onSaved = onSaved
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving,
                shape = AppChipShape
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.share_import_saving))
                } else {
                    Text(stringResource(R.string.share_import_save_without_enabling))
                }
            }
        }

        Spacer(modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun PrivateReferenceToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (checked) {
            DismissGreen.copy(alpha = 0.10f)
        } else {
            SurfaceLight.copy(alpha = 0.58f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) DismissGreen.copy(alpha = 0.28f) else BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = DismissGreen,
                    uncheckedColor = TextMuted,
                    checkmarkColor = SurfaceDark
                )
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = DismissGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = DismissGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_import_strip_private_references),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (checked) {
                        "Recommended for alarms from other people or public links."
                    } else {
                        "Keep only when you trust the sender and need these references."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SharedImportDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextMuted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.34f)
        )
        Text(
            text = value,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.66f)
        )
    }
}

private fun Alarm.formatSharedImportTime(): String {
    return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

private fun Alarm.challengeSummary(): String {
    return when {
        challengeChain.isNotBlank() -> challengeChain
            .split(",")
            .mapNotNull { it.trim().takeIf(String::isNotBlank)?.toSharedImportLabel() }
            .joinToString(" + ")
        challengeType.isNotBlank() && challengeType != "NONE" -> challengeType.toSharedImportLabel()
        else -> "None"
    }
}

private fun Alarm.soundSummary(): String {
    return when {
        ringtonePool.isNotBlank() -> "Random ringtone pool"
        internetRadioUrl.isNotBlank() -> "Internet radio"
        spotifyUri.isNotBlank() -> "Spotify"
        ringtoneUri == "silent" -> "Silent alarm"
        ringtoneUri.isNotBlank() -> "Custom ringtone"
        else -> "Device default"
    }
}

private fun String.toSharedImportLabel(): String {
    val normalized = trim()
        .replace('_', ' ')
        .lowercase(Locale.US)
    return normalized.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
    }
}

private fun Alarm.sharedImportRiskLabels(): List<String> = buildList {
    if (ringtoneUri.isNotBlank() || ringtonePool.isNotBlank()) add("Custom sound URI")
    if (spotifyUri.isNotBlank()) add("Spotify URI")
    if (internetRadioUrl.isNotBlank()) add("Internet radio URL")
    if (guardianEnabled || guardianPhone.isNotBlank()) add("Guardian Angel contact")
    if (hueEnabled) add("Philips Hue sunrise setting")
    if (nfcTagId.isNotBlank()) add("NFC tag identifier")
    if (barcodeValue.isNotBlank()) add("Barcode or QR value")
    if (photoMatchUri.isNotBlank()) add("Photo match URI")
    if (wifiDismissSsid.isNotBlank()) add("Wi-Fi SSID")
    if (locationDismissEnabled) add("Location dismiss coordinates")
    if (morningRoutine.isNotBlank()) add("Morning routine text")
    if (challengeType.uppercase(Locale.US) in referenceBackedChallenges) {
        add("Reference-backed challenge")
    }
    if (challengeChain.split(",").any { it.trim().uppercase(Locale.US) in referenceBackedChallenges }) {
        add("Challenge chain reference")
    }
}

private val referenceBackedChallenges = setOf(
    "NFC_SCAN",
    "BARCODE_SCAN",
    "PHOTO_MATCH",
    "WIFI_CONNECT"
)
