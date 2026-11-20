package com.marble098.marblebr.feature.vault

import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.WebAppContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های منطقِ داشبورد «مراقبت از حساب‌ها».
 *
 * این تست‌ها بدون اندروید اجرا می‌شوند و همان قواعدی را می‌سنجند که ViewModel برای
 * فیلتر/جستجوی سایت‌ها و متن‌های دوزبانه به کار می‌برد؛ اگر روزی این قواعد تغییر کنند،
 * رفتارِ کاربری (فیلترهای چیپی و جستجو) بی‌صدا خراب نمی‌شود.
 */
class VaultSiteFilterTest {

    private val sites = listOf(
        WebAppContainer(id = "github", domain = "github.com", displayName = "GitHub", neverSignOutEnabled = true),
        WebAppContainer(id = "mail", domain = "mail.google.com", displayName = "Gmail", neverSignOutEnabled = false),
        WebAppContainer(
            id = "chat",
            domain = "web.telegram.org",
            displayName = "Telegram",
            neverSignOutEnabled = true,
            isPinnedPwa = true,
        ),
    )

    private fun applyFilter(containers: List<WebAppContainer>, filter: VaultSiteFilter): List<WebAppContainer> =
        containers.filter { container ->
            when (filter) {
                VaultSiteFilter.ALL -> true
                VaultSiteFilter.PROTECTED -> container.neverSignOutEnabled
                VaultSiteFilter.UNPROTECTED -> !container.neverSignOutEnabled
                VaultSiteFilter.PINNED -> container.isPinnedPwa
            }
        }

    private fun search(containers: List<WebAppContainer>, query: String): List<WebAppContainer> {
        val needle = query.trim()
        return containers.filter { container ->
            needle.isEmpty() ||
                container.domain.contains(needle, ignoreCase = true) ||
                container.displayName.contains(needle, ignoreCase = true)
        }
    }

    @Test
    fun `all filter keeps every site`() {
        assertEquals(3, applyFilter(sites, VaultSiteFilter.ALL).size)
    }

    @Test
    fun `protected filter returns only guarded sites`() {
        val protectedSites = applyFilter(sites, VaultSiteFilter.PROTECTED)
        assertEquals(listOf("github", "chat"), protectedSites.map { it.id })
    }

    @Test
    fun `unprotected filter returns only unguarded sites`() {
        val unprotected = applyFilter(sites, VaultSiteFilter.UNPROTECTED)
        assertEquals(listOf("mail"), unprotected.map { it.id })
    }

    @Test
    fun `pinned filter returns pinned shortcuts only`() {
        assertEquals(listOf("chat"), applyFilter(sites, VaultSiteFilter.PINNED).map { it.id })
    }

    @Test
    fun `search matches domain and display name ignoring case`() {
        assertEquals(listOf("github"), search(sites, "GITH").map { it.id })
        assertEquals(listOf("mail"), search(sites, "gmail").map { it.id })
        assertTrue(search(sites, "   ").size == 3)
        assertTrue(search(sites, "does-not-exist").isEmpty())
    }

    @Test
    fun `filter labels exist in both languages`() {
        val persian = MarbleStrings.forResolvedLanguage(AppLanguage.PERSIAN)
        val english = MarbleStrings.forResolvedLanguage(AppLanguage.ENGLISH)
        VaultSiteFilter.entries.forEach { filter ->
            assertTrue(filterLabel(persian, filter).isNotBlank())
            assertTrue(filterLabel(english, filter).isNotBlank())
        }
        assertFalse(persian.vaultFilterAll == english.vaultFilterAll)
    }

    @Test
    fun `cookie and keep alive bounds match the sliders`() {
        assertEquals(7, VaultDashboardViewModel.MIN_COOKIE_DAYS)
        assertEquals(730, VaultDashboardViewModel.MAX_COOKIE_DAYS)
        assertEquals(15, VaultDashboardViewModel.MIN_KEEP_ALIVE_MINUTES)
        assertTrue(VaultDashboardViewModel.MAX_KEEP_ALIVE_MINUTES >= VaultDashboardViewModel.MIN_KEEP_ALIVE_MINUTES)
    }

    private fun filterLabel(strings: MarbleStrings, filter: VaultSiteFilter): String = when (filter) {
        VaultSiteFilter.ALL -> strings.vaultFilterAll
        VaultSiteFilter.PROTECTED -> strings.vaultFilterProtected
        VaultSiteFilter.UNPROTECTED -> strings.vaultFilterUnprotected
        VaultSiteFilter.PINNED -> strings.vaultFilterPinned
    }
}
