package com.sysadmindoc.alarmclock.ui.alarmlist

import com.sysadmindoc.alarmclock.ui.alarmedit.toAlarmChallengeSummary
import androidx.compose.ui.res.pluralStringResource
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.model.ShiftPattern
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec
import com.sysadmindoc.alarmclock.ui.adaptive.shouldUseTwoPaneLayout
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
    // Saveable: a rotation used to close the dialog while the download it
    // started carried on in the ViewModel.
    var showYouTubeDialog by rememberSaveable { mutableStateOf(false) }
    val youTubeAvailable = com.sysadmindoc.alarmclock.ui.components.isYouTubeDownloaderAvailable()

    var statsAlarmLabel by remember { mutableStateOf<String?>(null) }
    val alarmStats by viewModel.alarmStats.collectAsStateWithLifecycle()
    if (statsAlarmLabel != null && alarmStats != null) {
        val stats = alarmStats!!
        AlertDialog(
            onDismissRequest = { statsAlarmLabel = null; viewModel.clearAlarmStats() },
            confirmButton = {
                TextButton(onClick = { statsAlarmLabel = null; viewModel.clearAlarmStats() }) {
                    Text(stringResource(R.string.settings_close))
                }
            },
            title = { Text(statsAlarmLabel ?: stringResource(R.string.alarmlist_alarm_history)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.alarm_list_stats_window),
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (stats.fireCount == 0) {
                        // A brand-new (or recently-cleared) alarm has nothing to
                        // report yet — frame it rather than dumping all-zero stats.
                        Text(
                            stringResource(R.string.alarm_list_stats_empty),
                            color = TextSecondary
                        )
                    } else {
                        Text(stringResource(R.string.alarm_list_stats_fired, stats.fireCount))
                        Text(
                            stringResource(
                                R.string.alarm_list_stats_avg_snoozes,
                                String.format("%.1f", stats.avgSnoozesPerFire)
                            )
                        )
                        Text(stringResource(R.string.alarm_list_stats_avg_dismiss, stats.avgDismissTimeSec))
                        if (stats.missedCount > 0) {
                            Text(
                                stringResource(R.string.alarm_list_stats_missed, stats.missedCount),
                                color = AccentRed
                            )
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
    val listState = rememberLazyListState()
    var draggingAlarmId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var selectedAlarmId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Read here: the snackbar itself is shown from a coroutine.
    val savedToneTemplate = stringResource(R.string.alarmlist_saved_tone)
    val savedToneMessage = { title: String -> savedToneTemplate.format(title) }

    // Outside the visibility check on purpose: a download started here keeps
    // running after the dialog closes, and its result still has to arrive.
    com.sysadmindoc.alarmclock.ui.components.YouTubeDownloadResults(
        onDownloaded = { savedTitle ->
            showYouTubeDialog = false
            snackbarScope.launch {
                snackbarHostState.showSnackbar(
                    savedToneMessage(savedTitle),
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

    if (showYouTubeDialog) {
        com.sysadmindoc.alarmclock.ui.components.YouTubeDownloadDialog(
            onDismiss = { showYouTubeDialog = false }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    val alarmDeletedMessage = stringResource(R.string.alarmlist_alarm_deleted)
    val undoLabel = stringResource(R.string.timer_undo)
    LaunchedEffect(state.undoAlarm) {
        state.undoAlarm?.let {
            val result = snackbarHostState.showSnackbar(
                message = alarmDeletedMessage,
                actionLabel = undoLabel,
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
    val visibleAlarmIds = filteredAlarms.map { it.id }
    val currentVisibleAlarmIds by rememberUpdatedState(visibleAlarmIds)
    val canReorderAlarms = !state.isSelectionMode && filteredAlarms.size > 1

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
                        if (state.selectedIds.size == 1) stringResource(R.string.alarmlist_delete_alarm) else stringResource(R.string.alarmlist_delete_alarms, state.selectedIds.size)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
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

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val useTwoPane = !state.isSelectionMode && shouldUseTwoPaneLayout(maxWidth.value)
                val selectedAlarm = filteredAlarms.firstOrNull { it.id == selectedAlarmId }

                LaunchedEffect(useTwoPane, filteredAlarms) {
                    if (!useTwoPane || filteredAlarms.isEmpty()) {
                        selectedAlarmId = null
                    } else if (filteredAlarms.none { it.id == selectedAlarmId }) {
                        selectedAlarmId = filteredAlarms.first().id
                    }
                }

                val alarmListContent: LazyListScope.() -> Unit = {
                item {
                    AlarmHeader(
                        remainingTime = state.remainingTime,
                        hasAlarms = state.nextAlarm != null,
                        alarmCount = state.alarms.size,
                        vacationActive = state.vacationActive,
                        pausedUntilMillis = state.pausedUntilMillis,
                        onResumeAlarms = viewModel::resumeAlarms,
                        sortLabel = when (state.sortOrder) {
                            AlarmSortOrder.TIME -> stringResource(R.string.alarmlist_sort_by_time)
                            AlarmSortOrder.MANUAL -> stringResource(R.string.alarmlist_manual_order)
                            AlarmSortOrder.CREATED -> stringResource(R.string.alarmlist_newest_first)
                            AlarmSortOrder.ENABLED_FIRST -> stringResource(R.string.alarmlist_active_first)
                        },
                        onCycleSort = viewModel::cycleSortOrder,
                    )
                }

                if (state.groups.any { it.isNotBlank() } || state.alarms.size > 3) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (state.groups.any { it.isNotBlank() }) {
                                GroupFilterRow(
                                    title = stringResource(R.string.alarm_list_groups),
                                    groups = state.groups.filter { it.isNotBlank() },
                                    selectedGroup = state.selectedGroup,
                                    onSelectGroup = viewModel::selectGroup
                                )
                            }

                            if (state.profiles.any { it.isNotBlank() }) {
                                GroupFilterRow(
                                    title = stringResource(R.string.alarm_list_profiles),
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
                                        placeholder = { Text(stringResource(R.string.alarm_list_search_placeholder)) },
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
                                        title = stringResource(R.string.alarm_list_empty_title),
                                        description = stringResource(R.string.alarm_list_empty_description),
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
                                        title = stringResource(R.string.alarm_list_no_matches_title),
                                        description = stringResource(R.string.alarm_list_no_matches_description),
                                        footer = {
                                            TextButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    viewModel.selectGroup(null)
                                                    viewModel.selectProfile(null)
                                                }
                                            ) {
                                                Text(stringResource(R.string.alarm_list_clear_filters), color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
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
                                    title = stringResource(R.string.alarm_list_duplicate_time_title),
                                    message = stringResource(
                                        R.string.alarm_list_duplicate_time_message,
                                        timeLabels
                                    ),
                                    icon = Icons.Default.Warning,
                                    color = SnoozeYellow,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(filteredAlarms, key = { it.id }) { alarm ->
                            val isDragging = draggingAlarmId == alarm.id
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetPx else 0f
                                        alpha = if (isDragging) 0.94f else 1f
                                        scaleX = if (isDragging) 1.01f else 1f
                                        scaleY = if (isDragging) 1.01f else 1f
                                    }
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .animateItem(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canReorderAlarms) {
                                    AlarmReorderHandle(
                                        enabled = canReorderAlarms,
                                        alarmLabel = alarm.label.ifBlank { formatAlarmTime(alarm, state.is24HourFormat) },
                                        // v1.13.15: TalkBack-reachable reorder — same persistence path as drag.
                                        onMoveUp = {
                                            val ids = currentVisibleAlarmIds
                                            val index = ids.indexOf(alarm.id)
                                            if (index > 0) {
                                                viewModel.moveAlarm(
                                                    movedAlarmId = alarm.id,
                                                    targetAlarmId = ids[index - 1],
                                                    visibleAlarmIds = ids
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        onMoveDown = {
                                            val ids = currentVisibleAlarmIds
                                            val index = ids.indexOf(alarm.id)
                                            if (index in 0 until ids.lastIndex) {
                                                viewModel.moveAlarm(
                                                    movedAlarmId = alarm.id,
                                                    targetAlarmId = ids[index + 1],
                                                    visibleAlarmIds = ids
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        modifier = Modifier.pointerInput(canReorderAlarms, alarm.id) {
                                            if (canReorderAlarms) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggingAlarmId = alarm.id
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggingAlarmId = null
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggingAlarmId = null
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetPx += dragAmount.y
                                                        val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { it.key == alarm.id }
                                                        val targetInfo = draggedInfo?.let { dragged ->
                                                            val draggedCenter = dragged.offset + (dragged.size / 2) + dragOffsetPx
                                                            listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
                                                                candidate.key is Long &&
                                                                    candidate.key != alarm.id &&
                                                                    draggedCenter >= candidate.offset &&
                                                                    draggedCenter <= candidate.offset + candidate.size
                                                            }
                                                        }
                                                        val targetAlarmId = targetInfo?.key as? Long
                                                        if (targetAlarmId != null) {
                                                            viewModel.moveAlarm(
                                                                movedAlarmId = alarm.id,
                                                                targetAlarmId = targetAlarmId,
                                                                visibleAlarmIds = currentVisibleAlarmIds
                                                            )
                                                            dragOffsetPx = 0f
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Box(modifier = Modifier.weight(1f)) {
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
                                            pausedUntilMillis = state.pausedUntilMillis,
                                            isActivePaneSelection = useTwoPane && selectedAlarmId == alarm.id,
                                            onToggle = { viewModel.toggleAlarm(alarm) },
                                            onForceToggle = { viewModel.forceDisableAlarm(alarm) },
                                            onClick = {
                                                if (useTwoPane) {
                                                    selectedAlarmId = alarm.id
                                                } else {
                                                    onEditAlarm(alarm.id)
                                                }
                                            },
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
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { showTemplates = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.alarm_list_templates))
                                }
                                TextButton(
                                    onClick = onAddAlarm,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.new_alarm))
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
                if (youTubeAvailable) {
                    item {
                        YouTubeDownloadCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = { showYouTubeDialog = true }
                        )
                    }
                }
                }

                val alarmListPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                if (useTwoPane) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .widthIn(min = 360.dp, max = 520.dp)
                                .fillMaxHeight(),
                            contentPadding = alarmListPadding,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = alarmListContent
                        )
                        AlarmDetailPane(
                            alarm = selectedAlarm,
                            is24Hour = state.is24HourFormat,
                            suppressedByVacation = selectedAlarm?.let { alarm ->
                                alarm.isEnabled &&
                                    state.vacationStartMillis > 0L &&
                                    state.vacationEndMillis > state.vacationStartMillis &&
                                    alarm.nextTriggerTime in state.vacationStartMillis..state.vacationEndMillis
                            } == true,
                            pausedUntilMillis = state.pausedUntilMillis,
                            onEdit = { alarm -> onEditAlarm(alarm.id) },
                            onToggle = { alarm -> viewModel.toggleAlarm(alarm) },
                            onForceToggle = { alarm -> viewModel.forceDisableAlarm(alarm) },
                            onDelete = { alarm -> viewModel.deleteAlarm(alarm) },
                            onSkipNext = { alarm -> viewModel.skipNextOccurrence(alarm) },
                            onDuplicate = { alarm -> viewModel.duplicateAlarm(alarm) },
                            onShare = { alarm -> shareAlarm(context, alarm, state.is24HourFormat) },
                            onShowHistory = { alarm ->
                                statsAlarmLabel = alarm.label.ifBlank { "%d:%02d".format(alarm.hour, alarm.minute) }
                                viewModel.loadAlarmStats(alarm.id)
                            },
                            onAddAlarm = onAddAlarm,
                            onBrowseTemplates = { showTemplates = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 18.dp, top = 12.dp, bottom = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = alarmListPadding,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = alarmListContent
                    )
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
                    Text(stringResource(R.string.alarm_edit_create))
                }
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.alarm_list_browse_templates))
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
                    Text(stringResource(R.string.alarm_edit_create))
                }
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.alarm_list_browse_templates))
                }
            }
        }
    }
}

@Composable
private fun AlarmDetailPane(
    alarm: Alarm?,
    is24Hour: Boolean,
    suppressedByVacation: Boolean,
    pausedUntilMillis: Long = 0L,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm) -> Unit,
    onForceToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onSkipNext: (Alarm) -> Unit,
    onDuplicate: (Alarm) -> Unit,
    onShare: (Alarm) -> Unit,
    onShowHistory: (Alarm) -> Unit,
    onAddAlarm: () -> Unit,
    onBrowseTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier.semantics {
            contentDescription = if (alarm == null) {
                "Alarm detail pane"
            } else {
                "Alarm detail pane for ${alarm.label.ifBlank { formatAlarmTime(alarm, is24Hour) }}"
            }
        },
        highlighted = alarm?.isEnabled == true
    ) {
        if (alarm == null) {
            AppEmptyState(
                icon = Icons.Default.AlarmAdd,
                title = stringResource(R.string.alarm_list_detail_empty_title),
                description = stringResource(R.string.alarm_list_detail_empty_description),
                footer = {
                    AlarmListEmptyActions(
                        onAddAlarm = onAddAlarm,
                        onBrowseTemplates = onBrowseTemplates
                    )
                }
            )
            return@AppSurfaceCard
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AppSectionTitle(
                title = alarm.label.ifBlank { "Alarm details" },
                description = if (suppressedByVacation) {
                    "Paused until vacation ends"
                } else {
                    nextOccurrenceLabel(alarm, is24Hour, pausedUntilMillis)
                },
                action = {
                    AppStatusChip(
                        label = if (alarm.isEnabled) stringResource(R.string.alarmlist_enabled) else stringResource(R.string.settings_paused),
                        icon = if (alarm.isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        color = if (alarm.isEnabled) DismissGreen else TextMuted
                    )
                }
            )

            Text(
                text = formatAlarmTime(alarm, is24Hour),
                color = if (alarm.isEnabled) TextPrimary else TextMuted,
                style = ClockTimeSmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (suppressedByVacation) {
                    AppStatusChip(
                        label = stringResource(R.string.alarm_list_paused_by_vacation),
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
                alarm.shiftPatternChipLabel()?.let { label ->
                    AppStatusChip(
                        label = label,
                        color = SnoozeYellow
                    )
                }
                if (alarm.usesFixedTimezone) {
                    AppStatusChip(label = alarm.fixedTimezoneId, color = SnoozeYellow)
                }
                if (alarm.group.isNotBlank()) {
                    AppStatusChip(label = alarm.group)
                }
                alarm.challengeChainLabel()?.let { challengeLabel ->
                    AppStatusChip(label = challengeLabel, color = SnoozeYellow)
                }
                if (alarm.ringtoneUri == "silent") {
                    AppStatusChip(label = stringResource(R.string.alarm_edit_silent), color = TextMuted)
                }
            }

            val alarmToggleDescription = stringResource(
                R.string.alarmlist_alarm,
                alarm.label.ifBlank { formatAlarmTime(alarm, is24Hour) }
            )
            val alarmToggleState = if (alarm.isEnabled) {
                stringResource(R.string.alarmlist_enabled)
            } else {
                stringResource(R.string.alarm_edit_disabled)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .combinedClickable(
                        onClick = { onToggle(alarm) },
                        onLongClick = { if (alarm.isEnabled) onForceToggle(alarm) }
                    )
                    .semantics {
                        contentDescription = alarmToggleDescription
                        stateDescription = alarmToggleState
                        role = Role.Switch
                    },
                shape = RoundedCornerShape(10.dp),
                color = SurfaceMedium,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.alarm_list_alarm_state), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (alarm.isEnabled) stringResource(R.string.alarmlist_tap_to_pause_long_press_to) else stringResource(R.string.alarmlist_tap_to_enable_this_alarm),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = null,
                        colors = appSwitchColors(),
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onEdit(alarm) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.edit_alarm))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onDuplicate(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.alarm_list_duplicate))
                    }
                    OutlinedButton(
                        onClick = { onShare(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.alarm_list_share))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onShowHistory(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.alarm_list_history))
                    }
                    OutlinedButton(
                        onClick = { onDelete(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.alarm_list_delete))
                    }
                }
                if (alarm.isEnabled && alarm.isRecurringSchedule) {
                    OutlinedButton(
                        onClick = { onSkipNext(alarm) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.alarm_list_skip_next_occurrence))
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
    pausedUntilMillis: Long,
    onResumeAlarms: () -> Unit,
    sortLabel: String,
    onCycleSort: () -> Unit,
) {
    val pausedUntilLabel = pausedUntilMillis
        .takeIf { it > 0L }
        ?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }
    AlarmClockHeroHeader(
        title = stringResource(R.string.alarm_list_title),
        subtitle = when {
            pausedUntilLabel != null -> stringResource(R.string.alarmlist_paused_until, pausedUntilLabel)
            hasAlarms && remainingTime.isNotBlank() -> stringResource(R.string.alarmlist_next_alarm, remainingTime)
            alarmCount > 0 -> stringResource(R.string.alarmlist_all_alarms_paused)
            else -> stringResource(R.string.alarmlist_no_alarms_scheduled)
        },
        actions = {
            IconButton(onClick = onCycleSort) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = sortLabel,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        badge = when {
            // Resume lived in Settings only, three screens from where the user
            // sees the effect.
            pausedUntilLabel != null -> {
                {
                    AppStatusChip(
                        label = stringResource(R.string.alarm_list_resume_alarms),
                        icon = Icons.Default.NotificationsActive,
                        color = SnoozeYellow,
                        onClick = onResumeAlarms
                    )
                }
            }

            vacationActive -> {
                {
                    AppStatusChip(
                        label = stringResource(R.string.vacation_mode),
                        icon = Icons.Default.BeachAccess,
                        color = SnoozeYellow
                    )
                }
            }

            else -> null
        }
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
                label = stringResource(R.string.alarm_list_group_all),
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
private fun AlarmReorderHandle(
    enabled: Boolean,
    alarmLabel: String,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    modifier: Modifier = Modifier
) {
    // v1.13.15: WCAG 2.5.7 — expose drag-equivalent moves as accessibility actions.
    val moveUpLabel = stringResource(R.string.alarm_list_move_up)
    val moveDownLabel = stringResource(R.string.alarm_list_move_down)
    val dragHandleDescription = if (enabled) {
        stringResource(R.string.alarmlist_drag_handle_for, alarmLabel)
    } else {
        stringResource(R.string.alarmlist_drag_handle_unavailable)
    }
    val dragHandleState = if (enabled) {
        stringResource(R.string.settings_ready)
    } else {
        stringResource(R.string.alarm_edit_disabled)
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) {
                    SurfaceMedium
                } else {
                    SurfaceMedium.copy(alpha = 0.42f)
                }
            )
            .semantics {
                contentDescription = dragHandleDescription
                stateDescription = dragHandleState
                if (enabled) {
                    customActions = listOf(
                        CustomAccessibilityAction(moveUpLabel) { onMoveUp() },
                        CustomAccessibilityAction(moveDownLabel) { onMoveDown() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else TextMuted
        )
    }
}

@Composable
private fun QuickAlarmRow(
    onQuickAlarm: (Int) -> Unit,
    napDefaultMinutes: Int = 20
) {
    AppSurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        AppSectionTitle(
            title = stringResource(R.string.alarm_list_quick_alarms)
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
                    accessibilityLabel = "Set quick alarm for $label",
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
            text = stringResource(R.string.alarm_list_power_nap),
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
                    label = stringResource(R.string.alarmlist_min, minutes),
                    selected = isDefault,
                    leadingIcon = if (isDefault) Icons.Default.CheckCircle else null,
                    selectionSemantics = false,
                    accessibilityLabel = if (isDefault) {
                        stringResource(R.string.alarmlist_set_power_nap_default, minutes)
                    } else {
                        stringResource(R.string.alarmlist_set_power_nap, minutes)
                    },
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
    pausedUntilMillis: Long = 0L,
    isActivePaneSelection: Boolean = false,
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
    // Resolved outside the semantics lambda, which is not a composable scope.
    val selectedStateDescription = stringResource(R.string.settings_selected)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                if (isActivePaneSelection) {
                    selected = true
                    stateDescription = selectedStateDescription
                }
            },
        shape = shapeTokens.card,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isActivePaneSelection -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                alarm.isEnabled -> SurfaceCard
                else -> SurfaceCard.copy(alpha = 0.55f)
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val alarmToggleLabel = alarm.label.ifBlank { formatAlarmTime(alarm, is24Hour) }
                    val compactToggleDescription =
                        stringResource(R.string.alarmlist_alarm, alarmToggleLabel)
                    val compactToggleState = if (alarm.isEnabled) {
                        stringResource(R.string.alarmlist_enabled)
                    } else {
                        stringResource(R.string.alarm_edit_disabled)
                    }
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onToggle() },
                                onLongClick = { if (alarm.isEnabled) onForceToggle() }
                            )
                            .semantics {
                                contentDescription = compactToggleDescription
                                stateDescription = compactToggleState
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
                                text = { Text(stringResource(R.string.alarm_list_edit)) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_list_duplicate)) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onDuplicate() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_list_share)) },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onShare() }
                            )
                            if (alarm.isEnabled && alarm.isRecurringSchedule) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.alarm_list_skip_next)) },
                                    leadingIcon = { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; onSkipNext() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_list_history)) },
                                leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onShowHistory() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.alarm_list_delete), color = AccentRed) },
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
                    stringResource(R.string.alarmlist_paused_until_vacation_ends)
                } else {
                    nextOccurrenceLabel(alarm, is24Hour, pausedUntilMillis)
                },
                color = if (suppressedByVacation) SnoozeYellow else TextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            val silentLabel = stringResource(R.string.alarm_edit_silent)
            val chainLabel = alarm.challengeChainLabel()
            val metadata = buildList {
                if (alarm.label.isNotBlank() && alarm.repeatLabel.isNotBlank()) add(alarm.repeatLabel)
                alarm.shiftPatternChipLabel()?.let(::add)
                if (alarm.usesFixedTimezone) add(alarm.fixedTimezoneId)
                if (alarm.group.isNotBlank()) add(alarm.group)
                chainLabel?.let(::add)
                if (alarm.ringtoneUri == "silent") add(silentLabel)
            }
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" · "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    // A chain plus a group plus a timezone overruns two lines on
                    // a narrow phone; clip without a marker hid that silently.
                    overflow = TextOverflow.Ellipsis
                )
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
                        Text(stringResource(R.string.alarmlist_selected, selectedCount), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (selectedCount == totalCount) {
                                stringResource(R.string.alarmlist_bulk_scope_all)
                            } else {
                                stringResource(R.string.alarmlist_bulk_scope_selected)
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (selectedCount < totalCount) {
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.alarm_list_select_visible), color = MaterialTheme.colorScheme.primary)
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
                    Text(stringResource(R.string.alarm_list_enable))
                }
                OutlinedButton(
                    onClick = onDisableSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.alarm_list_pause))
                }
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.alarm_list_delete))
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
    val selectionState = if (isSelected) {
        stringResource(R.string.settings_selected)
    } else {
        stringResource(R.string.settings_not_selected)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleSelect)
            .semantics {
                selected = isSelected
                stateDescription = selectionState
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
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.alarmlist_share_alarm))
        )
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.alarmlist_no_share_target),
            Toast.LENGTH_SHORT
        ).show()
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

/**
 * The dismiss challenges this alarm will actually run, in order.
 *
 * A mission chain lives in `challengeChain` and is independent of the single
 * `challengeType`, so a chain on an alarm whose type is NONE used to show no
 * challenge at all on the card, and a chain on a typed alarm showed only the
 * first step.
 */
internal fun Alarm.challengeChainSteps(): List<String> {
    val chain = challengeChain.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (chain.isNotEmpty()) return chain
    return if (challengeType != "NONE") listOf(challengeType) else emptyList()
}

/**
 * Card-sized summary of [challengeChainSteps], or null when there is none.
 *
 * Composable because the step names come from the same resources the alarm
 * editor uses; the private copy this used to keep named the same challenges
 * differently ("Math (Easy)" against "Easy math").
 */
@Composable
internal fun Alarm.challengeChainLabel(): String? {
    val steps = challengeChainSteps()
    return when {
        steps.isEmpty() -> null
        steps.size > 3 ->
            pluralStringResource(R.plurals.alarmlist_challenge_count, steps.size, steps.size)
        // joinToString takes a non-inline lambda, so resolve the names first.
        else -> steps.map { it.toAlarmChallengeSummary() }.joinToString(" · ")
    }
}

private fun Alarm.shiftPatternChipLabel(): String? {
    val pattern = ShiftPattern.fromKey(shiftPattern) ?: return null
    if (shiftPatternStartDate.isBlank()) return null
    return pattern.shortLabel
}

private fun nextOccurrenceLabel(
    alarm: Alarm,
    is24Hour: Boolean,
    pausedUntilMillis: Long = 0L
): String {
    if (alarm.isEnabled && pausedUntilMillis > 0L) {
        // "Pause alarms for N days" zeroes every trigger, so the card used to
        // claim the alarm needed re-enabling by hand. It does not.
        val until = Instant.ofEpochMilli(pausedUntilMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        return "All alarms paused until $until"
    }
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
            containerColor = SurfaceCard
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
                    text = stringResource(R.string.alarm_list_alarm_sounds),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.alarm_list_youtube_downloads),
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
