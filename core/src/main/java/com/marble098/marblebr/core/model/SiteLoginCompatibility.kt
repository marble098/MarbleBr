package com.marble098.marblebr.core.model

import java.net.URI

/**
 * «حالت سازگاری ورود» — چرا لازم است؟
 *
 * کاربر گزارش داد در سایت‌های بانکی (مثلِ `neon.enbank.ir`) نمی‌تواند وارد شود. علتِ رایجِ این
 * مشکل در مرورگرهای مبتنی بر Gecko سه چیز است:
 *
 * ۱. **مسدودسازیِ ردیاب‌ها:** صفحهٔ ورودِ بانک‌ها تقریباً همیشه اسکریپتِ ضدتقلب، کپچا یا ویجتِ
 *    کدِ پیامکی را از دامنهٔ سومی می‌گیرد. با روشن بودنِ «محافظت از ردیابی»، همان اسکریپت
 *    بی‌صدا بلاک می‌شود و دکمهٔ ورود هیچ کاری نمی‌کند.
 * ۲. **پنجرهٔ تازه (popup):** مرحلهٔ رمزِ دوم/پرداخت در بسیاری از بانک‌ها در پنجرهٔ تازه باز
 *    می‌شود؛ اگر آن پنجره بلاک شود، ورود نیمه‌کاره می‌ماند.
 * ۳. **بلاکرِ تبلیغ:** بعضی مسیرهای درخواست (مثل `/adserver/`) در دامنهٔ پرداخت هم بلاک می‌شدند.
 *
 * این فایل یک تصمیمِ خالص و قابلِ تست می‌سازد: «برای این دامنه، آیا محافظتِ ردیابی و بلاکر
 * کنار بروند و پنجرهٔ تازه آزاد شود؟» هیچ داده‌ای ذخیره یا ارسال نمی‌شود و کوکی‌ها دست‌نخورده
 * می‌مانند (حفاظتِ نشست همان‌طور کار می‌کند).
 */
data class LoginCompatibilityPlan(
    /** آیا محافظتِ ردیابی و بلاکرِ تبلیغ برای همین دامنه کنار برود؟ */
    val relaxedContentBlocking: Boolean = false,
    /** آیا پنجرهٔ تازهٔ همین دامنه بدونِ پرسش باز شود؟ (برای مرحلهٔ رمزِ دوم و پرداخت) */
    val allowPopups: Boolean = false,
    /** دلیلِ تصمیم؛ برای نمایش در منو و پیامِ کوتاه. */
    val reason: LoginCompatibilityReason = LoginCompatibilityReason.NONE,
) {
    val isRelaxed: Boolean get() = relaxedContentBlocking || allowPopups
}

/** چرا حالتِ سازگاری روشن است؟ */
enum class LoginCompatibilityReason {
    /** نه دامنهٔ حساس است و نه کاربر چیزی خواسته؛ رفتارِ عادی. */
    NONE,

    /** دامنه در فهرستِ بانک‌ها/درگاه‌های پرداختِ شناخته‌شده است. */
    KNOWN_FINANCIAL_HOST,

    /** خودِ کاربر از منوی همین سایت روشنش کرد (برای هر سایتی). */
    MANUAL,
}

object SiteLoginCompatibility {

    /**
     * دامنه‌های بانک‌ها و درگاه‌های پرداختِ ایرانی که ورودشان به اسکریپت/پنجرهٔ دامنهٔ سوم
     * وابسته است. زیردامنه‌ها هم به‌طور خودکار پوشش داده می‌شوند (`neon.enbank.ir` ← `enbank.ir`).
     */
    val financialHostSuffixes: Set<String> = setOf(
        // بانک‌ها
        "enbank.ir",
        "en-bank.ir",
        "bmi.ir",
        "bankmelli-iran.com",
        "bankmellat.ir",
        "bsi.ir",
        "banksepah.ir",
        "banktejarat.ir",
        "tejaratbank.ir",
        "refah-bank.ir",
        "bankrefah.ir",
        "bankparsian.ir",
        "parsian-bank.ir",
        "samanbank.ir",
        "sb24.ir",
        "bankpasargad.com",
        "bankpasargad.ir",
        "bankmaskan.ir",
        "bankkeshavarzi.ir",
        "shahr-bank.ir",
        "city-bank.ir",
        "bank-day.ir",
        "daybank.ir",
        "bankgardeshgari.ir",
        "bankkhavarmianeh.ir",
        "middleeastbank.ir",
        "karafarinbank.ir",
        "postbank.ir",
        "toseebank.ir",
        "tosee.ir",
        "blubank.com",
        "blubank.ir",
        // زیرساختِ پرداخت و درگاه‌ها
        "shaparak.ir",
        "shaparak.gov.ir",
        "cbi.ir",
        "sadad.ir",
        "sadad.co.ir",
        "asanpardakht.ir",
        "asanpardakht.net",
        "behpardakht.com",
        "irankish.com",
        "irankish.ir",
        "zarinpal.com",
        "idpay.ir",
        "pay.ir",
        "payping.ir",
        "zibal.ir",
        "nextpay.ir",
        "payir.ir",
        "mabnatejarat.com",
    )

    /**
     * نشانه‌های واژگانی؛ برای بانک‌های تازه یا دامنه‌هایی که کاربر خودش وارد کرده است.
     * عمداً کوتاه و محافظه‌کارانه است تا حالتِ سازگاری روی سایت‌های معمولی روشن نشود.
     */
    private val financialHostKeywords = listOf(
        "bank",
        "shaparak",
        "enbank",
        "irankish",
        "payping",
        "zarinpal",
        "asanpardakht",
        "behpardakht",
        "sadad",
        "zibal",
        "nextpay",
    )

    /** میزبانِ یک آدرس (بدونِ `www.`)، یا رشتهٔ خالی اگر آدرس نامعتبر باشد. */
    fun hostOf(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return runCatching {
            URI(normalized).host?.lowercase()?.removePrefix("www.").orEmpty()
        }.getOrDefault("")
    }

    /**
     * آیا این میزبان بانک/درگاهِ پرداخت است؟
     *
     * تطبیق هم روی خودِ دامنه و هم روی زیردامنه‌ها انجام می‌شود، ولی مرزِ نقطه رعایت می‌شود تا
     * `notabank.example` با `bank` اشتباه گرفته نشود.
     */
    fun isSensitiveHost(host: String): Boolean {
        val normalized = host.trim().lowercase().removePrefix("www.")
        if (normalized.isEmpty()) return false
        if (financialHostSuffixes.any { normalized == it || normalized.endsWith(".$it") }) return true
        val labels = normalized.split('.')
        return financialHostKeywords.any { keyword -> labels.any { it == keyword || it.startsWith(keyword) } }
    }

    /** آیا این آدرس به یک بانک/درگاه اشاره می‌کند؟ */
    fun isSensitiveUrl(url: String): Boolean = isSensitiveHost(hostOf(url))

    /**
     * نقشهٔ سازگاری برای یک آدرس.
     *
     * @param manual آیا کاربر خودش از منوی سایت درخواست کرده است؟
     * @param automatic آیا تشخیصِ خودکارِ بانکی مجاز است؟ (کاربر می‌تواند در تنظیمات خاموشش کند)
     */
    fun planFor(
        url: String,
        manual: Boolean = false,
        automatic: Boolean = true,
    ): LoginCompatibilityPlan = when {
        manual -> LoginCompatibilityPlan(
            relaxedContentBlocking = true,
            allowPopups = true,
            reason = LoginCompatibilityReason.MANUAL,
        )

        automatic && isSensitiveUrl(url) -> LoginCompatibilityPlan(
            relaxedContentBlocking = true,
            allowPopups = true,
            reason = LoginCompatibilityReason.KNOWN_FINANCIAL_HOST,
        )

        else -> LoginCompatibilityPlan()
    }
}
