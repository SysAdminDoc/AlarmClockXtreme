package com.sysadmindoc.alarmclock.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionTest {

    @Test
    fun `private and link-local endpoints are classified as local`() {
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://192.168.1.40/clip/v2/resource/light"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://10.0.0.12/hook"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://172.20.1.5/hook"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://169.254.1.2/hook"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://[fe80::1]/hook"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://[fd00::1]/hook"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://[fc00::1]/hook"))
    }

    @Test
    fun `localhost local names and mdns endpoints are classified as local`() {
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://localhost/hook"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://homeassistant:8123/api/webhook/abc"))
        assertTrue(LocalNetworkPermission.isLikelyLocalEndpoint("https://bridge.local/api"))
    }

    @Test
    fun `public endpoints and malformed urls are not local`() {
        assertFalse(LocalNetworkPermission.isLikelyLocalEndpoint("https://example.com/hook"))
        assertFalse(LocalNetworkPermission.isLikelyLocalEndpoint("https://fc-example.com/hook"))
        assertFalse(LocalNetworkPermission.isLikelyLocalEndpoint("https://fd.example.com/hook"))
        assertFalse(LocalNetworkPermission.isLikelyLocalEndpoint("https://8.8.8.8/hook"))
        assertFalse(LocalNetworkPermission.isLikelyLocalEndpoint("not a url"))
    }
}
