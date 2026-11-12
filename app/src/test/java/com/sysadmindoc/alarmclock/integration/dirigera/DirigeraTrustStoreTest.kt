package com.sysadmindoc.alarmclock.integration.dirigera

import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.integration.hue.HuePinResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirigeraTrustStoreTest {
    @Test
    fun firstHubCertificateIsPinnedApartFromTheHueBridge() = runTest {
        val preferences = mockk<PreferencesManager>()
        val hueFingerprint = "c".repeat(64)
        var settings = AppSettings(hueBridgeCertFingerprint = hueFingerprint)
        coEvery { preferences.update(any()) } coAnswers {
            val transform = firstArg<(AppSettings) -> AppSettings>()
            settings = transform(settings)
        }
        coEvery { preferences.getCurrentSettings() } answers { settings }
        val store = DirigeraTrustStore(preferences)
        val first = "a".repeat(64)
        val changed = "b".repeat(64)

        val accepted = store.rememberFirstUse(first.uppercase())
        val again = store.rememberFirstUse(first)
        val rejected = store.rememberFirstUse(changed)

        assertEquals(HuePinResult.Accepted(first, newlyPinned = true), accepted)
        assertEquals(HuePinResult.Accepted(first, newlyPinned = false), again)
        assertEquals(HuePinResult.Changed(first, changed), rejected)
        assertEquals(first, settings.dirigeraCertFingerprint)
        assertEquals(hueFingerprint, settings.hueBridgeCertFingerprint)
    }

    @Test
    fun malformedFingerprintIsRejectedWithoutWritingPreferences() = runTest {
        val preferences = mockk<PreferencesManager>(relaxed = true)
        val store = DirigeraTrustStore(preferences)

        val result = store.rememberFirstUse("not-a-sha256")

        assertTrue(result is HuePinResult.Invalid)
        coVerify(exactly = 0) { preferences.update(any()) }
    }
}
