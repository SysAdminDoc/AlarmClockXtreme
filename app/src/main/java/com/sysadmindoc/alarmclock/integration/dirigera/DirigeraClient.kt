package com.sysadmindoc.alarmclock.integration.dirigera

import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.integration.hue.HueBridgeClient
import com.sysadmindoc.alarmclock.integration.hue.HueTofuTrustManager
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** One light on the hub, as much of it as the sunrise needs. */
data class DirigeraLight(
    val id: String,
    val name: String,
    val isReachable: Boolean,
    val canDim: Boolean,
    /** Color temperature range in kelvin, or null for a light without white tuning. */
    val warmestKelvin: Int?,
    val coolestKelvin: Int?
)

sealed interface DirigeraPairResult {
    data class Paired(val accessToken: String, val observedFingerprint: String?) : DirigeraPairResult
    /** Nobody pressed the action button before the app stopped waiting. */
    data object ButtonNotPressed : DirigeraPairResult
    data class CertificateChanged(val expected: String, val observed: String) : DirigeraPairResult
    data object InvalidHost : DirigeraPairResult
    data class Failed(val reason: String) : DirigeraPairResult
}

sealed interface DirigeraLightsResult {
    data class Loaded(
        val lights: List<DirigeraLight>,
        val observedFingerprint: String?
    ) : DirigeraLightsResult
    /** The hub no longer takes the token, so the phone has to pair again. */
    data object Unauthorized : DirigeraLightsResult
    data class CertificateChanged(val expected: String, val observed: String) : DirigeraLightsResult
    data class Failed(val reason: String) : DirigeraLightsResult
}

/**
 * The connection settings for one conversation with the hub. The hub signs its
 * own certificate, so it gets the same first-use pin as the Hue bridge, and
 * [observedFingerprint] is the certificate it showed on the last handshake.
 */
class DirigeraSession internal constructor(
    val client: OkHttpClient,
    val pinnedFingerprint: String,
    private val trustManager: HueTofuTrustManager?
) {
    val observedFingerprint: String? get() = trustManager?.observedFingerprint
}

/**
 * Local API of the IKEA DIRIGERA hub: HTTPS on port 8443 under /v1, a bearer
 * token issued through a PKCE pairing that needs a press of the button on the
 * hub, and one attribute per PATCH when changing a light.
 */
@Singleton
class DirigeraClient @Inject constructor(
    private val baseClient: OkHttpClient
) {
    fun openSession(pinnedFingerprint: String): DirigeraSession {
        val trustManager = HueTofuTrustManager(pinnedFingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
        val client = baseClient.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            // The certificate names the hub, not its LAN address; the pin is
            // what proves it is the same hub.
            .hostnameVerifier { _, _ -> true }
            .build()
        return DirigeraSession(client, pinnedFingerprint, trustManager)
    }

    /**
     * Pairs the way the IKEA app does: ask the hub for a code, wait for the
     * action button, then trade the code and the PKCE verifier for a token.
     * [onPressButton] runs once the hub is waiting for the press.
     */
    suspend fun pair(
        rawHost: String,
        session: DirigeraSession,
        clientName: String,
        onPressButton: () -> Unit = {},
        verifier: String = DirigeraPkce.newVerifier(),
        pollIntervalMillis: Long = POLL_INTERVAL_MS,
        maxPolls: Int = MAX_POLLS
    ): DirigeraPairResult {
        val host = sanitiseHost(rawHost) ?: return DirigeraPairResult.InvalidHost
        val authorize = Request.Builder()
            .url(
                apiUrl(host, "oauth/authorize").newBuilder()
                    .addQueryParameter("audience", AUDIENCE)
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("code_challenge", DirigeraPkce.challenge(verifier))
                    .addQueryParameter("code_challenge_method", "S256")
                    .build()
            )
            .get()
            .build()
        val code = try {
            session.client.newCall(authorize).execute().use { response ->
                if (!response.isSuccessful) {
                    return DirigeraPairResult.Failed("HTTP_${response.code}")
                }
                parseObject(response.body.string())?.get("code") as? String
            }
        } catch (error: Exception) {
            return certificateChange(session)
                ?.let { (expected, observed) -> DirigeraPairResult.CertificateChanged(expected, observed) }
                ?: DirigeraPairResult.Failed(error::class.java.simpleName)
        }
        if (code.isNullOrBlank()) return DirigeraPairResult.Failed("NO_CODE")

        onPressButton()
        val tokenRequest = Request.Builder()
            .url(apiUrl(host, "oauth/token"))
            .post(
                FormBody.Builder()
                    .add("code", code)
                    .add("name", clientName)
                    .add("grant_type", "authorization_code")
                    .add("code_verifier", verifier)
                    .build()
            )
            .build()
        repeat(maxPolls) { attempt ->
            if (attempt > 0) delay(pollIntervalMillis)
            try {
                session.client.newCall(tokenRequest).execute().use { response ->
                    // Until the button is pressed the hub answers with a
                    // client error, so anything short of a token means wait.
                    if (response.isSuccessful) {
                        val token = parseObject(response.body.string())?.get("access_token") as? String
                        val accepted = token?.let { sanitiseAccessToken(it) }
                            ?: return DirigeraPairResult.Failed("NO_TOKEN")
                        return DirigeraPairResult.Paired(accepted, session.observedFingerprint)
                    }
                }
            } catch (error: Exception) {
                return certificateChange(session)
                    ?.let { (expected, observed) -> DirigeraPairResult.CertificateChanged(expected, observed) }
                    ?: DirigeraPairResult.Failed(error::class.java.simpleName)
            }
        }
        return DirigeraPairResult.ButtonNotPressed
    }

    fun listLights(rawHost: String, rawToken: String, session: DirigeraSession): DirigeraLightsResult {
        val host = sanitiseHost(rawHost) ?: return DirigeraLightsResult.Failed("INVALID_HOST")
        val token = sanitiseAccessToken(rawToken) ?: return DirigeraLightsResult.Unauthorized
        val request = Request.Builder()
            .url(apiUrl(host, "devices"))
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return try {
            session.client.newCall(request).execute().use { response ->
                when {
                    response.code == 401 || response.code == 403 -> DirigeraLightsResult.Unauthorized
                    !response.isSuccessful -> DirigeraLightsResult.Failed("HTTP_${response.code}")
                    else -> parseLights(response.body.string())
                        ?.let { DirigeraLightsResult.Loaded(it, session.observedFingerprint) }
                        ?: DirigeraLightsResult.Failed("BAD_JSON")
                }
            }
        } catch (error: Exception) {
            certificateChange(session)
                ?.let { (expected, observed) -> DirigeraLightsResult.CertificateChanged(expected, observed) }
                ?: DirigeraLightsResult.Failed(error::class.java.simpleName)
        }
    }

    /**
     * Sets one light. The hub takes a single attribute per PATCH, so level,
     * color temperature and power go out as separate calls, in that order:
     * a light switched on last comes up at the new level instead of flashing
     * its old one first. When switching on, the level goes out once more
     * afterwards, in case the light ignored it while it was off. A null
     * [level] or [kelvin] is left alone.
     */
    fun setLight(
        session: DirigeraSession,
        rawHost: String,
        rawToken: String,
        deviceId: String,
        level: Int?,
        kelvin: Int?,
        turnOn: Boolean,
        transitionMillis: Long
    ): Boolean {
        val host = sanitiseHost(rawHost) ?: return false
        val token = sanitiseAccessToken(rawToken) ?: return false
        val id = HueBridgeClient.sanitiseToken(deviceId) ?: return false
        val bodies = buildList {
            level?.let { add(attributeBody("lightLevel", it, transitionMillis)) }
            kelvin?.let { add(attributeBody("colorTemperature", it, transitionMillis)) }
            if (turnOn) {
                add(powerBody(true))
                level?.let { add(attributeBody("lightLevel", it, 0L)) }
            }
        }
        return bodies.all { body -> patchDevice(session, host, token, id, body) }
    }

    private fun patchDevice(
        session: DirigeraSession,
        host: String,
        token: String,
        deviceId: String,
        body: String
    ): Boolean = try {
        val request = Request.Builder()
            .url(apiUrl(host, "devices").newBuilder().addPathSegment(deviceId).build())
            .header("Authorization", "Bearer $token")
            .patch(body.toRequestBody(JSON))
            .build()
        session.client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
        false
    }

    private fun certificateChange(session: DirigeraSession): Pair<String, String>? {
        val pinned = session.pinnedFingerprint
        val observed = session.observedFingerprint ?: return null
        return if (pinned.isNotBlank() && !pinned.equals(observed, ignoreCase = true)) {
            pinned to observed
        } else {
            null
        }
    }

    companion object {
        const val PORT = 8443
        const val AUDIENCE = "homesmart.local"
        const val CLIENT_NAME = "AlarmClockXtreme"

        /** About a minute of waiting for the button, which is how long the hub keeps the code. */
        const val POLL_INTERVAL_MS = 2_000L
        const val MAX_POLLS = 30

        private val JSON = "application/json".toMediaType()
        private val anyAdapter = Moshi.Builder().build().adapter(Any::class.java)

        /**
         * Same local-only rule as the Hue bridge. The hub always listens on
         * [PORT], so an address with a port of its own is refused rather than
         * quietly ignored.
         */
        fun sanitiseHost(raw: String): String? {
            val host = HueBridgeClient.sanitiseHost(raw) ?: return null
            val hasPort = if (host.startsWith("[")) host.contains("]:") else host.contains(':')
            return host.takeUnless { hasPort }
        }

        /** The hub issues a JWT; anything else could smuggle header text. */
        fun sanitiseAccessToken(raw: String): String? {
            val trimmed = raw.trim()
            return trimmed.takeIf { ACCESS_TOKEN.matches(it) }
        }

        fun parseLightIds(raw: String): List<String> =
            raw.split(",").mapNotNull { HueBridgeClient.sanitiseToken(it.trim()) }.distinct()

        internal fun apiUrl(host: String, path: String): HttpUrl =
            "https://$host:$PORT/v1".toHttpUrl().newBuilder().addPathSegments(path).build()

        internal fun powerBody(on: Boolean): String = "[{\"attributes\":{\"isOn\":$on}}]"

        internal fun attributeBody(name: String, value: Int, transitionMillis: Long): String =
            buildString {
                append("[{\"attributes\":{\"").append(name).append("\":").append(value).append('}')
                if (transitionMillis > 0L) append(",\"transitionTime\":").append(transitionMillis)
                append("}]")
            }

        /** The lights in a /v1/devices answer, sorted by name, or null when it is not JSON. */
        fun parseLights(json: String): List<DirigeraLight>? {
            val devices = runCatching { anyAdapter.fromJson(json) }.getOrNull() as? List<*>
                ?: return null
            return devices.mapNotNull { (it as? Map<*, *>)?.toLight() }
                .sortedBy { it.name.lowercase() }
        }

        private fun Map<*, *>.toLight(): DirigeraLight? {
            if (this["deviceType"] != "light" && this["type"] != "light") return null
            val id = (this["id"] as? String)?.let { HueBridgeClient.sanitiseToken(it) } ?: return null
            val attributes = this["attributes"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val canReceive = ((this["capabilities"] as? Map<*, *>)?.get("canReceive") as? List<*>)
                ?.filterIsInstance<String>()
            fun accepts(attribute: String): Boolean =
                canReceive?.contains(attribute) ?: attributes.containsKey(attribute)
            val name = (attributes["customName"] as? String)?.takeIf { it.isNotBlank() }
                ?: (this["customName"] as? String)?.takeIf { it.isNotBlank() }
                ?: id
            // Hubs commonly report the range back to front (min 4000, max
            // 2202), so take the ends by value rather than by name.
            val first = (attributes["colorTemperatureMin"] as? Number)?.toInt()?.takeIf { it > 0 }
            val second = (attributes["colorTemperatureMax"] as? Number)?.toInt()?.takeIf { it > 0 }
            val range = if (accepts("colorTemperature") && first != null && second != null) {
                minOf(first, second) to maxOf(first, second)
            } else {
                null
            }
            return DirigeraLight(
                id = id,
                name = name,
                isReachable = this["isReachable"] as? Boolean ?: true,
                canDim = accepts("lightLevel"),
                warmestKelvin = range?.first,
                coolestKelvin = range?.second
            )
        }

        private fun parseObject(json: String): Map<*, *>? =
            runCatching { anyAdapter.fromJson(json) }.getOrNull() as? Map<*, *>

        private val ACCESS_TOKEN = Regex("^[A-Za-z0-9._~+/=-]{1,4096}$")
    }
}
