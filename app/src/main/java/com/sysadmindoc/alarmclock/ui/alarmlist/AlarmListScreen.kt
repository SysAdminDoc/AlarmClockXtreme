package com.sysadmindoc.alarmclock.ui.alarmlist

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec
import com.sysadmindoc.alarmclock.ui.alarmlist.components.SwipeableAlarmCard
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.components.appSwitchColors
import com.sysadmindoc.alarmclock.ui.templates.TemplatePickerSheet
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.ClockTimeSmall
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.LocalAppShapeTokens
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: AlarmListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTemplates by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }

    // v1.7.1: Prominent (non-tucked) YouTube download entry. The user can
    // build up an alarm-sound library without first creating an alarm.
    var showYouTubeDialog by remember { mutableStateOf(false) }
    val youTubeAvailable = com.sysadmindoc.alarmclock.ui.components.isYouTubeDownloaderAvailable()

    var statsAlarmLabel by remember { mutableStateOf<String?>(null) }
    val alarmStats by viewModel.alarmStats.collectAsStateWithLifecycle()
    if (statsAlarmLabel != null && alarmStats != null) {
        val stats = alarmStats!!
        AlertDialog(
            onDismissRequest = { statsAlarmLabel = null; viewModel.clearAlarmStats() },
            confirmButton = {
                TextButton(onClick = { statsAlarmLabel = null; viewModel.clearAlarmStats() }) {
                    Text("Close")
                }
            },
            title = { Text(statsAlarmLabel ?: "Alarm history") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Last 30 days", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    if (stats.fireCount == 0) {
                        // A brand-new (or recently-cleared) alarm has nothing to
                        // report yet — frame it rather than dumping all-zero stats.
                        Text(
                            "This alarm hasn't fired in the last 30 days yet.",
                            color = TextSecondary
                        )
                    } else {
                        Text("Fired ${stats.fireCount} times")
                        Text("Avg ${String.format("%.1f", stats.avgSnoozesPerFire)} snoozes per fire")
                        Text("Avg dismiss in ${stats.avgDismissTimeSec}s")
                        if (stats.missedCount > 0) {
                            Text("${stats.missedCount} missed", color = AccentRed)
                        }
                    }
                }
            }
        )
    }

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
    val snackbarScope = androidx.compose.runtime.rememberCoroutineScope()

    if (showYouTubeDialog) {
        com.sysadmindoc.alarmclock.ui.components.YouTubeDownloadDialog(
            onDismiss = { showYouTubeDialog = false },
            onDownloaded = { savedTitle ->
                showYouTubeDialog = false
                snackbarScope.launch {
                    snackbarHostState.showSnackbar(
                        "Saved \"$savedTitle\". Pick it from any alarm's sound list.",
                        duration = SnackbarDuration.Long
                    )
                }
            },
            onError = { msg ->
                showYouTubeDialog = false
                snackbarScope.launch {
                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
                }
            }
        )
    }

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
                        showBulkDeleteConfirmation = false
                        viewModel.deleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
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
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
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
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        // v1.7.1: Skip the inner Scaffold's default system insets — the outer
        // AppNavigation Scaffold already paddings NavHost with the bottom-nav
        // inset. Without this, both scaffolds compete for the same insets and
        // the alarm list stops well short of the floating nav.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                // v1.7.1: dropped from 120dp → 24dp. The outer AppNavigation
                // Scaffold already pads NavHost with the bottom-nav inset, so
                // the prior 120dp was a redundant gap above the floating nav.
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
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
                }

                // v1.7.1: Top-level YouTube downloader card. Sits ABOVE the
                // alarm list so users see it as a first-class action, not
                // something hidden behind "create new alarm." Tapping the
                // card opens the same dialog as the picker; the saved tone
                // becomes available in every alarm's sound picker.
                if (youTubeAvailable) {
                    item {
                        YouTubeDownloadCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = { showYouTubeDialog = true }
                        )
                    }
                }

                if (state.groups.any { it.isNotBlank() } || state.alarms.size > 3) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (state.groups.any { it.isNotBlank() }) {
                                GroupFilterRow(
                                    title = "Groups",
                                    groups = state.groups.filter { it.isNotBlank() },
                                    selectedGroup = state.selectedGroup,
                                    onSelectGroup = viewModel::selectGroup
                                )
                            }

                            if (state.profiles.any { it.isNotBlank() }) {
                                GroupFilterRow(
                                    title = "Profiles",
                                    groups = state.profiles.filter { it.isNotBlank() },
                                    selectedGroup = state.selectedProfile,
                                    onSelectGroup = viewModel::selectProfile
                                )
                            }

                            if (state.alarms.size > 3) {
                                AppSurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Try “weekday”, “gym”, or “medication”") },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, "Clear search", tint = TextMuted)
                                                }
                                            }
                                        },
                                        colors = appOutlinedTextFieldColors(),
                                        shape = AppInputShape,
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                when {
                    state.alarms.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                    AppEmptyState(
                                        icon = Icons.Default.AlarmAdd,
                                        title = "No alarms yet",
                                        description = "Create your first wake-up, or start from a template.",
                                        footer = {
                                            AlarmListEmptyActions(
                                                onAddAlarm = onAddAlarm,
                                                onBrowseTemplates = { showTemplates = true }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    filteredAlarms.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                    AppEmptyState(
                                        icon = Icons.Default.Search,
                                        title = "No alarms match that search",
                                        description = "Try a different label or clear your filters to bring everything back.",
                                        footer = {
                                            TextButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    viewModel.selectGroup(null)
                                                    viewModel.selectProfile(null)
                                                }
                                            ) {
                                                Text("Clear filters", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            // v1.7.1: Dropped the "Saved alarms" section title +
                            // description — the hero subtitle already says
                            // "Tap an alarm to edit it." Keeping just the
                            // action row reclaims another ~50dp of vertical
                            // space so the first alarm card lands above the
                            // fold on standard phones.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                    OutlinedButton(
                                        onClick = { showTemplates = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Templates")
                                    }
                                    Button(
                                        onClick = onAddAlarm,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("New alarm")
                                    }
                            }
                        }
                        val conflictTimes = filteredAlarms
                            .filter { it.isEnabled }
                            .groupBy { it.hour * 60 + it.minute }
                            .filterValues { it.size > 1 }
                            .keys
                        if (conflictTimes.isNotEmpty()) {
                            item {
                                val timeLabels = conflictTimes.joinToString(", ") { totalMin ->
                                    val h = totalMin / 60
                                    val m = totalMin % 60
                                    if (state.is24HourFormat) "%02d:%02d".format(h, m)
                                    else "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
                                }
                                AppInlineNotice(
                                    title = "Duplicate fire time",
                                    message = "Multiple enabled alarms are set for $timeLabels. Review them if that was not intentional.",
                                    icon = Icons.Default.Warning,
                                    color = SnoozeYellow,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(filteredAlarms, key = { it.id }) { alarm ->
                            Box(modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()) {
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
                                        // v1.5.2: Surface vacation suppression per-card.
                                        val suppressedByVacation = alarm.isEnabled &&
                                            state.vacationStartMillis > 0L &&
                                            state.vacationEndMillis > state.vacationStartMillis &&
                                            alarm.nextTriggerTime in
                                                state.vacationStartMillis..state.vacationEndMillis
                                        AlarmCard(
                                            alarm = alarm,
                                            is24Hour = state.is24HourFormat,
                                            suppressedByVacation = suppressedByVacation,
                                            onToggle = { viewModel.toggleAlarm(alarm) },
                                            onForceToggle = { viewModel.forceDisableAlarm(alarm) },
                                            onClick = { onEditAlarm(alarm.id) },
                                            onDelete = { viewModel.deleteAlarm(alarm) },
                                            onSkipNext = { viewModel.skipNextOccurrence(alarm) },
                                            onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                            onShare = { shareAlarm(context, alarm, state.is24HourFormat) },
                                            onShowHistory = {
                                                statsAlarmLabel = alarm.label.ifBlank { "%d:%02d".format(alarm.hour, alarm.minute) }
                                                viewModel.loadAlarmStats(alarm.id)
                                            },
                                            onLongClick = { viewModel.toggleSelection(alarm.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        QuickAlarmRow(
                            onQuickAlarm = viewModel::createQuickAlarm,
                            napDefaultMinutes = state.napDefaultMinutes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmListEmptyActions(
    onAddAlarm: () -> Unit,
    onBrowseTemplates: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAddAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create alarm")
                }
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Browse templates")
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddAlarm,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create alarm")
                }
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Browse templates")
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
        title = when {
            hasAlarms && remainingTime.isNotBlank() -> "Next alarm in $remainingTime"
            alarmCount > 0 -> "All alarms paused"
            else -> "No alarms scheduled"
        },
        subtitle = when {
            hasAlarms && remainingTime.isNotBlank() -> "Tap an alarm to edit, or add a new one below."
            alarmCount > 0 -> "Enable a saved alarm when you want it back in rotation."
            else -> "Create your first wake-up, or start from a template."
        },
        badge = {
            AppStatusChip(
                label = when (alarmCount) {
                    0 -> "Ready to schedule"
                    1 -> "1 alarm"
                    else -> "$alarmCount alarms"
                },
                icon = if (alarmCount == 0) Icons.Default.AlarmAdd else Icons.Default.Notifications
            )
            if (!hasAlarms && alarmCount > 0) {
                AppStatusChip(
                    label = "Paused",
                    icon = Icons.Default.NotificationsOff,
                    color = TextMuted
                )
            }
            // v1.7.1: The sort chip is now tappable so the standalone "Sort"
            // button (which used to live in the actions slot at the very top
            // right of the hero) can go away entirely. The gear icon next to
            // it duplicated the Settings tab and was likewise removed. This
            // claws back ~70dp of vertical space at the top of the screen.
            AppStatusChip(
                label = sortLabel,
                icon = Icons.AutoMirrored.Filled.Sort,
                onClick = onCycleSort
            )
            if (vacationActive) {
                AppStatusChip(
                    label = "Vacation mode",
                    icon = Icons.Default.BeachAccess,
                    color = SnoozeYellow
                )
            }
        }
        // actions slot intentionally left null — see badge comment above.
    )
}

@Composable
private fun GroupFilterRow(
    title: String,
    groups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitle(title = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppFilterChip(
                label = "All",
                selected = selectedGroup == null,
                onClick = { onSelectGroup(null) },
                selectionSemantics = true,
            )
            groups.forEach { group ->
                AppFilterChip(
                    label = group,
                    selected = selectedGroup == group,
                    onClick = { onSelectGroup(if (selectedGroup == group) null else group) },
                    selectionSemantics = true,
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
            description = "Tap a duration to schedule it now."
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10 to "10 min", 30 to "30 min", 60 to "1 hour", 120 to "2 hours").forEach { (minutes, label) ->
                AppFilterChip(
                    label = label,
                    selected = false,
                    onClick = { onQuickAlarm(minutes) },
                )
            }
        }
        // v1.4.0 nap row, v1.5.0 pre-selects the user's default.
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        Text(
            text = "Power nap",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
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
                AppFilterChip(
                    label = "$minutes min",
                    selected = isDefault,
                    leadingIcon = if (isDefault) Icons.Default.CheckCircle else null,
                    selectionSemantics = false,
                    onClick = { onQuickAlarm(minutes) },
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
    suppressedByVacation: Boolean = false,
    onToggle: () -> Unit,
    onForceToggle: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSkipNext: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onShowHistory: () -> Unit = {},
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val shapeTokens = LocalAppShapeTokens.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shapeTokens.card,
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) SurfaceCard else SurfaceCard.copy(alpha = 0.55f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (alarm.isEnabled) {
                com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
            } else {
                com.sysadmindoc.alarmclock.ui.theme.BorderSubtle.copy(alpha = 0.5f)
            }
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatAlarmTime(alarm, is24Hour),
                        color = if (alarm.isEnabled) TextPrimary else TextMuted,
                        style = ClockTimeSmall
                    )
                    Text(
                        text = alarm.label.ifBlank { alarm.repeatLabel },
                        color = if (alarm.isEnabled) TextSecondary else TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val alarmToggleLabel = alarm.label.ifBlank { formatAlarmTime(alarm, is24Hour) }
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onToggle() },
                                onLongClick = { if (alarm.isEnabled) onForceToggle() }
                            )
                            .semantics {
                                contentDescription = "$alarmToggleLabel alarm"
                                stateDescription = if (alarm.isEnabled) "Enabled" else "Disabled"
                                role = Role.Switch
                            }
                    ) {
                        Switch(
                            checked = alarm.isEnabled,
                            onCheckedChange = null,
                            colors = appSwitchColors(),
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
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
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onShare() }
                            )
                            if (alarm.isEnabled && alarm.repeatDays.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Skip next") },
                                    leadingIcon = { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; onSkipNext() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("History") },
                                leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onShowHistory() }
                            )
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
                // v1.5.2: Be honest when vacation mode is swallowing the fire.
                text = if (suppressedByVacation) {
                    "Paused until vacation ends"
                } else {
                    nextOccurrenceLabel(alarm, is24Hour)
                },
                color = if (suppressedByVacation) SnoozeYellow else TextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            // One unified chip row — previously two separate horizontal-scroll
            // rows, which made cards stack a little taller and produced
            // mismatched gaps when only one row had content.
            val showChipRow = suppressedByVacation ||
                alarm.repeatLabel.isNotBlank() ||
                alarm.group.isNotBlank() ||
                alarm.challengeType != "NONE" ||
                alarm.ringtoneUri == "silent"
            if (showChipRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (suppressedByVacation) {
                        AppStatusChip(
                            label = "Paused by vacation",
                            icon = Icons.Default.BeachAccess,
                            color = SnoozeYellow
                        )
                    }
                    if (alarm.repeatLabel.isNotBlank()) {
                        AppStatusChip(
                            label = alarm.repeatLabel,
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (alarm.group.isNotBlank()) {
                        AppStatusChip(label = alarm.group)
                    }
                    if (alarm.challengeType != "NONE") {
                        AppStatusChip(
                            label = challengeTypeLabel(alarm.challengeType),
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DismissGreen)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enable")
                }
                OutlinedButton(
                    onClick = onDisableSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause")
                }
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
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
    val shapeTokens = LocalAppShapeTokens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleSelect)
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        shape = shapeTokens.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                SurfaceMedium
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
            else com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
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

private fun shareAlarm(context: Context, alarm: Alarm, is24Hour: Boolean) {
    val deepLink = AlarmShareCodec.createDeepLink(alarm)
    val title = alarm.label.ifBlank { "Alarm ${formatAlarmTime(alarm, is24Hour)}" }
    val shareText = buildString {
        appendLine("AlarmClockXtreme alarm: $title")
        appendLine("Time: ${formatAlarmTime(alarm, is24Hour)}")
        append("Import: $deepLink")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "AlarmClockXtreme alarm: $title")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share alarm"))
    }.onFailure {
        Toast.makeText(context, "No app is available to share this alarm.", Toast.LENGTH_SHORT).show()
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

private fun challengeTypeLabel(type: String): String = when (type) {
    "MATH_EASY"      -> "Math (Easy)"
    "MATH_MEDIUM"    -> "Math (Medium)"
    "MATH_HARD"      -> "Math (Hard)"
    "SHAKE"          -> "Shake Phone"
    "SEQUENCE"       -> "Number Sequence"
    "MEMORY_PATTERN" -> "Memory Pattern"
    "TYPING"         -> "Type a Phrase"
    "WALK_STEPS"     -> "Walk Steps"
    "NFC_SCAN"       -> "NFC Tag Scan"
    "BARCODE_SCAN"   -> "Barcode Scan"
    "PHOTO_MATCH"    -> "Photo Match"
    "SQUAT"          -> "Squats"
    "WIFI_CONNECT"   -> "Wi-Fi Connect"
    "MAZE"           -> "Maze Puzzle"
    "COUNT_SHEEP"    -> "Count the Sheep"
    "SIMON_SAYS"     -> "Simon Says"
    "DATE_BACKWARDS" -> "Type Date Backwards"
    "STROOP"         -> "Stroop Color Test"
    else             -> type.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
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

@Composable
private fun YouTubeDownloadCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shapeTokens = LocalAppShapeTokens.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        shape = shapeTokens.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Download alarm sound from YouTube",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Paste a URL — the sound is added to your library, ready to use on any alarm.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
