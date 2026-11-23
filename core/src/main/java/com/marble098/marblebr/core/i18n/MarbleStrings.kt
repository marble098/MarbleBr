package com.marble098.marblebr.core.i18n

import com.marble098.marblebr.core.model.BackButtonBehavior
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.ColorPalette
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.ThemeMode
import java.util.Locale

/**
 * همه ی متن های قابل نمایش MarbleBr در یک جا و به دو زبان.
 *
 * متن ها عمداً ساده، کوتاه و صمیمی نوشته شده اند: هر گزینه می گوید «چه کاری می کند» و «چه سودی دارد»،
 * بدون اصطلاح فنیِ گیج کننده.
 */
abstract class MarbleStrings {

    // Multi-account browsing, encrypted credentials and page actions.
    abstract val accountsTitle: String
    abstract val accountsDescription: String
    abstract val accountCurrent: String
    abstract val accountCreate: String
    abstract val accountName: String
    abstract val accountOpen: String
    abstract val credentialVaultTitle: String
    abstract val credentialVaultDescription: String
    abstract val credentialLockRequired: String
    abstract val credentialLockUnavailable: String
    abstract val credentialAdd: String
    abstract val credentialEdit: String
    abstract val credentialSite: String
    abstract val credentialOriginReview: String
    abstract val credentialUsername: String
    abstract val credentialPassword: String
    abstract val credentialNotes: String
    abstract val credentialSave: String
    abstract val credentialDelete: String
    abstract val credentialDeleteBody: String
    abstract val credentialInvalid: String
    abstract val credentialFailed: String
    abstract val credentialEmpty: String
    abstract val credentialUnreadable: String
    abstract val credentialGenerate: String
    abstract val credentialShow: String
    abstract val credentialHide: String
    abstract val credentialCopyUser: String
    abstract val credentialCopyPassword: String
    abstract val credentialCopied: String
    abstract val contextTitle: String
    abstract val contextOpenTab: String
    abstract val contextCopyLink: String
    abstract val contextShareLink: String
    abstract val contextSaveImage: String
    abstract val contextCopyImage: String
    abstract val contextShareText: String
    abstract val contextSavePdf: String
    abstract val contextCopyText: String
    abstract val menuSharePage: String
    abstract val pdfSaved: String
    abstract val pdfFailed: String
    abstract val credentialUnlock: String
    abstract val accountSessionWarning: String

    // ---------------------------------------------------------------- عمومی
    abstract val appName: String
    abstract val back: String
    abstract val cancel: String
    abstract val ok: String
    abstract val later: String
    abstract val close: String
    abstract val dismiss: String

    // ------------------------------------------------------- زبان و ظاهر
    abstract val languageTitle: String
    abstract val languageDescription: String
    abstract val languageSystem: String
    abstract val languagePersian: String
    abstract val languageEnglish: String
    abstract val appearanceTitle: String
    abstract val themeModeTitle: String
    abstract val themeSystem: String
    abstract val themeLight: String
    abstract val themeDark: String
    abstract val dynamicColorTitle: String
    abstract val dynamicColorDescription: String

    // --------------------------------------------------------- مرورگر
    abstract val addressPlaceholder: String
    abstract val copyLink: String
    abstract val shareLink: String
    abstract val browserMenu: String
    abstract val tabsLabel: String
    abstract val protectionLabel: String
    abstract val bookmarksLabel: String
    abstract val settingsLabel: String
    abstract val bookmarkToggle: String
    abstract val reload: String
    abstract val goBack: String
    abstract val goForward: String
    abstract val protectionOnChip: String
    abstract val protectionOffChip: String
    abstract val privateTabChip: String
    abstract val menuSaveState: String
    abstract val menuSaveStateHint: String
    abstract val menuTimeMachine: String
    abstract val menuTimeMachineHint: String
    abstract val menuFindInPage: String
    abstract val menuDesktopModeOn: String
    abstract val menuDesktopModeOff: String
    abstract val menuSaveShortcut: String
    abstract val menuFreeSpace: String
    abstract val menuFreeSpaceHint: String
    abstract val findInPagePlaceholder: String
    abstract val findPrevious: String
    abstract val findNext: String
    abstract val findClose: String
    abstract val reloginTitle: String
    abstract val reloginConfirm: String
    abstract val reloginDismiss: String
    abstract val readyTitle: String
    abstract val readyBody: String
    abstract val quickSaveState: String
    abstract val quickEarlierCopies: String
    abstract val newTab: String
    abstract val newPrivateTab: String
    abstract val closeTab: String
    abstract val newTabTitle: String
    abstract val privateTabTitle: String
    abstract val addressSelectionTitle: String
    abstract val addressSelectionDescription: String
    abstract val addressSelectAll: String
    abstract val addressSelectPart: String
    abstract val addressCursor: String
    abstract val googleNewTab: String
    abstract val googleNewTabDescription: String
    abstract val goToAddress: String
    abstract val navigationFailed: String
    abstract val tabProcessEnded: String
    abstract val stopLoading: String
    abstract val duplicateTab: String
    abstract val reopenTab: String
    abstract val closeAllTabs: String
    abstract val confirmCloseAllTabs: String
    abstract val searchTabs: String
    abstract val noTabs: String
    abstract val noMatchingTabs: String
    abstract val clearHistory: String
    abstract val emptyBookmarks: String
    abstract val emptyHistory: String
    abstract val emptyDownloads: String
    abstract val downloadNotificationText: String

    // -------------------------------------------- پیام های کوتاه (بنر)
    abstract val bannerBookmarkAdded: String
    abstract val bannerBookmarkRemoved: String
    abstract val bannerShortcutAdded: String
    abstract val bannerShortcutCreated: String
    abstract val bannerSnapshotSaved: String
    abstract val bannerCacheCleared: String
    abstract val bannerHistoryCleared: String
    abstract val bannerDownloadStarted: String
    abstract val downloadFailed: String
    abstract val downloadInProgress: String
    abstract val snapshotUnavailable: String
    abstract val bannerDesktopOn: String
    abstract val bannerDesktopOff: String

    // ------------------------------------------------------- تنظیمات
    abstract val settingsTitle: String
    abstract val settingsIntro: String
    abstract val protectionSectionTitle: String
    abstract val protectionSectionBody: String
    abstract val globalNeverSignOutTitle: String
    abstract val globalNeverSignOutDescription: String
    abstract val biometricTitle: String
    abstract val biometricDescription: String
    abstract val keepAliveTitle: String
    abstract val keepAliveDescription: String
    abstract val credentialTitle: String
    abstract val credentialDescription: String
    abstract val adBlockTitle: String
    abstract val adBlockDescription: String
    abstract val freeSpaceTitle: String
    abstract val freeSpaceDescription: String
    abstract val freeSpaceButton: String
    abstract val freeSpaceDone: String
    abstract val networkTitle: String
    abstract val networkBody: String
    abstract val dnsLabel: String
    abstract val dnsCustomHint: String
    abstract val proxyLabel: String
    abstract val proxyHostHint: String
    abstract val proxyPortHint: String
    abstract val saveNetwork: String
    abstract val networkSaved: String
    abstract val aboutTitle: String

    // ---------------------------------------------------- گاوصندوق
    abstract val vaultTitle: String
    abstract val vaultIntro: String
    abstract val vaultSecurityTitle: String
    abstract val vaultSecurityBody: String
    abstract val unlockVault: String
    abstract val biometricPromptTitle: String
    abstract val biometricPromptSubtitle: String
    abstract val biometricCancelled: String
    abstract val reloginUnavailable: String
    abstract val reloginUnsupportedType: String
    abstract val reloginManualHint: String
    abstract val reloginPromptSubtitle: String
    abstract val sitesSectionTitle: String
    abstract val sitesSectionBody: String
    abstract val protectThisSite: String
    abstract val signOutNow: String
    abstract val signOutConfirmTitle: String
    abstract val signOutConfirmBody: String
    abstract val signOutConfirmAction: String
    abstract val timeMachineTitle: String
    abstract val timeMachineBody: String
    abstract val emptySnapshots: String
    abstract val emptySites: String
    abstract val backgroundSnapshotLabel: String
    abstract val restore: String
    abstract val healthLogTitle: String
    abstract val healthLogBody: String
    abstract val feedbackRestoreFailed: String
    abstract val gotIt: String

    // ------------------------------------------- داشبورد بازطراحی‌شده
    abstract val vaultHeroTitle: String
    abstract val vaultHeroBody: String
    abstract val vaultStatSites: String
    abstract val vaultStatProtected: String
    abstract val vaultStatSnapshots: String
    abstract val vaultLastSaved: String
    abstract val vaultNever: String
    abstract val vaultSearchHint: String
    abstract val vaultFilterAll: String
    abstract val vaultFilterProtected: String
    abstract val vaultFilterUnprotected: String
    abstract val vaultFilterPinned: String
    abstract val vaultNoMatch: String
    abstract val vaultProtectionOffBanner: String
    abstract val vaultSectionSites: String
    abstract val vaultSectionTimeline: String
    abstract val vaultSectionActivity: String
    abstract val vaultSiteProtected: String
    abstract val vaultSiteUnprotected: String
    abstract val vaultSitePinned: String
    abstract val vaultSiteDetailsTitle: String
    abstract val vaultProtectSwitchTitle: String
    abstract val vaultProtectSwitchBody: String
    abstract val vaultKeepAliveSwitchTitle: String
    abstract val vaultKeepAliveSwitchBody: String
    abstract val vaultCookieDaysTitle: String
    abstract val vaultCookieDaysBody: String
    abstract val vaultSnapshotsOfSite: String
    abstract val vaultSaveNow: String
    abstract val vaultDelete: String
    abstract val vaultDeleteConfirmTitle: String
    abstract val vaultDeleteConfirmBody: String
    abstract val vaultClose: String
    abstract val vaultEmptyActivity: String
    abstract val vaultFilterEmpty: String
    abstract val vaultHealthHealthy: String
    abstract val vaultProtectAll: String
    abstract val vaultUnprotectAll: String
    abstract val vaultProtectAllOnDone: String
    abstract val vaultProtectAllOffDone: String
    abstract val vaultRestoreNeedsOpenTab: String
    abstract val vaultRestoreSuperseded: String
    abstract val vaultRestoreConfirmTitle: String
    abstract val vaultRestoreConfirmBody: String
    abstract val vaultQuickActionsTitle: String
    abstract val vaultJumpToSite: String
    abstract val feedbackRestoreEmpty: String
    abstract val quickPageSection: String
    abstract val quickLibrarySection: String
    abstract val quickToolsSection: String
    abstract fun vaultSnapshotCount(count: Int): String
    abstract fun vaultLastSavedAgo(label: String): String
    abstract fun vaultKeepAliveEveryMinutes(minutes: Int): String
    abstract fun vaultDeleteSnapshotDone(domain: String): String

    // ----------------------- منوی تازه، نسخهٔ طلایی، نگهبان انقضا و بیمهٔ نشست
    abstract val menuSectionPage: String
    abstract val menuSectionProtection: String
    abstract val menuSectionTools: String
    abstract val menuSectionApp: String
    abstract val menuProtectedChip: String
    abstract val menuPrivateHint: String

    abstract val vaultGoldenTitle: String
    abstract val vaultGoldenPin: String
    abstract val vaultGoldenUnpin: String
    abstract val vaultGoldenHint: String

    abstract val vaultExpiryTitle: String
    abstract val vaultExpiryBody: String
    abstract val vaultExpiryAllFresh: String
    abstract val vaultExpiryExpired: String
    abstract val vaultExpiryRenew: String
    abstract val vaultExpiryRenewAll: String

    abstract val vaultInsuranceTitle: String
    abstract val vaultInsuranceBody: String
    abstract val vaultInsuranceRun: String

    abstract fun vaultGoldenPinnedDone(domain: String): String
    abstract fun vaultGoldenRemoved(domain: String): String
    abstract fun vaultExpiryDaysLeft(days: Int): String
    abstract fun vaultExpiryRenewed(domain: String): String
    abstract fun vaultExpiryRenewedCount(count: Int): String
    abstract fun vaultInsuranceDone(count: Int): String

    // ---------------------------------------- افزودن هر سایتی
    abstract val addAnySiteTitle: String
    abstract val addAnySiteDescription: String
    abstract val addAnySiteHint: String
    abstract val addAnySiteAction: String
    abstract val addAnySiteAdded: String
    abstract fun addAnySiteFallback(attempt: Int, of: Int): String
    abstract fun addAnySiteRoute(route: String): String
    abstract val addAnySiteTriedAll: String
    abstract val routeDirect: String
    abstract val routeDesktop: String
    abstract val routeMobile: String
    abstract val routeCanonical: String
    abstract val routePwa: String
    abstract val routeManual: String
    abstract val routeReadOnly: String
    abstract val loginMethodTitle: String
    abstract val loginMethodPassword: String
    abstract val loginMethodGoogle: String
    abstract val loginMethodApple: String
    abstract val loginMethodPhone: String
    abstract val loginMethodEmailCode: String
    abstract val loginMethodMagicLink: String
    abstract val loginMethodPasskey: String
    abstract val loginMethodWallet: String
    abstract val loginMethodSso: String
    abstract val loginMethodUnknown: String
    abstract val loginMethodUnknownHint: String
    abstract val blockerIframe: String
    abstract val blockerBrowser: String
    abstract val blockerThirdParty: String
    abstract val blockerJavaScript: String
    abstract val blockerNetwork: String
    abstract val blockerOtp: String

    // ------------------------------------ ذخیرهٔ هوشمند گذرواژه
    abstract val saveLoginTitle: String
    abstract val saveLoginBody: String
    abstract val saveLoginConfirm: String
    abstract val saveLoginNotNow: String
    abstract val saveLoginNeverForSite: String
    abstract val saveLoginSaved: String
    abstract val saveLoginFailed: String
    abstract val saveLoginAutoTitle: String
    abstract val saveLoginAutoBody: String
    abstract val saveLoginFillTitle: String
    abstract val saveLoginFillAction: String
    abstract val saveLoginNoStored: String
    abstract val saveLoginWeakerTitle: String
    abstract val saveLoginWeakerBody: String

    /* ------------------------- «ورودِ هوشمند» و «رمزِ یکبارمصرف» ------------------------- */
    abstract val saveLoginUsernameTitle: String
    abstract val saveLoginConfirmPrefilled: String
    abstract val saveLoginAnotherAccount: String
    abstract val saveLoginPasswordChangedHint: String
    abstract val saveLoginFirstAccountHint: String
    abstract val saveLoginAccountAdded: String
    abstract val autofillConfirmTitle: String
    abstract val autofillConfirmBody: String
    abstract val autofillConfirmAction: String
    abstract val autofillUseNow: String
    abstract val autofillAlternatives: String
    abstract val autofillFillNow: String
    abstract val codeFieldChip: String
    abstract val codeUnavailableTitle: String
    abstract val codeUnavailableBody: String
    abstract val codeFilled: String
    abstract val codeSmsPermission: String
    abstract val codeNotificationAccess: String
    abstract val codeOpenSettings: String
    abstract val codeHowTitle: String
    abstract val codeHowBody: String
    abstract val codePasteFromClipboard: String
    abstract val codeClipboardEmpty: String
    abstract val codeManualHint: String
    abstract val loginRejectedNotice: String
    abstract val saveLoginNeverAgain: String
    abstract val saveModeTitle: String
    abstract val saveModeAsk: String
    abstract val saveModeAskBody: String
    abstract val saveModeAuto: String
    abstract val saveModeAutoBody: String
    abstract val saveModeOff: String
    abstract val saveModeOffBody: String
    abstract val otpAssistTitle: String
    abstract val otpAssistBody: String
    abstract fun otpSourceSummary(source: String): String
    abstract val otpManualAlwaysTitle: String
    abstract fun passwordStrengthLabel(level: String): String
    abstract val passwordStrengthVeryWeak: String
    abstract val passwordStrengthWeak: String
    abstract val passwordStrengthFair: String
    abstract val passwordStrengthStrong: String
    abstract val passwordStrengthExcellent: String
    abstract val phishingWarningTitle: String
    abstract fun phishingWarningBody(host: String, impersonated: String): String
    abstract val phishingWarningDismiss: String
    abstract val phishingWarningTrustOnce: String
    abstract val originTrusted: String
    abstract val originUnknown: String
    abstract val originLookalike: String
    abstract val loginGuardEnabledTitle: String
    abstract val autoFillEnabledTitle: String
    abstract val autoFillEnabledBody: String
    abstract val loginGuardEnabledBody: String

    // ------------------------------------- پشتیبان‌گیری
    abstract val backupTitle: String
    abstract val backupDescription: String
    abstract val backupCreate: String
    abstract val backupCreateBody: String
    abstract val backupPassword: String
    abstract val backupPasswordConfirm: String
    abstract val backupPasswordHint: String
    abstract val backupPasswordMismatch: String
    abstract val backupPasswordTooShort: String
    abstract val backupPasswordWarning: String
    abstract val backupRestore: String
    abstract val backupRestoreBody: String
    abstract val backupRestorePickHint: String
    abstract val backupRestorePreviewTitle: String
    abstract fun backupRestorePreviewBody(newItems: Int, updatedItems: Int, duplicates: Int): String
    abstract val backupRestoreApply: String
    abstract val backupRestoreDone: String
    abstract fun backupRestoreDoneBody(credentials: Int, sessions: Int, sites: Int): String
    abstract val backupWrongPassword: String
    abstract val backupNotABackup: String
    abstract val backupEmpty: String
    abstract val backupSavedTo: String
    abstract val backupFailed: String
    abstract val backupCantShare: String
    abstract val backupIncludeCredentials: String
    abstract val backupIncludeSessions: String
    abstract val backupIncludeSites: String

    // ------------------------------------------- نبض نشست
    abstract val pulseTitle: String
    abstract val pulseDescription: String
    abstract val pulseRunNow: String
    abstract val pulseEmpty: String
    abstract fun pulseSiteLine(domain: String, urgency: String): String
    abstract val pulseUrgencyLow: String
    abstract val pulseUrgencyNormal: String
    abstract val pulseUrgencyHigh: String
    abstract val pulseUrgencyCritical: String
    abstract val pulseDone: String
    abstract val pulseToggle: String

    // ----------------------------------------------------- فرمت ها
    abstract fun tabsCount(count: Int): String
    abstract fun adsBlocked(count: Int): String
    abstract fun downloadsMenu(count: Int): String
    abstract fun openTabsTitle(count: Int): String
    abstract fun bookmarksCount(count: Int): String
    abstract fun historyCount(count: Int): String
    abstract fun savedPath(path: String): String
    abstract fun reloginBody(domain: String): String
    abstract fun adBlockChip(count: Int): String
    abstract fun sitesCount(count: Int): String
    abstract fun keepAliveEvery(minutes: Int): String
    abstract fun keepSignedInFor(days: Int): String
    abstract fun signOutDialogTitle(domain: String): String
    abstract fun signOutDialogBody(domain: String): String
    abstract fun snapshotLine(date: String, cookies: Int, storageKeys: Int): String
    abstract fun feedbackProtection(domain: String, enabled: Boolean): String
    abstract fun feedbackCookieDays(domain: String, days: Int): String
    abstract fun feedbackKeepAliveOn(domain: String, minutes: Int): String
    abstract fun feedbackKeepAliveOff(domain: String): String
    abstract fun feedbackRestored(label: String, cookies: Int): String
    abstract fun feedbackSignedOut(domain: String): String
    abstract fun aboutBody(version: String): String
    abstract fun reloginAutoTitle(domain: String): String
    abstract fun autoSnapshotLabel(domain: String): String
    abstract fun manualSnapshotLabel(domain: String): String
    abstract fun pwaShortcutLongLabel(title: String): String

    // --------------------------------------------------- صفحه ی خانه
    abstract val homeTitle: String
    abstract val homeIntro: String
    abstract val homeSearchHint: String
    abstract val homeRecentTitle: String
    abstract val homeEmptyRecent: String
    abstract val homeShortcutsTitle: String
    abstract val homeEmptyShortcuts: String

    // --------------------------------------------------- امتیاز دهی
    abstract val rateTitle: String
    abstract val rateMessage: String
    abstract val rateNow: String
    abstract val rateLater: String
    abstract val rateDontAskAgain: String

    // --------------------------------------------- تنظیمات صفحه ی خانه
    abstract val homeScreenTitle: String
    abstract val searchEngineTitle: String
    abstract val searchEngineDescription: String
    abstract val homeShortcutPositionTitle: String
    abstract val homeShortcutPositionDescription: String
    abstract val shortcutPositionNone: String
    abstract val shortcutPositionFloating: String
    abstract val shortcutPositionBuiltIn: String
    abstract val homeSeeAll: String

    // ------------------------------------------- تب بندی و نوارهای شیشه ای
    abstract val settingsTabAppearance: String
    abstract val settingsTabBrowser: String
    abstract val settingsTabProtection: String
    abstract val settingsTabNetwork: String
    abstract val settingsTabAbout: String
    abstract val colorPaletteTitle: String
    abstract val colorPaletteDescription: String
    abstract fun colorPaletteLabel(palette: ColorPalette): String
    abstract val paletteDynamicHint: String
    abstract val themePreviewTitle: String
    abstract val themePreviewBody: String

    abstract val chromeBarStyleTitle: String
    abstract val chromeBarStyleDescription: String
    abstract val chromeSolid: String
    abstract val chromeGlass: String
    abstract val chromeFloating: String
    abstract val backBehaviorTitle: String
    abstract val backBehaviorDescription: String
    abstract val backSmart: String
    abstract val backPageThenHome: String
    abstract val backHome: String
    abstract val backPageThenExit: String
    abstract val backAsk: String
    abstract val backAskTitle: String
    abstract val backAskBody: String
    abstract val backAskToPage: String
    abstract val backAskToHome: String
    abstract val backAskExit: String
    abstract val tabsBehaviorTitle: String
    abstract val askBeforeClosingTabs: String
    abstract val askBeforeClosingTabsDescription: String
    abstract val closeTabConfirmTitle: String
    abstract fun closeTabConfirmBody(title: String): String

    // ------------------------------------------------ میانبرهای کاربردی
    abstract val quickActionsTitle: String
    abstract val quickCopyLink: String
    abstract val quickShare: String
    abstract val quickScreenshot: String
    abstract val quickFindInPage: String
    abstract val quickDesktop: String
    abstract val quickPrivate: String
    abstract val quickSaveLogin: String
    abstract val quickTimeMachine: String
    abstract val quickDownloads: String
    abstract val quickClearCache: String
    abstract val quickAddShortcut: String
    abstract val quickRefresh: String
    abstract val quickHome: String
    abstract val quickTabs: String
    abstract val quickSettings: String
    abstract val quickBookmarks: String
    abstract val quickSignOut: String
    abstract val quickNewTab: String
    abstract fun quickSiteSignedInFor(days: Int, minutes: Int): String

    // --------------------------------------------- پنل استار دادن به مخزن
    // --------------------------------------- سه قابلیتِ تازه
    abstract val readAloudTitle: String
    abstract val readAloudDescription: String
    abstract val readAloudStart: String
    abstract val readAloudStop: String
    abstract val readAloudUnavailable: String
    abstract val readerModeTitle: String
    abstract val readerModeDescription: String
    abstract val readerModeOn: String
    abstract val readerModeOff: String
    abstract val readerModeUnavailable: String
    abstract val sitePermissionsTitle: String
    abstract val sitePermissionsDescription: String
    abstract val permissionJavascript: String
    abstract val permissionAutoplay: String
    abstract val permissionGeolocation: String
    abstract val permissionCamera: String
    abstract val permissionMicrophone: String
    abstract val permissionNotifications: String
    abstract val permissionAllowed: String
    abstract val permissionBlocked: String
    abstract val permissionAsk: String
    abstract val sitePermissionsSection: String
    abstract val sitePermissionsReset: String
    abstract val sitePermissionsResetDone: String
    abstract val sitePermissionsSaved: String
    abstract val menuReadAloud: String
    abstract val menuReaderMode: String
    abstract val menuSitePermissions: String
    abstract val menuSmartRename: String

    abstract val starRepoBoldLine: String
    abstract val starRepoHeadline: String
    abstract val starRepoSubline: String
    abstract val starRepoButton: String
    abstract val starRepoLater: String
    abstract val starRepoNever: String
    abstract val starRepoTelegram: String
    abstract val starRepoWeb: String
    abstract val starRepoThanks: String
    abstract val starRepoBadgeFree: String
    abstract val starRepoBadgePrivate: String
    abstract val starRepoBadgeFast: String
    abstract val starRepoBadgeBilingual: String

    // ------------------------------------------- برچسب های شمارشی
    fun themeModeLabel(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> themeSystem
        ThemeMode.LIGHT -> themeLight
        ThemeMode.DARK -> themeDark
    }

    fun chromeBarStyleLabel(style: ChromeBarStyle): String = when (style) {
        ChromeBarStyle.SOLID -> chromeSolid
        ChromeBarStyle.GLASS -> chromeGlass
        ChromeBarStyle.FLOATING -> chromeFloating
    }

    fun backBehaviorLabel(behavior: BackButtonBehavior): String = when (behavior) {
        BackButtonBehavior.SMART -> backSmart
        BackButtonBehavior.PAGE_THEN_HOME -> backPageThenHome
        BackButtonBehavior.HOME -> backHome
        BackButtonBehavior.PAGE_THEN_EXIT -> backPageThenExit
        BackButtonBehavior.ASK -> backAsk
    }

    fun languageLabel(language: AppLanguage): String = when (language) {
        AppLanguage.SYSTEM -> languageSystem
        AppLanguage.PERSIAN -> languagePersian
        AppLanguage.ENGLISH -> languageEnglish
    }

    fun dnsModeLabel(mode: DnsMode): String = when (mode) {
        DnsMode.SYSTEM -> dnsSystem
        DnsMode.CLOUDFLARE -> "Cloudflare (1.1.1.1)"
        DnsMode.GOOGLE -> "Google (8.8.8.8)"
        DnsMode.QUAD9 -> "Quad9 (9.9.9.9)"
        DnsMode.CUSTOM -> dnsCustom
    }

    fun proxyTypeLabel(type: ProxyType): String = when (type) {
        ProxyType.NONE -> proxyNone
        ProxyType.HTTP -> "HTTP"
        ProxyType.SOCKS5 -> "SOCKS5"
    }

    fun healthStatusLabel(status: SessionHealthStatus): String = when (status) {
        SessionHealthStatus.HEALTHY -> statusHealthy
        SessionHealthStatus.PROTECTED_ACTIVE -> statusProtected
        SessionHealthStatus.HEALING_IN_PROGRESS -> statusHealing
        SessionHealthStatus.RECOVERED_FROM_VAULT -> statusRecovered
        SessionHealthStatus.SERVER_REVOKED_FALLBACK,
        SessionHealthStatus.REAUTH_REQUIRED,
        -> statusNeedsSignIn
        SessionHealthStatus.RESTORE_UNVERIFIED -> statusRestoreUnverified
        SessionHealthStatus.RESTORE_FAILED -> statusRestoreFailed
        SessionHealthStatus.DISABLED -> statusProtectionOff
    }

    fun healthReasonLabel(reason: HealthReason): String = when (reason) {
        HealthReason.SITE_PROTECTION_OFF -> reasonProtectionOff
        HealthReason.HEALTHY -> reasonHealthy
        HealthReason.UNAUTHORIZED -> reasonUnauthorized
        HealthReason.LOGIN_REDIRECT -> reasonLoginRedirect
        HealthReason.AUTH_COOKIE_MISSING -> reasonCookieMissing
        HealthReason.STORAGE_CLEARED -> reasonStorageCleared
        HealthReason.HEALED -> reasonHealed
        HealthReason.SERVER_REVOKED -> reasonServerRevoked
        HealthReason.RESTORE_ATTEMPTED -> reasonRestoreAttempted
        HealthReason.RESTORE_UNAVAILABLE -> reasonRestoreUnavailable
        HealthReason.UNKNOWN -> reasonUnknown
    }

    /** متنِ انگلیسی/فارسیِ امن برای خواندن کدِ ذخیره شده در گزارش رویدادها. */
    fun healthReasonLabelOrNull(code: String): HealthReason? =
        runCatching { HealthReason.valueOf(code) }.getOrNull()

    // ------------------------------------------------- برچسب های داخلی
    protected abstract val dnsSystem: String
    protected abstract val dnsCustom: String
    protected abstract val proxyNone: String
    protected abstract val statusHealthy: String
    protected abstract val statusProtected: String
    protected abstract val statusHealing: String
    protected abstract val statusRecovered: String
    protected abstract val statusNeedsSignIn: String
    protected abstract val statusProtectionOff: String
    protected abstract val reasonProtectionOff: String
    protected abstract val reasonHealthy: String
    protected abstract val reasonUnauthorized: String
    protected abstract val reasonLoginRedirect: String
    protected abstract val reasonCookieMissing: String
    protected abstract val reasonStorageCleared: String
    protected abstract val reasonHealed: String
    protected abstract val reasonServerRevoked: String
    protected abstract val reasonUnknown: String
    protected abstract val statusRestoreUnverified: String
    protected abstract val statusRestoreFailed: String
    protected abstract val reasonRestoreAttempted: String
    protected abstract val reasonRestoreUnavailable: String

    // ------------------------------------------------- دسترسی‌ها و پاپ‌آپ
    abstract val permissionPopups: String
    abstract val permissionPopupTitle: String
    abstract val permissionPopupBody: String
    abstract val permissionAllow: String
    abstract val permissionBlock: String
    abstract val permissionAskTitle: String
    abstract fun permissionRequestBody(domain: String, permission: String): String
    abstract val permissionAndroidRequired: String
    abstract val permissionDenied: String

    // ------------------------------------------------------- پروکسی
    abstract val proxyHostInvalid: String
    abstract val proxyPortInvalid: String
    abstract val proxyRestartRequired: String

    // ----------------------------------------------- پیشنهادهای نوار آدرس
    abstract val suggestionHistory: String
    abstract val suggestionBookmark: String
    abstract val suggestionMostVisited: String
    abstract val suggestionOpenTabs: String
    abstract val suggestionsTitle: String
    abstract val suggestionsEmpty: String

    // ------------------------------------------- کتابخانه: نشان‌ها/تاریخچه
    abstract val libraryTitle: String
    abstract val libraryBookmarksTab: String
    abstract val libraryHistoryTab: String
    abstract val librarySearchHint: String
    abstract val libraryAllBookmarks: String
    abstract val libraryAddBookmark: String
    abstract val libraryEditBookmark: String
    abstract val librarySave: String
    abstract val libraryBookmarkTitle: String
    abstract val libraryBookmarkUrl: String
    abstract val libraryBookmarkFolder: String
    abstract val libraryNewFolder: String
    abstract val libraryFolderName: String
    abstract val libraryMoveToFolder: String
    abstract val libraryDeleteBookmark: String
    abstract val libraryDeleteBookmarkBody: String
    abstract val libraryOpenInNewTab: String
    abstract val libraryDeleteHistoryItem: String
    abstract val libraryDeleteHistoryBody: String
    abstract val libraryDeleteSiteHistory: String
    abstract val libraryClearHistoryTitle: String
    abstract val libraryClearHistoryBody: String
    abstract val libraryToday: String
    abstract val libraryYesterday: String
    abstract val libraryThisWeek: String
    abstract val libraryThisMonth: String
    abstract val libraryOlder: String
    abstract val librarySort: String
    abstract val libraryMoreOptions: String
    abstract val librarySortDate: String
    abstract val librarySortVisits: String
    abstract val librarySortTitle: String
    abstract val libraryEmptyBookmarksSearch: String
    abstract val libraryEmptyHistorySearch: String
    abstract val libraryBookmarkSaved: String
    abstract val libraryBookmarkUpdated: String
    abstract val libraryBookmarkDeleted: String
    abstract val libraryFolderCreated: String
    abstract val libraryHistoryDeleted: String
    abstract val librarySiteHistoryDeleted: String
    abstract val libraryInvalidBookmark: String
    abstract val libraryAllFolders: String
    abstract val libraryFolderBookmarks: String
    abstract val libraryClearOlder: String
    abstract val libraryClearOlderBody: String
    abstract val libraryClearOlder7: String
    abstract val libraryClearOlder30: String

    // ------------------------------------ داشبورد «وارد بمان» و ابزارها
    abstract val vaultAdvancedTools: String
    abstract val vaultAdvancedToolsBody: String
    abstract val vaultHideAdvanced: String
    abstract val vaultSignedInTitle: String
    abstract val vaultSignedInBody: String

    companion object {
        fun forLanguage(
            language: AppLanguage,
            systemLanguageTag: String = Locale.getDefault().toLanguageTag(),
        ): MarbleStrings = when (language.resolve(systemLanguageTag)) {
            AppLanguage.PERSIAN -> PersianStrings
            else -> EnglishStrings
        }

        fun forResolvedLanguage(language: AppLanguage): MarbleStrings = when (language) {
            AppLanguage.PERSIAN -> PersianStrings
            else -> EnglishStrings
        }
    }
}

private object PersianStrings : MarbleStrings() {
    override val accountsTitle = "حساب‌های این سایت"
    override val accountsDescription = "هر حساب در فضای جداگانه ذخیره می‌شود. وارد حساب جدید شوید."
    override val accountCurrent = "حساب فعال"
    override val accountCreate = "افزودن حساب دیگر"
    override val accountName = "نام حساب"
    override val accountOpen = "باز کردن حساب"
    override val credentialVaultTitle = "گذرواژه‌ها و حساب‌ها"
    override val credentialVaultDescription = "فقط روی این دستگاه و به‌صورت رمزنگاری‌شده ذخیره می‌شوند."
    override val credentialLockRequired = "برای دیدن گذرواژه‌ها با اثر انگشت یا قفل گوشی تأیید کنید."
    override val credentialLockUnavailable = "ابتدا قفل صفحه یا اثر انگشت دستگاه را فعال کنید."
    override val credentialAdd = "افزودن اطلاعات ورود"
    override val credentialEdit = "ویرایش اطلاعات ورود"
    override val credentialSite = "آدرس سایت (https://...)"
    override val credentialOriginReview = "برای پرکردن امن، آدرس دقیق HTTPS (از جمله www و پورت) را بررسی و دوباره ذخیره کن."
    override val credentialUsername = "نام کاربری"
    override val credentialPassword = "گذرواژه"
    override val credentialNotes = "یادداشت"
    override val credentialSave = "ذخیره"
    override val credentialDelete = "حذف اطلاعات ورود؟"
    override val credentialDeleteBody = "این اطلاعات فقط از مخزن گذرواژه حذف می‌شود."
    override val credentialInvalid = "آدرس معتبر و نام کاربری یا گذرواژه را وارد کنید."
    override val credentialFailed = "ذخیره اطلاعات ممکن نشد. دوباره تلاش کنید."
    override val credentialEmpty = "هنوز اطلاعات ورودی ذخیره نشده است."
    override val credentialUnreadable = "برخی اطلاعات خوانده نشدند یا مخزن در دسترس نیست؛ هیچ موردی خودکار حذف نشده است."
    override val credentialGenerate = "ساخت گذرواژه قوی"
    override val credentialShow = "نمایش گذرواژه"
    override val credentialHide = "پنهان کردن گذرواژه"
    override val credentialCopyUser = "کپی نام کاربری"
    override val credentialCopyPassword = "کپی گذرواژه"
    override val credentialCopied = "کپی شد؛ پس از ۳۰ ثانیه پاک می‌شود."
    override val contextTitle = "گزینه‌های صفحه"
    override val contextOpenTab = "باز کردن پیوند در تب تازه"
    override val contextCopyLink = "کپی پیوند"
    override val contextShareLink = "اشتراک پیوند"
    override val contextSaveImage = "دانلود تصویر"
    override val contextCopyImage = "کپی آدرس تصویر"
    override val contextShareText = "اشتراک متن"
    override val contextSavePdf = "ذخیره صفحه به صورت PDF"
    override val contextCopyText = "کپی متن"
    override val menuSharePage = "به اشتراک گذاشتن صفحه"
    override val pdfSaved = "صفحه به صورت PDF ذخیره شد."
    override val pdfFailed = "ذخیره صفحه ممکن نشد."
    override val credentialUnlock = "باز کردن مخزن"
    override val accountSessionWarning = "اگر سایت نشست را در سرور باطل کند، ورود دوباره لازم است."

    override val appName = "MarbleBr"
    override val back = "بازگشت"
    override val cancel = "بی خیال"
    override val ok = "باشه"
    override val later = "بعداً"
    override val close = "بستن"
    override val dismiss = "بستن پیام"

    override val languageTitle = "زبان برنامه"
    override val languageDescription = "با «خودکار»، MarbleBr هم زبان گوشی ات می شود. اگر خواستی، فارسی یا انگلیسی را دستی انتخاب کن."
    override val languageSystem = "خودکار (مثل گوشی)"
    override val languagePersian = "فارسی"
    override val languageEnglish = "English"
    override val appearanceTitle = "ظاهر"
    override val themeModeTitle = "روشن یا تاریک"
    override val themeSystem = "مثل گوشی"
    override val themeLight = "روشن"
    override val themeDark = "تاریک"
    override val dynamicColorTitle = "رنگ های خودِ گوشی"
    override val dynamicColorDescription = "روی اندروید 12 و بالاتر، رنگ برنامه با تم گوشی ات هماهنگ می شود."

    override val addressPlaceholder = "آدرس سایت را بنویس یا سرچ کن"
    override val copyLink = "کپی پیوند"
    override val shareLink = "اشتراک گذاری پیوند"
    override val browserMenu = "گزینه های بیشتر"
    override val tabsLabel = "تب ها"
    override val protectionLabel = "محافظت"
    override val bookmarksLabel = "نشان ها"
    override val settingsLabel = "تنظیمات"
    override val bookmarkToggle = "نشان کردن این صفحه"
    override val reload = "بارگذاری دوباره"
    override val goBack = "صفحه قبل"
    override val goForward = "صفحه بعد"
    override val protectionOnChip = "مراقب این سایت هستیم"
    override val protectionOffChip = "مراقبت از این سایت خاموش است"
    override val privateTabChip = "تب خصوصی: چیزی ذخیره نمی شود"
    override val menuSaveState = "همین حالا ذخیره کن"
    override val menuSaveStateHint = "یک نسخه امن از ورودت می سازیم"
    override val menuTimeMachine = "برگرد به نسخه قبلی"
    override val menuTimeMachineHint = "اگر از حسابی پریدی بیرون، از اینجا برگردان"
    override val menuFindInPage = "جستجو در این صفحه"
    override val menuDesktopModeOn = "نسخه دسکتاپ را نشان بده"
    override val menuDesktopModeOff = "نسخه موبایل را نشان بده"
    override val menuSaveShortcut = "میانبر روی صفحه اصلی"
    override val menuFreeSpace = "خالی کردن حافظه موقت"
    override val menuFreeSpaceHint = "بدون اینکه از حسابی خارج شوی"
    override val findInPagePlaceholder = "در این صفحه بگرد…"
    override val findPrevious = "مورد قبلی"
    override val findNext = "مورد بعدی"
    override val findClose = "بستن جستجو"
    override val reloginTitle = "برای ادامه دوباره وارد شو"
    override val reloginConfirm = "ادامه در سایت"
    override val reloginDismiss = "نه، خودم وارد می شوم"
    override val readyTitle = "آماده ی مراقبت هستیم"
    override val readyBody = "کافی است یک آدرس بزنی. ما پشت صحنه مواظبیم که از حسابت بیرون نیفتی."
    override val quickSaveState = "ذخیره وضعیت"
    override val quickEarlierCopies = "نسخه های قبلی"
    override val newTab = "تب جدید"
    override val newPrivateTab = "تب خصوصی"
    override val closeTab = "بستن تب"
    override val newTabTitle = "تب جدید"
    override val privateTabTitle = "تب خصوصی"
    override val addressSelectionTitle = "انتخاب متن نوار آدرس"
    override val addressSelectionDescription = "هنگام لمس نوار آدرس، نحوهٔ انتخاب متن را تعیین کنید."
    override val addressSelectAll = "همهٔ آدرس"
    override val addressSelectPart = "بخش لمس شده"
    override val addressCursor = "فقط نشانگر"
    override val googleNewTab = "باز کردن گوگل در تب جدید"
    override val googleNewTabDescription = "اگر خاموش باشد، تب جدید صفحهٔ خانه را نشان می دهد و هیچ سایتی بارگیری نمی شود."
    override val goToAddress = "برو به آدرس"
    override val navigationFailed = "آدرس باز نشد. دوباره تلاش کنید."
    override val stopLoading = "توقف بارگیری"
    override val duplicateTab = "ایجاد نسخهٔ دوم تب"
    override val reopenTab = "بازگشایی تب بسته شده"
    override val closeAllTabs = "بستن همهٔ تب ها"
    override val confirmCloseAllTabs = "همهٔ تب های عادی و خصوصی بسته شوند؟"
    override val searchTabs = "جستجو در تب ها"
    override val noTabs = "هیچ تب بازی نیست."
    override val noMatchingTabs = "تبی با این عبارت پیدا نشد."
    override val clearHistory = "پاک کردن تاریخچه"
    override val emptyBookmarks = "هنوز چیزی نشان نکرده ای. با آیکن نشان بالای صفحه، هر سایتی را ذخیره کن."
    override val emptyHistory = "اینجا خالی است؛ سایت هایی که باز کنی اینجا می آیند."
    override val emptyDownloads = "هنوز فایلی دانلود نکرده ای."
    override val downloadNotificationText = "در حال دانلود با MarbleBr"

    override val bannerBookmarkAdded = "به نشان ها اضافه شد"
    override val bannerBookmarkRemoved = "از نشان ها حذف شد"
    override val bannerShortcutAdded = "میانبر ساخته شد؛ از صفحه اصلی بازش کن"
    override val bannerShortcutCreated = "فضای جداگانه ی این سایت آماده شد"
    override val bannerSnapshotSaved = "یک نسخه امن از ورودت ذخیره شد"
    override val bannerCacheCleared = "حافظه موقت خالی شد و همچنان وارد حساب هایت هستی"
    override val bannerHistoryCleared = "تاریخچه پاک شد"
    override val bannerDownloadStarted = "دانلود شروع شد"
    override val downloadFailed = "دانلود ناموفق بود"
    override val downloadInProgress = "در حال دانلود"
    override val snapshotUnavailable = "برای این صفحه دادهٔ نشست قابل ذخیره در دسترس نیست."
    override val bannerDesktopOn = "حالا نسخه دسکتاپ را می بینی"
    override val bannerDesktopOff = "حالا نسخه موبایل را می بینی"

    override val settingsTitle = "تنظیمات"
    override val settingsIntro = "هر چیزی را که لازم داری ساده و کوتاه اینجا تنظیم کن."
    override val protectionSectionTitle = "اینکه وارد بمانی"
    override val protectionSectionBody = "این گزینه ها تعیین می کنند MarbleBr چطور کمکت کند از حساب ها بیرون نیفتی."
    override val globalNeverSignOutTitle = "مراقبت از سایت های جدید"
    override val globalNeverSignOutDescription = "برای هر سایت تازه، محافظت از ورود به طور خودکار روشن باشد."
    override val biometricTitle = "قفل اثر انگشت"
    override val biometricDescription = "برای دیدن نسخه های ذخیره شده، اثر انگشت یا رمز گوشی لازم باشد."
    override val keepAliveTitle = "ثبت دوره‌ای نسخهٔ نشست"
    override val keepAliveDescription = "از تب‌های بازِ حساب‌های فعال نسخهٔ محلی می‌گیرد. زمان اجرا به زمان‌بندی اندروید بستگی دارد."
    override val credentialTitle = "یادآوری ورود دوباره"
    override val credentialDescription = "وقتی سایت درخواست ورود دارد، در تب فعال یادآوری نشان بده."
    override val adBlockTitle = "بستن تبلیغ و ردیاب"
    override val adBlockDescription = "صفحه ها سبک تر و سریع تر باز می شوند و کمتر ردیابی می شوی."
    override val freeSpaceTitle = "خالی کردن حافظه موقت"
    override val freeSpaceDescription = "فقط فایل های اضافی پاک می شوند؛ هیچ حسابی خارج نمی شود."
    override val freeSpaceButton = "خالی کن"
    override val freeSpaceDone = "حافظه موقت خالی شد"
    override val networkTitle = "شبکه (برای کاربران حرفه ای)"
    override val networkBody = "اگر DNS امن یا پروکسی خاصی می خواهی، از اینجا تنظیم کن. برای بیشتر آدم ها لازم نیست."
    override val dnsLabel = "DNS امن"
    override val dnsCustomHint = "آدرس سرور (با https شروع شود)"
    override val proxyLabel = "پروکسی"
    override val proxyHostHint = "آدرس یا IP پروکسی"
    override val proxyPortHint = "پورت"
    override val saveNetwork = "ذخیره تنظیمات شبکه"
    override val networkSaved = "تنظیمات شبکه ذخیره شد"
    override val aboutTitle = "درباره"

    override val vaultTitle = "مراقبت از حساب ها"
    override val vaultIntro = "حساب‌هایت اینجا امن‌اند."
    override val vaultSecurityTitle = "همه چیز روی همین گوشی می ماند"
    override val vaultSecurityBody = "همه‌چیز با کلید گوشی رمز می‌شود و جایی نمی‌رود."
    override val unlockVault = "باز کردن با اثر انگشت"
    override val biometricPromptTitle = "قفل مراقبت از حساب ها"
    override val biometricPromptSubtitle = "برای دیدن نسخه های ذخیره شده، هویتت را تأیید کن"
    override val biometricCancelled = "تأیید هویت لغو شد"
    override val reloginUnavailable = "برای این سایت ورود ذخیره شده ای پیدا نشد"
    override val reloginUnsupportedType = "این نوع ورود پشتیبانی نمی شود"
    override val reloginManualHint = "این نوع ورود را نمی‌توان خودکار انجام داد؛ صفحه باز می‌ماند تا با روشِ خودش وارد شوی و ورودت در مخزن ذخیره شود."
    override val reloginPromptSubtitle = "سایت نشستت را بست؛ با اثر انگشت دوباره وارد شو"
    override val sitesSectionTitle = "سایت ها"
    override val sitesSectionBody = "هر سایت فضای جدا و امن خودش را دارد."
    override val protectThisSite = "مراقبت از این سایت"
    override val signOutNow = "کامل خارج شو"
    override val signOutConfirmTitle = "از این سایت خارج می شوی؟"
    override val signOutConfirmBody = "ورود فعلی و همه ی نسخه های ذخیره شده ی این سایت پاک می شوند. بقیه سایت ها دست نخورده می مانند."
    override val signOutConfirmAction = "بله، خارج شو"
    override val timeMachineTitle = "برگشت به عقب"
    override val timeMachineBody = "به هر زمان که خواستی برگرد."
    override val emptySnapshots = "هنوز نسخه‌ای نساخته‌ایم؛ با باز کردن سایت خودش ساخته می‌شود."
    override val emptySites = "هنوز سایتی اضافه نشده. کافی است یک آدرس باز کنی."
    override val backgroundSnapshotLabel = "نسخه پس زمینه"
    override val restore = "برگردان"
    override val healthLogTitle = "چه اتفاقی افتاد؟"
    override val healthLogBody = "هر وقت چیزی برای ورودت پیش بیاید، اینجا با زبان ساده ثبت می شود."
    override val feedbackRestoreFailed = "نتوانستیم این نسخه را بخوانیم"
    override val gotIt = "باشه"

    override val vaultHeroTitle = "حساب‌هایت در امان‌اند"
    override val vaultHeroBody = "ورودها فقط روی همین گوشی ذخیره می‌شوند."
    override val vaultStatSites = "سایت"
    override val vaultStatProtected = "مراقبت فعال"
    override val vaultStatSnapshots = "نسخه ذخیره‌شده"
    override val vaultLastSaved = "آخرین ذخیره"
    override val vaultNever = "هنوز چیزی ذخیره نشده"
    override val vaultSearchHint = "جستجوی سایت…"
    override val vaultFilterAll = "همه"
    override val vaultFilterProtected = "مراقبت‌شده"
    override val vaultFilterUnprotected = "بدون مراقبت"
    override val vaultFilterPinned = "میانبری"
    override val vaultNoMatch = "سایتی با این جستجو پیدا نشد."
    override val vaultProtectionOffBanner = "محافظت از سایت‌های تازه خاموش است."
    override val vaultSectionSites = "سایت‌های تو"
    override val vaultSectionTimeline = "تاریخچهٔ نسخه‌ها"
    override val vaultSectionActivity = "گزارش محافظت"
    override val vaultSiteProtected = "مراقبت روشن"
    override val vaultSiteUnprotected = "مراقبت خاموش"
    override val vaultSitePinned = "میانبر صفحهٔ خانه"
    override val vaultSiteDetailsTitle = "تنظیمات این سایت"
    override val vaultProtectSwitchTitle = "مراقبت از ورود"
    override val vaultProtectSwitchBody = "داده‌های محلی تب محافظت می‌شوند؛ اعتبار ورود را خود سایت تعیین می‌کند."
    override val vaultKeepAliveSwitchTitle = "ثبت دوره‌ای نسخهٔ محلی"
    override val vaultKeepAliveSwitchBody = "از تب باز این حساب نسخهٔ محلی می‌گیرد. اندروید ممکن است ثبت را به تأخیر بیندازد."
    override val vaultCookieDaysTitle = "مدت نگهداری کوکی موقت"
    override val vaultCookieDaysBody = "کوکی‌های موقتِ مشاهده‌شده تا این مدت نگهداری می‌شوند؛ انقضا و اعتبار سرور تغییر نمی‌کند."
    override val vaultSnapshotsOfSite = "نسخه‌های همین سایت"
    override val vaultSaveNow = "همین حالا ذخیره کن"
    override val vaultDelete = "حذف"
    override val vaultDeleteConfirmTitle = "این نسخه حذف شود؟"
    override val vaultDeleteConfirmBody = "این نسخهٔ ذخیره‌شده برای همیشه پاک می‌شود و برگشت‌پذیر نیست."
    override val vaultClose = "بستن"
    override val vaultEmptyActivity = "هنوز اتفاقی نیفتاده."
    override val vaultFilterEmpty = "بعد از تغییر وضعیت، سایت‌ها همین‌جا دسته‌بندی می‌شوند."
    override val vaultHealthHealthy = "محافظت فعال است"
    override val vaultProtectAll = "مراقبت از همه"
    override val vaultUnprotectAll = "خاموش‌کردن مراقبت همه"
    override val vaultProtectAllOnDone = "مراقبت از همهٔ سایت‌ها روشن شد"
    override val vaultProtectAllOffDone = "مراقبت از همهٔ سایت‌ها خاموش شد"
    override val vaultRestoreNeedsOpenTab = "برای بازیابی، صفحهٔ همان سایت را در حساب مربوط باز نگه دار."
    override val vaultRestoreSuperseded = "نسخهٔ جدیدتری برای این حساب ثبت شده است. از تازه‌ترین نسخه استفاده کن یا دوباره وارد سایت شو."
    override val vaultRestoreConfirmTitle = "اعمال نسخهٔ محلی؟"
    override val vaultRestoreConfirmBody =
        "حتی تازه‌ترین نسخه ممکن است شامل توکن قدیمی باشد و باعث خروج یا ابطال نشست شود. " +
            "اعتبار ورود بررسی نمی‌شود؛ ورود مجدد از خود سایت امن‌تر است. با آگاهی از این خطر ادامه می‌دهی؟"
    override val vaultQuickActionsTitle = "کارهای سریع"
    override val vaultJumpToSite = "رفتن به سایت"
    override val feedbackRestoreEmpty = "بازیابی انجام نشد؛ صفحه و دسترسی شبکه را بررسی کن."
    override val quickPageSection = "این صفحه"
    override val quickLibrarySection = "کتابخانه"
    override val quickToolsSection = "محافظت و ابزار"
    override fun vaultSnapshotCount(count: Int) = "$count نسخه"
    override fun vaultLastSavedAgo(label: String) = "آخرین ذخیره: $label"
    override fun vaultKeepAliveEveryMinutes(minutes: Int) = "هر $minutes دقیقه"
    override fun vaultDeleteSnapshotDone(domain: String) = "نسخهٔ $domain حذف شد"

    override fun tabsCount(count: Int) = "$count تب باز"
    override fun adsBlocked(count: Int) = "$count تبلیغ و ردیاب بسته شد"
    override fun downloadsMenu(count: Int) = "دانلودها ($count)"
    override fun openTabsTitle(count: Int) = "تب های باز ($count)"
    override fun bookmarksCount(count: Int) = "نشان ها ($count)"
    override fun historyCount(count: Int) = "تاریخچه ($count)"
    override fun savedPath(path: String) = "ذخیره شده در: $path"
    override fun reloginBody(domain: String) =
        "سایت $domain درخواست ورود دارد. می‌توانی ورود را از صفحهٔ سایت ادامه بدهی."
    override fun adBlockChip(count: Int) = "تبلیغ بسته شده: $count"
    override fun sitesCount(count: Int) = "سایت ها ($count)"
    override fun keepAliveEvery(minutes: Int) = "بازهٔ ثبت نسخه ($minutes دقیقه)"
    override fun keepSignedInFor(days: Int) = "ورود را $days روز نگه دار"
    override fun signOutDialogTitle(domain: String) = "از $domain خارج می شوی؟"
    override fun signOutDialogBody(domain: String) =
        "ورود فعلی و نسخه های ذخیره شده ی $domain پاک می شوند. بقیه سایت ها دست نخورده می مانند."
    override fun snapshotLine(date: String, cookies: Int, storageKeys: Int) =
        "ذخیره شده در $date · $cookies کوکی · $storageKeys کلید حافظه"
    override fun feedbackProtection(domain: String, enabled: Boolean) =
        if (enabled) "مراقبت از $domain روشن شد" else "مراقبت از $domain خاموش شد"
    override fun feedbackCookieDays(domain: String, days: Int) = "ورود به $domain حالا $days روز می ماند"
    override fun feedbackKeepAliveOn(domain: String, minutes: Int) =
        "بازهٔ ثبت نسخهٔ $domain روی $minutes دقیقه تنظیم شد"
    override fun feedbackKeepAliveOff(domain: String) = "ثبت دوره‌ای نسخهٔ $domain خاموش شد"
    override fun feedbackRestored(label: String, cookies: Int) = "بازیابی «$label» انجام شد ($cookies کوکی به مرورگر ارائه شد)؛ ورود را بررسی کن."
    override fun feedbackSignedOut(domain: String) = "تب‌های $domain بسته و پاک‌سازی این حساب درخواست شد."
    override fun aboutBody(version: String) =
        "MarbleBr نسخه $version — ساخته شده که یک بار وارد شوی و بمانی. بدون ردیاب، بدون تبلیغ."
    override fun reloginAutoTitle(domain: String) = "ورود دوباره به $domain"
    override fun autoSnapshotLabel(domain: String) = "نسخه خودکار — $domain"
    override fun manualSnapshotLabel(domain: String) = "نسخه دستی — $domain"
    override fun pwaShortcutLongLabel(title: String) = "$title (بدون خروج خودکار)"

    override val homeTitle = "صفحه ی خانه"
    override val homeIntro = "ابتدا را از اینجا آغاز کنید"
    override val homeSearchHint = "جستجو یا آدرس سایت را بنویسید…"
    override val homeRecentTitle = "بازدیدهای اخیر"
    override val homeEmptyRecent = "هنوز بازدیدی ندارید"
    override val homeShortcutsTitle = "میانبرهای شما"
    override val homeEmptyShortcuts = "هنوز میانبری ندارید. سایت های مورد علاقه را از مرورگر ذخیره کنید."

    override val rateTitle = "به MarbleBr استار بدهید"
    override val rateMessage =
        "اگر MarbleBr به شما مفید بوده است، لطفاً به ما استار بدهید تا بتوانیم برایتان بهتر شویم."
    override val rateNow = "بله، با خوشحالی"
    override val rateLater = "بعداً"
    override val rateDontAskAgain = "نخواهم داد، دوباره نشان نده"

    override val homeScreenTitle = "تنظیمات صفحه ی خانه"
    override val searchEngineTitle = "موتور جستجو"
    override val searchEngineDescription =
        "موتور جستجوی پیش فرض برای نوار آدرس و صفحه ی خانه را انتخاب کنید."
    override val homeShortcutPositionTitle = "دکمهٔ تبِ تازه در صفحهٔ خانه"
    override val homeShortcutPositionDescription = "دکمهٔ تبِ تازه حالا داخل کاشی‌های میان‌بر است و دیگر جداگانه روی صفحه شناور نمی‌شود."
    override val shortcutPositionNone = "پنهان باشد"
    override val shortcutPositionFloating = "شناور (قدیمی)"
    override val shortcutPositionBuiltIn = "همراه میان‌برها (پیشنهادی)"

    override val homeSeeAll = "مشاهده همه »"

    override val settingsTabAppearance = "ظاهر"
    override val settingsTabBrowser = "مرورگر"
    override val settingsTabProtection = "محافظت"
    override val settingsTabNetwork = "شبکه"
    override val settingsTabAbout = "درباره"
    override val colorPaletteTitle = "پالت رنگی"
    override val colorPaletteDescription = "یکی از تم‌های آمادهٔ MarbleBr را انتخاب کن؛ همهٔ صفحه‌ها هماهنگ با همان رنگ می‌شوند."
    override fun colorPaletteLabel(palette: ColorPalette) = when (palette) {
        ColorPalette.DYNAMIC -> "رنگ‌های گوشی"
        ColorPalette.OCEAN -> "اقیانوس"
        ColorPalette.EMERALD -> "زمرد"
        ColorPalette.SUNSET -> "غروب"
        ColorPalette.ROYAL -> "شاهی"
        ColorPalette.CANDY -> "آب‌نبات"
        ColorPalette.GRAPHITE -> "گرافیت"
        ColorPalette.SAND -> "شنی"
    }
    override val paletteDynamicHint = "روی اندروید ۱۲ و بالاتر، رنگ‌ها را از تصویر پس‌زمینهٔ گوشی می‌گیریم."
    override val themePreviewTitle = "پیش‌نمایش زنده"
    override val themePreviewBody = "همین حالا رنگ دکمه‌ها و کارت‌ها را با تمِ تازه ببین؛ لازم نیست جایی بروی."

    override val chromeBarStyleTitle = "شکل نوارهای بالا و پایین"
    override val chromeBarStyleDescription = "«شناور» یعنی نوارها کاملاً شیشه ای و بی پس زمینه اند و صفحه از پشتشان دیده می شود."
    override val chromeSolid = "ساده و مات"
    override val chromeGlass = "شیشه ای مات"
    override val chromeFloating = "شیشه ای شناور"
    override val backBehaviorTitle = "دکمهٔ بازگشت"
    override val backBehaviorDescription = "انتخاب کن وقتی دکمهٔ بازگشت گوشی را می زنی چه اتفاقی بیفتد."
    override val backSmart = "هوشمند (پیشنهادی)"
    override val backPageThenHome = "اول صفحهٔ قبلی، بعد خانه"
    override val backHome = "همیشه به خانه"
    override val backPageThenExit = "اول صفحهٔ قبلی، بعد خروج"
    override val backAsk = "هر بار از من بپرس"
    override val backAskTitle = "برگرد به کجا؟"
    override val backAskBody = "دکمهٔ بازگشت را زدی؛ حالا خودت انتخاب کن."
    override val backAskToPage = "به صفحهٔ قبلی همین سایت"
    override val backAskToHome = "به صفحهٔ خانه"
    override val backAskExit = "خروج از برنامه"
    override val tabsBehaviorTitle = "رفتار تب ها"
    override val askBeforeClosingTabs = "تأیید قبل از بستن تب"
    override val askBeforeClosingTabsDescription = "اگر یک تب باز باشد و بخواهی ببندیش، اول از تو می پرسیم."
    override val closeTabConfirmTitle = "این تب بسته شود؟"
    override fun closeTabConfirmBody(title: String) = "تب «$title» بسته می شود؛ اگر خواستی با «بازگشایی تب بسته شده» برمی گردد."

    override val quickActionsTitle = "میانبرهای سریع"
    override val quickCopyLink = "کپی لینک"
    override val quickShare = "اشتراک"
    override val quickScreenshot = "عکس از صفحه"
    override val quickFindInPage = "جستجو در صفحه"
    override val quickDesktop = "دسکتاپ"
    override val quickPrivate = "تب خصوصی"
    override val quickSaveLogin = "ذخیره ورود"
    override val quickTimeMachine = "برگشت به عقب"
    override val quickDownloads = "دانلودها"
    override val quickClearCache = "خالی کردن کش"
    override val quickAddShortcut = "میانبر خانه"
    override val quickRefresh = "نوسازی"
    override val quickHome = "خانه"
    override val quickTabs = "تب ها"
    override val quickSettings = "تنظیمات"
    override val quickBookmarks = "نشان ها"
    override val quickSignOut = "خروج کامل"
    override val quickNewTab = "تب تازه"
    override fun quickSiteSignedInFor(days: Int, minutes: Int) =
        "این سایت $days روز وارد می ماند · هر $minutes دقیقه بی صدا زنده می شود"

    override val readAloudTitle = "بلندخوانی صفحه"
    override val readAloudDescription = "متن صفحه را با صدای گوشی برایت می‌خوانیم."
    override val readAloudStart = "بخوان"
    override val readAloudStop = "ساکت شو"
    override val readAloudUnavailable = "صفحهٔ فعلی چیزی برای خواندن ندارد."
    override val readerModeTitle = "حالت مطالعه"
    override val readerModeDescription = "تبلیغ‌ها و منوها را کنار می‌زنیم تا فقط متن بماند."
    override val readerModeOn = "حالت مطالعه روشن شد"
    override val readerModeOff = "حالت مطالعه خاموش شد"
    override val readerModeUnavailable = "این صفحه حالت مطالعه ندارد."
    override val sitePermissionsTitle = "دسترسی‌های این سایت"
    override val sitePermissionsDescription = "هر سایت چه چیزی از گوشی‌ات بخواهد، تو تصمیم می‌گیری."
    override val permissionJavascript = "اجرای جاوااسکریپت"
    override val permissionAutoplay = "پخش خودکار ویدیو"
    override val permissionGeolocation = "موقعیت مکانی"
    override val permissionCamera = "دوربین"
    override val permissionMicrophone = "میکروفون"
    override val permissionNotifications = "اعلان‌ها"
    override val permissionAllowed = "اجازه"
    override val permissionBlocked = "بلاک"
    override val permissionAsk = "بپرس"
    override val sitePermissionsSection = "دسترسی‌ها"
    override val sitePermissionsReset = "برگرد به پیش‌فرض"
    override val sitePermissionsResetDone = "دسترسی‌ها به حالت پیش‌فرض برگشت."
    override val sitePermissionsSaved = "دسترسی‌ها ذخیره شد."
    override val menuReadAloud = "بلندخوانی صفحه"
    override val menuReaderMode = "حالت مطالعه"
    override val menuSitePermissions = "دسترسی‌های سایت"
    override val menuSmartRename = "نام تازه برای این تب"

    override val starRepoBoldLine = "به MarbleBr استار بده ⭐"
    override val starRepoHeadline = "یک استار، هزار امید"
    override val starRepoSubline =
        "اگر MarbleBr کارت را راه انداخت، با یک استار در گیت هاب به ما انرژی بده تا سریع تر بهترش کنیم."
    override val starRepoButton = "همین حالا استار می دهم ⭐"
    override val starRepoLater = "باشه، بعداً"
    override val starRepoNever = "دیگر نشان نده"
    override val starRepoTelegram = "کانال تلگرام ما"
    override val starRepoWeb = "دیدن مخزن در گیت هاب"
    override val starRepoThanks = "ممنون که با ما هستی 💙"
    override val starRepoBadgeFree = "رایگان"
    override val starRepoBadgePrivate = "بدون ردیاب"
    override val starRepoBadgeFast = "سبک و سریع"
    override val starRepoBadgeBilingual = "دوزبانه"

    override val menuSectionPage = "این صفحه"
    override val menuSectionProtection = "محافظت از ورود"
    override val menuSectionTools = "کتابخانه و ابزار"
    override val menuSectionApp = "برنامه"
    override val menuProtectedChip = "ورودت محافظت می‌شود"
    override val menuPrivateHint = "حالت خصوصی؛ چیزی ذخیره نمی‌شود"

    override val vaultGoldenTitle = "نسخهٔ طلایی"
    override val vaultGoldenPin = "طلایی کن"
    override val vaultGoldenUnpin = "برداشتن قفل"
    override val vaultGoldenHint = "نسخهٔ طلایی هیچ‌وقت خودکار پاک نمی‌شود."

    override val vaultExpiryTitle = "نگهبان انقضا"
    override val vaultExpiryBody = "برای تازه‌سازی، سایت را باز و ورود را بررسی کنید؛ سپس نسخهٔ باز را ذخیره کنید."
    override val vaultExpiryAllFresh = "در نسخه‌های بررسی‌شده کوکی نزدیک انقضا یافت نشد."
    override val vaultExpiryExpired = "منقضی شده"
    override val vaultExpiryRenew = "ذخیرهٔ تب باز"
    override val vaultExpiryRenewAll = "ذخیرهٔ تب‌های باز"

    override val vaultInsuranceTitle = "بیمهٔ نشست"
    override val vaultInsuranceBody = "از تب‌های محافظت‌شدهٔ باز نسخهٔ تازه می‌سازیم."
    override val vaultInsuranceRun = "بیمهٔ فوری"

    override fun vaultGoldenPinnedDone(domain: String) = "نسخهٔ $domain طلایی و قفل شد."
    override fun vaultGoldenRemoved(domain: String) = "قفل نسخهٔ $domain برداشته شد."
    override fun vaultExpiryDaysLeft(days: Int) = "$days روز مانده"
    override fun vaultExpiryRenewed(domain: String) = "نسخهٔ تب باز $domain ذخیره شد؛ تمدید ورود به سایت بستگی دارد."
    override fun vaultExpiryRenewedCount(count: Int) = "از $count تب باز نسخه ذخیره شد؛ اعتبار ورود را سایت تعیین می‌کند."
    override fun vaultInsuranceDone(count: Int) = "از $count سایت نسخهٔ تازه گرفتیم."

    override val dnsSystem = "خودکار"
    override val dnsCustom = "سفارشی"
    override val proxyNone = "بدون پروکسی"
    override val statusHealthy = "سالم"
    override val statusProtected = "مراقبت فعال"
    override val statusHealing = "در حال ترمیم"
    override val statusRecovered = "تلاش برای بازیابی"
    override val statusNeedsSignIn = "نیاز به ورود دوباره"
    override val statusProtectionOff = "مراقبت خاموش"
    override val reasonProtectionOff = "برای این سایت مراقبت خاموش است."
    override val reasonHealthy = "همه چیز مرتب است؛ همچنان وارد حسابی."
    override val reasonUnauthorized = "سایت درخواست تو را رد کرد."
    override val reasonLoginRedirect = "سایت تو را به صفحه ورود فرستاد."
    override val reasonCookieMissing = "کوکی ورودت ناپدید شد."
    override val reasonStorageCleared = "حافظه ی محلی سایت پاک شد."
    override val reasonHealed = "برای بازیابی نشست تلاش شد؛ وضعیت ورود را بررسی کن."
    override val reasonServerRevoked = "سایت نشست را بست؛ با اثر انگشت یا Passkey سریع برگرد."
    override val reasonUnknown = "اتفاقی برای نشست افتاد که بررسی شد."
    override val tabProcessEnded = "صفحه بسته شد. برای باز کردن دوبارهٔ آن، تازه‌سازی را بزن."
    override val statusRestoreUnverified = "ورود تأیید نشده"
    override val statusRestoreFailed = "بازیابی ناموفق"
    override val reasonRestoreAttempted = "نسخهٔ محلی اعمال شد. وضعیت ورود را در سایت بررسی کن."
    override val reasonRestoreUnavailable = "هیچ داده‌ای از نسخهٔ محلی اعمال نشد."

    // ---------------------------------------- افزودن هر سایتی
    override val addAnySiteTitle = "افزودن هر سایتی، حتی سخت‌ها"
    override val addAnySiteDescription = "هر آدرسی را بنویس؛ اگر یک راه جواب نداد، خودمان راه بعدی را امتحان می‌کنیم."
    override val addAnySiteHint = "آدرس سایت، مثلاً bank.example یا app.example.ir"
    override val addAnySiteAction = "افزودن سایت"
    override val addAnySiteAdded = "سایت اضافه شد و در فهرست است."
    override fun addAnySiteFallback(attempt: Int, of: Int) = "راه $attempt از $of جواب نداد؛ راه بعدی را امتحان می‌کنیم."
    override fun addAnySiteRoute(route: String) = "راه پیشنهادی: $route"
    override val addAnySiteTriedAll = "همهٔ راه‌ها امتحان شد. سایت در فهرست می‌ماند و هر وقت خواستی دوباره تلاش کن."
    override val routeDirect = "باز کردنِ مستقیم"
    override val routeDesktop = "نمای دسکتاپ"
    override val routeMobile = "نمای موبایلِ سایت"
    override val routeCanonical = "نشانیِ اصلی بدون www"
    override val routePwa = "پنجرهٔ مستقل (مانند اپ)"
    override val routeManual = "ورودِ دستی با مخزن"
    override val routeReadOnly = "نمایش نسخهٔ ذخیره‌شده"
    override val loginMethodTitle = "این سایت چطور وارد می‌شود؟"
    override val loginMethodPassword = "با نام کاربری و گذرواژه"
    override val loginMethodGoogle = "با حساب گوگل"
    override val loginMethodApple = "با حساب اپل"
    override val loginMethodPhone = "با کد پیامکی"
    override val loginMethodEmailCode = "با کد ایمیل"
    override val loginMethodMagicLink = "با لینکِ ورود در ایمیل"
    override val loginMethodPasskey = "با اثر انگشت یا پاسکی"
    override val loginMethodWallet = "با کیف پول"
    override val loginMethodSso = "با حساب سازمانی"
    override val loginMethodUnknown = "روش ورود هنوز معلوم نیست"
    override val loginMethodUnknownHint = "صفحه را باز کن؛ نشانه‌ها را خودمان پیدا می‌کنیم."
    override val blockerIframe = "فرم ورود داخل قابِ دیگری است؛ ورودِ دستی از مخزن راه‌حل است."
    override val blockerBrowser = "سایت مرورگرهای داخلی را نمی‌پذیرد؛ نمای دسکتاپ را امتحان می‌کنیم."
    override val blockerThirdParty = "ورود به کوکیِ سایتِ دیگر وابسته است؛ پنجرهٔ مستقل کمک می‌کند."
    override val blockerJavaScript = "صفحه بدون اجرای اسکریپت چیزی نشان نمی‌دهد."
    override val blockerNetwork = "شبکه یا خودِ سایت پاسخ نداد؛ نسخهٔ ذخیره‌شده را نشان می‌دهیم."
    override val blockerOtp = "این سایت فقط کد یک‌بارمصرف می‌خواهد؛ کد را ذخیره نمی‌کنیم."
    override val saveLoginTitle = "این ورود را ذخیره کنم؟"
    override val saveLoginBody = "نام کاربری و گذرواژه با کلیدِ دستگاه رمز می‌شوند و فقط روی همین گوشی می‌مانند."
    override val saveLoginConfirm = "ذخیره کن"
    override val saveLoginNotNow = "الآن نه"
    override val saveLoginNeverForSite = "برای این سایت دیگر نپرس"
    override val saveLoginSaved = "ورود ذخیره شد."
    override val saveLoginFailed = "ذخیره نشد؛ بعداً از مخزن دوباره امتحان کن."
    override val saveLoginAutoTitle = "ذخیرهٔ هوشمندِ ورود"
    override val saveLoginAutoBody = "فقط پس از ورودِ موفق و با تأییدِ خودت ذخیره می‌شود."
    override val saveLoginFillTitle = "ورودِ ذخیره‌شده پیدا شد"
    override val saveLoginFillAction = "پر کردنِ فرم"
    override val saveLoginNoStored = "برای این سایت ورودی ذخیره نشده است."
    override val saveLoginWeakerTitle = "گذرواژهٔ تازه ضعیف‌تر است"
    override val saveLoginWeakerBody = "گذرواژه‌ای که ساختی از قبلی ضعیف‌تر است؛ مطمئنی می‌خواهی عوضش کنی؟"
    override val saveLoginUsernameTitle = "این حساب را ذخیره کنم؟"
    override val saveLoginConfirmPrefilled = "ذخیره و پرکردنِ بعدی"
    override val saveLoginAnotherAccount = "این حساب برای این سایت تازه است؛ مطمئن باش حسابِ درست را انتخاب کرده‌ای."
    override val saveLoginPasswordChangedHint = "گذرواژهٔ این حساب عوض شده است. نسخهٔ تازه را جایگزین کنم؟"
    override val saveLoginFirstAccountHint = "اولین حسابِ این سایت است؛ از دفعهٔ بعد فرم را خودمان پر می‌کنیم."
    override val saveLoginAccountAdded = "حساب به مخزن اضافه شد."
    override val autofillConfirmTitle = "ورودِ ذخیره‌شده پیدا شد"
    override val autofillConfirmBody = "برای پرکردنِ نام کاربری و گذرواژه، اثر انگشت یا قفل گوشی را تأیید کن."
    override val autofillConfirmAction = "پر کردن با اثر انگشت"
    override val autofillUseNow = "همین حالا پر کن"
    override val autofillAlternatives = "حساب‌های دیگر این سایت"
    override val autofillFillNow = "پر کردن با این حساب"
    override val codeFieldChip = "کد را بگذار"
    override val codeUnavailableTitle = "کدی پیدا نشد"
    override val codeUnavailableBody =
        "پیامک یا اعلانِ تازه‌ای با رمزِ یکبارمصرف نبود. دسترسیِ پیامک/اعلان را در تنظیمات بررسی کن " +
            "یا کد را کپی کن و در کادر بچسبان."
    override val codeFilled = "کد در کادر نوشته شد."
    override val codeSmsPermission = "خواندنِ پیامک (اختیاری)"
    override val codeNotificationAccess = "دسترسی به اعلان‌ها (اختیاری)"
    override val codeOpenSettings = "باز کردنِ تنظیمات"
    override val codeHowTitle = "رمزِ یکبارمصرف چطور پر می‌شود؟"
    override val codeHowBody =
        "وقتی صفحه‌ای کد بخواهد، MarbleBr پیامک و اعلان‌های همین گوشی را نگاه می‌کند، " +
            "فقط عددِ کد را برمی‌دارد و در کادرِ درست می‌نویسد. پیام‌ها ذخیره یا ارسال نمی‌شوند."
    override val codePasteFromClipboard = "چسباندن از کلیپ‌بورد"
    override val codeClipboardEmpty = "کلیپ‌بورد کدِ معتبری ندارد."
    override val codeManualHint = "اگر اجازهٔ خواندن ندهی، همیشه می‌توانی کد را کپی کنی و از دکمهٔ زیر بچسبانی."
    override val loginRejectedNotice = "ورود پذیرفته نشد؛ چیزی ذخیره نکردیم."
    override val saveLoginNeverAgain = "برای این سایت دیگر نپرس"
    override val saveModeTitle = "ذخیرهٔ ورودها"
    override val saveModeAsk = "همیشه بپرس"
    override val saveModeAskBody = "پس از هر ورودِ موفق، خودت تصمیم می‌گیری ذخیره شود یا نه."
    override val saveModeAuto = "ذخیرهٔ خودکارِ مواردِ روشن"
    override val saveModeAutoBody = "ورودِ اول به یک سایت بی‌صدا ذخیره می‌شود؛ مواردِ مبهم هنوز می‌پرسند."
    override val saveModeOff = "هرگز ذخیره نکن"
    override val saveModeOffBody = "هیچ ورودی ذخیره نمی‌شود؛ فرم‌های موجود در مخزن دست‌نخورده می‌مانند."
    override val otpAssistTitle = "رمزِ یکبارمصرف"
    override val otpAssistBody = "کدِ پیامک یا اعلان را در کادرِ صفحه بنویس؛ اگر اجازه ندهی، خودت می‌چسبانی."
    override fun otpSourceSummary(source: String) = "منبعِ خودکار: $source"
    override val otpManualAlwaysTitle = "همیشه می‌توانی دستی بچسبانی"
    override fun passwordStrengthLabel(level: String) = "قدرت گذرواژه: $level"
    override val passwordStrengthVeryWeak = "بسیار ضعیف"
    override val passwordStrengthWeak = "ضعیف"
    override val passwordStrengthFair = "متوسط"
    override val passwordStrengthStrong = "قوی"
    override val passwordStrengthExcellent = "عالی"
    override val phishingWarningTitle = "مراقب باش؛ این آدرس اصل نیست"
    override fun phishingWarningBody(host: String, impersonated: String) =
        "$host شبیه $impersonated است ولی دامنهٔ دیگری دارد. اینجا گذرواژه ننویس."
    override val phishingWarningDismiss = "بستن"
    override val phishingWarningTrustOnce = "می‌دانم، ادامه می‌دهم"
    override val originTrusted = "آدرسِ آشنا"
    override val originUnknown = "سایتِ تازه برای تو"
    override val originLookalike = "شبیه یک سایتِ آشنا"
    override val loginGuardEnabledTitle = "نگهبانِ بدلِ ورود"
    override val autoFillEnabledTitle = "پرکردنِ خودکارِ فرمِ ورود"
    override val autoFillEnabledBody = "روی صفحهٔ ورودِ سایت‌های شناخته‌شده، ورودِ ذخیره‌شده را با یک لمس پر می‌کنیم."
    override val loginGuardEnabledBody = "اگر آدرسی شبیه سایتِ آشنا باشد ولی دامنهٔ دیگری داشته باشد، پیش از تایپِ گذرواژه هشدار می‌دهیم."
    override val backupTitle = "پشتیبان‌گیری از گذرواژه‌ها و ورودها"
    override val backupDescription = "یک فایلِ رمزشده بساز و هر جا خواستی بازگردان؛ بدونِ گذرواژه‌ات قابلِ خواندن نیست."
    override val backupCreate = "ساخت فایل پشتیبان"
    override val backupCreateBody = "یک گذرواژه برای خودِ فایل انتخاب کن. اگر آن را گم کنی، فایل باز نمی‌شود."
    override val backupPassword = "گذرواژهٔ فایل پشتیبان"
    override val backupPasswordConfirm = "تکرار گذرواژه"
    override val backupPasswordHint = "دست‌کم ۸ نویسه؛ جملهٔ طولانی بهتر است."
    override val backupPasswordMismatch = "دو گذرواژه یکسان نیستند."
    override val backupPasswordTooShort = "گذرواژهٔ فایل باید دست‌کم ۸ نویسه باشد."
    override val backupPasswordWarning = "این گذرواژه را جایی امن نگه دار؛ بدونِ آن هیچ راه بازیابی وجود ندارد."
    override val backupRestore = "بازگردانی از فایل"
    override val backupRestoreBody = "فایل پشتیبان و گذرواژهٔ آن را وارد کن؛ اول پیش‌نمایش می‌بینی، بعد اعمال می‌کنی."
    override val backupRestorePickHint = "انتخاب فایل پشتیبان"
    override val backupRestorePreviewTitle = "پیش‌نمایشِ بازگردانی"
    override fun backupRestorePreviewBody(newItems: Int, updatedItems: Int, duplicates: Int) =
        "$newItems مورد تازه اضافه می‌شود، $updatedItems مورد به‌روز می‌شود و $duplicates مورد تکرارِ تکراری نادیده می‌ماند."
    override val backupRestoreApply = "اعمال کن"
    override val backupRestoreDone = "بازگردانی انجام شد."
    override fun backupRestoreDoneBody(credentials: Int, sessions: Int, sites: Int) =
        "$credentials ورود، $sessions نسخهٔ نشست و $sites سایت بازگردانی شد."
    override val backupWrongPassword = "گذرواژهٔ فایل درست نیست."
    override val backupNotABackup = "این فایل، پشتیبانِ MarbleBr نیست."
    override val backupEmpty = "چیزی برای پشتیبان‌گیری نیست."
    override val backupSavedTo = "فایل پشتیبان آماده شد."
    override val backupFailed = "ساخت فایل پشتیبان انجام نشد."
    override val backupCantShare = "برنامه‌ای برای ذخیرهٔ فایل پیدا نشد."
    override val backupIncludeCredentials = "گذرواژه‌ها"
    override val backupIncludeSessions = "نسخه‌های نشست"
    override val backupIncludeSites = "تنظیمات سایت‌ها"
    override val pulseTitle = "نبضِ نشست"
    override val pulseDescription = "بر اساس استفاده و زمان نسخه‌های محلی، تب‌های باز را برای ثبت نسخه پیشنهاد می‌کند؛ وضعیت ورود را نمی‌سنجد."
    override val pulseRunNow = "ثبت نسخه از تب‌های باز"
    override val pulseEmpty = "پیشنهادی نیست یا تب مناسب باز نیست."
    override fun pulseSiteLine(domain: String, urgency: String) = "$domain · $urgency"
    override val pulseUrgencyLow = "آرام"
    override val pulseUrgencyNormal = "معمولی"
    override val pulseUrgencyHigh = "زودتر لازم است"
    override val pulseUrgencyCritical = "نسخهٔ محلی قدیمی"
    override val pulseDone = "نسخهٔ محلی ثبت شد؛ اعتبار ورود بررسی نشده است."
    override val permissionPopups = "باز کردن پنجرهٔ جدید"
    override val permissionPopupTitle = "این سایت می‌خواهد پنجرهٔ تازه باز کند"
    override val permissionPopupBody = "اگر منتظرِ پرداخت، ورود با گوگل یا پیش‌نمایشِ فایل هستی، اجازه بده."
    override val permissionAllow = "اجازه"
    override val permissionBlock = "بلاک"
    override val permissionAskTitle = "دسترسی به گوشی"
    override fun permissionRequestBody(domain: String, permission: String) =
        "$domain می‌خواهد از $permission استفاده کند."
    override val permissionAndroidRequired = "برای این کار، اجازهٔ گوشی هم لازم است."
    override val permissionDenied = "دسترسی داده نشد."

    override val proxyHostInvalid = "آدرس پروکسی معتبر نیست (مثلاً 127.0.0.1 یا proxy.example.com)"
    override val proxyPortInvalid = "پورت باید بین ۱ تا ۶۵۵۳۵ باشد."
    override val proxyRestartRequired = "برای اعمالِ پروکسی، برنامه را ببند و دوباره باز کن."

    override val suggestionHistory = "بازدید اخیر"
    override val suggestionBookmark = "نشان"
    override val suggestionMostVisited = "پربازدید"
    override val suggestionOpenTabs = "تب باز"
    override val suggestionsTitle = "پیشنهادها"
    override val suggestionsEmpty = "پیشنهادی پیدا نشد."

    override val libraryTitle = "کتابخانه"
    override val libraryBookmarksTab = "نشان‌ها"
    override val libraryHistoryTab = "تاریخچه"
    override val librarySearchHint = "جستجو در نشان‌ها و تاریخچه…"
    override val libraryAllBookmarks = "همهٔ نشان‌ها"
    override val libraryAddBookmark = "افزودن نشان"
    override val libraryEditBookmark = "ویرایش نشان"
    override val librarySave = "ذخیره"
    override val libraryBookmarkTitle = "عنوان"
    override val libraryBookmarkUrl = "آدرس"
    override val libraryBookmarkFolder = "پوشه"
    override val libraryNewFolder = "پوشهٔ تازه"
    override val libraryFolderName = "نام پوشه"
    override val libraryMoveToFolder = "انتقال به پوشه"
    override val libraryDeleteBookmark = "حذف نشان"
    override val libraryDeleteBookmarkBody = "این نشان برای همیشه حذف می‌شود."
    override val libraryOpenInNewTab = "باز کردن در تب تازه"
    override val libraryDeleteHistoryItem = "حذف از تاریخچه"
    override val libraryDeleteHistoryBody = "این آدرس از تاریخچه حذف می‌شود؛ نشان‌ها دست‌نخورده می‌مانند."
    override val libraryDeleteSiteHistory = "حذف همهٔ بازدیدهای این سایت"
    override val libraryClearHistoryTitle = "پاک کردن تاریخچه"
    override val libraryClearHistoryBody = "کل تاریخچهٔ مرور پاک می‌شود. نشان‌ها و ورودها دست‌نخورده می‌مانند."
    override val libraryToday = "امروز"
    override val libraryYesterday = "دیروز"
    override val libraryThisWeek = "این هفته"
    override val libraryThisMonth = "این ماه"
    override val libraryOlder = "قدیمی‌تر"
    override val librarySort = "مرتب‌سازی"
    override val libraryMoreOptions = "گزینه‌های بیشتر"
    override val librarySortDate = "تاریخ"
    override val librarySortVisits = "تعداد بازدید"
    override val librarySortTitle = "عنوان"
    override val libraryEmptyBookmarksSearch = "نشانِ مطابقی پیدا نشد."
    override val libraryEmptyHistorySearch = "تاریخچه‌ای مطابقی پیدا نشد."
    override val libraryBookmarkSaved = "نشان ذخیره شد"
    override val libraryBookmarkUpdated = "نشان به‌روز شد"
    override val libraryBookmarkDeleted = "نشان حذف شد"
    override val libraryFolderCreated = "پوشه ساخته شد"
    override val libraryHistoryDeleted = "از تاریخچه حذف شد"
    override val librarySiteHistoryDeleted = "بازدیدهای این سایت حذف شد"
    override val libraryInvalidBookmark = "عنوان و آدرس را کامل وارد کنید."
    override val libraryAllFolders = "همهٔ پوشه‌ها"
    override val libraryFolderBookmarks = "نشان‌های این پوشه"
    override val libraryClearOlder = "پاک‌سازیِ تاریخچهٔ قدیمی"
    override val libraryClearOlderBody = "بازدیدهای قدیمی‌تر از بازهٔ انتخاب‌شده حذف می‌شوند؛ نشان‌ها دست‌نخورده می‌مانند."
    override val libraryClearOlder7 = "قدیمی‌تر از ۷ روز"
    override val libraryClearOlder30 = "قدیمی‌تر از ۳۰ روز"

    override val vaultAdvancedTools = "ابزارهای بیشتر"
    override val vaultAdvancedToolsBody = "پشتیبان‌گیری، نبض نشست و نگهبان انقضا"
    override val vaultHideAdvanced = "بستن ابزارها"
    override val vaultSignedInTitle = "همیشه وارد بمان"
    override val vaultSignedInBody = "سایت‌های تازه با محافظت روشن ساخته می‌شوند؛ وضعیتِ هر سایتِ موجود را جداگانه مدیریت کن."
    override val pulseToggle = "نبضِ هوشمند"
}

private object EnglishStrings : MarbleStrings() {
    override val accountsTitle = "Accounts on this site"
    override val accountsDescription = "Each account has its own storage. Sign in to the new account."
    override val accountCurrent = "Current account"
    override val accountCreate = "Add another account"
    override val accountName = "Account name"
    override val accountOpen = "Open account"
    override val credentialVaultTitle = "Passwords and accounts"
    override val credentialVaultDescription = "Stored encrypted on this device only."
    override val credentialLockRequired = "Authenticate with biometrics or your device screen lock to see passwords."
    override val credentialLockUnavailable = "Set up a device screen lock or biometrics first."
    override val credentialAdd = "Add login"
    override val credentialEdit = "Edit login"
    override val credentialSite = "Site URL (https://...)"
    override val credentialOriginReview = "For safe autofill, review and save the exact HTTPS address, including www and port."
    override val credentialUsername = "Username"
    override val credentialPassword = "Password"
    override val credentialNotes = "Notes"
    override val credentialSave = "Save"
    override val credentialDelete = "Delete this login?"
    override val credentialDeleteBody = "This removes the login from the password vault only."
    override val credentialInvalid = "Enter a valid site URL and a username or password."
    override val credentialFailed = "Could not save the login. Please try again."
    override val credentialEmpty = "No saved logins yet."
    override val credentialUnreadable = "Some entries could not be read or the vault is unavailable; nothing was deleted automatically."
    override val credentialGenerate = "Generate strong password"
    override val credentialShow = "Show password"
    override val credentialHide = "Hide password"
    override val credentialCopyUser = "Copy username"
    override val credentialCopyPassword = "Copy password"
    override val credentialCopied = "Copied; cleared after 30 seconds."
    override val contextTitle = "Page options"
    override val contextOpenTab = "Open link in new tab"
    override val contextCopyLink = "Copy link"
    override val contextShareLink = "Share link"
    override val contextSaveImage = "Download image"
    override val contextCopyImage = "Copy image address"
    override val contextShareText = "Share text"
    override val contextSavePdf = "Save page as PDF"
    override val contextCopyText = "Copy text"
    override val menuSharePage = "Share page"
    override val pdfSaved = "Page saved as PDF."
    override val pdfFailed = "Could not save the page."
    override val credentialUnlock = "Unlock vault"
    override val accountSessionWarning = "If the site revokes a session on its server, you must sign in again."

    override val appName = "MarbleBr"
    override val back = "Back"
    override val cancel = "Cancel"
    override val ok = "OK"
    override val later = "Later"
    override val close = "Close"
    override val dismiss = "Dismiss"

    override val languageTitle = "App language"
    override val languageDescription = "On Automatic, MarbleBr follows your phone language. You can also pick Persian or English yourself."
    override val languageSystem = "Automatic (like my phone)"
    override val languagePersian = "فارسی"
    override val languageEnglish = "English"
    override val appearanceTitle = "Appearance"
    override val themeModeTitle = "Light or dark"
    override val themeSystem = "Like my phone"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val dynamicColorTitle = "Colors from your phone"
    override val dynamicColorDescription = "On Android 12 and newer, the app matches your phone theme."

    override val addressPlaceholder = "Type an address or search"
    override val copyLink = "Copy link"
    override val shareLink = "Share link"
    override val browserMenu = "More options"
    override val tabsLabel = "Tabs"
    override val protectionLabel = "Protection"
    override val bookmarksLabel = "Bookmarks"
    override val settingsLabel = "Settings"
    override val bookmarkToggle = "Bookmark this page"
    override val reload = "Reload"
    override val goBack = "Back a page"
    override val goForward = "Forward a page"
    override val protectionOnChip = "We’re watching this site"
    override val protectionOffChip = "Protection is off for this site"
    override val privateTabChip = "Private tab: nothing is saved"
    override val menuSaveState = "Save right now"
    override val menuSaveStateHint = "We make a safe copy of your login"
    override val menuTimeMachine = "Go back to an earlier copy"
    override val menuTimeMachineHint = "Signed out by accident? Restore it here"
    override val menuFindInPage = "Find on this page"
    override val menuDesktopModeOn = "Show desktop version"
    override val menuDesktopModeOff = "Show mobile version"
    override val menuSaveShortcut = "Shortcut on home screen"
    override val menuFreeSpace = "Free up temporary space"
    override val menuFreeSpaceHint = "Without signing you out"
    override val findInPagePlaceholder = "Find on this page…"
    override val findPrevious = "Previous match"
    override val findNext = "Next match"
    override val findClose = "Close search"
    override val reloginTitle = "Sign in again to continue"
    override val reloginConfirm = "Continue on site"
    override val reloginDismiss = "No, I’ll sign in myself"
    override val readyTitle = "Ready to look after you"
    override val readyBody = "Just type an address. We quietly keep you signed in while you browse."
    override val quickSaveState = "Save this state"
    override val quickEarlierCopies = "Earlier copies"
    override val newTab = "New tab"
    override val newPrivateTab = "Private tab"
    override val closeTab = "Close tab"
    override val newTabTitle = "New tab"
    override val privateTabTitle = "Private tab"
    override val addressSelectionTitle = "Address bar selection"
    override val addressSelectionDescription = "Choose what is selected when you tap the address bar."
    override val addressSelectAll = "Entire address"
    override val addressSelectPart = "Tapped segment"
    override val addressCursor = "Cursor only"
    override val googleNewTab = "Open Google in new tabs"
    override val googleNewTabDescription = "When off, new tabs stay on Home and no website loads."
    override val goToAddress = "Go to address"
    override val navigationFailed = "Could not open the address. Try again."
    override val stopLoading = "Stop loading"
    override val duplicateTab = "Duplicate tab"
    override val reopenTab = "Reopen closed tab"
    override val closeAllTabs = "Close all tabs"
    override val confirmCloseAllTabs = "Close all regular and private tabs?"
    override val searchTabs = "Search tabs"
    override val noTabs = "No open tabs."
    override val noMatchingTabs = "No matching tabs."
    override val clearHistory = "Clear history"
    override val emptyBookmarks = "Nothing saved yet. Use the bookmark icon at the top to save a site."
    override val emptyHistory = "Nothing here yet. Sites you open will show up here."
    override val emptyDownloads = "No downloads yet."
    override val downloadNotificationText = "Downloading with MarbleBr"

    override val bannerBookmarkAdded = "Added to bookmarks"
    override val bannerBookmarkRemoved = "Removed from bookmarks"
    override val bannerShortcutAdded = "Shortcut added — open it from your home screen"
    override val bannerShortcutCreated = "A separate space for this site is ready"
    override val bannerSnapshotSaved = "A safe copy of your login was saved"
    override val bannerCacheCleared = "Temporary files cleared — you’re still signed in"
    override val bannerHistoryCleared = "History cleared"
    override val bannerDownloadStarted = "Download started"
    override val downloadFailed = "Download failed"
    override val downloadInProgress = "Downloading"
    override val snapshotUnavailable = "This page has no session data available to save."
    override val bannerDesktopOn = "Desktop version is on"
    override val bannerDesktopOff = "Mobile version is on"

    override val settingsTitle = "Settings"
    override val settingsIntro = "Tune what you need, in plain language."
    override val protectionSectionTitle = "Staying signed in"
    override val protectionSectionBody = "These choices decide how MarbleBr keeps you inside your accounts."
    override val globalNeverSignOutTitle = "Protect new sites"
    override val globalNeverSignOutDescription = "Turn session protection on automatically for every new site."
    override val biometricTitle = "Fingerprint lock"
    override val biometricDescription = "Ask for your fingerprint or phone PIN before showing saved copies."
    override val keepAliveTitle = "Periodic session snapshots"
    override val keepAliveDescription = "Saves local snapshots from open tabs for enabled accounts. Android schedules when they run."
    override val credentialTitle = "Sign-in reminders"
    override val credentialDescription = "Show a reminder in the active tab when the site asks for sign-in."
    override val adBlockTitle = "Block ads and trackers"
    override val adBlockDescription = "Pages load lighter and faster, with less tracking."
    override val freeSpaceTitle = "Free up temporary space"
    override val freeSpaceDescription = "Only temporary files go — you stay signed in."
    override val freeSpaceButton = "Free up"
    override val freeSpaceDone = "Temporary files cleared"
    override val networkTitle = "Network (advanced)"
    override val networkBody = "Set a secure DNS or a proxy here if you need one. Most people can skip this."
    override val dnsLabel = "Secure DNS"
    override val dnsCustomHint = "Server address (starts with https)"
    override val proxyLabel = "Proxy"
    override val proxyHostHint = "Proxy address or IP"
    override val proxyPortHint = "Port"
    override val saveNetwork = "Save network settings"
    override val networkSaved = "Network settings saved"
    override val aboutTitle = "About"

    override val vaultTitle = "Looking after your accounts"
    override val vaultIntro = "See which sites MarbleBr protects and which copies it has saved."
    override val vaultSecurityTitle = "Everything stays on this phone"
    override val vaultSecurityBody = "Everything is locked with your phone key and never leaves it."
    override val unlockVault = "Unlock with fingerprint"
    override val biometricPromptTitle = "Account protection lock"
    override val biometricPromptSubtitle = "Confirm it’s you to see saved copies"
    override val biometricCancelled = "Identity check cancelled"
    override val reloginUnavailable = "No saved sign-in was found for this site"
    override val reloginUnsupportedType = "This kind of sign-in isn’t supported"
    override val reloginManualHint = "This sign-in can’t be automated; the page stays open so you can sign in your way and save it to the vault."
    override val reloginPromptSubtitle = "The site ended your session — confirm with your fingerprint to get back in"
    override val sitesSectionTitle = "Sites"
    override val sitesSectionBody = "Every site has its own safe space."
    override val protectThisSite = "Protect this site"
    override val signOutNow = "Sign out completely"
    override val signOutConfirmTitle = "Sign out of this site?"
    override val signOutConfirmBody = "Your current login and every saved copy of this site will be deleted. Other sites stay untouched."
    override val signOutConfirmAction = "Yes, sign me out"
    override val timeMachineTitle = "Go back in time"
    override val timeMachineBody = "Jump back to any moment you like."
    override val emptySnapshots = "No copies yet — we make them as you browse."
    override val emptySites = "No sites yet. Just open an address."
    override val backgroundSnapshotLabel = "Background copy"
    override val restore = "Restore"
    override val healthLogTitle = "What happened?"
    override val healthLogBody = "Anything that happens to a login shows up here."
    override val feedbackRestoreFailed = "We couldn’t read that copy"
    override val gotIt = "Got it"

    override val vaultHeroTitle = "Your accounts are safe"
    override val vaultHeroBody = "Logins are saved on this phone only."
    override val vaultStatSites = "Sites"
    override val vaultStatProtected = "Protected"
    override val vaultStatSnapshots = "Saved copies"
    override val vaultLastSaved = "Last saved"
    override val vaultNever = "Nothing saved yet"
    override val vaultSearchHint = "Search sites…"
    override val vaultFilterAll = "All"
    override val vaultFilterProtected = "Protected"
    override val vaultFilterUnprotected = "Unprotected"
    override val vaultFilterPinned = "Shortcut"
    override val vaultNoMatch = "No site matches this search."
    override val vaultProtectionOffBanner = "Protection for new sites is off."
    override val vaultSectionSites = "Your sites"
    override val vaultSectionTimeline = "Copy history"
    override val vaultSectionActivity = "Protection log"
    override val vaultSiteProtected = "Protection on"
    override val vaultSiteUnprotected = "Protection off"
    override val vaultSitePinned = "Home screen shortcut"
    override val vaultSiteDetailsTitle = "Settings for this site"
    override val vaultProtectSwitchTitle = "Guard my login"
    override val vaultProtectSwitchBody = "Protects local tab data; the site decides whether your sign-in is valid."
    override val vaultKeepAliveSwitchTitle = "Periodic local snapshots"
    override val vaultKeepAliveSwitchBody = "Saves local snapshots from an open tab for this account. Android may delay capture."
    override val vaultCookieDaysTitle = "Session cookie retention"
    override val vaultCookieDaysBody = "Observed session cookies are retained for this long; server expiry and validity do not change."
    override val vaultSnapshotsOfSite = "Copies of this site"
    override val vaultSaveNow = "Save right now"
    override val vaultDelete = "Delete"
    override val vaultDeleteConfirmTitle = "Delete this copy?"
    override val vaultDeleteConfirmBody = "This saved copy is removed for good and cannot be undone."
    override val vaultClose = "Close"
    override val vaultEmptyActivity = "Nothing has happened yet."
    override val vaultFilterEmpty = "Sites appear here once their protection changes."
    override val vaultHealthHealthy = "Protection is enabled"
    override val vaultProtectAll = "Protect all"
    override val vaultUnprotectAll = "Unprotect all"
    override val vaultProtectAllOnDone = "Protection turned on for all sites"
    override val vaultProtectAllOffDone = "Protection turned off for all sites"
    override val vaultRestoreNeedsOpenTab = "Keep the site open in the matching account to restore its data."
    override val vaultRestoreSuperseded = "A newer snapshot exists for this account. Use the latest snapshot or sign in again."
    override val vaultRestoreConfirmTitle = "Apply this local copy?"
    override val vaultRestoreConfirmBody =
        "Even the latest copy may contain old tokens and cause a sign-out or session revocation. " +
            "Sign-in is not verified; signing in through the site is safer. Continue knowing this risk?"
    override val vaultQuickActionsTitle = "Quick actions"
    override val vaultJumpToSite = "Jump to site"
    override val feedbackRestoreEmpty = "Nothing could be restored; check the open page and network connection."
    override val quickPageSection = "This page"
    override val quickLibrarySection = "Library"
    override val quickToolsSection = "Protection & tools"
    override fun vaultSnapshotCount(count: Int) = "$count copies"
    override fun vaultLastSavedAgo(label: String) = "Last saved: $label"
    override fun vaultKeepAliveEveryMinutes(minutes: Int) = "Every $minutes min"
    override fun vaultDeleteSnapshotDone(domain: String) = "Removed a copy of $domain"

    override fun tabsCount(count: Int) = "$count open tabs"
    override fun adsBlocked(count: Int) = "$count ads and trackers blocked"
    override fun downloadsMenu(count: Int) = "Downloads ($count)"
    override fun openTabsTitle(count: Int) = "Open tabs ($count)"
    override fun bookmarksCount(count: Int) = "Bookmarks ($count)"
    override fun historyCount(count: Int) = "History ($count)"
    override fun savedPath(path: String) = "Saved to: $path"
    override fun reloginBody(domain: String) =
        "$domain is asking you to sign in. Continue on the site to authenticate."
    override fun adBlockChip(count: Int) = "Ads blocked: $count"
    override fun sitesCount(count: Int) = "Sites ($count)"
    override fun keepAliveEvery(minutes: Int) = "Snapshot interval ($minutes min)"
    override fun keepSignedInFor(days: Int) = "Keep me signed in for $days days"
    override fun signOutDialogTitle(domain: String) = "Sign out of $domain?"
    override fun signOutDialogBody(domain: String) =
        "Your current login and saved copies for $domain will be deleted. Other sites stay untouched."
    override fun snapshotLine(date: String, cookies: Int, storageKeys: Int) =
        "Saved $date · $cookies cookies · $storageKeys storage keys"
    override fun feedbackProtection(domain: String, enabled: Boolean) =
        if (enabled) "Protection is on for $domain" else "Protection is off for $domain"
    override fun feedbackCookieDays(domain: String, days: Int) = "You’ll stay signed in to $domain for $days days"
    override fun feedbackKeepAliveOn(domain: String, minutes: Int) =
        "Snapshot interval for $domain set to $minutes minutes"
    override fun feedbackKeepAliveOff(domain: String) = "Periodic snapshots for $domain are off"
    override fun feedbackRestored(label: String, cookies: Int) = "Restore attempted for “$label” ($cookies cookies offered to the browser); check your sign-in."
    override fun feedbackSignedOut(domain: String) = "Closed tabs and requested cleanup for this $domain account."
    override fun aboutBody(version: String) =
        "MarbleBr version $version — built so you sign in once and stay in. No trackers, no ads."
    override fun reloginAutoTitle(domain: String) = "Sign in to $domain again"
    override fun autoSnapshotLabel(domain: String) = "Auto copy — $domain"
    override fun manualSnapshotLabel(domain: String) = "Manual copy — $domain"
    override fun pwaShortcutLongLabel(title: String) = "$title (stays signed in)"

    override val homeTitle = "Home"
    override val homeIntro = "Start browsing from here"
    override val homeSearchHint = "Search or type a website address…"
    override val homeRecentTitle = "Recent history"
    override val homeEmptyRecent = "No recent visits yet"
    override val homeShortcutsTitle = "Your shortcuts"
    override val homeEmptyShortcuts = "No shortcuts yet. Save your favorite sites from the browser."

    override val rateTitle = "Rate MarbleBr"
    override val rateMessage =
        "If you like MarbleBr, please give us a star so we can keep improving it for you."
    override val rateNow = "Yes, with pleasure"
    override val rateLater = "Later"
    override val rateDontAskAgain = "I won't, don't ask again"

    override val homeScreenTitle = "Home screen"
    override val searchEngineTitle = "Search engine"
    override val searchEngineDescription =
        "Choose the default search engine for the address bar and home screen."
    override val homeShortcutPositionTitle = "New-tab button on the home screen"
    override val homeShortcutPositionDescription = "The new-tab button now lives inside the shortcut tiles instead of floating on top of the page."
    override val shortcutPositionNone = "Hidden"
    override val shortcutPositionFloating = "Floating (old)"
    override val shortcutPositionBuiltIn = "With the shortcuts (recommended)"

    override val homeSeeAll = "See all →"

    override val settingsTabAppearance = "Appearance"
    override val settingsTabBrowser = "Browser"
    override val settingsTabProtection = "Protection"
    override val settingsTabNetwork = "Network"
    override val settingsTabAbout = "About"
    override val colorPaletteTitle = "Color palette"
    override val colorPaletteDescription = "Pick one of MarbleBr’s ready-made themes; every screen follows the same colors."
    override fun colorPaletteLabel(palette: ColorPalette) = when (palette) {
        ColorPalette.DYNAMIC -> "Your phone’s colors"
        ColorPalette.OCEAN -> "Ocean"
        ColorPalette.EMERALD -> "Emerald"
        ColorPalette.SUNSET -> "Sunset"
        ColorPalette.ROYAL -> "Royal"
        ColorPalette.CANDY -> "Candy"
        ColorPalette.GRAPHITE -> "Graphite"
        ColorPalette.SAND -> "Sand"
    }
    override val paletteDynamicHint = "On Android 12 and newer we take the colors from your phone wallpaper."
    override val themePreviewTitle = "Live preview"
    override val themePreviewBody = "See how buttons and cards look in the new theme right away."

    override val chromeBarStyleTitle = "Top and bottom bar style"
    override val chromeBarStyleDescription = "“Floating” means the bars are pure glass with no background, so the page shows through them."
    override val chromeSolid = "Plain and solid"
    override val chromeGlass = "Frosted glass"
    override val chromeFloating = "Floating glass"
    override val backBehaviorTitle = "Back button"
    override val backBehaviorDescription = "Choose what happens when you press your phone’s back button."
    override val backSmart = "Smart (recommended)"
    override val backPageThenHome = "Previous page, then Home"
    override val backHome = "Always Home"
    override val backPageThenExit = "Previous page, then exit"
    override val backAsk = "Ask me every time"
    override val backAskTitle = "Go back where?"
    override val backAskBody = "You pressed back — pick what should happen."
    override val backAskToPage = "Previous page on this site"
    override val backAskToHome = "The Home screen"
    override val backAskExit = "Exit the app"
    override val tabsBehaviorTitle = "Tabs behaviour"
    override val askBeforeClosingTabs = "Confirm before closing a tab"
    override val askBeforeClosingTabsDescription = "If a tab is open and you close it, we’ll ask first."
    override val closeTabConfirmTitle = "Close this tab?"
    override fun closeTabConfirmBody(title: String) = "The tab “$title” will be closed. You can bring it back with “Reopen closed tab”."

    override val quickActionsTitle = "Quick shortcuts"
    override val quickCopyLink = "Copy link"
    override val quickShare = "Share"
    override val quickScreenshot = "Page screenshot"
    override val quickFindInPage = "Find on page"
    override val quickDesktop = "Desktop"
    override val quickPrivate = "Private tab"
    override val quickSaveLogin = "Save login"
    override val quickTimeMachine = "Time machine"
    override val quickDownloads = "Downloads"
    override val quickClearCache = "Clear cache"
    override val quickAddShortcut = "Home shortcut"
    override val quickRefresh = "Reload"
    override val quickHome = "Home"
    override val quickTabs = "Tabs"
    override val quickSettings = "Settings"
    override val quickBookmarks = "Bookmarks"
    override val quickSignOut = "Sign out fully"
    override val quickNewTab = "New tab"
    override fun quickSiteSignedInFor(days: Int, minutes: Int) =
        "This site stays signed in for $days days · quiet check-in every $minutes min"

    override val readAloudTitle = "Read the page aloud"
    override val readAloudDescription = "We read the page text out loud with your phone’s voice."
    override val readAloudStart = "Read"
    override val readAloudStop = "Stop reading"
    override val readAloudUnavailable = "There is nothing to read on this page."
    override val readerModeTitle = "Reader mode"
    override val readerModeDescription = "Ads and menus go away, leaving just the text."
    override val readerModeOn = "Reader mode is on"
    override val readerModeOff = "Reader mode is off"
    override val readerModeUnavailable = "This page doesn’t support reader mode."
    override val sitePermissionsTitle = "Permissions for this site"
    override val sitePermissionsDescription = "You decide what a site may use on your phone."
    override val permissionJavascript = "Run JavaScript"
    override val permissionAutoplay = "Autoplay video"
    override val permissionGeolocation = "Location"
    override val permissionCamera = "Camera"
    override val permissionMicrophone = "Microphone"
    override val permissionNotifications = "Notifications"
    override val permissionAllowed = "Allow"
    override val permissionBlocked = "Block"
    override val permissionAsk = "Ask"
    override val sitePermissionsSection = "Permissions"
    override val sitePermissionsReset = "Reset to defaults"
    override val sitePermissionsResetDone = "Permissions are back to default."
    override val sitePermissionsSaved = "Permissions saved."
    override val menuReadAloud = "Read aloud"
    override val menuReaderMode = "Reader mode"
    override val menuSitePermissions = "Site permissions"
    override val menuSmartRename = "Give this tab a name"

    override val starRepoBoldLine = "Star MarbleBr ⭐"
    override val starRepoHeadline = "One star, a thousand hopes"
    override val starRepoSubline =
        "If MarbleBr made your day easier, one star on GitHub gives us the push to make it even better."
    override val starRepoButton = "I’ll star it now ⭐"
    override val starRepoLater = "Sure, later"
    override val starRepoNever = "Don’t show again"
    override val starRepoTelegram = "Our Telegram channel"
    override val starRepoWeb = "Open the repo"
    override val starRepoThanks = "Thanks for being here 💙"
    override val starRepoBadgeFree = "Free"
    override val starRepoBadgePrivate = "No trackers"
    override val starRepoBadgeFast = "Light & fast"
    override val starRepoBadgeBilingual = "Bilingual"

    override val menuSectionPage = "This page"
    override val menuSectionProtection = "Login protection"
    override val menuSectionTools = "Library & tools"
    override val menuSectionApp = "App"
    override val menuProtectedChip = "Login protected"
    override val menuPrivateHint = "Private mode — nothing is saved"

    override val vaultGoldenTitle = "Golden version"
    override val vaultGoldenPin = "Make golden"
    override val vaultGoldenUnpin = "Remove lock"
    override val vaultGoldenHint = "A golden version is never deleted automatically."

    override val vaultExpiryTitle = "Expiry guardian"
    override val vaultExpiryBody = "Open the site and check your login, then save the open tab. Only the site can renew it."
    override val vaultExpiryAllFresh = "No expiring cookie was found in the snapshots checked."
    override val vaultExpiryExpired = "Expired"
    override val vaultExpiryRenew = "Save open tab"
    override val vaultExpiryRenewAll = "Save open tabs"

    override val vaultInsuranceTitle = "Session insurance"
    override val vaultInsuranceBody = "Take a fresh copy of protected tabs that are open."
    override val vaultInsuranceRun = "Insure now"

    override fun vaultGoldenPinnedDone(domain: String) = "The $domain version is now golden and locked."
    override fun vaultGoldenRemoved(domain: String) = "The lock on the $domain version was removed."
    override fun vaultExpiryDaysLeft(days: Int) = "$days days left"
    override fun vaultExpiryRenewed(domain: String) = "Saved the open $domain tab. Login renewal depends on the site."
    override fun vaultExpiryRenewedCount(count: Int) = "Saved $count open tabs; login validity depends on the site."
    override fun vaultInsuranceDone(count: Int) = "Took a fresh copy of $count sites."

    override val dnsSystem = "Automatic"
    override val dnsCustom = "Custom"
    override val proxyNone = "No proxy"
    override val statusHealthy = "Healthy"
    override val statusProtected = "Protection on"
    override val statusHealing = "Healing"
    override val statusRecovered = "Restore attempted"
    override val statusNeedsSignIn = "Needs a new sign-in"
    override val statusProtectionOff = "Protection off"
    override val reasonProtectionOff = "Protection is off for this site."
    override val reasonHealthy = "Everything is fine — you’re still signed in."
    override val reasonUnauthorized = "The site rejected the request."
    override val reasonLoginRedirect = "The site sent you to its login page."
    override val reasonCookieMissing = "Your login cookie disappeared."
    override val reasonStorageCleared = "The site’s local storage was wiped."
    override val reasonHealed = "Session restore was attempted; check whether you are still signed in."
    override val reasonServerRevoked = "The site ended your session — get back in with your fingerprint or a passkey."

    override val reasonUnknown = "Something touched your session, so we checked it."
    override val tabProcessEnded = "The page stopped. Reload to reopen it."
    override val statusRestoreUnverified = "Sign-in unverified"
    override val statusRestoreFailed = "Restore failed"
    override val reasonRestoreAttempted = "Local data was applied. Check sign-in on the site."
    override val reasonRestoreUnavailable = "No local snapshot data was applied."

    // ---------------------------------------- Add any site
    override val addAnySiteTitle = "Add any site, even the difficult ones"
    override val addAnySiteDescription = "Type any address. If one way fails, we try the next one for you."
    override val addAnySiteHint = "Site address, e.g. bank.example or app.example.com"
    override val addAnySiteAction = "Add site"
    override val addAnySiteAdded = "Added. The site is in your list."
    override fun addAnySiteFallback(attempt: Int, of: Int) = "Way $attempt of $of didn’t work — trying the next one."
    override fun addAnySiteRoute(route: String) = "Suggested way: $route"
    override val addAnySiteTriedAll = "Every way was tried. The site stays in your list; try again anytime."
    override val routeDirect = "Open directly"
    override val routeDesktop = "Desktop view"
    override val routeMobile = "Site’s mobile view"
    override val routeCanonical = "Canonical address without www"
    override val routePwa = "Standalone window (app-like)"
    override val routeManual = "Manual sign-in from the vault"
    override val routeReadOnly = "Show the saved copy"
    override val loginMethodTitle = "How does this site sign in?"
    override val loginMethodPassword = "Username and password"
    override val loginMethodGoogle = "Google account"
    override val loginMethodApple = "Apple account"
    override val loginMethodPhone = "SMS code"
    override val loginMethodEmailCode = "Email code"
    override val loginMethodMagicLink = "Magic link in email"
    override val loginMethodPasskey = "Fingerprint or passkey"
    override val loginMethodWallet = "Wallet"
    override val loginMethodSso = "Work account"
    override val loginMethodUnknown = "Sign-in method not known yet"
    override val loginMethodUnknownHint = "Open the page — we’ll look for the clues ourselves."
    override val blockerIframe = "The login form lives in another frame; manual sign-in from the vault works."
    override val blockerBrowser = "The site refuses in-app browsers; we’ll try desktop view."
    override val blockerThirdParty = "Sign-in relies on another site’s cookies; the standalone window helps."
    override val blockerJavaScript = "The page shows nothing without scripts running."
    override val blockerNetwork = "The network or the site didn’t answer; we’ll show the saved copy."
    override val blockerOtp = "This site only asks for a one-time code; we never store codes."
    override val saveLoginTitle = "Save this sign-in?"
    override val saveLoginBody = "Your username and password are encrypted with your device key and stay on this phone."
    override val saveLoginConfirm = "Save it"
    override val saveLoginNotNow = "Not now"
    override val saveLoginNeverForSite = "Never ask for this site"
    override val saveLoginSaved = "Sign-in saved."
    override val saveLoginFailed = "Couldn’t save it; try again from the vault."
    override val saveLoginAutoTitle = "Smart sign-in saving"
    override val saveLoginAutoBody = "Saved only after a successful sign-in, and only with your approval."
    override val saveLoginFillTitle = "Saved sign-in found"
    override val saveLoginFillAction = "Fill the form"
    override val saveLoginNoStored = "No saved sign-in for this site yet."
    override val saveLoginWeakerTitle = "The new password is weaker"
    override val saveLoginWeakerBody = "The password you just used is weaker than the saved one. Replace it anyway?"
    override val saveLoginUsernameTitle = "Save this sign-in?"
    override val saveLoginConfirmPrefilled = "Save and fill next time"
    override val saveLoginAnotherAccount = "This is a new account for this site — make sure it is the right one."
    override val saveLoginPasswordChangedHint = "This account's password changed. Replace the saved one?"
    override val saveLoginFirstAccountHint = "First account for this site; from now on we fill the form for you."
    override val saveLoginAccountAdded = "Account added to the vault."
    override val autofillConfirmTitle = "Saved sign-in found"
    override val autofillConfirmBody = "Confirm your fingerprint or device lock to fill the username and password."
    override val autofillConfirmAction = "Fill with fingerprint"
    override val autofillUseNow = "Fill now"
    override val autofillAlternatives = "Other accounts for this site"
    override val autofillFillNow = "Fill with this account"
    override val codeFieldChip = "Fill code"
    override val codeUnavailableTitle = "No code found"
    override val codeUnavailableBody =
        "No recent SMS or notification carried a one-time code. " +
            "Check the SMS/notification access, or copy the code and paste it."
    override val codeFilled = "Code written into the box."
    override val codeSmsPermission = "Read SMS (optional)"
    override val codeNotificationAccess = "Notification access (optional)"
    override val codeOpenSettings = "Open settings"
    override val codeHowTitle = "How is the one-time code filled?"
    override val codeHowBody =
        "When a page asks for a code, MarbleBr looks at SMS and notifications on this phone, " +
            "keeps only the numeric code and writes it into the right box. " +
            "Messages are never stored or sent."
    override val codePasteFromClipboard = "Paste from clipboard"
    override val codeClipboardEmpty = "The clipboard has no valid code."
    override val codeManualHint = "If you don't grant access, you can always copy the code and paste it with the button below."
    override val loginRejectedNotice = "Sign-in was rejected; nothing was saved."
    override val saveLoginNeverAgain = "Never ask for this site"
    override val saveModeTitle = "Saving sign-ins"
    override val saveModeAsk = "Always ask"
    override val saveModeAskBody = "After every successful sign-in you decide whether to save it."
    override val saveModeAuto = "Auto-save clear cases"
    override val saveModeAutoBody = "A site's first sign-in is saved silently; ambiguous cases still ask."
    override val saveModeOff = "Never save"
    override val saveModeOffBody = "Nothing new is saved; credentials already in the vault stay untouched."
    override val otpAssistTitle = "One-time codes"
    override val otpAssistBody = "Writes the SMS/notification code into the page's box; if you decline access you paste it yourself."
    override fun otpSourceSummary(source: String) = "Automatic source: $source"
    override val otpManualAlwaysTitle = "You can always paste it yourself"
    override fun passwordStrengthLabel(level: String) = "Password strength: $level"
    override val passwordStrengthVeryWeak = "Very weak"
    override val passwordStrengthWeak = "Weak"
    override val passwordStrengthFair = "Fair"
    override val passwordStrengthStrong = "Strong"
    override val passwordStrengthExcellent = "Excellent"
    override val phishingWarningTitle = "Careful — this address isn’t the real one"
    override fun phishingWarningBody(host: String, impersonated: String) =
        "$host looks like $impersonated but belongs to a different domain. Don’t type your password here."
    override val phishingWarningDismiss = "Close"
    override val phishingWarningTrustOnce = "I understand, continue"
    override val originTrusted = "A site you know"
    override val originUnknown = "New to you"
    override val originLookalike = "Looks like a site you know"
    override val loginGuardEnabledTitle = "Lookalike sign-in guard"
    override val autoFillEnabledTitle = "Autofill sign-in forms"
    override val autoFillEnabledBody = "On sign-in pages of sites you know, one tap fills the saved credentials."
    override val loginGuardEnabledBody = "If an address looks like a site you know but sits on another domain, we warn before you type a password."
    override val backupTitle = "Back up passwords and sign-ins"
    override val backupDescription = "Create one encrypted file and restore it anywhere; it can’t be read without your password."
    override val backupCreate = "Create backup file"
    override val backupCreateBody = "Pick a password for the file itself. If you lose it, the file can’t be opened."
    override val backupPassword = "Backup file password"
    override val backupPasswordConfirm = "Repeat password"
    override val backupPasswordHint = "At least 8 characters; a long sentence is better."
    override val backupPasswordMismatch = "The two passwords don’t match."
    override val backupPasswordTooShort = "The file password needs at least 8 characters."
    override val backupPasswordWarning = "Keep this password somewhere safe; there is no other way to recover it."
    override val backupRestore = "Restore from file"
    override val backupRestoreBody = "Pick the backup file and type its password; you see a preview before anything is applied."
    override val backupRestorePickHint = "Choose backup file"
    override val backupRestorePreviewTitle = "Restore preview"
    override fun backupRestorePreviewBody(newItems: Int, updatedItems: Int, duplicates: Int) =
        "$newItems new items will be added, $updatedItems updated, and $duplicates duplicates skipped."
    override val backupRestoreApply = "Apply"
    override val backupRestoreDone = "Restore finished."
    override fun backupRestoreDoneBody(credentials: Int, sessions: Int, sites: Int) =
        "$credentials sign-ins, $sessions session versions and $sites sites restored."
    override val backupWrongPassword = "That file password is wrong."
    override val backupNotABackup = "This file isn’t a MarbleBr backup."
    override val backupEmpty = "There’s nothing to back up yet."
    override val backupSavedTo = "Backup file is ready."
    override val backupFailed = "Couldn’t create the backup file."
    override val backupCantShare = "No app was found to save the file."
    override val backupIncludeCredentials = "Passwords"
    override val backupIncludeSessions = "Session versions"
    override val backupIncludeSites = "Site settings"
    override val pulseTitle = "Session pulse"
    override val pulseDescription = "Suggests local checkpoints from open tabs based on usage and snapshot age; it does not check sign-in."
    override val pulseRunNow = "Save open tabs now"
    override val pulseEmpty = "No suggestion or matching open tab is available."
    override fun pulseSiteLine(domain: String, urgency: String) = "$domain · $urgency"
    override val pulseUrgencyLow = "calm"
    override val pulseUrgencyNormal = "normal"
    override val pulseUrgencyHigh = "needed soon"
    override val pulseUrgencyCritical = "old local copy"
    override val pulseDone = "Local copy saved; sign-in has not been verified."
    override val permissionPopups = "Open pop-up windows"
    override val permissionPopupTitle = "This site wants to open a new window"
    override val permissionPopupBody = "Allow it if you are expecting a payment, Google sign-in or a file preview."
    override val permissionAllow = "Allow"
    override val permissionBlock = "Block"
    override val permissionAskTitle = "Device access"
    override fun permissionRequestBody(domain: String, permission: String) =
        "$domain wants to use $permission."
    override val permissionAndroidRequired = "This also needs a phone permission."
    override val permissionDenied = "Permission denied."

    override val proxyHostInvalid = "Proxy address is invalid (for example 127.0.0.1 or proxy.example.com)"
    override val proxyPortInvalid = "Port must be between 1 and 65535."
    override val proxyRestartRequired = "To apply the proxy, close and reopen the app."

    override val suggestionHistory = "Recent"
    override val suggestionBookmark = "Bookmark"
    override val suggestionMostVisited = "Most visited"
    override val suggestionOpenTabs = "Open tab"
    override val suggestionsTitle = "Suggestions"
    override val suggestionsEmpty = "No suggestions found."

    override val libraryTitle = "Library"
    override val libraryBookmarksTab = "Bookmarks"
    override val libraryHistoryTab = "History"
    override val librarySearchHint = "Search bookmarks and history…"
    override val libraryAllBookmarks = "All bookmarks"
    override val libraryAddBookmark = "Add bookmark"
    override val libraryEditBookmark = "Edit bookmark"
    override val librarySave = "Save"
    override val libraryBookmarkTitle = "Title"
    override val libraryBookmarkUrl = "Address"
    override val libraryBookmarkFolder = "Folder"
    override val libraryNewFolder = "New folder"
    override val libraryFolderName = "Folder name"
    override val libraryMoveToFolder = "Move to folder"
    override val libraryDeleteBookmark = "Delete bookmark"
    override val libraryDeleteBookmarkBody = "This bookmark will be removed for good."
    override val libraryOpenInNewTab = "Open in new tab"
    override val libraryDeleteHistoryItem = "Remove from history"
    override val libraryDeleteHistoryBody = "This address will be removed from history; bookmarks stay untouched."
    override val libraryDeleteSiteHistory = "Remove all visits to this site"
    override val libraryClearHistoryTitle = "Clear history"
    override val libraryClearHistoryBody = "All browsing history will be cleared. Bookmarks and sign-ins stay untouched."
    override val libraryToday = "Today"
    override val libraryYesterday = "Yesterday"
    override val libraryThisWeek = "This week"
    override val libraryThisMonth = "This month"
    override val libraryOlder = "Older"
    override val librarySort = "Sort"
    override val libraryMoreOptions = "More options"
    override val librarySortDate = "Date"
    override val librarySortVisits = "Visits"
    override val librarySortTitle = "Title"
    override val libraryEmptyBookmarksSearch = "No matching bookmarks."
    override val libraryEmptyHistorySearch = "No matching history."
    override val libraryBookmarkSaved = "Bookmark saved"
    override val libraryBookmarkUpdated = "Bookmark updated"
    override val libraryBookmarkDeleted = "Bookmark deleted"
    override val libraryFolderCreated = "Folder created"
    override val libraryHistoryDeleted = "Removed from history"
    override val librarySiteHistoryDeleted = "Removed all visits to this site"
    override val libraryInvalidBookmark = "Enter a title and a complete address."
    override val libraryAllFolders = "All folders"
    override val libraryFolderBookmarks = "Bookmarks in this folder"
    override val libraryClearOlder = "Clear old history"
    override val libraryClearOlderBody = "Visits older than the selected range are removed; bookmarks stay untouched."
    override val libraryClearOlder7 = "Older than 7 days"
    override val libraryClearOlder30 = "Older than 30 days"

    override val vaultAdvancedTools = "More tools"
    override val vaultAdvancedToolsBody = "Backup, session pulse and expiry guardian"
    override val vaultHideAdvanced = "Hide tools"
    override val vaultSignedInTitle = "Always stay signed in"
    override val vaultSignedInBody = "New sites start protected; manage the sign-in setting for existing sites individually."
    override val pulseToggle = "Smart pulse"
}
