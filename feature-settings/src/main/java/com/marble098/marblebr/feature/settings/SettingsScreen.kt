package com.marble098.marblebr.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات مرورگر MarbleBr") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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
            // 1. Appearance & Persian RTL
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "ظاهر، تم Material 3 Expressive و زبان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(mode.labelFa) },
                                )
                            }
                        }
                        SettingSwitchRow(
                            title = "رنگ پویا (Dynamic Color / Material You)",
                            checked = prefs.dynamicColorEnabled,
                            onCheckedChange = viewModel::setDynamicColorEnabled,
                        )
                        SettingSwitchRow(
                            title = "چیدمان راست‌به‌چپ فارسی (Persian RTL)",
                            checked = prefs.forcePersianRtl,
                            onCheckedChange = viewModel::setForcePersianRtl,
                        )
                    }
                }
            }

            // 2. Never Sign-out Engine & Biometric Security
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "موتور Never Sign-out و امنیت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        SettingSwitchRow(
                            title = "فعال‌سازی پیش‌فرض Never Sign-out برای سایت‌های جدید",
                            checked = prefs.globalNeverSignOutEnabled,
                            onCheckedChange = viewModel::setGlobalNeverSignOut,
                        )
                        SettingSwitchRow(
                            title = "قفل بیومتریک (اثر انگشت / چهره) برای Session Vault",
                            checked = prefs.biometricLockRequired,
                            onCheckedChange = viewModel::setBiometricLockRequired,
                        )
                        SettingSwitchRow(
                            title = "Smart Keep-alive در پس‌زمینه (تمدید نشست‌های لغزشی)",
                            checked = prefs.smartKeepAliveEnabled,
                            onCheckedChange = viewModel::setSmartKeepAliveEnabled,
                        )
                        SettingSwitchRow(
                            title = "ورود خودکار با Credential Manager / Passkey در صورت ابطال سرور",
                            checked = prefs.credentialFallbackEnabled,
                            onCheckedChange = viewModel::setCredentialFallbackEnabled,
                        )
                        SettingSwitchRow(
                            title = "بلوکر تبلیغ و ردیاب سبک",
                            checked = prefs.adBlockEnabled,
                            onCheckedChange = viewModel::setAdBlockEnabled,
                        )
                        Button(
                            onClick = viewModel::clearCachesOnly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_clear_cache_only_btn"),
                        ) {
                            Text("پاک کردن کش (بدون پاک شدن نشست‌ها و لاگین‌ها)")
                        }
                    }
                }
            }

            // 3. Optional Proxy & DNS-over-HTTPS
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "تنظیمات DNS-over-HTTPS و پروکسی اختیاری",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("حالت DNS امن:", style = MaterialTheme.typography.bodyMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DnsMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = selectedDns == mode,
                                    onClick = { selectedDns = mode },
                                    label = { Text(mode.labelFa) },
                                )
                            }
                        }
                        if (selectedDns == DnsMode.CUSTOM) {
                            OutlinedTextField(
                                value = customDohUri,
                                onValueChange = { customDohUri = it },
                                label = { Text("آدرس سرور DoH سفارشی") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Text("نوع پروکسی:", style = MaterialTheme.typography.bodyMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProxyType.entries.forEach { pType ->
                                FilterChip(
                                    selected = selectedProxyType == pType,
                                    onClick = { selectedProxyType = pType },
                                    label = { Text(pType.labelFa) },
                                )
                            }
                        }

                        if (selectedProxyType != ProxyType.NONE) {
                            OutlinedTextField(
                                value = proxyHost,
                                onValueChange = { proxyHost = it },
                                label = { Text("هاست یا IP پروکسی (مثلاً 127.0.0.1)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = proxyPortText,
                                onValueChange = { proxyPortText = it },
                                label = { Text("پورت پروکسی (مثلاً 1080)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

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
                            Text("ذخیره تنظیمات شبکه و DNS")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
