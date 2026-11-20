package com.marble098.marblebr.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ModalBottomSheet
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.siteBadgeAccent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.WebAppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تعداد باکس‌های سایت در هر ردیف. */
private const val SITES_PER_ROW = 2

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
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val strings = LocalMarbleStrings.current

    var pendingSignOutContainer by remember { mutableStateOf<WebAppContainer?>(null) }
    var detailsContainer by remember { mutableStateOf<WebAppContainer?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.vaultTitle, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.vaultSecurityTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = strings.vaultSecurityBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (!isBiometricUnlocked) {
                            Button(
                                onClick = onRequestBiometricUnlock,
                                modifier = Modifier.testTag("unlock_vault_biometric_btn"),
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.unlockVault)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.vaultIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            feedbackMessage?.let { message ->
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
                                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::dismissFeedback) {
                                Text(strings.gotIt)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    icon = Icons.Default.Shield,
                    title = strings.sitesSectionTitle,
                    subtitle = strings.sitesSectionBody,
                )
            }

            if (containers.isEmpty()) {
                item {
                    Text(
                        text = strings.emptySites,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ۵) همهٔ سایت‌های «واردشده» به‌صورت باکس‌های کنار هم، با لوگوی سایت روی هر باکس.
            containers.chunked(SITES_PER_ROW).forEach { rowContainers ->
                item(key = "site_row_" + rowContainers.joinToString("-") { it.id }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowContainers.forEach { container ->
                            SiteBox(
                                container = container,
                                modifier = Modifier.weight(1f),
                                onClick = { detailsContainer = container },
                            )
                        }
                        repeat(SITES_PER_ROW - rowContainers.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    icon = Icons.Default.History,
                    title = strings.timeMachineTitle,
                    subtitle = strings.timeMachineBody,
                )
            }

            if (snapshots.isEmpty()) {
                item {
                    Text(
                        text = strings.emptySnapshots,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(snapshots, key = { it.id }) { snapshot ->
                    SnapshotTimelineCard(
                        snapshot = snapshot,
                        label = localizedSnapshotLabel(strings, snapshot),
                        onRollback = { viewModel.rollbackToSnapshot(snapshot) },
                    )
                }
            }

            if (healthEvents.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Default.Shield,
                        title = strings.healthLogTitle,
                        subtitle = strings.healthLogBody,
                    )
                }
                items(healthEvents, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = event.domain,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = strings.healthStatusLabel(statusOf(event.statusName)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = reasonTextOf(strings, event.descriptionFa),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    detailsContainer?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = { detailsContainer = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ContainerControlPanel(
                container = selected,
                onToggleNeverSignOut = { viewModel.toggleSiteNeverSignOut(selected, it) },
                onCookieDaysChanged = { viewModel.updateCookieExpirationDays(selected, it) },
                onToggleKeepAlive = { viewModel.updateKeepAlivePolicy(selected, it, selected.keepAliveIntervalMinutes) },
                onSignOutNowClicked = {
                    detailsContainer = null
                    pendingSignOutContainer = selected
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingSignOutContainer?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingSignOutContainer = null },
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            title = { Text(strings.signOutDialogTitle(target.domain)) },
            text = { Text(strings.signOutDialogBody(target.domain)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = {
                        viewModel.signOutNow(target)
                        pendingSignOutContainer = null
                    },
                ) {
                    Text(strings.signOutConfirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSignOutContainer = null }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * باکسِ یک سایتِ واردشده در شبکهٔ «سایت‌ها».
 *
 * لوگوی سایت روی خودِ باکس می‌نشیند و باکس‌ها کنار هم چیده می‌شوند. لمس هر باکس،
 * پنلِ کاملِ تنظیمات همان سایت را باز می‌کند.
 */
@Composable
private fun SiteBox(
    container: WebAppContainer,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val accent = remember(container.domain) { siteBadgeAccent(container.domain) }
    Card(
        modifier = modifier
            .testTag("container_card_${container.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SiteBadgeLogo(
                displayName = container.displayName,
                domain = container.domain,
                size = 48.dp,
            )
            Text(
                text = container.displayName.ifBlank { container.domain },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = container.domain,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (container.neverSignOutEnabled) accent else MaterialTheme.colorScheme.outline),
                )
                Text(
                    text = if (container.neverSignOutEnabled) {
                        strings.feedbackProtection(container.domain, true)
                    } else {
                        strings.feedbackProtection(container.domain, false)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ContainerControlPanel(
    container: WebAppContainer,
    onToggleNeverSignOut: (Boolean) -> Unit,
    onCookieDaysChanged: (Int) -> Unit,
    onToggleKeepAlive: (Boolean) -> Unit,
    onSignOutNowClicked: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    var sliderDays by remember(container.cookieExpirationDays) {
        mutableFloatStateOf(container.cookieExpirationDays.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SiteBadgeLogo(
                    displayName = container.displayName,
                    domain = container.domain,
                    size = 38.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = container.displayName.ifBlank { container.domain },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = container.domain,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    text = strings.keepAliveEvery(container.keepAliveIntervalMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = container.keepAliveEnabled,
                    onCheckedChange = onToggleKeepAlive,
                    enabled = container.neverSignOutEnabled,
                )
            }

            Text(
                text = strings.keepSignedInFor(sliderDays.toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = sliderDays,
                onValueChange = { sliderDays = it },
                onValueChangeFinished = { onCookieDaysChanged(sliderDays.toInt()) },
                valueRange = 30f..730f,
                enabled = container.neverSignOutEnabled,
            )

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
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.signOutNow)
            }
    }
}

@Composable
private fun SnapshotTimelineCard(
    snapshot: SessionSnapshotMeta,
    label: String,
    onRollback: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val formattedDate = remember(snapshot.capturedAtEpochMillis) {
        SimpleDateFormat("yyyy/MM/dd · HH:mm", Locale.getDefault())
            .format(Date(snapshot.capturedAtEpochMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = strings.snapshotLine(
                            formattedDate,
                            snapshot.cookieCount,
                            snapshot.storageKeyCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(
                onClick = onRollback,
                modifier = Modifier.testTag("rollback_snapshot_${snapshot.id}"),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.restore)
            }
        }
    }
}

private fun localizedSnapshotLabel(strings: MarbleStrings, snapshot: SessionSnapshotMeta): String = when {
    snapshot.label.isBlank() -> strings.autoSnapshotLabel(snapshot.domain)
    snapshot.label == "background-keep-alive" -> strings.backgroundSnapshotLabel
    else -> snapshot.label
}

private fun statusOf(statusName: String): SessionHealthStatus =
    runCatching { SessionHealthStatus.valueOf(statusName) }.getOrDefault(SessionHealthStatus.HEALTHY)

private fun reasonTextOf(strings: MarbleStrings, stored: String): String {
    val reason = runCatching { HealthReason.valueOf(stored) }.getOrNull()
    return if (reason != null) strings.healthReasonLabel(reason) else stored
}
