package com.sysadmindoc.alarmclock.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {

    @Test
    fun encryptedBackupRoundTripRestoresPlainJson() {
        val plainJson = """{"version":5,"alarms":[{"label":"Gym"}],"settings":null}"""

        val encrypted = EncryptedBackupCodec.encrypt(plainJson, "correct horse battery staple")
        val decrypted = EncryptedBackupCodec.decrypt(encrypted, "correct horse battery staple")

        assertEquals(plainJson, decrypted)
        assertFalse(encrypted.contains("Gym"))
        assertTrue(encrypted.contains(EncryptedBackupCodec.FORMAT))
        assertTrue(encrypted.contains(EncryptedBackupCodec.CIPHER_ALGORITHM))
    }

    @Test
    fun wrongPassphraseCannotDecryptBackup() {
        val encrypted = EncryptedBackupCodec.encrypt("""{"version":5}""", "right passphrase")

        val result = runCatching {
            EncryptedBackupCodec.decrypt(encrypted, "wrong passphrase")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun missingPassphraseIsRejectedBeforeEncryption() {
        assertTrue(
            runCatching {
                EncryptedBackupCodec.encrypt("""{"version":5}""", "   ")
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
