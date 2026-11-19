package com.marble098.marblebr.core.i18n

import java.util.Locale

/**
 * زبان رابط کاربری MarbleBr.
 *
 * کاربر می‌تواند از تنظیمات بین «فارسی»، «انگلیسی» و «خودکار (هم‌زبان گوشی)» انتخاب کند.
 */
enum class AppLanguage {
    SYSTEM,
    PERSIAN,
    ENGLISH;

    /**
     * زبان واقعیِ قابل استفاده پس از اعمال حالت «خودکار».
     */
    fun resolve(systemLanguageTag: String = Locale.getDefault().toLanguageTag()): AppLanguage =
        when (this) {
            SYSTEM -> if (systemLanguageTag.lowercase(Locale.ROOT).startsWith("fa")) PERSIAN else ENGLISH
            else -> this
        }

    /**
     * آیا زبان انتخاب‌شده راست‌به‌چپ است؟
     */
    fun isRtl(systemLanguageTag: String = Locale.getDefault().toLanguageTag()): Boolean =
        resolve(systemLanguageTag) == PERSIAN
}
