package com.sysadmindoc.alarmclock.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * F15: Philips Hue sunrise simulation.
 * Gradually ramps up warm-white light from 0→254 brightness over [huePreWakeMinutes] minutes.
 * Input: KEY_ALARM_ID. Reads bridge IP, API key, and comma-separated light IDs from preferences.
 *
 * v1.11.5 (roadmap N5): API v2 (HTTPS, header auth, CLIP v2 resource shape)
 * with v1 fallback. Probes v2 first on every fresh-bridge run; remembers the
 * verdict per bridge IP in SharedPrefs so subsequent runs skip the probe.
 * v1 is the documented-deprecated path and is kept for ~6 months while users
 * upgrade their bridges past firmware 1.40.
 */
@HiltWorker
class HueSunriseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        private val JSON = "application/json".toMediaType()
        private const val STEPS = 20          // brightness increments
        private const val WARM_CT = 500       // ~2000K warm white (Hue range 153–500)

        /**
         * Per-bridge cache of "this bridge speaks v2". Keyed on the bridge IP.
         * Negative cache (v2 probe failed → use v1) lives in the same SharedPrefs
         * as `"v1"`; a positive verdict is `"v2"`.
         */
        private const val PREFS_HUE = "hue_api_capability"
    }

    // OkHttp client for v1 (plain HTTP) — fast LAN path, no certificate dance.
    private val httpV1 = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * OkHttp client for v2 (HTTPS to the bridge). Hue bridges present a
     * self-signed certificate whose CN is the bridge ID (a MAC-derived hash),
     * so the cert never matches the user's LAN IP — strict hostname
     * verification would reject every connection. We pair an allow-all
     * hostname verifier with an unconditional trust manager.
     *
     * Threat model: traffic stays on the user's LAN. An attacker already on
     * that LAN can intercept brightness commands, which is a trivial info
     * disclosure ("the user's lights are ramping up at 6am"). The risk is
     * the same as plain HTTP v1 today. A future hardening pass can bundle
     * the Signify root CA + pin the bridge ID — tracked separately on the
     * roadmap.
     */
    private val httpV2: OkHttpClient by lazy {
        val trustEverything = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustEverything, java.security.SecureRandom())
        }
        OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustEverything[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _: String, _: SSLSession -> true })
            .build()
    }

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1)
        if (alarmId < 0) return Result.failure()

        val alarm = repository.getById(alarmId) ?: return Result.failure()
        if (!alarm.hueEnabled) return Result.success()

        val settings = preferencesManager.getCurrentSettings()
        val bridgeIp = sanitiseHost(settings.hueBridgeIp)
            ?: return Result.failure()
        val apiKey = sanitiseToken(settings.hueApiKey)
            ?: return Result.failure()
        val lightIds = settings.hueLightIds
            .split(",")
            .map { sanitiseToken(it.trim()) }
            .filterNotNull()
        if (lightIds.isEmpty()) return Result.failure()

        // Pick API version. Cached verdict wins; on cache miss, probe v2 once
        // and remember the result. Probing uses the first configured light at
        // its current state, so the user doesn't see a flash from the probe.
        val useV2 = resolveApiVersion(bridgeIp, apiKey, lightIds.first())

        val totalMs = alarm.huePreWakeMinutes * 60_000L
        val stepMs = totalMs / STEPS

        // Turn lights on at min brightness first
        lightIds.forEach { id ->
            putLightState(useV2, bridgeIp, apiKey, id, on = true, bri = 1, ct = WARM_CT)
        }

        // Ramp up brightness over the pre-wake window
        for (step in 1..STEPS) {
            delay(stepMs)
            val bri = (step * 254 / STEPS).coerceIn(1, 254)
            lightIds.forEach { id ->
                putLightState(useV2, bridgeIp, apiKey, id, on = true, bri = bri, ct = WARM_CT)
            }
        }

        return Result.success()
    }

    /**
     * Returns `true` if the bridge speaks v2, `false` for v1. Caches the
     * verdict in SharedPrefs so we don't probe on every fire.
     */
    private fun resolveApiVersion(bridgeIp: String, apiKey: String, sampleLightId: String): Boolean {
        val prefs = applicationContext.getSharedPreferences(PREFS_HUE, Context.MODE_PRIVATE)
        when (prefs.getString("ver:$bridgeIp", null)) {
            "v2" -> return true
            "v1" -> return false
        }
        val v2Reachable = probeV2(bridgeIp, apiKey, sampleLightId)
        prefs.edit().putString("ver:$bridgeIp", if (v2Reachable) "v2" else "v1").apply()
        return v2Reachable
    }

    /**
     * GET the sample light resource via CLIP v2. A 200 with a body containing
     * the resource means v2 works against this bridge with this key. Anything
     * else (4xx/5xx, TLS handshake failure, connection refused) means v1.
     */
    private fun probeV2(bridgeIp: String, apiKey: String, sampleLightId: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://$bridgeIp/clip/v2/resource/light/$sampleLightId")
                .header("hue-application-key", apiKey)
                .get()
                .build()
            httpV2.newCall(request).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun putLightState(
        useV2: Boolean,
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ) {
        if (useV2) putLightStateV2(bridgeIp, apiKey, lightId, on, bri, ct)
        else putLightStateV1(bridgeIp, apiKey, lightId, on, bri, ct)
    }

    /**
     * v1 (deprecated): `PUT http://{ip}/api/{key}/lights/{id}/state`
     * Body: `{"on":bool,"bri":0..254,"ct":153..500}`
     */
    private fun putLightStateV1(
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ) {
        try {
            val body = """{"on":$on,"bri":$bri,"ct":$ct}"""
            val request = Request.Builder()
                .url("http://$bridgeIp/api/$apiKey/lights/$lightId/state")
                .put(body.toRequestBody(JSON))
                .build()
            httpV1.newCall(request).execute().close()
        } catch (_: Exception) {
            // Non-fatal: next step will retry
        }
    }

    /**
     * v2 (current): `PUT https://{ip}/clip/v2/resource/light/{rid}`
     * Header: `hue-application-key: {applicationkey}`
     * Body: `{"on":{"on":bool},"dimming":{"brightness":0..100},"color_temperature":{"mirek":153..500}}`
     *
     * v1's 0–254 brightness scale is converted to v2's 0–100 percent.
     */
    private fun putLightStateV2(
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ) {
        try {
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
            httpV2.newCall(request).execute().close()
        } catch (_: Exception) {
            // Non-fatal: next step will retry
        }
    }

    /**
     * The Hue bridge IP is user-entered, so we restrict it to characters legal in
     * a hostname/IP (digits, letters, dot, dash, optional :port) to prevent path
     * injection ("../") or scheme smuggling into the URL string.
     */
    private fun sanitiseHost(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val pattern = Regex("^[A-Za-z0-9.\\-]{1,253}(:\\d{1,5})?$")
        return if (pattern.matches(trimmed)) trimmed else null
    }

    /**
     * Hue API keys and light IDs are alphanumeric (Philips spec uses [A-Za-z0-9-]),
     * so anything else in user-entered values is treated as malformed. Prevents
     * slashes / spaces / unicode from being concatenated into the URL.
     */
    private fun sanitiseToken(raw: String): String? {
        if (raw.isBlank()) return null
        val pattern = Regex("^[A-Za-z0-9_\\-]{1,128}$")
        return if (pattern.matches(raw)) raw else null
    }
}
