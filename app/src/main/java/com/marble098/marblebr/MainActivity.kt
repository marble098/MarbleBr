package com.marble098.marblebr

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: BrowserPreferencesRepository

    @Inject
    lateinit var biometricVaultLock: BiometricVaultLock

    @Inject
    lateinit var credentialFallbackAuthenticator: CredentialFallbackAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // لبه‌به‌لبه با نوارهای شفاف؛ رنگ آیکن‌ها را خودِ تم (روشن/تاریک) تنظیم می‌کند.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM),
        )

        val initialUrlFromIntent = intent?.dataString

        setContent {
            val prefs by preferencesRepository.preferencesFlow.collectAsState(initial = BrowserPreferences())
            val coroutineScope = rememberCoroutineScope()
            var showRateDialog by remember { mutableStateOf(false) }

            val rateLaunchThreshold = 3

            fun openPlayStore() {
                val packageName = this.packageName
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (marketIntent.resolveActivity(packageManager) != null) {
                    startActivity(marketIntent)
                    return
                }
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(Intent.createChooser(webIntent, null))
            }

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
                    onRateNow = {
                        showRateDialog = false
                        coroutineScope.launch {
                            preferencesRepository.setRateDialogShown(true)
                        }
                        openPlayStore()
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
                    initialUrl = initialUrlFromIntent,
                    biometricVaultLock = biometricVaultLock,
                    credentialFallbackAuthenticator = credentialFallbackAuthenticator,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

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
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalMarbleStrings.current
    var isVaultBiometricUnlocked by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = "browser",
    ) {
        composable("browser") {
            val browserViewModel: BrowserViewModel = hiltViewModel()
            if (!initialUrl.isNullOrBlank()) {
                remember(initialUrl) {
                    browserViewModel.navigateToInput(initialUrl)
                    true
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
                initialShowHome = initialUrl.isNullOrBlank(),
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
