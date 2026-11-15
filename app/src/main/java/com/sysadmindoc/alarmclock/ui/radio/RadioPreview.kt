package com.sysadmindoc.alarmclock.ui.radio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sysadmindoc.alarmclock.service.AlarmAudioRouting
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Plays a station before it's chosen, on the alarm stream and through the same
 * Media3 stack the alarm uses, so a station that previews is one that rings.
 * Main thread only, like the player behind it.
 */
interface RadioPreview {

    sealed interface State {
        data object Idle : State
        data class Loading(val url: String) : State
        data class Playing(val url: String) : State
        data class Failed(val url: String) : State
    }

    val state: StateFlow<State>

    fun play(url: String)

    fun stop()
}

internal val RadioPreview.State.streamUrl: String?
    get() = when (this) {
        RadioPreview.State.Idle -> null
        is RadioPreview.State.Loading -> url
        is RadioPreview.State.Playing -> url
        is RadioPreview.State.Failed -> url
    }

class ExoRadioPreview @Inject constructor(
    @ApplicationContext private val context: Context
) : RadioPreview {

    private val _state = MutableStateFlow<RadioPreview.State>(RadioPreview.State.Idle)
    override val state: StateFlow<RadioPreview.State> = _state.asStateFlow()

    private var player: ExoPlayer? = null

    override fun play(url: String) {
        release()
        val exo = ExoPlayer.Builder(context).build()
        player = exo
        _state.value = RadioPreview.State.Loading(url)
        exo.setAudioAttributes(AlarmAudioRouting.media3AlarmMusicAttributes(), false)
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // A listener from a player already replaced by the next tap.
                if (player !== exo) return
                when (playbackState) {
                    Player.STATE_READY -> _state.value = RadioPreview.State.Playing(url)
                    Player.STATE_ENDED -> stop()
                    Player.STATE_BUFFERING, Player.STATE_IDLE -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (player !== exo) return
                release()
                _state.value = RadioPreview.State.Failed(url)
            }
        })
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.playWhenReady = true
        exo.prepare()
    }

    override fun stop() {
        release()
        _state.value = RadioPreview.State.Idle
    }

    private fun release() {
        player?.release()
        player = null
    }
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class RadioPreviewModule {
    // Unscoped: every view model gets its own player and releases it in onCleared.
    @Binds
    abstract fun bindRadioPreview(impl: ExoRadioPreview): RadioPreview
}
