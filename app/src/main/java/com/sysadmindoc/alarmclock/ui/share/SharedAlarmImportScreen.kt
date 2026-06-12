package com.sysadmindoc.alarmclock.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.components.AppChipShape
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.util.Locale

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
                    contentDescription = "Discard shared alarm",
                    tint = TextPrimary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Review shared alarm",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Saved imports stay disabled until you turn them on.",
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
            SharedImportDetailRow(label = "Time", value = alarm.formatSharedImportTime())
            SharedImportDetailRow(label = "Repeat", value = alarm.repeatLabel)
            SharedImportDetailRow(label = "Challenge", value = alarm.challengeSummary())
            SharedImportDetailRow(label = "Sound", value = alarm.soundSummary())
            SharedImportDetailRow(label = "Status", value = "Will be saved disabled")
        }

        AppSurfaceCard(highlighted = riskyFields.isNotEmpty()) {
            Text(
                text = "Private references",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (riskyFields.isEmpty()) {
                Text(
                    text = "No contact, location, Wi-Fi, media, or challenge reference was found.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    riskyFields.forEach { label ->
                        Text(
                            text = "- $label",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = stripRiskyFields,
                        onCheckedChange = { stripRiskyFields = it },
                        modifier = Modifier.semantics {
                            contentDescription = "Strip private references before saving"
                        }
                    )
                    Text(
                        text = "Strip these references before saving",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
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
                Text("Discard")
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
                Text(if (uiState.isSaving) "Saving..." else "Save disabled")
            }
        }

        Spacer(modifier = Modifier.padding(bottom = 4.dp))
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
        challengeType.isNotBlank() && challengeType != "NONE" -> challengeType
        else -> "None"
    }
}

private fun Alarm.soundSummary(): String {
    return when {
        ringtonePool.isNotBlank() -> "Random ringtone pool"
        internetRadioUrl.isNotBlank() -> "Internet radio"
        spotifyUri.isNotBlank() -> "Spotify"
        ringtoneUri.isNotBlank() -> "Custom ringtone"
        else -> "Device default"
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
