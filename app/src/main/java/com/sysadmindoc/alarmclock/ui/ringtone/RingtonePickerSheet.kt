package com.sysadmindoc.alarmclock.ui.ringtone

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.*

data class RingtoneItem(
    val title: String,
    val uri: String,
    val isDefault: Boolean = false,
    val isSilent: Boolean = false
)

/**
 * Bottom sheet for selecting alarm ringtones.
 * Lists system alarm sounds + "Silent" + "Default" options.
 * Previews sound on tap with a short playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerSheet(
    currentUri: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ringtones = remember { loadRingtones(context) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    fun preview(uri: String) {
        mediaPlayer?.release()
        if (uri.isBlank() || playingUri == uri) {
            playingUri = null
            mediaPlayer = null
            return
        }

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
                    playingUri = null
                    it.release()
                }
            }
            playingUri = uri
        } catch (e: Exception) {
            playingUri = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            mediaPlayer?.release()
            onDismiss()
        },
        containerColor = SurfaceMedium,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Alarm Sound",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(ringtones) { ringtone ->
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
                        onClick = {
                            if (!ringtone.isSilent) preview(ringtone.uri)
                        },
                        onConfirm = {
                            mediaPlayer?.release()
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

@Composable
private fun RingtoneRow(
    ringtone: RingtoneItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentBlue.copy(alpha = 0.15f) else SurfaceCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/pause icon
            Icon(
                imageVector = when {
                    ringtone.isSilent -> Icons.Default.VolumeOff
                    isPlaying -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                tint = if (isSelected) AccentBlue else TextMuted,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    ringtone.title,
                    color = if (isSelected) AccentBlue else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (ringtone.isDefault) {
                    Text("Device default alarm sound", color = TextMuted, fontSize = 11.sp)
                }
            }

            // Select button
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
            } else {
                IconButton(onClick = onConfirm) {
                    Icon(Icons.Default.RadioButtonUnchecked, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun loadRingtones(context: Context): List<RingtoneItem> {
    val ringtones = mutableListOf<RingtoneItem>()

    // Default option
    ringtones.add(RingtoneItem("Default Alarm", "", isDefault = true))

    // Silent option
    ringtones.add(RingtoneItem("Silent", "", isSilent = true))

    // System alarm ringtones
    val manager = RingtoneManager(context)
    manager.setType(RingtoneManager.TYPE_ALARM)

    try {
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position).toString()
            ringtones.add(RingtoneItem(title, uri))
        }
    } catch (_: Exception) { }

    // Also include notification sounds as alternatives
    val notifManager = RingtoneManager(context)
    notifManager.setType(RingtoneManager.TYPE_NOTIFICATION)

    try {
        val cursor = notifManager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = notifManager.getRingtoneUri(cursor.position).toString()
            // Avoid duplicates
            if (ringtones.none { it.uri == uri }) {
                ringtones.add(RingtoneItem("$title (notification)", uri))
            }
        }
    } catch (_: Exception) { }

    return ringtones
}
