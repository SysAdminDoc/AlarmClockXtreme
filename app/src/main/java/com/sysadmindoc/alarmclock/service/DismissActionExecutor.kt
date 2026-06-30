package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.content.Intent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import com.sysadmindoc.alarmclock.worker.HueSunriseWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed class DismissActionResult {
    data object Skipped : DismissActionResult()
    data object Success : DismissActionResult()
    data class Failure(val reason: String) : DismissActionResult()
}

@Singleton
class DismissActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val client: OkHttpClient,
    moshi: Moshi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = "application/json".toMediaType()
    private val payloadAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    ).serializeNulls()

    fun executeAsync(alarm: Alarm) {
        scope.launch { execute(alarm) }
    }

    internal suspend fun execute(alarm: Alarm): DismissActionResult = withContext(Dispatchers.IO) {
        val type = alarm.dismissActionType.trim().uppercase(Locale.US)
        val payload = alarm.dismissActionPayload.trim()
        if (type == "NONE" || payload.isBlank()) return@withContext DismissActionResult.Skipped

        runCatching {
            when (type) {
                "WEBHOOK" -> executeWebhook(alarm, payload)
                "BROADCAST" -> executeBroadcast(alarm, payload)
                "HUE_SCENE" -> executeHueScene(payload)
                else -> DismissActionResult.Skipped
            }
        }.getOrElse { error ->
            DismissActionResult.Failure(error.message ?: error::class.java.simpleName)
        }
    }

    private fun executeWebhook(alarm: Alarm, url: String): DismissActionResult {
        if (!WebhookService.isAllowedWebhookUrl(url)) {
            return DismissActionResult.Failure("WEBHOOK_URL_REJECTED")
        }
        if (LocalNetworkPermission.requiresPermissionForUrl(url) &&
            !LocalNetworkPermission.isGranted(context)
        ) {
            return DismissActionResult.Failure("LOCAL_NETWORK_PERMISSION_MISSING")
        }

        val body = payloadAdapter.toJson(
            linkedMapOf(
                "schemaVersion" to 1,
                "event" to "dismiss_action",
                "alarmId" to alarm.id,
                "label" to alarm.label,
                "occurredAt" to java.time.Instant.now().toString()
            )
        )
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(json))
            .header("Content-Type", "application/json")
            .build()
        return executeRequest(request)
    }

    private fun executeBroadcast(alarm: Alarm, action: String): DismissActionResult {
        if (!isAllowedBroadcastAction(action)) {
            return DismissActionResult.Failure("BROADCAST_ACTION_REJECTED")
        }
        context.sendBroadcast(buildBroadcastIntent(context, alarm, action))
        return DismissActionResult.Success
    }

    private suspend fun executeHueScene(payload: String): DismissActionResult {
        val settings = preferencesManager.getCurrentSettings()
        if (LocalNetworkPermission.isRuntimeRequired() && !LocalNetworkPermission.isGranted(context)) {
            return DismissActionResult.Failure("LOCAL_NETWORK_PERMISSION_MISSING")
        }
        val bridgeIp = sanitiseHost(settings.hueBridgeIp)
            ?: return DismissActionResult.Failure("HUE_BRIDGE_REJECTED")
        val apiKey = sanitiseToken(settings.hueApiKey)
            ?: return DismissActionResult.Failure("HUE_API_KEY_REJECTED")
        val target = HueSceneTarget.parse(payload)
            ?: return DismissActionResult.Failure("HUE_SCENE_REJECTED")

        val v2Request = buildHueSceneRequestV2(bridgeIp, apiKey, target.sceneId)
        val v2Result = executeHueRequestV2(v2Request, settings)
        if (v2Result == DismissActionResult.Success || !settings.hueLegacyHttpEnabled) {
            return v2Result
        }

        return executeRequest(buildHueSceneRequestV1(bridgeIp, apiKey, target))
    }

    private suspend fun executeHueRequestV2(
        request: Request,
        settings: AppSettings
    ): DismissActionResult {
        val pinnedFingerprint = settings.hueBridgeCertFingerprint
        val tofuTm = HueSunriseWorker.TofuTrustManager(pinnedFingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(tofuTm), SecureRandom())
        }
        val v2Client = client.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, tofuTm)
            .hostnameVerifier { _, _ -> true }
            .build()

        val result = executeRequest(request, v2Client)
        if (result == DismissActionResult.Success && pinnedFingerprint.isBlank()) {
            tofuTm.observedFingerprint?.let { observed ->
                preferencesManager.update { current ->
                    if (current.hueBridgeCertFingerprint.isBlank()) {
                        current.copy(hueBridgeCertFingerprint = observed)
                    } else {
                        current
                    }
                }
            }
        }
        return result
    }

    private fun executeRequest(
        request: Request,
        requestClient: OkHttpClient = client
    ): DismissActionResult {
        requestClient.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                DismissActionResult.Success
            } else {
                DismissActionResult.Failure("HTTP_${response.code}")
            }
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()

        internal fun buildBroadcastIntent(context: Context, alarm: Alarm, action: String): Intent =
            Intent(action.trim()).apply {
                putExtra("alarmId", alarm.id)
                putExtra("label", alarm.label)
                setPackage(context.packageName)
            }

        internal fun buildHueSceneRequestV2(bridgeIp: String, apiKey: String, sceneId: String): Request {
            val body = """{"recall":{"action":"active"}}"""
            return Request.Builder()
                .url("https://$bridgeIp/clip/v2/resource/scene/$sceneId")
                .header("hue-application-key", apiKey)
                .put(body.toRequestBody(JSON))
                .build()
        }

        internal fun buildHueSceneRequestV1(
            bridgeIp: String,
            apiKey: String,
            target: HueSceneTarget
        ): Request {
            val body = """{"scene":"${target.sceneId}"}"""
            return Request.Builder()
                .url("http://$bridgeIp/api/$apiKey/groups/${target.groupId}/action")
                .put(body.toRequestBody(JSON))
                .build()
        }

        internal fun isAllowedBroadcastAction(action: String): Boolean {
            val trimmed = action.trim()
            if (trimmed.length !in 1..200) return false
            return Regex("^[A-Za-z][A-Za-z0-9_.-]*$").matches(trimmed)
        }

        internal fun sanitiseHost(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            val pattern = Regex("^[A-Za-z0-9.\\-]{1,253}(:\\d{1,5})?$")
            return if (pattern.matches(trimmed)) trimmed else null
        }

        internal fun sanitiseToken(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            val pattern = Regex("^[A-Za-z0-9_\\-]{1,128}$")
            return if (pattern.matches(trimmed)) trimmed else null
        }
    }
}

data class HueSceneTarget(
    val sceneId: String,
    val groupId: String = "0"
) {
    companion object {
        fun parse(raw: String): HueSceneTarget? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            val parts = trimmed.split(":", limit = 2)
            return when (parts.size) {
                1 -> sanitiseToken(parts[0])?.let { HueSceneTarget(sceneId = it) }
                2 -> {
                    val group = sanitiseToken(parts[0]) ?: return null
                    val scene = sanitiseToken(parts[1]) ?: return null
                    HueSceneTarget(sceneId = scene, groupId = group)
                }
                else -> null
            }
        }

        private fun sanitiseToken(raw: String): String? =
            DismissActionExecutor.sanitiseToken(raw)
    }
}
