package com.sysadmindoc.alarmclock.service

import javax.inject.Inject
import javax.inject.Singleton

/**
 * F-Droid stub. The yt-dlp library bundles a native Python interpreter that
 * isn't FOSS-compatible, so the f-droid flavor deliberately omits it. The UI
 * checks [isAvailable] and hides the "Download from YouTube" entry point on
 * this flavor — but the interface still resolves so the rest of the app
 * compiles cleanly.
 */
@Singleton
class FdroidYouTubeAudioDownloader @Inject constructor() : YouTubeAudioDownloader {
    override fun isAvailable(): Boolean = false

    override suspend fun downloadAsAlarm(youtubeUrl: String, displayName: String): Result<String> =
        Result.failure(
            UnsupportedOperationException(
                "YouTube downloads aren't available in the F-Droid build. Install the GitHub release if you need this feature."
            )
        )

    override suspend fun searchAlarmSounds(
        query: String,
        maxDurationSeconds: Int,
    ): Result<List<YouTubeSearchHit>> =
        Result.failure(
            UnsupportedOperationException(
                "YouTube search isn't available in the F-Droid build."
            )
        )
}

@Singleton
class FdroidYouTubeDownloadInitializer @Inject constructor() : YouTubeDownloadInitializer {
    override suspend fun initialize() { /* no-op */ }
}
