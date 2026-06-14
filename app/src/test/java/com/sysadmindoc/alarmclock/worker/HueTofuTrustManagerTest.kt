package com.sysadmindoc.alarmclock.worker

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Unit coverage for the Hue bridge TOFU (Trust On First Use) certificate
 * pinning introduced for the v2 HTTPS client. The bridge cert is self-signed
 * with a non-matching CN, so the app pins it by SHA-256 fingerprint instead of
 * trusting a CA chain.
 */
class HueTofuTrustManagerTest {

    private fun fakeCert(encoded: ByteArray): X509Certificate =
        mockk<X509Certificate>(relaxed = true).also {
            every { it.encoded } returns encoded
        }

    private fun fingerprintOf(encoded: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(encoded)
            .joinToString("") { "%02x".format(it) }

    @Test
    fun blankPinAcceptsAnyCertAndRecordsFingerprint() {
        val encoded = byteArrayOf(1, 2, 3, 4, 5)
        val tm = HueSunriseWorker.TofuTrustManager(pinnedFingerprint = "")

        tm.checkServerTrusted(arrayOf(fakeCert(encoded)), "RSA")

        assertEquals(fingerprintOf(encoded), tm.observedFingerprint)
    }

    @Test
    fun matchingPinIsAccepted() {
        val encoded = byteArrayOf(10, 20, 30)
        val pinned = fingerprintOf(encoded)
        val tm = HueSunriseWorker.TofuTrustManager(pinnedFingerprint = pinned)

        // Uppercase pin must still match (comparison is case-insensitive).
        val upperTm = HueSunriseWorker.TofuTrustManager(pinnedFingerprint = pinned.uppercase())

        tm.checkServerTrusted(arrayOf(fakeCert(encoded)), "RSA")
        upperTm.checkServerTrusted(arrayOf(fakeCert(encoded)), "RSA")

        assertEquals(pinned, tm.observedFingerprint)
    }

    @Test
    fun changedCertWithPinnedFingerprintIsRejected() {
        val pinned = fingerprintOf(byteArrayOf(1, 1, 1))
        val tm = HueSunriseWorker.TofuTrustManager(pinnedFingerprint = pinned)

        assertThrows(CertificateException::class.java) {
            tm.checkServerTrusted(arrayOf(fakeCert(byteArrayOf(9, 9, 9))), "RSA")
        }
    }

    @Test
    fun emptyChainIsRejected() {
        val tm = HueSunriseWorker.TofuTrustManager(pinnedFingerprint = "")

        assertThrows(CertificateException::class.java) {
            tm.checkServerTrusted(emptyArray(), "RSA")
        }
        assertThrows(CertificateException::class.java) {
            tm.checkServerTrusted(null, "RSA")
        }
    }

    @Test
    fun companionFingerprintMatchesManualDigest() {
        val encoded = byteArrayOf(42, 7, 13, 99)
        assertEquals(fingerprintOf(encoded), HueSunriseWorker.certFingerprint(fakeCert(encoded)))
    }
}
