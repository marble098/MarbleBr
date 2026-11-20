package com.marble098.marblebr.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marble098.marblebr.core.model.PermissionState
import com.marble098.marblebr.core.model.SitePermission
import com.marble098.marblebr.core.model.SitePermissionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ذخیره‌گاهِ تصمیم‌های «دسترسی‌های سایت».
 *
 * هر پروفایل به‌صورت JSON در DataStore نگه داشته می‌شود (کلید: `site_permissions`)، بنابراین:
 * - تصمیم‌های کاربر بین اجراها می‌مانند،
 * - هیچ دادهٔ حساسی ذخیره نمی‌شود (فقط دامنه و یک نقشهٔ سه‌حالته)،
 * - و هیچ چیزی به بیرون فرستاده نمی‌شود (هم‌راستا با قوانین حریم خصوصی AGENTS.md).
 */
@Singleton
class SitePermissionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val PROFILES = stringPreferencesKey("site_permissions_v1")
    }

    /** همهٔ پروفایل‌ها، کلید: دامنهٔ بدون `www`. */
    val profiles: Flow<Map<String, SitePermissionProfile>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.PROFILES] ?: return@map emptyMap()
        runCatching { Json.decodeFromString<Map<String, SitePermissionProfile>>(raw) }
            .getOrElse { error ->
                if (error is SerializationException) emptyMap() else emptyMap()
            }
    }

    /**
     * پروفایلِ یک دامنه همراه با تصمیم‌های ذخیره‌شده.
     *
     * اگر کاربر هنوز چیزی انتخاب نکرده باشد، پروفایلِ خالی برمی‌گردد؛ مقادیر مؤثر از
     * [com.marble098.marblebr.core.model.SitePermissionPolicy.defaults] خوانده می‌شوند.
     */
    suspend fun profileFor(domain: String): SitePermissionProfile {
        val key = normalizeDomain(domain)
        if (key.isEmpty()) return SitePermissionProfile(domain = "")
        val all = profiles.first()
        return all[key] ?: SitePermissionProfile(domain = key)
    }

    /** ثبتِ یک تصمیم و ذخیرهٔ فوری. */
    suspend fun setPermission(domain: String, permission: SitePermission, state: PermissionState) {
        val key = normalizeDomain(domain)
        if (key.isEmpty()) return
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.PROFILES])
            val profile = current[key] ?: SitePermissionProfile(domain = key)
            val updated = current + (key to profile.with(permission, state))
            prefs[Keys.PROFILES] = Json.encodeToString(updated)
        }
    }

    /** برداشتنِ همهٔ تصمیم‌های یک سایت (برگشت به پیش‌فرض). */
    suspend fun reset(domain: String) {
        val key = normalizeDomain(domain)
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.PROFILES])
            prefs[Keys.PROFILES] = Json.encodeToString(current - key)
        }
    }

    private fun decode(raw: String?): Map<String, SitePermissionProfile> =
        raw?.let { runCatching { Json.decodeFromString<Map<String, SitePermissionProfile>>(it) }.getOrNull() }
            ?: emptyMap()

    companion object {
        /** دامنهٔ یکسان‌سازی‌شده، بدون پروتکل/مسیر و بدون `www.` */
        fun normalizeDomain(urlOrDomain: String): String {
            val trimmed = urlOrDomain.trim()
            if (trimmed.isEmpty()) return ""
            val host = runCatching {
                val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
                java.net.URI(normalized).host.orEmpty()
            }.getOrDefault("")
            val clean = host.ifBlank { trimmed.substringAfter("://").substringBefore('/') }
            return clean.lowercase().removePrefix("www.")
        }
    }
}
