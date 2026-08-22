package com.sysadmindoc.alarmclock.integration.hue

import android.annotation.SuppressLint
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface HueV2ProbeResult {
    data class Reachable(val observedFingerprint: String) : HueV2ProbeResult
    data class CertificateChanged(
        val expectedFingerprint: String,
        val observedFingerprint: String
    ) : HueV2ProbeResult
    data class Failed(val reason: String) : HueV2ProbeResult
}

sealed interface HueConnectionResult {
    data class V2Reachable(val observedFingerprint: String) : HueConnectionResult
    data class CertificateChanged(
        val expectedFingerprint: String,
        val observedFingerprint: String
    ) : HueConnectionResult
    data class Unreachable(val reason: String) : HueConnectionResult
    data object InvalidConfiguration : HueConnectionResult
}

data class HueTofuClient(
    val client: OkHttpClient,
    val trustManager: HueTofuTrustManager
)

@Singleton
class HueBridgeClient @Inject constructor(
    private val baseClient: OkHttpClient
) {
    fun testConnection(
        rawBridgeHost: String,
        rawApiKey: String,
        pinnedFingerprint: String
    ): HueConnectionResult {
        val bridgeHost = sanitiseHost(rawBridgeHost)
            ?: return HueConnectionResult.InvalidConfiguration
        val apiKey = sanitiseToken(rawApiKey)
            ?: return HueConnectionResult.InvalidConfiguration
        val v2 = probeV2(
            bridgeHost = bridgeHost,
            apiKey = apiKey,
            resourcePath = "light",
            pinnedFingerprint = pinnedFingerprint
        )
        return resolveConnection(v2)
    }

    fun probeV2(
        bridgeHost: String,
        apiKey: String,
        resourcePath: String,
        pinnedFingerprint: String
    ): HueV2ProbeResult {
        val tofu = buildTofuClient(pinnedFingerprint)
        return try {
            val request = Request.Builder()
                .url("https://$bridgeHost/clip/v2/resource/$resourcePath")
                .header("hue-application-key", apiKey)
                .get()
                .build()
            tofu.client.newCall(request).execute().use { response ->
                val observed = tofu.trustManager.observedFingerprint
                if (response.isSuccessful && observed != null) {
                    HueV2ProbeResult.Reachable(observed)
                } else {
                    HueV2ProbeResult.Failed("HTTP_${response.code}")
                }
            }
        } catch (error: Exception) {
            val observed = tofu.trustManager.observedFingerprint
            if (pinnedFingerprint.isNotBlank() &&
                observed != null &&
                !pinnedFingerprint.equals(observed, ignoreCase = true)
            ) {
                HueV2ProbeResult.CertificateChanged(pinnedFingerprint, observed)
            } else {
                HueV2ProbeResult.Failed(error::class.java.simpleName)
            }
        }
    }

    fun buildTofuClient(pinnedFingerprint: String): HueTofuClient {
        val trustManager = HueTofuTrustManager(pinnedFingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
        val client = baseClient.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            // Hue bridges use self-signed certificates whose host name commonly
            // does not match the LAN IP; identity is enforced by the TOFU pin.
            .hostnameVerifier { _, _ -> true }
            .build()
        return HueTofuClient(client, trustManager)
    }

    /**
     * The bridge is only reachable over HTTPS now. targetSdk 36 blocks cleartext
     * and the app declares no network security config, so the old v1 HTTP probe
     * could never connect; keeping it only produced a setting that lied.
     */
    internal fun resolveConnection(
        v2Result: HueV2ProbeResult
    ): HueConnectionResult = when (v2Result) {
        is HueV2ProbeResult.Reachable ->
            HueConnectionResult.V2Reachable(v2Result.observedFingerprint)
        is HueV2ProbeResult.CertificateChanged ->
            HueConnectionResult.CertificateChanged(
                v2Result.expectedFingerprint,
                v2Result.observedFingerprint
            )
        is HueV2ProbeResult.Failed -> HueConnectionResult.Unreachable(v2Result.reason)
    }

    companion object {
        fun certFingerprint(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(cert.encoded).joinToString("") { "%02x".format(it) }
        }

        /**
         * A Hue bridge lives on the local network, and the TLS trust here is
         * trust-on-first-use with nothing pinned until the first successful
         * call. A public hostname would therefore be reachable over the
         * internet with an unverified certificate, so refuse anything that is
         * not a private address or an mDNS name.
         */
        fun sanitiseHost(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            if (!Regex("^[A-Za-z0-9.\\-]{1,253}(:\\d{1,5})?$").matches(trimmed)) return null
            val hostOnly = trimmed.substringBeforeLast(':', trimmed)
            return trimmed.takeIf { LocalNetworkPermission.isLikelyLocalHost(hostOnly) }
        }

        fun sanitiseToken(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            return trimmed.takeIf { Regex("^[A-Za-z0-9_\\-]{1,128}$").matches(it) }
        }
    }
}

@SuppressLint("CustomX509TrustManager")
class HueTofuTrustManager(
    private val pinnedFingerprint: String
) : X509TrustManager {
    @Volatile
    var observedFingerprint: String? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        if (chain.isNullOrEmpty()) throw CertificateException("Empty certificate chain")
        val observed = HueBridgeClient.certFingerprint(chain[0])
        observedFingerprint = observed
        if (pinnedFingerprint.isNotBlank() &&
            !pinnedFingerprint.equals(observed, ignoreCase = true)
        ) {
            throw CertificateException("Hue bridge certificate changed")
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
