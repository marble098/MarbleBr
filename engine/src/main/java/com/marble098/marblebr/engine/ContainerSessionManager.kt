package com.marble098.marblebr.engine

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.ContainerEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.LoginCompatibilityPlan
import com.marble098.marblebr.core.model.LoginCompatibilityReason
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.PermissionState
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.ReaderTextEngine
import com.marble098.marblebr.core.model.SitePermission
import com.marble098.marblebr.core.model.SitePermissionProfile
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import com.marble098.marblebr.core.model.SiteLoginCompatibility
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.vault.SessionVaultManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

data class PageContextTarget(val linkUrl: String?, val imageUrl: String?, val text: String? = null)

/** درخواستِ باز کردنِ پنجرهٔ تازه (popup) از سوی صفحه. */
data class PopupPermissionRequest(
    val tabId: String,
    val targetUri: String,
    val shouldPrompt: Boolean = true,
    val respond: (GeckoSession?) -> Unit,
)

/** درخواستِ یک دسترسیِ محتوایی (موقعیت، اعلان، دوربین/میکروفون، …). */
data class ContentPermissionRequest(
    val tabId: String,
    val url: String,
    val permission: SitePermission,
    val respond: (Boolean) -> Unit,
)

/** درخواستِ دسترسیِ سطحِ اندروید (دوربین، میکروفون، موقعیت). */
data class AndroidPermissionRequest(
    val tabId: String,
    val permissions: List<String>,
    val respond: (Boolean) -> Unit,
)

/** Restore stage only; never include cookie contents in diagnostic state. */
enum class CookieRestoreStatus {
    UNKNOWN,
    ORIGIN_MISMATCH,
    NO_COOKIES,
    INVALID_COOKIES,
    NO_ELIGIBLE_COOKIES,
    FRAME_FAILED,
    REQUEST_NO_TAB,
    REQUEST_OTHER_TAB,
    REQUEST_NOT_SEEN,
    RESPONSE_NOT_MATCHED,
    HEADERS_PROPOSED,
    BRIDGE_FAILED,
}

/**
 * @param cookiesRestored تعداد هدرهای پیشنهادی به Gecko، نه تأیید پذیرش نهایی کوکی‌ها.
 * @param storageKeysRestored تعداد کلیدهای localStorage/sessionStorage که نوشته شد.
 * @param openTabMissing تبی با همان حساب و origin باز نبود؛ هیچ داده‌ای در حساب دیگری نوشته نشد.
 * @param payloadUnavailable نسخه رمزگشایی نشد (خراب یا حذف‌شده).
 * @param cookieStatus مرحلهٔ تلاش برای بازگردانی، بدون ذخیرهٔ محتوای کوکی.
 */
data class VaultRestoreOutcome(
    val cookiesRestored: Int = 0,
    val storageKeysRestored: Int = 0,
    val openTabMissing: Boolean = false,
    val payloadUnavailable: Boolean = false,
    val superseded: Boolean = false,
    val cookieStatus: CookieRestoreStatus = CookieRestoreStatus.UNKNOWN,
)

/**
 * مدیریت مرکزی GeckoRuntime و کانتینرهای ایزوله GeckoSession.
 *
 * هر وب‌اپ یا PWA یک کانتینر مستقل با `contextId` اختصاصی دریافت می‌کند:
 * - خرابی یا پاک‌سازی یک کانتینر هیچ اثری روی بقیه ندارد.
 * - دکمه «پاک کردن کش» صرفاً `ClearFlags.ALL_CACHES` (`NETWORK_CACHE | IMAGE_CACHE`) را پاک می‌کند و هرگز نشست‌ها را پاک نمی‌کند.
 * - دکمه «Sign out now» پاک‌سازی همان `contextId` را درخواست و اسنپ‌شات‌های آن حساب را حذف می‌کند.
 */
@Singleton
class ContainerSessionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val containerDao: ContainerDao,
    private val vaultManager: SessionVaultManager,
    private val healthGuard: SessionHealthGuard,
    private val adBlocker: LightweightAdBlocker,
    private val localizationManager: LocalizationManager,
    private val preferencesRepository: com.marble098.marblebr.core.preferences.BrowserPreferencesRepository,
    /**
     * رصدگرِ ورود؛ رویدادهای «صفحه کامل شد»، «فرم ارسال شد» و «صفحهٔ کد باز شد» به او می‌رسند.
     *
     * چرا `Provider`؟ چون `LoginWatcher` خودش به همین کلاس وابسته است؛ تزریقِ مستقیم یک چرخهٔ
     * ساخت می‌سازد. Provider چرخه را می‌شکند (هنگامِ ساختِ این کلاس فراخوانی نمی‌شود) و مقدارِ
     * پیش‌فرض هم فقط برای تست‌های ابزاری است که این کلاس را دستی می‌سازند.
     */
    private val loginWatcher: Provider<LoginWatcher> = Provider { throw UnsupportedOperationException("LoginWatcher not wired") },
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeSessions = ConcurrentHashMap<String, GeckoSession>()
    private val sessionContainers = ConcurrentHashMap<GeckoSession, String>()

    /** آخرین آدرسِ دیده‌شدهٔ هر نشست؛ برای کاوشِ صفحهٔ ورود در پایانِ بارگذاری. */
    private val lastKnownUrls = ConcurrentHashMap<GeckoSession, String>()
    private val containerMutex = Mutex()

    /** پروفایلِ دسترسی‌های هر تب؛ delegate هر نشست همین را می‌خواند. */
    private val permissionProfiles = ConcurrentHashMap<String, SitePermissionProfile>()

    /** تب‌هایی که خصوصی‌اند؛ هیچ ورودی از آن‌ها ذخیره یا پر نمی‌شود. */
    private val privateSessions = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    /**
     * تب‌هایی که «حالت سازگاری ورود» برایشان روشن است.
     *
     * چرا؟ چون در سایت‌های بانکی، بلاک‌شدنِ یک اسکریپتِ دامنهٔ سوم (ضدتقلب/کپچا) یا یک پنجرهٔ
     * تازه (مرحلهٔ رمزِ دوم) باعث می‌شود کاربر هرگز نتواند وارد شود. در این حالت فقط برای همان
     * تب، محافظتِ ردیابی و بلاکرِ سبک کنار می‌روند و پنجرهٔ تازه آزاد می‌شود؛ کوکی‌ها و
     * گاوصندوقِ نشست دست‌نخورده می‌مانند.
     */
    private val loginCompatibilityTabs = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    /**
     * تب‌هایی که کاربر خودش حالتِ سازگاری را برایشان **خاموش** کرده است.
     *
     * این مجموعه بر تشخیصِ خودکارِ دامنهٔ بانکی اولویت دارد؛ وگرنه خاموش‌کردن روی یک دامنهٔ
     * حساس بی‌اثر می‌شد و دکمهٔ منو دقیقاً برعکسِ وضعیتِ واقعی نمایش می‌داد.
     */
    private val loginCompatibilityOptOutTabs = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    /** مقدارِ بلاکرِ تبلیغ برای هر تب؛ برای تغییرِ زندهٔ محافظتِ ردیابی لازم است. */
    private val adBlockByTab = ConcurrentHashMap<String, Boolean>()

    /**
     * حافظهٔ کوچکِ تشخیصِ دامنه.
     *
     * این تابع در مسیرِ هر درخواستِ شبکه هم خوانده می‌شود؛ تجزیهٔ مکررِ آدرس با `URI` در هر
     * درخواست، هزینهٔ بی‌مورد است. اندازهٔ حافظه سقف دارد و در صورتِ پر شدن، ساده خالی می‌شود.
     */
    private val compatibilityCache = ConcurrentHashMap<String, Boolean>()

    /** آیا این آدرس باید در «حالت سازگاری ورود» باز شود؟ (خودکار یا با درخواستِ کاربر) */
    fun loginCompatibilityPlanFor(tabId: String, url: String): LoginCompatibilityPlan {
        if (loginCompatibilityTabs.contains(tabId)) {
            return LoginCompatibilityPlan(
                relaxedContentBlocking = true,
                allowPopups = true,
                reason = LoginCompatibilityReason.MANUAL,
            )
        }
        if (loginCompatibilityOptOutTabs.contains(tabId)) return LoginCompatibilityPlan()
        if (url.isBlank()) return LoginCompatibilityPlan()
        if (compatibilityCache.size > COMPATIBILITY_CACHE_LIMIT) compatibilityCache.clear()
        val sensitive = compatibilityCache.getOrPut(url) { SiteLoginCompatibility.isSensitiveUrl(url) }
        return if (sensitive) {
            LoginCompatibilityPlan(
                relaxedContentBlocking = true,
                allowPopups = true,
                reason = LoginCompatibilityReason.KNOWN_FINANCIAL_HOST,
            )
        } else {
            LoginCompatibilityPlan()
        }
    }

    /** آیا تبِ جاری در حالت سازگاری است؟ (برای نمایش در منو) */
    fun isLoginCompatibilityEnabled(tabId: String): Boolean = loginCompatibilityTabs.contains(tabId)

    /**
     * روشن/خاموش کردنِ «حالت سازگاری ورود» برای یک تب.
     *
     * محافظتِ ردیابی همان لحظه روی نشستِ باز اعمال می‌شود تا کاربر بدونِ بستنِ تب نتیجه را ببیند.
     */
    fun setLoginCompatibility(tabId: String, enabled: Boolean) {
        if (enabled) {
            loginCompatibilityTabs.add(tabId)
            loginCompatibilityOptOutTabs.remove(tabId)
        } else {
            loginCompatibilityTabs.remove(tabId)
            loginCompatibilityOptOutTabs.add(tabId)
        }
        val session = activeSessions[tabId] ?: return
        val apply = {
            val url = lastKnownUrls[session].orEmpty()
            val relaxed = loginCompatibilityPlanFor(tabId, url).isRelaxed
            runCatching {
                session.settings.useTrackingProtection = (adBlockByTab[tabId] ?: true) && !relaxed
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply()
        } else {
            Handler(Looper.getMainLooper()).post(apply)
        }
    }

    /** آیا بلاکر/محافظتِ ردیابی برای این تب و این آدرس باید فعال باشد؟ */
    private fun trackingProtectionActive(tabId: String, url: String, adBlockEnabled: Boolean): Boolean =
        adBlockEnabled && !loginCompatibilityPlanFor(tabId, url).isRelaxed

    // پورت‌ها از تردِ Gecko هم لمس می‌شوند؛ نگه‌داشتن آن‌ها در ConcurrentHashMap از
    // ConcurrentModificationException و نشتِ پورتِ بسته‌شده جلوگیری می‌کند.
    private val sessionDataMutex = Mutex()
    private val loginNavigationIds = ConcurrentHashMap<GeckoSession, String>()
    private val loadingSessions = ConcurrentHashMap.newKeySet<GeckoSession>()

    private val capturePorts = ConcurrentHashMap<GeckoSession, WebExtension.Port>()
    private val snapshotRequests = ConcurrentHashMap<GeckoSession, SnapshotRequest>()
    private val restoreRequests = ConcurrentHashMap<GeckoSession, RestoreRequest>()

    /**
     * درخواست‌های «اجرای اسکریپت» که منتظرِ پاسخِ صفحه هستند.
     *
     * GeckoView هیچ متدِ `evaluateJS` روی [GeckoSession] ندارد (برخلافِ WebView اندروید)؛ اجرای
     * جاوااسکریپت فقط از مسیرِ افزونهٔ داخلی ممکن است. پس درخواست را با یک شناسه روی پورتِ
     * همان صفحه می‌فرستیم و پاسخِ `JS_RESULT` را با همان شناسه تطبیق می‌دهیم.
     */
    private val scriptRequests = ConcurrentHashMap<String, CompletableDeferred<String?>>()

    private var installedExtension: WebExtension? = null
    private var extensionReady: GeckoResult<WebExtension>? = null
    private val pendingInitialLoads = ConcurrentHashMap<String, String>()

    @Volatile
    private var geckoRuntime: GeckoRuntime? = null

    /**
     * تنظیماتِ شبکه‌ای که در اولین ساختِ [GeckoRuntime] اعمال می‌شود.
     *
     * **باگِ مهمِ نسخهٔ قبل:** `getOrCreateRuntime()` همیشه با مقدارِ پیش‌فرض (`ProxyDnsConfig()`)
     * صدا زده می‌شد، بنابراین پروکسیِ ذخیره‌شده در تنظیمات هرگز به Gecko نمی‌رسید و کاربر
     * فکر می‌کرد پروکسی متصل است. حالا:
     * - در `init` تنظیمات از DataStore خوانده و در این فیلد نگه داشته می‌شود؛
     * - اگر اولین نشست قبل از پایانِ خواندن ساخته شد، یک بار کوتاه و مشروط (حداکثر ۱.۵ ثانیه)
     *   منتظر می‌مانیم؛
     * - اگر باز هم چیزی نبود، از مقدارِ پیش‌فرض استفاده می‌شود (بدونِ پروکسی).
     */
    @Volatile
    private var runtimeNetworkConfig: ProxyDnsConfig? = null

    init {
        engineScope.launch {
            runCatching {
                runtimeNetworkConfig = preferencesRepository.preferencesFlow.first().proxyDnsConfig
            }
        }
    }

    /** آیا موتور Gecko قبلاً ساخته شده است؟ (برای پیامِ «راه‌اندازی دوباره لازم است») */
    fun isRuntimeCreated(): Boolean = geckoRuntime != null

    @Synchronized
    fun getOrCreateRuntime(proxyDnsConfig: ProxyDnsConfig = ProxyDnsConfig()): GeckoRuntime {
        geckoRuntime?.let { return it }

        val contentBlocking = ContentBlocking.Settings.Builder()
            .antiTracking(
                ContentBlocking.AntiTracking.DEFAULT or ContentBlocking.AntiTracking.STP,
            )
            // «همیشه لاگین بمان»: کوکی‌های شخصِ اول و سومِ مربوط به همان سایت پذیرفته
            // می‌شوند. پیش‌تر ACCEPT_FIRST_PARTY باعث می‌شد برخی سایت‌های بانکی/وب‌اپ‌ها
            // نشستِ ورود را روی دامنهٔ سوم (SSO/CDN) از دست بدهند.
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_ALL)
            .build()

        val effectiveNetworkConfig = resolveRuntimeNetworkConfig(proxyDnsConfig)

        val settingsBuilder = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .loginAutofillEnabled(true)
            .globalPrivacyControlEnabled(true)
            .contentBlocking(contentBlocking)

        effectiveNetworkConfig.resolvedDohUri()?.let { dohUri ->
            settingsBuilder.defaultRecursiveResolverUri(dohUri)
        }

        // فایلِ تنظیماتِ Gecko همیشه نوشته می‌شود: علاوه بر پروکسی، پرچم‌های ضروریِ
        // رفتارِ صفحه (مثل `dom.disable_open_during_load` برای پرسشِ popup) هم در آن است.
        val configFile = writeGeckoProxyConfigFile(effectiveNetworkConfig)
        settingsBuilder.configFilePath(configFile.absolutePath)

        val runtime = GeckoRuntime.create(appContext, settingsBuilder.build())
        installNeverSignOutExtension(runtime)
        geckoRuntime = runtime
        return runtime
    }

    private fun resolveRuntimeNetworkConfig(explicit: ProxyDnsConfig): ProxyDnsConfig {
        if (explicit.hasUsableProxy() || explicit.resolvedDohUri() != null) return explicit
        runtimeNetworkConfig?.let { return it }
        return runBlocking {
            withTimeoutOrNull(NETWORK_CONFIG_WAIT_MILLIS) {
                preferencesRepository.preferencesFlow.first().proxyDnsConfig
            }
        } ?: ProxyDnsConfig()
    }

    /**
     * بازیابی یا ساخت کانتینر ایزوله برای یک دامنه یا PWA.
     */
    suspend fun getOrCreateContainerForUrl(
        url: String,
        isPinnedPwa: Boolean = false,
        customDisplayName: String? = null,
    ): WebAppContainer = containerMutex.withLock {
        val domain = extractDomain(url)
        val defaultId = AccountContainers.defaultId(domain)
        val occupied = containerDao.getById(defaultId)
        val id = when {
            isPinnedPwa -> "${defaultId}_pwa_${UUID.randomUUID()}"

            occupied != null && occupied.domain != domain ->
                "${defaultId}_default_${UUID.nameUUIDFromBytes(domain.toByteArray())}"

            else -> defaultId
        }
        if (!isPinnedPwa) {
            val existing = containerDao.getById(id)?.takeIf { it.domain == domain && !it.isPinnedPwa }
            if (existing != null) return@withLock existing.toDomain()
        }
        val preferences = preferencesRepository.preferencesFlow.first()
        val container = WebAppContainer(
            id = id,
            domain = domain,
            displayName = customDisplayName ?: domain,
            contextId = "marble_ctx_$id",
            neverSignOutEnabled = preferences.globalNeverSignOutEnabled,
            cookieExpirationDays = preferences.defaultCookieLifetimeDays.coerceIn(7, 730),
            keepAliveEnabled = true,
            keepAliveIntervalMinutes = 30,
            isPinnedPwa = isPinnedPwa,
            pwaStartUrl = if (isPinnedPwa) url else null,
        )
        containerDao.upsert(ContainerEntity.fromDomain(container))
        container
    }

    /** A named account has its own durable Gecko context; it never replaces the default account. */
    suspend fun createAdditionalAccount(source: WebAppContainer, name: String): WebAppContainer {
        require(name.isNotBlank())
        val account = AccountContainers.newAccount(source, name, UUID.randomUUID().toString())
        containerDao.upsert(ContainerEntity.fromDomain(account))
        return account
    }

    /**
     * ساخت یا بازیابی GeckoSession ایزوله‌شده با contextId اختصاصی کانتینر.
     */
    fun getOrCreateGeckoSession(
        tabId: String,
        container: WebAppContainer,
        initialUrl: String,
        isPrivate: Boolean = false,
        isDesktopMode: Boolean = false,
        adBlockEnabled: Boolean = true,
        onLocationChanged: (url: String, isSecure: Boolean) -> Unit = { _, _ -> },
        onTitleChanged: (title: String) -> Unit = {},
        onProgressChanged: (progress: Int, isLoading: Boolean) -> Unit = { _, _ -> },
        onNavStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> },
        onReauthenticationRequired: (domain: String) -> Unit = {},
        onLoginPageChanged: (url: String) -> Unit = {},
        onDownloadRequested: (url: String, userAgent: String?) -> Unit = { _, _ -> },
        onContextMenu: (PageContextTarget) -> Unit = {},
        onPopupRequest: (PopupPermissionRequest) -> Unit = { it.respond(null) },
        onContentPermissionRequest: (ContentPermissionRequest) -> Unit = {},
        onAndroidPermissionRequest: (AndroidPermissionRequest) -> Unit = {},
        onWindowClose: () -> Unit = {},
        onProcessEnded: () -> Unit = {},
        openedByGecko: Boolean = false,
    ): GeckoSession {
        activeSessions[tabId]?.let { return it }

        adBlockByTab[tabId] = adBlockEnabled
        // بانک/درگاهِ پرداخت به‌طور خودکار در حالت سازگاری باز می‌شود؛ برای بقیهٔ سایت‌ها
        // فقط اگر کاربر از منو روشنش کرده باشد.
        val compatibility = loginCompatibilityPlanFor(tabId, initialUrl)

        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .contextId(if (isPrivate) null else container.contextId)
            .useTrackingProtection(adBlockEnabled && !compatibility.isRelaxed)
            .allowJavascript(permissionProfileOf(tabId).javascriptAllowed())
            .userAgentMode(
                if (isDesktopMode) {
                    GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                } else {
                    GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                },
            )
            .build()

        val session = GeckoSession(sessionSettings)
        val runtime = getOrCreateRuntime()
        attachSessionDelegates(
            session = session,
            container = container,
            adBlockEnabled = adBlockEnabled,
            isPrivate = isPrivate,
            onLocationChanged = onLocationChanged,
            onTitleChanged = onTitleChanged,
            onProgressChanged = onProgressChanged,
            onNavStateChanged = onNavStateChanged,
            onReauthenticationRequired = onReauthenticationRequired,
            onLoginPageChanged = onLoginPageChanged,
            onDownloadRequested = onDownloadRequested,
            onContextMenu = onContextMenu,
            tabId = tabId,
            onPopupRequest = onPopupRequest,
            onWindowClose = onWindowClose,
            onProcessEnded = onProcessEnded,
        )

        attachPermissionDelegates(
            session = session,
            tabId = tabId,
            onContentPermissionRequest = onContentPermissionRequest,
            onAndroidPermissionRequest = onAndroidPermissionRequest,
        )

        if (isHttpUrl(initialUrl)) lastKnownUrls[session] = initialUrl
        activeSessions[tabId] = session
        if (!isPrivate) sessionContainers[session] = container.id
        if (isPrivate) privateSessions.add(tabId) else privateSessions.remove(tabId)
        if (openedByGecko) {
            if (!isPrivate) {
                val available = installedExtension
                if (available != null) {
                    attachSessionExtensionBridge(session, available, container, onReauthenticationRequired, onLoginPageChanged)
                } else {
                    extensionReady?.accept({ extension ->
                        if (extension != null && activeSessions[tabId] === session) {
                            attachSessionExtensionBridge(session, extension, container, onReauthenticationRequired, onLoginPageChanged)
                        }
                    }, {})
                }
            }
            return session
        }
        session.open(runtime)
        if (isPrivate) {
            session.loadUri(initialUrl)
        } else {
            val available = installedExtension
            if (available != null) {
                attachSessionExtensionBridge(session, available, container, onReauthenticationRequired, onLoginPageChanged)
                session.loadUri(initialUrl)
            } else {
                // Install/attach before the first page loads. Otherwise a fresh install can miss
                // the content script entirely and never receive the tab's cookie policy.
                pendingInitialLoads[tabId] = initialUrl
                extensionReady?.accept({ extension ->
                    if (activeSessions[tabId] === session) {
                        if (extension != null) {
                            attachSessionExtensionBridge(session, extension, container, onReauthenticationRequired, onLoginPageChanged)
                        }
                        pendingInitialLoads.remove(tabId)?.let(session::loadUri)
                    }
                }, {
                    if (activeSessions[tabId] === session) pendingInitialLoads.remove(tabId)?.let(session::loadUri)
                })
                // A stuck extension install must not prevent ordinary browsing indefinitely.
                engineScope.launch {
                    delay(EXTENSION_START_TIMEOUT_MILLIS)
                    if (activeSessions[tabId] === session) pendingInitialLoads.remove(tabId)?.let(session::loadUri)
                }
            }
        }
        return session
    }

    /** The system document picker supplies the destination; PDF bytes never leave the device by us. */
    fun savePageAsPdf(tabId: String, destination: Uri, onComplete: (Boolean) -> Unit) {
        // GeckoSession فقط روی تردِ Main لمس می‌شود؛ اگر از ترد دیگری صدا زده شد، خودِ فراخوانی به Main منتقل می‌شود.
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { savePageAsPdf(tabId, destination, onComplete) }
            return
        }
        val session = activeSessions[tabId]
        if (session == null || !session.isOpen) {
            onComplete(false)
            return
        }
        session.saveAsPdf().accept({ stream ->
            if (stream == null) {
                onComplete(false)
            } else {
                engineScope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        runCatching {
                            stream.use { input ->
                                val output = requireNotNull(appContext.contentResolver.openOutputStream(destination))
                                output.use { input.copyTo(it) }
                            }
                            true
                        }.getOrDefault(false)
                    }
                    onComplete(saved)
                }
            }
        }, { onComplete(false) })
    }

    fun loadInOpenSession(tabId: String, url: String) {
        if (pendingInitialLoads.containsKey(tabId)) {
            pendingInitialLoads[tabId] = url
        } else {
            activeSessions[tabId]?.let { session ->
                loginNavigationIds[session] = UUID.randomUUID().toString()
                session.loadUri(url)
            }
        }
    }

    /** نشستِ بازِ فعلیِ یک تب؛ برای اتصالِ دوبارهٔ نما هنگام بازترکیب (recomposition) استفاده می‌شود. */
    fun activeSession(tabId: String): GeckoSession? = activeSessions[tabId]

    fun stopLoading(tabId: String) {
        pendingInitialLoads.remove(tabId)
        activeSessions[tabId]?.stop()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val session = activeSessions[tabId] ?: return
        session.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        reload(tabId)
    }

    fun goBack(tabId: String) {
        activeSessions[tabId]?.goBack()
    }

    fun goForward(tabId: String) {
        activeSessions[tabId]?.goForward()
    }

    fun reload(tabId: String) {
        activeSessions[tabId]?.let { session ->
            loginNavigationIds[session] = UUID.randomUUID().toString()
            session.reload()
        }
    }

    fun findInPage(
        tabId: String,
        query: String,
        backwards: Boolean = false,
        onResult: (current: Int, total: Int) -> Unit,
    ) {
        val session = activeSessions[tabId] ?: return
        if (query.isBlank()) {
            session.finder.clear()
            onResult(0, 0)
            return
        }
        session.finder.displayFlags = GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
        val flags = if (backwards) GeckoSession.FINDER_FIND_BACKWARDS else GeckoSession.FINDER_FIND_FORWARD
        session.finder.find(query, flags).accept { result ->
            if (result != null) {
                onResult(result.current, result.total)
            }
        }
    }

    fun clearFindInPage(tabId: String) {
        activeSessions[tabId]?.finder?.clear()
    }

    /* ---------------------------------- حالت مطالعه و بلندخوانی ---------------------------------- */

    /**
     * روشن/خاموش کردنِ «حالت مطالعه».
     *
     * اسکریپت در همان صفحه اجرا می‌شود (بدون بارگذاری دوباره) و عناصر مزاحم را پنهان می‌کند.
     * @return `true` اگر اسکریپت با موفقیت اجرا شد.
     */
    fun setReaderMode(tabId: String, enabled: Boolean, onResult: (Boolean) -> Unit = {}) {
        val script = if (enabled) ReaderTextEngine.readerScript else ReaderTextEngine.readerResetScript
        evaluateInPage(tabId, script) { result ->
            // اسکریپتِ حالت مطالعه رشتهٔ «on»/«off» را برمی‌گرداند؛ هر پاسخِ غیرخالی یعنی
            // اسکریپت واقعاً در صفحه اجرا شد.
            onResult(!result.isNullOrBlank())
        }
    }

    /**
     * استخراج متنِ اصلیِ صفحه برای بلندخوانی.
     *
     * @param onText متنی که باید خوانده شود؛ رشتهٔ خالی یعنی صفحه متنی نداشت.
     */
    fun readPageText(tabId: String, onText: (String) -> Unit) {
        evaluateInPage(tabId, ReaderTextEngine.extractReadableTextScript) { raw ->
            val text = ReaderTextEngine.extractReadableText(raw)
            onText(if (ReaderTextEngine.hasEnoughText(text)) text else "")
        }
    }

    /**
     * اجرای یک اسکریپت در صفحهٔ همان تب و برگرداندنِ نتیجه به‌صورت رشته.
     *
     * چرا از پورتِ افزونه؟ چون GeckoView متدِ `evaluateJS` ندارد؛ تنها راهِ پشتیبانی‌شده،
     * فرستادنِ پیام به content script همان صفحه است (همان پورتی که اسنپ‌شات‌ها را می‌گیرد).
     * اگر پیوندی برقرار نباشد (مثلاً صفحه‌ای که اسکریپتِ ما در آن اجرا نشده) نتیجه `null`
     * است و لایهٔ بالاتر پیامِ «در دسترس نیست» را نشان می‌دهد.
     */
    internal fun currentLoginPage(tabId: String, url: String): LoginPage? {
        val session = activeSessions[tabId] ?: return null
        if (session in loadingSessions || lastKnownUrls[session] != url || !url.startsWith("https://", true)) return null
        val port = capturePorts[session] ?: return null
        if (!sameOrigin(port.sender.url, url)) return null
        val containerId = sessionContainers[session] ?: return null
        val navigationId = loginNavigationIds[session] ?: return null
        return LoginPage(tabId, containerId, url, navigationId)
    }

    internal suspend fun evalInPageForLogin(page: LoginPage, script: String): String? =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            if (currentLoginPage(page.tabId, page.url) != page) {
                continuation.resumeWith(Result.success(null))
            } else {
                evaluateInPage(page.tabId, script, SCRIPT_TIMEOUT_MILLIS, expectedPage = page) { result ->
                    if (continuation.isActive) continuation.resumeWith(Result.success(result))
                }
            }
        }

    /** آدرسِ آخری که برای این تب دیده شده (بدونِ محتوای صفحه). */
    fun lastKnownUrl(tabId: String): String =
        activeSessions[tabId]?.let { lastKnownUrls[it] }.orEmpty()

    /** آیا این تب خصوصی است؟ (فقط برای تصمیمِ «هیچ‌چیز ذخیره نشود») */
    fun isPrivateSession(tabId: String): Boolean = privateSessions.contains(tabId)

    private fun evaluateInPage(
        tabId: String,
        script: String,
        timeoutMillis: Long = SCRIPT_TIMEOUT_MILLIS,
        expectedPage: LoginPage? = null,
        onResult: (String?) -> Unit,
    ) {
        val port = activeSessions[tabId]?.let { capturePorts[it] }
        if (port == null) {
            onResult(null)
            return
        }
        val requestId = UUID.randomUUID().toString()
        val response = CompletableDeferred<String?>()
        scriptRequests[requestId] = response
        engineScope.launch {
            try {
                if (expectedPage != null && currentLoginPage(tabId, expectedPage.url) != expectedPage) {
                    onResult(null)
                    return@launch
                }
                port.postMessage(
                    JSONObject()
                        .put("type", "EVAL_JS")
                        .put("expectedUrl", expectedPage?.url)
                        .put("requireVisible", expectedPage != null)
                        .put("requestId", requestId)
                        .put("script", script),
                )
                onResult(withTimeoutOrNull(timeoutMillis) { response.await() })
            } catch (_: Exception) {
                onResult(null)
            } finally {
                scriptRequests.remove(requestId)
            }
        }
    }

    private fun attachPermissionDelegates(
        session: GeckoSession,
        tabId: String,
        onContentPermissionRequest: (ContentPermissionRequest) -> Unit,
        onAndroidPermissionRequest: (AndroidPermissionRequest) -> Unit,
    ) {
        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onPopupPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.PopupPrompt,
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                // Gecko calls this for its built-in popup blocker only. onNewSession
                // is the final gate for both blocked and user-initiated windows.
                val decision = if (popupPolicy(tabId, session) == PermissionState.BLOCK) {
                    AllowOrDeny.DENY
                } else {
                    AllowOrDeny.ALLOW
                }
                return GeckoResult.fromValue(prompt.confirm(decision))
            }
        }

        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(
                session: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission,
            ): GeckoResult<Int> {
                // Persistent storage is a core part of "never sign out"; it is not a
                // user-facing permission and is always granted for a protected tab.
                if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE) {
                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                }
                val sitePermission = permissionFor(perm.permission) ?: return GeckoResult.fromValue(
                    GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY,
                )
                val state = permissionProfiles[tabId]?.stateOf(sitePermission)
                    ?: com.marble098.marblebr.core.model.SitePermissionPolicy.defaults[sitePermission]
                    ?: PermissionState.ASK
                return when (state) {
                    PermissionState.ALLOW -> GeckoResult.fromValue(
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW,
                    )
                    PermissionState.BLOCK -> GeckoResult.fromValue(
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY,
                    )
                    PermissionState.ASK -> {
                        val result = GeckoResult<Int>()
                        onContentPermissionRequest(
                            ContentPermissionRequest(
                                tabId = tabId,
                                url = perm.uri.orEmpty(),
                                permission = sitePermission,
                                respond = { allow ->
                                    result.complete(
                                        if (allow) {
                                            GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                                        } else {
                                            GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                                        },
                                    )
                                },
                            ),
                        )
                        result
                    }
                }
            }

            override fun onMediaPermissionRequest(
                session: GeckoSession,
                uri: String,
                video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback,
            ) {
                val profile = permissionProfiles[tabId]
                val videoState = profile?.stateOf(SitePermission.CAMERA)
                    ?: com.marble098.marblebr.core.model.SitePermissionPolicy.defaults[SitePermission.CAMERA]
                    ?: PermissionState.ASK
                val audioState = profile?.stateOf(SitePermission.MICROPHONE)
                    ?: com.marble098.marblebr.core.model.SitePermissionPolicy.defaults[SitePermission.MICROPHONE]
                    ?: PermissionState.ASK
                val videoAllowed = videoState == PermissionState.ALLOW
                val audioAllowed = audioState == PermissionState.ALLOW
                val videoSource = video?.firstOrNull()?.takeIf { videoAllowed }
                val audioSource = audio?.firstOrNull()?.takeIf { audioAllowed }
                if ((video != null && !videoAllowed) || (audio != null && !audioAllowed)) {
                    // For a mixed request, ask once for the strongest missing permission.
                    val requested = when {
                        video != null && !videoAllowed -> SitePermission.CAMERA
                        else -> SitePermission.MICROPHONE
                    }
                    onContentPermissionRequest(
                        ContentPermissionRequest(
                            tabId = tabId,
                            url = uri,
                            permission = requested,
                            respond = { allow ->
                                if (!allow) {
                                    callback.reject()
                                } else {
                                    val grantedVideo = video?.firstOrNull()?.takeIf { videoAllowed || requested == SitePermission.CAMERA }
                                    val grantedAudio = audio?.firstOrNull()?.takeIf { audioAllowed || requested == SitePermission.MICROPHONE }
                                    if (grantedVideo == null && grantedAudio == null) callback.reject() else callback.grant(grantedVideo, grantedAudio)
                                }
                            },
                        ),
                    )
                    return
                }
                if (videoSource == null && audioSource == null) {
                    callback.reject()
                } else {
                    callback.grant(videoSource, audioSource)
                }
            }

            override fun onAndroidPermissionsRequest(
                session: GeckoSession,
                permissions: Array<out String>?,
                callback: GeckoSession.PermissionDelegate.Callback,
            ) {
                val requested = permissions.orEmpty().toList()
                val allGranted = requested.all { permission ->
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        appContext,
                        permission,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                if (allGranted) {
                    callback.grant()
                    return
                }
                onAndroidPermissionRequest(
                    AndroidPermissionRequest(
                        tabId = tabId,
                        permissions = requested,
                        respond = { callback.grant() },
                    ),
                )
            }
        }
    }

    private fun permissionFor(permission: Int): SitePermission? = when (permission) {
        GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> SitePermission.GEOLOCATION
        GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> SitePermission.NOTIFICATIONS
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE,
        -> SitePermission.AUTOPLAY

        else -> null
    }

    /**
     * اعمالِ «دسترسی‌های سایت» روی نشست.
     *
     * - **جاوااسکریپت** با `session.settings.allowJavascript` روشن/خاموش می‌شود (همان راهی که
     *   مرورگرهای بزرگ استفاده می‌کنند و بلافاصله اثر می‌گذارد).
     * - **بقیهٔ دسترسی‌ها** (موقعیت، اعلان، دوربین/میکروفون، پخش خودکار) از طریق
     *   [GeckoSession.PermissionDelegate] پاسخ داده می‌شوند؛ پروفایل فعالِ هر تب در
     *   `permissionProfiles` نگه داشته می‌شود و delegate همان را می‌خواند.
     */
    fun applySitePermissions(tabId: String, profile: SitePermissionProfile) {
        permissionProfiles[tabId] = profile
        val session = activeSessions[tabId] ?: return
        session.settings.allowJavascript = profile.javascriptAllowed()
    }

    /** پروفایلِ فعالِ دسترسی‌های یک تب (برای نمایش در پنل تنظیمات سایت). */
    fun permissionProfileOf(tabId: String): SitePermissionProfile =
        permissionProfiles[tabId] ?: SitePermissionProfile(domain = "")

    /**
     * ویژگی ۷ Never Sign-out Engine:
     * دکمه «پاک کردن کش» فقط و فقط کش شبکه و تصاویر (`ALL_CACHES`) را پاک می‌کند
     * و هرگز به کوکی‌ها (`COOKIES`)، حافظه محلی (`DOM_STORAGES`) یا نشست‌ها (`AUTH_SESSIONS`) دست نمی‌زند.
     */
    fun clearCachesOnly(): Long {
        val flags = SAFE_CACHE_ONLY_CLEAR_FLAGS
        geckoRuntime?.storageController?.clearData(flags)
        return flags
    }

    /**
     * خروج دستی واقعی («Sign out now») به درخواست صریح کاربر برای یک کانتینر/سایت مشخص:
     * ۱. پاک‌سازی کامل داده‌های contextId آن کانتینر در GeckoView.
     * ۲. پاک‌سازی کوکی‌ها و DOM Storage آن دامنه.
     * ۳. حذف تمام اسنپ‌شات‌های ذخیره شده آن سایت از Session Vault.
     */
    suspend fun signOutNow(container: WebAppContainer) = withContext(Dispatchers.Main.immediate) {
        sessionDataMutex.withLock {
            // Close live writers before clearing this account's Gecko context and Vault snapshots.
            activeSessions.entries.filter { sessionContainers[it.value] == container.id }
                .map { it.key }.forEach(::closeTabSession)
            getOrCreateRuntime().storageController.clearDataForSessionContext(container.contextId)
            vaultManager.purgeSnapshotsForContainer(container.id)
        }
    }

    fun closeTabSession(tabId: String) {
        pendingInitialLoads.remove(tabId)
        permissionProfiles.remove(tabId)
        privateSessions.remove(tabId)
        loginCompatibilityTabs.remove(tabId)
        loginCompatibilityOptOutTabs.remove(tabId)
        adBlockByTab.remove(tabId)
        runCatching { loginWatcher.get().forget(tabId) }
        val session = activeSessions.remove(tabId) ?: return
        sessionContainers.remove(session)
        loginNavigationIds.remove(session)
        loadingSessions.remove(session)
        lastKnownUrls.remove(session)
        cancelPendingRestore(session)
        capturePorts.remove(session)?.disconnect()
        snapshotRequests.remove(session)?.response?.complete(false)
        // GeckoSession فقط روی تردِ Main معتبر است؛ اگر تماس از ترد دیگری آمد، بستن روی Main انجام می‌شود.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (session.isOpen) session.close()
        } else {
            Handler(Looper.getMainLooper()).post {
                runCatching { if (session.isOpen) session.close() }
            }
        }
    }

    /**
     * گرفتنِ اسنپ‌شات از تبِ بازِ همان حساب به‌درخواستِ داشبورد.
     *
     * اگر نشستِ فعالی برای همان دامنه باز باشد، از مسیر دقیقِ صفحه استفاده می‌کند؛ در غیر این
     * صورت مقدار «موفق» را برنمی‌گرداند چون داده‌ای برای ذخیره وجود ندارد.
     */
    suspend fun captureSiteSnapshotNow(container: WebAppContainer): Boolean {
        val match = withContext(Dispatchers.Main.immediate) {
            capturePorts.entries.firstOrNull { (session, port) ->
                session.isOpen && sessionContainers[session] == container.id && extractDomain(port.sender.url) == container.domain
            }
        } ?: return false
        val tabId = activeSessions.entries.firstOrNull { it.value === match.key }?.key ?: return false
        val url = lastKnownUrls[match.key] ?: return false
        return capturePageSnapshot(tabId, url, manual = true)
    }

    internal suspend fun captureScheduledCheckpoint(container: WebAppContainer) {
        withContext(Dispatchers.Main.immediate) {
            val intervalMillis = container.keepAliveIntervalMinutes.coerceAtLeast(15) * 60_000L
            val lastCapture = vaultManager.latestCaptureTime(container.id)
            if (lastCapture != null && System.currentTimeMillis() - lastCapture < intervalMillis) return@withContext
            val match = activeSessions.entries.firstOrNull { (_, session) ->
                session.isOpen && session !in loadingSessions && sessionContainers[session] == container.id &&
                    capturePorts[session]?.sender?.url?.let { extractDomain(it) == container.domain } == true
            } ?: return@withContext
            val url = lastKnownUrls[match.value] ?: return@withContext
            capturePageSnapshot(match.key, url)
        }
    }

    /** Restore only into an open tab of the exact account and origin, never into a global cookie jar. */
    suspend fun restoreSnapshotOnOpenTab(snapshot: SessionSnapshotMeta): VaultRestoreOutcome =
        withContext(Dispatchers.Main.immediate) {
            sessionDataMutex.withLock {
                val outcome = restoreLatestSnapshotOnOpenTab(snapshot)
                healthGuard.recordRestoreOutcome(snapshot.containerId, snapshot.domain, outcome)
                outcome
            }
        }

    private suspend fun restoreLatestSnapshotOnOpenTab(snapshot: SessionSnapshotMeta): VaultRestoreOutcome {
        val payload = vaultManager.restoreSnapshotById(snapshot.id)
            ?: return VaultRestoreOutcome(payloadUnavailable = true)
        if (vaultManager.hasNewerSnapshot(snapshot.id, payload)) return VaultRestoreOutcome(superseded = true)
        val (session, port) = capturePorts.entries.firstOrNull { (candidate, candidatePort) ->
            candidate.isOpen && candidate !in loadingSessions && sessionContainers[candidate] == payload.containerId &&
                sameOrigin(candidatePort.sender.url, payload.origin)
        } ?: return VaultRestoreOutcome(openTabMissing = true)
        val navigationId = loginNavigationIds[session] ?: return VaultRestoreOutcome(openTabMissing = true)
        val url = lastKnownUrls[session] ?: return VaultRestoreOutcome(openTabMissing = true)
        val applied = applyPayloadToSession(session, payload)
        if (capturePorts[session] === port && loginNavigationIds[session] == navigationId && lastKnownUrls[session] == url &&
            (applied.cookiesRestored > 0 || applied.storageKeysRestored > 0)
        ) {
            session.reload()
        }
        return applied
    }

    private suspend fun applyPayloadToSession(session: GeckoSession?, payload: SessionSnapshotPayload): VaultRestoreOutcome {
        if (session == null || sessionContainers[session] != payload.containerId) return VaultRestoreOutcome(openTabMissing = true)
        val port = capturePorts[session] ?: return VaultRestoreOutcome(openTabMissing = true)
        val expectedUrl = lastKnownUrls[session] ?: return VaultRestoreOutcome(openTabMissing = true)
        val navigationId = loginNavigationIds[session] ?: return VaultRestoreOutcome(openTabMissing = true)
        if (!sameOrigin(port.sender.url, payload.origin) || !sameOrigin(expectedUrl, payload.origin) || restoreRequests.containsKey(session)) {
            return VaultRestoreOutcome(openTabMissing = true)
        }
        val cookies = JSONArray()
        val now = System.currentTimeMillis()
        payload.cookies.filter { it.expiresEpochMillis?.let { expiry -> expiry > now } != false }.forEach { cookie ->
            cookies.put(JSONObject().apply {
                put("name", cookie.name)
                put("value", cookie.value)
                put("domain", cookie.domain)
                put("path", cookie.path)
                put("isHostOnly", cookie.isHostOnly)
                put("isSecure", cookie.isSecure)
                put("isHttpOnly", cookie.isHttpOnly)
                put("sameSite", cookie.sameSite)
                cookie.expiresEpochMillis?.let { put("expiresEpochMillis", it) }
            })
        }
        val body = JSONObject().apply {
            put("origin", payload.origin)
            put("cookies", cookies)
            put("localStorage", mapToJson(payload.localStorage))
            put("sessionStorage", mapToJson(payload.sessionStorage))
        }
        val request = RestoreRequest(UUID.randomUUID().toString(), expectedUrl, navigationId, CompletableDeferred())
        restoreRequests[session] = request
        return try {
            port.postMessage(
                JSONObject().put("type", "APPLY_VAULT_SNAPSHOT")
                    .put("expectedUrl", expectedUrl)
                    .put("requestId", request.id)
                    .put("payload", body),
            )
            withTimeoutOrNull(6_000) { request.response.await() } ?: VaultRestoreOutcome()
        } catch (_: Exception) {
            VaultRestoreOutcome()
        } finally {
            cancelPendingRestore(session, request)
        }
    }

    private fun cancelPendingRestore(session: GeckoSession, expected: RestoreRequest? = null) {
        val request = restoreRequests[session] ?: return
        if (expected != null && request !== expected) return
        if (!restoreRequests.remove(session, request)) return
        request.response.complete(VaultRestoreOutcome())
        runCatching { capturePorts[session]?.postMessage(JSONObject().put("type", "CANCEL_VAULT_RESTORE").put("requestId", request.id)) }
    }

    private fun mapToJson(map: Map<String, String>): JSONObject {
        val json = JSONObject()
        map.forEach { (key, value) -> json.put(key, value) }
        return json
    }

    /** مقدارِ سراسریِ «محافظت از سایت‌های جدید» از تنظیمات؛ داشبورد برای هشدارِ درست لازم دارد. */
    suspend fun neverSignOutGlobalEnabled(): Boolean =
        preferencesRepository.preferencesFlow.first().globalNeverSignOutEnabled

    suspend fun capturePageSnapshot(tabId: String, pageUrl: String, manual: Boolean = false): Boolean =
        withContext(Dispatchers.Main.immediate) { capturePageSnapshotOnMain(tabId, pageUrl, manual) }

    private suspend fun capturePageSnapshotOnMain(tabId: String, pageUrl: String, manual: Boolean): Boolean {
        val maxAttempts = if (manual) 2 else 1
        for (attempt in 1..maxAttempts) {
            val session = activeSessions[tabId] ?: return false
            val port = capturePorts[session] ?: return false
            val navigationId = loginNavigationIds[session] ?: return false
            if (session in loadingSessions || lastKnownUrls[session] != pageUrl || !sameOrigin(port.sender.url, pageUrl)) return false
            val inFlight = snapshotRequests[session]
            if (inFlight != null) {
                if (!manual) return false
                val existing = withTimeoutOrNull(if (inFlight.manual) MANUAL_SNAPSHOT_TIMEOUT_MILLIS else AUTO_SNAPSHOT_TIMEOUT_MILLIS) {
                    inFlight.response.await()
                }
                if (inFlight.manual && existing != null) return existing
                snapshotRequests.remove(session, inFlight)
                continue
            }
            val request = SnapshotRequest(UUID.randomUUID().toString(), manual, pageUrl, navigationId, CompletableDeferred())
            snapshotRequests[session] = request
            try {
                port.postMessage(
                    JSONObject().put("type", "CAPTURE_PAGE").put("requestId", request.id).put("expectedUrl", pageUrl),
                )
                val result = withTimeoutOrNull(if (manual) MANUAL_SNAPSHOT_TIMEOUT_MILLIS else AUTO_SNAPSHOT_TIMEOUT_MILLIS) {
                    request.response.await()
                }
                if (result != null) return result
                if (attempt < maxAttempts) delay(MANUAL_SNAPSHOT_RETRY_DELAY_MILLIS)
            } finally {
                snapshotRequests.remove(session, request)
            }
        }
        return false
    }

    private fun sameOrigin(first: String, second: String): Boolean = runCatching {
        val one = URI(first)
        val two = URI(second)
        one.host != null && one.host.equals(two.host, true) && one.scheme.equals(two.scheme, true) && effectivePort(one) == effectivePort(two)
    }.getOrDefault(false)

    private fun effectivePort(uri: URI): Int = uri.port.takeIf { it >= 0 } ?: if (uri.scheme.equals("https", true)) 443 else 80

    private data class SnapshotRequest(
        val id: String,
        val manual: Boolean,
        val expectedUrl: String,
        val navigationId: String,
        val response: CompletableDeferred<Boolean>,
    )
    private data class RestoreRequest(
        val id: String,
        val expectedUrl: String,
        val navigationId: String,
        val response: CompletableDeferred<VaultRestoreOutcome>,
    )

    /** Apply changed settings to already-open tabs as well as future ones. */
    suspend fun refreshCookiePolicies() {
        for ((session, port) in capturePorts) {
            val container = sessionContainers[session]?.let { containerDao.getById(it)?.toDomain() }
            if (container != null) sendCookiePolicy(port, container)
        }
    }

    private fun sendCookiePolicy(port: WebExtension.Port, container: WebAppContainer) {
        runCatching {
            port.postMessage(
                JSONObject().put("type", "SET_COOKIE_POLICY")
                    .put("enabled", container.neverSignOutEnabled)
                    .put("days", container.cookieExpirationDays)
                    .put("persistStorage", container.storagePersistRequested),
            )
        }
    }

    private fun installNeverSignOutExtension(runtime: GeckoRuntime) {
        val ready = runtime.webExtensionController.ensureBuiltIn(EXTENSION_ASSET_URI, EXTENSION_ID)
        extensionReady = ready
        ready.accept({ extension -> installedExtension = extension }, { _ ->
            // Browsing still works without the extension.
        })
    }

    private fun attachSessionExtensionBridge(
        session: GeckoSession,
        extension: WebExtension,
        container: WebAppContainer,
        onReauthenticationRequired: (String) -> Unit,
        onLoginPageChanged: (String) -> Unit,
    ) {
        session.webExtensionController.setMessageDelegate(
            extension,
            object : WebExtension.MessageDelegate {
                override fun onConnect(port: WebExtension.Port) {
                    val senderUrl = port.sender.url.orEmpty()
                    if (!port.sender.isTopLevel || extractDomain(senderUrl) != container.domain) return
                    capturePorts[session] = port
                    loginNavigationIds[session] = UUID.randomUUID().toString()
                    engineScope.launch {
                        val fresh = containerDao.getById(container.id)?.toDomain() ?: container
                        if (capturePorts[session] === port) sendCookiePolicy(port, fresh)
                    }
                    port.setDelegate(object : WebExtension.PortDelegate {
                        override fun onPortMessage(message: Any, port: WebExtension.Port) {
                            if (capturePorts[session] !== port) return
                            val json = message as? JSONObject ?: return
                            when (json.optString("event")) {
                                JS_RESULT_EVENT -> scriptRequests.remove(json.optString("requestId"))
                                    ?.complete(json.optString("result"))

                                "RESTORE_RESULT" -> {
                                    val request = restoreRequests[session] ?: return
                                    if (request.id == json.optString("requestId") && loginNavigationIds[session] == request.navigationId &&
                                        lastKnownUrls[session] == request.expectedUrl && session !in loadingSessions
                                    ) {
                                        request.response.complete(
                                            VaultRestoreOutcome(
                                                cookiesRestored = json.optInt("cookieCount").coerceAtLeast(0),
                                                storageKeysRestored = json.optInt("storageKeyCount").coerceAtLeast(0),
                                                cookieStatus = runCatching {
                                                    CookieRestoreStatus.valueOf(json.optString("cookieStatus"))
                                                }.getOrDefault(CookieRestoreStatus.UNKNOWN),
                                            ),
                                        )
                                    }
                                }

                                "AUTO_CAPTURE_REQUEST" -> {
                                    val url = json.optString("url")
                                    val tabId = activeSessions.entries.firstOrNull { it.value === session }?.key ?: return
                                    if (lastKnownUrls[session] != url || !sameOrigin(port.sender.url, url)) return
                                    engineScope.launch { capturePageSnapshot(tabId, url) }
                                }

                                "CAPTURE_FAILED" -> {
                                    val request = snapshotRequests[session] ?: return
                                    if (request.id == json.optString("requestId")) request.response.complete(false)
                                }

                                "STORAGE_SNAPSHOT" -> {
                                    if (!sameOrigin(port.sender.url, json.optString("origin"))) return
                                    val request = snapshotRequests[session]?.takeIf { it.id == json.optString("requestId") } ?: return
                                    val capturedAt = System.currentTimeMillis()
                                    engineScope.launch {
                                        val saved = sessionDataMutex.withLock {
                                            fun isCurrentSnapshot() = snapshotRequests[session] === request && !request.response.isCompleted &&
                                                capturePorts[session] === port && sessionContainers[session] == container.id &&
                                                session !in loadingSessions && loginNavigationIds[session] == request.navigationId &&
                                                lastKnownUrls[session] == request.expectedUrl && json.optString("href") == request.expectedUrl

                                            if (!isCurrentSnapshot()) return@withLock false
                                            val fresh = containerDao.getById(container.id)?.toDomain() ?: return@withLock false
                                            if (!isCurrentSnapshot() || (!request.manual && !fresh.neverSignOutEnabled)) return@withLock false
                                            runCatching { savePageSnapshot(json, fresh, request.manual, capturedAt) }.getOrDefault(false)
                                        }
                                        request.response.complete(saved)
                                    }
                                }

                                "HTTP_UNAUTHORIZED" -> {
                                    val url = json.optString("url")
                                    if (url != lastKnownUrls[session] || !sameOrigin(port.sender.url, url)) return
                                    engineScope.launch {
                                        val fresh = containerDao.getById(container.id)?.toDomain() ?: return@launch
                                        if (capturePorts[session] !== port || lastKnownUrls[session] != url) return@launch
                                        reportSessionChallenge(
                                            fresh,
                                            SessionHealthSignal.HttpUnauthorized(fresh.id, fresh.domain, url, json.optInt("statusCode")),
                                            onReauthenticationRequired,
                                        )
                                    }
                                }

                                "LOGIN_FORM_CHANGED" -> {
                                    val url = json.optString("url")
                                    if (url == lastKnownUrls[session] && sameOrigin(port.sender.url, url)) {
                                        onLoginPageChanged(url)
                                    }
                                }

                                // «ذخیرهٔ هوشمندِ ورود»: لحظهٔ ارسالِ فرم. مقادیر فقط در حافظه
                                // می‌مانند و تصمیمِ ذخیره پس از تأییدِ موفقیت گرفته می‌شود.
                                LOGIN_SUBMIT_EVENT -> {
                                    val pageUrl = json.optString("url")
                                    if (!pageUrl.startsWith("https://", true) || !sameOrigin(port.sender.url, pageUrl)) return
                                    val tabId = activeSessions.entries.firstOrNull {
                                        it.value === session
                                    }?.key ?: return
                                    engineScope.launch {
                                        runCatching {
                                            // پیامِ خطای دیده‌شده همان لحظه، این ارسال را رد می‌کند.
                                            // (بدونِ این پرچم، فرمِ ردشده هم کارتِ ذخیره می‌ساخت.)
                                            loginWatcher.get().captureSubmitted(
                                                tabId = tabId,
                                                url = pageUrl,
                                                username = json.optString("username"),
                                                password = json.optString("password"),
                                                otpFieldCount = json.optInt("otpFields"),
                                                errorVisible = json.optBoolean("errorVisible"),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        override fun onDisconnect(port: WebExtension.Port) {
                            if (capturePorts[session] === port) {
                                cancelPendingRestore(session)
                                capturePorts.remove(session)
                                snapshotRequests[session]?.response?.complete(false)
                            }
                        }
                    })
                    if (session !in loadingSessions && senderUrl == lastKnownUrls[session]) onLoginPageChanged(senderUrl)
                }
            },
            NATIVE_BRIDGE_APP_ID,
        )
    }

    private suspend fun reportSessionChallenge(
        container: WebAppContainer,
        signal: SessionHealthSignal,
        onReauthenticationRequired: (String) -> Unit,
    ) {
        val assessment = healthGuard.evaluateSignal(signal, container.neverSignOutEnabled)
        if (assessment.action == HealthGuardAction.ESCALATE_TO_CREDENTIAL_FALLBACK) {
            onReauthenticationRequired(container.domain)
        }
    }

    private suspend fun savePageSnapshot(
        json: JSONObject,
        container: WebAppContainer,
        manual: Boolean,
        capturedAt: Long,
    ): Boolean {
        val origin = json.optString("origin")
        if (extractDomain(origin) != container.domain) return false
        val payload = SessionSnapshotPayload(
            origin = origin,
            containerId = container.id,
            capturedAtEpochMillis = capturedAt,
            cookies = mergeSnapshotCookies(json, origin),
            localStorage = jsonObjectToMap(json.optJSONObject("localStorage")),
            sessionStorage = jsonObjectToMap(json.optJSONObject("sessionStorage")),
            indexedDbDump = jsonObjectToMap(json.optJSONObject("indexedDbDump")),
            storagePersisted = json.optBoolean("storagePersisted"),
        )
        val hasData = vaultManager.hasRestorableData(payload, capturedAt)
        if (!hasData || (!manual && !vaultManager.hasSessionCookieEvidence(payload, capturedAt))) {
            return false
        }
        val strings = localizationManager.current()
        vaultManager.captureSnapshot(
            payload = payload,
            targetCookieExpirationDays = container.cookieExpirationDays,
            triggerReason = if (manual) "MANUAL_USER_CAPTURE" else "PAGE_LOAD_SYNC",
            label = if (manual) strings.manualSnapshotLabel(container.domain) else strings.autoSnapshotLabel(container.domain),
        )
        return true
    }

    private fun popupPolicy(tabId: String, session: GeckoSession): PermissionState {
        val url = lastKnownUrls[session].orEmpty()
        val domain = url.let(::extractDomain)
        val profile = permissionProfiles[tabId]?.takeIf { it.domain == domain }
        // تصمیمِ صریحِ کاربر همیشه اول است: اگر خودش پنجره را «بلاک» کرده، سازگاری هم بازش نمی‌کند.
        val explicit = profile?.states?.get(SitePermission.POPUPS)
        if (explicit == PermissionState.BLOCK) return PermissionState.BLOCK
        // بانک/درگاه (یا درخواستِ کاربر): مرحلهٔ رمزِ دوم و پرداخت در پنجرهٔ تازه باز می‌شود و
        // اگر پرسیده یا بلاک شود، ورود نیمه‌کاره می‌ماند.
        if (loginCompatibilityPlanFor(tabId, url).allowPopups) return PermissionState.ALLOW
        return profile?.stateOf(SitePermission.POPUPS) ?: PermissionState.ASK
    }

    private fun attachSessionDelegates(
        session: GeckoSession,
        container: WebAppContainer,
        adBlockEnabled: Boolean,
        isPrivate: Boolean,
        onLocationChanged: (url: String, isSecure: Boolean) -> Unit,
        onTitleChanged: (title: String) -> Unit,
        onProgressChanged: (progress: Int, isLoading: Boolean) -> Unit,
        onNavStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onReauthenticationRequired: (String) -> Unit,
        onLoginPageChanged: (String) -> Unit,
        onDownloadRequested: (url: String, userAgent: String?) -> Unit,
        onContextMenu: (PageContextTarget) -> Unit,
        tabId: String,
        onPopupRequest: (PopupPermissionRequest) -> Unit,
        onWindowClose: () -> Unit,
        onProcessEnded: () -> Unit,
    ) {
        var previousUrl = ""
        var canBack = false
        var canFwd = false

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                val state = popupPolicy(tabId, session)
                val target = uri.ifBlank { "about:blank" }
                if (state == PermissionState.BLOCK || (target != "about:blank" && !isHttpUrl(target))) return null
                val result = GeckoResult<GeckoSession>()
                onPopupRequest(
                    PopupPermissionRequest(
                        tabId = tabId,
                        targetUri = target,
                        shouldPrompt = state == PermissionState.ASK,
                        respond = { result.complete(it) },
                    ),
                )
                return result
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest,
            ): GeckoResult<AllowOrDeny>? {
                // تصمیم بر اساسِ دامنهٔ «همین صفحه» گرفته می‌شود، نه دامنهٔ درخواستِ سوم؛
                // وگرنه در صفحهٔ بانک، درخواستِ اسکریپتِ دامنهٔ سوم دوباره بلاک می‌شد.
                val blockingEnabled = trackingProtectionActive(tabId, previousUrl.ifBlank { request.uri }, adBlockEnabled)
                if (!request.isDirectNavigation && !request.hasUserGesture && adBlocker.shouldBlockRequest(request.uri, previousUrl, blockingEnabled)) {
                    onProgressChanged(0, false)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // User-initiated sign-in and cross-origin SSO are normal navigation.
                if (!isPrivate && request.isRedirect && sameOrigin(previousUrl, request.uri) &&
                    extractDomain(request.uri) == container.domain &&
                    healthGuard.isSuspiciousLoginRedirect(previousUrl, request.uri)
                ) {
                    val fromUrl = previousUrl
                    engineScope.launch {
                        val fresh = containerDao.getById(container.id)?.toDomain() ?: return@launch
                        if (sessionContainers[session] != fresh.id) return@launch
                        reportSessionChallenge(
                            fresh,
                            SessionHealthSignal.LoginRedirectDetected(fresh.id, fresh.domain, fromUrl, request.uri),
                            onReauthenticationRequired,
                        )
                    }
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onSubframeLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest,
            ): GeckoResult<AllowOrDeny>? {
                val blockingEnabled = trackingProtectionActive(tabId, previousUrl.ifBlank { request.uri }, adBlockEnabled)
                if (adBlocker.shouldBlockRequest(request.uri, previousUrl, blockingEnabled)) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                val safeUrl = url.orEmpty()
                if (safeUrl.isNotBlank()) {
                    if (previousUrl != safeUrl) {
                        loginNavigationIds[session] = UUID.randomUUID().toString()
                        snapshotRequests.remove(session)?.response?.complete(false)
                        cancelPendingRestore(session)
                    }
                    previousUrl = safeUrl
                    lastKnownUrls[session] = safeUrl
                    // ورود به یک دامنهٔ بانکی (یا خروج از آن) همان لحظه محافظتِ ردیابیِ همین
                    // نشست را عوض می‌کند تا کاربر مجبور نباشد تب را ببندد و باز کند.
                    runCatching {
                        session.settings.useTrackingProtection =
                            trackingProtectionActive(tabId, safeUrl, adBlockByTab[tabId] ?: adBlockEnabled)
                    }
                    onLocationChanged(safeUrl, safeUrl.startsWith("https://"))
                    if (session !in loadingSessions) onLoginPageChanged(safeUrl)
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                canBack = canGoBack
                onNavStateChanged(canBack, canFwd)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                canFwd = canGoForward
                onNavStateChanged(canBack, canFwd)
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                loadingSessions.add(session)
                loginNavigationIds[session] = UUID.randomUUID().toString()
                snapshotRequests.remove(session)?.response?.complete(false)
                cancelPendingRestore(session)
                capturePorts.remove(session)
                onProgressChanged(0, true)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                onProgressChanged(progress, progress < 100)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                loadingSessions.remove(session)
                onProgressChanged(100, false)
                if (success) lastKnownUrls[session]?.let(onLoginPageChanged)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onCloseRequest(session: GeckoSession) = onWindowClose()

            override fun onCrash(session: GeckoSession) = onProcessExit(session)

            override fun onKill(session: GeckoSession) = onProcessExit(session)

            private fun onProcessExit(session: GeckoSession) {
                if (activeSessions[tabId] !== session) return
                val permissions = permissionProfiles[tabId]
                closeTabSession(tabId)
                if (permissions != null) permissionProfiles[tabId] = permissions
                onProcessEnded()
            }

            override fun onContextMenu(
                session: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement,
            ) {
                val link = element.linkUri?.takeIf(::isHttpUrl)
                val image = element.srcUri?.takeIf { element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE }
                    ?.takeIf(::isHttpUrl)
                // Long-pressing anywhere opens the context menu ("keep holding your finger on
                // the page content"): links and images offer their actions, plain content offers
                // the page-level ones. (`altText`/`title` are the text-ish properties available
                // on the session context element across GeckoView builds.)
                val text = (element.altText ?: element.title)?.trim()?.takeIf { it.isNotEmpty() }
                onContextMenu(PageContextTarget(link, image, text))
            }

            override fun onTitleChange(session: GeckoSession, title: String?) {
                if (!title.isNullOrBlank()) {
                    onTitleChanged(title)
                }
            }

            override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                onDownloadRequested(response.uri, null)
            }
        }
    }

    private fun mergeSnapshotCookies(json: JSONObject, origin: String): List<CookieRecord> {
        val host = runCatching { URI(origin).host?.lowercase() }.getOrNull() ?: return emptyList()
        val array = json.optJSONArray("cookies") ?: JSONArray()
        val observed = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            // Without observed scope metadata we cannot safely back up or replay a cookie.
            val domain = item.optString("domain").removePrefix(".").lowercase()
            val hostOnly = item.optBoolean("isHostOnly", true)
            if (domain.isBlank() || (hostOnly && domain != host) ||
                (!hostOnly && domain != host && !host.endsWith(".$domain"))
            ) {
                return@mapNotNull null
            }
            CookieRecord(
                name = item.optString("name"),
                value = item.optString("value"),
                domain = domain,
                path = item.optString("path", "/"),
                expiresEpochMillis = if (item.isNull("expiresEpochMillis")) null else item.optLong("expiresEpochMillis"),
                isSessionOnly = item.optBoolean("isSessionOnly"),
                isSecure = item.optBoolean("isSecure"),
                isHttpOnly = item.optBoolean("isHttpOnly"),
                sameSite = item.optString("sameSite", "unspecified"),
                isHostOnly = hostOnly,
            ).takeIf { it.name.isNotBlank() }
        }
        // document.cookie omits path/domain/expiry/HttpOnly metadata. Guessing it would widen
        // cookie scope or overwrite an HttpOnly cookie on restore; only use observed headers.
        return observed
    }

    private fun jsonObjectToMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = obj.optString(key, "")
        }
        return result
    }

    private fun writeGeckoProxyConfigFile(config: ProxyDnsConfig): File {
        val file = File(appContext.filesDir, "gecko-network-config.yaml")
        // هرگز مقدارِ خامِ کاربر را داخلِ YAML ننویسیم؛ نقل‌قول و کاراکترهای کنترلی حذف می‌شوند.
        val host = config.proxyHost.trim()
            .replace("\"", "")
            .replace("'", "")
            .filterNot { it == '\r' || it == '\n' || it == '\u0000' }
        val port = config.proxyPort.coerceIn(1, 65535)
        val yaml = buildString {
            appendLine("prefs:")
            when (config.proxyType) {
                ProxyType.HTTP -> {
                    appendLine("  network.proxy.type: 1")
                    appendLine("  network.proxy.http: \"$host\"")
                    appendLine("  network.proxy.http_port: $port")
                    appendLine("  network.proxy.ssl: \"$host\"")
                    appendLine("  network.proxy.ssl_port: $port")
                }

                ProxyType.SOCKS5 -> {
                    appendLine("  network.proxy.type: 1")
                    appendLine("  network.proxy.socks: \"$host\"")
                    appendLine("  network.proxy.socks_port: $port")
                    appendLine("  network.proxy.socks_version: 5")
                    appendLine("  network.proxy.socks_remote_dns: true")
                }

                ProxyType.NONE -> {
                    appendLine("  network.proxy.type: 5")
                }
            }
            // خالی‌کردنِ هر فهرستِ استثنای قبلی؛ وگرنه پروکسی برای بعضی دامنه‌ها اعمال نمی‌شد.
            appendLine("  network.proxy.no_proxies_on: \"\"")
            // Let the navigation delegate decide before Gecko creates the original window.
            // The built-in blocker replays window.open asynchronously and loses its return value.
            appendLine("  dom.disable_open_during_load: false")
        }
        file.writeText(yaml)
        return file
    }

    companion object {
        private const val EXTENSION_START_TIMEOUT_MILLIS = 10_000L
        private const val NETWORK_CONFIG_WAIT_MILLIS = 1_500L
        private const val AUTO_SNAPSHOT_TIMEOUT_MILLIS = 4_000L
        private const val MANUAL_SNAPSHOT_TIMEOUT_MILLIS = 9_000L
        private const val MANUAL_SNAPSHOT_RETRY_DELAY_MILLIS = 500L

        /** سقفِ حافظهٔ تشخیصِ «دامنهٔ بانکی»؛ از رشدِ بی‌پایان در مرورِ طولانی جلوگیری می‌کند. */
        private const val COMPATIBILITY_CACHE_LIMIT = 512

        const val EXTENSION_ASSET_URI = "resource://android/assets/extensions/never_signout/"
        const val EXTENSION_ID = "never_signout@marblebr.marble098.com"
        const val NATIVE_BRIDGE_APP_ID = "marble_never_signout"

        /** رویدادی که content script پس از اجرای اسکریپتِ بومی با نتیجه برمی‌گرداند. */
        const val JS_RESULT_EVENT = "JS_RESULT"

        /** لحظهٔ ارسالِ فرمِ ورود در صفحه؛ برای پیشنهادِ «ذخیرهٔ این ورود». */
        const val LOGIN_SUBMIT_EVENT = "LOGIN_SUBMIT"

        /** صفحه کادرِ رمزِ یکبارمصرف دارد و منتظرِ کد است. */

        /** مهلتِ پاسخِ صفحه به درخواستِ اجرای اسکریپت (حالت مطالعه/بلندخوانی). */
        const val SCRIPT_TIMEOUT_MILLIS = 2_500L

        /**
         * پرچم‌های امن برای دکمه «پاک کردن کش»:
         * فقط کش شبکه و کش تصاویر پاک می‌شود و هرگز کوکی‌ها یا نشست‌ها لمس نمی‌شوند.
         */
        const val SAFE_CACHE_ONLY_CLEAR_FLAGS: Long =
            StorageController.ClearFlags.ALL_CACHES or
                StorageController.ClearFlags.NETWORK_CACHE or
                StorageController.ClearFlags.IMAGE_CACHE

        fun isHttpUrl(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)

        fun extractDomain(url: String): String =
            runCatching {
                val normalized = if (url.contains("://")) url else "https://$url"
                URI(normalized).host?.lowercase()?.removePrefix("www.") ?: "marblebr.local"
            }.getOrDefault("marblebr.local")
    }
}

/** Identity of a non-private, HTTPS browser document in one account container. */
data class LoginPage internal constructor(
    val tabId: String,
    val containerId: String,
    val url: String,
    internal val navigationId: String,
)
