package com.marble098.marblebr.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedVaultBlob(
    val ciphertextBase64: String,
    val ivBase64: String,
    val plaintextSha256: String,
)

/**
 * رابط تأمین‌کننده کلید متقارن AES-256 برای رمزنگاری Session Vault.
 * در محیط اجرایی اندروید از AndroidKeyStore استفاده می‌کند و در تست‌های واحد قابل جایگزینی است.
 */
fun interface SecretKeyProvider {
    fun getOrCreateSecretKey(): SecretKey
}

@Singleton
class AndroidKeystoreSecretKeyProvider @Inject constructor() : SecretKeyProvider {
    override fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "marblebr_session_vault_aes256_key"
    }
}

/**
 * موتور رمزنگاری و رمزگشایی AES-256-GCM همراه با بررسی یکپارچگی SHA-256 برای اسنپ‌شات‌های Session Vault.
 */
@Singleton
class AesGcmVaultCipher @Inject constructor(
    private val secretKeyProvider: SecretKeyProvider,
) {
    fun encrypt(
        plaintext: String,
        associatedData: String = "",
        includePlaintextHash: Boolean = true,
    ): EncryptedVaultBlob {
        val secretKey = secretKeyProvider.getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        }
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)
        val ivBytes = cipher.iv

        return EncryptedVaultBlob(
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertextBytes),
            ivBase64 = Base64.getEncoder().encodeToString(ivBytes),
            plaintextSha256 = if (includePlaintextHash) sha256Hex(plaintextBytes) else "",
        )
    }

    fun decrypt(blob: EncryptedVaultBlob, associatedData: String = ""): String {
        val secretKey = secretKeyProvider.getOrCreateSecretKey()
        val ivBytes = Base64.getDecoder().decode(blob.ivBase64)
        val ciphertextBytes = Base64.getDecoder().decode(blob.ciphertextBase64)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        }

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        val computedSha256 = sha256Hex(decryptedBytes)
        check(blob.plaintextSha256.isEmpty() || computedSha256 == blob.plaintextSha256) {
            "SHA-256 integrity check failed for Session Vault snapshot"
        }
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
