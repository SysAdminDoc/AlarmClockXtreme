package com.sysadmindoc.alarmclock.ui.worldclock

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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    Scaffold(
        containerColor = SurfaceDark,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Add city")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AlarmClockHeroHeader(
                title = "World Clock",
                subtitle = "Track the cities that matter without doing time-zone math in your head.",
                overline = "Global time",
                badge = {
                    AppStatusChip(
                        label = "${state.localZone} • ${state.localTime}",
                        icon = Icons.Default.Schedule
                    )
                    AppStatusChip(
                        label = "${state.clocks.size} cities",
                        icon = Icons.Default.Public
                    )
                }
            )

            if (state.clocks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    AppSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        AppEmptyState(
                            icon = Icons.Default.Public,
                            title = "No world clocks yet",
                            description = "Add the cities you check most often and keep them one tap away.",
                            footer = {
                                TextButton(onClick = viewModel::showAddDialog) {
                                    Text("Add a city", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AppSectionTitle(
                            title = "Saved cities",
                            description = "Offsets update live so you can compare times instantly."
                        )
                    }

                    items(state.clocks, key = { it.zoneId }) { entry ->
                        WorldClockCard(
                            entry = entry,
                            onRemove = { viewModel.removeZone(entry.zoneId) }
                        )
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
                AppStatusChip(
                    label = entry.offsetLabel,
                    icon = Icons.Default.Language,
                    color = if (entry.isAhead) MaterialTheme.colorScheme.primary else TextMuted
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
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove city", tint = TextMuted)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        title = {
            Text("Add time zone", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search city or region") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchResults.isEmpty() && searchQuery.length >= 2) {
                    AppEmptyState(
                        icon = Icons.Default.Search,
                        title = "No matching cities",
                        description = "Try a broader city name or search by time-zone region."
                    )
                }

                if (searchQuery.isBlank()) {
                    AppEmptyState(
                        icon = Icons.Default.Public,
                        title = "Search for a city",
                        description = "Type at least two characters to find a city or a time-zone region."
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { entry ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = SurfaceCard.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelect(entry.zoneId) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.cityName, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                Text(entry.zoneId, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        containerColor = SurfaceDark
    )
}
