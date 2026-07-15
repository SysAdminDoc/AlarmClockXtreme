package com.sysadmindoc.alarmclock.integration.hue

import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface HuePinResult {
    data class Accepted(val fingerprint: String, val newlyPinned: Boolean) : HuePinResult
    data class Changed(val expectedFingerprint: String, val observedFingerprint: String) : HuePinResult
    data object Invalid : HuePinResult
}

@Singleton
class HueTrustStore @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    suspend fun rememberFirstUse(observedFingerprint: String): HuePinResult {
        val normalized = observedFingerprint.trim().lowercase(Locale.US)
        if (!FINGERPRINT.matches(normalized)) return HuePinResult.Invalid
        var newlyPinned = false
        preferencesManager.update { current ->
            if (current.hueBridgeCertFingerprint.isBlank()) {
                newlyPinned = true
                current.copy(hueBridgeCertFingerprint = normalized)
            } else {
                current
            }
        }
        val effective = preferencesManager.getCurrentSettings()
            .hueBridgeCertFingerprint.trim().lowercase(Locale.US)
        return if (effective == normalized) {
            HuePinResult.Accepted(effective, newlyPinned)
        } else {
            HuePinResult.Changed(effective, normalized)
        }
    }

    companion object {
        private val FINGERPRINT = Regex("^[0-9a-f]{64}$")
    }
}
