package com.sysadmindoc.alarmclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.YouTubeSearchHit
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

private enum class DownloadMode { Search, PasteUrl }

/**
 * Reusable YouTube → alarm-sound download dialog. Two modes:
 *  - **Search** (default): NewPipe-backed text search ("rooster crowing
 *    alarm" → list of short clips). Tap a result to download.
 *  - **Paste URL**: classic URL-paste flow.
 *
 * Mirrors the dual-input pattern in the Aura/FreeVibe app's YouTube tab.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun YouTubeDownloadDialog(
    onDismiss: () -> Unit,
    onDownloaded: (savedTitle: String) -> Unit,
    onError: (message: String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloader = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            YouTubeDialogEntryPoint::class.java
        ).youTubeAudioDownloader()
    }

    var mode by remember { mutableStateOf(DownloadMode.Search) }
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<YouTubeSearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var inFlight by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!inFlight && !searching) onDismiss() },
        icon = {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Download alarm sound",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == DownloadMode.Search,
                        onClick = { mode = DownloadMode.Search },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        enabled = !inFlight && !searching,
                        label = { Text("Search YouTube") }
                    )
                    SegmentedButton(
                        selected = mode == DownloadMode.PasteUrl,
                        onClick = { mode = DownloadMode.PasteUrl },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        enabled = !inFlight && !searching,
                        label = { Text("Paste URL") }
                    )
                }

                when (mode) {
                    DownloadMode.Search -> SearchBody(
                        query = query,
                        onQueryChange = { query = it },
                        searching = searching,
                        results = hits,
                        statusMessage = statusMessage,
                        canSubmit = !inFlight,
                        onSearch = {
                            if (query.isBlank()) return@SearchBody
                            searching = true
                            statusMessage = ""
                            hits = emptyList()
                            scope.launch {
                                val r = downloader.searchAlarmSounds(query.trim())
                                searching = false
                                r.fold(
                                    onSuccess = { found ->
                                        hits = found
                                        if (found.isEmpty()) {
                                            statusMessage = "No short clips found. Try a different search."
                                        }
                                    },
                                    onFailure = { e ->
                                        statusMessage = e.message ?: "Search failed."
                                    }
                                )
                            }
                        },
                        onPick = { hit ->
                            inFlight = true
                            statusMessage = "Downloading \"${hit.title.take(40)}\"…"
                            scope.launch {
                                val r = downloader.downloadAsAlarm(hit.videoUrl, hit.title)
                                inFlight = false
                                r.fold(
                                    onSuccess = onDownloaded,
                                    onFailure = { e ->
                                        statusMessage = ""
                                        onError(e.message ?: "Download failed.")
                                    }
                                )
                            }
                        },
                        inFlight = inFlight,
                    )

                    DownloadMode.PasteUrl -> PasteBody(
                        url = url,
                        onUrlChange = { url = it },
                        name = name,
                        onNameChange = { name = it },
                        inFlight = inFlight,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = when (mode) {
                    DownloadMode.PasteUrl -> !inFlight && url.isNotBlank()
                    DownloadMode.Search -> false  // Search mode submits via tap-on-result
                },
                onClick = {
                    inFlight = true
                    val labelGuess = name.ifBlank { url.substringAfter("v=").substringBefore('&').take(11) }
                    scope.launch {
                        val result = downloader.downloadAsAlarm(url.trim(), labelGuess)
                        inFlight = false
                        result.fold(
                            onSuccess = onDownloaded,
                            onFailure = { e -> onError(e.message ?: "Download failed.") }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (mode == DownloadMode.PasteUrl) "Download" else "Pick a result")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inFlight && !searching) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceMedium,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun PasteBody(
    url: String,
    onUrlChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    inFlight: Boolean,
) {
    Text(
        "Paste a YouTube URL — the audio is saved into your device's Alarms folder, so it shows up wherever you pick alarm sounds.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        placeholder = { Text("https://youtube.com/watch?v=...") },
        singleLine = true,
        enabled = !inFlight,
        colors = appOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("Name this sound (optional)") },
        singleLine = true,
        enabled = !inFlight,
        colors = appOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    if (inFlight) DownloadingHint()
}

@Composable
private fun SearchBody(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    results: List<YouTubeSearchHit>,
    statusMessage: String,
    canSubmit: Boolean,
    onSearch: () -> Unit,
    onPick: (YouTubeSearchHit) -> Unit,
    inFlight: Boolean,
) {
    Text(
        "Search YouTube for short clips — try \"rooster crow\", \"piano bell\", or \"forest birds\". Tap a result to save it.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("rooster crow alarm") },
        singleLine = true,
        enabled = !searching && !inFlight,
        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
        trailingIcon = {
            TextButton(onClick = onSearch, enabled = canSubmit && !searching && query.isNotBlank()) {
                Text("Search", fontWeight = FontWeight.SemiBold)
            }
        },
        colors = appOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    if (searching) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Searching YouTube…",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (statusMessage.isNotBlank()) {
        Text(
            statusMessage,
            color = if (inFlight) MaterialTheme.colorScheme.primary else AccentRed,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (results.isNotEmpty() && !searching) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.videoUrl }) { hit ->
                SearchResultRow(
                    hit = hit,
                    enabled = !inFlight,
                    onClick = { onPick(hit) }
                )
            }
        }
    }

    if (inFlight) DownloadingHint()
}

@Composable
private fun SearchResultRow(
    hit: YouTubeSearchHit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLight)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = hit.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hit.uploader.isNotBlank()) {
                    Text(
                        text = hit.uploader,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "·",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = formatDuration(hit.durationSeconds),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun DownloadingHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Downloading. This can take 10–60 seconds.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Reactive probe — re-emits true once yt-dlp finishes unpacking its native
 * binaries on first launch. Without this the card / picker entry point
 * appears only after a tab switch, because the underlying state is an
 * AtomicBoolean and Compose doesn't observe it.
 */
@Composable
fun isYouTubeDownloaderAvailable(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloader = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            YouTubeDialogEntryPoint::class.java
        ).youTubeAudioDownloader()
    }
    val state = androidx.compose.runtime.produceState(
        initialValue = downloader.isAvailable(),
        key1 = downloader
    ) {
        // Poll until ready or until we leave composition. yt-dlp init takes
        // a few seconds on cold start; once true it never flips back, so we
        // can stop polling.
        while (!value) {
            value = downloader.isAvailable()
            if (value) break
            kotlinx.coroutines.delay(400)
        }
    }
    return state.value
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface YouTubeDialogEntryPoint {
    fun youTubeAudioDownloader(): YouTubeAudioDownloader
}
