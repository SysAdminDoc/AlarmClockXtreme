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
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

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
                val requestBuilder = Request.Builder()
                    .url(settings.webhookUrl)
                    .post(body.toRequestBody(JSON))
                    .header("Content-Type", "application/json")
                applySignatureHeaders(
                    builder = requestBuilder,
                    signingSecret = settings.webhookSigningSecret,
                    body = body
                )

                client.newCall(requestBuilder.build()).execute().use { response ->
                    recordDeliveryStatus(
                        event = event,
                        successful = response.isSuccessful,
                        code = response.code,
                        failure = null
                    )
                }
            } catch (e: Exception) {
                recordDeliveryStatus(
                    event = event,
                    successful = false,
                    code = null,
                    failure = e
                )
                // Never propagate — webhook failures must not affect alarm flow
            }
        }
    }

    /** Send a test webhook with event = "test" */
    suspend fun test(
        url: String,
        includeLabel: Boolean = true,
        signingSecret: String = ""
    ): Boolean {
        if (!isAllowedUrl(url)) return false
        if (LocalNetworkPermission.requiresPermissionForUrl(url) &&
            !LocalNetworkPermission.isGranted(context)
        ) {
            return false
        }
        return try {
            val body = buildTestPayloadJson(includeLabel = includeLabel)
            val requestBuilder = Request.Builder()
                .url(url)
                .post(body.toRequestBody(JSON))
                .header("Content-Type", "application/json")
            applySignatureHeaders(
                builder = requestBuilder,
                signingSecret = signingSecret,
                body = body
            )
            withContext(Dispatchers.IO) {
                client.newCall(requestBuilder.build()).execute().use { response ->
                    recordDeliveryStatus(
                        event = WebhookEvent.Test,
                        successful = response.isSuccessful,
                        code = response.code,
                        failure = null
                    )
                    response.isSuccessful
                }
            }
        } catch (e: Exception) {
            recordDeliveryStatus(
                event = WebhookEvent.Test,
                successful = false,
                code = null,
                failure = e
            )
            false
        }
    }

    /** Instance-level alias retained for callers that already use the singleton. */
    fun isAllowedUrl(url: String): Boolean = isAllowedWebhookUrl(url)

    private fun applySignatureHeaders(
        builder: Request.Builder,
        signingSecret: String,
        body: String,
        timestampEpochSeconds: Long = System.currentTimeMillis() / 1000
    ) {
        buildSignatureHeaders(
            signingSecret = signingSecret,
            timestampEpochSeconds = timestampEpochSeconds,
            body = body
        )?.let { headers ->
            builder.header("X-ACX-Timestamp", headers.timestamp)
            builder.header("X-ACX-Signature", headers.signature)
        }
    }

    private suspend fun recordDeliveryStatus(
        event: WebhookEvent,
        successful: Boolean,
        code: Int?,
        failure: Exception?
    ) {
        val status = buildDeliveryStatus(
            event = event,
            successful = successful,
            code = code,
            failure = failure
        )
        runCatching {
            preferencesManager.update {
                it.copy(
                    webhookLastDeliveryStatus = status,
                    webhookLastDeliveryAtMillis = System.currentTimeMillis()
                )
            }
        }
    }

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
        const val SIGNATURE_VERSION = "v1"
        const val SIGNATURE_MAX_SKEW_SECONDS = 5 * 60L

        data class WebhookSignatureHeaders(
            val timestamp: String,
            val signature: String
        )

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

        internal fun buildSignatureHeaders(
            signingSecret: String,
            timestampEpochSeconds: Long,
            body: String
        ): WebhookSignatureHeaders? {
            val secret = signingSecret.trim()
            if (secret.isBlank()) return null
            val signedPayload = "$timestampEpochSeconds.$body"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val digest = mac.doFinal(signedPayload.toByteArray(Charsets.UTF_8)).toLowerHex()
            return WebhookSignatureHeaders(
                timestamp = timestampEpochSeconds.toString(),
                signature = "$SIGNATURE_VERSION=$digest"
            )
        }

        private fun ByteArray.toLowerHex(): String = buildString(size * 2) {
            for (byte in this@toLowerHex) {
                val value = byte.toInt() and 0xff
                append("0123456789abcdef"[value ushr 4])
                append("0123456789abcdef"[value and 0x0f])
            }
        }

        internal fun isSignatureTimestampFresh(
            timestampEpochSeconds: Long,
            nowMillis: Long,
            maxSkewSeconds: Long = SIGNATURE_MAX_SKEW_SECONDS
        ): Boolean {
            if (timestampEpochSeconds <= 0 || maxSkewSeconds < 0) return false
            val nowEpochSeconds = nowMillis / 1000
            return abs(nowEpochSeconds - timestampEpochSeconds) <= maxSkewSeconds
        }

        internal fun buildDeliveryStatus(
            event: WebhookEvent,
            successful: Boolean,
            code: Int?,
            failure: Exception?
        ): String {
            val codeText = code?.let { " ($it)" }.orEmpty()
            return if (successful) {
                "${event.wireName} OK$codeText"
            } else {
                val reason = failure?.javaClass?.simpleName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { ": $it" }
                    .orEmpty()
                "${event.wireName} failed$codeText$reason"
            }
        }

        internal fun buildTestPayloadJson(
            includeLabel: Boolean,
            occurredAtMillis: Long = System.currentTimeMillis(),
            eventId: String = UUID.randomUUID().toString()
        ): String {
            return buildPayloadJson(
                event = WebhookEvent.Test,
                alarmId = 0,
                label = "Test Alarm",
                displayTime = "12:00 PM",
                includeLabel = includeLabel,
                scheduledForMillis = null,
                fireId = null,
                occurredAtMillis = occurredAtMillis,
                eventId = eventId
            )
        }
    }
}
