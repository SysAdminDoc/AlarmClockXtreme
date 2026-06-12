package com.sysadmindoc.alarmclock.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {

    @Test
    fun encryptedBackupRoundTripRestoresPlainJson() {
        val plainJson = """{"version":6,"alarms":[{"label":"Gym"}],"settings":null}"""

        val encrypted = EncryptedBackupCodec.encrypt(plainJson, "correct horse battery staple")
        val decrypted = EncryptedBackupCodec.decrypt(encrypted, "correct horse battery staple")

        assertEquals(plainJson, decrypted)
        assertFalse(encrypted.contains("Gym"))
        assertTrue(encrypted.contains(EncryptedBackupCodec.FORMAT))
        assertTrue(encrypted.contains(EncryptedBackupCodec.CIPHER_ALGORITHM))
        assertTrue(encrypted.contains("\"iterations\": ${EncryptedBackupCodec.PBKDF2_ITERATIONS}"))
    }

    @Test
    fun legacyIterationEnvelopeStillDecrypts() {
        val plainJson = """{"version":8,"settings":{"is24HourFormat":true}}"""
        val encrypted = EncryptedBackupCodec.encrypt(
            plainJson = plainJson,
            passphrase = "old backup passphrase",
            iterations = EncryptedBackupCodec.LEGACY_PBKDF2_ITERATIONS
        )

        val decrypted = EncryptedBackupCodec.decrypt(encrypted, "old backup passphrase")

        assertEquals(plainJson, decrypted)
        assertTrue(encrypted.contains("\"iterations\": ${EncryptedBackupCodec.LEGACY_PBKDF2_ITERATIONS}"))
    }

    @Test
    fun wrongPassphraseCannotDecryptBackup() {
        val encrypted = EncryptedBackupCodec.encrypt("""{"version":6}""", "right passphrase")

        val result = runCatching {
            EncryptedBackupCodec.decrypt(encrypted, "wrong passphrase")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun missingPassphraseIsRejectedBeforeEncryption() {
        assertTrue(
            runCatching {
                EncryptedBackupCodec.encrypt("""{"version":6}""", "   ")
            }.isFailure
        )
    }

    @Test
    fun invalidEnvelopeIsRejected() {
        val result = runCatching {
            EncryptedBackupCodec.decrypt("""{"format":"not-acx","version":1}""", "passphrase")
        }

        assertTrue(result.isFailure)
    }
}
