package com.sysadmindoc.alarmclock.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.integration.dirigera.DirigeraClient
import com.sysadmindoc.alarmclock.integration.dirigera.DirigeraLightsResult
import com.sysadmindoc.alarmclock.integration.dirigera.DirigeraTrustStore
import com.sysadmindoc.alarmclock.integration.hue.HueBridgeClient
import com.sysadmindoc.alarmclock.integration.hue.HuePinResult
import com.sysadmindoc.alarmclock.integration.hue.HueTrustStore
import com.sysadmindoc.alarmclock.integration.hue.HueV2ProbeResult
import com.sysadmindoc.alarmclock.util.LocalNetworkPermission
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
 * v1.11.5 (roadmap N5): API v2 only (HTTPS, header auth, CLIP v2 resource
 * shape). Each run proves v2 reachability and the current certificate before
 * sending light commands.
 *
 * v1.14.x: TOFU (Trust On First Use) certificate pinning for v2 HTTPS.
 * On first successful connection, the bridge cert SHA-256 fingerprint is saved
 * to DataStore. Subsequent connections reject certificate changes, surfacing
 * a blocking warning in Settings. The v1 plain-HTTP path is gone: cleartext is
 * blocked at this targetSdk, so it could never have connected.
 *
 * The same ramp also drives the lights picked on a paired IKEA DIRIGERA hub,
 * which has its own certificate pin. Each hub is checked on its own, so one
 * that is offline or misconfigured does not stop the sunrise on the other.
 */
@HiltWorker
class HueSunriseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val hueBridgeClient: HueBridgeClient,
    private val hueTrustStore: HueTrustStore,
    private val dirigeraClient: DirigeraClient,
    private val dirigeraTrustStore: DirigeraTrustStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ALARM_ID = "alarm_id"

        /**
         * Wall-clock bounds of the ramp, carried across segments so a
         * continuation resumes at the right brightness instead of restarting.
         * Absent on the first run, which derives them from the alarm.
         */
        const val KEY_RAMP_START = "ramp_start"
        const val KEY_RAMP_END = "ramp_end"

        private const val TAG = "HueSunriseWorker"
        private val JSON = "application/json".toMediaType()
        private const val WARM_CT = 500       // ~2000K warm white (Hue range 153-500)

        fun uniqueName(alarmId: Long): String = "hue_sunrise_$alarmId"
    }

    /**
     * One hub's lights. [show] sets them to a point on the Hue brightness
     * curve and reports whether every light took the command; [opening] is
     * the first command of a run, which also switches the lights on.
     */
    private fun interface SunriseTarget {
        fun show(brightness: Int, opening: Boolean, transitionMillis: Long): Boolean
    }

    override suspend fun doWork(): Result {
        // WorkManager's ten-minute clock starts here, not after the bridge
        // probe, so the hand-over budget has to be measured from here too.
        val runStartedAt = System.currentTimeMillis()
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1)
        if (alarmId < 0) return Result.failure()

        val alarm = repository.getById(alarmId) ?: return Result.failure()
        if (!alarm.hueEnabled) return Result.success()

        val settings = preferencesManager.getCurrentSettings()
        val targets = listOfNotNull(hueTarget(settings), dirigeraTarget(settings)).toMutableList()
        if (targets.isEmpty()) return Result.failure()

        val segmentStart = System.currentTimeMillis()
        val rampStart = inputData.getLong(KEY_RAMP_START, 0L).takeIf { it > 0L } ?: segmentStart
        val rampEnd = inputData.getLong(KEY_RAMP_END, 0L).takeIf { it > 0L }
            ?: (rampStart + alarm.huePreWakeMinutes * 60_000L)

        // A run that starts after the alarm has nothing useful to do. Touching
        // the lights here would snap them to full brightness hours later.
        if (HueSunriseRampPlan.isComplete(rampEnd, segmentStart)) {
            HueSunriseNotifications.cancel(applicationContext, alarm.id)
            return Result.success()
        }
        val transitionMillis = DirigeraSunrisePlan.transitionMillis(rampStart, rampEnd)

        // Resume at the brightness the wall clock calls for. A first run starts
        // at 1; a continuation, or a run WorkManager deferred, picks up where
        // the ramp actually is instead of replaying it.
        val openingBrightness =
            HueSunriseRampPlan.brightnessAt(rampStart, rampEnd, segmentStart)
        targets.retainAll { it.show(openingBrightness, opening = true, transitionMillis) }
        if (targets.isEmpty()) return Result.failure()

        HueSunriseNotifications.post(applicationContext, alarm.id, rampStart, rampEnd, segmentStart)
        // Hand over before WorkManager's ten-minute execution limit stops us.
        val handOverAt = HueSunriseRampPlan.segmentEndsAt(rampEnd, runStartedAt)
        var handingOver = false
        try {
            while (true) {
                val now = System.currentTimeMillis()
                if (HueSunriseRampPlan.isComplete(rampEnd, now)) break
                if (handOverAt != null && now >= handOverAt) {
                    handingOver = true
                    break
                }
                val nextAt = HueSunriseRampPlan.nextStepAt(rampStart, rampEnd, now) ?: break
                // Never sleep past the hand-over point: with a long pre-wake a
                // single step is minutes long, and waking only at step
                // boundaries would push the run past WorkManager's limit.
                val wakeAt = if (handOverAt != null) minOf(nextAt, handOverAt) else nextAt
                delay((wakeAt - now).coerceAtLeast(0L))
                if (handOverAt != null && System.currentTimeMillis() >= handOverAt) {
                    handingOver = true
                    break
                }
                val bri = HueSunriseRampPlan.brightnessAt(
                    rampStart, rampEnd, System.currentTimeMillis()
                )
                targets.retainAll { it.show(bri, opening = false, transitionMillis) }
                if (targets.isEmpty()) return Result.failure()
                HueSunriseNotifications.post(
                    context = applicationContext,
                    alarmId = alarm.id,
                    startWallClockMillis = rampStart,
                    endWallClockMillis = rampEnd
                )
            }
        } finally {
            // The next segment re-posts it; anything else means the ramp is over.
            if (!handingOver) HueSunriseNotifications.cancel(applicationContext, alarm.id)
        }
        if (handingOver) enqueueContinuation(alarm.id, rampStart, rampEnd)
        return Result.success()
    }

    private suspend fun hueTarget(settings: AppSettings): SunriseTarget? {
        if (SunriseLightHub.HUE !in SunriseLightHub.configured(settings)) return null
        val bridgeIp = HueBridgeClient.sanitiseHost(settings.hueBridgeIp) ?: run {
            // A public hostname is refused because the TLS trust here is
            // trust-on-first-use. Say so in the log rather than stopping
            // silently; the Hue settings group shows the same inline.
            Log.w(TAG, "Hue bridge host is not a local address, skipping it")
            return null
        }
        val apiKey = HueBridgeClient.sanitiseToken(settings.hueApiKey) ?: return null
        val lightIds = settings.hueLightIds
            .split(",")
            .map { HueBridgeClient.sanitiseToken(it.trim()) }
            .filterNotNull()
        if (lightIds.isEmpty()) return null

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
                HuePinResult.Invalid -> return null
            }
            is HueV2ProbeResult.CertificateChanged -> return null
            is HueV2ProbeResult.Failed -> null
        }
        // HTTPS only. targetSdk 36 blocks cleartext and no network security
        // config permits it, so a bridge that fails the v2 probe is simply
        // unreachable; the old v1 fallback could not have connected either.
        val v2Client = effectivePin?.let { hueBridgeClient.buildTofuClient(it).client }
            ?: return null
        return SunriseTarget { brightness, _, _ ->
            lightIds.all { id ->
                putLightStateV2(
                    v2Client, bridgeIp, apiKey, id,
                    on = true, bri = brightness, ct = WARM_CT
                )
            }
        }
    }

    private suspend fun dirigeraTarget(settings: AppSettings): SunriseTarget? {
        if (SunriseLightHub.DIRIGERA !in SunriseLightHub.configured(settings)) return null
        if (!LocalNetworkPermission.isGranted(applicationContext)) {
            Log.w(TAG, "Local network access is not granted, skipping the DIRIGERA hub")
            return null
        }
        val host = DirigeraClient.sanitiseHost(settings.dirigeraHubIp) ?: run {
            Log.w(TAG, "DIRIGERA hub host is not a local address, skipping it")
            return null
        }
        val token = settings.dirigeraAccessToken
        val probe = dirigeraClient.listLights(
            host, token, dirigeraClient.openSession(settings.dirigeraCertFingerprint)
        )
        if (probe !is DirigeraLightsResult.Loaded) {
            Log.w(TAG, "DIRIGERA hub did not list its lights, skipping it")
            return null
        }
        val pin = probe.observedFingerprint?.let { dirigeraTrustStore.rememberFirstUse(it) }
        if (pin !is HuePinResult.Accepted) return null
        val wanted = DirigeraClient.parseLightIds(settings.dirigeraLightIds).toSet()
        val lights = probe.lights.filter { it.id in wanted && it.isReachable }
        if (lights.isEmpty()) return null
        val session = dirigeraClient.openSession(pin.fingerprint)
        return SunriseTarget { brightness, opening, transitionMillis ->
            lights.all { light ->
                val warmest = light.warmestKelvin
                val coolest = light.coolestKelvin
                dirigeraClient.setLight(
                    session = session,
                    rawHost = host,
                    rawToken = token,
                    deviceId = light.id,
                    level = DirigeraSunrisePlan.lightLevel(brightness).takeIf { light.canDim },
                    kelvin = if (warmest != null && coolest != null) {
                        DirigeraSunrisePlan.kelvin(brightness, warmest, coolest)
                    } else {
                        null
                    },
                    turnOn = opening,
                    // Switching on jumps straight to the level; every later
                    // step fades so the room brightens without visible steps.
                    transitionMillis = if (opening) 0L else transitionMillis
                )
            }
        }
    }

    /**
     * Queues the next segment of the same ramp under the same unique name, so a
     * cancel or a reschedule still tears the whole chain down.
     */
    private fun enqueueContinuation(alarmId: Long, rampStart: Long, rampEnd: Long) {
        val request = androidx.work.OneTimeWorkRequestBuilder<HueSunriseWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putLong(KEY_ALARM_ID, alarmId)
                    .putLong(KEY_RAMP_START, rampStart)
                    .putLong(KEY_RAMP_END, rampEnd)
                    .build()
            )
            .build()
        // APPEND, not APPEND_OR_REPLACE: if the chain was cancelled while this
        // segment was finishing, the continuation must stay cancelled rather
        // than start a fresh ramp for an alarm that no longer exists.
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            uniqueName(alarmId),
            androidx.work.ExistingWorkPolicy.APPEND,
            request
        )
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
