package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.CredentialDao
import com.marble098.marblebr.core.model.AutofillCandidate
import com.marble098.marblebr.core.model.CredentialCaptureHeuristics
import com.marble098.marblebr.core.model.PasswordQualityChecker
import com.marble098.marblebr.core.model.VaultBackupCredential
import com.marble098.marblebr.core.database.CredentialEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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

    /* ------------------------------------------- پشتیبان‌گیری ------------------------------------------- */

    /**
     * رکوردهای قابلِ خواندن با شناسه و زمان، برای ساختِ بستهٔ پشتیبان.
     *
     * توجه: خروجی حاوی گذرواژهٔ رمزگشایی‌شده است و باید بی‌درنگ داخل بستهٔ رمزنگاری‌شده برود؛
     * هیچ‌جا ذخیره یا لاگ نمی‌شود.
     */
    suspend fun credentialsForBackup(): List<VaultBackupCredential> = dao.observeAll().first()
        .mapNotNull { entity ->
            runCatching {
                val saved = decrypt(entity)
                VaultBackupCredential(
                    id = saved.id,
                    site = saved.fields.site,
                    username = saved.fields.username,
                    password = saved.fields.password,
                    notes = saved.fields.notes,
                    createdAtEpochMillis = saved.createdAtEpochMillis,
                    updatedAtEpochMillis = saved.updatedAtEpochMillis,
                )
            }.getOrNull()
        }

    /** تعدادِ رکوردهایی که با کلیدِ فعلی باز نمی‌شوند؛ صادقانه در خروجیِ پشتیبان گزارش می‌شود. */
    suspend fun unreadableCredentialCount(): Int {
        val all = dao.observeAll().first()
        val readable = all.count { entity -> runCatching { decrypt(entity) }.isSuccess }
        return all.size - readable
    }

    /**
     * نوشتنِ یک رکورد از بستهٔ پشتیبان.
     *
     * @param id اگر داده شود، همان رکورد به‌روز می‌شود (تا AAD و شناسه دست‌نخورده بماند).
     */
    suspend fun saveFromBackup(record: VaultBackupCredential, id: String? = null): SavedCredential {
        val targetId = id ?: record.id
        val existing = dao.getById(targetId)
        val now = System.currentTimeMillis()
        require(isValidSite(record.site)) { "An HTTP(S) site URL is required" }
        require(record.username.isNotBlank() || record.password.isNotBlank()) { "Empty credentials" }
        val fields = CredentialFields(
            site = record.site.trim(),
            username = record.username,
            password = record.password,
            notes = record.notes,
        )
        val encrypted = cipher.encrypt(
            plaintext = json.encodeToString(fields),
            associatedData = associatedData(targetId),
            includePlaintextHash = false,
        )
        val entity = CredentialEntity(
            id = targetId,
            encryptedPayloadBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            payloadSha256 = encrypted.plaintextSha256,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: record.createdAtEpochMillis,
            updatedAtEpochMillis = now,
        )
        dao.upsert(entity)
        return decrypt(entity)
    }

    /** کاندیدهای تصمیمِ پرکردنِ خودکار؛ بدونِ گذرواژه و بدونِ رکوردهای ناخوانا. */
    suspend fun autofillCandidates(): List<AutofillCandidate> =
        credentialsForBackup().map { record ->
            AutofillCandidate(
                id = record.id,
                domain = CredentialCaptureHeuristics.domainOf(record.site),
                username = record.username,
                updatedAtEpochMillis = record.updatedAtEpochMillis,
                strengthScore = PasswordQualityChecker.evaluate(
                    record.password,
                    record.username,
                    CredentialCaptureHeuristics.domainOf(record.site),
                ).score,
            )
        }

    /** گذرواژه فقط پس از تأییدِ صریحِ کاربر و فقط برای همان لحظهٔ پرکردن خوانده می‌شود. */
    suspend fun credentialsForAutofill(id: String): AutofillSecret? {
        val entity = dao.getById(id) ?: return null
        val saved = runCatching { decrypt(entity) }.getOrNull() ?: return null
        if (saved.fields.password.isEmpty()) return null
        return AutofillSecret(username = saved.fields.username, password = saved.fields.password)
    }

    /** ذخیرهٔ خودکار یک ورودِ موفق؛ همان مسیرِ امنِ [save] با شناسه‌ای که از قبل داریم. */
    suspend fun saveCaptured(site: String, username: String, password: String, id: String? = null): SavedCredential =
        save(CredentialFields(site = site, username = username, password = password), id)

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

        private val UNSAFE_SCHEMES = setOf("javascript", "data", "file", "about", "blob", "content")

        /**
         * «هر سایتی» یعنی آدرسِ کوتاه هم پذیرفته می‌شود.
         *
         * پیش‌تر فقط `https://…` کامل قبول می‌شد و کاربری که `my.mci.ir` می‌نوشت پیامِ «آدرس
         * معتبر نیست» می‌گرفت. حالا دامنهٔ خالی هم به HTTPS تبدیل می‌شود؛ فقط اسکیم‌های خطرناک
         * (`javascript:`، `data:`، `file:` و مانندِ آن‌ها) و آدرسِ دارای `userInfo` رد می‌شوند،
         * چون همان‌ها ابزارِ فیشینگ و تزریق‌اند.
         */
        fun isValidSite(site: String): Boolean = runCatching {
            val trimmed = site.trim()
            if (trimmed.isEmpty()) return false
            val uri = URI(if (trimmed.contains("://")) trimmed else "https://$trimmed")
            val scheme = uri.scheme?.lowercase() ?: return false
            scheme !in UNSAFE_SCHEMES &&
                (scheme == "https" || scheme == "http") &&
                !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)

        /** آدرس را به شکلِ یکدست (`https://host/…`) درمی‌آورد؛ برای ذخیرهٔ ورودِ کوتاه. */
        fun normalizeSite(site: String): String {
            val trimmed = site.trim()
            if (trimmed.isEmpty()) return trimmed
            val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            return runCatching {
                val uri = URI(withScheme)
                if (uri.host.isNullOrBlank()) {
                    withScheme
                } else {
                    val scheme = uri.scheme.lowercase()
                    val path = uri.rawPath.orEmpty()
                    val query = uri.rawQuery?.let { "?$it" }.orEmpty()
                    "$scheme://${uri.host.lowercase()}$path$query"
                }
            }.getOrDefault(withScheme)
        }
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
