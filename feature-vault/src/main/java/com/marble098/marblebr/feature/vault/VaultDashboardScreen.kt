package com.marble098.marblebr.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.WebAppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultDashboardViewModel,
    isBiometricUnlocked: Boolean,
    onRequestBiometricUnlock: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val containers by viewModel.containers.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val healthEvents by viewModel.healthEvents.collectAsState()
    val feedbackMessage by viewModel.feedbackMessageFa.collectAsState()

    var pendingSignOutContainer by remember { mutableStateOf<WebAppContainer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("موتور Never Sign-out و ماشین زمان") },
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
            item {
                // Encryption & Biometric Security Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Session Vault رمزنگاری‌شده (AES-256-GCM + Android Keystore)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "تمامی اسنپ‌شات‌های کوکی، localStorage و IndexedDB به‌صورت محلی و رمزنگاری‌شده ذخیره می‌شوند و هیچ داده‌ای به سرور خارجی ارسال نمی‌شود.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!isBiometricUnlocked) {
                            Button(
                                onClick = onRequestBiometricUnlock,
                                modifier = Modifier.testTag("unlock_vault_biometric_btn"),
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("باز کردن قفل بیومتریک برای مدیریت اسنپ‌شات‌ها")
                            }
                        }
                    }
                }
            }

            feedbackMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(msg, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::dismissFeedback) {
                                Text("باشه")
                            }
                        }
                    }
                }
            }

            // Section 1: Isolated Containers & Per-Site Controls
            item {
                Text(
                    text = "کانتینرهای مستقل سایت‌ها و وب‌اپ‌ها (${containers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(containers, key = { it.id }) { container ->
                ContainerControlCard(
                    container = container,
                    onToggleNeverSignOut = { viewModel.toggleSiteNeverSignOut(container, it) },
                    onCookieDaysChanged = { viewModel.updateCookieExpirationDays(container, it) },
                    onToggleKeepAlive = {
                        viewModel.updateKeepAlivePolicy(container, it, container.keepAliveIntervalMinutes)
                    },
                    onSignOutNowClicked = { pendingSignOutContainer = container },
                )
            }

            // Section 2: Session Time Machine
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ماشین زمان نشست‌ها (Session Time Machine)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "با یک لمس به هرکدام از اسنپ‌شات‌های سالم قبلی برگردید:",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (snapshots.isEmpty()) {
                item {
                    Text(
                        text = "هنوز اسنپ‌شاتی ثبت نشده است. با مرور سایت‌ها، اسنپ‌شات‌ها به‌طور خودکار رمزنگاری و ذخیره می‌شوند.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(snapshots, key = { it.id }) { snapshot ->
                    SnapshotTimelineCard(
                        snapshot = snapshot,
                        onRollback = { viewModel.rollbackToSnapshot(snapshot) },
                    )
                }
            }

            // Section 3: Session Health Guard Events
            if (healthEvents.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "گزارش محافظ سلامت نشست (Session Health Guard)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                items(healthEvents, key = { it.id }) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${event.domain} • ${event.statusName}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = event.descriptionFa,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Confirmation Dialog for True Manual Sign-out ("Sign out now")
    pendingSignOutContainer?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingSignOutContainer = null },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
            title = { Text("خروج واقعی از ${target.domain}") },
            text = {
                Text(
                    "با زدن دکمه «خروج قطعی (Sign out now)»، تمامی کوکی‌ها، حافظه کانتینر ${target.contextId} و اسنپ‌شات‌های ذخیره‌شده این سایت در Session Vault پاک می‌شوند. " +
                        "سایر سایت‌ها و کانتینرها کاملاً دست‌نخورده باقی می‌مانند.",
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    onClick = {
                        viewModel.signOutNow(target)
                        pendingSignOutContainer = null
                    },
                ) {
                    Text("خروج قطعی (Sign out now)")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSignOutContainer = null }) {
                    Text("انصراف")
                }
            },
        )
    }
}

@Composable
private fun ContainerControlCard(
    container: WebAppContainer,
    onToggleNeverSignOut: (Boolean) -> Unit,
    onCookieDaysChanged: (Int) -> Unit,
    onToggleKeepAlive: (Boolean) -> Unit,
    onSignOutNowClicked: () -> Unit,
) {
    var sliderDays by remember(container.cookieExpirationDays) {
        mutableFloatStateOf(container.cookieExpirationDays.toFloat())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("container_card_${container.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = container.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Container ID: ${container.contextId}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = container.neverSignOutEnabled,
                    onCheckedChange = onToggleNeverSignOut,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Smart Keep-alive در پس‌زمینه (هر ${container.keepAliveIntervalMinutes} دقیقه)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = container.keepAliveEnabled,
                    onCheckedChange = onToggleKeepAlive,
                    enabled = container.neverSignOutEnabled,
                )
            }

            Column {
                Text(
                    text = "ماندگاری کوکی‌های نشست: ${sliderDays.toInt()} روز",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = sliderDays,
                    onValueChange = { sliderDays = it },
                    onValueChangeFinished = { onCookieDaysChanged(sliderDays.toInt()) },
                    valueRange = 30f..730f,
                    enabled = container.neverSignOutEnabled,
                )
            }

            Button(
                onClick = onSignOutNowClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_now_btn_${container.id}"),
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("خروج دستی واقعی از این سایت (Sign out now)")
            }
        }
    }
}

@Composable
private fun SnapshotTimelineCard(
    snapshot: SessionSnapshotMeta,
    onRollback: () -> Unit,
) {
    val formattedDate = remember(snapshot.capturedAtEpochMillis) {
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            .format(Date(snapshot.capturedAtEpochMillis))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${snapshot.label} • ${snapshot.domain}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "زمان: $formattedDate | کوکی‌ها: ${snapshot.cookieCount} | کلیدهای Storage: ${snapshot.storageKeyCount}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "SHA-256: ${snapshot.payloadSha256.take(16)}...",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            FilledTonalButton(
                onClick = onRollback,
                modifier = Modifier.testTag("rollback_snapshot_${snapshot.id}"),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("بازگشت")
            }
        }
    }
}
