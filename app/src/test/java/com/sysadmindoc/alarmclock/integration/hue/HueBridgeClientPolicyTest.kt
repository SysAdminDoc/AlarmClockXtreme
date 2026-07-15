package com.sysadmindoc.alarmclock.integration.hue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.OkHttpClient

class HueBridgeClientPolicyTest {
    private val client = HueBridgeClient(OkHttpClient())

    @Test
    fun legacyProbeIsNotAttemptedWhenExplicitToggleIsOff() {
        var attempted = false

        val result = client.resolveConnection(
            HueV2ProbeResult.Failed("unreachable"),
            allowLegacyHttp = false
        ) {
            attempted = true
            true
        }

        assertTrue(result is HueConnectionResult.Unreachable)
        assertFalse(attempted)
    }

    @Test
    fun certificateChangeNeverDowngradesToPlainHttp() {
        var attempted = false

        val result = client.resolveConnection(
            HueV2ProbeResult.CertificateChanged("expected", "observed"),
            allowLegacyHttp = true
        ) {
            attempted = true
            true
        }

        assertEquals(HueConnectionResult.CertificateChanged("expected", "observed"), result)
        assertFalse(attempted)
    }

    @Test
    fun enabledLegacyFallbackRunsOnlyAfterOrdinaryV2Failure() {
        var attempts = 0

        val result = client.resolveConnection(
            HueV2ProbeResult.Failed("ConnectException"),
            allowLegacyHttp = true
        ) {
            attempts += 1
            true
        }

        assertEquals(HueConnectionResult.V1Reachable, result)
        assertEquals(1, attempts)
    }
}
