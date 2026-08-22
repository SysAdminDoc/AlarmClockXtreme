package com.sysadmindoc.alarmclock.ui.alarmedit

import com.sysadmindoc.alarmclock.R
import androidx.annotation.StringRes
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.nfc.NfcAdapter
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.BuildConfig

/**
 * Physical-challenge readiness preflight.
 *
 * NFC, barcode, photo, walking, Wi-Fi, and motion challenges can be selected
 * before the required hardware, runtime permission, or reference data exists.
 * A misconfigured physical challenge is a wake-failure risk (the alarm cannot be
 * dismissed), not just setup friction. This evaluator turns the device snapshot
 * plus the alarm's reference fields into an actionable readiness verdict so the
 * editor can warn the user and block saving an undismissable challenge.
 *
 * The core [evaluateChallengeReadiness] is a pure function so it is fully unit
 * testable; [deviceChallengeCapabilities] builds the snapshot from a [Context].
 */
enum class ChallengeReadinessStatus {
    /** Everything required is present. */
    READY,

    /** A runtime permission is missing. Recoverable before the alarm fires, so non-blocking. */
    NEEDS_PERMISSION,

    /** Required hardware is absent (or a radio is off). Blocks save when absent. */
    NEEDS_HARDWARE,

    /** A required reference (NFC tag, barcode, photo, SSID) has not been registered. Blocks save. */
    NEEDS_REFERENCE
}

data class ChallengeReadiness(
    val status: ChallengeReadinessStatus,
    /**
     * An id, not the text. The motion verdicts used to splice an English
     * verb into an English sentence, which no translation can follow.
     */
    @StringRes val messageRes: Int
) {
    /**
     * Whether this verdict should prevent saving the alarm. Missing hardware or a
     * missing reference produces an alarm that can never be dismissed, so those
     * block; a missing permission only warns because it can be granted later.
     */
    val blocksSave: Boolean
        get() = status == ChallengeReadinessStatus.NEEDS_HARDWARE ||
            status == ChallengeReadinessStatus.NEEDS_REFERENCE
}

/** Hardware presence and runtime-permission snapshot for physical challenges. */
data class DeviceChallengeCapabilities(
    val hasAccelerometer: Boolean = true,
    val hasStepCounter: Boolean = true,
    val hasNfc: Boolean = true,
    val nfcEnabled: Boolean = true,
    val hasCamera: Boolean = true,
    val hasWifi: Boolean = true,
    val speechRecognitionAvailable: Boolean = true,
    val digitalInkRecognitionAvailable: Boolean = true,
    val activityRecognitionGranted: Boolean = true,
    val cameraGranted: Boolean = true,
    val recordAudioGranted: Boolean = true,
    val locationGranted: Boolean = true
)

/** The alarm reference strings a physical challenge may require. */
data class ChallengeReferences(
    val nfcTagId: String = "",
    val barcodeValue: String = "",
    val photoMatchUri: String = "",
    val wifiDismissSsid: String = ""
)

/**
 * Evaluate readiness for a single [challengeType]. Returns null for challenge
 * types that need no hardware, permission, or reference (math, typing, etc.).
 *
 * Verdict priority is worst-first: absent hardware and missing references block
 * save, a disabled radio or missing permission only warns.
 */
fun evaluateChallengeReadiness(
    challengeType: String,
    capabilities: DeviceChallengeCapabilities,
    references: ChallengeReferences
): ChallengeReadiness? = when (challengeType) {
    "SHAKE" -> motionReadiness(capabilities, R.string.readiness_no_motion_sensor_shake)
    "SQUAT" -> motionReadiness(capabilities, R.string.readiness_no_motion_sensor_squat)
    "PUSH_UP" -> motionReadiness(capabilities, R.string.readiness_no_motion_sensor_pushup)
    "PLANK_HOLD" -> motionReadiness(capabilities, R.string.readiness_no_motion_sensor_plank)
    "WALK_STEPS" -> when {
        !capabilities.hasStepCounter -> hardware(R.string.readiness_no_step_counter)
        !capabilities.activityRecognitionGranted ->
            permission(R.string.readiness_needs_activity_permission)
        else -> ready()
    }
    "VOICE_PHRASE" -> when {
        !capabilities.speechRecognitionAvailable ->
            permission(R.string.readiness_no_speech_recognition)
        !capabilities.recordAudioGranted ->
            permission(R.string.readiness_needs_microphone)
        else -> ready()
    }
    "HANDWRITING" -> when {
        !capabilities.digitalInkRecognitionAvailable ->
            permission(R.string.readiness_no_handwriting)
        else -> ready()
    }
    "NFC_SCAN" -> when {
        !capabilities.hasNfc -> hardware(R.string.readiness_no_nfc)
        references.nfcTagId.isBlank() -> reference(R.string.readiness_needs_nfc_tag)
        !capabilities.nfcEnabled -> permission(R.string.readiness_nfc_off)
        else -> ready()
    }
    "BARCODE_SCAN" -> when {
        !capabilities.hasCamera -> hardware(R.string.readiness_no_camera_barcode)
        references.barcodeValue.isBlank() -> reference(R.string.readiness_needs_barcode)
        !capabilities.cameraGranted -> permission(R.string.readiness_needs_camera_barcode)
        else -> ready()
    }
    "PHOTO_MATCH" -> when {
        !capabilities.hasCamera -> hardware(R.string.readiness_no_camera_photo)
        references.photoMatchUri.isBlank() -> reference(R.string.readiness_needs_photo)
        !capabilities.cameraGranted -> permission(R.string.readiness_needs_camera_photo)
        else -> ready()
    }
    "WIFI_CONNECT" -> when {
        !capabilities.hasWifi -> hardware(R.string.readiness_no_wifi)
        references.wifiDismissSsid.isBlank() -> reference(R.string.readiness_needs_ssid)
        !capabilities.locationGranted ->
            permission(R.string.readiness_needs_location_wifi)
        else -> ready()
    }
    else -> null
}

/**
 * Aggregate readiness across the active challenge (and any chained challenges).
 * Returns the worst verdict found, preferring blocking verdicts so the editor
 * surfaces the most important problem first. Returns null when nothing physical
 * is configured.
 */
fun evaluateActiveChallengeReadiness(
    challengeType: String,
    challengeChain: String,
    capabilities: DeviceChallengeCapabilities,
    references: ChallengeReferences
): ChallengeReadiness? {
    val types = buildList {
        if (challengeType.isNotBlank()) add(challengeType)
        challengeChain.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "NONE" }
            .forEach { add(it) }
    }.distinct()

    val verdicts = types.mapNotNull { evaluateChallengeReadiness(it, capabilities, references) }
    if (verdicts.isEmpty()) return null
    return verdicts.minByOrNull { it.status.severityRank() }
}

/** Lower rank = more severe / shown first. */
private fun ChallengeReadinessStatus.severityRank(): Int = when (this) {
    ChallengeReadinessStatus.NEEDS_HARDWARE -> 0
    ChallengeReadinessStatus.NEEDS_REFERENCE -> 1
    ChallengeReadinessStatus.NEEDS_PERMISSION -> 2
    ChallengeReadinessStatus.READY -> 3
}

private fun motionReadiness(
    caps: DeviceChallengeCapabilities,
    @StringRes missingSensor: Int
): ChallengeReadiness = if (!caps.hasAccelerometer) hardware(missingSensor) else ready()

private fun ready() =
    ChallengeReadiness(ChallengeReadinessStatus.READY, R.string.readiness_ready)
private fun permission(@StringRes message: Int) =
    ChallengeReadiness(ChallengeReadinessStatus.NEEDS_PERMISSION, message)
private fun hardware(@StringRes message: Int) =
    ChallengeReadiness(ChallengeReadinessStatus.NEEDS_HARDWARE, message)
private fun reference(@StringRes message: Int) =
    ChallengeReadiness(ChallengeReadinessStatus.NEEDS_REFERENCE, message)

/** Build the live capability snapshot from the device. */
fun deviceChallengeCapabilities(context: Context): DeviceChallengeCapabilities {
    val pm = context.packageManager
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

    val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    return DeviceChallengeCapabilities(
        hasAccelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
        hasStepCounter = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER),
        hasNfc = nfcAdapter != null,
        nfcEnabled = nfcAdapter?.isEnabled == true,
        hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
        hasWifi = pm.hasSystemFeature(PackageManager.FEATURE_WIFI),
        speechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context),
        digitalInkRecognitionAvailable = BuildConfig.FLAVOR == "play",
        activityRecognitionGranted = activityRecognitionGranted,
        cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED,
        recordAudioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED,
        locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    )
}

/**
 * Pure save-gate helper: returns the list of challenge types whose required
 * reference is missing, considering the active challenge and chain. Used to
 * block saving an undismissable alarm without needing a device context.
 */
fun missingChallengeReferences(
    challengeType: String,
    challengeChain: String,
    references: ChallengeReferences
): List<String> {
    val types = buildList {
        if (challengeType.isNotBlank() && challengeType != "NONE") add(challengeType)
        challengeChain.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "NONE" }
            .forEach { add(it) }
    }.distinct()

    return types.filter { type ->
        when (type) {
            "NFC_SCAN" -> references.nfcTagId.isBlank()
            "BARCODE_SCAN" -> references.barcodeValue.isBlank()
            "PHOTO_MATCH" -> references.photoMatchUri.isBlank()
            "WIFI_CONNECT" -> references.wifiDismissSsid.isBlank()
            else -> false
        }
    }
}
