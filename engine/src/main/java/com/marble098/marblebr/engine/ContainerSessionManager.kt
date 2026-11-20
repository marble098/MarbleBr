package com.marble098.marblebr.engine

import android.content.Context
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.ContainerEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.vault.SessionVaultManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import javax.inject.Singleton

/**
 * مدیریت مرکزی GeckoRuntime و کانتینرهای ایزوله GeckoSession.
 *
 * هر وب‌اپ یا PWA یک کانتینر مستقل با `contextId` اختصاصی دریافت می‌کند:
 * - خرابی یا پاک‌سازی یک کانتینر هیچ اثری روی بقیه ندارد.
 * - دکمه «پاک کردن کش» صرفاً `ClearFlags.ALL_CACHES` (`NETWORK_CACHE | IMAGE_CACHE`) را پاک می‌کند و هرگز نشست‌ها را پاک نمی‌کند.
 * - دکمه «Sign out now» داده‌های همان `contextId` و اسنپ‌شات‌های Session Vault آن سایت را به‌طور کامل پاک می‌کند.
 */
@Singleton
class ContainerSessionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val containerDao: ContainerDao,
    private val vaultManager: SessionVaultManager,
    private val healthGuard: SessionHealthGuard,
    private val adBlocker: LightweightAdBlocker,
    private val localizationManager: LocalizationManager,
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeSessions = ConcurrentHashMap<String, GeckoSession>()
    private val capturePorts = mutableMapOf<GeckoSession, WebExtension.Port>()
    private val snapshotRequests = mutableMapOf<GeckoSession, SnapshotRequest>()
    private var installedExtension: WebExtension? = null

    @Volatile
    private var geckoRuntime: GeckoRuntime? = null

    @Synchronized
    fun getOrCreateRuntime(proxyDnsConfig: ProxyDnsConfig = ProxyDnsConfig()): GeckoRuntime {
        geckoRuntime?.let { return it }

        val contentBlocking = ContentBlocking.Settings.Builder()
            .antiTracking(
                ContentBlocking.AntiTracking.DEFAULT or ContentBlocking.AntiTracking.STP,
            )
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)
            .build()

        val settingsBuilder = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .loginAutofillEnabled(true)
            .globalPrivacyControlEnabled(true)
            .contentBlocking(contentBlocking)

        proxyDnsConfig.resolvedDohUri()?.let { dohUri ->
            settingsBuilder.defaultRecursiveResolverUri(dohUri)
        }

        if (proxyDnsConfig.proxyType != ProxyType.NONE && proxyDnsConfig.proxyHost.isNotBlank()) {
            val configFile = writeGeckoProxyConfigFile(proxyDnsConfig)
            settingsBuilder.configFilePath(configFile.absolutePath)
        }

        val runtime = GeckoRuntime.create(appContext, settingsBuilder.build())
        installNeverSignOutExtension(runtime)
        geckoRuntime = runtime
        return runtime
    }

    /**
     * بازیابی یا ساخت کانتینر ایزوله برای یک دامنه یا PWA.
     */
    suspend fun getOrCreateContainerForUrl(
        url: String,
        isPinnedPwa: Boolean = false,
        customDisplayName: String? = null,
    ): WebAppContainer {
        val domain = extractDomain(url)
        if (!isPinnedPwa) {
            val existing = containerDao.getByDomain(domain)
            if (existing != null) {
                return existing.toDomain()
            }
        }

        val cleanId = domain.replace(Regex("[^a-zA-Z0-9_-]"), "_") +
            if (isPinnedPwa) "_pwa_${UUID.randomUUID().toString().take(6)}" else ""
        val container = WebAppContainer(
            id = cleanId,
            domain = domain,
            displayName = customDisplayName ?: domain,
            contextId = "marble_ctx_$cleanId",
            neverSignOutEnabled = true,
            cookieExpirationDays = 365,
            keepAliveEnabled = true,
            keepAliveIntervalMinutes = 30,
            isPinnedPwa = isPinnedPwa,
            pwaStartUrl = if (isPinnedPwa) url else null,
        )
        containerDao.upsert(ContainerEntity.fromDomain(container))
        return container
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
        onHealthRestored: (reason: HealthReason) -> Unit = {},
        onServerRevokedFallbackRequired: (domain: String) -> Unit = {},
        onDownloadRequested: (url: String, userAgent: String?) -> Unit = { _, _ -> },
    ): GeckoSession {
        activeSessions[tabId]?.let { return it }

        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .contextId(if (isPrivate) null else container.contextId)
            .useTrackingProtection(adBlockEnabled)
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
            onHealthRestored = onHealthRestored,
            onServerRevokedFallbackRequired = onServerRevokedFallbackRequired,
            onDownloadRequested = onDownloadRequested,
        )

        if (!isPrivate) {
            val attachBridge: (WebExtension) -> Unit = { extension ->
                attachSessionExtensionBridge(session, extension, container, onHealthRestored, onServerRevokedFallbackRequired)
            }
            val extension = installedExtension
            if (extension != null) {
                attachBridge(extension)
            } else {
                runtime.webExtensionController.ensureBuiltIn(EXTENSION_ASSET_URI, EXTENSION_ID).accept { available ->
                    if (available != null && activeSessions[tabId] === session) attachBridge(available)
                }
            }
        }

        activeSessions[tabId] = session
        session.open(runtime)
        session.loadUri(initialUrl)
        return session
    }

    fun loadInOpenSession(tabId: String, url: String) {
        activeSessions[tabId]?.loadUri(url)
    }

    /** نشستِ بازِ فعلیِ یک تب؛ برای اتصالِ دوبارهٔ نما هنگام بازترکیب (recomposition) استفاده می‌شود. */
    fun activeSession(tabId: String): GeckoSession? = activeSessions[tabId]

    fun stopLoading(tabId: String) {
        activeSessions[tabId]?.stop()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val session = activeSessions[tabId] ?: return
        session.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.reload()
    }

    fun goBack(tabId: String) {
        activeSessions[tabId]?.goBack()
    }

    fun goForward(tabId: String) {
        activeSessions[tabId]?.goForward()
    }

    fun reload(tabId: String) {
        activeSessions[tabId]?.reload()
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
    suspend fun signOutNow(container: WebAppContainer) {
        geckoRuntime?.storageController?.let { controller ->
            controller.clearDataForSessionContext(container.contextId)
            controller.clearDataFromBaseDomain(
                container.domain,
                StorageController.ClearFlags.COOKIES or
                    StorageController.ClearFlags.DOM_STORAGES or
                    StorageController.ClearFlags.AUTH_SESSIONS,
            )
        }
        vaultManager.purgeSnapshotsForContainer(container.id)
        healthGuard.resetCircuitBreaker(container.id)
    }

    fun closeTabSession(tabId: String) {
        val session = activeSessions.remove(tabId) ?: return
        capturePorts.remove(session)?.disconnect()
        snapshotRequests.remove(session)?.response?.complete(false)
        session.close()
    }

    suspend fun capturePageSnapshot(tabId: String, pageUrl: String): Boolean {
        val session = activeSessions[tabId] ?: return false
        val port = capturePorts[session] ?: return false
        if (!sameOrigin(port.sender.url, pageUrl) || session in snapshotRequests) return false
        val request = SnapshotRequest(UUID.randomUUID().toString(), CompletableDeferred())
        snapshotRequests[session] = request
        return try {
            port.postMessage(JSONObject().put("type", "CAPTURE_PAGE").put("requestId", request.id))
            withTimeoutOrNull(4_000) { request.response.await() } ?: false
        } finally {
            snapshotRequests.remove(session)
        }
    }

    private fun sameOrigin(first: String, second: String): Boolean = runCatching {
        val one = URI(first)
        val two = URI(second)
        one.host != null && one.host.equals(two.host, true) && one.scheme.equals(two.scheme, true) && effectivePort(one) == effectivePort(two)
    }.getOrDefault(false)

    private fun effectivePort(uri: URI): Int = uri.port.takeIf { it >= 0 } ?: if (uri.scheme.equals("https", true)) 443 else 80

    private data class SnapshotRequest(val id: String, val response: CompletableDeferred<Boolean>)

    private fun installNeverSignOutExtension(runtime: GeckoRuntime) {
        runtime.webExtensionController
            .ensureBuiltIn(EXTENSION_ASSET_URI, EXTENSION_ID)
            .accept { extension ->
                if (extension != null) {
                    installedExtension = extension
                }
            }
    }

    private fun attachSessionExtensionBridge(
        session: GeckoSession,
        extension: WebExtension,
        container: WebAppContainer,
        onHealthRestored: (HealthReason) -> Unit,
        onServerRevokedFallbackRequired: (String) -> Unit,
    ) {
        session.webExtensionController.setMessageDelegate(
            extension,
            object : WebExtension.MessageDelegate {
                override fun onConnect(port: WebExtension.Port) {
                    if (!port.sender.isTopLevel || extractDomain(port.sender.url) != container.domain) return
                    capturePorts[session] = port
                    port.setDelegate(object : WebExtension.PortDelegate {
                        override fun onPortMessage(message: Any, port: WebExtension.Port) {
                            if (capturePorts[session] !== port) return
                            val json = message as? JSONObject ?: return
                            val request = snapshotRequests[session] ?: return
                            if (request.id != json.optString("requestId") || !sameOrigin(port.sender.url, json.optString("origin"))) return
                            engineScope.launch {
                                val saved = runCatching { savePageSnapshot(json, container, manual = true) }.getOrDefault(false)
                                request.response.complete(saved)
                            }
                        }

                        override fun onDisconnect(port: WebExtension.Port) {
                            if (capturePorts[session] === port) {
                                capturePorts.remove(session)
                                snapshotRequests[session]?.response?.complete(false)
                            }
                        }
                    })
                }

                override fun onMessage(
                    nativeApp: String,
                    message: Any,
                    sender: WebExtension.MessageSender,
                ): GeckoResult<Any>? {
                    val json = message as? JSONObject ?: return null
                    val eventType = json.optString("event")
                    if (eventType == "STORAGE_SNAPSHOT" && (!sender.isTopLevel || !sameOrigin(sender.url, json.optString("origin")))) return null
                    engineScope.launch {
                        handleExtensionEvent(
                            json = json,
                            eventType = eventType,
                            session = session,
                            container = container,
                            onHealthRestored = onHealthRestored,
                            onServerRevokedFallbackRequired = onServerRevokedFallbackRequired,
                        )
                    }
                    return GeckoResult.fromValue(JSONObject().put("ok", true))
                }
            },
            NATIVE_BRIDGE_APP_ID,
        )
    }

    private suspend fun handleExtensionEvent(
        json: JSONObject,
        eventType: String,
        session: GeckoSession,
        container: WebAppContainer,
        onHealthRestored: (HealthReason) -> Unit,
        onServerRevokedFallbackRequired: (String) -> Unit,
    ) {
        val freshContainer = containerDao.getById(container.id)?.toDomain() ?: container
        if (!freshContainer.neverSignOutEnabled) return

        when (eventType) {
            "STORAGE_SNAPSHOT" -> savePageSnapshot(json, freshContainer, manual = false)

            "HTTP_UNAUTHORIZED" -> {
                val url = json.optString("url")
                val statusCode = json.optInt("statusCode", 401)
                val assessment = healthGuard.evaluateSignal(
                    signal = SessionHealthSignal.HttpUnauthorized(
                        containerId = freshContainer.id,
                        domain = freshContainer.domain,
                        url = url,
                        statusCode = statusCode,
                    ),
                    neverSignOutEnabled = freshContainer.neverSignOutEnabled,
                )
                applyHealthGuardDecision(
                    assessmentAction = assessment.action,
                    reason = assessment.reason,
                    session = session,
                    container = freshContainer,
                    onHealthRestored = onHealthRestored,
                    onServerRevokedFallbackRequired = onServerRevokedFallbackRequired,
                )
            }

            "COOKIE_REMOVED" -> {
                val cookieName = json.optString("cookieName")
                val dummyCookie = CookieRecord(name = cookieName, value = "x", domain = freshContainer.domain)
                if (dummyCookie.isLikelyAuthCookie()) {
                    val assessment = healthGuard.evaluateSignal(
                        signal = SessionHealthSignal.AuthCookieMissing(
                            containerId = freshContainer.id,
                            domain = freshContainer.domain,
                            missingCookieNames = listOf(cookieName),
                        ),
                        neverSignOutEnabled = freshContainer.neverSignOutEnabled,
                    )
                    applyHealthGuardDecision(
                        assessmentAction = assessment.action,
                        reason = assessment.reason,
                        session = session,
                        container = freshContainer,
                        onHealthRestored = onHealthRestored,
                        onServerRevokedFallbackRequired = onServerRevokedFallbackRequired,
                    )
                }
            }
        }
    }

    private suspend fun savePageSnapshot(json: JSONObject, container: WebAppContainer, manual: Boolean): Boolean {
        val origin = json.optString("origin")
        if (extractDomain(origin) != container.domain) return false
        val payload = SessionSnapshotPayload(
            origin = origin,
            containerId = container.id,
            capturedAtEpochMillis = System.currentTimeMillis(),
            cookies = parseDocumentCookieString(json.optString("cookieString"), container.domain),
            localStorage = jsonObjectToMap(json.optJSONObject("localStorage")),
            sessionStorage = jsonObjectToMap(json.optJSONObject("sessionStorage")),
            indexedDbDump = jsonObjectToMap(json.optJSONObject("indexedDbDump")),
            storagePersisted = json.optBoolean("storagePersisted"),
        )
        if (!vaultManager.evaluatePayloadHealth(payload)) return false
        val strings = localizationManager.current()
        vaultManager.captureSnapshot(
            payload = payload,
            targetCookieExpirationDays = container.cookieExpirationDays,
            triggerReason = if (manual) "MANUAL_USER_CAPTURE" else "PAGE_LOAD_SYNC",
            label = if (manual) strings.manualSnapshotLabel(container.domain) else strings.autoSnapshotLabel(container.domain),
        )
        return true
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
        onHealthRestored: (HealthReason) -> Unit,
        onServerRevokedFallbackRequired: (domain: String) -> Unit,
        onDownloadRequested: (url: String, userAgent: String?) -> Unit,
    ) {
        var previousUrl = ""
        var canBack = false
        var canFwd = false

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest,
            ): GeckoResult<AllowOrDeny>? {
                if (!request.isDirectNavigation && !request.hasUserGesture && adBlocker.shouldBlockRequest(request.uri, previousUrl, adBlockEnabled)) {
                    onProgressChanged(0, false)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                if (!isPrivate && healthGuard.isSuspiciousLoginRedirect(previousUrl, request.uri)) {
                    engineScope.launch {
                        val freshContainer = containerDao.getById(container.id)?.toDomain() ?: container
                        val assessment = healthGuard.evaluateSignal(
                            signal = SessionHealthSignal.LoginRedirectDetected(
                                containerId = freshContainer.id,
                                domain = freshContainer.domain,
                                fromUrl = previousUrl,
                                redirectedToUrl = request.uri,
                            ),
                            neverSignOutEnabled = freshContainer.neverSignOutEnabled,
                        )
                        applyHealthGuardDecision(
                            assessmentAction = assessment.action,
                            reason = assessment.reason,
                            session = session,
                            container = freshContainer,
                            onHealthRestored = onHealthRestored,
                            onServerRevokedFallbackRequired = onServerRevokedFallbackRequired,
                        )
                    }
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onSubframeLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest,
            ): GeckoResult<AllowOrDeny>? {
                if (adBlocker.shouldBlockRequest(request.uri, previousUrl, adBlockEnabled)) {
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
                    previousUrl = safeUrl
                    onLocationChanged(safeUrl, safeUrl.startsWith("https://"))
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
                onProgressChanged(0, true)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                onProgressChanged(progress, progress < 100)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                onProgressChanged(100, false)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
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

    private suspend fun applyHealthGuardDecision(
        assessmentAction: HealthGuardAction,
        reason: HealthReason,
        session: GeckoSession,
        container: WebAppContainer,
        onHealthRestored: (HealthReason) -> Unit,
        onServerRevokedFallbackRequired: (String) -> Unit,
    ) {
        when (assessmentAction) {
            HealthGuardAction.RESTORE_LATEST_SNAPSHOT -> {
                val restored = vaultManager.restoreLatestHealthySnapshot(container.id)
                if (restored != null) {
                    session.reload()
                    onHealthRestored(reason)
                }
            }
            HealthGuardAction.ESCALATE_TO_CREDENTIAL_FALLBACK -> {
                onServerRevokedFallbackRequired(container.domain)
            }
            HealthGuardAction.NONE -> Unit
        }
    }

    private fun parseDocumentCookieString(cookieString: String, domain: String): List<CookieRecord> {
        if (cookieString.isBlank()) return emptyList()
        return cookieString.split(";")
            .mapNotNull { pair ->
                val trimmed = pair.trim()
                val idx = trimmed.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val name = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim()
                CookieRecord(
                    name = name,
                    value = value,
                    domain = domain,
                    isSessionOnly = true,
                )
            }
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
        val yaml = buildString {
            appendLine("prefs:")
            when (config.proxyType) {
                ProxyType.HTTP -> {
                    appendLine("  network.proxy.type: 1")
                    appendLine("  network.proxy.http: \"${config.proxyHost}\"")
                    appendLine("  network.proxy.http_port: ${config.proxyPort}")
                    appendLine("  network.proxy.ssl: \"${config.proxyHost}\"")
                    appendLine("  network.proxy.ssl_port: ${config.proxyPort}")
                }
                ProxyType.SOCKS5 -> {
                    appendLine("  network.proxy.type: 1")
                    appendLine("  network.proxy.socks: \"${config.proxyHost}\"")
                    appendLine("  network.proxy.socks_port: ${config.proxyPort}")
                    appendLine("  network.proxy.socks_version: 5")
                    appendLine("  network.proxy.socks_remote_dns: true")
                }
                ProxyType.NONE -> {
                    appendLine("  network.proxy.type: 5")
                }
            }
        }
        file.writeText(yaml)
        return file
    }

    companion object {
        const val EXTENSION_ASSET_URI = "resource://android/assets/extensions/never_signout/"
        const val EXTENSION_ID = "never_signout@marblebr.marble098.com"
        const val NATIVE_BRIDGE_APP_ID = "marble_never_signout"

        /**
         * پرچم‌های امن برای دکمه «پاک کردن کش»:
         * فقط کش شبکه و کش تصاویر پاک می‌شود و هرگز کوکی‌ها یا نشست‌ها لمس نمی‌شوند.
         */
        const val SAFE_CACHE_ONLY_CLEAR_FLAGS: Long =
            StorageController.ClearFlags.ALL_CACHES or
                StorageController.ClearFlags.NETWORK_CACHE or
                StorageController.ClearFlags.IMAGE_CACHE

        fun extractDomain(url: String): String =
            runCatching {
                val normalized = if (url.contains("://")) url else "https://$url"
                URI(normalized).host?.lowercase()?.removePrefix("www.") ?: "marblebr.local"
            }.getOrDefault("marblebr.local")
    }
}
