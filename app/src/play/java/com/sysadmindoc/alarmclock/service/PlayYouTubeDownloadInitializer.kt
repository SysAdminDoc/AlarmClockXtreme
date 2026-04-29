package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unpacks the bundled yt-dlp / Python binaries from assets into the app's
 * private data dir on first launch. Cheap on subsequent launches — the SDK
 * checks for an "installed" marker before re-extracting.
 *
 * Failure here must not crash the app: the YouTube download UI checks
 * [PlayYouTubeAudioDownloader.isAvailable] before letting the user start a
 * download, so a botched init just means the entry point stays disabled.
 */
@Singleton
class PlayYouTubeDownloadInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: PlayYouTubeAudioDownloader,
) : YouTubeDownloadInitializer {

    override suspend fun initialize() {
        try {
            YoutubeDL.getInstance().init(context)
            downloader.markInitialized()
        } catch (e: Throwable) {
            Log.w("YtDlpInit", "yt-dlp init failed; YouTube downloads will be disabled", e)
        }
    }
}
