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
import com.marble098.marblebr.core.model.CredentialSaveMode
import com.marble098.marblebr.core.model.ColorPalette
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SearchEngine
import com.marble098.marblebr.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val colorPalette: ColorPalette = ColorPalette.DYNAMIC,
    val dynamicColorEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val proxyDnsConfig: ProxyDnsConfig = ProxyDnsConfig(),
    val searchEngineTemplate: String = "https://www.google.com/search?q=%s",
    val selectedSearchEngineId: String = SearchEngine.DEFAULT.id,
    val homeShortcutPosition: HomeShortcutPosition = HomeShortcutPosition.BUILT_IN,
    val chromeBarStyle: ChromeBarStyle = ChromeBarStyle.FLOATING,
    val backButtonBehavior: BackButtonBehavior = BackButtonBehavior.SMART,
    val askBeforeClosingTabs: Boolean = false,
    val addressSelectionMode: AddressSelectionMode = AddressSelectionMode.SELECT_ALL,
    val openGoogleOnNewTab: Boolean = false,
    val rateDialogShown: Boolean = false,
    val rateLaunchCount: Int = 0,
    /** ذخیرهٔ هوشمندِ ورودها پس از ورودِ موفق. */
    val autoSaveCredentials: Boolean = true,
    /** پیشنهادِ پرکردنِ خودکار روی صفحهٔ ورود. */
    val autoFillCredentials: Boolean = true,
    /** «نبضِ نشست»: یادآوریِ بازدیدِ سبک از سایت‌هایی که خواب‌آلود می‌شوند. */
    val sessionPulseEnabled: Boolean = true,
    /** هشدارِ بدلِ ورود پیش از تایپِ گذرواژه در دامنه‌های مشابه. */
    val loginGuardEnabled: Boolean = true,
    /** «رمزِ یکبارمصرف»: خواندنِ کد از پیامک/اعلان و نوشتنِ آن در کادرِ صفحه. */
    val otpAssistEnabled: Boolean = true,
    /** فاصلهٔ احترام به «الآن نه» برای همان سایت؛ پیش‌فرض ۱۲ ساعت. */
    val savePromptCooldownHours: Int = 12,
    /** حالتِ ذخیرهٔ ورود؛ نامِ enum نگه داشته می‌شود تا DataStore فقط رشته بداند. */
    val credentialSaveModeName: String = CredentialSaveMode.ASK.name,
) {
    /**
     * حالتِ ذخیرهٔ ورود؛ از پرچمِ قدیمیِ `autoSaveCredentials` و کلیدِ تازهٔ `credential_save_mode`
     * ساخته می‌شود تا ارتقای نسخه، تنظیمِ کاربر را از دست ندهد.
     */
    val credentialSaveMode: CredentialSaveMode
        get() = when (credentialSaveModeName) {
            CredentialSaveMode.AUTO_SAVE.name -> CredentialSaveMode.AUTO_SAVE
            CredentialSaveMode.DISABLED.name -> CredentialSaveMode.DISABLED
            // ارتقا از نسخهٔ قدیمی: کلیدِ تازه نیست، پس پرچمِ بولین تصمیم می‌گیرد.
            else -> if (autoSaveCredentials) CredentialSaveMode.ASK else CredentialSaveMode.DISABLED
        }
}

@Singleton
class BrowserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        /** «الآن نه» بیش از ۳۰ روز نگه داشته نمی‌شود؛ بعد از آن اگر لازم بود دوباره می‌پرسیم. */
        const val DISMISSAL_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }

    private object Keys {
        val GLOBAL_NEVER_SIGNOUT = booleanPreferencesKey("global_never_signout")
        val DEFAULT_COOKIE_DAYS = intPreferencesKey("default_cookie_days")
        val MAX_SNAPSHOTS_PER_SITE = intPreferencesKey("max_snapshots_per_site")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val SMART_KEEP_ALIVE = booleanPreferencesKey("smart_keep_alive")
        val CREDENTIAL_FALLBACK = booleanPreferencesKey("credential_fallback")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
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
        val AUTO_SAVE_CREDENTIALS = booleanPreferencesKey("auto_save_credentials")
        val AUTO_FILL_CREDENTIALS = booleanPreferencesKey("auto_fill_credentials")
        val SESSION_PULSE = booleanPreferencesKey("session_pulse_enabled")
        val LOGIN_GUARD = booleanPreferencesKey("login_guard_enabled")
        val CREDENTIAL_SAVE_MODE = stringPreferencesKey("credential_save_mode")
        val OTP_ASSIST = booleanPreferencesKey("otp_assist_enabled")
        val SAVE_PROMPT_COOLDOWN = intPreferencesKey("save_prompt_cooldown_hours")
        val LOGIN_DISMISSALS = stringPreferencesKey("login_dismissals_v1")
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
            colorPalette = prefs[Keys.COLOR_PALETTE]?.let {
                runCatching { ColorPalette.valueOf(it) }.getOrNull()
            } ?: ColorPalette.DYNAMIC,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
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
            } ?: HomeShortcutPosition.BUILT_IN,
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
            autoSaveCredentials = prefs[Keys.AUTO_SAVE_CREDENTIALS] ?: true,
            autoFillCredentials = prefs[Keys.AUTO_FILL_CREDENTIALS] ?: true,
            sessionPulseEnabled = prefs[Keys.SESSION_PULSE] ?: true,
            loginGuardEnabled = prefs[Keys.LOGIN_GUARD] ?: true,
            otpAssistEnabled = prefs[Keys.OTP_ASSIST] ?: true,
            savePromptCooldownHours = prefs[Keys.SAVE_PROMPT_COOLDOWN] ?: 12,
            credentialSaveModeName = prefs[Keys.CREDENTIAL_SAVE_MODE]
                ?: if (prefs[Keys.AUTO_SAVE_CREDENTIALS] ?: true) CredentialSaveMode.ASK.name else CredentialSaveMode.DISABLED.name,
        )
    }

    /**
     * «الآن نه»های کاربر برای هر دامنه — **ماندگار**.
     *
     * باگ اصلیِ نسخهٔ قبلی همین بود: این وضعیت در حافظهٔ `PasswordStudio` نگه داشته می‌شد، پس با
     * هر بار بستنِ اپ (یا حتی تغییرِ صفحه) پاک می‌شد و کاربر دوباره و دوباره کارتِ ذخیره را
     * می‌دید. حالا در DataStore و به‌صورتِ یک نگاشتِ کوچکِ «دامنه → زمان» ذخیره می‌شود.
     */
    val loginDismissals: Flow<Map<String, Long>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.LOGIN_DISMISSALS].orEmpty()
        if (raw.isBlank()) return@map emptyMap<String, Long>()
        runCatching {
            Json.decodeFromString<Map<String, Long>>(raw)
        }.getOrDefault(emptyMap())
    }

    /** ثبتِ «الآن نه» برای یک دامنه؛ رکوردهای قدیمی‌تر از ۳۰ روز خودبه‌خود حذف می‌شوند. */
    suspend fun rememberLoginDismissal(domain: String, atEpochMillis: Long) {
        if (domain.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.LOGIN_DISMISSALS]?.let { raw ->
                runCatching { Json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()
            }.orEmpty()
            val fresh = current
                .filterValues { atEpochMillis - it < DISMISSAL_RETENTION_MILLIS }
                .toMutableMap()
            fresh[domain] = atEpochMillis
            prefs[Keys.LOGIN_DISMISSALS] = Json.encodeToString(fresh.toMap())
        }
    }

    /** پاک‌کردنِ «الآن نه» برای یک دامنه (مثلاً وقتی کاربر دوباره ذخیره را پذیرفت). */
    suspend fun clearLoginDismissal(domain: String) {
        if (domain.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.LOGIN_DISMISSALS]?.let { raw ->
                runCatching { Json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()
            }.orEmpty()
            if (domain !in current) return@edit
            prefs[Keys.LOGIN_DISMISSALS] = Json.encodeToString((current - domain).toMap())
        }
    }

    suspend fun setCredentialSaveMode(mode: CredentialSaveMode) {
        dataStore.edit {
            it[Keys.CREDENTIAL_SAVE_MODE] = mode.name
            // پرچمِ قدیمی برای سازگاری: مرورگری که فقط بولین را می‌شناخت، «هرگز نپرس» را می‌فهمد.
            it[Keys.AUTO_SAVE_CREDENTIALS] = mode != CredentialSaveMode.DISABLED
        }
    }

    suspend fun setOtpAssistEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OTP_ASSIST] = enabled }
    }

    /**
     * خواندنِ مستقیمِ حالتِ ذخیرهٔ ورود از DataStore (برای موتور که به `BrowserPreferences` دست
     * ندارد). معناها:
     * - `ASK` همیشه می‌پرسد (پیش‌فرضِ محافظه‌کارانه).
     * - `AUTO_SAVE` ورودهای بی‌ابهام را بی‌صدا ذخیره می‌کند و فقط موارد مبهم را می‌پرسد.
     * - `DISABLED` هرگز ذخیره نمی‌کند.
     */
    suspend fun credentialSaveMode(): CredentialSaveMode = preferencesFlow.first().credentialSaveMode

    suspend fun setSavePromptCooldownHours(hours: Int) {
        dataStore.edit { it[Keys.SAVE_PROMPT_COOLDOWN] = hours.coerceIn(1, 168) }
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

    /** انتخاب پالت رنگی؛ حالت «رنگ‌های گوشی» همان Material You است. */
    suspend fun setColorPalette(palette: ColorPalette) {
        dataStore.edit {
            it[Keys.COLOR_PALETTE] = palette.name
            it[Keys.DYNAMIC_COLOR] = palette == ColorPalette.DYNAMIC
        }
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

    suspend fun setAutoSaveCredentials(enabled: Boolean) {
        dataStore.edit {
            it[Keys.AUTO_SAVE_CREDENTIALS] = enabled
            it[Keys.CREDENTIAL_SAVE_MODE] = if (enabled) CredentialSaveMode.AUTO_SAVE.name else CredentialSaveMode.ASK.name
        }
    }

    suspend fun setAutoFillCredentials(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_FILL_CREDENTIALS] = enabled }
    }

    suspend fun setSessionPulseEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SESSION_PULSE] = enabled }
    }

    suspend fun setLoginGuardEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LOGIN_GUARD] = enabled }
    }
}
