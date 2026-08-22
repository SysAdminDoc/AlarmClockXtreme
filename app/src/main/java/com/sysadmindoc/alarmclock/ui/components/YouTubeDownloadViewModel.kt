package com.sysadmindoc.alarmclock.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the two long jobs behind the YouTube dialog: the download and the
 * downloader-engine update.
 *
 * Both used to run on the dialog's `rememberCoroutineScope`, which dies with
 * the composition. Rotating the phone mid-download cancelled it, and because
 * every field the dialog shows is saved across a rotation, the dialog came back
 * looking exactly as it did with nothing downloading and nothing said. A
 * ViewModel survives the configuration change, so the job does too.
 */
@HiltViewModel
class YouTubeDownloadViewModel @Inject constructor(
    private val downloader: YouTubeAudioDownloader
) : ViewModel() {

    /** What finished while the dialog may or may not have been on screen. */
    internal sealed interface Outcome {
        data class Downloaded(val savedTitle: String) : Outcome
        data class Failed(val error: Throwable, val action: YouTubeDialogAction) : Outcome
    }

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    private val _updatingEngine = MutableStateFlow(false)
    val updatingEngine: StateFlow<Boolean> = _updatingEngine.asStateFlow()

    private val _engineVersion = MutableStateFlow(downloader.engineVersionName())
    val engineVersion: StateFlow<String?> = _engineVersion.asStateFlow()

    private val _engineUpdateMessage = MutableStateFlow<String?>(null)
    val engineUpdateMessage: StateFlow<String?> = _engineUpdateMessage.asStateFlow()

    /**
     * Held as state rather than emitted as an event: a rotation unsubscribes
     * the collector for a moment, and a download that lands in that gap must
     * not be lost. The dialog clears it once it has acted on it.
     */
    private val _outcome = MutableStateFlow<Outcome?>(null)
    internal val outcome: StateFlow<Outcome?> = _outcome.asStateFlow()

    fun download(youtubeUrl: String, displayName: String) {
        // A second tap while one is running would start a competing job whose
        // result overwrites the first.
        if (_downloading.value) return
        _downloading.value = true
        _outcome.value = null
        viewModelScope.launch {
            val result = downloader.downloadAsAlarm(youtubeUrl, displayName)
            _downloading.value = false
            _outcome.value = result.fold(
                onSuccess = { Outcome.Downloaded(it) },
                onFailure = { Outcome.Failed(it, YouTubeDialogAction.Download) }
            )
        }
    }

    fun updateEngine() {
        if (_updatingEngine.value) return
        _updatingEngine.value = true
        _outcome.value = null
        _engineUpdateMessage.value = null
        viewModelScope.launch {
            val result = downloader.updateEngine()
            _updatingEngine.value = false
            result.fold(
                onSuccess = { update ->
                    _engineVersion.value = update.afterVersionName ?: update.beforeVersionName
                    _engineUpdateMessage.value = update.userMessage()
                },
                onFailure = { _outcome.value = Outcome.Failed(it, YouTubeDialogAction.EngineUpdate) }
            )
        }
    }

    fun consumeOutcome() {
        _outcome.value = null
    }

    fun consumeEngineUpdateMessage() {
        _engineUpdateMessage.value = null
    }
}
