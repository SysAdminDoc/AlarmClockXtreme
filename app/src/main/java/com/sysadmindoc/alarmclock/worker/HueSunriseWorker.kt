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

/**
 * F15: Philips Hue sunrise simulation.
 * Gradually ramps up warm-white light from 0→254 brightness over [huePreWakeMinutes] minutes.
 * Input: KEY_ALARM_ID. Reads bridge IP, API key, and comma-separated light IDs from preferences.
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
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

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

        val totalMs = alarm.huePreWakeMinutes * 60_000L
        val stepMs = totalMs / STEPS

        // Turn lights on at min brightness first
        lightIds.forEach { id ->
            putLightState(bridgeIp, apiKey, id, on = true, bri = 1, ct = WARM_CT)
        }

        // Ramp up brightness over the pre-wake window
        for (step in 1..STEPS) {
            delay(stepMs)
            val bri = (step * 254 / STEPS).coerceIn(1, 254)
            lightIds.forEach { id ->
                putLightState(bridgeIp, apiKey, id, on = true, bri = bri, ct = WARM_CT)
            }
        }

        return Result.success()
    }

    private fun putLightState(
        bridgeIp: String, apiKey: String, lightId: String,
        on: Boolean, bri: Int, ct: Int
    ) {
        try {
            val body = """{"on":$on,"bri":$bri,"ct":$ct}"""
            val request = Request.Builder()
                .url("http://$bridgeIp/api/$apiKey/lights/$lightId/state")
                .put(body.toRequestBody(JSON))
                .build()
            http.newCall(request).execute().close()
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
