package com.marble098.marblebr.feature.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.AutofillCandidate
import com.marble098.marblebr.core.model.LoginMethod
import com.marble098.marblebr.core.model.PasswordStrengthLevel
import com.marble098.marblebr.core.model.SiteAdditionPlan
import com.marble098.marblebr.core.model.SiteBlocker
import com.marble098.marblebr.core.model.SiteRoute
import com.marble098.marblebr.core.designsystem.MarbleGlassIconButton
import com.marble098.marblebr.core.model.OfferReason
import com.marble098.marblebr.core.model.PasswordNotice
import com.marble098.marblebr.core.model.isTransient
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.model.BackNavigationResolver
import com.marble098.marblebr.core.model.BackTarget
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.DownloadState
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.SitePermissionProfile
import com.marble098.marblebr.core.model.MarbleLinks
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.core.preferences.AddressSelectionMode
import com.marble098.marblebr.core.preferences.BrowserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.mozilla.geckoview.GeckoView

/**
 * صفحهٔ مرورگر: نوار بالا، نوار تب پایین، صفحهٔ خانه و شیت‌های کمکی.
 *
 * ### چرا باز شدن کیبورد قبلاً لگ داشت؟
 * کل صفحه در یک `Scaffold` با `Modifier.imePadding()` قرار داشت. با باز شدن کیبورد،
 * ارتفاعِ دسترس‌پذیر تغییر می‌کرد، این تغییر به **همهٔ** فرزندان (از جمله شیت‌های سنگین و
 * `GeckoView`) منتقل می‌شد و هر فریم یک بازچیدمان (relayout) کامل اتفاق می‌افتاد. علاوه بر
 * این، در هر بازترکیب یک `FocusRequester`/`onFocusChanged` تازه ساخته می‌شد.
 *
 * اصلاح ریشه‌ای:
 * ۱. `imePadding` فقط روی محتوای صفحهٔ خانه/شیت‌هایی که ورودی متنی دارند اعمال می‌شود،
 *    نه روی کل Scaffold و نه روی سطح `GeckoView`.
 * ۲. وضعیت «فوکوس» در یک `remember` پایدار نگه داشته می‌شود و مقدارش با `rememberUpdatedState`
 *    به لایه‌های پایین می‌رسد تا بازترکیب نوار ناشی از تایپ، نمای وب را دوباره نسازد.
 * ۳. نوار بالا با `key(activeTab?.id)` ایزوله است و `AndroidView` صفحهٔ وب از بازترکیب‌های
 *    نوار آدرس مصون می‌ماند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenVaultDashboard: (containerId: String?) -> Unit,
    onOpenSettings: () -> Unit,
    renderGeckoWebView: Boolean = true,
    /**
     * قفلِ بیومتریکِ اپ را می‌طلبد و در صورتِ موفقیت [onSuccess] را صدا می‌زند.
     *
     * چرا از بیرون تزریق می‌شود؟ چون `BiometricPrompt` به `FragmentActivity` نیاز دارد و لایهٔ
     * قابلیت نباید به Activity وابسته شود. پیش‌فرضِ خالی، پیش‌نمایش و آزمون را ساده می‌کند.
     */
    onRequestBiometricUnlock: (title: String, onSuccess: () -> Unit) -> Unit = { _, onSuccess -> onSuccess() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val siteAccounts by viewModel.siteAccounts.collectAsState()
    val savePrompt by viewModel.savePrompt.collectAsState()
    val passwordNotice by viewModel.passwordNotice.collectAsState()
    val codeFieldAvailable by viewModel.codeFieldAvailable.collectAsState()
    val passwordCount by viewModel.storedPasswordCount.collectAsState()
    val protectedSiteCount by viewModel.protectedSiteCount.collectAsState()
    val pulseDomains by viewModel.pulseDomains.collectAsState()
    val blockedAdsCount by viewModel.adBlocker.blockedCount.collectAsState()
    val addressSuggestions by viewModel.addressSuggestions.collectAsState()
    val strings = LocalMarbleStrings.current
    // خبرهای کوتاهِ ورود (کد نوشته شد، فرم پر شد، …) در نوارِ بالای صفحه دیده می‌شوند،
    // نه در دیالوگ؛ سپس پیام پاک می‌شود تا تکرار نشود.
    LaunchedEffect(passwordNotice) {
        val notice = passwordNotice ?: return@LaunchedEffect
        if (!notice.isTransient) return@LaunchedEffect
        val text = when (notice) {
            is PasswordNotice.CodeFilled -> strings.codeFilled
            PasswordNotice.CodeNotFound -> strings.codeUnavailableTitle
            PasswordNotice.CodeFieldNotFound -> strings.codeManualHint
            is PasswordNotice.Filled -> strings.autofillUseNow
            PasswordNotice.NoStoredLogin -> strings.saveLoginNoStored
            else -> strings.saveLoginSaved
        }
        viewModel.showBanner(text)
        viewModel.consumePasswordNotice()
    }

    // اگر کاربر «رمزِ یکبارمصرف» را روشن کرده باشد، همین‌جا (و فقط همین‌جا) اجازه‌های اختیاری
    // پیامک/اعلان پرسیده می‌شوند؛ ردکردن باعثِ هیچ خطایی نمی‌شود.
    LoginAssistPermissionEffect(
        otpAssistEnabled = prefs.otpAssistEnabled,
        onSmsPermissionResult = { granted ->
            if (granted) viewModel.startCodeListening(force = true) else viewModel.stopCodeListening()
        },
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    val showHome = uiState.showHome
    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    var backPromptVisible by remember { mutableStateOf(false) }
    var pendingCloseTabId by remember { mutableStateOf<String?>(null) }

    val activeTab = uiState.activeTab
    var pdfTargetTabId by rememberSaveable { mutableStateOf<String?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val tabId = pdfTargetTabId
        pdfTargetTabId = null
        if (uri != null && tabId != null) viewModel.savePageAsPdf(tabId, uri)
    }
    val requestPdf: (String, String) -> Unit = { tabId, title ->
        pdfTargetTabId = tabId
        val safeTitle = title.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(70).ifBlank { "page" }
        pdfPicker.launch("$safeTitle.pdf")
    }
    // «میان‌بر صفحهٔ خانه» دیگر یک دکمهٔ مستقلِ شناور روی محتوا نیست؛ فقط کاربرانی که
    // صریحاً حالت شناور را انتخاب کرده‌اند آن را می‌بینند (پیش‌فرض: داخل کاشی‌های میان‌بر).
    val hasHomeShortcut = showHome && prefs.homeShortcutPosition == HomeShortcutPosition.FLOATING
    val hasPage = !activeTab?.url.isNullOrBlank()
    val focusManager = LocalFocusManager.current
    val clipboardContext = LocalContext.current

    // پایدار نگه‌داشتن مسیر ارسال: از ساختِ لامبدای تازه در هر بازترکیب جلوگیری می‌کند.
    val submitState = rememberUpdatedState<(String) -> Unit> { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            // پس از ارسال، پیشنهادها نباید در چرخهٔ بعدی دوباره ظاهر شوند.
            viewModel.onAddressFocusChanged(false)
            viewModel.onAddressQueryChanged("")
            viewModel.navigateToInput(query)
        }
    }
    val submit: (String) -> Unit = { query -> submitState.value(query) }

    // دکمهٔ بازگشت: رفتارش را کاربر در تنظیمات انتخاب می‌کند.
    val backTarget = BackNavigationResolver.resolve(
        behavior = prefs.backButtonBehavior,
        isOnHome = showHome,
        canGoBack = activeTab?.canGoBack == true,
        tabCount = uiState.tabs.size,
        canExitApp = showHome,
    )
    val activity = LocalContext.current.findActivity()
    val exitBack: () -> Unit = { if (activity == null || !activity.moveTaskToBack(true)) viewModel.showHome() }
    BackHandler(enabled = true) {
        when (backTarget) {
            BackTarget.PAGE_BACK -> viewModel.goBack()
            BackTarget.HOME -> viewModel.showHome()
            BackTarget.EXIT -> exitBack()
            BackTarget.ASK -> backPromptVisible = true
        }
    }
    val isBookmarked = activeTab != null && bookmarks.any { it.url == activeTab.url }

    // نوارهای بالا و پایین «ثابت» هستند و محتوای سایت دقیقاً بین آن دو جا می‌گیرد.
    //
    // چرا عوض شد؟ پیش‌تر GeckoView تمام صفحه را می‌گرفت و نوارها با zIndex روی آن شناور
    // بودند؛ نتیجه این بود که هدر و فوترِ خیلی از سایت‌ها زیر نوار بالا/پایین پنهان می‌ماند
    // و کاربر نمی‌توانست روی آن‌ها بزند (شکایت اصلی: «با سایت‌ها نمی‌شه کار کرد»). حالا نوار
    // بالا و پایین هر کدام فضای خودشان را دارند و صفحهٔ سایت فقط در ناحیهٔ میانی رندر می‌شود،
    // در حالی که هر دو نوار همیشه سرِ جای خودشان میخ‌شده‌اند.
    val menuState = BrowserMenuState(
        domain = if (showHome) "" else activeTab?.url?.let(::siteDomain).orEmpty(),
        displayName = activeTab?.title.orEmpty(),
        isPrivate = activeTab?.isPrivate == true,
        isProtected = uiState.activeAccountForCurrentSite()?.neverSignOutEnabled == true,
        isDesktopMode = activeTab?.isDesktopMode == true,
        hasPage = hasPage && !showHome,
        canManageSiteAccounts = uiState.activeAccountForCurrentSite() != null,
        downloadsCount = downloads.size,
        hasSavedLogin = uiState.hasStoredLoginForCurrentSite,
        acceptsOneTimeCode = codeFieldAvailable,
        isLoginCompatibilityOn = activeTab?.loginCompatibility == true,
    )
    val menuActions = BrowserMenuActions(
        onCopyLink = {
            val url = activeTab?.url
            if (!url.isNullOrBlank()) {
                copyTextToClipboard(context = clipboardContext, label = strings.copyLink, text = url)
            }
        },
        onShareLink = {
            val url = activeTab?.url
            if (!url.isNullOrBlank()) {
                val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                clipboardContext.startActivity(Intent.createChooser(share, strings.shareLink))
            }
        },
        onFindInPage = { viewModel.toggleFindInPage(true) },
        onToggleDesktop = viewModel::toggleDesktopMode,
        onSaveState = viewModel::captureManualSnapshotNow,
        onOpenTimeMachine = { onOpenVaultDashboard(activeTab?.containerId) },
        onInstallShortcut = viewModel::installCurrentSiteAsPwa,
        onDuplicateTab = viewModel::duplicateTab,
        onAutofillLogin = viewModel::requestAutofill,
        onFillOneTimeCode = viewModel::fillOneTimeCodeNow,
        onOpenDownloads = { showDownloadsSheet = true },
        onFreeSpace = viewModel::clearCacheWithoutTouchingSessions,
        onOpenSettings = onOpenSettings,
        onReadAloud = viewModel::startReadAloud,
        onReaderMode = viewModel::toggleReaderMode,
        onSitePermissions = { viewModel.toggleSitePermissionsPanel(true) },
        onSiteAccounts = { showAccounts = true },
        onSavePdf = { activeTab?.let { requestPdf(it.id, it.title) } },
        onSharePage = { viewModel.shareCurrentPage() },
        onBookmarkPage = { viewModel.toggleBookmarkCurrentPage() },
        onToggleLoginCompatibility = viewModel::toggleLoginCompatibility,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BrowserTopBar(
            style = prefs.chromeBarStyle,
            showHome = showHome,
            activeTab = activeTab,
            url = if (showHome) "" else activeTab?.url.orEmpty(),
            isBookmarked = isBookmarked,
            canBookmark = hasPage && activeTab?.isPrivate != true,
            addressMode = prefs.addressSelectionMode,
            isLoading = activeTab?.isLoading == true,
            progress = activeTab?.progress ?: 0,
            isFindInPageVisible = uiState.isFindInPageVisible,
            findQuery = uiState.findQuery,
            findMatchIndex = activeTab?.findMatchIndex ?: 0,
            findTotalMatches = activeTab?.findTotalMatches ?: 0,
            bannerMessage = uiState.bannerMessage,
            blockedAdsCount = blockedAdsCount.toInt(),
            showAdChip = showHome && blockedAdsCount > 0,
            menuExpanded = showOverflowMenu,
            menuState = menuState,
            menuActions = menuActions,
            onSubmit = submit,
            onBookmark = viewModel::toggleBookmarkCurrentPage,
            onQuickActions = { showQuickActions = true },
            onPreviewOverflow = { showOverflowMenu = true },
            onDismissMenu = { showOverflowMenu = false },
            onFindQueryChanged = { viewModel.updateFindQuery(it, backwards = false) },
            onFindNext = { viewModel.updateFindQuery(uiState.findQuery, backwards = false) },
            onFindPrevious = { viewModel.updateFindQuery(uiState.findQuery, backwards = true) },
            onFindClose = { viewModel.toggleFindInPage(false) },
            onDismissBanner = viewModel::dismissBanner,
            onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
            suggestions = addressSuggestions,
            showSuggestions = uiState.addressFocused &&
                uiState.addressQuery.isNotBlank() &&
                addressSuggestions.isNotEmpty(),
            onPickSuggestion = { picked ->
                viewModel.onAddressFocusChanged(false)
                viewModel.onAddressQueryChanged("")
                submit(picked)
            },
            onAddressQueryChanged = viewModel::onAddressQueryChanged,
            onAddressFocusChanged = viewModel::onAddressFocusChanged,
        )

        SitePermissionPromptHost(
            pending = uiState.pendingPermission,
            onResolve = viewModel::resolvePendingPermission,
        )

        // نشانگر «حالت مطالعه روشن است»: یک نوار نازک با دکمهٔ خاموش‌کردن، تا کاربر بداند
        // ظاهرِ سادهٔ صفحه عمدی است و بتواند با یک لمس به حالت عادی برگردد.
        AnimatedVisibility(visible = uiState.isReaderModeOn) {
            ReaderModeNotice(onTurnOff = viewModel::toggleReaderMode)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LauncherContent(
                uiState = uiState,
                showHome = showHome,
                hasPage = hasPage,
                hasHomeShortcut = hasHomeShortcut,
                renderGeckoWebView = renderGeckoWebView,
                prefs = prefs,
                bookmarks = bookmarks,
                historyItems = historyItems,
                downloadsCount = downloads.size,
                blockedAdsCount = blockedAdsCount.toInt(),
                tabsCount = uiState.tabs.size,
                passwordCount = passwordCount,
                protectedSiteCount = protectedSiteCount,
                pulseDomains = pulseDomains,
                submit = submit,
                viewModel = viewModel,
                onOpenVaultDashboard = onOpenVaultDashboard,
                onOpenSettings = onOpenSettings,
                onShowBookmarks = { showBookmarksHistorySheet = true },
                onShowDownloads = { showDownloadsSheet = true },
                onShowQuickActions = { showQuickActions = true },
                onShowTabs = { showTabsSheet = true },
                onSavePdf = requestPdf,
                onClearBackPrompt = { backPromptVisible = false },
            )
        }

        BrowserBottomBar(
            style = prefs.chromeBarStyle,
            canGoBack = activeTab?.canGoBack == true,
            canGoForward = activeTab?.canGoForward == true,
            isLoading = activeTab?.isLoading == true,
            reloadEnabled = !showHome && (hasPage || activeTab?.isLoading == true),
            isHomeSelected = showHome,
            tabCount = uiState.tabs.size,
            onBack = viewModel::goBack,
            onForward = viewModel::goForward,
            onReloadOrStop = if (activeTab?.isLoading == true) viewModel::stopLoading else viewModel::reloadPage,
            onHome = viewModel::showHome,
            onTabs = { showTabsSheet = true },
        )
    }

    // ---------- قابلیت‌های تازه: حالت مطالعه / بلندخوانی / دسترسی‌های سایت ----------
    uiState.readAloudText?.let { readable ->
        ReadAloudSheet(
            text = readable,
            onDismiss = viewModel::stopReadAloud,
        )
    }

    if (uiState.showSitePermissions) {
        SitePermissionsSheet(
            profile = uiState.activePermissionProfile ?: SitePermissionProfile(domain = ""),
            pageUrl = activeTab?.url.orEmpty(),
            onCycle = viewModel::cycleSitePermission,
            onReset = viewModel::resetSitePermissions,
            onDismiss = { viewModel.toggleSitePermissionsPanel(false) },
        )
    }

    uiState.contextTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissPageContextMenu) {
            PageContextMenu(
                target = target,
                canDownload = activeTab?.isPrivate != true,
                onOpen = { url -> viewModel.dismissPageContextMenu(); viewModel.openLinkInNewTab(url) },
                onCopy = { url ->
                    viewModel.dismissPageContextMenu()
                    copyTextToClipboard(clipboardContext, strings.copyLink, url)
                },
                onShare = { url ->
                    viewModel.dismissPageContextMenu()
                    val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    clipboardContext.startActivity(Intent.createChooser(share, strings.shareLink))
                },
                onDownload = { url -> viewModel.dismissPageContextMenu(); viewModel.enqueueDownload(url) },
                onSavePdf = {
                    viewModel.dismissPageContextMenu()
                    activeTab?.let { requestPdf(it.id, it.title) }
                },
                onSharePage = { viewModel.dismissPageContextMenu(); viewModel.shareCurrentPage() },
                onBookmarkPage = { viewModel.dismissPageContextMenu(); viewModel.toggleBookmarkCurrentPage() },
                onCopyText = {
                    viewModel.dismissPageContextMenu()
                    copyTextToClipboard(clipboardContext, strings.contextCopyText, target.text.orEmpty())
                },
            )
        }
    }

    if (showAccounts) {
        ModalBottomSheet(onDismissRequest = { showAccounts = false }) {
            SiteAccountsSheet(
                accounts = siteAccounts,
                activeId = uiState.activeContainer?.id,
                onSelect = { account -> showAccounts = false; viewModel.openSiteAccount(account) },
                onCreate = { name -> showAccounts = false; viewModel.createSiteAccount(name) },
            )
        }
    }

    if (showQuickActions) {
        ModalBottomSheet(onDismissRequest = { showQuickActions = false }) {
            QuickActionsSheet(
                hasPage = hasPage,
                activeTab = activeTab,
                prefs = prefs,
                blockedAdsCount = blockedAdsCount.toInt(),
                downloadsCount = downloads.size,
                tabsCount = uiState.tabs.size,
                viewModel = viewModel,
                onSubmit = submit,
                onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
                onOpenTabs = { showTabsSheet = true },
                onOpenBookmarks = { showBookmarksHistorySheet = true },
                onOpenDownloads = { showDownloadsSheet = true },
                onOpenSettings = onOpenSettings,
                onCopyLink = { url -> copyTextToClipboard(context = clipboardContext, label = strings.quickCopyLink, text = url) },
                onShareLink = { url ->
                    val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    clipboardContext.startActivity(Intent.createChooser(share, strings.quickShare))
                },
                onDismiss = { showQuickActions = false },
            )
        }
    }

    if (showBookmarksHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksHistorySheet = false }) {
            LibrarySheet(
                bookmarks = bookmarks,
                historyItems = historyItems,
                onSelectUrl = { url ->
                    showBookmarksHistorySheet = false
                    submit(url)
                },
                onOpenInNewTab = { url ->
                    showBookmarksHistorySheet = false
                    viewModel.openNewTab(url)
                },
                onClearHistory = viewModel::clearBrowsingHistory,
                onClearHistoryOlderThan = viewModel::clearHistoryOlderThan,
                onAddBookmark = { title, url, folder, result -> viewModel.addBookmark(title, url, folder, result) },
                onUpdateBookmark = viewModel::updateBookmark,
                onMoveBookmark = viewModel::moveBookmarkToFolder,
                onDeleteBookmark = viewModel::deleteBookmark,
                onDeleteHistoryItem = viewModel::deleteHistoryItem,
                onSearch = viewModel::searchLibrary,
            )
        }
    }

    if (showDownloadsSheet) {
        LaunchedEffect(Unit) {
            while (isActive) {
                viewModel.refreshDownloads()
                delay(1_500)
            }
        }
        ModalBottomSheet(onDismissRequest = { showDownloadsSheet = false }) {
            DownloadsSheet(downloads = downloads)
        }
    }

    if (showTabsSheet) {
        ModalBottomSheet(onDismissRequest = { showTabsSheet = false }) {
            TabsSwitcherSheet(
                tabs = uiState.tabs,
                activeTabId = uiState.activeTabId,
                onSelectTab = {
                    viewModel.selectTab(it)
                    showTabsSheet = false
                },
                onCloseTab = { tabId ->
                    if (prefs.askBeforeClosingTabs) pendingCloseTabId = tabId else viewModel.closeTab(tabId)
                },
                canReopenTab = uiState.canReopenTab,
                onReopenTab = {
                    viewModel.reopenClosedTab()
                    showTabsSheet = false
                },
                onCloseAll = viewModel::closeAllTabs,
                onNewNormalTab = {
                    viewModel.openNewTab(isPrivate = false)
                    showTabsSheet = false
                },
                onNewPrivateTab = {
                    viewModel.openNewTab(isPrivate = true)
                    showTabsSheet = false
                },
            )
        }
    }

    // پرسشِ «برگرد به کجا؟» وقتی کاربر حالت «هر بار از من بپرس» را انتخاب کرده است.
    if (backPromptVisible) {
        AlertDialog(
            onDismissRequest = { backPromptVisible = false },
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
            title = { Text(strings.backAskTitle) },
            text = { Text(strings.backAskBody) },
            confirmButton = {
                Button(
                    onClick = {
                        backPromptVisible = false
                        if (activeTab?.canGoBack == true) viewModel.goBack() else viewModel.showHome()
                    },
                    enabled = !showHome && activeTab?.canGoBack == true,
                ) {
                    Text(strings.backAskToPage)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            backPromptVisible = false
                            viewModel.showHome()
                        },
                        enabled = !showHome,
                    ) {
                        Text(strings.backAskToHome)
                    }
                    TextButton(
                        onClick = {
                            backPromptVisible = false
                            if (activity == null || !activity.moveTaskToBack(true)) viewModel.showHome()
                        },
                    ) {
                        Text(strings.backAskExit)
                    }
                }
            },
        )
    }

    // «ذخیرهٔ هوشمندِ ورود»: کارتِ تأیید با هشدارهای روشن و امکانِ اصلاحِ نامِ حساب.
    // مقادیر از پیش پر شده‌اند (یکی از سه درخواستِ اصلیِ کاربر) و با یک ضربه ذخیره می‌شوند.
    savePrompt?.let { prompt ->
        var editUsername by remember(prompt.username) { mutableStateOf(prompt.username) }
        var showUsernameField by remember(prompt.username) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissPasswordPrompt,
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text(strings.saveLoginTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.saveLoginBody)
                    Text(
                        text = prompt.site,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (showUsernameField) {
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it },
                            label = { Text(strings.credentialUsername) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("save_login_username"),
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = editUsername.ifBlank { prompt.maskedUsername },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { showUsernameField = true }) {
                                Text(strings.credentialEdit)
                            }
                        }
                    }
                    if (prompt.alternatives.isNotEmpty()) {
                        Text(
                            text = strings.autofillAlternatives,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        prompt.alternatives.firstOrNull { it != editUsername }?.let { other ->
                            TextButton(onClick = { editUsername = other }) { Text(other) }
                        }
                    }
                    // دلیلِ پرسیدن؛ «چرا این کارت آمد؟» باید همیشه روشن باشد.
                    Text(
                        text = when (prompt.reason) {
                            OfferReason.FIRST_ACCOUNT -> strings.saveLoginFirstAccountHint
                            OfferReason.ANOTHER_ACCOUNT -> strings.saveLoginAnotherAccount
                            OfferReason.PASSWORD_CHANGED -> strings.saveLoginPasswordChangedHint
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PasswordStrengthRow(prompt.strength, strings)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptPasswordPrompt(editUsername) },
                    modifier = Modifier.testTag("save_login_confirm"),
                ) {
                    Text(strings.saveLoginConfirmPrefilled)
                }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = viewModel::dismissPasswordPrompt) { Text(strings.saveLoginNotNow) }
                    TextButton(onClick = viewModel::neverAskForSiteAgain) {
                        Text(strings.saveLoginNeverAgain, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
        )
    }

    // هشدارهای «نگه‌داشتنی»: دامنهٔ بدل، گذرواژهٔ ضعیف‌تر، ذخیرهٔ ناموفق، ورودِ ردشده.
    // خبرهای کوتاه (کد نوشته شد، کدی پیدا نشد، فرم پر شد) در نوارِ بالای صفحه می‌آیند و
    // همین‌جا دیالوگ نمی‌سازند تا تایپ و پیمایشِ کاربر قطع نشود.
    passwordNotice?.takeIf { !it.isTransient }?.let { notice ->
        AlertDialog(
            onDismissRequest = viewModel::consumePasswordNotice,
            icon = {
                Icon(
                    imageVector = if (notice is PasswordNotice.PhishingWarning) Icons.Default.Shield else Icons.Default.Lock,
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    when (notice) {
                        is PasswordNotice.PhishingWarning -> strings.phishingWarningTitle
                        is PasswordNotice.Weaker -> strings.saveLoginWeakerTitle
                        PasswordNotice.LoginRejected -> strings.loginRejectedNotice
                        PasswordNotice.SaveFailed -> strings.saveLoginFailed
                        else -> strings.saveLoginWeakerTitle
                    },
                )
            },
            text = {
                Text(
                    when (notice) {
                        is PasswordNotice.PhishingWarning ->
                            strings.phishingWarningBody(notice.host, notice.impersonated ?: strings.originLookalike)

                        is PasswordNotice.Weaker -> strings.saveLoginWeakerBody
                        PasswordNotice.LoginRejected -> strings.loginRejectedNotice
                        PasswordNotice.SaveFailed -> strings.saveLoginFailed
                        else -> strings.saveLoginWeakerBody
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumePasswordNotice) {
                    Text(if (notice is PasswordNotice.PhishingWarning) strings.phishingWarningDismiss else strings.close)
                }
            },
        )
    }

    // پرسشِ اثر انگشت برای پرکردنِ ورود.
    uiState.autofillUnlockRequestId?.let { requestId ->
        AlertDialog(
            onDismissRequest = viewModel::dismissAutofillUnlock,
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text(strings.autofillConfirmTitle) },
            text = { Text(strings.autofillConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestBiometricUnlock(strings.autofillConfirmAction) {
                            viewModel.onBiometricConfirmedForAutofill(requestId)
                        }
                    },
                    modifier = Modifier.testTag("autofill_unlock_confirm"),
                ) {
                    Text(strings.autofillConfirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAutofillUnlock) { Text(strings.later) }
            },
        )
    }

    if (uiState.showSiteCompatibilitySheet) {
        val sheetUrl = uiState.activeTab?.url.orEmpty()
        var candidates by remember { mutableStateOf<List<AutofillCandidate>>(emptyList()) }
        LaunchedEffect(sheetUrl, uiState.showSiteCompatibilitySheet) {
            candidates = if (sheetUrl.isBlank()) emptyList() else viewModel.siteCandidates(sheetUrl)
        }
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSiteCompatibilitySheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SiteCompatibilitySheet(
                strings = strings,
                plan = uiState.sitePlan,
                candidates = candidates,
                onTryNext = viewModel::tryNextSiteRoute,
                onFill = { credentialId ->
                    viewModel.autofillFromVault(credentialId)
                    viewModel.closeSiteCompatibilitySheet()
                },
                onCaptureCurrentForm = {
                    viewModel.checkForSubmittableLogin()
                    viewModel.closeSiteCompatibilitySheet()
                },
                modifier = Modifier,
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    pendingCloseTabId?.let { tabId ->
        val title = uiState.tabs.firstOrNull { it.id == tabId }?.title.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingCloseTabId = null },
            title = { Text(strings.closeTabConfirmTitle) },
            text = { Text(strings.closeTabConfirmBody(title.ifBlank { strings.tabsLabel })) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCloseTabId = null
                        viewModel.closeTab(tabId)
                    },
                ) {
                    Text(strings.closeTab)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCloseTabId = null }) { Text(strings.cancel) }
            },
        )
    }

    uiState.fallbackPromptDomain?.let { domain ->
        AlertDialog(
            onDismissRequest = viewModel::dismissFallbackPrompt,
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text(strings.reloginTitle) },
            text = { Text(strings.reloginBody(domain)) },
            confirmButton = {
                Button(onClick = viewModel::dismissFallbackPrompt) {
                    Text(strings.reloginConfirm)
                }
            },
        )
    }
}

/* ------------------------------- افزودن هر سایتی / ورود دستی ------------------------------- */

/**
 * برگهٔ «این سایت چطور وارد می‌شود؟»
 *
 * به‌جای پیامِ «پشتیبانی نمی‌شود»، روشِ ورودِ شناسایی‌شده و نردبانِ راه‌های جایگزین را نشان
 * می‌دهد. هر راه یک ضربه است و کانتینر/نشستِ فعلی حفظ می‌شود.
 */
@Composable
private fun SiteCompatibilitySheet(
    strings: MarbleStrings,
    plan: SiteAdditionPlan?,
    candidates: List<AutofillCandidate>,
    onTryNext: () -> Unit,
    onFill: (String) -> Unit,
    onCaptureCurrentForm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MarbleLayout.Screen),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = strings.addAnySiteTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = strings.addAnySiteDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        if (plan == null) {
            Text(
                text = strings.loginMethodUnknownHint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline,
            )
            return@Column
        }
        Text(
            text = strings.loginMethodTitle,
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
        )
        Text(
            text = loginMethodLabel(plan.assessment.primary, strings),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        plan.assessment.blockers
            .filter { it != SiteBlocker.NONE }
            .forEach { blocker ->
                Text(
                    text = blockerLabel(blocker, strings),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        if (plan.steps.isNotEmpty()) {
            Text(
                text = strings.addAnySiteRoute(routeLabel(plan.steps.first().route, strings)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (plan.hasFallbacks) {
                Button(onClick = onTryNext) { Text(strings.addAnySiteAction) }
            }
            FilledTonalButton(onClick = onCaptureCurrentForm) {
                Text(strings.saveLoginConfirm)
            }
        }
        if (candidates.isEmpty()) {
            Text(
                text = strings.saveLoginNoStored,
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline,
            )
        } else {
            Text(
                text = strings.saveLoginFillTitle,
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            candidates.take(4).forEach { candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceContainerHigh.copy(alpha = 0.45f))
                        .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = candidate.username,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = candidate.domain,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = { onFill(candidate.id) }) {
                        Text(strings.saveLoginFillAction)
                    }
                }
            }
        }
    }
}

private fun routeLabel(route: SiteRoute?, strings: MarbleStrings): String = when (route) {
    SiteRoute.DIRECT -> strings.routeDirect
    SiteRoute.DESKTOP_USER_AGENT -> strings.routeDesktop
    SiteRoute.MOBILE_SUBDOMAIN -> strings.routeMobile
    SiteRoute.CANONICAL_HOST -> strings.routeCanonical
    SiteRoute.PWA_WINDOW -> strings.routePwa
    SiteRoute.VAULT_MANUAL_LOGIN -> strings.routeManual
    SiteRoute.READ_ONLY_SNAPSHOT -> strings.routeReadOnly
    null -> strings.routeDirect
}

internal fun loginMethodLabel(method: LoginMethod, strings: MarbleStrings): String = when (method) {
    LoginMethod.PASSWORD -> strings.loginMethodPassword
    LoginMethod.GOOGLE -> strings.loginMethodGoogle
    LoginMethod.APPLE -> strings.loginMethodApple
    LoginMethod.PHONE_CODE -> strings.loginMethodPhone
    LoginMethod.EMAIL_CODE -> strings.loginMethodEmailCode
    LoginMethod.MAGIC_LINK -> strings.loginMethodMagicLink
    LoginMethod.PASSKEY -> strings.loginMethodPasskey
    LoginMethod.WALLET -> strings.loginMethodWallet
    LoginMethod.CORPORATE_SSO -> strings.loginMethodSso
    LoginMethod.UNKNOWN -> strings.loginMethodUnknown
}

internal fun blockerLabel(blocker: SiteBlocker, strings: MarbleStrings): String = when (blocker) {
    SiteBlocker.IFRAME_LOGIN -> strings.blockerIframe
    SiteBlocker.BROWSER_REFUSED -> strings.blockerBrowser
    SiteBlocker.THIRD_PARTY_COOKIES -> strings.blockerThirdParty
    SiteBlocker.JAVASCRIPT_ONLY -> strings.blockerJavaScript
    SiteBlocker.NETWORK_ERROR -> strings.blockerNetwork
    SiteBlocker.ONE_TIME_CODE_ONLY -> strings.blockerOtp
    SiteBlocker.NONE -> strings.loginMethodUnknownHint
}

internal fun strengthLabel(level: PasswordStrengthLevel, strings: MarbleStrings): String = when (level) {
    PasswordStrengthLevel.VERY_WEAK -> strings.passwordStrengthVeryWeak
    PasswordStrengthLevel.WEAK -> strings.passwordStrengthWeak
    PasswordStrengthLevel.FAIR -> strings.passwordStrengthFair
    PasswordStrengthLevel.STRONG -> strings.passwordStrengthStrong
    PasswordStrengthLevel.EXCELLENT -> strings.passwordStrengthExcellent
}

/* ---------------------------------------- نوار بالا ---------------------------------------- */

@Composable
private fun BrowserTopBar(
    style: ChromeBarStyle,
    showHome: Boolean,
    activeTab: BrowserTab?,
    url: String,
    isBookmarked: Boolean,
    canBookmark: Boolean,
    addressMode: AddressSelectionMode,
    isLoading: Boolean,
    progress: Int,
    isFindInPageVisible: Boolean,
    findQuery: String,
    findMatchIndex: Int,
    findTotalMatches: Int,
    bannerMessage: String?,
    blockedAdsCount: Int,
    showAdChip: Boolean,
    menuExpanded: Boolean,
    menuState: BrowserMenuState,
    menuActions: BrowserMenuActions,
    onSubmit: (String) -> Unit,
    onBookmark: () -> Unit,
    onQuickActions: () -> Unit,
    onPreviewOverflow: () -> Unit,
    onDismissMenu: () -> Unit,
    onFindQueryChanged: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onFindClose: () -> Unit,
    onDismissBanner: () -> Unit,
    onOpenVault: () -> Unit,
    suggestions: List<com.marble098.marblebr.core.model.AddressSuggestion> = emptyList(),
    showSuggestions: Boolean = false,
    onPickSuggestion: (String) -> Unit = {},
    onAddressQueryChanged: (String) -> Unit = {},
    onAddressFocusChanged: (Boolean) -> Unit = {},
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(30.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .then(
                if (style == ChromeBarStyle.FLOATING) {
                    Modifier.padding(horizontal = 2.dp)
                } else {
                    Modifier
                        .liquidGlass(style = style, shape = shape, elevation = if (style == ChromeBarStyle.GLASS) 10.dp else 3.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MarbleGlassIconButton(
                icon = if (activeTab?.isPrivate == true) Icons.Default.VisibilityOff else Icons.Default.Shield,
                contentDescription = strings.protectionLabel,
                onClick = onOpenVault,
                style = style,
                tint = when (activeTab?.healthStatus) {
                    SessionHealthStatus.PROTECTED_ACTIVE,
                    SessionHealthStatus.HEALTHY,
                    SessionHealthStatus.RECOVERED_FROM_VAULT,
                    -> colors.primary

                    SessionHealthStatus.SERVER_REVOKED_FALLBACK,
                    SessionHealthStatus.REAUTH_REQUIRED,
                    -> colors.error

                    else -> colors.outline
                },
                size = 42.dp,
                iconSize = 19.dp,
                testTag = "never_signout_shield_btn",
            )

            key(activeTab?.id) {
                BrowserAddressField(
                    url = url,
                    secure = activeTab?.isSecure == true,
                    isBookmarked = isBookmarked,
                    canBookmark = canBookmark,
                    mode = addressMode,
                    onSubmit = onSubmit,
                    onBookmark = onBookmark,
                    modifier = Modifier.weight(1f),
                    onQueryChanged = onAddressQueryChanged,
                    onFocusChanged = onAddressFocusChanged,
                )
            }

            MarbleGlassIconButton(
                icon = Icons.Default.Bolt,
                contentDescription = strings.quickActionsTitle,
                onClick = onQuickActions,
                style = style,
                tint = colors.primary,
                size = 42.dp,
                iconSize = 21.dp,
                testTag = "quick_actions_btn",
            )
            // منو داخل همین Box رندر می‌شود تا Popup دقیقاً زیرِ دکمهٔ سه‌نقطه باز شود
            // (باگ قبلی: منو در ستونِ صفحه رندر می‌شد و جای دیگری باز می‌شد).
            Box {
                MarbleGlassIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = strings.browserMenu,
                    onClick = onPreviewOverflow,
                    style = style,
                    selected = menuExpanded,
                    size = 42.dp,
                    iconSize = 21.dp,
                    testTag = "browser_overflow_menu_btn",
                )
                BrowserOverflowMenu(
                    expanded = menuExpanded,
                    onDismiss = onDismissMenu,
                    state = menuState,
                    actions = menuActions,
                )
            }
        }

        // پیشنهادهای زندهٔ نوار آدرس: تاریخچه، نشان‌ها و پربازدیدها. با انتخابِ هر مورد،
        // همان آدرس باز می‌شود و نوار بسته می‌شود.
        if (showSuggestions) {
            AddressSuggestionList(
                suggestions = suggestions,
                onPick = { suggestion ->
                    onAddressFocusChanged(false)
                    onPickSuggestion(suggestion.url)
                },
            )
        }

        Box(Modifier.fillMaxWidth().height(2.5.dp)) {
            if (!showHome && isLoading) {
                val fraction = (progress / 100f).coerceIn(0f, 1f)
                val visibleProgress by animateFloatAsState(fraction, label = "page load")
                LinearProgressIndicator(
                    progress = { if (fraction == 0f) 0.04f else visibleProgress },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50)),
                    color = colors.primary,
                    trackColor = colors.primary.copy(alpha = 0.12f),
                )
            }
        }

        AnimatedVisibility(
            visible = isFindInPageVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FindInPageBar(
                query = findQuery,
                matchIndex = findMatchIndex,
                totalMatches = findTotalMatches,
                onQueryChanged = onFindQueryChanged,
                onNext = onFindNext,
                onPrevious = onFindPrevious,
                onClose = onFindClose,
            )
        }

        AnimatedVisibility(
            visible = showAdChip,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            AssistChip(
                onClick = onOpenVault,
                label = {
                    Text(
                        text = strings.adsBlocked(blockedAdsCount),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = strings.adBlockChip(blockedAdsCount),
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }

        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            BannerCard(message = bannerMessage.orEmpty(), onDismiss = onDismissBanner)
        }
    }
}

@Composable
private fun BannerCard(message: String, onDismiss: () -> Unit) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.secondaryContainer.copy(alpha = 0.92f))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = strings.dismiss, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * نوار آدرس سبک و بدون بازترکیبِ سنگین.
 *
 * از [BasicTextField] استفاده می‌کند (نه [OutlinedTextField]) تا هر تایپ فقط یک لایهٔ متن
 * را دوباره بکشد؛ رنگ و شکل در Composable جدا کش می‌شوند.
 */
@Composable
fun BrowserAddressField(
    url: String,
    secure: Boolean,
    isBookmarked: Boolean,
    canBookmark: Boolean,
    mode: AddressSelectionMode,
    onSubmit: (String) -> Unit,
    onBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    /** متنِ تازهٔ تایپ‌شده؛ برای ساختنِ پیشنهادهای زنده. */
    onQueryChanged: (String) -> Unit = {},
    /** فوکوس/خروجِ فوکوس؛ پیشنهادها فقط هنگامِ فوکوس دیده می‌شوند. */
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(TextFieldValue(url)) }
    // هر لمسِ تازهٔ نوار آدرس، یک «نشانهٔ انتخاب» می‌سازد؛ با تغییر آن، انتخاب و مکان‌نما
    // دوباره اعمال می‌شود. (پیش‌تر فقط اولین فوکوس انتخاب می‌کرد و لمس‌های بعدی کاری نمی‌کردند.)
    var tapSelectRequest by remember { mutableIntStateOf(0) }
    var touchGeneration by remember { mutableIntStateOf(0) }
    val style = ChromeBarStyle.FLOATING
    val submitNow = rememberUpdatedState(onSubmit)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // به‌روزرسانیِ لحظه‌ای: متنِ تایپ‌شده به لایهٔ داده می‌رود تا پیشنهادها ساخته شوند.
    // (فوکوس فقط از مسیرِ `onFocusChanged` در BasicTextField گزارش می‌شود؛ قبلاً اینجا هم
    // روی هر تغییرِ متن دوباره `onFocusChanged(focused)` ارسال می‌شد که بازترکیب‌های بیهوده
    // و «show/hide»های تکراریِ IME («ALREADY_HIDDEN») می‌ساخت.)
    LaunchedEffect(input.text) {
        onQueryChanged(input.text)
    }

    LaunchedEffect(url, focused) {
        if (!focused && input.text != url) input = TextFieldValue(url)
    }
    // Focus can be acquired by accessibility/keyboard as well as a touch. Do not overwrite typing.
    LaunchedEffect(focused, tapSelectRequest, mode) {
        if (!focused) return@LaunchedEffect
        val originalText = input.text
        val originalTouchGeneration = touchGeneration
        delay(100)
        // Ignore focus's own cursor adjustment, but never override a later user touch or typing.
        if (focused && input.text == originalText && touchGeneration == originalTouchGeneration) {
            input = input.copy(selection = addressSelection(input.text, input.selection.end, mode))
        }
    }

    Row(
        modifier = modifier
            .height(44.dp)
            .testTag("smart_address_frame")
            .floatingGlassPill(style = style, emphasized = focused)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                // لمسِ نوار (نه خودِ متن) → فوکوس، انتخاب کامل و مکان‌نمای انتها.
                focusRequester.requestFocus()
                keyboard?.show()
                input = input.copy(selection = addressSelection(input.text, input.selection.end, mode))
                tapSelectRequest++
            }
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (secure) Icons.Default.Lock else Icons.Default.Search,
            contentDescription = null,
            tint = if (secure) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (input.text.isEmpty()) {
                Text(
                    text = strings.addressPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = input,
                onValueChange = { updated ->
                    // اگر کاربر بدون تایپ فقط جای مکان‌نما را عوض کند، همان را نگه می‌داریم؛
                    // اما متنِ تازه همیشه پذیرفته می‌شود.
                    input = updated
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(mode) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            touchGeneration++
                            val firstFocus = !focused
                            var moved = false
                            var released = false
                            do {
                                // Observe the final pass even when BasicTextField consumed the touch.
                                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
                                if (change == null) break
                                if ((change.position - down.position).getDistance() > slop) moved = true
                                if (!change.pressed) released = true
                            } while (!released)
                            if (firstFocus && released && !moved && focused) {
                                input = input.copy(selection = addressSelection(input.text, input.selection.end, mode))
                            }
                        }
                    }
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocusChanged(it.isFocused)
                    }
                    .focusRequester(focusRequester)
                    .testTag("smart_address_bar")
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.Enter && event.key != Key.NumPadEnter) {
                            false
                        } else {
                            if (event.type == KeyEventType.KeyUp) submitNow.value(input.text)
                            true
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submitNow.value(input.text) }),
            )
        }
        if (focused) {
            IconButton(
                onClick = { submitNow.value(input.text) },
                enabled = input.text.isNotBlank(),
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.ArrowRightAlt,
                    contentDescription = strings.goToAddress,
                    tint = if (input.text.isNotBlank()) colors.primary else colors.outline,
                    modifier = Modifier.size(19.dp),
                )
            }
        } else if (canBookmark) {
            IconButton(onClick = onBookmark, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = strings.bookmarkToggle,
                    tint = if (isBookmarked) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/* --------------------------------------- محتوای صفحه --------------------------------------- */

@Composable
private fun LauncherContent(
    uiState: BrowserUiState,
    showHome: Boolean,
    hasPage: Boolean,
    hasHomeShortcut: Boolean,
    renderGeckoWebView: Boolean,
    prefs: BrowserPreferences,
    bookmarks: List<BookmarkItem>,
    historyItems: List<HistoryItem>,
    downloadsCount: Int,
    blockedAdsCount: Int,
    tabsCount: Int,
    passwordCount: Int,
    protectedSiteCount: Int,
    pulseDomains: List<String>,
    submit: (String) -> Unit,
    viewModel: BrowserViewModel,
    onOpenVaultDashboard: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowQuickActions: () -> Unit,
    onShowTabs: () -> Unit,
    onSavePdf: (String, String) -> Unit,
    onClearBackPrompt: () -> Unit,
) {
    val activeTab = uiState.activeTab
    val isInspection = LocalInspectionMode.current
    val container = uiState.activeContainer

    // با عوض شدن صفحه یا تب، پرسشِ بازگشتِ بی‌ربط بسته می‌شود.
    LaunchedEffect(uiState.activeTabId, showHome) { onClearBackPrompt() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showHome -> HomeContent(
                modifier = Modifier,
                passwordCount = passwordCount,
                protectedSiteCount = protectedSiteCount,
                pulseSites = pulseDomains,
                onOpenVault = { onOpenVaultDashboard(null) },
                onAddAnySite = viewModel::addAnySite,
                onOpenPasswordStudio = viewModel::openSiteCompatibilitySheet,
                bookmarks = bookmarks,
                historyItems = historyItems,
                showNewTabTile = prefs.homeShortcutPosition != HomeShortcutPosition.NONE,
                chromeBarStyle = prefs.chromeBarStyle,
                blockedAdsCount = blockedAdsCount,
                downloadsCount = downloadsCount,
                onSearch = submit,
                onNewTab = { viewModel.openNewTab() },
                onNewPrivateTab = { viewModel.openNewTab(isPrivate = true) },
                onOpenBookmarksHistory = onShowBookmarks,
                onOpenDownloads = onShowDownloads,
                onOpenSettings = onOpenSettings,
                onOpenQuickActions = onShowQuickActions,
                onOpenTabs = onShowTabs,
                tabsCount = tabsCount,
            )

            activeTab != null && hasPage && renderGeckoWebView && !isInspection && container != null && container.id == activeTab.containerId -> {
                key(activeTab.id, activeTab.contextId, activeTab.sessionRevision) {
                    GeckoSessionWebView(
                        tab = activeTab,
                        container = container,
                        viewModel = viewModel,
                        onSavePdf = { onSavePdf(activeTab.id, activeTab.title) },
                    )
                }
            }

            !renderGeckoWebView || isInspection -> ReadyCard(
                tab = activeTab,
                onCaptureSnapshot = viewModel::captureManualSnapshotNow,
                onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
            )
        }

        if (hasHomeShortcut) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = MarbleLayout.Screen, bottom = 12.dp),
            ) {
                HomeShortcutButton(onClick = { viewModel.openNewTab() })
            }
        }
    }
}

/**
 * دکمهٔ شناورِ «تبِ تازه» — فقط وقتی کاربر در تنظیمات حالت «شناور» را انتخاب کند.
 *
 * پیش‌فرضِ برنامه این دکمه را **نشان نمی‌دهد**؛ چون کاشی «تب تازه» در صفحهٔ خانه همین کار را
 * می‌کند و کاربر گفته بود این دکمهٔ مستقل روی صفحه اضافه و آزاردهنده است. نسخهٔ شناور هم
 * با سایهٔ نرم و اندازهٔ متعادل‌تر (۴۸dp) بازطراحی شد تا اگر کسی خواستش، زشت نباشد.
 */
@Composable
private fun HomeShortcutButton(onClick: () -> Unit) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = colors.primary.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("home_shortcut_btn"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = strings.newTab,
            tint = colors.onPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/* ------------------------------- شیت‌های کمکی و اجزای بازمانده ------------------------------- */

@Composable
private fun GeckoSessionWebView(
    tab: BrowserTab,
    container: WebAppContainer,
    viewModel: BrowserViewModel,
    onSavePdf: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            GeckoView(ctx).apply {
                val geckoSession = viewModel.browserSession(tab, container)
                ctx.findActivity()?.let { activity ->
                    geckoSession.selectionActionDelegate = PageSelectionDelegate(
                        activity, strings.contextShareText, strings.contextSavePdf, onSavePdf,
                    )
                }
                GeckoSurfaceBinding.bind(this, geckoSession)
            }
        },
        update = { view ->
            // هر بازترکیب، نمای نمایش‌داده‌شده را به نشستِ فعلیِ همان تب وصل و فعال نگه می‌دارد.
            // اگر نما هنگام جابه‌جایی صفحه از نشست جدا شده باشد، صفحه بدون نیاز به بازگشایی اپ دوباره دیده می‌شود.
            viewModel.sessionManager.activeSession(tab.id)?.let { GeckoSurfaceBinding.bind(view, it) }
        },
        onRelease = { GeckoSurfaceBinding.unbind(it) },
    )
}

@Composable
private fun FindInPageBar(
    query: String,
    matchIndex: Int,
    totalMatches: Int,
    onQueryChanged: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .floatingGlassPill(style = ChromeBarStyle.FLOATING)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .testTag("find_in_page_input"),
            singleLine = true,
            textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
            cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = strings.findInPagePlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
        Text(
            text = "$matchIndex/$totalMatches",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        IconButton(onClick = onPrevious, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = strings.findPrevious, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = strings.findNext, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = strings.findClose, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ReadyCard(
    tab: BrowserTab?,
    onCaptureSnapshot: () -> Unit,
    onOpenVault: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.primaryContainer.copy(alpha = 0.28f), colors.background),
                ),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = strings.readyTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = strings.readyBody,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = tab?.url ?: MarbleLinks.GITHUB_URL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCaptureSnapshot) { Text(strings.quickSaveState) }
                    FilledTonalButton(onClick = onOpenVault) { Text(strings.quickEarlierCopies) }
                }
            }
        }
    }
}

/**
 * شیتِ تب‌ها — بازطراحی برای خوانایی و استفادهٔ آسان‌تر.
 *
 * هر تب یک کارتِ افقی با لوگوی سایت، عنوان، دامنه و دکمهٔ بستن است؛ تبِ فعال با حاشیه و
 * پس‌زمینهٔ رنگی برجسته می‌شود و نوار ابزار بالای شیت همیشه در دسترس است.
 */
@Composable
private fun TabsSwitcherSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    canReopenTab: Boolean,
    onReopenTab: () -> Unit,
    onCloseAll: () -> Unit,
    onNewNormalTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    var filter by remember { mutableStateOf("") }
    var confirmCloseAll by remember { mutableStateOf(false) }
    val visibleTabs = remember(tabs, filter) {
        if (filter.isBlank()) {
            tabs
        } else {
            tabs.filter {
                it.url.contains(filter, ignoreCase = true) || it.title.contains(filter, ignoreCase = true)
            }
        }
    }
    if (confirmCloseAll) {
        AlertDialog(
            onDismissRequest = { confirmCloseAll = false },
            title = { Text(strings.closeAllTabs) },
            text = { Text(strings.confirmCloseAllTabs) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloseAll = false
                    onCloseAll()
                }) { Text(strings.closeAllTabs) }
            },
            dismissButton = { TextButton(onClick = { confirmCloseAll = false }) { Text(strings.dismiss) } },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.openTabsTitle(tabs.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = strings.tabsBehaviorTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colors.onSurfaceVariant,
                )
            }
            MarbleGlassIconButton(
                icon = Icons.Default.VisibilityOff,
                contentDescription = strings.newPrivateTab,
                onClick = onNewPrivateTab,
                style = ChromeBarStyle.FLOATING,
                size = 40.dp,
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                    .clickable(onClick = onNewNormalTab),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.newTab, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        if (tabs.size > 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .floatingGlassPill(style = ChromeBarStyle.FLOATING)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                    cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (filter.isEmpty()) {
                                Text(
                                    text = strings.searchTabs,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        if (visibleTabs.isEmpty()) {
            Text(
                text = if (tabs.isEmpty()) strings.noTabs else strings.noMatchingTabs,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(visibleTabs, key = { it.id }) { tab ->
                TabCard(
                    tab = tab,
                    isSelected = tab.id == activeTabId,
                    onSelect = { onSelectTab(tab.id) },
                    onClose = { onCloseTab(tab.id) },
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(onClick = onReopenTab, enabled = canReopenTab) { Text(strings.reopenTab) }
            if (tabs.isNotEmpty()) {
                TextButton(onClick = { confirmCloseAll = true }) { Text(strings.closeAllTabs) }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val domain = remember(tab.url) { siteDomain(tab.url) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            // کارتِ تب، سبک و شیشه‌ای است: تبِ فعال با هالهٔ نازکِ رنگی و نوار کناری مشخص
            // می‌شود (نه با پنلِ توپُرِ سنگین که در نسخهٔ قبل کل ردیف را می‌پوشاند).
            .background(
                when {
                    isSelected -> colors.surface.copy(alpha = 0.78f)
                    tab.isPrivate -> colors.tertiary.copy(alpha = 0.10f)
                    else -> colors.surface.copy(alpha = 0.52f)
                },
            )
            .border(
                width = if (isSelected) 1.2.dp else 0.9.dp,
                color = when {
                    isSelected -> colors.primary.copy(alpha = 0.55f)
                    tab.isPrivate -> colors.tertiary.copy(alpha = 0.35f)
                    else -> colors.outlineVariant.copy(alpha = 0.30f)
                },
                shape = shape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (tab.url.isBlank()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (tab.isPrivate) Icons.Default.VisibilityOff else Icons.Default.Tab,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            SiteBadgeLogo(displayName = tab.title, domain = domain, size = 32.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.title.ifBlank {
                    tab.url.ifBlank { if (tab.isPrivate) strings.privateTabTitle else strings.newTabTitle }
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    tab.isPrivate -> strings.privateTabTitle
                    tab.url.isBlank() -> strings.newTabTitle
                    else -> domain
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.primary),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = strings.closeTab, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DownloadsSheet(downloads: List<DownloadItem>) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(strings.downloadsMenu(downloads.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (downloads.isEmpty()) {
            Text(
                text = strings.emptyDownloads,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(downloads, key = { it.id }) { dl ->
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(colors.surface.copy(alpha = 0.55f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(dl.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = dl.url,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val total = dl.totalBytes
                        val status = when (dl.state) {
                            DownloadState.COMPLETED -> strings.savedPath(dl.destinationPath)
                            DownloadState.FAILED -> strings.downloadFailed
                            DownloadState.IN_PROGRESS -> strings.downloadInProgress
                        }
                        if (dl.state == DownloadState.IN_PROGRESS && total > 0) {
                            val fraction = (dl.downloadedBytes.toFloat() / total).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------ کمکی‌ها ------------------------------------ */

/** کپی متن در کلیپ‌بورد سیستم. */
internal fun copyTextToClipboard(context: Context, label: String, text: String) {
    runCatching {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

/** پیدا کردن Activity از Context برای بستن برنامه. */
internal fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
