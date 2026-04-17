package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * F8: Outbound webhook / Tasker integration.
 *
 * On alarm fire, snooze, or dismiss: POST a JSON payload to the user-configured URL.
 * All errors are caught silently — this must never crash or delay alarm operations.
 */
@Singleton
class WebhookService @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    /** Fire-and-forget webhook call. Must be called inside a coroutine. */
    suspend fun fire(event: String, alarmId: Long, label: String, timeFormatted: String) {
        try {
            val settings = preferencesManager.getCurrentSettings()
            if (!settings.webhookEnabled || settings.webhookUrl.isBlank()) return
            if (!isAllowedUrl(settings.webhookUrl)) return

            val body = buildJson(event, alarmId, label, timeFormatted)
            val request = Request.Builder()
                .url(settings.webhookUrl)
                .post(body.toRequestBody(JSON))
                .header("Content-Type", "application/json")
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { /* consume and close */ }
            }
        } catch (_: Exception) {
            // Never propagate — webhook failures must not affect alarm flow
        }
    }

    /** Send a test webhook with event = "test" */
    suspend fun test(url: String): Boolean {
        if (!isAllowedUrl(url)) return false
        return try {
            val body = buildJson("test", 0, "Test Alarm", "12:00 PM")
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(JSON))
                .header("Content-Type", "application/json")
                .build()
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { it.isSuccessful }
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Instance-level alias retained for callers that already use the singleton. */
    fun isAllowedUrl(url: String): Boolean = isAllowedWebhookUrl(url)

    companion object {
        /**
         * Reject webhook URLs that are not http(s) and reject malformed input early
         * so a typo (e.g. "javascript:" or "file://") never reaches OkHttp's URL
         * parser. Plain http is still allowed because Tasker / local automation
         * servers frequently run on a LAN without TLS, but the Settings screen
         * flags this for the user.
         */
        @JvmStatic
        fun isAllowedWebhookUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isBlank()) return false
            val lower = trimmed.lowercase()
            return (lower.startsWith("http://") || lower.startsWith("https://")) &&
                    trimmed.toHttpUrlOrNull() != null
        }
    }

    private fun buildJson(event: String, alarmId: Long, label: String, time: String): String {
        return org.json.JSONObject().apply {
            put("event", event)
            put("alarmId", alarmId)
            put("label", label)
            put("time", time)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }
}
