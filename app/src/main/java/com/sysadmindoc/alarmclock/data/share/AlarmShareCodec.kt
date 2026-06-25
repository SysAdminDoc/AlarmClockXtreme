package com.sysadmindoc.alarmclock.data.share

import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.backup.AlarmBackup
import com.sysadmindoc.alarmclock.data.backup.toAlarmBackup
import com.sysadmindoc.alarmclock.data.backup.toAlarmOrNull
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

@JsonClass(generateAdapter = true)
data class AlarmSharePayload(
    val version: Int = 1,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val alarm: AlarmBackup
)

object AlarmShareCodec {
    const val SCHEME = "acx"
    const val HOST = "alarm"
    const val DATA_PARAM = "data"
    private const val MAX_SUPPORTED_VERSION = 1
    private val REFERENCE_BACKED_CHALLENGES = setOf(
        "NFC_SCAN",
        "BARCODE_SCAN",
        "PHOTO_MATCH",
        "WIFI_CONNECT"
    )

    /**
     * Hard ceiling on the base64 token we'll attempt to decode. A real
     * AlarmBackup serialises to ~1-2 KB; even a worst-case alarm with the
     * maximum challenge chain, ringtone pool, and morning routine sits well
     * under 8 KB. 16 KB leaves comfortable headroom while ensuring a hostile
     * deep-link can't OOM the process by handing us a multi-megabyte token.
     */
    private const val MAX_TOKEN_LENGTH = 16 * 1024

    private val payloadAdapter = Moshi.Builder()
        .build()
        .adapter(AlarmSharePayload::class.java)

    fun createDeepLink(alarm: Alarm): String {
        return "$SCHEME://$HOST?$DATA_PARAM=${encodeToken(alarm)}"
    }

    fun encodeToken(alarm: Alarm): String {
        val payload = AlarmSharePayload(alarm = alarm.sanitized().toAlarmBackup())
        val json = payloadAdapter.toJson(payload)
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
    }

    fun decodeToken(token: String): Result<Alarm> {
        return runCatching {
            require(token.isNotBlank()) { "Empty share token" }
            require(token.length <= MAX_TOKEN_LENGTH) {
                "Shared alarm token exceeds maximum size"
            }
            val json = String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
            val payload = payloadAdapter.fromJson(json)
                ?: throw IllegalArgumentException("Invalid shared alarm payload")
            require(payload.version in 1..MAX_SUPPORTED_VERSION) {
                "Unsupported shared alarm version ${payload.version}"
            }
            payload.alarm.toAlarmOrNull()
                ?: throw IllegalArgumentException("Shared alarm payload is not usable")
        }
    }

    internal fun tokenStorageKey(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
        val hash = buildString(digest.size * 2) {
            digest.forEach { byte ->
                append("%02x".format(Locale.US, byte.toInt() and 0xff))
            }
        }
        return "${token.length}:$hash"
    }

    fun prepareImportedAlarm(alarm: Alarm, nowMillis: Long = System.currentTimeMillis()): Alarm {
        return alarm.copy(
            id = 0,
            label = alarm.label.ifBlank { "Shared alarm" },
            isEnabled = false,
            createdAt = nowMillis,
            nextTriggerTime = 0
        ).sanitized()
    }

    fun stripRiskyImportedFields(alarm: Alarm): Alarm {
        return alarm.copy(
            ringtoneUri = "",
            spotifyUri = "",
            nfcTagId = "",
            barcodeValue = "",
            photoMatchUri = "",
            hueEnabled = false,
            guardianEnabled = false,
            guardianPhone = "",
            locationDismissEnabled = false,
            locationDismissLat = 0.0,
            locationDismissLng = 0.0,
            locationDismissRadius = 100,
            wifiDismissSsid = "",
            internetRadioUrl = "",
            morningRoutine = "",
            ringtonePool = "",
            challengeType = alarm.challengeType
                .takeUnless { it.uppercase(Locale.US) in REFERENCE_BACKED_CHALLENGES }
                ?: "NONE",
            challengeChain = alarm.challengeChain
                .split(",")
                .map { it.trim() }
                .filter {
                    it.isNotEmpty() && it.uppercase(Locale.US) !in REFERENCE_BACKED_CHALLENGES
                }
                .joinToString(","),
            dismissActionType = "NONE",
            dismissActionPayload = ""
        ).sanitized()
    }
}
