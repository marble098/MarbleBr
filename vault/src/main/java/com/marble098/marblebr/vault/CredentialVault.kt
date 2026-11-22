package com.marble098.marblebr.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.marble098.marblebr.core.database.CredentialDao
import com.marble098.marblebr.core.model.AutofillCandidate
import com.marble098.marblebr.core.model.CredentialCaptureHeuristics
import com.marble098.marblebr.core.model.PasswordFingerprint
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
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CredentialFields(
    val site: String,
    val username: String,
    val password: String,
    val notes: String = "",
    /** آخرین رسیدنِ کدِ یکبارمصرف به این حساب (برای نمایشِ «تازه»)؛ عددِ غیرحساس است. */
    val lastOneTimeCodeAtEpochMillis: Long = 0L,
    /** آخرین استفاده برای پرکردنِ خودکار؛ فقط زمان، نه محتوا. */
    val lastAutofillAtEpochMillis: Long = 0L,
) {
    /** طولِ گذرواژه برای نمایشِ وضعیت؛ خودِ گذرواژه هرگز در فراداده نمی‌رود. */
    val passwordLength: Int get() = password.length
}

data class SavedCredential(
    val id: String,
    val fields: CredentialFields,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    /** دامنهٔ ثبت‌شدهٔ همین رکورد؛ پایهٔ تصمیمِ «کدام حساب پر شود». */
    val domain: String get() = CredentialCaptureHeuristics.domainOf(fields.site)

    /** گذرواژهٔ کوتاه و بی‌اثر است (کدِ یکبارمصرف یا گذرواژهٔ خالی). */
    val looksLikePlaceholder: Boolean get() = fields.password.length < 6 || fields.password.all(Char::isDigit)
}

/**
 * نام کاربری و گذرواژه برای **یک** پرکردن، در همان لحظه.
 *
 * چرا نوعِ جدا و نه [SavedCredential]؟ چون این شیء فقط دو مقدارِ لازم را دارد و عمرش به همان
 * فراخوانی ختم می‌شود؛ هرچه کمتر در دستِ لایه‌های بالاتر بچرخد، احتمالِ ماندنِ گذرواژه در حافظه
 * یا رسیدنش به لاگ کمتر است.
 */
data class AutofillSecret(
    val username: String,
    val password: String,
) {
    /** نمای پوشیده برای لاگ/آزمون؛ هرگز گذرواژه را برنمی‌گرداند. */
    override fun toString(): String = "AutofillSecret(username=$username, password=***)"
}

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
    private val fingerprintKeyProvider: FingerprintKeyProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Collect only while unlocked; keep unreadable records intact and report them to the user.
    fun observeCredentials(): Flow<CredentialReadResult> = dao.observeAll().map { entries ->
        val readable = entries.mapNotNull { entity -> runCatching { decryptOrThrow(entity) }.getOrNull() }
        CredentialReadResult(readable, unreadableCount = entries.size - readable.size)
    }.catch { emit(CredentialReadResult(storageUnavailable = true)) }

    suspend fun save(fields: CredentialFields, id: String? = null): SavedCredential {
        require(isValidSite(fields.site)) { "An HTTP(S) site URL is required" }
        require(fields.username.isNotBlank() || fields.password.isNotBlank()) { "Empty credentials" }
        val key = id ?: UUID.randomUUID().toString()
        val old = if (id != null) requireNotNull(dao.getById(id)) else null
        val now = System.currentTimeMillis()
        // فرادادهٔ «آخرین استفاده» و «آخرین کد» نباید با یک ذخیرهٔ تازه صفر شود؛ مثلاً وقتی
        // گذرواژهٔ عوض‌شده با `saveCaptured` نوشته می‌شود، ترتیبِ اولویتِ حساب‌ها نباید بپرد.
        val previous = old?.let { runCatching { decryptOrThrow(it) }.getOrNull() }?.fields
        val merged = fields.copy(
            site = fields.site.trim(),
            lastOneTimeCodeAtEpochMillis = if (fields.lastOneTimeCodeAtEpochMillis > 0) {
                fields.lastOneTimeCodeAtEpochMillis
            } else {
                previous?.lastOneTimeCodeAtEpochMillis ?: 0L
            },
            lastAutofillAtEpochMillis = if (fields.lastAutofillAtEpochMillis > 0) {
                fields.lastAutofillAtEpochMillis
            } else {
                previous?.lastAutofillAtEpochMillis ?: 0L
            },
        )
        val encrypted = cipher.encrypt(
            plaintext = json.encodeToString(merged),
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
        return decryptOrThrow(entity)
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    /* ------------------------------------- اثر انگشتِ گذرواژه ------------------------------------- */

    /**
     * اثر انگشتِ کوتاهِ گذرواژهٔ یک رکورد (HMAC با کلیدِ دستگاه + شناسه به‌عنوانِ نمک).
     *
     * برای تصمیمِ «آیا گذرواژه عوض شده؟» لازم است و هرگز خودِ گذرواژه یا هشِ بدونِ کلید را
     * بیرون نمی‌دهد. فقط پس از رمزگشاییِ کاملِ رکورد ساخته می‌شود.
     */
    suspend fun passwordFingerprint(id: String): String? {
        val entity = dao.getById(id) ?: return null
        val saved = runCatching { decryptOrThrow(entity) }.getOrNull() ?: return null
        if (saved.fields.password.isEmpty()) return null
        return fingerprintOf(saved.fields.password, entity.id)
    }

    /** محاسبهٔ اثر انگشت با نمکِ شناسهٔ رکورد (تابعِ خالص برای آزمون). */
    fun fingerprintOf(password: String, salt: String): String =
        PasswordFingerprint.of(password, mac = fingerprintKeyProvider.fingerprintMac(), salt = salt)

    /* ------------------------------------ فرادادهٔ سبک برای رابط ------------------------------------ */

    /**
     * نمایی سبک از رکوردها برای تصمیم‌های «پرکردن» و «ذخیره» — بدونِ گذرواژه.
     *
     * سه چیز لازم است: کدام حساب‌ها برای این دامنه داریم، کدام تازه‌تر استفاده شده، و گذرواژهٔ
     * ذخیره‌شده چه اندازه/قدرتی دارد. همه فقط پس از رمزگشایی محاسبه می‌شوند و هیچ‌کدام در
     * جایی بیرون از اپ منتشر نمی‌شوند.
     */
    suspend fun loginAccountIndex(): List<SavedCredential> =
        dao.observeAll().first().mapNotNull { entity ->
            runCatching { decryptOrThrow(entity) }.getOrNull()
        }

    /** یک رکورد کامل (رمزگشایی‌شده) با شناسه؛ فقط برای همان لحظهٔ تصمیم‌گیری. */
    suspend fun credentialById(id: String): SavedCredential? {
        val entity = dao.getById(id) ?: return null
        return runCatching { decryptOrThrow(entity) }.getOrNull()
    }

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
                val saved = decryptOrThrow(entity)
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
        val readable = all.count { entity -> runCatching { decryptOrThrow(entity) }.isSuccess }
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
        return decryptOrThrow(entity)
    }

    /** کاندیدهای تصمیمِ پرکردنِ خودکار؛ بدونِ گذرواژه و بدونِ رکوردهای ناخوانا. */
    suspend fun autofillCandidates(): List<AutofillCandidate> =
        loginAccountIndex().map { saved ->
            AutofillCandidate(
                id = saved.id,
                domain = saved.domain,
                username = saved.fields.username,
                updatedAtEpochMillis = saved.updatedAtEpochMillis,
                lastUsedAtEpochMillis = saved.fields.lastAutofillAtEpochMillis.takeIf { it > 0L },
                strengthScore = PasswordQualityChecker.evaluate(
                    saved.fields.password,
                    saved.fields.username,
                    saved.domain,
                ).score,
            )
        }

    /**
     * ثبتِ «این حساب همین حالا برای پرکردن استفاده شد» و/یا «کدِ یکبارمصرف به آن رسید».
     * فقط فرادادهٔ زمانی نوشته می‌شود؛ گذرواژه دست‌نخورده می‌ماند.
     */
    suspend fun touchCredential(
        id: String,
        autofilledAtEpochMillis: Long? = null,
        oneTimeCodeAtEpochMillis: Long? = null,
    ): Boolean {
        val entity = dao.getById(id) ?: return false
        val saved = runCatching { decryptOrThrow(entity) }.getOrNull() ?: return false
        if (autofilledAtEpochMillis == null && oneTimeCodeAtEpochMillis == null) return true
        val updated = saved.fields.copy(
            lastAutofillAtEpochMillis = autofilledAtEpochMillis ?: saved.fields.lastAutofillAtEpochMillis,
            lastOneTimeCodeAtEpochMillis = oneTimeCodeAtEpochMillis ?: saved.fields.lastOneTimeCodeAtEpochMillis,
        )
        save(updated, id)
        return true
    }

    /** گذرواژه فقط پس از تأییدِ صریحِ کاربر و فقط برای همان لحظهٔ پرکردن خوانده می‌شود. */
    suspend fun credentialsForAutofill(id: String): AutofillSecret? {
        val entity = dao.getById(id) ?: return null
        val saved = runCatching { decryptOrThrow(entity) }.getOrNull() ?: return null
        if (saved.fields.password.isEmpty()) return null
        return AutofillSecret(username = saved.fields.username, password = saved.fields.password)
    }

    /** ذخیرهٔ خودکار یک ورودِ موفق؛ همان مسیرِ امنِ [save] با شناسه‌ای که از قبل داریم. */
    suspend fun saveCaptured(site: String, username: String, password: String, id: String? = null): SavedCredential =
        save(CredentialFields(site = site, username = username, password = password), id)

    private fun decryptOrThrow(entity: CredentialEntity): SavedCredential {
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
                    // `www.` حذف می‌شود تا ورودِ ذخیره‌شده با دامنهٔ سایت در پرکردنِ خودکار
                    // یکی باشد (همان قاعده‌ای که CredentialCaptureHeuristics.domainOf دارد).
                    val host = uri.host.lowercase().removePrefix("www.")
                    "$scheme://$host$path$query"
                }
            }.getOrDefault(withScheme)
        }
    }
}

/**
 * تأمین‌کنندهٔ HMACِ اثر انگشتِ گذرواژه.
 *
 * کلید از Android Keystore خوانده/ساخته می‌شود و **قابل استخراج نیست**؛ بنابراین به‌جای بایت‌های
 * کلید، یک [Mac] آمادهٔ استفاده می‌دهیم. این تفاوت مهم است: خواندنِ `key.encoded` روی کلیدِ
 * Keystore `null` برمی‌گرداند و اثر انگشت را همیشه بی‌اثر می‌کرد.
 */
fun interface FingerprintKeyProvider {
    fun fingerprintMac(): javax.crypto.Mac
}

/**
 * کلیدِ HMAC روی `AndroidKeyStore`.
 *
 * چرا `HmacSHA256` و نه یک رشتهٔ تصادفی در SharedPreferences؟ چون کلیدِ سخت‌افزاری قابل استخراج
 * نیست؛ حتی با دسترسیِ فیزیکی به فایل‌های اپ، رمزگشاییِ گذرواژه از روی اثر انگشت ممکن نیست.
 */
@Singleton
class KeystoreFingerprintKeyProvider @Inject constructor() : FingerprintKeyProvider {
    /**
     * کلید در اولین استفاده ساخته می‌شود (نه در سازنده) تا بازکردنِ اپ هرگز به Keystore
     * وابسته نباشد؛ اگر دستگاه Keystore نداشته باشد، فقط «تشخیصِ تغییرِ گذرواژه» بی‌اثر می‌شود
     * و بقیهٔ کارِ ورود سالم می‌ماند.
     */
    override fun fingerprintMac(): javax.crypto.Mac {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        val key = existing ?: createKey()
        // کلیدِ Keystore قابل استخراج نیست: `encoded` آن `null` است و باید خودِ کلید به `Mac`
        // داده شود، با providerِ AndroidKeyStore. اگر بایت‌ها خوانده شوند، امضای HMAC همیشه خطا
        // می‌دهد و مسیرِ «گذرواژه عوض شد» بی‌صدا از کار می‌افتد.
        return javax.crypto.Mac.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            ANDROID_KEYSTORE,
        ).apply { init(key) }
    }

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "marblebr_password_fingerprint_hmac"
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
