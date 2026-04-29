package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 *
 * v1.6.3: The HTTP call runs on an application-lived [SupervisorJob] scope
 * (instead of being launched inside [AlarmService.serviceScope]). The previous
 * design was racing `stopSelf()`: the dismiss / snooze handlers called
 * `serviceScope.launch { webhookService.fire(...) }` and then immediately
 * called `stopSelf()`, which triggered `onDestroy()` → `serviceScope.cancel()`
 * — so the 5-second OkHttp call was very often cancelled before the request
 * left the device. Tasker integrations were missing the "dismissed" event for
 * users with slow connections.
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

    // Application-lived scope. SupervisorJob so a crash in one fire doesn't
    // cancel the next one; Dispatchers.IO since OkHttp.execute() is blocking.
    // Never cancelled — the process tear-down releases all child jobs.
    private val webhookScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fire-and-forget webhook call. Caller does NOT need to await — the call
     * runs on an application-lived scope so the AlarmService can stopSelf()
     * immediately without cancelling the request.
     */
    fun fireAsync(event: String, alarmId: Long, label: String, timeFormatted: String) {
        webhookScope.launch {
            try {
                val settings = preferencesManager.getCurrentSettings()
                if (!settings.webhookEnabled || settings.webhookUrl.isBlank()) return@launch
                if (!isAllowedUrl(settings.webhookUrl)) return@launch

                val body = buildJson(event, alarmId, label, timeFormatted)
                val request = Request.Builder()
                    .url(settings.webhookUrl)
                    .post(body.toRequestBody(JSON))
                    .header("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { /* consume and close */ }
            } catch (_: Exception) {
                // Never propagate — webhook failures must not affect alarm flow
            }
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
