package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.CredentialDao
import com.marble098.marblebr.core.database.CredentialEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CredentialFields(
    val site: String,
    val username: String,
    val password: String,
    val notes: String = "",
)

data class SavedCredential(
    val id: String,
    val fields: CredentialFields,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class CredentialReadResult(
    val credentials: List<SavedCredential> = emptyList(),
    val unreadableCount: Int = 0,
    val storageUnavailable: Boolean = false,
)

/** Credentials are encrypted in full; neither site, username, password nor notes are Room columns. */
@Singleton
class CredentialVault @Inject constructor(
    private val dao: CredentialDao,
    private val cipher: AesGcmVaultCipher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Collect only while unlocked; keep unreadable records intact and report them to the user.
    fun observeCredentials(): Flow<CredentialReadResult> = dao.observeAll().map { entries ->
        val readable = entries.mapNotNull { entity -> runCatching { decrypt(entity) }.getOrNull() }
        CredentialReadResult(readable, unreadableCount = entries.size - readable.size)
    }.catch { emit(CredentialReadResult(storageUnavailable = true)) }

    suspend fun save(fields: CredentialFields, id: String? = null): SavedCredential {
        require(isValidSite(fields.site)) { "An HTTP(S) site URL is required" }
        require(fields.username.isNotBlank() || fields.password.isNotBlank()) { "Empty credentials" }
        val key = id ?: UUID.randomUUID().toString()
        val old = if (id != null) requireNotNull(dao.getById(id)) else null
        val now = System.currentTimeMillis()
        val encrypted = cipher.encrypt(
            plaintext = json.encodeToString(fields.copy(site = fields.site.trim())),
            associatedData = associatedData(key),
            // An unkeyed hash of a password record would enable offline guessing.
            includePlaintextHash = false,
        )
        val entity = CredentialEntity(
            id = key,
            encryptedPayloadBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            payloadSha256 = encrypted.plaintextSha256,
            createdAtEpochMillis = old?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )
        dao.upsert(entity)
        return decrypt(entity)
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    private fun decrypt(entity: CredentialEntity): SavedCredential {
        val plaintext = cipher.decrypt(
            EncryptedVaultBlob(entity.encryptedPayloadBase64, entity.ivBase64, entity.payloadSha256),
            associatedData(entity.id),
        )
        return SavedCredential(
            entity.id,
            json.decodeFromString<CredentialFields>(plaintext),
            entity.createdAtEpochMillis,
            entity.updatedAtEpochMillis,
        )
    }

    companion object {
        private fun associatedData(id: String) = "credential:$id"

        fun isValidSite(site: String): Boolean = runCatching {
            val uri = URI(site.trim())
            uri.scheme?.lowercase() in setOf("https", "http") &&
                !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)
    }
}

/** Cryptographically secure password generation, with all four character classes represented. */
object StrongPasswordGenerator {
    private val groups = listOf(
        "ABCDEFGHJKLMNPQRSTUVWXYZ",
        "abcdefghijkmnopqrstuvwxyz",
        "23456789",
        "!@#%+-_=",
    )
    private val random = SecureRandom()

    fun generate(length: Int = 20): String {
        require(length in 12..128)
        val alphabet = groups.joinToString("")
        val chars = groups.map { it[random.nextInt(it.length)] }.toMutableList()
        repeat(length - chars.size) { chars += alphabet[random.nextInt(alphabet.length)] }
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return chars.joinToString("")
    }
}
