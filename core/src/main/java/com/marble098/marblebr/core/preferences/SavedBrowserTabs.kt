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
) {
    companion object {
        fun fromTab(tab: BrowserTab) = SavedBrowserTab(tab.id, tab.url, tab.containerId, tab.title, tab.isDesktopMode)
    }
}
