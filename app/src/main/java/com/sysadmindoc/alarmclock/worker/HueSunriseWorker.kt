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
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * F15: Philips Hue sunrise simulation.
 * Gradually ramps up warm-white light from 0->254 brightness over [huePreWakeMinutes] minutes.
 * Input: KEY_ALARM_ID. Reads bridge IP, API key, and comma-separated light IDs from preferences.
 *
 * v1.11.5 (roadmap N5): API v2 (HTTPS, header auth, CLIP v2 resource shape)
 * with v1 fallback. Probes v2 first on every fresh-bridge run; remembers the
 * verdict per bridge IP in SharedPrefs so subsequent runs skip the probe.
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
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        private val JSON = "application/json".toMediaType()
        private const val STEPS = 20          // brightness increments
        private const val WARM_CT = 500       // ~2000K warm white (Hue range 153-500)

        private const val PREFS_HUE = "hue_api_capability"

        fun certFingerprint(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(cert.encoded)
                .joinToString("") { "%02x".format(it) }
        }
    }

    private fun buildTofuClient(pinnedFingerprint: String): Pair<OkHttpClient, TofuTrustManager> {
        val tofuTm = TofuTrustManager(pinnedFingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(tofuTm), java.security.SecureRandom())
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, tofuTm)
            .hostnameVerifier { _, _ -> true }
            .build()
        return client to tofuTm
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
        val bridgeIp = sanitiseHost(settings.hueBridgeIp)
            ?: return Result.failure()
        val apiKey = sanitiseToken(settings.hueApiKey)
            ?: return Result.failure()
        val lightIds = settings.hueLightIds
            .split(",")
            .map { sanitiseToken(it.trim()) }
            .filterNotNull()
        if (lightIds.isEmpty()) return Result.failure()

        val useV2 = resolveApiVersion(bridgeIp, apiKey, lightIds.first(), settings)
        if (!useV2 && !settings.hueLegacyHttpEnabled) {
            return Result.failure()
        }

        val pinnedFingerprint = settings.hueBridgeCertFingerprint
        val (v2Client, tofuTm) = if (useV2) buildTofuClient(pinnedFingerprint) else (null to null)

        val totalMs = alarm.huePreWakeMinutes * 60_000L
        val stepMs = totalMs / STEPS

        lightIds.forEach { id ->
            putLightState(useV2, v2Client, bridgeIp, apiKey, id, on = true, bri = 1, ct = WARM_CT)
        }

        if (useV2 && tofuTm != null && pinnedFingerprint.isBlank()) {
            val observed = tofuTm.observedFingerprint
            if (observed != null) {
                preferencesManager.update {
                    it.copy(hueBridgeCertFingerprint = observed)
                }
            }
        }

        for (step in 1..STEPS) {
            delay(stepMs)
            val bri = (step * 254 / STEPS).coerceIn(1, 254)
            lightIds.forEach { id ->
                putLightState(useV2, v2Client, bridgeIp, apiKey, id, on = true, bri = bri, ct = WARM_CT)
            }
        }

        return Result.success()
    }

    private fun resolveApiVersion(
        bridgeIp: String, apiKey: String, sampleLightId: String,
        settings: com.sysadmindoc.alarmclock.data.preferences.AppSettings
    ): Boolean {
        val prefs = applicationContext.getSharedPreferences(PREFS_HUE, Context.MODE_PRIVATE)
        when (prefs.getString("ver:$bridgeIp", null)) {
            "v2" -> return true
            "v1" -> return false
        }
        val v2Reachable = probeV2(bridgeIp, apiKey, sampleLightId, settings.hueBridgeCertFingerprint)
        prefs.edit().putString("ver:$bridgeIp", if (v2Reachable) "v2" else "v1").apply()
        return v2Reachable
    }

    private fun probeV2(
        bridgeIp: String, apiKey: String, sampleLightId: String,
        pinnedFingerprint: String
    ): Boolean {
        return try {
            val (client, _) = buildTofuClient(pinnedFingerprint)
            val request = Request.Builder()
                .url("https://$bridgeIp/clip/v2/resource/light/$sampleLightId")
                .header("hue-application-key", apiKey)
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun putLightState(
        useV2: Boolean,
        v2Client: OkHttpClient?,
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ) {
        if (useV2 && v2Client != null) putLightStateV2(v2Client, bridgeIp, apiKey, lightId, on, bri, ct)
        else if (!useV2) putLightStateV1(bridgeIp, apiKey, lightId, on, bri, ct)
    }

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

    private fun putLightStateV2(
        client: OkHttpClient,
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
            client.newCall(request).execute().close()
        } catch (_: Exception) {
            // Non-fatal: next step will retry
        }
    }

    private fun sanitiseHost(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val pattern = Regex("^[A-Za-z0-9.\\-]{1,253}(:\\d{1,5})?$")
        return if (pattern.matches(trimmed)) trimmed else null
    }

    private fun sanitiseToken(raw: String): String? {
        if (raw.isBlank()) return null
        val pattern = Regex("^[A-Za-z0-9_\\-]{1,128}$")
        return if (pattern.matches(raw)) raw else null
    }

    /**
     * TOFU (Trust On First Use) TrustManager for Hue bridge certificates.
     * If [pinnedFingerprint] is blank, accepts any cert and records its fingerprint.
     * If [pinnedFingerprint] is set, rejects certs whose SHA-256 doesn't match.
     */
    class TofuTrustManager(private val pinnedFingerprint: String) : X509TrustManager {
        var observedFingerprint: String? = null
            private set

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            if (chain.isNullOrEmpty()) throw CertificateException("Empty certificate chain")
            val leafFingerprint = certFingerprint(chain[0])
            observedFingerprint = leafFingerprint
            if (pinnedFingerprint.isNotBlank() && !pinnedFingerprint.equals(leafFingerprint, ignoreCase = true)) {
                throw CertificateException(
                    "Hue bridge certificate changed. Expected $pinnedFingerprint, got $leafFingerprint. " +
                    "Clear the pinned fingerprint in Settings if this is expected (e.g. bridge replacement)."
                )
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
