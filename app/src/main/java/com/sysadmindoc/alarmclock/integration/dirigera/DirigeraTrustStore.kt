package com.sysadmindoc.alarmclock.integration.dirigera

import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.integration.hue.HuePinResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trust on first use for the DIRIGERA hub certificate, kept apart from the Hue
 * pin so pairing or forgetting one hub never touches the other.
 */
@Singleton
class DirigeraTrustStore @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    suspend fun rememberFirstUse(observedFingerprint: String): HuePinResult {
        val normalized = observedFingerprint.trim().lowercase(Locale.US)
        if (!FINGERPRINT.matches(normalized)) return HuePinResult.Invalid
        var newlyPinned = false
        preferencesManager.update { current ->
            if (current.dirigeraCertFingerprint.isBlank()) {
                newlyPinned = true
                current.copy(dirigeraCertFingerprint = normalized)
            } else {
                current
            }
        }
        val effective = preferencesManager.getCurrentSettings()
            .dirigeraCertFingerprint.trim().lowercase(Locale.US)
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
