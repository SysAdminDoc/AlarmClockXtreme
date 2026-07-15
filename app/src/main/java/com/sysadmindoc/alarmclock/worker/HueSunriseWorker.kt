package com.sysadmindoc.alarmclock.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.integration.hue.HueBridgeClient
import com.sysadmindoc.alarmclock.integration.hue.HuePinResult
import com.sysadmindoc.alarmclock.integration.hue.HueTrustStore
import com.sysadmindoc.alarmclock.integration.hue.HueV2ProbeResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * F15: Philips Hue sunrise simulation.
 * Gradually ramps up warm-white light from 0->254 brightness over [huePreWakeMinutes] minutes.
 * Input: KEY_ALARM_ID. Reads bridge IP, API key, and comma-separated light IDs from preferences.
 *
 * v1.11.5 (roadmap N5): API v2 (HTTPS, header auth, CLIP v2 resource shape)
 * with an explicit legacy-v1 fallback. Each run proves v2 reachability and the
 * current certificate before sending light commands; a pin mismatch can never
 * downgrade to plain HTTP.
 *
 * v1.14.x: TOFU (Trust On First Use) certificate pinning for v2 HTTPS.
 * On first successful connection, the bridge cert SHA-256 fingerprint is saved
 * to DataStore. Subsequent connections reject certificate changes, surfacing
 * a blocking warning in Settings. v1 HTTP is behind an explicit legacy toggle
 * (default off) and will be removed in a future release.
 */
@HiltWorker
class HueSunriseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val hueBridgeClient: HueBridgeClient,
    private val hueTrustStore: HueTrustStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        private val JSON = "application/json".toMediaType()
        private const val STEPS = 20          // brightness increments
        private const val WARM_CT = 500       // ~2000K warm white (Hue range 153-500)

    }

    private val httpV1: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1)
        if (alarmId < 0) return Result.failure()

        val alarm = repository.getById(alarmId) ?: return Result.failure()
        if (!alarm.hueEnabled) return Result.success()

        val settings = preferencesManager.getCurrentSettings()
        val bridgeIp = HueBridgeClient.sanitiseHost(settings.hueBridgeIp)
            ?: return Result.failure()
        val apiKey = HueBridgeClient.sanitiseToken(settings.hueApiKey)
            ?: return Result.failure()
        val lightIds = settings.hueLightIds
            .split(",")
            .map { HueBridgeClient.sanitiseToken(it.trim()) }
            .filterNotNull()
        if (lightIds.isEmpty()) return Result.failure()

        val v2Probe = hueBridgeClient.probeV2(
            bridgeHost = bridgeIp,
            apiKey = apiKey,
            resourcePath = "light/${lightIds.first()}",
            pinnedFingerprint = settings.hueBridgeCertFingerprint
        )
        val effectivePin = when (v2Probe) {
            is HueV2ProbeResult.Reachable -> when (
                val pin = hueTrustStore.rememberFirstUse(v2Probe.observedFingerprint)
            ) {
                is HuePinResult.Accepted -> pin.fingerprint
                is HuePinResult.Changed,
                HuePinResult.Invalid -> return Result.failure()
            }
            is HueV2ProbeResult.CertificateChanged -> return Result.failure()
            is HueV2ProbeResult.Failed -> null
        }
        val useV2 = effectivePin != null
        if (!useV2 && !settings.hueLegacyHttpEnabled) return Result.failure()
        val v2Client = effectivePin?.let { hueBridgeClient.buildTofuClient(it).client }

        val totalMs = alarm.huePreWakeMinutes * 60_000L
        val stepMs = totalMs / STEPS

        if (lightIds.any { id ->
                !putLightState(useV2, v2Client, bridgeIp, apiKey, id, on = true, bri = 1, ct = WARM_CT)
            }
        ) {
            return Result.failure()
        }

        for (step in 1..STEPS) {
            delay(stepMs)
            val bri = (step * 254 / STEPS).coerceIn(1, 254)
            if (lightIds.any { id ->
                    !putLightState(useV2, v2Client, bridgeIp, apiKey, id, on = true, bri = bri, ct = WARM_CT)
                }
            ) {
                return Result.failure()
            }
        }

        return Result.success()
    }

    private fun putLightState(
        useV2: Boolean,
        v2Client: OkHttpClient?,
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ): Boolean {
        return if (useV2 && v2Client != null) {
            putLightStateV2(v2Client, bridgeIp, apiKey, lightId, on, bri, ct)
        } else if (!useV2) {
            putLightStateV1(bridgeIp, apiKey, lightId, on, bri, ct)
        } else {
            false
        }
    }

    private fun putLightStateV1(
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ): Boolean {
        return try {
            val body = """{"on":$on,"bri":$bri,"ct":$ct}"""
            val request = Request.Builder()
                .url("http://$bridgeIp/api/$apiKey/lights/$lightId/state")
                .put(body.toRequestBody(JSON))
                .build()
            httpV1.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun putLightStateV2(
        client: OkHttpClient,
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ): Boolean {
        return try {
            val brightnessPct = (bri * 100f / 254f).coerceIn(1f, 100f)
            val body = buildString {
                append("{")
                append("\"on\":{\"on\":$on},")
                append("\"dimming\":{\"brightness\":")
                append(String.format(java.util.Locale.US, "%.2f", brightnessPct))
                append("},")
                append("\"color_temperature\":{\"mirek\":$ct}")
                append("}")
            }
            val request = Request.Builder()
                .url("https://$bridgeIp/clip/v2/resource/light/$lightId")
                .header("hue-application-key", apiKey)
                .put(body.toRequestBody(JSON))
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

}
