package com.sysadmindoc.alarmclock.ui.worldclock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.R
import androidx.compose.ui.res.stringResource

@Composable
fun WorldClockScreen(
    viewModel: WorldClockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<WorldClockEntry?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // Single LazyColumn with hero + content as items. Mirrors the
        // AlarmListScreen pattern so layout regressions like "cities don't
        // render" can't happen — the LazyColumn owns the full vertical
        // budget, no nested scrollables fighting for space.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                AlarmClockHeroHeader(
                    title = stringResource(R.string.world_world_clock),
                    subtitle = "${state.localZone} · ${state.localTime}",
                    actions = {
                        IconButton(onClick = viewModel::showAddDialog) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.world_add_city),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            if (state.clocks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                    ) {
                        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            AppEmptyState(
                                icon = Icons.Default.Public,
                                title = stringResource(R.string.world_no_world_clocks_yet),
                                description = stringResource(R.string.world_add_cities_check_most_often),
                                footer = {
                                    Button(
                                        onClick = viewModel::showAddDialog,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(stringResource(R.string.world_add_city))
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        AppSurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            state.clocks.forEachIndexed { index, entry ->
                                WorldClockCard(
                                    entry = entry,
                                    onRemove = { pendingRemoval = entry }
                                )
                                if (index < state.clocks.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = TextMuted.copy(alpha = 0.16f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddTimeZoneDialog(
            searchQuery = state.searchQuery,
            searchResults = state.searchResults,
            onQueryChange = viewModel::searchZones,
            onSelect = viewModel::addZone,
            onDismiss = viewModel::hideAddDialog
        )
    }

    pendingRemoval?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AccentRed
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeZone(entry.zoneId)
                        pendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.world_remove_city))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.world_keep_city), color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.world_remove_city_title, entry.cityName),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.world_remove_city_message, entry.zoneId),
                    color = TextSecondary
                )
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun WorldClockCard(
    entry: WorldClockEntry,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                entry.cityName,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "${entry.date} · ${entry.zoneId}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Text(
                worldClockOffsetLabel(entry),
                color = worldClockAccent(entry),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            entry.time,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall
        )
        IconButton(onClick = onRemove) {
            Icon(
                // The overflow glyph promised a menu; there is only one action.
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.world_remove_city_action, entry.cityName),
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun AddTimeZoneDialog(
    searchQuery: String,
    searchResults: List<WorldClockEntry>,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val showPrompt = searchQuery.isBlank()
    val showNoResults = searchResults.isEmpty() && searchQuery.length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close), color = TextSecondary)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.world_add_city_2), color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.world_search_by_city_country_time),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.world_search_city_country_region)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchResults.isNotEmpty()) {
                    AppStatusChip(
                        label = stringResource(R.string.worldclock_match, searchResults.size, if (searchResults.size == 1) "" else "es"),
                        icon = Icons.Default.Public,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (showPrompt || showNoResults) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (showNoResults) Icons.Default.Search else Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (showNoResults) TextMuted else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (showNoResults) stringResource(R.string.worldclock_no_city_matches_that_search) else stringResource(R.string.dashboard_search_city),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = if (showNoResults) {
                                    "Try a broader city name, or search a region such as Europe or America."
                                } else {
                                    "Type at least two characters to search the available time zones."
                                },
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { entry ->
                        // Resolved before the semantics lambda, which is
                        // not a composable scope.
                        val addCityLabel = stringResource(
                            R.string.world_add_city_action,
                            entry.cityName,
                            entry.time,
                            worldClockOffsetLabel(entry)
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = addCityLabel
                                }
                                .clickable(role = Role.Button) { onSelect(entry.zoneId) },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCard.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(entry.cityName, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    Text(entry.zoneId, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    AppStatusChip(
                                        label = worldClockOffsetLabel(entry),
                                        icon = Icons.Default.Language,
                                        color = worldClockAccent(entry)
                                    )
                                }
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = entry.time,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = entry.date,
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    AppStatusChip(
                                        label = stringResource(R.string.alarm_edit_add),
                                        icon = Icons.Default.Add,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    )
}

/** The offset in words. Built here so it can be translated. */
@Composable
private fun worldClockOffsetLabel(entry: WorldClockEntry): String = when {
    entry.offsetHours == 0.0 -> stringResource(R.string.world_same_time)
    entry.offsetHours > 0 -> stringResource(
        R.string.world_hours_ahead,
        formatOffsetHours(entry.offsetHours)
    )
    else -> stringResource(R.string.world_hours_behind, formatOffsetHours(entry.offsetHours))
}

private fun formatOffsetHours(hours: Double): String {
    val magnitude = kotlin.math.abs(hours)
    return if (magnitude % 1.0 == 0.0) magnitude.toInt().toString() else "%.1f".format(magnitude)
}

@Composable
private fun worldClockAccent(entry: WorldClockEntry) = when {
    entry.offsetHours == 0.0 -> DismissGreen
    entry.isAhead -> MaterialTheme.colorScheme.primary
    else -> TextMuted
}
