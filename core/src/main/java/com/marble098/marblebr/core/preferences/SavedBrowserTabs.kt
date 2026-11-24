package com.marble098.marblebr.core.preferences

import com.marble098.marblebr.core.model.BrowserTab
import kotlinx.serialization.Serializable

@Serializable
data class SavedBrowserTabs(
    val tabs: List<SavedBrowserTab>,
    val activeTabId: String,
    val showHome: Boolean,
) {
    companion object {
        fun fromTabs(tabs: List<BrowserTab>, activeTabId: String, showHome: Boolean): SavedBrowserTabs {
            val regularTabs = tabs.filterNot { it.isPrivate }.map(SavedBrowserTab::fromTab)
            val active = regularTabs.firstOrNull { it.id == activeTabId }
            return SavedBrowserTabs(regularTabs, active?.id ?: regularTabs.lastOrNull()?.id.orEmpty(), showHome || active == null)
        }
    }
}

@Serializable
data class SavedBrowserTab(
    val id: String,
    val url: String,
    val containerId: String,
    val title: String,
    val isDesktopMode: Boolean,
    val pageZoomPercent: Int = 100,
) {
    companion object {
        fun fromTab(tab: BrowserTab) = SavedBrowserTab(
            id = tab.id,
            url = tab.url,
            containerId = tab.containerId,
            title = tab.title,
            isDesktopMode = tab.isDesktopMode,
            pageZoomPercent = tab.pageZoomPercent,
        )
    }
}
