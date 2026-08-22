package com.sysadmindoc.alarmclock.service

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R

/**
 * Downloads YouTube audio and saves it as a system alarm tone (visible in the
 * device's MediaStore under Alarms/).
 *
 * Two implementations exist:
 *  - **play flavor** (`PlayYouTubeAudioDownloader`): real downloader backed by
 *    yt-dlp + OkHttp. Saves to `MediaStore.Audio.Media` with `IS_ALARM=1` and
 *    `RELATIVE_PATH=DIRECTORY_ALARMS` so the saved file shows up in
 *    [com.sysadmindoc.alarmclock.ui.ringtone.RingtonePickerSheet] without any
 *    extra wiring — the picker already enumerates `RingtoneManager.TYPE_ALARM`.
 *  - **fdroid flavor** (`FdroidYouTubeAudioDownloader`): no-op stub that
 *    returns a clear "not available in this build" failure. The fdroid build
 *    deliberately excludes the youtubedl-android library (it bundles a native
 *    Python interpreter that isn't FOSS-compatible).
 *
 * UI checks [isAvailable] to hide the entry point on f-droid builds.
 */
/** A single YouTube hit returned by [YouTubeAudioDownloader.searchAlarmSounds]. */
data class YouTubeSearchHit(
    val videoUrl: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
)

enum class YouTubeEngineUpdateState {
    Updated,
    AlreadyCurrent
}

data class YouTubeEngineUpdateResult(
    val state: YouTubeEngineUpdateState,
    val beforeVersionName: String?,
    val afterVersionName: String?,
) {
    /**
     * The message id for this outcome, and [afterVersionName] as its argument
     * when there is one.
     *
     * A resource id rather than a sentence: this is a data class with no
     * Context, and its message is what the download dialog's status line
     * shows. Resolve it with `context.getString(userMessageRes, ...)`.
     */
    @get:StringRes
    val userMessageRes: Int get() = when (state) {
        YouTubeEngineUpdateState.Updated ->
            if (afterVersionName != null) {
                R.string.youtube_engine_updated_to
            } else {
                R.string.youtube_engine_updated
            }
        YouTubeEngineUpdateState.AlreadyCurrent ->
            if (afterVersionName != null) {
                R.string.youtube_engine_current
            } else {
                R.string.youtube_engine_already_current
            }
    }
}

interface YouTubeAudioDownloader {
    /** True when the underlying engine is present and initialised. */
    fun isAvailable(): Boolean

    /**
     * Best-effort local yt-dlp version label. Must not perform network I/O;
     * used only for user-facing diagnostics in the Play-flavor downloader UI.
     */
    fun engineVersionName(): String? = null

    companion object {
        const val MIN_SAFE_VERSION = "2026.06.09"
    }

    /**
     * User-triggered updater for the bundled yt-dlp engine. Implementations
     * must keep the old engine in place if the update fails.
     */
    suspend fun updateEngine(): Result<YouTubeEngineUpdateResult>

    suspend fun resetEngine(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Not available in this build"))

    /**
     * Download the best-quality audio track from a YouTube URL and save it as
     * an alarm in MediaStore. Returns success with the new file's name (which
     * the user will then see in the ringtone picker), or failure with a
     * human-readable error message.
     */
    suspend fun downloadAsAlarm(youtubeUrl: String, displayName: String): Result<String>

    /**
     * Search YouTube (no API key, no quotas) for short clips that make sense
     * as alarm sounds. Filters to videos under [maxDurationSeconds] so a 90
     * minute reaction video doesn't show up alongside a 12-second rooster.
     * Stub on f-droid — returns failure with a clear message.
     */
    suspend fun searchAlarmSounds(
        query: String,
        maxDurationSeconds: Int = 240,
    ): Result<List<YouTubeSearchHit>>

    /**
     * Resolve a directly-playable streaming URL for [youtubeUrl]. Uses the
     * lowest-bitrate audio track so the preview starts fast (~1-3 s) and
     * doesn't burn data. The returned URL is signed and time-limited (~6 h);
     * callers should re-resolve if they cached one and it's stale. Stub on
     * f-droid.
     */
    suspend fun getPreviewStreamUrl(youtubeUrl: String): Result<String>
}

/**
 * One-shot startup hook for any flavor-specific initialisation
 * (yt-dlp's binary unpack, FFmpeg init, etc.). Called from
 * [com.sysadmindoc.alarmclock.AlarmClockApp.onCreate] off the main thread.
 */
interface YouTubeDownloadInitializer {
    suspend fun initialize()
}
