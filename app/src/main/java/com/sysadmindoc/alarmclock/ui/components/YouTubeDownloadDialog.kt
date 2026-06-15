package com.sysadmindoc.alarmclock.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.YouTubeSearchHit
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

private enum class DownloadMode { Search, PasteUrl }

internal enum class YouTubeDialogAction {
    Preview,
    Search,
    Download,
    EngineUpdate
}

/**
 * Reusable YouTube → alarm-sound download dialog. Two modes:
 *  - **Search** (default): NewPipe-backed text search ("rooster crowing
 *    alarm" → list of short clips). Each result has a preview button that
 *    streams the lowest-bitrate audio so the user can audition before
 *    committing to the download.
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
    var hasSearched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var inFlight by remember { mutableStateOf(false) }
    var updatingEngine by remember { mutableStateOf(false) }
    var engineVersion by remember { mutableStateOf(downloader.engineVersionName()) }
    var statusMessage by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }

    // Preview machinery — the URL currently resolving (loading) or playing.
    // Owns a single MediaPlayer so a second preview tap stops the first.
    var loadingPreviewUrl by remember { mutableStateOf<String?>(null) }
    var playingPreviewUrl by remember { mutableStateOf<String?>(null) }
    val mediaPlayerHolder = remember { mutableStateOf<MediaPlayer?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()

    fun setStatus(message: String, isError: Boolean = false) {
        statusMessage = message
        statusIsError = isError
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        loadingPreviewUrl = null
        playingPreviewUrl = null
        mediaPlayerHolder.value?.let { player ->
            try { if (player.isPlaying) player.stop() } catch (_: Exception) {}
            try { player.release() } catch (_: Exception) {}
        }
        mediaPlayerHolder.value = null
    }

    fun togglePreview(hit: YouTubeSearchHit) {
        if (playingPreviewUrl == hit.videoUrl || loadingPreviewUrl == hit.videoUrl) {
            stopPreview()
            return
        }
        // Switching previews — kill any in-flight first.
        stopPreview()
        loadingPreviewUrl = hit.videoUrl
        previewJob = scope.launch {
            val resolved = downloader.getPreviewStreamUrl(hit.videoUrl)
            resolved.fold(
                onSuccess = { streamUrl ->
                    if (loadingPreviewUrl != hit.videoUrl) return@fold // stale (user tapped another)
                    try {
                        val player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setDataSource(streamUrl)
                            setOnPreparedListener {
                                if (loadingPreviewUrl != hit.videoUrl) {
                                    try { release() } catch (_: Exception) {}
                                    return@setOnPreparedListener
                                }
                                loadingPreviewUrl = null
                                playingPreviewUrl = hit.videoUrl
                                start()
                            }
                            setOnCompletionListener { stopPreview() }
                            setOnErrorListener { _, _, _ ->
                                setStatus("That preview could not play. Try another result.", isError = true)
                                stopPreview()
                                true
                            }
                            prepareAsync()
                        }
                        mediaPlayerHolder.value = player
                    } catch (_: Exception) {
                        setStatus("That preview could not start. Try another result.", isError = true)
                        stopPreview()
                    }
                },
                onFailure = { e ->
                    if (loadingPreviewUrl == hit.videoUrl) {
                        setStatus(youTubeDialogErrorMessage(e, YouTubeDialogAction.Preview), isError = true)
                    }
                    loadingPreviewUrl = null
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    AlertDialog(
        onDismissRequest = {
            if (!inFlight && !searching && !updatingEngine) {
                stopPreview()
                onDismiss()
            }
        },
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
                EngineUpdatePanel(
                    versionName = engineVersion,
                    updating = updatingEngine,
                    enabled = !inFlight && !searching,
                    onUpdate = {
                        stopPreview()
                        updatingEngine = true
                        setStatus("Updating downloader engine...")
                        scope.launch {
                            val result = downloader.updateEngine()
                            updatingEngine = false
                            result.fold(
                                onSuccess = { update ->
                                    engineVersion = update.afterVersionName ?: update.beforeVersionName
                                    setStatus(update.userMessage())
                                },
                                onFailure = { error ->
                                    setStatus(
                                        youTubeDialogErrorMessage(error, YouTubeDialogAction.EngineUpdate),
                                        isError = true
                                    )
                                }
                            )
                        }
                    }
                )

                if (statusMessage.isNotBlank()) {
                    AppFeedbackCard(
                        title = if (statusIsError) "Action needed" else "Downloader status",
                        message = statusMessage,
                        icon = if (statusIsError) Icons.Default.Warning else Icons.Default.CheckCircle,
                        color = if (statusIsError) AccentRed else MaterialTheme.colorScheme.primary
                    )
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == DownloadMode.Search,
                        onClick = {
                            stopPreview()
                            mode = DownloadMode.Search
                        },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        enabled = !inFlight && !searching && !updatingEngine,
                        label = { Text("Search YouTube") }
                    )
                    SegmentedButton(
                        selected = mode == DownloadMode.PasteUrl,
                        onClick = {
                            stopPreview()
                            mode = DownloadMode.PasteUrl
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        enabled = !inFlight && !searching && !updatingEngine,
                        label = { Text("Paste URL") }
                    )
                }

                when (mode) {
                    DownloadMode.Search -> SearchBody(
                        query = query,
                        onQueryChange = {
                            query = it
                            hasSearched = false
                        },
                        searching = searching,
                        hasSearched = hasSearched,
                        results = hits,
                        canSubmit = !inFlight && !updatingEngine,
                        controlsEnabled = !inFlight && !updatingEngine,
                        loadingPreviewUrl = loadingPreviewUrl,
                        playingPreviewUrl = playingPreviewUrl,
                        onTogglePreview = ::togglePreview,
                        onSearch = {
                            if (query.isBlank()) return@SearchBody
                            stopPreview()
                            searching = true
                            hasSearched = true
                            setStatus("")
                            hits = emptyList()
                            scope.launch {
                                val r = downloader.searchAlarmSounds(query.trim())
                                searching = false
                                r.fold(
                                    onSuccess = { found ->
                                        hits = found
                                        if (found.isEmpty()) {
                                            setStatus("")
                                        }
                                    },
                                    onFailure = { e ->
                                        setStatus(youTubeDialogErrorMessage(e, YouTubeDialogAction.Search), isError = true)
                                    }
                                )
                            }
                        },
                        onPick = { hit ->
                            stopPreview()
                            inFlight = true
                            setStatus("Downloading \"${hit.title.take(40)}\"...")
                            scope.launch {
                                val r = downloader.downloadAsAlarm(hit.videoUrl, hit.title)
                                inFlight = false
                                r.fold(
                                    onSuccess = onDownloaded,
                                    onFailure = { e ->
                                        setStatus("")
                                        onError(youTubeDialogErrorMessage(e, YouTubeDialogAction.Download))
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
                        controlsEnabled = !inFlight && !updatingEngine,
                    )
                }
            }
        },
        confirmButton = {
            if (mode == DownloadMode.PasteUrl) {
                Button(
                    enabled = !inFlight && !updatingEngine && url.isNotBlank(),
                    onClick = {
                        stopPreview()
                        inFlight = true
                        val labelGuess = name.ifBlank { url.substringAfter("v=").substringBefore('&').take(11) }
                        scope.launch {
                            val result = downloader.downloadAsAlarm(url.trim(), labelGuess)
                            inFlight = false
                            result.fold(
                                onSuccess = onDownloaded,
                                onFailure = { e ->
                                    onError(youTubeDialogErrorMessage(e, YouTubeDialogAction.Download))
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Download")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    stopPreview()
                    onDismiss()
                },
                enabled = !inFlight && !searching && !updatingEngine
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceMedium,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun EngineUpdatePanel(
    versionName: String?,
    updating: Boolean,
    enabled: Boolean,
    onUpdate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Downloader engine",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = versionName
                    ?.let { "yt-dlp $it. Update only if YouTube search or downloads stop working." }
                    ?: "Update yt-dlp only if YouTube search or downloads stop working.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(
            onClick = onUpdate,
            enabled = enabled && !updating
        ) {
            if (updating) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text("Update", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PasteBody(
    url: String,
    onUrlChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    inFlight: Boolean,
    controlsEnabled: Boolean,
) {
    val focusManager = LocalFocusManager.current
    Text(
        "Paste a YouTube URL. The audio is saved into your device's Alarms folder, so it appears wherever you pick alarm sounds.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        placeholder = { Text("https://youtube.com/watch?v=...") },
        singleLine = true,
        enabled = controlsEnabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next
        ),
        colors = appOutlinedTextFieldColors(),
        shape = AppInputShape,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("Name this sound (optional)") },
        singleLine = true,
        enabled = controlsEnabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        colors = appOutlinedTextFieldColors(),
        shape = AppInputShape,
        modifier = Modifier.fillMaxWidth()
    )
    if (inFlight) DownloadingHint()
}

@Composable
private fun SearchBody(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    hasSearched: Boolean,
    results: List<YouTubeSearchHit>,
    canSubmit: Boolean,
    controlsEnabled: Boolean,
    loadingPreviewUrl: String?,
    playingPreviewUrl: String?,
    onTogglePreview: (YouTubeSearchHit) -> Unit,
    onSearch: () -> Unit,
    onPick: (YouTubeSearchHit) -> Unit,
    inFlight: Boolean,
) {
    val canSearch = canSubmit && !searching && query.isNotBlank()
    Text(
        "Try \"rooster crow\", \"piano bell\", or \"forest birds\". Preview first, then save the result that feels right.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("rooster crow alarm") },
        singleLine = true,
        enabled = controlsEnabled && !searching,
        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
        trailingIcon = {
            TextButton(onClick = onSearch, enabled = canSearch) {
                Text("Search", fontWeight = FontWeight.SemiBold)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (canSearch) onSearch() }),
        colors = appOutlinedTextFieldColors(),
        shape = AppInputShape,
        modifier = Modifier.fillMaxWidth()
    )

    if (searching) {
        AppSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
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
                    "Searching YouTube...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
                    isLoadingPreview = loadingPreviewUrl == hit.videoUrl,
                    isPlayingPreview = playingPreviewUrl == hit.videoUrl,
                    enabled = controlsEnabled && !inFlight,
                    onTogglePreview = { onTogglePreview(hit) },
                    onPick = { onPick(hit) }
                )
            }
        }
    } else if (hasSearched && !searching && query.isNotBlank()) {
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            AppEmptyState(
                icon = Icons.Default.Search,
                title = "No matching clips",
                description = "Try a shorter phrase, a different sound name, or paste a direct URL instead.",
                footer = {
                    OutlinedButton(
                        onClick = onSearch,
                        enabled = canSubmit && query.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Search again")
                    }
                }
            )
        }
    }

    if (inFlight) DownloadingHint()
}

@Composable
private fun SearchResultRow(
    hit: YouTubeSearchHit,
    isLoadingPreview: Boolean,
    isPlayingPreview: Boolean,
    enabled: Boolean,
    onTogglePreview: () -> Unit,
    onPick: () -> Unit,
) {
    val highlight = isPlayingPreview || isLoadingPreview
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else SurfaceLight
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview button — circular, primary-tinted, with a loading
            // spinner overlay while the stream URL resolves.
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onTogglePreview,
                    enabled = enabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingPreview) "Stop preview" else "Preview sound",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (isLoadingPreview) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Title + meta — the entire row (minus the preview button) is the
            // tap target for "save this sound." Splitting tap zones makes
            // both gestures deliberate: ▶ to listen, body to save.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onPick)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = hit.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        Text("-", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = formatDuration(hit.durationSeconds),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isPlayingPreview) {
                        Text("-", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Previewing",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                AppStatusChip(
                    label = if (highlight) "Tap row to save this preview" else "Tap row to save",
                    icon = Icons.Default.CloudDownload,
                    color = if (highlight) MaterialTheme.colorScheme.primary else DismissGreen
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

internal fun youTubeDialogErrorMessage(
    error: Throwable,
    action: YouTubeDialogAction
): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("not available in this build", ignoreCase = true) ->
            "YouTube downloads are not available in this build."
        error is UnknownHostException ->
            "Check your connection and try again."
        error is SocketTimeoutException ->
            "YouTube took too long to respond. Try again later."
        error is SSLException ->
            "Could not connect securely to YouTube. Try again later."
        message.contains("HTTP 403") || message.contains("HTTP 429") ->
            "YouTube is blocking this request right now. Try updating the downloader engine or try again later."
        message.contains("HTTP", ignoreCase = true) ->
            "YouTube did not return a usable audio stream. Try another result or try again later."
        message.contains("extractor", ignoreCase = true) ||
            message.contains("signature", ignoreCase = true) ||
            message.contains("player response", ignoreCase = true) ->
            "YouTube changed how this video is served. Update the downloader engine and try again."
        message.contains("invalid", ignoreCase = true) ||
            message.contains("unsupported", ignoreCase = true) ->
            "Enter a valid YouTube link."
        error is IOException ->
            "The YouTube request could not be completed. Check your connection and try again."
        else -> when (action) {
            YouTubeDialogAction.Preview ->
                "Could not get a preview stream. Try another result."
            YouTubeDialogAction.Search ->
                "Search failed. Try a shorter phrase or try again later."
            YouTubeDialogAction.Download ->
                "Download failed. Try another link or update the downloader engine."
            YouTubeDialogAction.EngineUpdate ->
                "Update failed. Your current engine was kept. Check your connection and try again."
        }
    }
}

/**
 * v1.7.3: Faux-progress download hint.
 *
 * Real progress is hard to surface here because yt-dlp's `--get-url` resolve step
 * has no progress signal, and OkHttp byte-counting only kicks in once the
 * stream resolves. A static spinner read as "stuck" in user testing.
 *
 * The faux-progress curve is `1 - e^(-3t) * 0.92` over 30 s: fast off the
 * line, slows asymptotically toward 92% so completion (which jumps it to
 * 100% instantly) still feels like a finish, not a fast-forward. The status
 * label rotates through phases on the same timer so the user sees something
 * change every few seconds.
 */
@Composable
private fun DownloadingHint() {
    val phases = remember {
        listOf(
            "Resolving audio stream...",
            "Connecting to YouTube...",
            "Downloading audio...",
            "Almost there...",
            "Saving to your alarms..."
        )
    }
    var progress by remember { mutableStateOf(0f) }
    var phaseIndex by remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val totalMs = 30_000L
        val ticks = 60
        repeat(ticks) { i ->
            kotlinx.coroutines.delay(totalMs / ticks)
            val t = (i + 1).toFloat() / ticks
            // Asymptotic curve: leaps to ~30% in the first 4s, then crawls.
            progress = (1f - kotlin.math.exp(-3f * t)) * 0.92f
            phaseIndex = (i * phases.size / ticks).coerceIn(0, phases.lastIndex)
        }
    }

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 450),
        label = "download-progress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = phases[phaseIndex],
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = SurfaceLight
        )
        Text(
            text = "This can take 10-60 seconds depending on the clip and your connection.",
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
    var available by remember(downloader) { mutableStateOf(downloader.isAvailable()) }
    LaunchedEffect(downloader) {
        // Poll until ready or until we leave composition. yt-dlp init takes
        // a few seconds on cold start; once true it never flips back, so we
        // can stop polling.
        while (!available) {
            kotlinx.coroutines.delay(400)
            available = downloader.isAvailable()
        }
    }
    return available
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface YouTubeDialogEntryPoint {
    fun youTubeAudioDownloader(): YouTubeAudioDownloader
}
