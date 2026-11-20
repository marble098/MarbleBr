package com.marble098.marblebr.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.model.BackButtonBehavior
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SearchEngine
import com.marble098.marblebr.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class AddressSelectionMode { SELECT_ALL, SELECT_PART, CURSOR }

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
    val selectedSearchEngineId: String = SearchEngine.DEFAULT.id,
    val homeShortcutPosition: HomeShortcutPosition = HomeShortcutPosition.BOTTOM,
    val chromeBarStyle: ChromeBarStyle = ChromeBarStyle.FLOATING,
    val backButtonBehavior: BackButtonBehavior = BackButtonBehavior.SMART,
    val askBeforeClosingTabs: Boolean = false,
    val addressSelectionMode: AddressSelectionMode = AddressSelectionMode.SELECT_ALL,
    val openGoogleOnNewTab: Boolean = false,
    val rateDialogShown: Boolean = false,
    val rateLaunchCount: Int = 0,
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
        val SELECTED_SEARCH_ENGINE_ID = stringPreferencesKey("selected_search_engine_id")
        val HOME_SHORTCUT_POSITION = stringPreferencesKey("home_shortcut_position")
        val CHROME_BAR_STYLE = stringPreferencesKey("chrome_bar_style")
        val BACK_BUTTON_BEHAVIOR = stringPreferencesKey("back_button_behavior")
        val ASK_BEFORE_CLOSING_TABS = booleanPreferencesKey("ask_before_closing_tabs")
        val SAVED_TABS = stringPreferencesKey("saved_tabs_v1")
        val ADDRESS_SELECTION = stringPreferencesKey("address_selection")
        val GOOGLE_NEW_TAB = booleanPreferencesKey("google_new_tab")
        val RATE_DIALOG_SHOWN = booleanPreferencesKey("rate_dialog_shown")
        val RATE_LAUNCH_COUNT = intPreferencesKey("rate_launch_count")
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
            selectedSearchEngineId = prefs[Keys.SELECTED_SEARCH_ENGINE_ID] ?: SearchEngine.DEFAULT.id,
            homeShortcutPosition = prefs[Keys.HOME_SHORTCUT_POSITION]?.let {
                runCatching { HomeShortcutPosition.valueOf(it) }.getOrNull()
            } ?: HomeShortcutPosition.BOTTOM,
            chromeBarStyle = ChromeBarStyle.entries.firstOrNull { it.name == prefs[Keys.CHROME_BAR_STYLE] }
                ?: ChromeBarStyle.FLOATING,
            backButtonBehavior = BackButtonBehavior.entries.firstOrNull { it.name == prefs[Keys.BACK_BUTTON_BEHAVIOR] }
                ?: BackButtonBehavior.SMART,
            askBeforeClosingTabs = prefs[Keys.ASK_BEFORE_CLOSING_TABS] ?: false,
            addressSelectionMode = AddressSelectionMode.entries.firstOrNull { it.name == prefs[Keys.ADDRESS_SELECTION] }
                ?: AddressSelectionMode.SELECT_ALL,
            openGoogleOnNewTab = prefs[Keys.GOOGLE_NEW_TAB] ?: false,
            rateDialogShown = prefs[Keys.RATE_DIALOG_SHOWN] ?: false,
            rateLaunchCount = prefs[Keys.RATE_LAUNCH_COUNT] ?: 0,
        )
    }

    val savedBrowserTabs: Flow<SavedBrowserTabs?> = dataStore.data.map { prefs ->
        prefs[Keys.SAVED_TABS]?.let { saved ->
            try {
                Json.decodeFromString<SavedBrowserTabs>(saved)
            } catch (_: SerializationException) {
                null
            }
        }
    }

    suspend fun saveBrowserTabs(snapshot: SavedBrowserTabs) {
        dataStore.edit { it[Keys.SAVED_TABS] = Json.encodeToString(snapshot) }
    }

    suspend fun setAddressSelectionMode(mode: AddressSelectionMode) {
        dataStore.edit { it[Keys.ADDRESS_SELECTION] = mode.name }
    }

    suspend fun setOpenGoogleOnNewTab(enabled: Boolean) {
        dataStore.edit { it[Keys.GOOGLE_NEW_TAB] = enabled }
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

    suspend fun setSelectedSearchEngineId(engineId: String) {
        dataStore.edit { it[Keys.SELECTED_SEARCH_ENGINE_ID] = engineId }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        dataStore.edit {
            it[Keys.SELECTED_SEARCH_ENGINE_ID] = engine.id
            it[Keys.SEARCH_ENGINE] = engine.searchUrlTemplate
        }
    }

    suspend fun setHomeShortcutPosition(position: HomeShortcutPosition) {
        dataStore.edit { it[Keys.HOME_SHORTCUT_POSITION] = position.name }
    }

    suspend fun setChromeBarStyle(style: ChromeBarStyle) {
        dataStore.edit { it[Keys.CHROME_BAR_STYLE] = style.name }
    }

    suspend fun setBackButtonBehavior(behavior: BackButtonBehavior) {
        dataStore.edit { it[Keys.BACK_BUTTON_BEHAVIOR] = behavior.name }
    }

    suspend fun setAskBeforeClosingTabs(enabled: Boolean) {
        dataStore.edit { it[Keys.ASK_BEFORE_CLOSING_TABS] = enabled }
    }

    suspend fun setRateDialogShown(shown: Boolean) {
        dataStore.edit { it[Keys.RATE_DIALOG_SHOWN] = shown }
    }

    suspend fun setRateLaunchCount(count: Int) {
        dataStore.edit { it[Keys.RATE_LAUNCH_COUNT] = count.coerceIn(0, 100) }
    }
}
