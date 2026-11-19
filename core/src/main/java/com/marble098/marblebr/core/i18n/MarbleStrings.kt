package com.marble098.marblebr.core.i18n

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

    // ------------------------------------------- برچسب‌های شمارشی
    fun themeModeLabel(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> themeSystem
        ThemeMode.LIGHT -> themeLight
        ThemeMode.DARK -> themeDark
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
