package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.VaultBackupCodec
import com.marble098.marblebr.core.model.VaultBackupFormat
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * رمزنگاریِ بستهٔ پشتیبان، جدا از دیتابیس تا بتوان آن را مستقل آزمایش کرد.
 *
 * طرح: PBKDF2-HMAC-SHA256 (نمکِ تصادفیِ ۱۶ بایتی) → AES-256-GCM (بردارِ تصادفیِ ۱۲ بایتی).
 * کلید هیچ‌وقت کنارِ فایل ذخیره نمی‌شود؛ فقط گذرواژهٔ کاربر آن را بازمی‌سازد.
 */
object BackupCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    /** دامنهٔ ثابتِ AAD تا بدنهٔ رمزشده جای دیگری قابلِ استفاده نباشد. */
    private const val ASSOCIATED_DATA = "marblebr-vault-backup-v1"

    private val random = SecureRandom()

    data class Sealed(val ciphertextBase64: String, val saltBase64: String, val ivBase64: String, val sha256: String)

    fun seal(
        plaintext: String,
        password: String,
        iterations: Int = VaultBackupFormat.KDF_ITERATIONS,
    ): Sealed {
        val salt = ByteArray(VaultBackupFormat.SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(VaultBackupFormat.IV_BYTES).also(random::nextBytes)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(ASSOCIATED_DATA.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return Sealed(
            ciphertextBase64 = encoder.encodeToString(ciphertext),
            saltBase64 = encoder.encodeToString(salt),
            ivBase64 = encoder.encodeToString(iv),
            sha256 = sha256Hex(ciphertext),
        )
    }

    /** @throws javax.crypto.AEADBadTagException اگر گذرواژه اشتباه یا بدنه دست‌کاری شده باشد. */
    fun open(
        ciphertextBase64: String,
        password: String,
        saltBase64: String,
        ivBase64: String,
        iterations: Int,
    ): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val iv = Base64.getDecoder().decode(ivBase64)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(ASSOCIATED_DATA.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64)), Charsets.UTF_8)
    }

    fun verify(ciphertextBase64: String, expectedSha256: String): Boolean {
        if (expectedSha256.isEmpty()) return true
        return runCatching {
            val bytes = Base64.getDecoder().decode(ciphertextBase64)
            MessageDigest.isEqual(sha256Hex(bytes).toByteArray(), expectedSha256.toByteArray())
        }.getOrDefault(false)
    }

    fun passwordCheck(saltBase64: String, iterations: Int, password: String): String =
        VaultBackupCodec.passwordCheck(saltBase64, iterations, password)

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations.coerceAtLeast(MIN_ITERATIONS), 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    const val MIN_ITERATIONS = 100_000
}
