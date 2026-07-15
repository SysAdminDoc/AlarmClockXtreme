package com.sysadmindoc.alarmclock.integration.hue

import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HueTrustStoreTest {
    @Test
    fun firstObservedCertificateIsPinnedWithoutOverwritingItLater() = runTest {
        val preferences = mockk<PreferencesManager>()
        var settings = AppSettings()
        coEvery { preferences.update(any()) } coAnswers {
            val transform = firstArg<(AppSettings) -> AppSettings>()
            settings = transform(settings)
        }
        coEvery { preferences.getCurrentSettings() } answers { settings }
        val store = HueTrustStore(preferences)
        val first = "a".repeat(64)
        val changed = "b".repeat(64)

        val accepted = store.rememberFirstUse(first)
        val rejected = store.rememberFirstUse(changed)

        assertEquals(HuePinResult.Accepted(first, newlyPinned = true), accepted)
        assertEquals(HuePinResult.Changed(first, changed), rejected)
        assertEquals(first, settings.hueBridgeCertFingerprint)
    }

    @Test
    fun malformedFingerprintIsRejectedWithoutWritingPreferences() = runTest {
        val preferences = mockk<PreferencesManager>(relaxed = true)
        val store = HueTrustStore(preferences)

        val result = store.rememberFirstUse("not-a-sha256")

        assertTrue(result is HuePinResult.Invalid)
        coVerify(exactly = 0) { preferences.update(any()) }
    }
}
