package com.marble098.marblebr.core.i18n

import com.marble098.marblebr.core.model.BackButtonBehavior
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.ThemeMode
import java.util.Locale

/**
 * همه‌ی متن‌های قابل‌نمایش MarbleBr در یک جا و به دو زبان.
 *
 * متن‌ها عمداً ساده، کوتاه و صمیمی نوشته شده‌اند: هر گزینه می‌گوید «چه کاری می‌کند» و «چه سودی دارد»،
 * بدون اصطلاح فنیِ گیج‌کننده.
 */
abstract class MarbleStrings {

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

    // -------------------------------------------- پیام‌های کوتاه (بنر)
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

    // ----------------------------------------------------- فرمت‌ها
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

    // --------------------------------------------------- صفحه‌ی خانه
    abstract val homeTitle: String
    abstract val homeIntro: String
    abstract val homeSearchHint: String
    abstract val homeRecentTitle: String
    abstract val homeEmptyRecent: String
    abstract val homeShortcutsTitle: String
    abstract val homeEmptyShortcuts: String

    // --------------------------------------------------- امتیاز‌دهی
    abstract val rateTitle: String
    abstract val rateMessage: String
    abstract val rateNow: String
    abstract val rateLater: String
    abstract val rateDontAskAgain: String

    // --------------------------------------------- تنظیمات صفحه‌ی خانه
    abstract val homeScreenTitle: String
    abstract val searchEngineTitle: String
    abstract val searchEngineDescription: String
    abstract val homeShortcutPositionTitle: String
    abstract val shortcutPositionNone: String
    abstract val shortcutPositionTop: String
    abstract val shortcutPositionBottom: String
    abstract val homeSeeAll: String

    // ------------------------------------------- تب‌بندی و نوارهای شیشه‌ای
    abstract val settingsTabAppearance: String
    abstract val settingsTabBrowser: String
    abstract val settingsTabProtection: String
    abstract val settingsTabNetwork: String
    abstract val settingsTabAbout: String
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
    abstract val quickSaveState: String
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
    abstract val quickSiteSignedInFor(days: Int, minutes: Int): String

    // --------------------------------------------- پنل استار دادن به مخزن
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

    // ------------------------------------------- برچسب‌های شمارشی
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
        SessionHealthStatus.SERVER_REVOKED_FALLBACK -> statusNeedsSignIn
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
        HealthReason.UNKNOWN -> reasonUnknown
    }

    /** متنِ انگلیسی/فارسیِ امن برای خواندن کدِ ذخیره‌شده در گزارش رویدادها. */
    fun healthReasonLabelOrNull(code: String): HealthReason? =
        runCatching { HealthReason.valueOf(code) }.getOrNull()

    // ------------------------------------------------- برچسب‌های داخلی
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
    override val appName = "MarbleBr"
    override val back = "بازگشت"
    override val cancel = "بی‌خیال"
    override val ok = "باشه"
    override val later = "بعداً"
    override val close = "بستن"
    override val dismiss = "بستن پیام"

    override val languageTitle = "زبان برنامه"
    override val languageDescription = "با «خودکار»، MarbleBr هم‌زبان گوشی‌ات می‌شود. اگر خواستی، فارسی یا انگلیسی را دستی انتخاب کن."
    override val languageSystem = "خودکار (مثل گوشی)"
    override val languagePersian = "فارسی"
    override val languageEnglish = "English"
    override val appearanceTitle = "ظاهر"
    override val themeModeTitle = "روشن یا تاریک"
    override val themeSystem = "مثل گوشی"
    override val themeLight = "روشن"
    override val themeDark = "تاریک"
    override val dynamicColorTitle = "رنگ‌های خودِ گوشی"
    override val dynamicColorDescription = "روی اندروید ۱۲ و بالاتر، رنگ برنامه با تم گوشی‌ات هماهنگ می‌شود."

    override val addressPlaceholder = "آدرس سایت را بنویس یا سرچ کن"
    override val copyLink = "کپی پیوند"
    override val shareLink = "اشتراک‌گذاری پیوند"
    override val browserMenu = "گزینه‌های بیشتر"
    override val tabsLabel = "تب‌ها"
    override val protectionLabel = "محافظت"
    override val bookmarksLabel = "نشان‌ها"
    override val settingsLabel = "تنظیمات"
    override val bookmarkToggle = "نشان کردن این صفحه"
    override val reload = "بارگذاری دوباره"
    override val goBack = "صفحه قبل"
    override val goForward = "صفحه بعد"
    override val protectionOnChip = "مراقب این سایت هستیم"
    override val protectionOffChip = "مراقبت از این سایت خاموش است"
    override val privateTabChip = "تب خصوصی: چیزی ذخیره نمی‌شود"
    override val menuSaveState = "همین حالا ذخیره کن"
    override val menuSaveStateHint = "یک نسخه امن از ورودت می‌سازیم"
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
    override val reloginConfirm = "بازگردان به حسابم"
    override val reloginDismiss = "نه، خودم وارد می‌شوم"
    override val readyTitle = "آماده‌ی مراقبت هستیم"
    override val readyBody = "کافی است یک آدرس بزنی. ما پشت صحنه مواظبیم که از حسابت بیرون نیفتی."
    override val quickSaveState = "ذخیره وضعیت"
    override val quickEarlierCopies = "نسخه‌های قبلی"
    override val newTab = "تب جدید"
    override val newPrivateTab = "تب خصوصی"
    override val closeTab = "بستن تب"
    override val newTabTitle = "تب جدید"
    override val privateTabTitle = "تب خصوصی"
    override val addressSelectionTitle = "انتخاب متن نوار آدرس"
    override val addressSelectionDescription = "هنگام لمس نوار آدرس، نحوهٔ انتخاب متن را تعیین کنید."
    override val addressSelectAll = "همهٔ آدرس"
    override val addressSelectPart = "بخش لمس‌شده"
    override val addressCursor = "فقط نشانگر"
    override val googleNewTab = "باز کردن گوگل در تب جدید"
    override val googleNewTabDescription = "اگر خاموش باشد، تب جدید صفحهٔ خانه را نشان می‌دهد و هیچ سایتی بارگیری نمی‌شود."
    override val goToAddress = "برو به آدرس"
    override val navigationFailed = "آدرس باز نشد. دوباره تلاش کنید."
    override val stopLoading = "توقف بارگیری"
    override val duplicateTab = "ایجاد نسخهٔ دوم تب"
    override val reopenTab = "بازگشایی تب بسته‌شده"
    override val closeAllTabs = "بستن همهٔ تب‌ها"
    override val confirmCloseAllTabs = "همهٔ تب‌های عادی و خصوصی بسته شوند؟"
    override val searchTabs = "جستجو در تب‌ها"
    override val noTabs = "هیچ تب بازی نیست."
    override val noMatchingTabs = "تبی با این عبارت پیدا نشد."
    override val clearHistory = "پاک کردن تاریخچه"
    override val emptyBookmarks = "هنوز چیزی نشان نکرده‌ای. با آیکن نشان بالای صفحه، هر سایتی را ذخیره کن."
    override val emptyHistory = "اینجا خالی است؛ سایت‌هایی که باز کنی اینجا می‌آیند."
    override val emptyDownloads = "هنوز فایلی دانلود نکرده‌ای."
    override val downloadNotificationText = "در حال دانلود با MarbleBr"

    override val bannerBookmarkAdded = "به نشان‌ها اضافه شد"
    override val bannerBookmarkRemoved = "از نشان‌ها حذف شد"
    override val bannerShortcutAdded = "میانبر ساخته شد؛ از صفحه اصلی بازش کن"
    override val bannerShortcutCreated = "فضای جداگانه‌ی این سایت آماده شد"
    override val bannerSnapshotSaved = "یک نسخه امن از ورودت ذخیره شد"
    override val bannerCacheCleared = "حافظه موقت خالی شد و همچنان وارد حساب‌هایت هستی"
    override val bannerHistoryCleared = "تاریخچه پاک شد"
    override val bannerDownloadStarted = "دانلود شروع شد"
    override val downloadFailed = "دانلود ناموفق بود"
    override val downloadInProgress = "در حال دانلود"
    override val snapshotUnavailable = "برای این صفحه دادهٔ نشست قابل ذخیره در دسترس نیست."
    override val bannerDesktopOn = "حالا نسخه دسکتاپ را می‌بینی"
    override val bannerDesktopOff = "حالا نسخه موبایل را می‌بینی"

    override val settingsTitle = "تنظیمات"
    override val settingsIntro = "هر چیزی را که لازم داری ساده و کوتاه اینجا تنظیم کن."
    override val protectionSectionTitle = "اینکه وارد بمانی"
    override val protectionSectionBody = "این گزینه‌ها تعیین می‌کنند MarbleBr چطور کمکت کند از حساب‌ها بیرون نیفتی."
    override val globalNeverSignOutTitle = "مراقبت از سایت‌های جدید"
    override val globalNeverSignOutDescription = "برای هر سایت تازه، محافظت از ورود به‌طور خودکار روشن باشد."
    override val biometricTitle = "قفل اثر انگشت"
    override val biometricDescription = "برای دیدن نسخه‌های ذخیره‌شده، اثر انگشت یا رمز گوشی لازم باشد."
    override val keepAliveTitle = "زنده نگه‌داشتن در پس‌زمینه"
    override val keepAliveDescription = "سایت‌هایی که زود بیرونت می‌کنند، هر چند وقت یک‌بار بی‌صدا بازدید می‌شوند."
    override val credentialTitle = "ورود سریع با اثر انگشت"
    override val credentialDescription = "اگر خود سایت نشستت را بست، با Passkey یا رمز ذخیره‌شده سریع برمی‌گردی."
    override val adBlockTitle = "بستن تبلیغ و ردیاب"
    override val adBlockDescription = "صفحه‌ها سبک‌تر و سریع‌تر باز می‌شوند و کمتر ردیابی می‌شوی."
    override val freeSpaceTitle = "خالی کردن حافظه موقت"
    override val freeSpaceDescription = "فقط فایل‌های اضافی پاک می‌شوند؛ هیچ حسابی خارج نمی‌شود."
    override val freeSpaceButton = "خالی کن"
    override val freeSpaceDone = "حافظه موقت خالی شد"
    override val networkTitle = "شبکه (برای کاربران حرفه‌ای)"
    override val networkBody = "اگر DNS امن یا پروکسی خاصی می‌خواهی، از اینجا تنظیم کن. برای بیشتر آدم‌ها لازم نیست."
    override val dnsLabel = "DNS امن"
    override val dnsCustomHint = "آدرس سرور (با https شروع شود)"
    override val proxyLabel = "پروکسی"
    override val proxyHostHint = "آدرس یا IP پروکسی"
    override val proxyPortHint = "پورت"
    override val saveNetwork = "ذخیره تنظیمات شبکه"
    override val networkSaved = "تنظیمات شبکه ذخیره شد"
    override val aboutTitle = "درباره"

    override val vaultTitle = "مراقبت از حساب‌ها"
    override val vaultIntro = "اینجا می‌بینی MarbleBr از کدام سایت‌ها محافظت می‌کند و چه نسخه‌هایی ذخیره کرده است."
    override val vaultSecurityTitle = "همه‌چیز روی همین گوشی می‌ماند"
    override val vaultSecurityBody = "نسخه‌های امن ورودت با کلید سخت‌افزاری گوشی رمز می‌شوند و جایی فرستاده نمی‌شوند."
    override val unlockVault = "باز کردن با اثر انگشت"
    override val biometricPromptTitle = "قفل مراقبت از حساب‌ها"
    override val biometricPromptSubtitle = "برای دیدن نسخه‌های ذخیره‌شده، هویتت را تأیید کن"
    override val biometricCancelled = "تأیید هویت لغو شد"
    override val reloginUnavailable = "برای این سایت ورود ذخیره‌شده‌ای پیدا نشد"
    override val reloginUnsupportedType = "این نوع ورود پشتیبانی نمی‌شود"
    override val reloginPromptSubtitle = "سایت نشستت را بست؛ با اثر انگشت دوباره وارد شو"
    override val sitesSectionTitle = "سایت‌ها"
    override val sitesSectionBody = "هر سایت فضای جدا دارد؛ خروج از یکی به بقیه کاری ندارد."
    override val protectThisSite = "مراقبت از این سایت"
    override val signOutNow = "کامل خارج شو"
    override val signOutConfirmTitle = "از این سایت خارج می‌شوی؟"
    override val signOutConfirmBody = "ورود فعلی و همه‌ی نسخه‌های ذخیره‌شده‌ی این سایت پاک می‌شوند. بقیه سایت‌ها دست‌نخورده می‌مانند."
    override val signOutConfirmAction = "بله، خارج شو"
    override val timeMachineTitle = "برگشت به عقب"
    override val timeMachineBody = "یک نسخه را انتخاب کن تا ورودت به همان زمان برگردد."
    override val emptySnapshots = "هنوز نسخه‌ای ساخته نشده. وقتی سایت باز کنی، خودکار ساخته می‌شود."
    override val emptySites = "هنوز سایتی اضافه نشده. کافی است یک آدرس باز کنی."
    override val backgroundSnapshotLabel = "نسخه پس‌زمینه"
    override val restore = "برگردان"
    override val healthLogTitle = "چه اتفاقی افتاد؟"
    override val healthLogBody = "هر وقت چیزی برای ورودت پیش بیاید، اینجا با زبان ساده ثبت می‌شود."
    override val feedbackRestoreFailed = "نتوانستیم این نسخه را بخوانیم"
    override val gotIt = "باشه"

    override fun tabsCount(count: Int) = "$count تب باز"
    override fun adsBlocked(count: Int) = "$count تبلیغ و ردیاب بسته شد"
    override fun downloadsMenu(count: Int) = "دانلودها ($count)"
    override fun openTabsTitle(count: Int) = "تب‌های باز ($count)"
    override fun bookmarksCount(count: Int) = "نشان‌ها ($count)"
    override fun historyCount(count: Int) = "تاریخچه ($count)"
    override fun savedPath(path: String) = "ذخیره‌شده در: $path"
    override fun reloginBody(domain: String) =
        "سایت $domain تو را از حساب خارج کرده است. با اثر انگشت یا Passkey می‌توانی سریع برگردی."
    override fun adBlockChip(count: Int) = "تبلیغ بسته‌شده: $count"
    override fun sitesCount(count: Int) = "سایت‌ها ($count)"
    override fun keepAliveEvery(minutes: Int) = "بی‌صدا بازدید کن (هر $minutes دقیقه)"
    override fun keepSignedInFor(days: Int) = "ورود را $days روز نگه دار"
    override fun signOutDialogTitle(domain: String) = "از $domain خارج می‌شوی؟"
    override fun signOutDialogBody(domain: String) =
        "ورود فعلی و نسخه‌های ذخیره‌شده‌ی $domain پاک می‌شوند. بقیه سایت‌ها دست‌نخورده می‌مانند."
    override fun snapshotLine(date: String, cookies: Int, storageKeys: Int) =
        "ذخیره‌شده در $date · $cookies کوکی · $storageKeys کلید حافظه"
    override fun feedbackProtection(domain: String, enabled: Boolean) =
        if (enabled) "مراقبت از $domain روشن شد" else "مراقبت از $domain خاموش شد"
    override fun feedbackCookieDays(domain: String, days: Int) = "ورود به $domain حالا $days روز می‌ماند"
    override fun feedbackKeepAliveOn(domain: String, minutes: Int) =
        "برای $domain هر $minutes دقیقه بی‌صدا زنده‌سازی انجام می‌شود"
    override fun feedbackKeepAliveOff(domain: String) = "زنده‌سازی خودکار $domain خاموش شد"
    override fun feedbackRestored(label: String, cookies: Int) = "نسخه «$label» برگشت ($cookies کوکی)"
    override fun feedbackSignedOut(domain: String) = "از $domain خارج شدی و نسخه‌هایش پاک شد"
    override fun aboutBody(version: String) =
        "MarbleBr نسخه $version — ساخته شده که یک بار وارد شوی و بمانی. بدون ردیاب، بدون تبلیغ."
    override fun reloginAutoTitle(domain: String) = "ورود دوباره به $domain"
    override fun autoSnapshotLabel(domain: String) = "نسخه خودکار — $domain"
    override fun manualSnapshotLabel(domain: String) = "نسخه دستی — $domain"
    override fun pwaShortcutLongLabel(title: String) = "$title (بدون خروج خودکار)"

    override val homeTitle = "صفحه‌ی خانه"
    override val homeIntro = "ابتدا را از اینجا آغاز کنید"
    override val homeSearchHint = "جستجو یا آدرس سایت را بنویسید…"
    override val homeRecentTitle = "بازدیدهای اخیر"
    override val homeEmptyRecent = "هنوز بازدیدی ندارید"
    override val homeShortcutsTitle = "میانبرهای شما"
    override val homeEmptyShortcuts = "هنوز میانبری ندارید. سایت‌های مورد علاقه را از مرورگر ذخیره کنید."

    override val rateTitle = "به MarbleBr استار بدهید"
    override val rateMessage =
        "اگر MarbleBr به شما مفید بوده است، لطفاً به ما استار بدهید تا بتوانیم برایتان بهتر شویم."
    override val rateNow = "بله، با خوشحالی"
    override val rateLater = "بعداً"
    override val rateDontAskAgain = "نخواهم داد، دوباره نشان نده"

    override val homeScreenTitle = "تنظیمات صفحه‌ی خانه"
    override val searchEngineTitle = "موتور جستجو"
    override val searchEngineDescription =
        "موتور جستجوی پیش‌فرض برای نوار آدرس و صفحه‌ی خانه را انتخاب کنید."
    override val homeShortcutPositionTitle = "محل دکمه‌ی میانبر صفحه‌ی خانه"
    override val shortcutPositionNone = "هیچ‌کدام"
    override val shortcutPositionTop = "بالا"
    override val shortcutPositionBottom = "پایین"

    override val homeSeeAll = "مشاهده همه »"

    override val settingsTabAppearance = "ظاهر"
    override val settingsTabBrowser = "مرورگر"
    override val settingsTabProtection = "محافظت"
    override val settingsTabNetwork = "شبکه"
    override val settingsTabAbout = "درباره"
    override val chromeBarStyleTitle = "شکل نوارهای بالا و پایین"
    override val chromeBarStyleDescription = "«شناور» یعنی نوارها کاملاً شیشه‌ای و بی‌پس‌زمینه‌اند و صفحه از پشتشان دیده می‌شود."
    override val chromeSolid = "ساده و مات"
    override val chromeGlass = "شیشه‌ای مات"
    override val chromeFloating = "شیشه‌ای شناور"
    override val backBehaviorTitle = "دکمهٔ بازگشت"
    override val backBehaviorDescription = "انتخاب کن وقتی دکمهٔ بازگشت گوشی را می‌زنی چه اتفاقی بیفتد."
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
    override val tabsBehaviorTitle = "رفتار تب‌ها"
    override val askBeforeClosingTabs = "تأیید قبل از بستن تب"
    override val askBeforeClosingTabsDescription = "اگر یک تب باز باشد و بخواهی ببندیش، اول از تو می‌پرسیم."
    override val closeTabConfirmTitle = "این تب بسته شود؟"
    override fun closeTabConfirmBody(title: String) = "تب «$title» بسته می‌شود؛ اگر خواستی با «بازگشایی تب بسته‌شده» برمی‌گردد."

    override val quickActionsTitle = "میانبرهای سریع"
    override val quickCopyLink = "کپی لینک"
    override val quickShare = "اشتراک"
    override val quickScreenshot = "عکس از صفحه"
    override val quickFindInPage = "جستجو در صفحه"
    override val quickDesktop = "دسکتاپ"
    override val quickPrivate = "تب خصوصی"
    override val quickSaveState = "ذخیره ورود"
    override val quickTimeMachine = "برگشت به عقب"
    override val quickDownloads = "دانلودها"
    override val quickClearCache = "خالی کردن کش"
    override val quickAddShortcut = "میانبر خانه"
    override val quickRefresh = "نوسازی"
    override val quickHome = "خانه"
    override val quickTabs = "تب‌ها"
    override val quickSettings = "تنظیمات"
    override val quickBookmarks = "نشان‌ها"
    override val quickSignOut = "خروج کامل"
    override val quickNewTab = "تب تازه"
    override fun quickSiteSignedInFor(days: Int, minutes: Int) =
        "این سایت $days روز وارد می‌ماند · هر $minutes دقیقه بی‌صدا زنده می‌شود"

    override val starRepoBoldLine = "به MarbleBr استار بده ⭐"
    override val starRepoHeadline = "یک استار، هزار امید"
    override val starRepoSubline =
        "اگر MarbleBr کارت را راه انداخت، با یک استار در گیت‌هاب به ما انرژی بده تا سریع‌تر بهترش کنیم."
    override val starRepoButton = "همین حالا استار می‌دهم ⭐"
    override val starRepoLater = "باشه، بعداً"
    override val starRepoNever = "دیگر نشان نده"
    override val starRepoTelegram = "کانال تلگرام ما"
    override val starRepoWeb = "دیدن مخزن در گیت‌هاب"
    override val starRepoThanks = "ممنون که با ما هستی 💙"
    override val starRepoBadgeFree = "رایگان"
    override val starRepoBadgePrivate = "بدون ردیاب"
    override val starRepoBadgeFast = "سبک و سریع"
    override val starRepoBadgeBilingual = "دوزبانه"

    override val dnsSystem = "خودکار"
    override val dnsCustom = "سفارشی"
    override val proxyNone = "بدون پروکسی"
    override val statusHealthy = "سالم"
    override val statusProtected = "مراقبت فعال"
    override val statusHealing = "در حال ترمیم"
    override val statusRecovered = "ترمیم شد"
    override val statusNeedsSignIn = "نیاز به ورود دوباره"
    override val statusProtectionOff = "مراقبت خاموش"
    override val reasonProtectionOff = "برای این سایت مراقبت خاموش است."
    override val reasonHealthy = "همه‌چیز مرتب است؛ همچنان وارد حسابی."
    override val reasonUnauthorized = "سایت درخواست تو را رد کرد."
    override val reasonLoginRedirect = "سایت تو را به صفحه ورود فرستاد."
    override val reasonCookieMissing = "کوکی ورودت ناپدید شد."
    override val reasonStorageCleared = "حافظه‌ی محلی سایت پاک شد."
    override val reasonHealed = "نشستت را خودکار برگرداندیم؛ لازم نیست کاری کنی."
    override val reasonServerRevoked = "سایت نشست را بست؛ با اثر انگشت یا Passkey سریع برگرد."
    override val reasonUnknown = "اتفاقی برای نشست افتاد که بررسی شد."
}

private object EnglishStrings : MarbleStrings() {
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
    override val reloginConfirm = "Get me back in"
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
    override val keepAliveTitle = "Keep sites alive in the background"
    override val keepAliveDescription = "Sites that log you out early get a quiet visit every now and then."
    override val credentialTitle = "Quick sign-in with fingerprint"
    override val credentialDescription = "If a site ends your session, get back in with a passkey or saved password."
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
    override val vaultSecurityBody = "Saved logins are locked with your phone’s hardware key and never sent anywhere."
    override val unlockVault = "Unlock with fingerprint"
    override val biometricPromptTitle = "Account protection lock"
    override val biometricPromptSubtitle = "Confirm it’s you to see saved copies"
    override val biometricCancelled = "Identity check cancelled"
    override val reloginUnavailable = "No saved sign-in was found for this site"
    override val reloginUnsupportedType = "This kind of sign-in isn’t supported"
    override val reloginPromptSubtitle = "The site ended your session — confirm with your fingerprint to get back in"
    override val sitesSectionTitle = "Sites"
    override val sitesSectionBody = "Each site has its own space. Signing out of one won’t touch the others."
    override val protectThisSite = "Protect this site"
    override val signOutNow = "Sign out completely"
    override val signOutConfirmTitle = "Sign out of this site?"
    override val signOutConfirmBody = "Your current login and every saved copy of this site will be deleted. Other sites stay untouched."
    override val signOutConfirmAction = "Yes, sign me out"
    override val timeMachineTitle = "Go back in time"
    override val timeMachineBody = "Pick a copy to jump your login back to that moment."
    override val emptySnapshots = "No copies yet. They’re created automatically while you browse."
    override val emptySites = "No sites yet. Just open an address."
    override val backgroundSnapshotLabel = "Background copy"
    override val restore = "Restore"
    override val healthLogTitle = "What happened?"
    override val healthLogBody = "Anything that affects your logins is listed here in plain words."
    override val feedbackRestoreFailed = "We couldn’t read that copy"
    override val gotIt = "Got it"

    override fun tabsCount(count: Int) = "$count open tabs"
    override fun adsBlocked(count: Int) = "$count ads and trackers blocked"
    override fun downloadsMenu(count: Int) = "Downloads ($count)"
    override fun openTabsTitle(count: Int) = "Open tabs ($count)"
    override fun bookmarksCount(count: Int) = "Bookmarks ($count)"
    override fun historyCount(count: Int) = "History ($count)"
    override fun savedPath(path: String) = "Saved to: $path"
    override fun reloginBody(domain: String) =
        "$domain signed you out. Use your fingerprint or a passkey to get back in quickly."
    override fun adBlockChip(count: Int) = "Ads blocked: $count"
    override fun sitesCount(count: Int) = "Sites ($count)"
    override fun keepAliveEvery(minutes: Int) = "Quiet check-in (every $minutes min)"
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
        "$domain gets a quiet check-in every $minutes minutes"
    override fun feedbackKeepAliveOff(domain: String) = "Background check-ins for $domain are off"
    override fun feedbackRestored(label: String, cookies: Int) = "Restored “$label” ($cookies cookies)"
    override fun feedbackSignedOut(domain: String) = "Signed out of $domain and deleted its copies"
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
    override val homeShortcutPositionTitle = "Home screen shortcut button position"
    override val shortcutPositionNone = "None"
    override val shortcutPositionTop = "Top"
    override val shortcutPositionBottom = "Bottom"

    override val homeSeeAll = "See all →"

    override val settingsTabAppearance = "Appearance"
    override val settingsTabBrowser = "Browser"
    override val settingsTabProtection = "Protection"
    override val settingsTabNetwork = "Network"
    override val settingsTabAbout = "About"
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
    override val quickSaveState = "Save login"
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

    override val dnsSystem = "Automatic"
    override val dnsCustom = "Custom"
    override val proxyNone = "No proxy"
    override val statusHealthy = "Healthy"
    override val statusProtected = "Protection on"
    override val statusHealing = "Healing"
    override val statusRecovered = "Restored"
    override val statusNeedsSignIn = "Needs a new sign-in"
    override val statusProtectionOff = "Protection off"
    override val reasonProtectionOff = "Protection is off for this site."
    override val reasonHealthy = "Everything is fine — you’re still signed in."
    override val reasonUnauthorized = "The site rejected the request."
    override val reasonLoginRedirect = "The site sent you to its login page."
    override val reasonCookieMissing = "Your login cookie disappeared."
    override val reasonStorageCleared = "The site’s local storage was wiped."
    override val reasonHealed = "We restored your session automatically — nothing for you to do."
    override val reasonServerRevoked = "The site ended your session — get back in with your fingerprint or a passkey."
    override val reasonUnknown = "Something touched your session, so we checked it."
}
