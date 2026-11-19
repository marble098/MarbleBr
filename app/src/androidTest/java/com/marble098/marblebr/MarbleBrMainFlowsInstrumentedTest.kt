package com.marble098.marblebr

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marble098.marblebr.core.designsystem.MarbleBrTheme
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.WebAppContainer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarbleBrMainFlowsInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun marbleBrThemeRendersRtlPersianContainerCards() {
        val sampleContainer = WebAppContainer(
            id = "github_com",
            domain = "github.com",
            displayName = "github.com",
            contextId = "marble_ctx_github_com",
            neverSignOutEnabled = true,
            cookieExpirationDays = 365,
        )
        val sampleSnapshot = SessionSnapshotMeta(
            id = "snap_1",
            containerId = "github_com",
            domain = "github.com",
            capturedAtEpochMillis = 1_763_500_000_000L,
            cookieCount = 4,
            storageKeyCount = 6,
            payloadSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            isHealthy = true,
            triggerReason = "AUTO",
            label = "اسنپ‌شات اولیه",
        )

        composeTestRule.setContent {
            MarbleBrTheme(forceRtl = true) {
                Text("کانتینر: ${sampleContainer.contextId} - ${sampleSnapshot.label}")
            }
        }

        composeTestRule.onNodeWithText("کانتینر: marble_ctx_github_com - اسنپ‌شات اولیه").assertIsDisplayed()
    }
}
