package com.marble098.marblebr.feature.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import com.marble098.marblebr.core.database.DownloadDao
import com.marble098.marblebr.core.database.DownloadEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.DownloadState
import com.marble098.marblebr.core.model.WebAppContainer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
 * مدیریت دانلود فایل‌ها از طریق DownloadManager اندروید، بدون انتقال کوکی‌های کانتینر.
 */
@Singleton
class BrowserDownloadLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val localizationManager: LocalizationManager,
) {
    suspend fun enqueueDownload(url: String, mimeType: String = "application/octet-stream"): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "http") return false
        val fileName = URLUtil.guessFileName(url, null, mimeType).replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
        val strings = localizationManager.current()
        val dm = context.getSystemService(DownloadManager::class.java) ?: return false
        val downloadId = withContext(Dispatchers.IO) {
            runCatching {
                val request = DownloadManager.Request(uri)
                    .setTitle(fileName)
                    .setDescription(strings.downloadNotificationText)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                dm.enqueue(request)
            }.getOrNull()
        } ?: return false
        downloadDao.upsert(
            DownloadEntity(
                id = downloadId.toString(),
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                totalBytes = -1L,
                downloadedBytes = 0L,
                stateName = DownloadState.IN_PROGRESS.name,
                destinationPath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName",
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return true
    }

    suspend fun refreshDownloads() = withContext(Dispatchers.IO) {
        val dm = context.getSystemService(DownloadManager::class.java) ?: return@withContext
        for (download in downloadDao.observeAll().first()) {
            val id = download.id.toLongOrNull() ?: continue
            if (download.stateName != DownloadState.IN_PROGRESS.name) continue
            val updated = dm.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    download.copy(stateName = DownloadState.FAILED.name)
                } else {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val state = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadState.COMPLETED
                        DownloadManager.STATUS_FAILED -> DownloadState.FAILED
                        else -> DownloadState.IN_PROGRESS
                    }
                    download.copy(
                        stateName = state.name,
                        downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                        totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    )
                }
            }
            if (updated != null && updated != download) downloadDao.upsert(updated)
        }
    }
}
