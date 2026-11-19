package com.marble098.marblebr.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class BrowserPreferences(
    val globalNeverSignOutEnabled: Boolean = true,
    val defaultCookieLifetimeDays: Int = 365,
    val maxSnapshotsPerSite: Int = 10,
    val biometricLockRequired: Boolean = true,
    val smartKeepAliveEnabled: Boolean = true,
    val credentialFallbackEnabled: Boolean = true,
    val adBlockEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val proxyDnsConfig: ProxyDnsConfig = ProxyDnsConfig(),
    val searchEngineTemplate: String = "https://www.google.com/search?q=%s",
)

@Singleton
class BrowserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val GLOBAL_NEVER_SIGNOUT = booleanPreferencesKey("global_never_signout")
        val DEFAULT_COOKIE_DAYS = intPreferencesKey("default_cookie_days")
        val MAX_SNAPSHOTS_PER_SITE = intPreferencesKey("max_snapshots_per_site")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val SMART_KEEP_ALIVE = booleanPreferencesKey("smart_keep_alive")
        val CREDENTIAL_FALLBACK = booleanPreferencesKey("credential_fallback")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("app_language")
        val DNS_MODE = stringPreferencesKey("dns_mode")
        val CUSTOM_DOH_URI = stringPreferencesKey("custom_doh_uri")
        val PROXY_TYPE = stringPreferencesKey("proxy_type")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
    }

    val preferencesFlow: Flow<BrowserPreferences> = dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.SYSTEM

        val dnsMode = prefs[Keys.DNS_MODE]?.let {
            runCatching { DnsMode.valueOf(it) }.getOrNull()
        } ?: DnsMode.SYSTEM

        val proxyType = prefs[Keys.PROXY_TYPE]?.let {
            runCatching { ProxyType.valueOf(it) }.getOrNull()
        } ?: ProxyType.NONE

        val language = prefs[Keys.LANGUAGE]?.let {
            runCatching { AppLanguage.valueOf(it) }.getOrNull()
        } ?: AppLanguage.SYSTEM

        BrowserPreferences(
            globalNeverSignOutEnabled = prefs[Keys.GLOBAL_NEVER_SIGNOUT] ?: true,
            defaultCookieLifetimeDays = prefs[Keys.DEFAULT_COOKIE_DAYS] ?: 365,
            maxSnapshotsPerSite = prefs[Keys.MAX_SNAPSHOTS_PER_SITE] ?: 10,
            biometricLockRequired = prefs[Keys.BIOMETRIC_LOCK] ?: true,
            smartKeepAliveEnabled = prefs[Keys.SMART_KEEP_ALIVE] ?: true,
            credentialFallbackEnabled = prefs[Keys.CREDENTIAL_FALLBACK] ?: true,
            adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
            themeMode = theme,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            language = language,
            proxyDnsConfig = ProxyDnsConfig(
                dnsMode = dnsMode,
                customDohUri = prefs[Keys.CUSTOM_DOH_URI] ?: "https://cloudflare-dns.com/dns-query",
                proxyType = proxyType,
                proxyHost = prefs[Keys.PROXY_HOST] ?: "",
                proxyPort = prefs[Keys.PROXY_PORT] ?: 1080,
            ),
            searchEngineTemplate = prefs[Keys.SEARCH_ENGINE] ?: "https://www.google.com/search?q=%s",
        )
    }

    suspend fun setGlobalNeverSignOut(enabled: Boolean) {
        dataStore.edit { it[Keys.GLOBAL_NEVER_SIGNOUT] = enabled }
    }

    suspend fun setDefaultCookieLifetimeDays(days: Int) {
        dataStore.edit { it[Keys.DEFAULT_COOKIE_DAYS] = days.coerceIn(7, 730) }
    }

    suspend fun setMaxSnapshotsPerSite(count: Int) {
        dataStore.edit { it[Keys.MAX_SNAPSHOTS_PER_SITE] = count.coerceIn(3, 30) }
    }

    suspend fun setBiometricLockRequired(required: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_LOCK] = required }
    }

    suspend fun setSmartKeepAliveEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SMART_KEEP_ALIVE] = enabled }
    }

    suspend fun setCredentialFallbackEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.CREDENTIAL_FALLBACK] = enabled }
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setProxyDnsConfig(config: ProxyDnsConfig) {
        dataStore.edit {
            it[Keys.DNS_MODE] = config.dnsMode.name
            it[Keys.CUSTOM_DOH_URI] = config.customDohUri
            it[Keys.PROXY_TYPE] = config.proxyType.name
            it[Keys.PROXY_HOST] = config.proxyHost
            it[Keys.PROXY_PORT] = config.proxyPort
        }
    }
}
