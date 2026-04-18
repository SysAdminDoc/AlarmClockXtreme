package com.sysadmindoc.alarmclock.ui.ringtone

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

data class RingtoneItem(
    val title: String,
    val uri: String,
    val isDefault: Boolean = false,
    val isSilent: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerSheet(
    currentUri: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ringtones = remember { loadRingtones(context) }
    val currentSelection = remember(currentUri, ringtones) {
        ringtones.firstOrNull { ringtone ->
            when {
                ringtone.isSilent -> currentUri == "silent"
                ringtone.isDefault -> currentUri.isBlank()
                else -> currentUri == ringtone.uri
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val filteredRingtones = remember(ringtones, searchQuery) {
        if (searchQuery.isBlank()) {
            ringtones
        } else {
            ringtones.filter { ringtone ->
                ringtoneSearchText(ringtone).contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    fun stopPreview() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingUri = null
    }

    fun preview(uri: String) {
        if (uri.isBlank()) {
            stopPreview()
            return
        }

        if (playingUri == uri) {
            stopPreview()
            return
        }

        stopPreview()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, Uri.parse(uri))
                prepare()
                isLooping = false
                start()
                setOnCompletionListener {
                    stopPreview()
                }
            }
            playingUri = uri
        } catch (_: Exception) {
            stopPreview()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        containerColor = SurfaceMedium,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppSectionTitle(
                title = "Alarm Sound",
                description = "Preview tones before applying them. Default and silent options stay available at the top."
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = currentSelection?.title ?: "Default Alarm",
                    color = MaterialTheme.colorScheme.primary
                )
                AppStatusChip(
                    label = "${filteredRingtones.size} result${if (filteredRingtones.size == 1) "" else "s"}",
                    color = DismissGreen
                )
                AppStatusChip(
                    label = "Tap a tone to preview",
                    color = TextMuted
                )
                if (playingUri != null) {
                    AppStatusChip(
                        label = "Preview playing",
                        color = AccentBlue
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                },
                placeholder = {
                    Text("Search alarm sounds", color = TextMuted)
                },
                singleLine = true,
                colors = appOutlinedTextFieldColors()
            )

            if (filteredRingtones.isEmpty()) {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    AppEmptyState(
                        icon = Icons.Default.Search,
                        title = "No matching tones",
                        description = "Nothing matches \"$searchQuery\". Try a shorter or different term."
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(
                        items = filteredRingtones,
                        key = { ringtone ->
                            when {
                                ringtone.isDefault -> "default"
                                ringtone.isSilent -> "silent"
                                else -> ringtone.uri
                            }
                        }
                    ) { ringtone ->
                        val isSelected = when {
                            ringtone.isSilent -> currentUri == "silent"
                            ringtone.isDefault -> currentUri.isBlank()
                            else -> currentUri == ringtone.uri
                        }
                        val isPlaying = playingUri == ringtone.uri

                        RingtoneRow(
                            ringtone = ringtone,
                            isSelected = isSelected,
                            isPlaying = isPlaying,
                            onPreview = {
                                if (!ringtone.isDefault && !ringtone.isSilent) {
                                    preview(ringtone.uri)
                                }
                            },
                            onConfirm = {
                                stopPreview()
                                val selectedUri = when {
                                    ringtone.isSilent -> "silent"
                                    ringtone.isDefault -> ""
                                    else -> ringtone.uri
                                }
                                onSelect(selectedUri)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RingtoneRow(
    ringtone: RingtoneItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPreview: () -> Unit,
    onConfirm: () -> Unit
) {
    val accent = when {
        ringtone.isSilent -> SnoozeYellow
        ringtone.isDefault -> DismissGreen
        isSelected || isPlaying -> MaterialTheme.colorScheme.primary
        ringtone.title.contains("(notification)", ignoreCase = true) -> AccentBlue
        else -> TextSecondary
    }
    val supportsPreview = !ringtone.isDefault && !ringtone.isSilent

    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (supportsPreview) onPreview() else onConfirm()
            },
        highlighted = isSelected || isPlaying,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        ringtone.isSilent -> Icons.AutoMirrored.Filled.VolumeOff
                        isPlaying -> Icons.Default.Stop
                        ringtone.title.contains("(notification)", ignoreCase = true) -> Icons.Default.Notifications
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = ringtone.title,
                    tint = accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = ringtone.title,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = ringtoneSubtitle(ringtone, supportsPreview),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        isSelected -> AppStatusChip(
                            label = "Selected",
                            color = MaterialTheme.colorScheme.primary
                        )
                        isPlaying -> AppStatusChip(
                            label = "Previewing",
                            color = AccentBlue
                        )
                        ringtone.isDefault -> AppStatusChip(
                            label = "Recommended",
                            color = DismissGreen
                        )
                        ringtone.isSilent -> AppStatusChip(
                            label = "Quiet mode",
                            color = SnoozeYellow
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Current alarm sound",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    TextButton(onClick = onConfirm) {
                        Text("Use", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (supportsPreview) {
                    IconButton(onClick = onPreview) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop preview" else "Preview sound",
                            tint = if (isPlaying) MaterialTheme.colorScheme.primary else TextMuted
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

private fun ringtoneSubtitle(ringtone: RingtoneItem, supportsPreview: Boolean): String = when {
    ringtone.isDefault -> "Uses your device's current default alarm sound."
    ringtone.isSilent -> "No audio. Best paired with vibration or other wake effects."
    supportsPreview -> "Tap to preview, then choose Use when it feels right."
    else -> "Ready to apply."
}

private fun ringtoneSearchText(ringtone: RingtoneItem): String = buildString {
    append(ringtone.title)
    if (ringtone.isDefault) append(" default recommended")
    if (ringtone.isSilent) append(" silent quiet")
}

private fun loadRingtones(context: Context): List<RingtoneItem> {
    val ringtones = mutableListOf<RingtoneItem>()

    ringtones += RingtoneItem("Default Alarm", "", isDefault = true)
    ringtones += RingtoneItem("Silent", "", isSilent = true)

    val alarmManager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_ALARM)
    }

    try {
        val cursor = alarmManager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = alarmManager.getRingtoneUri(cursor.position).toString()
            ringtones += RingtoneItem(title = title, uri = uri)
        }
    } catch (_: Exception) {
    }

    val notificationManager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_NOTIFICATION)
    }

    try {
        val cursor = notificationManager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = notificationManager.getRingtoneUri(cursor.position).toString()
            if (ringtones.none { it.uri == uri }) {
                ringtones += RingtoneItem(title = "$title (notification)", uri = uri)
            }
        }
    } catch (_: Exception) {
    }

    return ringtones
}
