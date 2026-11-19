package com.marble098.marblebr.feature.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Environment
import com.marble098.marblebr.core.database.DownloadDao
import com.marble098.marblebr.core.database.DownloadEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.DownloadState
import com.marble098.marblebr.core.model.WebAppContainer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نصب‌کننده وب‌اپ و PWA روی صفحه اصلی اندروید (Pinned Shortcut) همراه با Container مستقل.
 */
@Singleton
class PwaShortcutInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun requestPinWebAppShortcut(
        container: WebAppContainer,
        launchUrl: String,
        title: String,
        longLabel: String = title,
    ): Boolean {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(launchUrl)).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_CONTAINER_ID, container.id)
            putExtra(EXTRA_CONTEXT_ID, container.contextId)
            putExtra(EXTRA_PWA_MODE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcutInfo = ShortcutInfo.Builder(context, "pwa_${container.id}")
            .setShortLabel(title.take(18).ifBlank { container.displayName })
            .setLongLabel(longLabel)
            .setIcon(Icon.createWithResource(context, android.R.drawable.sym_def_app_icon))
            .setIntent(launchIntent)
            .build()

        return shortcutManager.requestPinShortcut(shortcutInfo, null)
    }

    companion object {
        const val EXTRA_CONTAINER_ID = "marblebr.extra.CONTAINER_ID"
        const val EXTRA_CONTEXT_ID = "marblebr.extra.CONTEXT_ID"
        const val EXTRA_PWA_MODE = "marblebr.extra.PWA_MODE"
    }
}

/**
 * مدیریت دانلود فایل‌ها در مرورگر با ارسال کوکی‌های کانتینر.
 */
@Singleton
class BrowserDownloadLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val localizationManager: LocalizationManager,
) {
    suspend fun enqueueDownload(
        url: String,
        suggestedFileName: String? = null,
        mimeType: String = "application/octet-stream",
        cookieHeader: String? = null,
    ) {
        val fileName = suggestedFileName?.takeIf { it.isNotBlank() }
            ?: Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() }
            ?: "marblebr_download_${System.currentTimeMillis()}"

        val strings = localizationManager.current()
        runCatching {
            val dm = context.getSystemService(DownloadManager::class.java)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription(strings.downloadNotificationText)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            if (!cookieHeader.isNullOrBlank()) {
                request.addRequestHeader("Cookie", cookieHeader)
            }
            dm?.enqueue(request)
        }

        downloadDao.upsert(
            DownloadEntity(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                totalBytes = -1L,
                downloadedBytes = 0L,
                stateName = DownloadState.COMPLETED.name,
                destinationPath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName",
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }
}
