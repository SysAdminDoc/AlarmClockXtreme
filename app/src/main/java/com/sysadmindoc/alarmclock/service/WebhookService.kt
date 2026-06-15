package com.sysadmindoc.alarmclock.service

import android.content.Context
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import dagger.hilt.android.qualifiers.ApplicationContext
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class WebhookEvent(val wireName: String) {
    AlarmFired("alarm_fired"),
    AlarmSnoozed("alarm_snoozed"),
    AlarmDismissed("alarm_dismissed"),
    AlarmMissed("alarm_missed"),
    AlarmSkipped("alarm_skipped"),
    Test("test")
}

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
    @ApplicationContext private val context: Context,
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
    fun fireAsync(
        event: WebhookEvent,
        alarmId: Long,
        label: String,
        timeFormatted: String,
        scheduledForMillis: Long? = null,
        fireId: String? = null
    ) {
        webhookScope.launch {
            try {
                val settings = preferencesManager.getCurrentSettings()
                if (!settings.webhookEnabled || settings.webhookUrl.isBlank()) return@launch
                if (!isAllowedUrl(settings.webhookUrl)) return@launch
                if (LocalNetworkPermission.requiresPermissionForUrl(settings.webhookUrl) &&
                    !LocalNetworkPermission.isGranted(context)
                ) {
                    return@launch
                }

                val body = buildPayloadJson(
                    event = event,
                    alarmId = alarmId,
                    label = label,
                    displayTime = timeFormatted,
                    includeLabel = settings.webhookIncludeLabel,
                    scheduledForMillis = scheduledForMillis,
                    fireId = fireId
                )
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
        if (LocalNetworkPermission.requiresPermissionForUrl(url) &&
            !LocalNetworkPermission.isGranted(context)
        ) {
            return false
        }
        return try {
            val body = buildPayloadJson(
                event = WebhookEvent.Test,
                alarmId = 0,
                label = "Test Alarm",
                displayTime = "12:00 PM",
                includeLabel = true,
                scheduledForMillis = null,
                fireId = null
            )
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
         * Reject webhook URLs that are not HTTPS and reject malformed input early
         * so a typo (e.g. "javascript:" or "file://") never reaches OkHttp's URL
         * parser. Plain HTTP is intentionally rejected instead of enabling
         * app-wide cleartext traffic for one integration surface.
         */
        @JvmStatic
        fun isAllowedWebhookUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isBlank()) return false
            val parsed = trimmed.toHttpUrlOrNull() ?: return false
            return parsed.isHttps
        }

        const val PAYLOAD_SCHEMA_VERSION = 1

        private val payloadJsonAdapter = Moshi.Builder()
            .build()
            .adapter<Map<String, Any?>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            .serializeNulls()

        internal fun buildPayloadJson(
            event: WebhookEvent,
            alarmId: Long,
            label: String,
            displayTime: String,
            includeLabel: Boolean,
            scheduledForMillis: Long?,
            fireId: String?,
            occurredAtMillis: Long = System.currentTimeMillis(),
            eventId: String = UUID.randomUUID().toString()
        ): String {
            val payload = linkedMapOf<String, Any?>(
                "schemaVersion" to PAYLOAD_SCHEMA_VERSION,
                "event" to event.wireName,
                "eventId" to eventId,
                "occurredAt" to Instant.ofEpochMilli(occurredAtMillis).toString(),
                "alarmId" to alarmId,
                "scheduledFor" to scheduledForMillis?.let { Instant.ofEpochMilli(it).toString() },
                "displayTime" to displayTime,
                "labelIncluded" to includeLabel
            ).apply {
                if (includeLabel) {
                    put("label", label)
                }
                if (!fireId.isNullOrBlank()) {
                    put("fireId", fireId)
                }
            }
            return payloadJsonAdapter.toJson(payload)
        }
    }
}
