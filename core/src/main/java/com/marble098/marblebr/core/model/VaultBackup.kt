package com.marble098.marblebr.core.model

import kotlinx.serialization.Serializable

/**
 * «پشتیبان‌گیری از گذرواژه‌ها و ورودها» — منطقِ خالصِ ساخت/خواندن/ادغامِ بستهٔ پشتیبان.
 *
 * قاعده‌های امنیتی که در همین فایل تضمین می‌شوند:
 * - بستهٔ پشتیبان **همیشه** رمزنگاری‌شده است؛ ساختارِ فایل هیچ نام کاربری، گذرواژه، آدرس سایت،
 *   کوکی یا نشستی را به‌صورتِ متنِ ساده ندارد.
 * - گذرواژهٔ بسته هرگز در فایل ذخیره نمی‌شود؛ فقط یک «اثرانگشتِ تأیید» تا اگر کاربر رمز را
 *   اشتباه بزند، پیامِ روشن بگیرد نه خطای مبهم.
 * - ادغام، **هیچ‌وقت** دادهٔ موجود را بی‌خبر حذف نمی‌کند: یا نادیده می‌گیرد، یا فقط تازه‌تر را
 *   می‌نویسد، یا اگر کاربر «جایگزینی» بخواهد صریحاً از او تأیید گرفته می‌شود.
 */
object VaultBackupFormat {
    const val FORMAT_VERSION = 1
    const val FILE_PREFIX = "marblebr-backup"
    const val FILE_EXTENSION = "mbrx"

    /** شمارندهٔ تکرار برای مشتق‌سازیِ کلید از گذرواژه (PBKDF2-HMAC-SHA256). */
    const val KDF_ITERATIONS = 210_000
    const val SALT_BYTES = 16
    const val IV_BYTES = 12

    fun suggestedFileName(nowEpochMillis: Long): String {
        val stamp = java.time.Instant.ofEpochMilli(nowEpochMillis)
            .atZone(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        return "$FILE_PREFIX-$stamp.$FILE_EXTENSION"
    }

    /** آیا این نام، شکلِ یک فایلِ پشتیبانِ ما را دارد؟ (فقط برای راهنمای انتخاب فایل) */
    fun looksLikeBackup(fileName: String): Boolean {
        val lowered = fileName.lowercase()
        return lowered.endsWith(".$FILE_EXTENSION") || lowered.contains(FILE_PREFIX)
    }
}

/** محتوای رمزنگاری‌نشدهٔ بسته؛ فقط در حافظه و فقط در همان لحظه وجود دارد. */
@Serializable
data class VaultBackupPayload(
    val createdAtEpochMillis: Long,
    val appVersionName: String,
    val kdf: KdfMetadata = KdfMetadata(),
    /** رکوردهای ورود؛ نام کاربری و گذرواژه فقط داخل همین ساختارِ رمزنگاری‌شده می‌آیند. */
    val credentials: List<VaultBackupCredential> = emptyList(),
    /** خلاصهٔ نشست‌ها: کوکی‌ها/حافظهٔ رمزنگاری‌شدهٔ خودِ Session Vault (بدون کلید). */
    val sessions: List<VaultBackupSession> = emptyList(),
    val sites: List<VaultBackupSite> = emptyList(),
)

@Serializable
data class KdfMetadata(
    val algorithm: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = VaultBackupFormat.KDF_ITERATIONS,
    val saltBase64: String = "",
)

@Serializable
data class VaultBackupCredential(
    val id: String,
    val site: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val createdAtEpochMillis: Long = 0,
    val updatedAtEpochMillis: Long = 0,
    val autofillOrigin: String? = null,
)

@Serializable
data class VaultBackupSession(
    val id: String,
    val containerId: String,
    val domain: String,
    val capturedAtEpochMillis: Long,
    val ciphertextBase64: String,
    val ivBase64: String,
    val payloadSha256: String,
    val cookieCount: Int,
    val storageKeyCount: Int,
    val isHealthy: Boolean,
    val triggerReason: String,
    val label: String,
    val isPinned: Boolean,
)

@Serializable
data class VaultBackupSite(
    val domain: String,
    val displayName: String,
    val neverSignOutEnabled: Boolean,
    val cookieExpirationDays: Int,
    val keepAliveEnabled: Boolean,
    val keepAliveIntervalMinutes: Int,
)

/** سربرگِ روشنِ فایل؛ هیچ داده‌ای از کاربر در آن نیست. */
data class VaultBackupHeader(
    val magic: String = "MARBLEBR-VAULT-BACKUP",
    val formatVersion: Int = VaultBackupFormat.FORMAT_VERSION,
    val createdAtEpochMillis: Long = 0,
    val iterations: Int = VaultBackupFormat.KDF_ITERATIONS,
    val saltBase64: String = "",
    val ivBase64: String = "",
    val ciphertextSha256: String = "",
    val kdfCheckBase64: String = "",
    val credentialCount: Int = 0,
    val sessionCount: Int = 0,
    val siteCount: Int = 0,
)

sealed interface VaultBackupParseResult {
    data class Ready(val header: VaultBackupHeader, val ciphertextBase64: String) : VaultBackupParseResult
    data class Invalid(val reason: BackupProblem) : VaultBackupParseResult
}

enum class BackupProblem {
    NOT_A_BACKUP,
    UNSUPPORTED_VERSION,
    CORRUPTED,
    EMPTY_PASSWORD,
    WRONG_PASSWORD,
    EMPTY_BACKUP,
}

/**
 * سریال‌سازیِ متنی سربرگ. عمداً JSON نیست: خواندنِ آن با یک نگاه هم ممکن است، و هیچ کلیدِ
 * ناشناسی نمی‌تواند جای دادهٔ واقعی را بگیرد.
 *
 * قالب: خطِ اول `MARBLEBR-VAULT-BACKUP`, سپس `key=value`ها و در انتها بدنهٔ Base64 در خطوطِ ۷۶
 * کاراکتری.
 */
object VaultBackupCodec {

    private const val BODY_SEPARATOR = "---BODY---"

    fun encode(header: VaultBackupHeader, ciphertextBase64: String): String = buildString {
        appendLine(header.magic)
        appendLine("format=${header.formatVersion}")
        appendLine("createdAt=${header.createdAtEpochMillis}")
        appendLine("iterations=${header.iterations}")
        appendLine("salt=${header.saltBase64}")
        appendLine("iv=${header.ivBase64}")
        appendLine("sha256=${header.ciphertextSha256}")
        appendLine("kdfCheck=${header.kdfCheckBase64}")
        appendLine("credentials=${header.credentialCount}")
        appendLine("sessions=${header.sessionCount}")
        appendLine("sites=${header.siteCount}")
        appendLine(BODY_SEPARATOR)
        ciphertextBase64.chunked(76).forEach { appendLine(it) }
    }

    fun decode(text: String): VaultBackupParseResult {
        val normalized = text.replace("\r\n", "\n").trim()
        if (normalized.isEmpty()) return VaultBackupParseResult.Invalid(BackupProblem.NOT_A_BACKUP)
        if (!normalized.startsWith("MARBLEBR-VAULT-BACKUP")) {
            return VaultBackupParseResult.Invalid(BackupProblem.NOT_A_BACKUP)
        }
        val separatorIndex = normalized.indexOf(BODY_SEPARATOR)
        if (separatorIndex < 0) return VaultBackupParseResult.Invalid(BackupProblem.CORRUPTED)

        val head = normalized.substring(0, separatorIndex).trim().lines().filter { it.isNotBlank() }
        val values = head.drop(1).mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) null else line.substring(0, index).trim() to line.substring(index + 1).trim()
        }.toMap()

        val version = values["format"]?.toIntOrNull() ?: return VaultBackupParseResult.Invalid(BackupProblem.CORRUPTED)
        if (version > VaultBackupFormat.FORMAT_VERSION) {
            return VaultBackupParseResult.Invalid(BackupProblem.UNSUPPORTED_VERSION)
        }
        val body = normalized.substring(separatorIndex + BODY_SEPARATOR.length)
            .lines()
            .joinToString("") { it.trim() }
            .filterNot { it.isWhitespace() }
        if (body.isEmpty()) return VaultBackupParseResult.Invalid(BackupProblem.CORRUPTED)
        if (!body.matches(Regex("^[A-Za-z0-9+/=]+$"))) {
            return VaultBackupParseResult.Invalid(BackupProblem.CORRUPTED)
        }

        val header = VaultBackupHeader(
            formatVersion = version,
            createdAtEpochMillis = values["createdAt"]?.toLongOrNull() ?: 0L,
            iterations = values["iterations"]?.toIntOrNull()?.takeIf { it >= 100_000 }
                ?: VaultBackupFormat.KDF_ITERATIONS,
            saltBase64 = values["salt"].orEmpty(),
            ivBase64 = values["iv"].orEmpty(),
            ciphertextSha256 = values["sha256"].orEmpty(),
            kdfCheckBase64 = values["kdfCheck"].orEmpty(),
            credentialCount = values["credentials"]?.toIntOrNull() ?: 0,
            sessionCount = values["sessions"]?.toIntOrNull() ?: 0,
            siteCount = values["sites"]?.toIntOrNull() ?: 0,
        )
        if (header.saltBase64.isEmpty() || header.ivBase64.isEmpty()) {
            return VaultBackupParseResult.Invalid(BackupProblem.CORRUPTED)
        }
        return VaultBackupParseResult.Ready(header, body)
    }

    /** «اثرانگشتِ تأیید» گذرواژهٔ بسته تا رمزِ اشتباه، پیامِ روشن بدهد. */
    fun passwordCheck(saltBase64: String, iterations: Int, password: String): String {
        val salt = runCatching { java.util.Base64.getDecoder().decode(saltBase64) }.getOrDefault(ByteArray(0))
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations.coerceAtLeast(1), 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(derived)
        return java.util.Base64.getEncoder().encodeToString(digest)
    }
}

/** چه چیزی از بستهٔ پشتیبان اعمال شود. */
data class BackupRestoreOptions(
    val restoreCredentials: Boolean = true,
    val restoreSessions: Boolean = true,
    val restoreSites: Boolean = true,
    /** اگر روشن باشد، رکوردهای تکراری با نسخهٔ داخلِ بسته جایگزین می‌شوند. */
    val overwriteExisting: Boolean = true,
)

data class BackupConflict<T>(val incoming: T, val existing: T)

enum class MergeStrategy { ADD, UPDATE, SKIP }

/**
 * ادغامِ قطعی و بی‌خطر. خروجی همیشه توضیح می‌دهد چه اتفاقی می‌افتد تا رابط کاربری بتواند
 * پیش از نوشتن، پیش‌نمایشِ صادقانه نشان دهد.
 */
data class MergePlan<T>(
    val additions: List<T> = emptyList(),
    val updates: List<T> = emptyList(),
    val skipped: List<T> = emptyList(),
) {
    val totalChanges: Int get() = additions.size + updates.size
}

object VaultBackupMerger {

    fun planCredentials(
        incoming: List<VaultBackupCredential>,
        existing: List<VaultBackupCredential>,
        overwriteExisting: Boolean,
    ): MergePlan<VaultBackupCredential> {
        val existingById = existing.associateBy { it.id }
        val existingByKey = existing.groupBy { key(it.site, it.username) }
        val additions = mutableListOf<VaultBackupCredential>()
        val updates = mutableListOf<VaultBackupCredential>()
        val skipped = mutableListOf<VaultBackupCredential>()
        val seenInBackup = mutableSetOf<String>()

        for (credential in incoming) {
            val identity = key(credential.site, credential.username)
            if (!seenInBackup.add(identity)) {
                skipped += credential
                continue
            }
            val sameId = existingById[credential.id]
            if (sameId != null) {
                if (overwriteExisting && credential.updatedAtEpochMillis >= sameId.updatedAtEpochMillis) {
                    updates += credential
                } else {
                    skipped += credential
                }
                continue
            }
            val sameSiteUser = existingByKey[identity]?.firstOrNull()
            if (sameSiteUser != null) {
                if (overwriteExisting && credential.updatedAtEpochMillis > sameSiteUser.updatedAtEpochMillis) {
                    // شناسهٔ موجود حفظ می‌شود تا AAD رکورد در دیتابیس نشکند.
                    updates += credential.copy(id = sameSiteUser.id)
                } else {
                    skipped += credential
                }
                continue
            }
            additions += credential
        }
        return MergePlan(additions, updates, skipped)
    }

    fun planSessions(
        incoming: List<VaultBackupSession>,
        existingIds: Set<String>,
        overwriteExisting: Boolean,
    ): MergePlan<VaultBackupSession> {
        val additions = mutableListOf<VaultBackupSession>()
        val updates = mutableListOf<VaultBackupSession>()
        val skipped = mutableListOf<VaultBackupSession>()
        for (session in incoming) {
            when {
                session.id !in existingIds -> additions += session
                overwriteExisting -> updates += session
                else -> skipped += session
            }
        }
        return MergePlan(additions, updates, skipped)
    }

    /** شناسهٔ یکتای «سایت + نام کاربری»؛ با trim و lowercase تا تکراریِ ظاهری ساخته نشود. */
    fun key(site: String, username: String): String =
        "${normalizeSite(site)}|${username.trim().lowercase()}"

    fun normalizeSite(site: String): String = site.trim()
        .removeSuffix("/")
        .lowercase()

    fun strategyFor(plan: MergePlan<*>): MergeStrategy = when {
        plan.additions.isNotEmpty() -> MergeStrategy.ADD
        plan.updates.isNotEmpty() -> MergeStrategy.UPDATE
        else -> MergeStrategy.SKIP
    }
}
