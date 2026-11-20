package com.marble098.marblebr.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.BackButtonBehavior
import com.marble098.marblebr.core.model.ChromeBarStyle
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                        }
                    },
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("settings_tab_row"),
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.testTag("settings_tab_${tab.name.lowercase()}"),
                            text = {
                                Text(
                                    text = tab.title(strings),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = strings.settingsIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
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

            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

private fun SettingsTab.title(strings: MarbleStrings): String = when (this) {
    SettingsTab.APPEARANCE -> strings.settingsTabAppearance
    SettingsTab.BROWSER -> strings.settingsTabBrowser
    SettingsTab.PROTECTION -> strings.settingsTabProtection
    SettingsTab.NETWORK -> strings.settingsTabNetwork
    SettingsTab.ABOUT -> strings.settingsTabAbout
}

/* ------------------------------------------------ تبِ ظاهر */

@Composable
private fun appearanceTab(
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
            Spacer(Modifier.height(8.dp))
            SettingSwitch(
                title = strings.dynamicColorTitle,
                description = strings.dynamicColorDescription,
                checked = prefs.dynamicColorEnabled,
                onCheckedChange = viewModel::setDynamicColorEnabled,
            )
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

@Composable
private fun browserTab(
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
            OptionChips(
                options = HomeShortcutPosition.entries,
                selected = selectedHomeShortcut,
                label = { position ->
                    when (position) {
                        HomeShortcutPosition.NONE -> strings.shortcutPositionNone
                        HomeShortcutPosition.TOP -> strings.shortcutPositionTop
                        HomeShortcutPosition.BOTTOM -> strings.shortcutPositionBottom
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

@Composable
private fun protectionTab(
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
@Composable
private fun networkTab(
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

@Composable
private fun aboutTab(strings: MarbleStrings, appVersionName: String) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
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
