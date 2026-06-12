package com.sysadmindoc.alarmclock.data.backup

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@JsonClass(generateAdapter = true)
data class EncryptedBackupData(
    val format: String = EncryptedBackupCodec.FORMAT,
    val version: Int = EncryptedBackupCodec.VERSION,
    val kdf: String = EncryptedBackupCodec.KDF_ALGORITHM,
    val cipher: String = EncryptedBackupCodec.CIPHER_ALGORITHM,
    val iterations: Int = EncryptedBackupCodec.PBKDF2_ITERATIONS,
    val salt: String,
    val iv: String,
    val payload: String
)

object EncryptedBackupCodec {
    const val FORMAT = "acx-encrypted-backup"
    const val VERSION = 1
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    const val PBKDF2_ITERATIONS = 600_000
    internal const val LEGACY_PBKDF2_ITERATIONS = 210_000

    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 1_000_000

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(EncryptedBackupData::class.java).indent("  ")
    private val base64Encoder = Base64.getEncoder()
    private val base64Decoder = Base64.getDecoder()

    fun encrypt(
        plainJson: String,
        passphrase: String,
        secureRandom: SecureRandom = SecureRandom(),
        iterations: Int = PBKDF2_ITERATIONS
    ): String {
        require(passphrase.isNotBlank()) { "Backup passphrase is required" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported backup encryption settings" }

        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
        val envelope = EncryptedBackupData(
            iterations = iterations,
            salt = encode(salt),
            iv = encode(iv),
            payload = ""
        )

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, envelope.iterations), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(envelope.aad())
        val encrypted = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))

        return adapter.toJson(envelope.copy(payload = encode(encrypted)))
    }

    fun decrypt(encryptedJson: String, passphrase: String): String {
        require(passphrase.isNotBlank()) { "Backup passphrase is required" }

        val envelope = adapter.fromJson(encryptedJson)
            ?: throw IllegalArgumentException("Invalid encrypted backup format")
        validate(envelope)

        val salt = decode(envelope.salt)
        val iv = decode(envelope.iv)
        val payload = decode(envelope.payload)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            deriveKey(passphrase, salt, envelope.iterations),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(envelope.aad())
        return String(cipher.doFinal(payload), StandardCharsets.UTF_8)
    }

    private fun validate(envelope: EncryptedBackupData) {
        require(envelope.format == FORMAT) { "Invalid encrypted backup format" }
        require(envelope.version == VERSION) { "Unsupported encrypted backup version ${envelope.version}" }
        require(envelope.kdf == KDF_ALGORITHM) { "Unsupported backup key derivation" }
        require(envelope.cipher == CIPHER_ALGORITHM) { "Unsupported backup cipher" }
        require(envelope.iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported backup encryption settings" }
        require(envelope.salt.isNotBlank() && envelope.iv.isNotBlank() && envelope.payload.isNotBlank()) {
            "Encrypted backup is missing required data"
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val chars = passphrase.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
        return try {
            val keyBytes = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun EncryptedBackupData.aad(): ByteArray {
        return listOf(format, version, kdf, cipher, iterations).joinToString("|")
            .toByteArray(StandardCharsets.UTF_8)
    }

    private fun encode(bytes: ByteArray): String = base64Encoder.encodeToString(bytes)

    private fun decode(value: String): ByteArray = base64Decoder.decode(value)
}
