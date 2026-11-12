package com.sysadmindoc.alarmclock.integration.dirigera

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Proof Key for Code Exchange (RFC 7636), which the DIRIGERA hub requires when
 * a new client pairs. The verifier never leaves the phone until the token
 * request, so a code someone else reads off the network is useless to them.
 */
object DirigeraPkce {
    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    /** RFC 7636 allows 43 to 128 characters; the longest gives the most entropy. */
    const val VERIFIER_LENGTH = 128

    fun newVerifier(random: SecureRandom = SecureRandom()): String =
        buildString(VERIFIER_LENGTH) {
            repeat(VERIFIER_LENGTH) { append(UNRESERVED[random.nextInt(UNRESERVED.length)]) }
        }

    /** The S256 challenge: base64url of the SHA-256 of the verifier, without padding. */
    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
