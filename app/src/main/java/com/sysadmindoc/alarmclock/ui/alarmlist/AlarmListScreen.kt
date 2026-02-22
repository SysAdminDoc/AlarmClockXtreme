package com.sysadmindoc.alarmclock.ui.alarmlist

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmlist.components.SwipeableAlarmCard
import com.sysadmindoc.alarmclock.ui.templates.AlarmTemplate
import com.sysadmindoc.alarmclock.ui.templates.TemplatePickerSheet
import com.sysadmindoc.alarmclock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: AlarmListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTemplates by remember { mutableStateOf(false) }

    if (showTemplates) {
        TemplatePickerSheet(
            onSelect = { template ->
                viewModel.createFromTemplate(template)
                showTemplates = false
            },
            onDismiss = { showTemplates = false }
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Template mini-FAB
                SmallFloatingActionButton(
                    onClick = { showTemplates = true },
                    containerColor = SurfaceCard,
                    contentColor = AccentBlue
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Templates", modifier = Modifier.size(20.dp))
                }
                // Main add FAB
                FloatingActionButton(
                    onClick = onAddAlarm,
                    containerColor = AccentBlue,
                    contentColor = TextPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add alarm")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with countdown and settings gear
            AlarmHeader(
                remainingTime = state.remainingTime,
                hasAlarms = state.nextAlarm != null,
                vacationActive = state.vacationActive,
                sortLabel = when (state.sortOrder) {
                    AlarmSortOrder.TIME -> "Time"
                    AlarmSortOrder.CREATED -> "Newest"
                    AlarmSortOrder.ENABLED_FIRST -> "Active"
                },
                onCycleSort = viewModel::cycleSortOrder,
                onOpenSettings = onOpenSettings
            )

            // Quick alarm chips
            QuickAlarmRow(onQuickAlarm = viewModel::createQuickAlarm)

            // Search bar
            var searchQuery by remember { mutableStateOf("") }
            if (state.alarms.size > 3) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search alarms...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceCard,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = AccentBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Filter alarms by search
            val filteredAlarms = remember(state.alarms, searchQuery) {
                if (searchQuery.isBlank()) state.alarms
                else state.alarms.filter { alarm ->
                    alarm.label.contains(searchQuery, ignoreCase = true) ||
                    alarm.repeatLabel.contains(searchQuery, ignoreCase = true)
                }
            }

            // Alarm list
            if (state.alarms.isEmpty()) {
                EmptyState()
            } else if (filteredAlarms.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No alarms match \"$searchQuery\"", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = filteredAlarms,
                        key = { it.id }
                    ) { alarm ->
                        SwipeableAlarmCard(
                            onDelete = { viewModel.deleteAlarm(alarm) }
                        ) {
                            AlarmCard(
                                alarm = alarm,
                                onToggle = { viewModel.toggleAlarm(alarm) },
                                onClick = { onEditAlarm(alarm.id) },
                                onDelete = { viewModel.deleteAlarm(alarm) },
                                onSkipNext = { viewModel.skipNextOccurrence(alarm) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmHeader(
    remainingTime: String,
    hasAlarms: Boolean,
    vacationActive: Boolean,
    sortLabel: String = "Time",
    onCycleSort: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(HeaderTop, HeaderBottom)
                )
            )
    ) {
        // Sort + Settings - top right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            TextButton(onClick = onCycleSort) {
                Icon(Icons.Default.Sort, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(sortLabel, color = TextSecondary, fontSize = 11.sp)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
            }
        }

        // Vacation mode indicator
        if (vacationActive) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BeachAccess, null, tint = SnoozeYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Vacation", color = SnoozeYellow, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasAlarms && remainingTime.isNotBlank()) {
                Text("Remaining", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = remainingTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text("to next alarm", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else {
                Text(
                    text = "No alarms set",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickAlarmRow(onQuickAlarm: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Quick:",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        listOf(10 to "+10m", 30 to "+30m", 60 to "+1h", 120 to "+2h").forEach { (mins, label) ->
            AssistChip(
                onClick = { onQuickAlarm(mins) },
                label = { Text(label, fontSize = 12.sp, color = TextPrimary) },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCard),
                border = null
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSkipNext: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentBlue,
                    checkedTrackColor = AccentBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = ToggleOff,
                    uncheckedTrackColor = ToggleTrackOff
                ),
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
                    val amPm = if (alarm.hour < 12) "AM" else "PM"
                    Text(
                        text = "$hour12:${String.format("%02d", alarm.minute)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) TextPrimary else TextMuted
                    )
                    Text(
                        text = amPm,
                        fontSize = 16.sp,
                        color = if (alarm.isEnabled) TextSecondary else TextMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                }
                Text(
                    text = alarm.label.ifBlank { alarm.repeatLabel },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (alarm.isEnabled) TextSecondary else TextMuted
                )

                // Indicators row: challenge type, sound
                if (alarm.challengeType != "NONE" || alarm.ringtoneUri == "silent") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (alarm.challengeType != "NONE") {
                            val challengeLabel = alarm.challengeType.lowercase()
                                .replace("_", " ").replaceFirstChar { it.uppercase() }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SnoozeYellow.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    challengeLabel,
                                    fontSize = 10.sp,
                                    color = SnoozeYellow,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (alarm.ringtoneUri == "silent") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TextMuted.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Silent",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = if (alarm.isEnabled) Icons.Default.Notifications
                else Icons.Default.NotificationsOff,
                contentDescription = if (alarm.isEnabled) "Alarm enabled" else "Alarm disabled",
                tint = if (alarm.isEnabled) AccentBlue else TextMuted,
                modifier = Modifier.size(24.dp)
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onClick() }
                    )
                    if (alarm.isEnabled && alarm.repeatDays.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Skip Next") },
                            onClick = { showMenu = false; onSkipNext() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = AccentRed) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AlarmAdd,
                contentDescription = "Add alarm",
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No alarms yet", style = MaterialTheme.typography.titleLarge, color = TextMuted)
            Text("Tap + to add your first alarm", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}
