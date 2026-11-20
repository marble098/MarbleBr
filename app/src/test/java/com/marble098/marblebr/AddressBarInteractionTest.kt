package com.marble098.marblebr

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.marble098.marblebr.core.designsystem.MarbleBrTheme
import com.marble098.marblebr.core.preferences.AddressSelectionMode
import com.marble098.marblebr.feature.browser.BrowserAddressField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercise a touch on the actual text field, not only the pure selection helper. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AddressBarInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun directTouchSelectsWholeAddressAndTypingReplacesIt() {
        val url = "https://my.mci.ir/panel"
        compose.setContent {
            MarbleBrTheme {
                BrowserAddressField(
                    url = url,
                    secure = true,
                    isBookmarked = false,
                    canBookmark = false,
                    mode = AddressSelectionMode.SELECT_ALL,
                    onSubmit = {},
                    onBookmark = {},
                )
            }
        }
        val field = compose.onNodeWithTag("smart_address_bar")
        field.performTouchInput { click(center) }
        compose.waitForIdle()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, url.length)))
        field.performTextInput("replacement")
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("replacement")))
    }
}
