package com.sysadmindoc.alarmclock.ui.worldclock

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    viewModel: WorldClockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SurfaceDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = AccentBlue,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add time zone")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        "World Clock",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${state.localZone} - ${state.localTime}",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Clock list
            if (state.clocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No time zones added", color = TextMuted, fontSize = 16.sp)
                        Text("Tap + to add a city", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.clocks,
                        key = { it.zoneId }
                    ) { entry ->
                        WorldClockCard(
                            entry = entry,
                            onRemove = { viewModel.removeZone(entry.zoneId) }
                        )
                    }
                }
            }
        }
    }

    // Add time zone dialog
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
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // City info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.cityName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    entry.date,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    entry.offsetLabel,
                    color = if (entry.isAhead) AccentBlue else TextMuted,
                    fontSize = 12.sp
                )
            }

            // Time display
            Text(
                entry.time,
                color = AccentBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showDeleteConfirm = false
                }) {
                    Text("Remove", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            title = { Text("Remove ${entry.cityName}?", color = TextPrimary) },
            text = { Text("This time zone will be removed from your world clock.", color = TextSecondary) },
            containerColor = SurfaceMedium
        )
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
                Text("Cancel", color = TextSecondary)
            }
        },
        title = {
            Text("Add Time Zone", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search city or region...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceCard
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (searchResults.isEmpty() && searchQuery.length >= 2) {
                    Text(
                        "No results found",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.zoneId }
                    ) { entry ->
                        SearchResultRow(
                            entry = entry,
                            onClick = { onSelect(entry.zoneId) }
                        )
                    }
                }
            }
        },
        containerColor = SurfaceMedium
    )
}

@Composable
private fun SearchResultRow(
    entry: WorldClockEntry,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Public,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.cityName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    entry.zoneId,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
