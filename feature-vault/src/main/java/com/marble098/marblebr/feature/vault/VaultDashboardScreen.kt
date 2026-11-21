package com.marble098.marblebr.feature.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleBackButton
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.MarbleGlassIconButton
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.designsystem.siteBadgeAccent
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.PulseRecommendation
import com.marble098.marblebr.core.model.PulseUrgency
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.vault.RestorePreview
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * داشبورد «مراقبت از حساب‌ها» — بازطراحی کامل (ظاهری، عملکردی و فنی).
 *
 * ### نقدِ نسخهٔ قبل
 * - **ظاهری:** یک کارت بزرگِ آبی، متن‌های توضیحیِ طولانی، باکس‌های سایتِ هم‌شکل با نامِ
 *   دامنهٔ تکراری و لیستِ زمان‌ها که همهٔ سایت‌ها را قاطی می‌کرد؛ چیدمان با `chunked(2)` در
 *   LazyColumn هر بار کل ردیف‌ها را از نو می‌ساخت.
 * - **عملکردی:** تغییرات سوییچ در پنل بازتاب نمی‌شد، «ذخیره» از داشبورد ممکن نبود، حذفِ
 *   نسخه وجود نداشت و بازهٔ زنده‌سازی پس از تغییر اعمال نمی‌شد.
 * - **فنی:** `detailsContainer` شیءِ کهنه را نگه می‌داشت، اسنپ‌شات‌های همهٔ سایت‌ها در یک
 *   لیست بی‌گروه نمایش داده می‌شد، و کلیکِ پشت‌سرهم می‌توانست عملیات را دوباره اجرا کند.
 *
 * ### ساختار تازه
 * ۱. **سربرگ شیشه‌ای** با آمار زنده (تعداد سایت، درصد محافظت، تعداد نسخه‌ها، آخرین ذخیره).
 * ۲. **جستجو + فیلترهای چیپی** برای پیدا کردن سریع بین سایت‌ها.
 * ۳. **شبکهٔ دو ستونیِ سایت‌ها** با لوگوی رنگی، بج وضعیت و نوار سلامت.
 * ۴. **تاریخچهٔ گروه‌بندی‌شده بر اساس سایت** در قالبِ تایم‌لاین عمودی.
 * ۵. **پنل جزئیات کامل** با سوییچ‌های توضیح‌دار، اسلایدر مدت ورود، بازهٔ زنده‌سازی و
 *    عملیات‌های ذخیره/export/حذف — همه با بازخورد فوری.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultDashboardViewModel,
    isBiometricUnlocked: Boolean,
    onRequestBiometricUnlock: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val overview by viewModel.overview.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val healthEvents by viewModel.healthEvents.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val openSite by viewModel.openSite.collectAsState()
    val openSiteSnapshots by viewModel.openSiteSnapshots.collectAsState()
    val busyAction by viewModel.busyAction.collectAsState()
    val globalProtection by viewModel.globalProtectionEnabled.collectAsState()
    val expiryWatch by viewModel.expiryWatch.collectAsState()
    val pulsePlan by viewModel.pulsePlan.collectAsState()
    val pendingBackup by viewModel.pendingBackup.collectAsState()
    val restorePreview by viewModel.restorePreview.collectAsState()
    val backupBusy by viewModel.backupBusy.collectAsState()
    val strings = LocalMarbleStrings.current
    val context = LocalContext.current
    var showBackupDialog by remember { mutableStateOf(false) }

    // ذخیرهٔ فایل پشتیبان با انتخاب‌گرِ خودِ سیستم (بدونِ دسترسی به حافظهٔ خارجی).
    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val payload = pendingBackup
        if (uri != null && payload != null) {
            val written = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(payload.text.toByteArray(Charsets.UTF_8))
                } != null
            }.getOrDefault(false)
            if (!written) viewModel.onBackupShareFailed()
        }
        viewModel.consumePendingBackup()
    }

    // فایلِ پشتیبانِ ساخته‌شده آماده است؛ نامِ پیشنهادی را به انتخاب‌گر بده.
    LaunchedEffect(pendingBackup?.fileName) {
        pendingBackup?.let { backupSaver.launch(it.fileName) }
    }

    var pendingSignOutContainer by remember { mutableStateOf<WebAppContainer?>(null) }
    var pendingDeleteSnapshot by remember { mutableStateOf<SessionSnapshotMeta?>(null) }

    // پیام‌های موفقیت/خطا خودشان کنار می‌روند؛ قبلاً تا بستنِ دستی روی صفحه می‌ماندند.
    LaunchedEffect(feedback?.id) {
        if (feedback != null) {
            delay(3_200)
            viewModel.dismissFeedback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        VaultBackdrop()

        // نوار وضعیت (status bar): پیش از این، محتوای اسکرول‌شوندهٔ این صفحه از لبهٔ بالای
        // پنجره شروع می‌شد و سربرگ زیر نوار وضعیتِ گوشی می‌رفت. حالا ارتفاع نوار وضعیت به
        // padding بالای فهرست اضافه می‌شود تا هیچ چیزی زیر آن نباشد (لبه‌به‌لبه حفظ می‌شود).
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MarbleLayout.Screen,
                end = MarbleLayout.Screen,
                top = statusBarTop + MarbleLayout.Inner,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MarbleLayout.SectionGap),
        ) {
            item(key = "topbar") {
                VaultTopBar(
                    strings = strings,
                    onNavigateBack = onNavigateBack,
                    onRequestBiometricUnlock = onRequestBiometricUnlock,
                    isBiometricUnlocked = isBiometricUnlocked,
                )
            }

            item(key = "hero") {
                VaultHero(
                    overview = overview,
                    strings = strings,
                    isBiometricUnlocked = isBiometricUnlocked,
                    busyAction = busyAction,
                    onRequestBiometricUnlock = onRequestBiometricUnlock,
                    onProtectAll = { viewModel.setProtectionForAll(true) },
                    onUnprotectAll = { viewModel.setProtectionForAll(false) },
                )
            }

            if (!globalProtection) {
                item(key = "protection_off") {
                    InfoBanner(
                        icon = Icons.Default.Shield,
                        message = strings.vaultProtectionOffBanner,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            item(key = "credentials") {
                CredentialVaultSection(
                    vault = viewModel.credentialVault,
                    unlocked = isBiometricUnlocked,
                    onUnlock = onRequestBiometricUnlock,
                )
            }

            item(key = "backup") {
                BackupCard(
                    strings = strings,
                    busy = backupBusy,
                    onCreate = { showBackupDialog = true },
                    onPreview = { text, password -> viewModel.previewRestore(text, password) },
                    onApply = viewModel::applyRestore,
                    onDiscard = viewModel::discardRestorePreview,
                    preview = restorePreview,
                )
            }

            item(key = "session_pulse") {
                SessionPulseCard(
                    strings = strings,
                    recommendations = pulsePlan,
                    busyAction = busyAction,
                    onRunNow = viewModel::runSessionPulseNow,
                )
            }

            item(key = "filters") {
                VaultSearchAndFilters(
                    query = query,
                    onQueryChange = viewModel::setQuery,
                    filter = filter,
                    onFilterChange = viewModel::setFilter,
                    strings = strings,
                    counts = mapOf(
                        VaultSiteFilter.ALL to overview.allContainers.size,
                        VaultSiteFilter.PROTECTED to overview.protectedSites,
                        VaultSiteFilter.UNPROTECTED to overview.allContainers.count { !it.neverSignOutEnabled },
                        VaultSiteFilter.PINNED to overview.allContainers.count { it.isPinnedPwa },
                    ),
                )
            }

            item(key = "expiry_guardian") {
                ExpiryGuardianCard(
                    expiring = expiryWatch,
                    strings = strings,
                    busyAction = busyAction,
                    onRenewSite = { containerId ->
                        overview.allContainers.firstOrNull { it.id == containerId }?.let(viewModel::renewSiteLogin)
                    },
                    onRenewAll = viewModel::renewAllExpiringLogins,
                    onInsureNow = viewModel::insureSessionsNow,
                )
            }

            item(key = "sites_header") {
                SectionTitle(
                    icon = Icons.Default.Shield,
                    title = strings.vaultSectionSites,
                    subtitle = strings.sitesSectionBody,
                    trailing = overview.filteredContainers
                        .takeIf { it.isNotEmpty() }
                        ?.let { strings.vaultSnapshotCount(it.size) },
                )
            }

            if (overview.filteredContainers.isEmpty()) {
                item(key = "sites_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.Shield,
                        text = if (overview.allContainers.isEmpty()) strings.emptySites else strings.vaultNoMatch,
                        hint = if (overview.allContainers.isEmpty()) strings.vaultFilterEmpty else null,
                    )
                }
            } else {
                val rows = overview.filteredContainers.chunked(SITES_PER_ROW)
                rows.forEachIndexed { index, rowContainers ->
                    item(key = "site_row_${rowContainers.joinToString("-") { it.id }}_$index") {
                        Row(
                            // IntrinsicSize.Max: هر دو کارتِ یک ردیف دقیقاً هم‌ارتفاع می‌شوند،
                            // حتی اگر یکی بج «میانبر» داشته باشد و دیگری نداشته باشد.
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
                        ) {
                            rowContainers.forEach { container ->
                                SiteCard(
                                    container = container,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    snapshotCount = snapshots.count { it.containerId == container.id },
                                    onClick = { viewModel.openSiteDetails(container) },
                                )
                            }
                            if (rowContainers.size < SITES_PER_ROW) {
                                repeat(SITES_PER_ROW - rowContainers.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            item(key = "timeline_header") {
                SectionTitle(
                    icon = Icons.Default.History,
                    title = strings.vaultSectionTimeline,
                    subtitle = strings.timeMachineBody,
                    trailing = if (snapshots.isEmpty()) null else strings.vaultSnapshotCount(snapshots.size),
                )
            }

            if (snapshots.isEmpty()) {
                item(key = "timeline_empty") {
                    EmptyStateCard(icon = Icons.Default.History, text = strings.emptySnapshots, hint = null)
                }
            } else {
                // نسخه‌ها بر اساس سایت گروه‌بندی می‌شوند تا فهرستِ طولانی و بی‌نظم قبلی نباشد.
                val grouped = snapshots.groupBy { it.domain }.values.take(GROUPED_TIMELINE_SITES)
                grouped.forEach { siteSnapshots ->
                    val domain = siteSnapshots.first().domain
                    item(key = "timeline_group_$domain") {
                        SnapshotGroup(
                            domain = domain,
                            snapshots = siteSnapshots.take(3),
                            totalCount = siteSnapshots.size,
                            strings = strings,
                            busyAction = busyAction,
                            onRollback = { snapshot -> viewModel.rollbackToSnapshot(snapshot) },
                            onDelete = { snapshot -> pendingDeleteSnapshot = snapshot },
                            onTogglePin = viewModel::toggleSnapshotPinned,
                        )
                    }
                }
            }

            item(key = "activity_header") {
                SectionTitle(
                    icon = Icons.Default.VerifiedUser,
                    title = strings.vaultSectionActivity,
                    subtitle = strings.healthLogBody,
                    trailing = null,
                )
            }
            if (healthEvents.isEmpty()) {
                item(key = "activity_empty") {
                    EmptyStateCard(icon = Icons.Default.VerifiedUser, text = strings.vaultEmptyActivity, hint = null)
                }
            } else {
                items(healthEvents.take(6), key = { "event_${it.id}" }) { event ->
                    HealthEventRow(
                        domain = event.domain,
                        statusName = event.statusName,
                        reasonCode = event.descriptionFa,
                        timestamp = event.timestampEpochMillis,
                        strings = strings,
                    )
                }
            }
        }

        // پیام موفقیت/خطا شناور پایین صفحه تا با اسکرول از دید خارج نشود.
        AnimatedVisibility(
            visible = feedback != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            feedback?.let { message ->
                SnackStyleFeedback(message = message.message, onDismiss = viewModel::dismissFeedback)
            }
        }
    }

    openSite?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSiteDetails,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SiteControlPanel(
                container = selected,
                snapshots = openSiteSnapshots,
                strings = strings,
                busyAction = busyAction,
                onToggleNeverSignOut = { viewModel.toggleSiteNeverSignOut(selected, it) },
                onCookieDaysChanged = { viewModel.updateCookieExpirationDays(selected, it) },
                onToggleKeepAlive = { viewModel.updateKeepAlivePolicy(selected, it, selected.keepAliveIntervalMinutes) },
                onKeepAliveIntervalChanged = { viewModel.updateKeepAliveInterval(selected, it) },
                onSaveNow = { viewModel.captureSnapshotNow(selected) },
                onRollback = viewModel::rollbackToSnapshot,
                onDeleteSnapshot = { pendingDeleteSnapshot = it },
                onTogglePin = viewModel::toggleSnapshotPinned,
                onSignOutNow = {
                    viewModel.closeSiteDetails()
                    pendingSignOutContainer = selected
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showBackupDialog) {
        BackupCreateDialog(
            strings = strings,
            busy = backupBusy,
            onDismiss = { showBackupDialog = false },
            onCreate = { password ->
                showBackupDialog = false
                viewModel.createBackup(password)
            },
        )
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

    pendingDeleteSnapshot?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSnapshot = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(strings.vaultDeleteConfirmTitle) },
            text = { Text(strings.vaultDeleteConfirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSnapshot(target)
                        pendingDeleteSnapshot = null
                    },
                ) {
                    Text(strings.vaultDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSnapshot = null }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

/* ----------------------------------------- پس‌زمینه ----------------------------------------- */

@Composable
private fun VaultBackdrop() {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primary.copy(alpha = if (dark) 0.20f else 0.10f),
                        colors.background,
                        colors.tertiary.copy(alpha = if (dark) 0.12f else 0.06f),
                    ),
                ),
            ),
    )
}

/* ------------------------------------------ سربرگ ------------------------------------------ */

@Composable
private fun VaultTopBar(
    strings: MarbleStrings,
    onNavigateBack: () -> Unit,
    onRequestBiometricUnlock: () -> Unit,
    isBiometricUnlocked: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarbleBackButton(
            onClick = onNavigateBack,
            contentDescription = strings.back,
            testTag = "vault_back_btn",
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.vaultTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = strings.vaultIntro,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MarbleGlassIconButton(
            icon = if (isBiometricUnlocked) Icons.Default.VerifiedUser else Icons.Default.Fingerprint,
            contentDescription = strings.unlockVault,
            onClick = onRequestBiometricUnlock,
            style = ChromeBarStyle.FLOATING,
            tint = if (isBiometricUnlocked) MaterialTheme.colorScheme.primary else null,
            testTag = "unlock_vault_biometric_btn",
        )
    }
}

@Composable
private fun VaultHero(
    overview: VaultOverview,
    strings: MarbleStrings,
    isBiometricUnlocked: Boolean,
    busyAction: String?,
    onRequestBiometricUnlock: () -> Unit,
    onProtectAll: () -> Unit,
    onUnprotectAll: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    val progress by animateFloatAsState(
        targetValue = overview.protectedPercent.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "protected progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                style = ChromeBarStyle.GLASS,
                shape = shape,
                fillAlphaOverride = 0.55f,
                elevation = 10.dp,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.vaultHeroTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.vaultHeroBody,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!isBiometricUnlocked) {
                MarbleGlassIconButton(
                    icon = Icons.Default.Fingerprint,
                    contentDescription = strings.unlockVault,
                    onClick = onRequestBiometricUnlock,
                    style = ChromeBarStyle.GLASS,
                    tint = colors.primary,
                    size = 38.dp,
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color = colors.primary,
            trackColor = colors.primary.copy(alpha = 0.16f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill(
                modifier = Modifier.weight(1f),
                value = overview.totalSites.toString(),
                label = strings.vaultStatSites,
            )
            StatPill(
                modifier = Modifier.weight(1f),
                value = overview.protectedSites.toString(),
                label = strings.vaultStatProtected,
                accent = colors.primary,
            )
            StatPill(
                modifier = Modifier.weight(1f),
                value = overview.totalSnapshots.toString(),
                label = strings.vaultStatSnapshots,
                accent = colors.tertiary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = overview.latestSnapshotAt?.let {
                    strings.vaultLastSavedAgo(dateLabel(it))
                } ?: strings.vaultNever,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (busyAction == "protect-all") {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.primary,
                )
            } else {
                // اقدام گروهی: قبلاً برای روشن/خاموش کردنِ محافظت باید برای هر سایت جدا
                // وارد پنل جزئیات می‌شد؛ حالا یک لمس کافی است.
                val allProtected = overview.totalSites > 0 && overview.protectedSites == overview.totalSites
                SmallActionPill(
                    text = if (allProtected) strings.vaultUnprotectAll else strings.vaultProtectAll,
                    icon = if (allProtected) Icons.Default.Shield else Icons.Default.VerifiedUser,
                    onClick = if (allProtected) onUnprotectAll else onProtectAll,
                    enabled = overview.totalSites > 0,
                )
            }
        }
    }
}

@Composable
private fun SmallActionPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.primary.copy(alpha = if (enabled) 0.16f else 0.08f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) colors.primary else colors.outline,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
            color = if (enabled) colors.primary else colors.outline,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.55f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accent ?: colors.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.5.sp, lineHeight = 12.sp),
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------ جستجو و فیلترها ------------------------------------ */

@Composable
private fun VaultSearchAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: VaultSiteFilter,
    onFilterChange: (VaultSiteFilter) -> Unit,
    strings: MarbleStrings,
    counts: Map<VaultSiteFilter, Int> = emptyMap(),
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .liquidGlass(
                    style = ChromeBarStyle.GLASS,
                    shape = RoundedCornerShape(22.dp),
                    fillAlphaOverride = 0.62f,
                    glow = false,
                    elevation = 4.dp,
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("vault_search_field"),
                singleLine = true,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = strings.vaultSearchHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.dismiss, modifier = Modifier.size(15.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VaultSiteFilter.entries.forEach { option ->
                val selected = option == filter
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) {
                                Brush.horizontalGradient(listOf(colors.primary, colors.tertiary))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(colors.surface.copy(alpha = 0.6f), colors.surface.copy(alpha = 0.6f)),
                                )
                            },
                        )
                        .border(
                            width = 0.9.dp,
                            color = if (selected) Color.Transparent else colors.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(50),
                        )
                        .clickable(role = Role.Tab, onClick = { onFilterChange(option) })
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    val count = counts[option]
                    Text(
                        text = if (count != null && count > 0) {
                            "${option.label(strings)} · $count"
                        } else {
                            option.label(strings)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
                        color = if (selected) colors.onPrimary else colors.onSurface,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun VaultSiteFilter.label(strings: MarbleStrings): String = when (this) {
    VaultSiteFilter.ALL -> strings.vaultFilterAll
    VaultSiteFilter.PROTECTED -> strings.vaultFilterProtected
    VaultSiteFilter.UNPROTECTED -> strings.vaultFilterUnprotected
    VaultSiteFilter.PINNED -> strings.vaultFilterPinned
}

/* ------------------------------------- کارتِ سایت ------------------------------------- */

@Composable
private fun SiteCard(
    container: WebAppContainer,
    snapshotCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val accent = remember(container.domain) { siteBadgeAccent(container.domain) }
    val shape = RoundedCornerShape(MarbleLayout.CardCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "site card press",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("container_card_${container.id}")
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.45f), colors.outlineVariant.copy(alpha = 0.22f)),
                ),
                shape = shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SiteBadgeLogo(
                displayName = container.displayName,
                domain = container.domain,
                size = 38.dp,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (container.neverSignOutEnabled) accent else colors.outline),
            )
        }
        Text(
            text = container.displayName.ifBlank { container.domain },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = container.domain,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(
                text = if (container.neverSignOutEnabled) strings.vaultSiteProtected else strings.vaultSiteUnprotected,
                accent = if (container.neverSignOutEnabled) accent else colors.outline,
            )
            if (container.isPinnedPwa) {
                StatusChip(text = strings.vaultSitePinned, accent = colors.tertiary)
            }
        }
        Text(
            text = strings.vaultSnapshotCount(snapshotCount),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusChip(text: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.5.sp),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ----------------------------------- تایم‌لاین نسخه‌ها ----------------------------------- */

@Composable
private fun SnapshotGroup(
    domain: String,
    snapshots: List<SessionSnapshotMeta>,
    totalCount: Int,
    strings: MarbleStrings,
    busyAction: String?,
    onRollback: (SessionSnapshotMeta) -> Unit,
    onDelete: (SessionSnapshotMeta) -> Unit,
    onTogglePin: (SessionSnapshotMeta) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = remember(domain) { siteBadgeAccent(domain) }
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.55f))
            .border(0.9.dp, colors.outlineVariant.copy(alpha = 0.35f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SiteBadgeLogo(displayName = domain, domain = domain, size = 26.dp)
            Text(
                text = domain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.vaultSnapshotCount(totalCount),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                color = colors.onSurfaceVariant,
            )
        }
        snapshots.forEach { snapshot ->
            SnapshotRow(
                snapshot = snapshot,
                strings = strings,
                accent = accent,
                isBusy = busyAction == "rollback-${snapshot.id}",
                onRollback = { onRollback(snapshot) },
                onDelete = { onDelete(snapshot) },
                onTogglePin = { onTogglePin(snapshot) },
            )
        }
    }
}

@Composable
private fun SnapshotRow(
    snapshot: SessionSnapshotMeta,
    strings: MarbleStrings,
    accent: Color,
    isBusy: Boolean,
    onRollback: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val rowShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(colors.surfaceContainerLow.copy(alpha = 0.65f))
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        snapshot.isPinned -> colors.tertiary
                        snapshot.isHealthy -> accent
                        else -> colors.outline
                    },
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = localizedSnapshotLabel(strings, snapshot),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.snapshotLine(
                    dateLabel(snapshot.capturedAtEpochMillis),
                    snapshot.cookieCount,
                    snapshot.storageKeyCount,
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier
                .size(30.dp)
                .testTag("pin_snapshot_${snapshot.id}"),
        ) {
            Icon(
                imageVector = if (snapshot.isPinned) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (snapshot.isPinned) strings.vaultGoldenUnpin else strings.vaultGoldenPin,
                tint = if (snapshot.isPinned) colors.tertiary else colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp),
            )
        }
        IconButton(
            onClick = onRollback,
            enabled = !isBusy,
            modifier = Modifier
                .size(30.dp)
                .testTag("rollback_snapshot_${snapshot.id}"),
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = strings.restore,
                tint = colors.primary,
                modifier = Modifier.size(17.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = strings.vaultDelete,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/* ------------------------- نگهبان انقضا و بیمهٔ نشست ------------------------- */

/**
 * کارتِ «نگهبان انقضا» + «بیمهٔ نشست».
 *
 * دو قابلیتِ تازهٔ نگه‌داشتنِ ورود در یک کارت جمع شده‌اند چون هر دو یک سؤال را جواب می‌دهند:
 * «آیا ورودهای من الان در خطرند؟»
 * - اگر ورودی نزدیک انقضا باشد، تب باز پس از بررسی ورود می‌تواند دوباره ذخیره شود.
 * - دکمهٔ «بیمهٔ فوری» از تب‌های محافظت‌شدهٔ باز نسخهٔ تازه می‌گیرد.
 */
@Composable
private fun ExpiryGuardianCard(
    expiring: List<ExpiringSite>,
    strings: MarbleStrings,
    busyAction: String?,
    onRenewSite: (String) -> Unit,
    onRenewAll: () -> Unit,
    onInsureNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MarbleLayout.CardCorner)
    val accent = if (expiring.isEmpty()) colors.primary else colors.tertiary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.55f))
            .border(0.9.dp, accent.copy(alpha = 0.35f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.10f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.vaultExpiryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (expiring.isEmpty()) strings.vaultExpiryAllFresh else strings.vaultExpiryBody,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        expiring.take(4).forEach { site ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expiring_site_${site.containerId}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
            ) {
                SiteBadgeLogo(displayName = site.domain, domain = site.domain, size = 26.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.domain,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (site.isExpired) {
                            strings.vaultExpiryExpired
                        } else {
                            strings.vaultExpiryDaysLeft(site.daysLeft)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                        color = if (site.isExpired) colors.error else colors.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = { onRenewSite(site.containerId) },
                    enabled = busyAction == null,
                ) {
                    Text(strings.vaultExpiryRenew, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
        ) {
            if (expiring.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onRenewAll,
                    enabled = busyAction == null,
                    modifier = Modifier.weight(1f).testTag("renew_all_logins_btn"),
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.vaultExpiryRenewAll, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FilledTonalButton(
                onClick = onInsureNow,
                enabled = busyAction == null,
                modifier = Modifier.weight(1f).testTag("insure_sessions_btn"),
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.vaultInsuranceRun, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Text(
            text = strings.vaultInsuranceBody,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
            color = colors.outline,
        )
    }
}

/* ------------------------------------ گزارش محافظت ------------------------------------ */

@Composable
private fun HealthEventRow(
    domain: String,
    statusName: String,
    reasonCode: String,
    timestamp: Long,
    strings: MarbleStrings,
) {
    val colors = MaterialTheme.colorScheme
    val accent = remember(domain) { siteBadgeAccent(domain) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = domain,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reasonTextOf(strings, reasonCode),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = strings.healthStatusLabel(statusOf(statusName)),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.5.sp),
                color = colors.primary,
                maxLines = 1,
            )
            Text(
                text = dateLabel(timestamp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp),
                color = colors.outline,
                maxLines = 1,
            )
        }
    }
}

/* ------------------------------------ پنل تنظیمات سایت ------------------------------------ */

@Composable
private fun SiteControlPanel(
    container: WebAppContainer,
    snapshots: List<SessionSnapshotMeta>,
    strings: MarbleStrings,
    busyAction: String?,
    onToggleNeverSignOut: (Boolean) -> Unit,
    onCookieDaysChanged: (Int) -> Unit,
    onToggleKeepAlive: (Boolean) -> Unit,
    onKeepAliveIntervalChanged: (Int) -> Unit,
    onSaveNow: () -> Unit,
    onRollback: (SessionSnapshotMeta) -> Unit,
    onDeleteSnapshot: (SessionSnapshotMeta) -> Unit,
    onTogglePin: (SessionSnapshotMeta) -> Unit,
    onSignOutNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = remember(container.domain) { siteBadgeAccent(container.domain) }
    var sliderDays by remember(container.cookieExpirationDays) {
        mutableFloatStateOf(container.cookieExpirationDays.toFloat())
    }
    var keepAliveMinutes by remember(container.keepAliveIntervalMinutes) {
        mutableFloatStateOf(container.keepAliveIntervalMinutes.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // پنلِ جزئیات بلند است (سوییچ‌ها، اسلایدرها و فهرست نسخه‌ها) و روی نمایشگرهای
            // کوچک از صفحه بیرون می‌زد؛ حالا داخل شیت اسکرول می‌شود.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SiteBadgeLogo(displayName = container.displayName, domain = container.domain, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = container.displayName.ifBlank { container.domain },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = container.domain,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(
                text = if (container.neverSignOutEnabled) strings.vaultSiteProtected else strings.vaultSiteUnprotected,
                accent = if (container.neverSignOutEnabled) accent else colors.outline,
            )
        }

        Text(
            text = strings.vaultSiteDetailsTitle,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )

        PanelSwitch(
            icon = Icons.Default.Shield,
            title = strings.vaultProtectSwitchTitle,
            body = strings.vaultProtectSwitchBody,
            checked = container.neverSignOutEnabled,
            accent = accent,
            onCheckedChange = onToggleNeverSignOut,
        )

        PanelSwitch(
            icon = Icons.Default.Bolt,
            title = strings.vaultKeepAliveSwitchTitle,
            body = strings.vaultKeepAliveSwitchBody,
            checked = container.keepAliveEnabled,
            accent = colors.tertiary,
            enabled = container.neverSignOutEnabled,
            onCheckedChange = onToggleKeepAlive,
        )

        if (container.keepAliveEnabled && container.neverSignOutEnabled) {
            Text(
                text = strings.vaultKeepAliveEveryMinutes(keepAliveMinutes.toInt()),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = colors.onSurfaceVariant,
            )
            Slider(
                value = keepAliveMinutes,
                onValueChange = { keepAliveMinutes = it },
                onValueChangeFinished = { onKeepAliveIntervalChanged(keepAliveMinutes.toInt()) },
                valueRange = keepAliveRange(),
                steps = 27,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = strings.vaultCookieDaysTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                text = strings.vaultCookieDaysBody,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            text = strings.keepSignedInFor(sliderDays.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Slider(
            value = sliderDays,
            onValueChange = { sliderDays = it },
            onValueChangeFinished = { onCookieDaysChanged(sliderDays.toInt()) },
            valueRange = cookieDaysRange(),
            enabled = container.neverSignOutEnabled,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onSaveNow,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_snapshot_now_btn_${container.id}"),
                enabled = busyAction == null,
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.vaultSaveNow, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onSignOutNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("sign_out_now_btn_${container.id}"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(strings.signOutNow, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (snapshots.isNotEmpty()) {
            Text(
                text = strings.vaultSnapshotsOfSite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            snapshots.take(4).forEach { snapshot ->
                SnapshotRow(
                    snapshot = snapshot,
                    strings = strings,
                    accent = accent,
                    isBusy = busyAction == "rollback-${snapshot.id}",
                    onRollback = { onRollback(snapshot) },
                    onDelete = { onDeleteSnapshot(snapshot) },
                    onTogglePin = { onTogglePin(snapshot) },
                )
            }
        }
    }
}

@Composable
private fun PanelSwitch(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLow.copy(alpha = if (enabled) 0.75f else 0.4f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/* --------------------------------------- پشتیبان‌گیری --------------------------------------- */

/**
 * کارتِ «پشتیبان‌گیری از گذرواژه‌ها و ورودها».
 *
 * دو مسیرِ روشن: ساختِ فایلِ رمزشده با گذرواژهٔ خودِ کاربر، و بازگردانیِ دومرحله‌ای
 * (اول پیش‌نمایش، بعد اعمال). هیچ دادهٔ موجودی بی‌خبر بازنویسی نمی‌شود.
 */
@Composable
private fun BackupCard(
    strings: MarbleStrings,
    busy: Boolean,
    onCreate: () -> Unit,
    onPreview: (String, String) -> Unit,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    preview: RestorePreview?,
) {
    val colors = MaterialTheme.colorScheme
    var password by remember { mutableStateOf("") }
    var fileText by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.42f))
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.30f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(
            icon = Icons.Default.CloudUpload,
            title = strings.backupTitle,
            subtitle = strings.backupDescription,
            trailing = null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate, enabled = !busy) { Text(strings.backupCreate) }
        }
        Text(
            text = strings.backupPasswordWarning,
            style = MaterialTheme.typography.labelSmall,
            color = colors.error,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = strings.backupRestoreBody,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        BasicTextField(
            value = fileText,
            onValueChange = { fileText = it },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerHigh.copy(alpha = 0.55f))
                .padding(10.dp),
            decorationBox = { inner ->
                if (fileText.isEmpty()) {
                    Text(
                        text = strings.backupRestorePickHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outline,
                    )
                }
                inner()
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text(strings.backupPassword) },
                modifier = Modifier.weight(1f),
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = { onPreview(fileText, password) },
                enabled = !busy && fileText.isNotBlank() && password.isNotEmpty(),
            ) {
                Text(strings.backupRestore)
            }
        }
        if (preview != null) {
            Text(
                text = strings.backupRestorePreviewTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = strings.backupRestorePreviewBody(
                    preview.newCredentials + preview.newSessions + preview.newSites,
                    preview.updatedCredentials + preview.updatedSessions + preview.updatedSites,
                    preview.duplicateCredentials,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, enabled = !busy) { Text(strings.backupRestoreApply) }
                TextButton(onClick = onDiscard) { Text(strings.cancel) }
            }
        }
    }
}

/** گفت‌وگوی ساختِ فایل پشتیبان: گذرواژه + تکرار، با بررسیِ محلی قبل از رمزگذاری. */
@Composable
private fun BackupCreateDialog(
    strings: MarbleStrings,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && password != confirm
    val tooShort = password.isNotEmpty() && password.length < 8
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
        title = { Text(strings.backupCreate) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.backupCreateBody)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text(strings.backupPassword) },
                    isError = tooShort,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    singleLine = true,
                    label = { Text(strings.backupPasswordConfirm) },
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = when {
                        tooShort -> strings.backupPasswordTooShort
                        mismatch -> strings.backupPasswordMismatch
                        else -> strings.backupPasswordHint
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tooShort || mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(password) },
                enabled = !busy && password.length >= 8 && password == confirm,
            ) {
                Text(strings.backupCreate)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}

/* ---------------------------------------- نبضِ نشست ---------------------------------------- */

/** ایدهٔ تازه: پیشنهادِ بیدارسازیِ نشست‌ها پیش از خواب‌آلود شدن، بر پایهٔ عادتِ واقعیِ کاربر. */
@Composable
private fun SessionPulseCard(
    strings: MarbleStrings,
    recommendations: List<PulseRecommendation>,
    busyAction: String?,
    onRunNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.42f))
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.30f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(
            icon = Icons.Default.Bolt,
            title = strings.pulseTitle,
            subtitle = strings.pulseDescription,
            trailing = if (recommendations.isEmpty()) null else recommendations.size.toString(),
        )
        if (recommendations.isEmpty()) {
            Text(
                text = strings.pulseEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        } else {
            recommendations.take(4).forEach { recommendation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(urgencyColor(recommendation.urgency)),
                    )
                    Text(
                        text = strings.pulseSiteLine(
                            recommendation.domain,
                            urgencyLabel(recommendation.urgency, strings),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Button(
                onClick = onRunNow,
                enabled = busyAction == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.pulseRunNow)
            }
        }
    }
}

@Composable
private fun urgencyColor(urgency: PulseUrgency): Color = when (urgency) {
    PulseUrgency.LOW -> MaterialTheme.colorScheme.outline
    PulseUrgency.NORMAL -> MaterialTheme.colorScheme.primary
    PulseUrgency.HIGH -> MaterialTheme.colorScheme.tertiary
    PulseUrgency.CRITICAL -> MaterialTheme.colorScheme.error
}

private fun urgencyLabel(urgency: PulseUrgency, strings: MarbleStrings): String = when (urgency) {
    PulseUrgency.LOW -> strings.pulseUrgencyLow
    PulseUrgency.NORMAL -> strings.pulseUrgencyNormal
    PulseUrgency.HIGH -> strings.pulseUrgencyHigh
    PulseUrgency.CRITICAL -> strings.pulseUrgencyCritical
}

/* -------------------------------------- اجزای مشترک -------------------------------------- */

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String?,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                color = colors.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyStateCard(icon: ImageVector, text: String, hint: String?) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.40f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(colors.outlineVariant.copy(alpha = 0.45f), colors.outlineVariant.copy(alpha = 0.12f)),
                ),
                shape = shape,
            )
            .padding(vertical = 18.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                color = colors.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InfoBanner(
    icon: ImageVector,
    message: String,
    containerColor: Color,
    contentColor: Color,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SnackStyleFeedback(message: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .liquidGlass(
                style = ChromeBarStyle.GLASS,
                shape = RoundedCornerShape(18.dp),
                fillAlphaOverride = 0.92f,
                glow = false,
                elevation = 12.dp,
            )
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
        }
    }
}

/* ----------------------------------------- کمکی ----------------------------------------- */

/** قالبِ تاریخِ مشترکِ داشبورد؛ یک‌بار ساخته می‌شود (نه در هر بازترکیب). */
private const val DATE_PATTERN = "yyyy/MM/dd · HH:mm"

private fun dateLabel(epochMillis: Long): String =
    SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date(epochMillis))

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

/** بازهٔ اسلایدرِ زنده‌سازی، هم‌گام با محدودیت‌های ViewModel. */
private fun keepAliveRange(): ClosedFloatingPointRange<Float> =
    VaultDashboardViewModel.MIN_KEEP_ALIVE_MINUTES.toFloat()..VaultDashboardViewModel.MAX_KEEP_ALIVE_MINUTES.toFloat()

/** بازهٔ اسلایدرِ مدت ماندن ورود، هم‌گام با محدودیت‌های ViewModel. */
private fun cookieDaysRange(): ClosedFloatingPointRange<Float> =
    VaultDashboardViewModel.MIN_COOKIE_DAYS.toFloat()..VaultDashboardViewModel.MAX_COOKIE_DAYS.toFloat()

/** تعداد کارت‌های سایت در هر ردیف شبکه. */
private const val SITES_PER_ROW = 2

/** حداکثر تعداد گروه‌های سایت در تایم‌لاین (برای جلوگیری از فهرست بی‌پایان). */
private const val GROUPED_TIMELINE_SITES = 6
