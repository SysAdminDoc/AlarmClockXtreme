package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.content.Intent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.integration.hue.HueBridgeClient
import com.sysadmindoc.alarmclock.integration.hue.HuePinResult
import com.sysadmindoc.alarmclock.integration.hue.HueTrustStore
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
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
    private val hueBridgeClient: HueBridgeClient,
    private val hueTrustStore: HueTrustStore,
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
        if (v2Result == DismissActionResult.Success ||
            !settings.hueLegacyHttpEnabled ||
            (v2Result is DismissActionResult.Failure &&
                v2Result.reason == "HUE_CERTIFICATE_CHANGED")
        ) {
            return v2Result
        }

        return executeRequest(buildHueSceneRequestV1(bridgeIp, apiKey, target))
    }

    private suspend fun executeHueRequestV2(
        request: Request,
        settings: AppSettings
    ): DismissActionResult {
        val pinnedFingerprint = settings.hueBridgeCertFingerprint
        val tofu = hueBridgeClient.buildTofuClient(pinnedFingerprint)
        val result = runCatching { executeRequest(request, tofu.client) }
            .getOrElse { error ->
                val observed = tofu.trustManager.observedFingerprint
                if (pinnedFingerprint.isNotBlank() &&
                    observed != null &&
                    !pinnedFingerprint.equals(observed, ignoreCase = true)
                ) {
                    return DismissActionResult.Failure("HUE_CERTIFICATE_CHANGED")
                }
                throw error
            }
        if (result == DismissActionResult.Success && pinnedFingerprint.isBlank()) {
            tofu.trustManager.observedFingerprint?.let { observed ->
                when (hueTrustStore.rememberFirstUse(observed)) {
                    is HuePinResult.Accepted -> Unit
                    is HuePinResult.Changed,
                    HuePinResult.Invalid -> {
                        return DismissActionResult.Failure("HUE_CERTIFICATE_CHANGED")
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
            return HueBridgeClient.sanitiseHost(raw)
        }

        internal fun sanitiseToken(raw: String): String? {
            return HueBridgeClient.sanitiseToken(raw)
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
