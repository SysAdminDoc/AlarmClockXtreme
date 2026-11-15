package com.sysadmindoc.alarmclock.ui.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.RadioStation
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppSheetDragHandle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppTextButton
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

/**
 * Station search for the alarm's internet radio (#65). Every row is an https
 * stream the directory checked recently, so anything chosen here can ring.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RadioStationSearchSheet(
    viewModel: RadioStationViewModel,
    currentUrl: String,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    StopRadioPreviewWhenHidden(viewModel)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceMedium,
        dragHandle = { AppSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppSectionTitle(
                title = stringResource(R.string.radio_find_station),
                description = stringResource(R.string.radio_search_description)
            )

            OutlinedTextField(
                value = search.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                placeholder = {
                    Text(stringResource(R.string.radio_search_placeholder), color = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    viewModel.searchNow()
                }),
                colors = appOutlinedTextFieldColors(),
                shape = AppInputShape
            )

            val previewFailed = previewState as? RadioPreview.State.Failed
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (search.searching) {
                    AppStatusChip(label = stringResource(R.string.radio_searching), color = AccentBlue)
                } else if (search.searchedQuery != null && !search.unreachable) {
                    AppStatusChip(
                        label = pluralStringResource(
                            R.plurals.ringtone_result_count,
                            search.results.size,
                            search.results.size
                        ),
                        color = DismissGreen
                    )
                }
                if (previewState is RadioPreview.State.Playing) {
                    AppStatusChip(label = stringResource(R.string.ringtone_preview_playing), color = AccentBlue)
                }
            }

            if (previewFailed != null) {
                AppInlineNotice(
                    title = stringResource(R.string.radio_preview_failed_title),
                    message = stringResource(R.string.radio_preview_failed_body),
                    icon = Icons.Default.Warning,
                    color = SnoozeYellow
                )
            }

            when {
                search.unreachable && !search.searching -> {
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        AppEmptyState(
                            icon = Icons.Default.CloudOff,
                            title = stringResource(R.string.radio_unreachable_title),
                            description = stringResource(R.string.radio_unreachable_body),
                            accent = SnoozeYellow,
                            footer = {
                                AppTextButton(onClick = viewModel::searchNow) {
                                    Text(stringResource(R.string.dashboard_retry), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }
                }
                search.searchedQuery == null && !search.searching -> {
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        AppEmptyState(
                            icon = Icons.Default.Radio,
                            title = stringResource(R.string.radio_search_hint_title),
                            description = stringResource(R.string.radio_search_hint_body)
                        )
                    }
                }
                search.results.isEmpty() && search.searching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                search.results.isEmpty() -> {
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        AppEmptyState(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.radio_no_stations_title),
                            description = stringResource(
                                R.string.radio_no_stations_body,
                                search.searchedQuery.orEmpty()
                            )
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(items = search.results, key = { it.id.ifEmpty { it.streamUrl } }) { station ->
                            val rowUrl = previewState.streamUrl
                            StationRow(
                                station = station,
                                isSelected = station.streamUrl == currentUrl.trim(),
                                isLoading = previewState is RadioPreview.State.Loading && rowUrl == station.streamUrl,
                                isPlaying = previewState is RadioPreview.State.Playing && rowUrl == station.streamUrl,
                                previewFailed = previewFailed?.url == station.streamUrl,
                                onPreview = { viewModel.togglePreview(station.streamUrl) },
                                onChoose = {
                                    onChoose(viewModel.choose(station))
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.radio_attribution),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

/**
 * A live stream never ends on its own, so the preview stops when the caller
 * leaves composition or the app leaves the screen, not only on an explicit Stop.
 */
@Composable
fun StopRadioPreviewWhenHidden(viewModel: RadioStationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stopPreview()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPreview()
        }
    }
}

@Composable
private fun StationRow(
    station: RadioStation,
    isSelected: Boolean,
    isLoading: Boolean,
    isPlaying: Boolean,
    previewFailed: Boolean,
    onPreview: () -> Unit,
    onChoose: () -> Unit
) {
    val accent = when {
        previewFailed -> SnoozeYellow
        isSelected || isPlaying -> MaterialTheme.colorScheme.primary
        else -> TextSecondary
    }
    AppSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        highlighted = isSelected || isPlaying,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onPreview
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                } else {
                    Icon(
                        imageVector = when {
                            previewFailed -> Icons.Default.Warning
                            isPlaying -> Icons.Default.Stop
                            else -> Icons.Default.Radio
                        },
                        contentDescription = null,
                        tint = accent
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = station.name,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                stationDetails(station)?.let { details ->
                    Text(
                        text = details,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    isSelected -> AppStatusChip(
                        label = stringResource(R.string.settings_selected),
                        color = MaterialTheme.colorScheme.primary
                    )
                    previewFailed -> AppStatusChip(
                        label = stringResource(R.string.radio_preview_failed_chip),
                        color = SnoozeYellow
                    )
                    isPlaying -> AppStatusChip(
                        label = stringResource(R.string.youtube_previewing),
                        color = AccentBlue
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreview) {
                    val active = isPlaying || isLoading
                    Icon(
                        imageVector = if (active) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (active) {
                            stringResource(R.string.components_stop_preview)
                        } else {
                            stringResource(R.string.components_preview_sound)
                        },
                        tint = if (active) MaterialTheme.colorScheme.primary else TextMuted
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.radio_current_station),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    AppTextButton(onClick = onChoose) {
                        Text(stringResource(R.string.dashboard_use), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Country, then codec and bitrate, leaving out whatever the directory doesn't know. */
@Composable
private fun stationDetails(station: RadioStation): String? {
    val bitrate = if (station.bitrate > 0) stringResource(R.string.radio_bitrate, station.bitrate) else ""
    val format = listOf(station.codec, bitrate).filter { it.isNotEmpty() }.joinToString(" ")
    val country = station.country
    return when {
        country.isNotEmpty() && format.isNotEmpty() ->
            stringResource(R.string.radio_station_details, country, format)
        country.isNotEmpty() -> country
        format.isNotEmpty() -> format
        else -> null
    }
}
