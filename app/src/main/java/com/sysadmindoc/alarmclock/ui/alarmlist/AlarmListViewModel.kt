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
import com.sysadmindoc.alarmclock.domain.VacationAlarmPolicy
import com.sysadmindoc.alarmclock.ui.templates.AlarmTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val isSelectionMode: Boolean = false,
    val napDefaultMinutes: Int = 20,
    // v1.5.2: Current vacation window bounds surfaced so the list card can
    // flag individual alarms whose next trigger falls inside it — before
    // this, the scheduler silently suppressed them while the UI still said
    // "Next alarm in 3 days". 0/0 = no active window.
    val vacationStartMillis: Long = 0L,
    val vacationEndMillis: Long = 0L
)

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val calculator: NextAlarmCalculator,
    private val preferencesManager: PreferencesManager,
    private val eventRepository: AlarmEventRepository
) : ViewModel() {

    /** One-shot events for polished list action feedback. */
    private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val feedbackEvents: SharedFlow<String> = _feedback.asSharedFlow()

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
        val filtered = if (snap.selectedGroup.isNullOrBlank()) {
            alarms
        } else {
            alarms.filter { it.group == snap.selectedGroup }
        }

        val sorted = when (sort) {
            AlarmSortOrder.TIME -> filtered.sortedBy { it.hour * 60 + it.minute }
            AlarmSortOrder.CREATED -> filtered.sortedByDescending { it.id }
            AlarmSortOrder.ENABLED_FIRST -> filtered.sortedWith(
                compareByDescending<Alarm> { it.isEnabled }.thenBy { it.hour * 60 + it.minute }
            )
        }

        // Extract unique groups from all alarms (not filtered), hiding the
        // empty/default group so the chip row never gets a blank filter.
        val groups = alarms.map { it.group.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val now = System.currentTimeMillis()

        AlarmListUiState(
            alarms = sorted,
            nextAlarm = nextAlarm,
            remainingTime = if (nextAlarm != null && nextAlarm.nextTriggerTime > 0) {
                calculator.formatRemaining(nextAlarm.nextTriggerTime)
            } else "",
            vacationActive = VacationAlarmPolicy.isActive(settings, now),
            sortOrder = sort,
            is24HourFormat = settings.is24HourFormat,
            groups = groups,
            selectedGroup = snap.selectedGroup,
            undoAlarm = snap.undoAlarm,
            selectedIds = snap.selectedIds,
            isSelectionMode = snap.isSelectionMode,
            napDefaultMinutes = settings.napDefaultMinutes,
            vacationStartMillis = if (VacationAlarmPolicy.hasConfiguredWindow(settings)) {
                settings.vacationStartMillis
            } else 0L,
            vacationEndMillis = if (VacationAlarmPolicy.hasConfiguredWindow(settings)) {
                settings.vacationEndMillis
            } else 0L
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

    fun selectMany(alarmIds: Set<Long>) {
        _selectedIds.value = alarmIds
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
            scheduler.syncBedtimeDndRule()
            clearSelection()
            if (ids.isNotEmpty()) {
                emitFeedback(
                    if (ids.size == 1) {
                        "1 alarm deleted"
                    } else {
                        "${ids.size} alarms deleted"
                    }
                )
            }
        }
    }

    fun enableSelected() {
        viewModelScope.launch {
            var enabledCount = 0
            _selectedIds.value.forEach { id ->
                val alarm = repository.getById(id) ?: return@forEach
                if (!alarm.isEnabled) {
                    val nextTrigger = calculator.calculate(alarm)
                    repository.setEnabled(id, enabled = true, nextTrigger = nextTrigger)
                    scheduler.schedule(alarm.copy(isEnabled = true, nextTriggerTime = nextTrigger))
                    enabledCount += 1
                }
            }
            clearSelection()
            if (enabledCount > 0) {
                emitFeedback(
                    if (enabledCount == 1) {
                        "1 alarm enabled"
                    } else {
                        "$enabledCount alarms enabled"
                    }
                )
            }
        }
    }

    fun disableSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            _selectedIds.value.forEach { id ->
                repository.setEnabled(id, enabled = false, nextTrigger = 0)
                scheduler.cancel(id)
            }
            scheduler.syncBedtimeDndRule()
            clearSelection()
            if (ids.isNotEmpty()) {
                emitFeedback(
                    if (ids.size == 1) {
                        "1 alarm paused"
                    } else {
                        "${ids.size} alarms paused"
                    }
                )
            }
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newEnabled = !alarm.isEnabled
            if (newEnabled) {
                val nextTrigger = calculator.calculate(alarm)
                repository.setEnabled(alarm.id, enabled = true, nextTrigger = nextTrigger)
                scheduler.schedule(alarm.copy(isEnabled = true, nextTriggerTime = nextTrigger))
                emitFeedback("Alarm enabled for ${formatFeedbackTime(nextTrigger)}")
            } else {
                repository.setEnabled(alarm.id, enabled = false, nextTrigger = 0)
                scheduler.cancel(alarm.id)
                scheduler.syncBedtimeDndRule()
                emitFeedback("Alarm paused")
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            repository.delete(alarm)
            scheduler.syncBedtimeDndRule()
            _undoAlarm.value = alarm
        }
    }

    fun undoDelete() {
        val alarm = _undoAlarm.value ?: return
        _undoAlarm.value = null
        viewModelScope.launch {
            val id = repository.save(alarm)
            val restored = alarm.copy(id = id)
            if (restored.isEnabled) {
                if (restored.nextTriggerTime > System.currentTimeMillis()) {
                    scheduler.scheduleAt(restored, restored.nextTriggerTime)
                } else {
                    scheduler.schedule(restored)
                }
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
            emitFeedback(
                if (duplicate.label.isBlank()) {
                    "Alarm duplicated"
                } else {
                    "\"${duplicate.label}\" duplicated"
                }
            )
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
            scheduler.scheduleAt(alarm.copy(id = id), triggerTime)
            emitFeedback("Quick alarm set for ${formatFeedbackTime(triggerTime)}")
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
            if (isRelative) {
                scheduler.scheduleAt(saved, saved.nextTriggerTime)
            } else {
                scheduler.schedule(saved)
            }
            emitFeedback(
                if (isRelative) {
                    "${template.name} starts in ${template.minute} minutes"
                } else {
                    "${template.name} scheduled for ${formatTemplateTime(template)}"
                }
            )
        }
    }

    /**
     * Skip the next occurrence of a repeating alarm.
     * Cancels the current schedule and reschedules for the occurrence after next.
     */
    fun skipNextOccurrence(alarm: Alarm) {
        if (alarm.repeatDays.isEmpty()) return // Only for repeating alarms
        if (alarm.nextTriggerTime <= 0L) return // No scheduled occurrence to skip
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
            scheduler.scheduleAt(updated, afterSkip)

            val skippedDate = Instant.ofEpochMilli(afterSkip)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a"))
            emitFeedback("Next occurrence skipped — resuming $skippedDate")
        }
    }

    private fun emitFeedback(message: String) {
        _feedback.tryEmit(message)
    }

    private fun formatFeedbackTime(triggerTime: Long): String {
        return Instant.ofEpochMilli(triggerTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    private fun formatTemplateTime(template: AlarmTemplate): String {
        return Instant.ofEpochMilli(
            System.currentTimeMillis()
        ).atZone(ZoneId.systemDefault()).withHour(template.hour).withMinute(template.minute)
            .withSecond(0)
            .withNano(0)
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }
}
