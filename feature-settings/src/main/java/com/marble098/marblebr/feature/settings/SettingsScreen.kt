package com.marble098.marblebr.feature.settings

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.ThemeMode

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
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // 1) زبان و ظاهر
            item {
                SettingsSection(
                    icon = Icons.Default.Language,
                    title = strings.appearanceTitle,
                    description = strings.languageDescription,
                ) {
                    OptionChips(
                        options = AppLanguage.entries,
                        selected = prefs.language,
                        label = { strings.languageLabel(it) },
                        onSelect = viewModel::setLanguage,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.themeModeTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OptionChips(
                        options = ThemeMode.entries,
                        selected = prefs.themeMode,
                        label = { strings.themeModeLabel(it) },
                        onSelect = viewModel::setThemeMode,
                    )
                    Spacer(Modifier.height(6.dp))
                    SettingSwitch(
                        title = strings.dynamicColorTitle,
                        description = strings.dynamicColorDescription,
                        checked = prefs.dynamicColorEnabled,
                        onCheckedChange = viewModel::setDynamicColorEnabled,
                    )
                }
            }

            // 2) ماندن در حساب‌ها
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
                    SettingSwitch(
                        title = strings.adBlockTitle,
                        description = strings.adBlockDescription,
                        checked = prefs.adBlockEnabled,
                        onCheckedChange = viewModel::setAdBlockEnabled,
                    )
                }
            }

            // 3) آزاد کردن فضا
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

            // 4) شبکه
            item {
                SettingsSection(
                    icon = Icons.Default.Public,
                    title = strings.networkTitle,
                    description = strings.networkBody,
                ) {
                    Text(strings.dnsLabel, style = MaterialTheme.typography.titleMedium)
                    OptionChips(
                        options = DnsMode.entries,
                        selected = selectedDns,
                        label = { strings.dnsModeLabel(it) },
                        onSelect = { selectedDns = it },
                    )
                    if (selectedDns == DnsMode.CUSTOM) {
                        OutlinedTextField(
                            value = customDohUri,
                            onValueChange = { customDohUri = it },
                            label = { Text(strings.dnsCustomHint) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(strings.proxyLabel, style = MaterialTheme.typography.titleMedium)
                    OptionChips(
                        options = ProxyType.entries,
                        selected = selectedProxyType,
                        label = { strings.proxyTypeLabel(it) },
                        onSelect = { selectedProxyType = it },
                    )
                    if (selectedProxyType != ProxyType.NONE) {
                        OutlinedTextField(
                            value = proxyHost,
                            onValueChange = { proxyHost = it },
                            label = { Text(strings.proxyHostHint) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = proxyPortText,
                            onValueChange = { proxyPortText = it },
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

            // 5) درباره
            item {
                SettingsSection(
                    icon = Icons.Default.Info,
                    title = strings.aboutTitle,
                    description = strings.aboutBody(viewModel.appVersionName),
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
                            text = viewModel.appVersionName.ifBlank { "-" },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

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
