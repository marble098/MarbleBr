package com.marble098.marblebr.feature.browser

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * تنها نقطهٔ اتصالِ یک [GeckoSession] به یک [GeckoView].
 *
 * ریشهٔ باگ «صفحهٔ لود شده نشان داده نمی‌شود»:
 * GeckoView فعال‌سازی نشست (`setActive`) را فقط در لحظهٔ ساخت یا تغییر Surface انجام می‌دهد و
 * `releaseSession` نشست را از نما جدا (و غیرفعال) می‌کند. قبلاً اتصال نشست به نما در چند مسیر جدا
 * (factory، onRelease و تغییر کلید Compose) انجام می‌شد و هیچ‌کدام وضعیت فعال‌بودن را تضمین نمی‌کرد؛
 * در نتیجه بعد از Enter ممکن بود نشستِ درست فعال نشود و صفحه تا جابه‌جایی تب یا بازگشت به برنامه سیاه بماند.
 *
 * قرارداد این کلاس:
 * ۱. هر نشست در هر لحظه فقط به یک نما وصل است.
 * ۲. نشستِ نمایش‌داده‌شده صریحاً فعال می‌شود؛ نشستِ جداشده غیرفعال می‌شود اما هرگز بسته نمی‌شود
 *    (بستن نشست فقط با [ContainerSessionManager] انجام می‌شود).
 * ۳. [bind] روی نمایی که از قبل همان نشست را دارد هیچ کاری جز ثبت نما انجام نمی‌دهد (ایمن برای بازترکیب).
 */
internal object GeckoSurfaceBinding {

    fun bind(view: GeckoView, session: GeckoSession) {
        BrowserViewRegistry.register(view)
        if (view.session === session) return
        detachCurrent(view)
        view.setSession(session)
        session.setActive(true)
    }

    fun unbind(view: GeckoView) {
        BrowserViewRegistry.unregister(view)
        detachCurrent(view)
    }

    private fun detachCurrent(view: GeckoView) {
        // releaseSession نشست را نمی‌بندد؛ فقط اتصال نما را قطع می‌کند و نشستِ قبلی را برمی‌گرداند.
        val previous = view.releaseSession() ?: return
        if (previous.isOpen) previous.setActive(false)
    }
}
