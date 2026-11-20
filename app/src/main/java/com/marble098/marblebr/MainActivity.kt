package com.marble098.marblebr

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleBrTheme
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.engine.CredentialFallbackAuthenticator
import com.marble098.marblebr.feature.browser.BrowserScreen
import com.marble098.marblebr.feature.browser.BrowserViewModel
import com.marble098.marblebr.feature.browser.PwaShortcutInstaller
import com.marble098.marblebr.feature.browser.RateAppDialog
import com.marble098.marblebr.feature.settings.SettingsScreen
import com.marble098.marblebr.feature.settings.SettingsViewModel
import com.marble098.marblebr.feature.vault.VaultDashboardScreen
import com.marble098.marblebr.feature.vault.VaultDashboardViewModel
import com.marble098.marblebr.vault.BiometricAuthResult
import com.marble098.marblebr.vault.BiometricVaultLock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * منحنی حرکتِ نرم و «جهنده» برای گذارهای اپ (حس Material 3 Expressive).
 */
private val MarbleEasing: Easing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)

/** منحنیِ ورودِ ملایم‌تر برای صفحه‌های تنظیمات و گاوصندوق؛ بدون پرشِ مقیاس. */
private val MarbleSoftEasing: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: BrowserPreferencesRepository

    @Inject
    lateinit var biometricVaultLock: BiometricVaultLock

    @Inject
    lateinit var credentialFallbackAuthenticator: CredentialFallbackAuthenticator

    private var incomingNavigation by mutableStateOf<IncomingNavigation?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // حالت کیبورد از AndroidManifest (adjustResize) می‌آید و `imePadding` فقط در صفحهٔ
        // خانه/شیت‌ها اعمال می‌شود؛ در نتیجه نمای وب هنگام باز شدن کیبورد بازچیدمان نمی‌شود.
        // (این فراخوانی صریح، رفتار را روی دستگاه‌هایی که manifest را بازنویسی می‌کنند تضمین می‌کند.)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // لبه‌به‌لبه با نوارهای شفاف؛ رنگ آیکن‌ها را خودِ تم (روشن/تاریک) تنظیم می‌کند.
        // نوار ناوبری هم شفاف است تا شیشهٔ شناورِ نوار تب از پشت آن دیده شود.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        if (savedInstanceState == null) incomingNavigation = resolveIncomingNavigation(intent)

        setContent {
            val prefs by preferencesRepository.preferencesFlow.collectAsState(initial = BrowserPreferences())
            val coroutineScope = rememberCoroutineScope()
            var showRateDialog by remember { mutableStateOf(false) }

            val rateLaunchThreshold = 3

            LaunchedEffect(prefs.rateDialogShown, prefs.rateLaunchCount) {
                if (!prefs.rateDialogShown && prefs.rateLaunchCount >= rateLaunchThreshold) {
                    showRateDialog = true
                }
            }

            // شمارش راه‌اندازی‌ها تا پنل امتیازدهیِ اولین ورود نمایش داده شود
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val current = preferencesRepository.preferencesFlow.first()
                    if (!current.rateDialogShown) {
                        preferencesRepository.setRateLaunchCount(current.rateLaunchCount + 1)
                    }
                }
            }

            if (showRateDialog) {
                RateAppDialog(
                    onStarNow = {
                        showRateDialog = false
                        coroutineScope.launch {
                            preferencesRepository.setRateDialogShown(true)
                        }
                    },
                    onLater = { showRateDialog = false },
                    onNever = {
                        showRateDialog = false
                        coroutineScope.launch {
                            preferencesRepository.setRateDialogShown(true)
                        }
                    },
                )
            }

            MarbleBrTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColorEnabled,
                language = prefs.language,
            ) {
                MarbleBrAppNavHost(
                    activity = this@MainActivity,
                    initialUrl = incomingNavigation?.url,
                    pinnedContainerId = incomingNavigation?.pinnedContainerId,
                    onUrlConsumed = { incomingNavigation = null },
                    biometricVaultLock = biometricVaultLock,
                    credentialFallbackAuthenticator = credentialFallbackAuthenticator,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingNavigation = resolveIncomingNavigation(intent) ?: incomingNavigation
    }

    private fun resolveIncomingNavigation(intent: Intent?): IncomingNavigation? {
        val text = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> if (intent.type == "text/plain") intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() else null
            else -> null
        } ?: return null
        val url = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(text)?.value
            ?.takeIf { !Uri.parse(it).host.isNullOrBlank() } ?: return null
        val pinned = intent?.takeIf { it.action == Intent.ACTION_VIEW }
            ?.getStringExtra(PwaShortcutInstaller.EXTRA_CONTAINER_ID)
        return IncomingNavigation(url, pinned)
    }

    private data class IncomingNavigation(val url: String, val pinnedContainerId: String?)

}

@Composable
fun MarbleBrAppNavHost(
    activity: AppCompatActivity,
    initialUrl: String?,
    biometricVaultLock: BiometricVaultLock,
    credentialFallbackAuthenticator: CredentialFallbackAuthenticator,
    renderGeckoWebView: Boolean = true,
    onUrlConsumed: () -> Unit = {},
    pinnedContainerId: String? = null,
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalMarbleStrings.current
    var isVaultBiometricUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(initialUrl) {
        if (initialUrl != null) navController.popBackStack("browser", inclusive = false)
    }

    NavHost(
        navController = navController,
        startDestination = "browser",
        enterTransition = {
            // «لایهٔ شیشه‌ای که روی صفحه می‌نشیند»: فقط یک لغزشِ کوتاه از پایین + روشن‌شدنِ نرم
            // و مقیاسِ بسیار جزئی. مقیاسِ بزرگ و جابه‌جاییِ زیاد، همان چیزی بود که حرکت را
            // ارزان و پرش‌دار نشان می‌داد.
            fadeIn(tween(durationMillis = 230, delayMillis = 40)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 420, easing = MarbleSoftEasing),
                    initialOffsetY = { full -> full / 14 },
                ) +
                scaleIn(
                    animationSpec = tween(durationMillis = 420, easing = MarbleSoftEasing),
                    initialScale = 0.972f,
                )
        },
        exitTransition = {
            // صفحهٔ پشتی کمی عقب می‌نشیند (بدون محو شدنِ کامل) تا حس عمق بماند و
            // هنگام برگشت، محتوا «از نو» ساخته نشود.
            fadeOut(tween(durationMillis = 200, easing = FastOutLinearInEasing)) +
                scaleOut(tween(durationMillis = 260, easing = MarbleEasing), targetScale = 0.985f)
        },
        popEnterTransition = {
            // بازگشت، قرینهٔ ورود است: صفحهٔ قبلی آرام بزرگ می‌شود و از لبه بالا می‌آید.
            fadeIn(tween(durationMillis = 240)) +
                scaleIn(tween(durationMillis = 400, easing = MarbleSoftEasing), initialScale = 0.976f) +
                slideInVertically(tween(durationMillis = 400, easing = MarbleSoftEasing)) { full -> full / 20 }
        },
        popExitTransition = {
            // صفحهٔ رویی به‌سمت پایین سُر می‌خورد و محو می‌شود؛ بدون بزرگ‌شدنِ آزاردهنده.
            fadeOut(tween(durationMillis = 170)) +
                slideOutVertically(tween(durationMillis = 300, easing = MarbleEasing)) { full -> full / 16 }
        },
    ) {
        composable("browser") {
            val browserViewModel: BrowserViewModel = hiltViewModel()
            LaunchedEffect(initialUrl, pinnedContainerId) {
                if (!initialUrl.isNullOrBlank()) {
                    browserViewModel.openNewTab(initialUrl, pinnedContainerId = pinnedContainerId)
                    onUrlConsumed()
                }
            }
            BrowserScreen(
                viewModel = browserViewModel,
                onOpenVaultDashboard = { navController.navigate("vault") },
                onOpenSettings = { navController.navigate("settings") },
                onTriggerBiometricPasskeyRelogin = { domain ->
                    coroutineScope.launch {
                        credentialFallbackAuthenticator.attemptBiometricRelogin(
                            activity = activity,
                            rpIdOrDomain = domain,
                        )
                    }
                },
                renderGeckoWebView = renderGeckoWebView,
            )
        }

        composable("vault") {
            val vaultViewModel: VaultDashboardViewModel = hiltViewModel()
            VaultDashboardScreen(
                viewModel = vaultViewModel,
                isBiometricUnlocked = isVaultBiometricUnlocked,
                onRequestBiometricUnlock = {
                    coroutineScope.launch {
                        val result = biometricVaultLock.authenticate(
                            activity = activity,
                            title = strings.biometricPromptTitle,
                            subtitle = strings.biometricPromptSubtitle,
                        )
                        if (result is BiometricAuthResult.Authenticated ||
                            result is BiometricAuthResult.NotAvailableOrNotEnrolled
                        ) {
                            isVaultBiometricUnlocked = true
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
