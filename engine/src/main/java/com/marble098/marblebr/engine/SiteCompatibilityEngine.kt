package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.LoginObservationProbe
import com.marble098.marblebr.core.model.LoginPageObservation
import com.marble098.marblebr.core.model.PasswordStrength
import com.marble098.marblebr.core.model.SiteAdditionPlan
import com.marble098.marblebr.core.model.SiteAdditionPlanner
import com.marble098.marblebr.core.model.SiteBlocker
import com.marble098.marblebr.core.model.SiteRoute
import com.marble098.marblebr.core.model.CredentialCaptureHeuristics
import com.marble098.marblebr.core.model.PasswordQualityChecker
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** وضعیتِ «افزودن هر سایتی» برای یک تب. */
data class TabSiteState(
    val url: String = "",
    val observation: LoginPageObservation = LoginPageObservation.EMPTY,
    val failedRoutes: Set<SiteRoute> = emptySet(),
    val attemptedRoutes: List<SiteRoute> = emptyList(),
)

/**
 * پلِ «افزودن هر سایتی» بین مرورگر و موتور.
 *
 * چرا یک کلاس جدا؟ چون [ContainerSessionManager] باید فقط ناوبری و نشست را بداند. این کلاس
 * تصمیم‌های «چه چیزی در صفحه است» و «کدام راهِ جایگزین بمانَد» را نگه می‌دارد. تصمیم‌های
 * «ورود/ذخیره/کد» جای دیگری هستند ([com.marble098.marblebr.vault.PasswordStudio] و
 * [LoginWatcher]) تا هر کلاس یک مسئولیت داشته باشد.
 */
@Singleton
class SiteCompatibilityEngine @Inject constructor() {
    private val tabStates = ConcurrentHashMap<String, TabSiteState>()

    fun stateOf(tabId: String): TabSiteState = tabStates[tabId] ?: TabSiteState()

    fun onPageLoaded(tabId: String, url: String, observationRaw: String?) {
        val observation = LoginObservationProbe.parse(observationRaw)
        val previous = tabStates[tabId]
        tabStates[tabId] = (previous ?: TabSiteState()).copy(url = url, observation = observation)
    }

    /** نقشهٔ راهِ کامل برای آدرسی که کاربر می‌خواهد اضافه کند. */
    fun planFor(tabId: String, url: String, preferDesktop: Boolean = false): SiteAdditionPlan {
        val state = tabStates[tabId]
        val observation = state?.takeIf { it.url == url }?.observation ?: LoginPageObservation.EMPTY
        return SiteAdditionPlanner.plan(
            url = url,
            observation = observation,
            failedRoutes = state?.failedRoutes.orEmpty(),
            preferDesktop = preferDesktop,
        )
    }

    /** مسیر بعدیِ نردبان؛ `null` یعنی همهٔ راه‌ها امتحان شده‌اند. */
    fun nextFallback(tabId: String, url: String): SiteRoute? =
        planFor(tabId, url).steps.firstOrNull()?.route

    fun markRouteFailed(tabId: String, route: SiteRoute) {
        val state = tabStates[tabId] ?: TabSiteState()
        tabStates[tabId] = state.copy(
            failedRoutes = state.failedRoutes + route,
            attemptedRoutes = state.attemptedRoutes + route,
        )
    }

    fun resetRoutes(tabId: String) {
        val state = tabStates[tabId] ?: return
        tabStates[tabId] = state.copy(failedRoutes = emptySet(), attemptedRoutes = emptyList())
    }

    /** آیا این صفحه یکی از دلایلِ «نشد» را نشان می‌دهد؟ */
    fun blockersOf(tabId: String, url: String): List<SiteBlocker> =
        planFor(tabId, url).assessment.blockers.filterNot { it == SiteBlocker.NONE }

    fun forgetTab(tabId: String) {
        tabStates.remove(tabId)
    }

    /** کیفیتِ گذرواژه برای فرمِ ورودِ دستی در مخزن. */
    fun evaluatePassword(password: String, username: String, site: String): PasswordStrength =
        PasswordQualityChecker.evaluate(password, username, CredentialCaptureHeuristics.domainOf(site))
}
