package com.marble098.marblebr

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marble098.marblebr.core.designsystem.MarbleBrTheme
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.engine.CredentialFallbackAuthenticator
import com.marble098.marblebr.feature.browser.BrowserScreen
import com.marble098.marblebr.feature.browser.BrowserViewModel
import com.marble098.marblebr.feature.settings.SettingsScreen
import com.marble098.marblebr.feature.settings.SettingsViewModel
import com.marble098.marblebr.feature.vault.VaultDashboardScreen
import com.marble098.marblebr.feature.vault.VaultDashboardViewModel
import com.marble098.marblebr.vault.BiometricAuthResult
import com.marble098.marblebr.vault.BiometricVaultLock
import dagger.hilt.android.AndroidEntryPoint
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
        enableEdgeToEdge()

        val initialUrlFromIntent = intent?.dataString

        setContent {
            val prefs by preferencesRepository.preferencesFlow.collectAsState(initial = BrowserPreferences())
            MarbleBrTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColorEnabled,
                forceRtl = prefs.forcePersianRtl,
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
                onOpenVaultDashboard = {
                    navController.navigate("vault")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
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
                        val result = biometricVaultLock.authenticate(activity)
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
