package com.sysadmindoc.alarmclock.ui.worldclock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                AlarmClockHeroHeader(
                    title = "World Clock",
                    subtitle = "Track the cities that matter without doing time-zone math in your head.",
                    overline = "Global time",
                    badge = {
                        AppStatusChip(
                            label = "${state.localZone} • ${state.localTime}",
                            icon = Icons.Default.Schedule
                        )
                        if (state.clocks.isNotEmpty()) {
                            AppStatusChip(
                                label = "${state.clocks.size} cities",
                                icon = Icons.Default.Public
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
                                title = "No world clocks yet",
                                description = "Add the cities you check most often and keep them one tap away.",
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
                                        Text("Add city")
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        AppSectionTitle(
                            title = "Saved cities",
                            description = "Offsets update live so you can compare times instantly."
                        )
                    }
                }

                items(state.clocks, key = { it.zoneId }) { entry ->
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        )
                    ) {
                        WorldClockCard(
                            entry = entry,
                            onRemove = { pendingRemoval = entry }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = viewModel::showAddDialog,
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = TextPrimary,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Add city")
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
                    Text("Remove city")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Keep city", color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = "Remove ${entry.cityName}?",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "This removes the saved world clock for ${entry.zoneId}. You can add it again later.",
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
    AppSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    entry.cityName,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    entry.date,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    entry.zoneId,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                AppStatusChip(
                    label = entry.offsetLabel,
                    icon = Icons.Default.Language,
                    color = worldClockAccent(entry)
                )
            }

            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    entry.time,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCard.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.12f))
                ) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove city", tint = TextMuted)
                    }
                }
            }
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
                Text("Close", color = TextSecondary)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Add a city", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Search by city name or time-zone region, then tap a result to add it.",
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
                    placeholder = { Text("Search city or region") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchResults.isNotEmpty()) {
                    AppStatusChip(
                        label = "${searchResults.size} match${if (searchResults.size == 1) "" else "es"}",
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
                                    text = if (showNoResults) "No matching cities yet" else "Search for a city",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = if (showNoResults) {
                                    "Try a broader city name or search by a time-zone region like Europe or America."
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
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                        label = entry.offsetLabel,
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
                                        label = "Add",
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

@Composable
private fun worldClockAccent(entry: WorldClockEntry) = when {
    entry.offsetLabel == "Same time" -> DismissGreen
    entry.isAhead -> MaterialTheme.colorScheme.primary
    else -> TextMuted
}
