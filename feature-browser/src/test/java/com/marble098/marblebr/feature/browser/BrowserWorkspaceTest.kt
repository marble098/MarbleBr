package com.marble098.marblebr.feature.browser

import androidx.compose.ui.text.TextRange
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.preferences.AddressSelectionMode
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.SavedBrowserTabs
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserWorkspaceTest {
    @Test
    fun restoreRetainsRegularTabsAndDesktopMode() {
        val blank = BrowserTab("blank", "", "", "")
        val web = BrowserTab("web", "example", "example", "https://example.org", isDesktopMode = true)
        val snapshot = SavedBrowserTabs.fromTabs(listOf(blank, web), web.id, showHome = false)
        val restored = Json.decodeFromString<SavedBrowserTabs>(Json.encodeToString(snapshot))
        assertEquals(listOf("blank", "web"), restored.tabs.map { it.id })
        assertEquals("web", restored.activeTabId)
        assertTrue(restored.tabs.last().isDesktopMode)
        assertFalse(restored.showHome)
    }

    @Test
    fun privateTabIsNotSerializedAndFallsBackToHome() {
        val regular = BrowserTab("regular", "", "", "")
        val privateTab = BrowserTab("private", "", "", "https://private.example", isPrivate = true)
        val snapshot = SavedBrowserTabs.fromTabs(listOf(regular, privateTab), privateTab.id, showHome = false)
        assertEquals("regular", snapshot.activeTabId)
        assertTrue(snapshot.showHome)
        assertFalse(Json.encodeToString(snapshot).contains("private"))
        assertTrue(SavedBrowserTabs.fromTabs(emptyList(), "", showHome = true).tabs.isEmpty())
    }

    @Test
    fun newTabsDoNotContactGoogleUnlessEnabled() {
        assertFalse(BrowserPreferences().openGoogleOnNewTab)
    }

    @Test
    fun addressSelectionRespectsModeAndTapLocation() {
        val url = "https://example.org/a/path?key=value"
        val caret = url.indexOf("path") + 1
        assertEquals(TextRange(0, url.length), addressSelection(url, caret, AddressSelectionMode.SELECT_ALL))
        assertEquals(TextRange(caret - 1, caret + 3), addressSelection(url, caret, AddressSelectionMode.SELECT_PART))
        assertEquals(TextRange(caret), addressSelection(url, caret, AddressSelectionMode.CURSOR))
        assertEquals(TextRange.Zero, addressSelection("", 0, AddressSelectionMode.SELECT_PART))
    }
}
