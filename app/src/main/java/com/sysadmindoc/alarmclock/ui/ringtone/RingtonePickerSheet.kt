package com.sysadmindoc.alarmclock.ui.ringtone

import androidx.compose.ui.res.pluralStringResource
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.service.AlarmAudioRouting
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

data class RingtoneItem(
    val title: String,
    val uri: String,
    val isDefault: Boolean = false,
    val isSilent: Boolean = false
)

/**
 * Result of enumerating system tones. [enumerationFailed] is true when the
 * RingtoneManager cursor threw, so the UI can distinguish "this device has no
 * extra tones" from "we couldn't read the tone list" (the latter looks like a
 * broken picker if surfaced silently).
 */
data class RingtoneLoadResult(
    val items: List<RingtoneItem>,
    val enumerationFailed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerSheet(
    currentUri: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // v1.7.0: Ringtones are loaded into state instead of remember-once so the
    // list refreshes after a YouTube download finishes (the new alarm tone
    // appears under Alarms/ in MediaStore and we re-enumerate to pick it up).
    var ringtoneLoad by remember { mutableStateOf(loadRingtones(context)) }
    val ringtones = ringtoneLoad.items
    val currentSelection = remember(currentUri, ringtones) {
        ringtones.firstOrNull { ringtone ->
            when {
                ringtone.isSilent -> currentUri == "silent"
                ringtone.isDefault -> currentUri.isBlank()
                else -> currentUri == ringtone.uri
            }
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewError by remember { mutableStateOf("") }
    var showYouTubeDialog by rememberSaveable { mutableStateOf(false) }
    val savedToAlarmsTemplate = stringResource(R.string.ringtone_saved_to_alarms)
    val savedToAlarmsMessage = { title: String -> savedToAlarmsTemplate.format(title) }
    var youTubeStatus by remember { mutableStateOf("") }
    var youTubeStatusIsError by remember { mutableStateOf(false) }
    var lastDownloadedTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var folderStatus by remember { mutableStateOf("") }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }.onSuccess {
            RingtoneFolderStore.addFolder(context, uri)
            ringtoneLoad = loadRingtones(context)
            folderStatus = context.getString(R.string.ringtone_folder_added)
        }.onFailure {
            folderStatus = context.getString(R.string.ringtone_folder_lost)
        }
    }

    // The shared YouTubeDownloadDialog handles its own Hilt lookup — we just
    // need a quick "is the engine up?" probe to decide whether to show the
    // entry point on this flavor.
    val youTubeAvailable = com.sysadmindoc.alarmclock.ui.components.isYouTubeDownloaderAvailable()

    val filteredRingtones = remember(ringtones, searchQuery) {
        if (searchQuery.isBlank()) {
            ringtones
        } else {
            ringtones.filter { ringtone ->
                ringtoneSearchText(ringtone).contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    // After a successful download, find the newly-saved ringtone in the
    // refreshed list and auto-scroll the user to it. Best-effort match by
    // case-insensitive title contains.
    LaunchedEffect(lastDownloadedTitle, ringtones) {
        val target = lastDownloadedTitle ?: return@LaunchedEffect
        ringtones.firstOrNull { it.title.contains(target, ignoreCase = true) }?.let {
            // The user can now tap "Use" on that row. We don't auto-select
            // because they may want to preview it first.
        }
    }

    // Outside the visibility check on purpose: a download started here keeps
    // running after the dialog closes, and its result still has to arrive.
    com.sysadmindoc.alarmclock.ui.components.YouTubeDownloadResults(
        onDownloaded = { savedTitle ->
            showYouTubeDialog = false
            lastDownloadedTitle = savedTitle
            youTubeStatusIsError = false
            youTubeStatus = savedToAlarmsMessage(savedTitle)
            // Refresh the picker list so the new file shows up immediately.
            ringtoneLoad = loadRingtones(context)
        },
        onError = { msg ->
            lastDownloadedTitle = null
            youTubeStatusIsError = true
            youTubeStatus = msg
        }
    )

    if (showYouTubeDialog) {
        com.sysadmindoc.alarmclock.ui.components.YouTubeDownloadDialog(
            onDismiss = { showYouTubeDialog = false }
        )
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

        // prepareAsync, not prepare: a SAF or folder tone reads through a
        // content provider, and preparing it on the main thread froze the sheet
        // for as long as that took. Assigned before configuration so a throw
        // part-way through still releases the native player rather than leaking
        // one per failed tap.
        val player = MediaPlayer()
        mediaPlayer = player
        playingUri = uri
        previewError = ""
        try {
            player.setAudioAttributes(AlarmAudioRouting.alarmSonificationAttributes())
            player.setDataSource(context, Uri.parse(uri))
            player.isLooping = false
            player.setOnPreparedListener { it.start() }
            player.setOnErrorListener { _, _, _ ->
                stopPreview()
                previewError =
                    "Could not preview that tone. You can still select it for the alarm."
                true
            }
            player.setOnCompletionListener { stopPreview() }
            player.prepareAsync()
        } catch (_: Exception) {
            stopPreview()
            previewError = "Could not preview that tone. You can still select it for the alarm."
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
                title = stringResource(R.string.ringtone_alarm_sound),
                description = stringResource(R.string.ringtone_preview_tones_before_applying_them),
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { folderLauncher.launch(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.ringtone_folder), fontWeight = FontWeight.SemiBold)
                            }
                            if (youTubeAvailable) {
                                OutlinedButton(
                                    onClick = { showYouTubeDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(stringResource(R.string.ringtone_youtube), fontWeight = FontWeight.SemiBold)
                                }
                            }
                    }
                }
            )

            if (youTubeStatus.isNotBlank()) {
                AppInlineNotice(
                    title = if (youTubeStatusIsError) stringResource(R.string.ringtone_download_needs_attention) else stringResource(R.string.ringtone_sound_saved),
                    message = youTubeStatus,
                    icon = if (youTubeStatusIsError) Icons.Default.Warning else Icons.Default.CheckCircle,
                    color = if (youTubeStatusIsError) AccentRed else DismissGreen
                )
            }

            if (folderStatus.isNotBlank()) {
                AppInlineNotice(
                    title = stringResource(R.string.ringtone_folder_source),
                    message = folderStatus,
                    icon = Icons.Default.FolderOpen,
                    color = DismissGreen
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = currentSelection?.title ?: stringResource(R.string.ringtone_default_alarm),
                    color = MaterialTheme.colorScheme.primary
                )
                AppStatusChip(
                    label = pluralStringResource(
                        R.plurals.ringtone_result_count,
                        filteredRingtones.size,
                        filteredRingtones.size
                    ),
                    color = DismissGreen
                )
                AppStatusChip(
                    label = stringResource(R.string.ringtone_tap_tone_preview),
                    color = TextMuted
                )
                if (playingUri != null) {
                    AppStatusChip(
                        label = stringResource(R.string.ringtone_preview_playing),
                        color = AccentBlue
                    )
                }
                if (previewError.isNotBlank()) {
                    AppStatusChip(
                        label = stringResource(R.string.ringtone_preview_unavailable),
                        color = SnoozeYellow
                    )
                }
            }

            if (previewError.isNotBlank()) {
                AppInlineNotice(
                    title = stringResource(R.string.ringtone_preview_unavailable),
                    message = previewError,
                    icon = Icons.Default.Warning,
                    color = SnoozeYellow
                )
            }

            if (ringtoneLoad.enumerationFailed) {
                AppInlineNotice(
                    title = stringResource(R.string.ringtone_sound_list_limited),
                    message = ringtoneEnumerationWarning(youTubeAvailable),
                    icon = Icons.Default.Warning,
                    color = SnoozeYellow
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.ringtone_search), tint = TextMuted)
                },
                placeholder = {
                    Text(stringResource(R.string.ringtone_search_alarm_sounds), color = TextMuted)
                },
                singleLine = true,
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape
            )

            if (filteredRingtones.isEmpty()) {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    AppEmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.ringtone_no_matching_tones),
                        description = stringResource(R.string.ringtone_nothing_matches, searchQuery)
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
            .clickable(role = Role.Button) {
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
                    .clip(RoundedCornerShape(12.dp))
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
                    contentDescription = null,
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
                            label = stringResource(R.string.settings_selected),
                            color = MaterialTheme.colorScheme.primary
                        )
                        isPlaying -> AppStatusChip(
                            label = stringResource(R.string.youtube_previewing),
                            color = AccentBlue
                        )
                        ringtone.isDefault -> AppStatusChip(
                            label = stringResource(R.string.ringtone_recommended),
                            color = DismissGreen
                        )
                        ringtone.isSilent -> AppStatusChip(
                            label = stringResource(R.string.ringtone_quiet_mode),
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
                        contentDescription = stringResource(R.string.ringtone_current_alarm_sound),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.dashboard_use), fontWeight = FontWeight.SemiBold)
                    }
                }

                if (supportsPreview) {
                    IconButton(onClick = onPreview) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.components_stop_preview) else stringResource(R.string.components_preview_sound),
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

@Composable
private fun ringtoneSubtitle(ringtone: RingtoneItem, supportsPreview: Boolean): String = when {
    ringtone.isDefault -> stringResource(R.string.ringtone_uses_your_device_s_current_default)
    ringtone.isSilent -> stringResource(R.string.ringtone_no_audio_best_paired_with_vibration)
    supportsPreview -> stringResource(R.string.ringtone_tap_to_preview_then_choose_use)
    else -> stringResource(R.string.ringtone_ready_to_apply)
}

private fun ringtoneSearchText(ringtone: RingtoneItem): String = buildString {
    append(ringtone.title)
    if (ringtone.isDefault) append(" default recommended")
    if (ringtone.isSilent) append(" silent quiet")
}

internal fun ringtoneEnumerationWarning(youTubeAvailable: Boolean): String =
    if (youTubeAvailable) {
        "Couldn't read this device's sound list. Default and Silent are still available, and you can add a new sound from YouTube."
    } else {
        "Couldn't read this device's sound list. Default and Silent are still available."
    }

private fun loadRingtones(context: Context): RingtoneLoadResult {
    val ringtones = mutableListOf<RingtoneItem>()
    var enumerationFailed = false

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
        // The device's alarm-tone list couldn't be read — surface this so the
        // sparse "Default + Silent only" list doesn't read as a broken picker.
        enumerationFailed = true
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
                ringtones += RingtoneItem(
                    title = context.getString(R.string.ringtone_notification_item, title),
                    uri = uri
                )
            }
        }
    } catch (_: Exception) {
        enumerationFailed = true
    }

    ringtones += RingtoneFolderStore.loadItems(context)

    return RingtoneLoadResult(items = ringtones, enumerationFailed = enumerationFailed)
}

// v1.7.1: The YouTube download dialog and its Hilt entry point moved to
// ui/components/YouTubeDownloadDialog.kt so the prominent entry on the
// Alarms screen and this picker can share one implementation.
