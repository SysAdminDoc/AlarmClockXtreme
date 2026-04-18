package com.sysadmindoc.alarmclock.ui.alarmlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmlist.components.SwipeableAlarmCard
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.templates.TemplatePickerSheet
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var searchQuery by remember { mutableStateOf("") }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }

    if (showTemplates) {
        TemplatePickerSheet(
            onSelect = { template ->
                viewModel.createFromTemplate(template)
                showTemplates = false
            },
            onDismiss = { showTemplates = false }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(state.undoAlarm) {
        state.undoAlarm?.let {
            val result = snackbarHostState.showSnackbar(
                message = "Alarm deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.confirmDelete()
            }
        }
    }

    val filteredAlarms = remember(state.alarms, searchQuery, state.selectedGroup) {
        state.alarms
            .filter { alarm ->
                state.selectedGroup == null || alarm.group == state.selectedGroup
            }
            .filter { alarm ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    alarm.label.contains(searchQuery, ignoreCase = true) ||
                        alarm.repeatLabel.contains(searchQuery, ignoreCase = true) ||
                        alarm.group.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    if (showBulkDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmation = false },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkDeleteConfirmation = false
                        viewModel.deleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text(
                        if (state.selectedIds.size == 1) "Delete alarm" else "Delete ${state.selectedIds.size} alarms"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = if (state.selectedIds.size == 1) {
                        "Delete selected alarm?"
                    } else {
                        "Delete ${state.selectedIds.size} selected alarms?"
                    },
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (state.selectedIds.size == filteredAlarms.size && filteredAlarms.isNotEmpty()) {
                        "This will remove every alarm currently visible in the list. Use this only if you are sure."
                    } else {
                        "This removes only the alarms currently selected. This bulk action does not offer per-alarm undo."
                    },
                    color = TextSecondary
                )
            },
            containerColor = SurfaceMedium
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showTemplates = true },
                    containerColor = SurfaceCard,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Alarm templates", modifier = Modifier.size(18.dp))
                }
                FloatingActionButton(
                    onClick = onAddAlarm,
                    containerColor = MaterialTheme.colorScheme.primary,
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
            AnimatedVisibility(visible = state.isSelectionMode) {
                SelectionActionBar(
                    selectedCount = state.selectedIds.size,
                    totalCount = filteredAlarms.size,
                    onSelectAll = { viewModel.selectMany(filteredAlarms.map { it.id }.toSet()) },
                    onClearSelection = viewModel::clearSelection,
                    onDeleteSelected = { showBulkDeleteConfirmation = true },
                    onEnableSelected = viewModel::enableSelected,
                    onDisableSelected = viewModel::disableSelected
                )
            }

            AlarmHeader(
                remainingTime = state.remainingTime,
                hasAlarms = state.nextAlarm != null,
                alarmCount = state.alarms.size,
                vacationActive = state.vacationActive,
                sortLabel = when (state.sortOrder) {
                    AlarmSortOrder.TIME -> "Sort by time"
                    AlarmSortOrder.CREATED -> "Newest first"
                    AlarmSortOrder.ENABLED_FIRST -> "Active first"
                },
                onCycleSort = viewModel::cycleSortOrder,
                onOpenSettings = onOpenSettings
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.groups.any { it.isNotBlank() }) {
                    GroupFilterRow(
                        groups = state.groups.filter { it.isNotBlank() },
                        selectedGroup = state.selectedGroup,
                        onSelectGroup = viewModel::selectGroup
                    )
                }

                QuickAlarmRow(
                    onQuickAlarm = viewModel::createQuickAlarm,
                    napDefaultMinutes = state.napDefaultMinutes
                )

                if (state.alarms.size > 3) {
                    AppSurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        AppSectionTitle(
                            title = "Search alarms",
                            description = "Filter by label, repeat schedule, or group."
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Try “weekday”, “gym”, or “medication”") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = TextMuted)
                                    }
                                }
                            },
                            colors = appOutlinedTextFieldColors(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            when {
                state.alarms.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            AppEmptyState(
                                icon = Icons.Default.AlarmAdd,
                                title = "No alarms yet",
                                description = "Create your first wake-up, or start from a template if you want a polished head start.",
                                footer = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = onAddAlarm,
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Create alarm")
                                        }
                                        OutlinedButton(
                                            onClick = { showTemplates = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Browse templates")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                filteredAlarms.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            AppEmptyState(
                                icon = Icons.Default.Search,
                                title = "No alarms match that search",
                                description = "Try a different label or clear your filters to bring everything back.",
                                footer = {
                                    TextButton(onClick = { searchQuery = "" }) {
                                        Text("Clear search", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAlarms, key = { it.id }) { alarm ->
                            val isSelected = alarm.id in state.selectedIds
                            if (state.isSelectionMode) {
                                SelectableAlarmCard(
                                    alarm = alarm,
                                    is24Hour = state.is24HourFormat,
                                    isSelected = isSelected,
                                    onToggleSelect = { viewModel.toggleSelection(alarm.id) }
                                )
                            } else {
                                SwipeableAlarmCard(
                                    onDelete = { viewModel.deleteAlarm(alarm) }
                                ) {
                                    AlarmCard(
                                        alarm = alarm,
                                        is24Hour = state.is24HourFormat,
                                        onToggle = { viewModel.toggleAlarm(alarm) },
                                        onClick = { onEditAlarm(alarm.id) },
                                        onDelete = { viewModel.deleteAlarm(alarm) },
                                        onSkipNext = { viewModel.skipNextOccurrence(alarm) },
                                        onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                        onLongClick = { viewModel.toggleSelection(alarm.id) }
                                    )
                                }
                            }
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
    alarmCount: Int,
    vacationActive: Boolean,
    sortLabel: String,
    onCycleSort: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlarmClockHeroHeader(
        title = if (hasAlarms && remainingTime.isNotBlank()) "Next alarm in $remainingTime" else "Alarm schedule",
        subtitle = if (hasAlarms && remainingTime.isNotBlank()) {
            "Everything important is visible at a glance, so it is easy to trust what rings next."
        } else {
            "Create, group, and refine alarms from one calm control center."
        },
        overline = "Alarms",
        badge = {
            AppStatusChip(
                label = if (alarmCount == 1) "1 alarm" else "$alarmCount alarms",
                icon = Icons.Default.Notifications
            )
            AppStatusChip(
                label = sortLabel,
                icon = Icons.AutoMirrored.Filled.Sort
            )
            if (vacationActive) {
                AppStatusChip(
                    label = "Vacation mode",
                    icon = Icons.Default.BeachAccess,
                    color = SnoozeYellow
                )
            }
        },
        actions = {
            TextButton(onClick = onCycleSort) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sort", color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Alarm settings", tint = TextMuted)
            }
        }
    )
}

@Composable
private fun GroupFilterRow(
    groups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitle(
            title = "Groups",
            description = "Filter recurring alarms by context."
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onSelectGroup(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    containerColor = SurfaceCard
                )
            )
            groups.forEach { group ->
                FilterChip(
                    selected = selectedGroup == group,
                    onClick = { onSelectGroup(if (selectedGroup == group) null else group) },
                    label = { Text(group) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        containerColor = SurfaceCard
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickAlarmRow(
    onQuickAlarm: (Int) -> Unit,
    napDefaultMinutes: Int = 20
) {
    AppSurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        AppSectionTitle(
            title = "Quick alarms",
            description = "Need a short reminder or power nap? Start one with a single tap."
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10 to "10 min", 30 to "30 min", 60 to "1 hour", 120 to "2 hours").forEach { (minutes, label) ->
                AssistChip(
                    onClick = { onQuickAlarm(minutes) },
                    label = { Text(label, color = TextPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCard),
                    border = null
                )
            }
        }
        // v1.4.0 nap row, v1.5.0 pre-selects the user's default.
        Text(
            text = "Power nap",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Always include the user's default nap length, even if it's not
            // one of the standard chip values, so the setting is honored here.
            val napOptions = (listOf(15, 20, 25, 45, 90) + napDefaultMinutes)
                .filter { it > 0 }
                .distinct()
                .sorted()
            napOptions.forEach { minutes ->
                val isDefault = minutes == napDefaultMinutes
                AssistChip(
                    onClick = { onQuickAlarm(minutes) },
                    label = {
                        Text(
                            text = if (isDefault) "$minutes min nap \u2022 default" else "$minutes min nap",
                            color = TextPrimary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isDefault) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        }
                    ),
                    border = null
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    is24Hour: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSkipNext: () -> Unit,
    onDuplicate: () -> Unit,
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) SurfaceMedium else SurfaceCard.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (alarm.isEnabled) TextMuted.copy(alpha = 0.14f) else TextMuted.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatAlarmTime(alarm, is24Hour),
                        color = if (alarm.isEnabled) TextPrimary else TextMuted,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = alarm.label.ifBlank { alarm.repeatLabel },
                        color = if (alarm.isEnabled) TextSecondary else TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = appSwitchColors()
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Alarm options", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { showMenu = false; onClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onDuplicate() }
                            )
                            if (alarm.isEnabled && alarm.repeatDays.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Skip next") },
                                    leadingIcon = { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; onSkipNext() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = AccentRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentRed, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Text(
                text = nextOccurrenceLabel(alarm, is24Hour),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = if (alarm.isEnabled) "Enabled" else "Paused",
                    icon = if (alarm.isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    color = if (alarm.isEnabled) DismissGreen else TextMuted
                )
                AppStatusChip(
                    label = alarm.repeatLabel,
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (alarm.group.isNotBlank() || alarm.challengeType != "NONE" || alarm.ringtoneUri == "silent") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (alarm.group.isNotBlank()) {
                        AppStatusChip(label = alarm.group)
                    }
                    if (alarm.challengeType != "NONE") {
                        AppStatusChip(
                            label = alarm.challengeType.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                            color = SnoozeYellow
                        )
                    }
                    if (alarm.ringtoneUri == "silent") {
                        AppStatusChip(label = "Silent", color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEnableSelected: () -> Unit,
    onDisableSelected: () -> Unit
) {
    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        highlighted = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, "Clear selection", tint = TextPrimary)
                    }
                    Column {
                        Text("$selectedCount selected", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (selectedCount == totalCount) {
                                "Bulk actions apply to everything currently on screen"
                            } else {
                                "Bulk actions apply only to the alarms you selected"
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (selectedCount < totalCount) {
                    TextButton(onClick = onSelectAll) {
                        Text("Select visible", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEnableSelected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DismissGreen)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enable")
                }
                OutlinedButton(
                    onClick = onDisableSelected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause")
                }
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableAlarmCard(
    alarm: Alarm,
    is24Hour: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleSelect)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                SurfaceMedium
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = TextMuted
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatAlarmTime(alarm, is24Hour),
                    color = if (alarm.isEnabled) TextPrimary else TextMuted,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = alarm.label.ifBlank { alarm.repeatLabel },
                    color = if (alarm.isEnabled) TextSecondary else TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = if (alarm.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else TextMuted
            )
        }
    }
}

private fun formatAlarmTime(alarm: Alarm, is24Hour: Boolean): String {
    return if (is24Hour) {
        String.format("%02d:%02d", alarm.hour, alarm.minute)
    } else {
        val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
        val amPm = if (alarm.hour < 12) "AM" else "PM"
        "$hour12:${String.format("%02d", alarm.minute)} $amPm"
    }
}

private fun nextOccurrenceLabel(alarm: Alarm, is24Hour: Boolean): String {
    if (!alarm.isEnabled || alarm.nextTriggerTime <= 0) {
        return "Paused until you re-enable this alarm"
    }
    val pattern = if (is24Hour) "EEE, MMM d • HH:mm" else "EEE, MMM d • h:mm a"
    val formatted = Instant.ofEpochMilli(alarm.nextTriggerTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern))
    return "Next occurrence: $formatted"
}
