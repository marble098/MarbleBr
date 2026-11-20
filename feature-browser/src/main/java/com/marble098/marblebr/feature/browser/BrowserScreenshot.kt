package com.marble098.marblebr.feature.browser

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import org.mozilla.geckoview.GeckoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * نگه‌داری نرم از آخرین [GeckoView] فعال تا بتوان از صفحهٔ در حال نمایش عکس گرفت.
 *
 * این کلاس هیچ داده‌ای را جایی نمی‌فرستد؛ عکس فقط در پوشهٔ اختصاصی خود اپ ذخیره می‌شود.
 */
internal object BrowserViewRegistry {
    private var activeView: WeakReference<GeckoView>? = null

    fun register(view: GeckoView?) {
        activeView = view?.let { WeakReference(it) }
    }

    fun current(): GeckoView? = activeView?.get()
}

/**
 * از صفحهٔ فعلی عکس می‌گیرد و آن را به‌صورت PNG در پوشهٔ تصاویر اپ ذخیره می‌کند.
 *
 * باید از رشتهٔ اصلی (main thread) صدا زده شود؛ ذخیره‌سازی فایل به رشتهٔ IO منتقل می‌شود.
 *
 * @param onResult مسیر فایل ذخیره‌شده یا `null` اگر عکس‌برداری ممکن نبود.
 */
internal fun captureAndSaveScreenshot(context: Context, onResult: (String?) -> Unit) {
    val view = BrowserViewRegistry.current()
    if (view == null) {
        onResult(null)
        return
    }
    val appContext = context.applicationContext
    view.capturePixels().accept { bitmap ->
        if (bitmap == null) {
            onResult(null)
            return@accept
        }
        CoroutineScope(Dispatchers.IO).launch {
            val path = runCatching { writeScreenshot(appContext, bitmap) }.getOrNull()
            withContext(Dispatchers.Main) { onResult(path) }
        }
    }
}

private fun writeScreenshot(context: Context, bitmap: Bitmap): String? {
    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
        "screenshots",
    )
    if (!directory.exists()) directory.mkdirs()
    val file = File(directory, "marblebr_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    return file.absolutePath
}
