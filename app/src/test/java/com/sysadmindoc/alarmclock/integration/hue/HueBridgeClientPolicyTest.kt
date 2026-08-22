package com.sysadmindoc.alarmclock.integration.hue

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bridge is reachable over HTTPS or not at all.
 *
 * The v1 plain-HTTP fallback and its opt-in toggle are gone: targetSdk 36
 * blocks cleartext and the app ships no network security config, so that path
 * could never have connected. The Settings toggle promising it was a lie.
 */
class HueBridgeClientPolicyTest {
    private val client = HueBridgeClient(OkHttpClient())

    @Test
    fun `an unreachable bridge stays unreachable`() {
        val result = client.resolveConnection(HueV2ProbeResult.Failed("ConnectException"))

        assertTrue(result is HueConnectionResult.Unreachable)
        assertEquals(
            "ConnectException",
            (result as HueConnectionResult.Unreachable).reason
        )
    }

    @Test
    fun `a changed certificate is surfaced, never worked around`() {
        val result = client.resolveConnection(
            HueV2ProbeResult.CertificateChanged("expected", "observed")
        )

        assertEquals(HueConnectionResult.CertificateChanged("expected", "observed"), result)
    }

    @Test
    fun `a successful probe carries the observed fingerprint through`() {
        val result = client.resolveConnection(HueV2ProbeResult.Reachable("ab:cd"))

        assertEquals(HueConnectionResult.V2Reachable("ab:cd"), result)
    }

    @Test
    fun `a public hostname is refused because the bridge certificate is unverified`() {
        listOf(
            "bridge.example.com",
            "hue.philips.com",
            "8.8.8.8",
            "203.0.113.5:443"
        ).forEach {
            assertNull("$it must not be accepted as a bridge host", HueBridgeClient.sanitiseHost(it))
        }
    }

    @Test
    fun `router-assigned local domains are accepted, not just dot-local`() {
        // A Hue bridge is routinely reachable only as hue.lan or hue.fritz.box.
        // Refusing those sent the sunrise worker down the silent-failure path
        // for anyone whose router does not hand out .local.
        listOf(
            "hue.lan",
            "hue.home",
            "hue.home.arpa",
            "hue.internal",
            "hue.intranet",
            "hue.private",
            "hue.localdomain",
            "hue.fritz.box"
        ).forEach {
            assertEquals("$it is a LAN-only name and must be accepted", it, HueBridgeClient.sanitiseHost(it))
        }
    }

    @Test
    fun `a bracketed IPv6 literal on the LAN is accepted and a public one is not`() {
        // The name regex runs first and has no colons in it, so an IPv6 address
        // could never reach the local-network check.
        assertEquals("[fd00::1]", HueBridgeClient.sanitiseHost("[fd00::1]"))
        assertEquals("[fe80::1]:443", HueBridgeClient.sanitiseHost("[fe80::1]:443"))
        assertNull(HueBridgeClient.sanitiseHost("[2001:4860:4860::8888]"))
    }

    @Test
    fun `local bridge addresses are still accepted`() {
        listOf(
            "192.168.1.42",
            "192.168.1.42:443",
            "10.0.0.8",
            "172.16.5.9",
            "philips-hue.local",
            "hue"
        ).forEach {
            assertEquals(it, HueBridgeClient.sanitiseHost(it))
        }
    }

    @Test
    fun `structurally invalid hosts are still refused`() {
        listOf("", "   ", "http://192.168.1.42", "192.168.1.42/clip", "192.168.1.42:notaport")
            .forEach { assertNull(HueBridgeClient.sanitiseHost(it)) }
    }
}
