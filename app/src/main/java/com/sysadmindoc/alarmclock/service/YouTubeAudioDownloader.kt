package com.sysadmindoc.alarmclock.service

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

interface YouTubeAudioDownloader {
    /** True when the underlying engine is present and initialised. */
    fun isAvailable(): Boolean

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
}

/**
 * One-shot startup hook for any flavor-specific initialisation
 * (yt-dlp's binary unpack, FFmpeg init, etc.). Called from
 * [com.sysadmindoc.alarmclock.AlarmClockApp.onCreate] off the main thread.
 */
interface YouTubeDownloadInitializer {
    suspend fun initialize()
}
