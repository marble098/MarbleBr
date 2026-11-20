package com.marble098.marblebr.core.model

/**
 * نتیجهٔ تصمیم‌گیری برای دکمهٔ بازگشت گوشی.
 */
enum class BackTarget {
    /** رفتن به صفحهٔ قبلیِ همان سایت داخل مرورگر. */
    PAGE_BACK,

    /** رفتن به صفحهٔ خانهٔ MarbleBr. */
    HOME,

    /** بستن برنامه (فقط وقتی استفاده می‌شود که تصمیم‌گیری با کاربر باشد). */
    EXIT,

    /** پرسیدن از کاربر که کدام کار انجام شود. */
    ASK,
}

/**
 * منطق خالص و قابل‌تست دکمهٔ بازگشت.
 *
 * این کلاس هیچ وابستگی‌ای به اندروید یا Compose ندارد تا بتوان رفتار همهٔ حالت‌ها را
 * با تست واحد بررسی کرد.
 */
object BackNavigationResolver {

    /**
     * @param behavior حالت انتخاب‌شده در تنظیمات.
     * @param isOnHome آیا کاربر در صفحهٔ خانه است؟
     * @param canGoBack آیا صفحهٔ فعلی می‌تواند به صفحهٔ قبلی برگردد؟
     * @param tabCount تعداد تب‌های باز.
     * @param canExitApp آیا بستن برنامه در این مرحله مجاز است؟ (مثلاً وقتی صفحهٔ خانه فعال است)
     */
    fun resolve(
        behavior: BackButtonBehavior,
        isOnHome: Boolean,
        canGoBack: Boolean,
        tabCount: Int,
        canExitApp: Boolean,
    ): BackTarget = when (behavior) {
        BackButtonBehavior.SMART -> when {
            !isOnHome && canGoBack -> BackTarget.PAGE_BACK
            !isOnHome -> BackTarget.HOME
            else -> BackTarget.EXIT
        }
        BackButtonBehavior.PAGE_THEN_HOME -> when {
            !isOnHome && canGoBack -> BackTarget.PAGE_BACK
            !isOnHome -> BackTarget.HOME
            else -> BackTarget.EXIT
        }
        BackButtonBehavior.HOME -> if (isOnHome) BackTarget.EXIT else BackTarget.HOME
        BackButtonBehavior.PAGE_THEN_EXIT -> when {
            !isOnHome && canGoBack -> BackTarget.PAGE_BACK
            isOnHome && canExitApp -> BackTarget.EXIT
            else -> BackTarget.EXIT
        }
        BackButtonBehavior.ASK -> when {
            // در صفحهٔ خانه با چند تبِ باز، پرسیدن گیج‌کننده است؛ مستقیم خارج می‌شویم.
            isOnHome && tabCount <= 1 -> BackTarget.EXIT
            isOnHome -> BackTarget.ASK
            else -> BackTarget.ASK
        }
    }

    /** گزینه‌هایی که باید در دیالوگ «برگرد به کجا؟» نمایش داده شوند. */
    fun askOptions(isOnHome: Boolean, canGoBack: Boolean): List<BackTarget> = buildList {
        if (!isOnHome && canGoBack) add(BackTarget.PAGE_BACK)
        if (!isOnHome) add(BackTarget.HOME)
        add(BackTarget.EXIT)
    }
}
