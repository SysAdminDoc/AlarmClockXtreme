package com.sysadmindoc.alarmclock.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedAlarmImportUiState(
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SharedAlarmImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedAlarmImportUiState())
    val uiState: StateFlow<SharedAlarmImportUiState> = _uiState.asStateFlow()

    fun saveDraft(
        alarm: Alarm,
        stripRiskyFields: Boolean,
        onSaved: (Long) -> Unit
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = SharedAlarmImportUiState(isSaving = true)
            val candidate = if (stripRiskyFields) {
                AlarmShareCodec.stripRiskyImportedFields(alarm)
            } else {
                alarm
            }
            val imported = AlarmShareCodec.prepareImportedAlarm(
                alarm = candidate,
                defaultLabel = context.getString(R.string.share_default_alarm_label)
            )
            try {
                val id = repository.save(imported)
                _uiState.value = SharedAlarmImportUiState()
                onSaved(id)
            } catch (_: Exception) {
                _uiState.value = SharedAlarmImportUiState(
                    error = "Could not save this shared alarm. Check the link and try again."
                )
            }
        }
    }
}
