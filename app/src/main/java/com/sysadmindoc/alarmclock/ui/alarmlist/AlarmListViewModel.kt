package com.sysadmindoc.alarmclock.ui.alarmlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import com.sysadmindoc.alarmclock.ui.templates.AlarmTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

enum class AlarmSortOrder { TIME, CREATED, ENABLED_FIRST }

private data class SelectionSnapshot(
    val selectedIds: Set<Long>,
    val isSelectionMode: Boolean,
    val undoAlarm: Alarm?,
    val selectedGroup: String?
)

data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarm: Alarm? = null,
    val remainingTime: String = "",
    val vacationActive: Boolean = false,
    val sortOrder: AlarmSortOrder = AlarmSortOrder.TIME,
    val is24HourFormat: Boolean = false,
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val undoAlarm: Alarm? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val calculator: NextAlarmCalculator,
    private val preferencesManager: PreferencesManager,
    private val eventRepository: AlarmEventRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(AlarmSortOrder.TIME)
    private val _selectedGroup = MutableStateFlow<String?>(null)
    private val _undoAlarm = MutableStateFlow<Alarm?>(null)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)

    // Ticker emits every 30s so the remaining-time countdown stays fresh
    private val ticker = flow {
        while (true) {
            emit(Unit)
            kotlinx.coroutines.delay(30_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Unit)

    val uiState: StateFlow<AlarmListUiState> = combine(
        repository.observeAll(),
        repository.observeNextAlarm(),
        preferencesManager.settings,
        _sortOrder,
        combine(ticker, _selectedIds, _isSelectionMode, _undoAlarm, _selectedGroup) { _, sel, mode, undo, group ->
            SelectionSnapshot(sel, mode, undo, group)
        }
    ) { alarms, nextAlarm, settings, sort, snap ->
        val sorted = when (sort) {
            AlarmSortOrder.TIME -> alarms.sortedBy { it.hour * 60 + it.minute }
            AlarmSortOrder.CREATED -> alarms.sortedByDescending { it.id }
            AlarmSortOrder.ENABLED_FIRST -> alarms.sortedByDescending { it.isEnabled }
        }

        // Extract unique groups
        val groups = alarms.map { it.group }.distinct().sorted()

        AlarmListUiState(
            alarms = sorted,
            nextAlarm = nextAlarm,
            remainingTime = if (nextAlarm != null && nextAlarm.nextTriggerTime > 0) {
                calculator.formatRemaining(nextAlarm.nextTriggerTime)
            } else "",
            vacationActive = settings.vacationModeEnabled &&
                    settings.vacationEndMillis > System.currentTimeMillis(),
            sortOrder = sort,
            is24HourFormat = settings.is24HourFormat,
            groups = groups,
            selectedGroup = snap.selectedGroup,
            undoAlarm = snap.undoAlarm,
            selectedIds = snap.selectedIds,
            isSelectionMode = snap.isSelectionMode
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AlarmListUiState()
    )

    fun cycleSortOrder() {
        _sortOrder.value = when (_sortOrder.value) {
            AlarmSortOrder.TIME -> AlarmSortOrder.CREATED
            AlarmSortOrder.CREATED -> AlarmSortOrder.ENABLED_FIRST
            AlarmSortOrder.ENABLED_FIRST -> AlarmSortOrder.TIME
        }
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    // -- Multi-select --

    fun toggleSelection(alarmId: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (alarmId in current) current - alarmId else current + alarmId
        _isSelectionMode.value = _selectedIds.value.isNotEmpty()
    }

    fun selectAll() {
        _selectedIds.value = uiState.value.alarms.map { it.id }.toSet()
        _isSelectionMode.value = _selectedIds.value.isNotEmpty()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            ids.forEach { id ->
                scheduler.cancel(id)
                repository.getById(id)?.let { repository.delete(it) }
            }
            clearSelection()
        }
    }

    fun enableSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val alarm = repository.getById(id) ?: return@forEach
                if (!alarm.isEnabled) {
                    val nextTrigger = calculator.calculate(alarm)
                    repository.setEnabled(id, enabled = true, nextTrigger = nextTrigger)
                    scheduler.schedule(alarm.copy(isEnabled = true, nextTriggerTime = nextTrigger))
                }
            }
            clearSelection()
        }
    }

    fun disableSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                repository.setEnabled(id, enabled = false, nextTrigger = 0)
                scheduler.cancel(id)
            }
            clearSelection()
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newEnabled = !alarm.isEnabled
            if (newEnabled) {
                val nextTrigger = calculator.calculate(alarm)
                repository.setEnabled(alarm.id, enabled = true, nextTrigger = nextTrigger)
                scheduler.schedule(alarm.copy(isEnabled = true, nextTriggerTime = nextTrigger))
            } else {
                repository.setEnabled(alarm.id, enabled = false, nextTrigger = 0)
                scheduler.cancel(alarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            repository.delete(alarm)
            _undoAlarm.value = alarm
        }
    }

    fun undoDelete() {
        val alarm = _undoAlarm.value ?: return
        _undoAlarm.value = null
        viewModelScope.launch {
            val id = repository.save(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm.copy(id = id))
            }
        }
    }

    fun confirmDelete() {
        _undoAlarm.value = null
    }

    /**
     * Duplicate an alarm with a new ID, same settings, enabled by default.
     */
    fun duplicateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val duplicate = alarm.copy(
                id = 0,
                label = if (alarm.label.isBlank()) "Copy" else "${alarm.label} (copy)",
                isEnabled = true,
                createdAt = System.currentTimeMillis(),
                nextTriggerTime = 0
            )
            val id = repository.save(duplicate)
            val saved = duplicate.copy(id = id)
            val nextTrigger = calculator.calculate(saved)
            repository.updateNextTrigger(id, nextTrigger)
            scheduler.schedule(saved.copy(nextTriggerTime = nextTrigger))
        }
    }

    /**
     * Create a quick alarm X minutes from now.
     * One-shot, no repeat, default settings.
     */
    fun createQuickAlarm(minutesFromNow: Int) {
        viewModelScope.launch {
            val triggerTime = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L)
            val instant = Instant.ofEpochMilli(triggerTime)
            val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()

            val alarm = Alarm(
                hour = localTime.hour,
                minute = localTime.minute,
                label = "${minutesFromNow}m quick alarm",
                isEnabled = true,
                repeatDays = emptySet(),
                nextTriggerTime = triggerTime
            )

            val id = repository.save(alarm)
            scheduler.schedule(alarm.copy(id = id))
        }
    }

    /**
     * Create an alarm from a predefined template.
     * Templates with hour=0 are treated as relative timers (minute field = minutes from now).
     */
    fun createFromTemplate(template: AlarmTemplate) {
        viewModelScope.launch {
            val isRelative = template.hour == 0 && template.minute > 0 && template.repeatDays.isEmpty()

            val alarm = if (isRelative) {
                val triggerTime = System.currentTimeMillis() + (template.minute * 60 * 1000L)
                val instant = Instant.ofEpochMilli(triggerTime)
                val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
                Alarm(
                    hour = localTime.hour,
                    minute = localTime.minute,
                    label = template.name,
                    isEnabled = true,
                    repeatDays = emptySet(),
                    gradualVolumeSeconds = template.gradualVolumeSeconds,
                    vibrationEnabled = template.vibrationEnabled,
                    vibrationIntensity = template.vibrationIntensity,
                    snoozeDurationMinutes = template.snoozeDurationMinutes,
                    challengeType = template.challengeType,
                    nextTriggerTime = triggerTime
                )
            } else {
                Alarm(
                    hour = template.hour,
                    minute = template.minute,
                    label = template.name,
                    isEnabled = true,
                    repeatDays = template.repeatDays,
                    gradualVolumeSeconds = template.gradualVolumeSeconds,
                    vibrationEnabled = template.vibrationEnabled,
                    vibrationIntensity = template.vibrationIntensity,
                    snoozeDurationMinutes = template.snoozeDurationMinutes,
                    challengeType = template.challengeType
                )
            }

            val id = repository.save(alarm)
            val saved = alarm.copy(id = id)
            scheduler.schedule(saved)
        }
    }

    /**
     * Skip the next occurrence of a repeating alarm.
     * Cancels the current schedule and reschedules for the occurrence after next.
     */
    fun skipNextOccurrence(alarm: Alarm) {
        if (alarm.repeatDays.isEmpty()) return // Only for repeating alarms
        viewModelScope.launch {
            // Record skip event
            eventRepository.record(
                AlarmEvent(
                    alarmId = alarm.id,
                    alarmLabel = alarm.label,
                    scheduledTime = alarm.nextTriggerTime,
                    firedAt = System.currentTimeMillis(),
                    action = AlarmEvent.ACTION_SKIPPED,
                    actionAt = System.currentTimeMillis(),
                    dayOfWeek = java.time.Instant.ofEpochMilli(alarm.nextTriggerTime)
                        .atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value
                )
            )

            // Cancel current and schedule for the one after next
            scheduler.cancel(alarm.id)
            val afterSkipTime = java.time.Instant.ofEpochMilli(alarm.nextTriggerTime + 60_000)
                .atZone(java.time.ZoneId.systemDefault())
            val afterSkip = calculator.calculate(alarm, afterSkipTime)
            val updated = alarm.copy(nextTriggerTime = afterSkip)
            repository.updateNextTrigger(alarm.id, afterSkip)
            scheduler.schedule(updated)
        }
    }
}
