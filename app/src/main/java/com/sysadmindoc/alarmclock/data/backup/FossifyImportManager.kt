package com.sysadmindoc.alarmclock.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fixed, user-presentable failure categories for Fossify import. Raw exception
 * text (JSON parser internals, storage paths) stays in the log only — the UI
 * maps each kind to a calm string resource.
 */
enum class FossifyImportErrorKind {
    NOT_FOSSIFY_EXPORT,
    UNREADABLE,
    CHANGED_AFTER_PREVIEW,
    NO_ALARMS
}

class FossifyImportException(
    val kind: FossifyImportErrorKind,
    cause: Throwable? = null
) : Exception(kind.name, cause)

data class FossifyAlarmPreview(
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<DayOfWeek>,
    val vibrationEnabled: Boolean,
    val sourceWasEnabled: Boolean,
    val soundName: String,
    val soundUri: String,
    val soundReadable: Boolean
)

data class FossifyImportPreview(
    val alarmCount: Int,
    val invalidAlarmCount: Int,
    val sourceEnabledAlarmCount: Int,
    val unreadableRingtoneCount: Int,
    val fingerprint: String,
    val alarms: List<FossifyAlarmPreview>
) {
    val canImport: Boolean get() = alarmCount > 0
}

internal data class FossifyAlarmCandidate(
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<DayOfWeek>,
    val vibrationEnabled: Boolean,
    val sourceWasEnabled: Boolean,
    val soundName: String,
    val soundUri: String
)

internal data class ParsedFossifyImport(
    val alarms: List<FossifyAlarmCandidate>,
    val invalidAlarmCount: Int
)

internal object FossifyImportMapper {
    fun toAlarm(candidate: FossifyAlarmCandidate, readableRingtoneUri: String): Alarm = Alarm(
        hour = candidate.hour,
        minute = candidate.minute,
        label = candidate.label,
        isEnabled = false,
        repeatDays = candidate.repeatDays,
        ringtoneUri = readableRingtoneUri,
        vibrationEnabled = candidate.vibrationEnabled,
        group = "Imported",
        profileName = "Fossify"
    )
}

internal object FossifyImportCodec {
    const val MAX_BYTES = 1_048_576
    const val MAX_ALARMS = 500
    private val adapter = Moshi.Builder().build().adapter(Any::class.java)

    fun parse(json: String): ParsedFossifyImport {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Fossify file exceeds 1 MiB" }
        val root = adapter.fromJson(json) as? Map<*, *>
            ?: throw IllegalArgumentException("Fossify file must be a JSON object")
        val rawAlarms = root["alarms"] as? List<*>
            ?: throw IllegalArgumentException("Fossify file does not contain an alarms array")
        require(rawAlarms.size <= MAX_ALARMS) { "Fossify file contains more than $MAX_ALARMS alarms" }

        var invalid = 0
        val alarms = rawAlarms.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: run { invalid++; return@mapNotNull null }
            val minutesSinceMidnight = parseMinutes(item) ?: run { invalid++; return@mapNotNull null }
            val daysMask = item.intValue("days")?.coerceIn(0, 127) ?: 0
            FossifyAlarmCandidate(
                hour = minutesSinceMidnight / 60,
                minute = minutesSinceMidnight % 60,
                label = item.stringValue("label").take(120),
                repeatDays = (0..6).mapNotNullTo(linkedSetOf()) { bit ->
                    if (daysMask and (1 shl bit) == 0) null else DayOfWeek.of(bit + 1)
                },
                vibrationEnabled = item.booleanValue("vibrate")
                    ?: item.booleanValue("vibrationEnabled")
                    ?: false,
                sourceWasEnabled = item.booleanValue("isEnabled")
                    ?: item.booleanValue("enabled")
                    ?: false,
                soundName = item.stringValue("soundName").take(120),
                soundUri = item.stringValue("soundUri").take(2_048)
            )
        }
        return ParsedFossifyImport(alarms, invalid)
    }

    private fun parseMinutes(item: Map<*, *>): Int? {
        val raw = item.longValue("time") ?: item.longValue("timeInMinutes") ?: return null
        val minutes = if (raw <= 1_440L) raw else raw / 60_000L
        return minutes.toInt().takeIf { it in 0..1_439 }
    }

    private fun Map<*, *>.stringValue(key: String): String =
        (this[key] as? String)?.trim().orEmpty()

    private fun Map<*, *>.longValue(key: String): Long? = when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun Map<*, *>.intValue(key: String): Int? = longValue(key)?.toInt()

    private fun Map<*, *>.booleanValue(key: String): Boolean? = when (val value = this[key]) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.toBooleanStrictOrNull()
        else -> null
    }
}

@Singleton
class FossifyImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository
) {
    private companion object {
        const val TAG = "FossifyImport"
    }

    suspend fun inspect(uri: Uri): Result<FossifyImportPreview> = runCatching {
        val bytes = readBoundedSanitized(uri)
        buildPreview(parseSanitized(bytes), fingerprint(bytes))
    }

    suspend fun import(uri: Uri, expectedFingerprint: String): Result<Int> = runCatching {
        val bytes = readBoundedSanitized(uri)
        if (fingerprint(bytes) != expectedFingerprint) {
            throw FossifyImportException(FossifyImportErrorKind.CHANGED_AFTER_PREVIEW)
        }
        val parsed = parseSanitized(bytes)
        if (parsed.alarms.isEmpty()) {
            throw FossifyImportException(FossifyImportErrorKind.NO_ALARMS)
        }
        val alarms = parsed.alarms.map { candidate ->
            FossifyImportMapper.toAlarm(
                candidate,
                candidate.soundUri.takeIf(::isReadableRingtone).orEmpty()
            )
        }
        repository.importDisabledAtomically(alarms).size
    }

    private fun buildPreview(parsed: ParsedFossifyImport, fingerprint: String): FossifyImportPreview {
        val previews = parsed.alarms.map { candidate ->
            FossifyAlarmPreview(
                hour = candidate.hour,
                minute = candidate.minute,
                label = candidate.label,
                repeatDays = candidate.repeatDays,
                vibrationEnabled = candidate.vibrationEnabled,
                sourceWasEnabled = candidate.sourceWasEnabled,
                soundName = candidate.soundName,
                soundUri = candidate.soundUri,
                soundReadable = candidate.soundUri.isBlank() || isReadableRingtone(candidate.soundUri)
            )
        }
        return FossifyImportPreview(
            alarmCount = previews.size,
            invalidAlarmCount = parsed.invalidAlarmCount,
            sourceEnabledAlarmCount = previews.count { it.sourceWasEnabled },
            unreadableRingtoneCount = previews.count { it.soundUri.isNotBlank() && !it.soundReadable },
            fingerprint = fingerprint,
            alarms = previews
        )
    }

    private fun isReadableRingtone(value: String): Boolean {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
        if (uri.scheme !in setOf("content", "android.resource")) return false
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    /** Detailed read failures go to the log; callers get a fixed sanitized kind. */
    private fun readBoundedSanitized(uri: Uri): ByteArray = try {
        readBounded(uri)
    } catch (error: IllegalArgumentException) {
        // Size-cap violation: a real Fossify Clock export is far under 1 MiB.
        Log.w(TAG, "Fossify file rejected by size cap", error)
        throw FossifyImportException(FossifyImportErrorKind.NOT_FOSSIFY_EXPORT, error)
    } catch (error: Exception) {
        Log.w(TAG, "Fossify file could not be read", error)
        throw FossifyImportException(FossifyImportErrorKind.UNREADABLE, error)
    }

    /** Detailed parse failures go to the log; callers get a fixed NOT_FOSSIFY_EXPORT kind. */
    private fun parseSanitized(bytes: ByteArray): ParsedFossifyImport = try {
        FossifyImportCodec.parse(bytes.toString(Charsets.UTF_8))
    } catch (error: Exception) {
        Log.w(TAG, "Selected file is not a readable Fossify export", error)
        throw FossifyImportException(FossifyImportErrorKind.NOT_FOSSIFY_EXPORT, error)
    }

    private fun readBounded(uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw java.io.FileNotFoundException("Unable to read Fossify file")
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                require(output.size() <= FossifyImportCodec.MAX_BYTES) { "Fossify file exceeds 1 MiB" }
            }
            output.toByteArray()
        }
    }

    private fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
