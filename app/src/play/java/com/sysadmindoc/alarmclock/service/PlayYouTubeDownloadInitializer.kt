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
            // NewPipe drives the in-dialog YouTube search. Init failure here
            // doesn't disable downloads — the URL-paste path still works
            // because it goes through yt-dlp directly. Just log + continue.
            try {
                org.schabi.newpipe.extractor.NewPipe.init(NewPipeDownloader)
            } catch (e: Exception) {
                Log.w("YtDlpInit", "NewPipe init failed; search will be unavailable", e)
            }
            downloader.markInitialized()
        } catch (e: Throwable) {
            Log.w("YtDlpInit", "yt-dlp init failed; YouTube downloads will be disabled", e)
        }
    }
}

/**
 * Minimal Downloader implementation NewPipe Extractor needs. Java's built-in
 * HttpURLConnection — no extra dependency, no shared state with the rest of
 * the app's OkHttp stack. Lifted from Aura's `DownloaderImpl`.
 */
private object NewPipeDownloader : org.schabi.newpipe.extractor.downloader.Downloader() {
    override fun execute(
        request: org.schabi.newpipe.extractor.downloader.Request,
    ): org.schabi.newpipe.extractor.downloader.Response {
        val url = java.net.URL(request.url())
        val conn = url.openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = request.httpMethod()
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"
            )
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            request.headers().forEach { (key, values) ->
                values.forEach { conn.addRequestProperty(key, it) }
            }
            request.dataToSend()?.let { data ->
                conn.doOutput = true
                conn.outputStream.use { it.write(data) }
            }
            val code = conn.responseCode
            val headers = conn.headerFields
                .filterKeys { it != null }
                .mapValues { (_, v) -> v }
            val body = try {
                (if (code < 400) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (_: Exception) { "" }
            return org.schabi.newpipe.extractor.downloader.Response(
                code,
                conn.responseMessage ?: "",
                headers,
                body,
                request.url(),
            )
        } finally {
            conn.disconnect()
        }
    }
}
