package com.sysadmindoc.alarmclock.service

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real yt-dlp-backed downloader. Ported from
 * `~/repos/Aura/app/src/main/java/com/freevibe/data/repository/YouTubeRepository.kt`
 * (audio extraction) and `com/freevibe/service/SoundApplier.kt` (MediaStore write).
 *
 * Stripped down for the alarm-clock use case:
 *  - No NewPipe / search — only "paste a URL, get an alarm sound" UX.
 *  - No FFmpeg — the raw `bestaudio` stream is saved as-is. This keeps the APK
 *    smaller and avoids the FFmpeg LD_LIBRARY_PATH gymnastics from Aura.
 *  - No stream-URL cache — alarm tones are downloaded once and reused from
 *    MediaStore, so a 6-hour token cache adds no value here.
 */
@Singleton
class PlayYouTubeAudioDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) : YouTubeAudioDownloader {

    private val initialized = AtomicBoolean(false)

    /**
     * Session-only cache for resolved preview URLs. YouTube's signed audio
     * URLs are valid for ~6 hours; we cache for half that to leave headroom.
     * Bounded so a session that searches all day doesn't grow unbounded.
     */
    private data class CachedStream(val url: String, val cachedAtMs: Long)
    private val previewCache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedStream>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedStream>?,
            ): Boolean = size > 64
        }
    )

    // Independent client — bigger timeouts than the shared NetworkModule
    // OkHttpClient because alarm-tone downloads are larger than holiday-API
    // calls. 30 s connect / 5 min read covers a 30 MB clip on 4G.
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun markInitialized() {
        initialized.set(true)
    }

    override fun isAvailable(): Boolean = initialized.get()

    override suspend fun getPreviewStreamUrl(youtubeUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(isAvailable()) {
                "YouTube engine still warming up — try again in a moment."
            }
            require(isLikelyYouTubeUrl(youtubeUrl)) {
                "That doesn't look like a YouTube URL."
            }
            // Session cache hit?
            previewCache[youtubeUrl]?.let { cached ->
                if (System.currentTimeMillis() - cached.cachedAtMs < PREVIEW_TTL_MS) {
                    return@runCatching cached.url
                }
                previewCache.remove(youtubeUrl)
            }
            // worstaudio = fastest to resolve, smallest to buffer; perfect for preview.
            // CVE-2026-26331 affects callers that enable yt-dlp's --netrc-cmd
            // option. ACX never exposes arbitrary yt-dlp options and only adds
            // this fixed allow-list after validating a whitespace-free YouTube URL.
            val request = YoutubeDLRequest(youtubeUrl).apply {
                addOption("-f", "worstaudio")
                addOption("--get-url")
                addOption("--socket-timeout", "20")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val streamUrl = response.out
                ?.trim()
                ?.lines()
                ?.firstOrNull { it.startsWith("http") }
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "Couldn't get a preview stream. The video may be age-restricted, removed, or region-locked."
                )
            previewCache[youtubeUrl] = CachedStream(streamUrl, System.currentTimeMillis())
            streamUrl
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "preview-url failed for $youtubeUrl", e)
            throw e
        }
    }

    override suspend fun searchAlarmSounds(
        query: String,
        maxDurationSeconds: Int,
    ): Result<List<YouTubeSearchHit>> = withContext(Dispatchers.IO) {
        runCatching {
            require(query.isNotBlank()) { "Type a search like \"rooster crow\" or \"piano bell\"." }
            val service = org.schabi.newpipe.extractor.NewPipe.getService(
                org.schabi.newpipe.extractor.ServiceList.YouTube.serviceId
            )
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                .filter { it.duration in 1..maxDurationSeconds.toLong() }
                .filter { !it.name.contains('#') }
                .take(15)
                .map { item ->
                    YouTubeSearchHit(
                        videoUrl = item.url,
                        title = item.name,
                        uploader = item.uploaderName ?: "",
                        durationSeconds = item.duration,
                    )
                }
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "search failed: $query", e)
            throw e
        }
    }

    override suspend fun downloadAsAlarm(
        youtubeUrl: String,
        displayName: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isAvailable()) {
                throw IllegalStateException("YouTube downloader is still warming up. Try again in a moment.")
            }
            require(isLikelyYouTubeUrl(youtubeUrl)) {
                "That doesn't look like a YouTube URL. Paste a watch link, share link, or shorts URL."
            }

            // Resolve the bestaudio direct URL via yt-dlp (--get-url, no download).
            // Keep this as a fixed option allow-list; do not thread user input
            // into yt-dlp flags such as --netrc-cmd.
            val request = YoutubeDLRequest(youtubeUrl).apply {
                addOption("-f", "bestaudio")
                addOption("--get-url")
                // Reasonable network timeout inside the python layer too.
                addOption("--socket-timeout", "30")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val streamUrl = response.out?.trim()?.lines()?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "Couldn't resolve an audio stream. The video may be age-restricted, removed, or region-locked."
                )

            // Decide a clean file name. Sanitise the user's label down to MediaStore-safe chars.
            val safeName = sanitizeName(displayName)
                .ifBlank { "youtube-alarm-${System.currentTimeMillis()}" }
            val savedName = saveStreamAsAlarm(streamUrl, safeName)
                ?: throw IllegalStateException("Couldn't write the downloaded audio to MediaStore.")
            savedName
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "downloadAsAlarm failed", e)
            throw e
        }
    }

    /**
     * Streams the resolved audio URL into MediaStore.Audio at
     * `Environment.DIRECTORY_ALARMS` with `IS_ALARM=1`, then flips
     * `IS_PENDING=0` so the system clock app + RingtoneManager pick it up.
     *
     * Returns the saved display name on success; `null` on any failure.
     * Mirrors `SoundApplier.saveUrlToMediaStore` in the Aura codebase but
     * inlined and locked to ContentType.ALARM.
     */
    private fun saveStreamAsAlarm(streamUrl: String, baseName: String): String? {
        val displayName = if (baseName.endsWith(".m4a", ignoreCase = true) ||
            baseName.endsWith(".mp3", ignoreCase = true) ||
            baseName.endsWith(".ogg", ignoreCase = true)
        ) {
            baseName
        } else {
            // YouTube bestaudio is overwhelmingly opus-in-webm or AAC-in-m4a; we
            // can't always know without sniffing, so default to .m4a — every
            // Android MediaPlayer can decode it via the system extractor.
            "$baseName.m4a"
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.IS_ALARM, true)
            put(MediaStore.Audio.Media.IS_RINGTONE, false)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_ALARMS)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        val ok = try {
            httpClient.newCall(Request.Builder().url(streamUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code} from audio CDN")
                }
                val body = resp.body ?: throw IllegalStateException("Empty audio body")
                val advertised = body.contentLength()
                if (advertised in 1..Long.MAX_VALUE && advertised > MAX_BYTES) {
                    throw IllegalStateException("Audio is too large (${advertised / (1024 * 1024)} MB)")
                }
                resolver.openOutputStream(uri)?.use { out ->
                    body.byteStream().use { input ->
                        var copied = 0L
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            copied += n
                            if (copied > MAX_BYTES) {
                                throw IllegalStateException("Audio is too large (${copied / (1024 * 1024)} MB)")
                            }
                            out.write(buf, 0, n)
                        }
                    }
                    true
                } ?: false
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                resolver.delete(uri, null, null)
                throw e
            }
            Log.w(TAG, "Stream copy failed", e)
            false
        }

        if (!ok) {
            resolver.delete(uri, null, null)
            return null
        }

        if (Build.VERSION.SDK_INT >= 29) {
            val finalize = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            resolver.update(uri, finalize, null, null)
        }
        return displayName
    }

    companion object {
        private const val TAG = "YtDlpDownloader"

        // 60 MB ceiling — covers ~30 minutes of 256 kbps AAC, more than any
        // reasonable alarm clip needs. Defends against a hostile or
        // mis-resolved CDN URL writing endlessly.
        private const val MAX_BYTES = 60L * 1024 * 1024

        // Half of YouTube's typical 6-hour signed-URL TTL.
        private const val PREVIEW_TTL_MS = 3L * 60 * 60 * 1000

        private val URL_REGEX = Regex(
            "^https?://(www\\.|m\\.|music\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/\\S+",
            RegexOption.IGNORE_CASE
        )

        // MediaStore display names tolerate most filename characters but slashes
        // and control chars are unsafe; collapse anything outside a safe set.
        private val UNSAFE = Regex("[^A-Za-z0-9 ._\\-()]+")

        fun isLikelyYouTubeUrl(url: String): Boolean = URL_REGEX.matches(url.trim())

        fun sanitizeName(raw: String): String =
            raw.trim()
                .replace(UNSAFE, " ")
                .replace(Regex(" +"), " ")
                .trim()                  // trim again after unsafe-char collapse
                .take(80)
                .lowercase(Locale.ROOT)
                .replace(' ', '-')
    }
}
