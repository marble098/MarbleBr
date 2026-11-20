package com.marble098.marblebr

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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

        // لبه‌به‌لبه با نوارهای شفاف؛ رنگ آیکن‌ها را خودِ تم (روشن/تاریک) تنظیم می‌کند.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM),
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

    private companion object {
        val LIGHT_SCRIM: Int = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM: Int = Color.argb(0xE6, 0x10, 0x12, 0x18)
    }
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
            // صفحهٔ تازه از پایین می‌آید، کمی بزرگ می‌شود و نرم روشن می‌شود.
            fadeIn(tween(durationMillis = 220, delayMillis = 40)) +
                slideInVertically(tween(durationMillis = 420, easing = MarbleEasing)) { full -> full / 6 } +
                scaleIn(tween(durationMillis = 420, easing = MarbleEasing), initialScale = 0.92f)
        },
        exitTransition = {
            // صفحهٔ پشتی آرام عقب می‌رود و محو می‌شود تا حس عمق بدهد.
            fadeOut(tween(durationMillis = 180, easing = FastOutLinearInEasing)) +
                slideOutVertically(tween(durationMillis = 320, easing = MarbleEasing)) { full -> -full / 14 } +
                scaleOut(tween(durationMillis = 320, easing = MarbleEasing), targetScale = 0.94f)
        },
        popEnterTransition = {
            fadeIn(tween(durationMillis = 200)) +
                scaleIn(tween(durationMillis = 380, easing = MarbleEasing), initialScale = 0.94f) +
                slideInVertically(tween(durationMillis = 380, easing = MarbleEasing)) { full -> -full / 18 }
        },
        popExitTransition = {
            fadeOut(tween(durationMillis = 200)) +
                scaleOut(tween(durationMillis = 340, easing = MarbleEasing), targetScale = 1.04f) +
                slideOutVertically(tween(durationMillis = 340, easing = MarbleEasing)) { full -> full / 5 }
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
