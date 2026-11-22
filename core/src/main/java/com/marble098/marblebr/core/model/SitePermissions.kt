package com.marble098.marblebr.core.model

import kotlinx.serialization.Serializable

/**
 * دسترسی‌هایی که یک سایت می‌تواند از گوشی بخواهد.
 *
 * هر مورد سه حالت دارد ([PermissionState]) و کاربر در پنل «دسترسی‌های سایت» تصمیم می‌گیرد.
 * این مدل عمداً در ماژول `core` و بدون هیچ وابستگی‌ای به GeckoView یا Android است تا
 * منطقش با تست واحد بررسی شود ([SitePermissionPolicy]).
 */
@Serializable
enum class SitePermission {
    JAVASCRIPT,
    AUTOPLAY,
    GEOLOCATION,
    CAMERA,
    MICROPHONE,
    NOTIFICATIONS,
    /** باز کردنِ پنجرهٔ تازه (popup) از سوی صفحه. */
    POPUPS,
}

/** وضعیتِ یک دسترسی برای یک سایت. */
@Serializable
enum class PermissionState {
    ALLOW,
    BLOCK,
    ASK,
}

/**
 * مجموعهٔ دسترسی‌های یک سایت و تصمیم نهایی دربارهٔ هرکدام.
 *
 * @param domain دامنهٔ سایت (بدون پروتکل) که این تصمیم‌ها به آن تعلق دارد.
 * @param states نقشهٔ دسترسی → وضعیت. هر دسترسیِ غایب، از [SitePermissionPolicy.defaults] می‌آید.
 */
@Serializable
data class SitePermissionProfile(
    val domain: String,
    val states: Map<SitePermission, PermissionState> = emptyMap(),
) {
    /** وضعیتِ مؤثر یک دسترسی (پیش‌فرض در صورت نبودِ تصمیمِ کاربر). */
    fun stateOf(permission: SitePermission): PermissionState =
        states[permission] ?: SitePermissionPolicy.defaults[permission] ?: PermissionState.ASK

    fun with(permission: SitePermission, state: PermissionState): SitePermissionProfile =
        copy(states = states + (permission to state))

    /** آیا اسکریپت‌های صفحه اجازهٔ اجرا دارند؟ (برای تصمیم‌گیری در موتور Gecko) */
    fun javascriptAllowed(): Boolean = stateOf(SitePermission.JAVASCRIPT) != PermissionState.BLOCK

    /** آیا پخش خودکارِ ویدیو مجاز است؟ */
    fun autoplayAllowed(): Boolean = stateOf(SitePermission.AUTOPLAY) == PermissionState.ALLOW

    /** آیا این پروفایل تصمیمِ سفارشیِ کاربر دارد؟ */
    fun isCustomized(): Boolean = states.isNotEmpty()
}

/**
 * سیاستِ دسترسی‌های سایت — منطقِ خالص و قابل‌تست.
 *
 * چرا این قابلیت لازم بود؟ مرورگرهای بزرگ برای هر سایت یک صفحهٔ «دسترسی‌ها» دارند تا کاربر
 * بتواند دوربین/میکروفون/موقعیت/اعلان را برای همان سایت ببندد. MarbleBr این را نداشت و
 * تنها راهِ بستنِ یک دسترسی، عوض کردنِ تنظیمات کل مرورگر بود.
 */
object SitePermissionPolicy {

    /**
     * پیش‌فرض‌های امن و منطقی:
     * - جاوااسکریپت، موقعیت، دوربین و میکروفون: «بپرس» (هیچ‌وقت بی‌اجازه فعال نمی‌شوند).
     * - پخش خودکار ویدیو: «بلاک» (داده و باتری کم می‌شود، کاربر راحت‌تر است).
     * - اعلان‌ها: «بپرس».
     */
    val defaults: Map<SitePermission, PermissionState> = mapOf(
        SitePermission.JAVASCRIPT to PermissionState.ALLOW,
        SitePermission.AUTOPLAY to PermissionState.BLOCK,
        SitePermission.GEOLOCATION to PermissionState.ASK,
        SitePermission.CAMERA to PermissionState.ASK,
        SitePermission.MICROPHONE to PermissionState.ASK,
        SitePermission.NOTIFICATIONS to PermissionState.ASK,
        SitePermission.POPUPS to PermissionState.ASK,
    )

    /** انتخابِ بعدی هنگام لمسِ یک دسترسی (چرخهٔ سه‌حالته: اجازه → بپرس → بلاک). */
    fun nextState(current: PermissionState): PermissionState = when (current) {
        PermissionState.ALLOW -> PermissionState.ASK
        PermissionState.ASK -> PermissionState.BLOCK
        PermissionState.BLOCK -> PermissionState.ALLOW
    }

    /** دسترسی‌هایی که در پنل نمایش داده می‌شوند، به ترتیبِ اهمیت. */
    val ordered: List<SitePermission> = listOf(
        SitePermission.JAVASCRIPT,
        SitePermission.AUTOPLAY,
        SitePermission.GEOLOCATION,
        SitePermission.CAMERA,
        SitePermission.MICROPHONE,
        SitePermission.NOTIFICATIONS,
        SitePermission.POPUPS,
    )

    /** یک‌خطِ خلاصه برای نشان دادن در کارت سایت (مثلاً «۲ دسترسی بسته»). */
    fun blockedCount(profile: SitePermissionProfile): Int =
        ordered.count { profile.stateOf(it) == PermissionState.BLOCK }
}
