package com.marble098.marblebr.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleBackButton
import com.marble098.marblebr.core.designsystem.MarbleSegmentedRow
import com.marble098.marblebr.core.designsystem.SelectablePalettes
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.designsystem.paletteColorScheme
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.BackButtonBehavior
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.ColorPalette
import com.marble098.marblebr.core.model.CredentialSaveMode
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.MarbleLinks
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SearchEngine
import com.marble098.marblebr.core.model.ThemeMode
import com.marble098.marblebr.core.preferences.AddressSelectionMode

/**
 * تب‌های صفحهٔ تنظیمات؛ هر دستهٔ تنظیمات فقط در تبِ خودش دیده می‌شود.
 */
private enum class SettingsTab(val icon: ImageVector) {
    APPEARANCE(Icons.Default.Language),
    BROWSER(Icons.Default.Home),
    PROTECTION(Icons.Default.Shield),
    NETWORK(Icons.Default.Public),
    ABOUT(Icons.Default.Info),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val prefs by viewModel.preferences.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val strings = LocalMarbleStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = SettingsTab.entries

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissSnackbar()
    }

    var selectedDns by remember(prefs.proxyDnsConfig.dnsMode) {
        mutableStateOf(prefs.proxyDnsConfig.dnsMode)
    }
    var customDohUri by remember(prefs.proxyDnsConfig.customDohUri) {
        mutableStateOf(prefs.proxyDnsConfig.customDohUri)
    }
    var selectedProxyType by remember(prefs.proxyDnsConfig.proxyType) {
        mutableStateOf(prefs.proxyDnsConfig.proxyType)
    }
    var selectedEngineId by remember(prefs.selectedSearchEngineId) {
        mutableStateOf(prefs.selectedSearchEngineId)
    }
    var selectedHomeShortcut by remember(prefs.homeShortcutPosition) {
        mutableStateOf(prefs.homeShortcutPosition)
    }
    var proxyHost by remember(prefs.proxyDnsConfig.proxyHost) {
        mutableStateOf(prefs.proxyDnsConfig.proxyHost)
    }
    var proxyPortText by remember(prefs.proxyDnsConfig.proxyPort) {
        mutableStateOf(prefs.proxyDnsConfig.proxyPort.toString())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // سربرگ شیشه‌ای: دکمهٔ بازگشتِ انیمیشنی + عنوان + انتخاب‌گرِ کپسولیِ تب‌ها
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MarbleBackButton(
                        onClick = onNavigateBack,
                        contentDescription = strings.back,
                        testTag = "settings_back_btn",
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = strings.settingsIntro,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                MarbleSegmentedRow(
                    options = tabs,
                    selected = tabs[selectedTabIndex],
                    label = { it.title(strings) },
                    icon = { it.icon },
                    onSelect = { selectedTabIndex = tabs.indexOf(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_tab_row"),
                    testTagPrefix = "settings_tab_segment",
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                ) {
                    item(key = "tab_intro") {
                        Text(
                            text = tabs[selectedTabIndex].description(strings),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    when (tabs[selectedTabIndex]) {
                        SettingsTab.APPEARANCE -> appearanceTab(strings, prefs, viewModel)

                        SettingsTab.BROWSER -> browserTab(
                            strings = strings,
                            prefs = prefs,
                            viewModel = viewModel,
                            selectedEngineId = selectedEngineId,
                            onSelectEngine = { engine ->
                                selectedEngineId = engine.id
                                viewModel.selectSearchEngine(engine)
                            },
                            selectedHomeShortcut = selectedHomeShortcut,
                            onSelectHomeShortcut = { position ->
                                selectedHomeShortcut = position
                                viewModel.setHomeShortcutPosition(position)
                            },
                        )

                        SettingsTab.PROTECTION -> protectionTab(strings, prefs, viewModel)

                        SettingsTab.NETWORK -> networkTab(
                            strings = strings,
                            viewModel = viewModel,
                            selectedDns = selectedDns,
                            onSelectDns = { selectedDns = it },
                            customDohUri = customDohUri,
                            onCustomDohUri = { customDohUri = it },
                            selectedProxyType = selectedProxyType,
                            onSelectProxyType = { selectedProxyType = it },
                            proxyHost = proxyHost,
                            onProxyHost = { proxyHost = it },
                            proxyPortText = proxyPortText,
                            onProxyPortText = { proxyPortText = it },
                        )

                        SettingsTab.ABOUT -> aboutTab(strings, viewModel.appVersionName)
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun SettingsTab.description(strings: MarbleStrings): String = when (this) {
    SettingsTab.APPEARANCE -> strings.appearanceTitle
    SettingsTab.BROWSER -> strings.homeScreenTitle
    SettingsTab.PROTECTION -> strings.protectionSectionBody
    SettingsTab.NETWORK -> strings.networkBody
    SettingsTab.ABOUT -> strings.aboutTitle
}

private fun SettingsTab.title(strings: MarbleStrings): String = when (this) {
    SettingsTab.APPEARANCE -> strings.settingsTabAppearance
    SettingsTab.BROWSER -> strings.settingsTabBrowser
    SettingsTab.PROTECTION -> strings.settingsTabProtection
    SettingsTab.NETWORK -> strings.settingsTabNetwork
    SettingsTab.ABOUT -> strings.settingsTabAbout
}

/* ------------------------------------------------ تبِ ظاهر */

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.appearanceTab(
    strings: MarbleStrings,
    prefs: com.marble098.marblebr.core.preferences.BrowserPreferences,
    viewModel: SettingsViewModel,
) {
    item {
        SettingsSection(
            icon = Icons.Default.Language,
            title = strings.appearanceTitle,
            description = strings.languageDescription,
        ) {
            SettingLabel(strings.languageTitle)
            OptionChips(
                options = AppLanguage.entries,
                selected = prefs.language,
                label = { strings.languageLabel(it) },
                onSelect = viewModel::setLanguage,
            )
            Spacer(Modifier.height(10.dp))
            SettingLabel(strings.themeModeTitle)
            OptionChips(
                options = ThemeMode.entries,
                selected = prefs.themeMode,
                label = { strings.themeModeLabel(it) },
                onSelect = viewModel::setThemeMode,
            )
        }
    }

    // پالت‌های رنگی حرفه‌ای + پیش‌نمایش زنده.
    item {
        SettingsSection(
            icon = Icons.Default.AutoAwesome,
            title = strings.colorPaletteTitle,
            description = strings.colorPaletteDescription,
        ) {
            PalettePicker(
                selected = prefs.colorPalette,
                strings = strings,
                onSelect = viewModel::setColorPalette,
            )
            if (prefs.colorPalette == ColorPalette.DYNAMIC) {
                Text(
                    text = strings.paletteDynamicHint,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            ThemePreviewCard(strings = strings)
        }
    }

    // ظاهر نوارهای بالا و پایین (شیشه‌ای شناور بی‌پس‌زمینه)
    item {
        SettingsSection(
            icon = Icons.Default.AutoAwesome,
            title = strings.chromeBarStyleTitle,
            description = strings.chromeBarStyleDescription,
        ) {
            OptionChips(
                options = ChromeBarStyle.entries,
                selected = prefs.chromeBarStyle,
                label = { strings.chromeBarStyleLabel(it) },
                onSelect = viewModel::setChromeBarStyle,
            )
        }
    }
}

/* ------------------------------------------------ تبِ مرورگر */

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.browserTab(
    strings: MarbleStrings,
    prefs: com.marble098.marblebr.core.preferences.BrowserPreferences,
    viewModel: SettingsViewModel,
    selectedEngineId: String,
    onSelectEngine: (SearchEngine) -> Unit,
    selectedHomeShortcut: HomeShortcutPosition,
    onSelectHomeShortcut: (HomeShortcutPosition) -> Unit,
) {
    item {
        // دکمهٔ بازگشت — کاربر انتخاب می‌کند «بک» چه معنایی داشته باشد.
        SettingsSection(
            icon = Icons.Default.Home,
            title = strings.backBehaviorTitle,
            description = strings.backBehaviorDescription,
        ) {
            OptionChips(
                options = BackButtonBehavior.entries,
                selected = prefs.backButtonBehavior,
                label = { strings.backBehaviorLabel(it) },
                onSelect = viewModel::setBackButtonBehavior,
            )
        }
    }

    item {
        SettingsSection(
            icon = Icons.Default.Home,
            title = strings.homeScreenTitle,
            description = strings.homeShortcutPositionTitle,
        ) {
            SettingSwitch(
                title = strings.googleNewTab,
                description = strings.googleNewTabDescription,
                checked = prefs.openGoogleOnNewTab,
                onCheckedChange = viewModel::setOpenGoogleOnNewTab,
            )
            Spacer(Modifier.height(12.dp))
            SettingLabel(strings.homeShortcutPositionTitle)
            Text(
                text = strings.homeShortcutPositionDescription,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OptionChips(
                options = listOf(
                    HomeShortcutPosition.BUILT_IN,
                    HomeShortcutPosition.FLOATING,
                    HomeShortcutPosition.NONE,
                ),
                selected = selectedHomeShortcut,
                label = { position ->
                    when (position) {
                        HomeShortcutPosition.BUILT_IN -> strings.shortcutPositionBuiltIn
                        HomeShortcutPosition.FLOATING -> strings.shortcutPositionFloating
                        HomeShortcutPosition.NONE -> strings.shortcutPositionNone
                    }
                },
                onSelect = onSelectHomeShortcut,
            )
        }
    }

    item {
        SettingsSection(
            icon = Icons.Default.AutoAwesome,
            title = strings.tabsBehaviorTitle,
            description = strings.quickTabs,
        ) {
            SettingSwitch(
                title = strings.askBeforeClosingTabs,
                description = strings.askBeforeClosingTabsDescription,
                checked = prefs.askBeforeClosingTabs,
                onCheckedChange = viewModel::setAskBeforeClosingTabs,
            )
        }
    }

    item {
        SettingsSection(
            icon = Icons.Default.Search,
            title = strings.searchEngineTitle,
            description = strings.searchEngineDescription,
        ) {
            SearchEngine.all.forEach { engine ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectEngine(engine) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedEngineId == engine.id,
                        onClick = { onSelectEngine(engine) },
                    )
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            text = engine.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = engine.searchUrlTemplate,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    item {
        SettingsSection(
            icon = Icons.Default.Search,
            title = strings.addressSelectionTitle,
            description = strings.addressSelectionDescription,
        ) {
            OptionChips(
                options = AddressSelectionMode.entries,
                selected = prefs.addressSelectionMode,
                label = { mode ->
                    when (mode) {
                        AddressSelectionMode.SELECT_ALL -> strings.addressSelectAll
                        AddressSelectionMode.SELECT_PART -> strings.addressSelectPart
                        AddressSelectionMode.CURSOR -> strings.addressCursor
                    }
                },
                onSelect = viewModel::setAddressSelectionMode,
            )
        }
    }
}

/* ------------------------------------------------ تبِ محافظت */

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.protectionTab(
    strings: MarbleStrings,
    prefs: com.marble098.marblebr.core.preferences.BrowserPreferences,
    viewModel: SettingsViewModel,
) {
    item {
        SettingsSection(
            icon = Icons.Default.Shield,
            title = strings.protectionSectionTitle,
            description = strings.protectionSectionBody,
        ) {
            SettingSwitch(
                title = strings.globalNeverSignOutTitle,
                description = strings.globalNeverSignOutDescription,
                checked = prefs.globalNeverSignOutEnabled,
                onCheckedChange = viewModel::setGlobalNeverSignOut,
            )
            SettingSwitch(
                title = strings.biometricTitle,
                description = strings.biometricDescription,
                checked = prefs.biometricLockRequired,
                onCheckedChange = viewModel::setBiometricLockRequired,
            )
            SettingSwitch(
                title = strings.keepAliveTitle,
                description = strings.keepAliveDescription,
                checked = prefs.smartKeepAliveEnabled,
                onCheckedChange = viewModel::setSmartKeepAliveEnabled,
            )
            SettingSwitch(
                title = strings.credentialTitle,
                description = strings.credentialDescription,
                checked = prefs.credentialFallbackEnabled,
                onCheckedChange = viewModel::setCredentialFallbackEnabled,
            )
            // سه حالتِ روشن به‌جای یک کلیدِ مبهم: «همیشه بپرس»، «خودکارِ مواردِ روشن»، «هرگز».
            // متنِ هر گزینه می‌گوید دقیقاً چه می‌شود، چون این تصمیم روی حریم خصوصی اثر دارد.
            SettingChoice(
                title = strings.saveModeTitle,
                description = strings.saveModeAskBody,
                options = listOf(
                    strings.saveModeAsk to CredentialSaveMode.ASK,
                    strings.saveModeAuto to CredentialSaveMode.AUTO_SAVE,
                    strings.saveModeOff to CredentialSaveMode.DISABLED,
                ),
                selected = prefs.credentialSaveMode,
                onSelect = viewModel::setCredentialSaveMode,
            )
            SettingSwitch(
                title = strings.otpAssistTitle,
                description = strings.otpAssistBody,
                checked = prefs.otpAssistEnabled,
                onCheckedChange = viewModel::setOtpAssistEnabled,
            )
            SettingSwitch(
                title = strings.autoFillEnabledTitle,
                description = strings.autoFillEnabledBody,
                checked = prefs.autoFillCredentials,
                onCheckedChange = viewModel::setAutoFillCredentials,
            )
            SettingSwitch(
                title = strings.loginGuardEnabledTitle,
                description = strings.loginGuardEnabledBody,
                checked = prefs.loginGuardEnabled,
                onCheckedChange = viewModel::setLoginGuardEnabled,
            )
            SettingSwitch(
                title = strings.pulseToggle,
                description = strings.pulseDescription,
                checked = prefs.sessionPulseEnabled,
                onCheckedChange = viewModel::setSessionPulseEnabled,
            )
        }
    }

    item {
        SettingsSection(
            icon = Icons.Default.Shield,
            title = strings.adBlockTitle,
            description = strings.adBlockDescription,
        ) {
            SettingSwitch(
                title = strings.adBlockTitle,
                description = strings.adBlockDescription,
                checked = prefs.adBlockEnabled,
                onCheckedChange = viewModel::setAdBlockEnabled,
            )
        }
    }
}

/* ------------------------------------------------ تبِ شبکه */

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.networkTab(
    strings: MarbleStrings,
    viewModel: SettingsViewModel,
    selectedDns: DnsMode,
    onSelectDns: (DnsMode) -> Unit,
    customDohUri: String,
    onCustomDohUri: (String) -> Unit,
    selectedProxyType: ProxyType,
    onSelectProxyType: (ProxyType) -> Unit,
    proxyHost: String,
    onProxyHost: (String) -> Unit,
    proxyPortText: String,
    onProxyPortText: (String) -> Unit,
) {
    item {
        SettingsSection(
            icon = Icons.Default.Public,
            title = strings.networkTitle,
            description = strings.networkBody,
        ) {
            SettingLabel(strings.dnsLabel)
            OptionChips(
                options = DnsMode.entries,
                selected = selectedDns,
                label = { strings.dnsModeLabel(it) },
                onSelect = onSelectDns,
            )
            if (selectedDns == DnsMode.CUSTOM) {
                OutlinedTextField(
                    value = customDohUri,
                    onValueChange = onCustomDohUri,
                    label = { Text(strings.dnsCustomHint) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(10.dp))
            SettingLabel(strings.proxyLabel)
            OptionChips(
                options = ProxyType.entries,
                selected = selectedProxyType,
                label = { strings.proxyTypeLabel(it) },
                onSelect = onSelectProxyType,
            )
            if (selectedProxyType != ProxyType.NONE) {
                OutlinedTextField(
                    value = proxyHost,
                    onValueChange = onProxyHost,
                    label = { Text(strings.proxyHostHint) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = proxyPortText,
                    onValueChange = onProxyPortText,
                    label = { Text(strings.proxyPortHint) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.updateDnsAndProxy(
                        dnsMode = selectedDns,
                        customDohUri = customDohUri,
                        proxyType = selectedProxyType,
                        proxyHost = proxyHost,
                        proxyPort = proxyPortText.toIntOrNull() ?: 1080,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.saveNetwork)
            }
        }
    }

    item {
        SettingsSection(
            icon = Icons.Default.CleaningServices,
            title = strings.freeSpaceTitle,
            description = strings.freeSpaceDescription,
        ) {
            Button(
                onClick = viewModel::clearCachesOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_clear_cache_only_btn"),
            ) {
                Text(strings.freeSpaceButton)
            }
        }
    }
}

/* ------------------------------------------------ تبِ درباره */

private fun LazyListScope.aboutTab(strings: MarbleStrings, appVersionName: String) {
    item {
        SettingsSection(
            icon = Icons.Default.Info,
            title = strings.aboutTitle,
            description = strings.aboutBody(appVersionName),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = appVersionName.ifBlank { "-" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = MarbleLinks.GITHUB_DISPLAY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = MarbleLinks.TELEGRAM_DISPLAY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ------------------------------------------------ اجزای مشترک */

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                style = ChromeBarStyle.GLASS,
                shape = shape,
                fillAlphaOverride = 0.58f,
                elevation = 6.dp,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(9.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * انتخابِ یکی از چند گزینه با رادیو.
 *
 * چرا به‌جای «چند کلیدِ روشن/خاموش»؟ چون حالتِ ذخیرهٔ ورود سه وضعیتِ متقابلاً ناسازگار است و
 * دو سوییچِ مستقل، کاربر را به ترکیبِ بی‌معنا (مثلاً «بپرس» و «هرگز» هم‌زمان) می‌رساند.
 */
@Composable
private fun <T> SettingChoice(
    title: String,
    description: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.padding(top = 4.dp)) {
            options.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { if (value != selected) onSelect(value) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = value == selected,
                        onClick = { if (value != selected) onSelect(value) },
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/* --------------------------------------------- پالت رنگی و پیش‌نمایش --------------------------------------------- */

/**
 * انتخاب‌گرِ پالت رنگی.
 *
 * هر گزینه یک «کارتِ کوچکِ رنگ» است: دو رنگِ امضای همان پالت (اصلی + مکمل) در یک کپسولِ
 * گرادیانی، به‌همراه نامِ پالت. برخلاف `FilterChip`های متنی، کاربر **پیش از انتخاب** می‌بیند
 * تم چه رنگی است. حالتِ «رنگ‌های گوشی» با آیکنِ گوشی و بدون گرادیان نمایش داده می‌شود.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PalettePicker(
    selected: ColorPalette,
    strings: MarbleStrings,
    onSelect: (ColorPalette) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // «رنگ‌های گوشی» همیشه اول است تا گزینهٔ پیش‌فرضِ Material You پیدا باشد.
        // پالت‌های دیگر با سواچِ دو رنگِ امضا نمایش داده می‌شوند.
        SelectablePalettes.forEach { palette ->
            val scheme = if (palette == ColorPalette.DYNAMIC) null else paletteColorScheme(palette, dark = false)
            PaletteChip(
                label = strings.colorPaletteLabel(palette),
                swatch = scheme?.let { it.primary to it.tertiary },
                isSelected = selected == palette,
                tag = palette.name.lowercase(),
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun PaletteChip(
    label: String,
    swatch: Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>?,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) colors.primary.copy(alpha = 0.16f) else colors.surface.copy(alpha = 0.6f),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            .testTag("palette_chip_$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (swatch != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(swatch.first, swatch.second))),
            )
        } else {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.primary else colors.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * پیش‌نمایش زندهٔ تم: چند بلوکِ کوچک با رنگ‌های فعلی برنامه.
 *
 * چرا؟ تغییر پالت در تنظیمات فوراً روی همین صفحه اعمال می‌شود؛ این کارت نشان می‌دهد رنگ
 * دکمه، چیپ و کارت در تمِ تازه چه می‌شود تا کاربر بدون رفت‌وبرگشت تصمیم بگیرد.
 */
@Composable
private fun ThemePreviewCard(strings: MarbleStrings) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLow.copy(alpha = 0.75f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = strings.themePreviewTitle,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
            fontWeight = FontWeight.Bold,
            color = colors.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primaryContainer),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.secondaryContainer),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainerHighest),
            )
        }
        Text(
            text = strings.themePreviewBody,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
            color = colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
