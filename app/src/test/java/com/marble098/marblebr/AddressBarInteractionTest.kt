package com.marble098.marblebr

import android.app.Application
import androidx.compose.ui.geometry.Offset
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercise real Compose gestures, not only the pure selection helper. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AddressBarInteractionTest {
    @get:Rule
    val compose = createComposeRule()
    private val url = "https://my.mci.ir/panel"

    private fun render(mode: AddressSelectionMode) {
        compose.setContent {
            MarbleBrTheme {
                BrowserAddressField(
                    url = url,
                    secure = true,
                    isBookmarked = false,
                    canBookmark = false,
                    mode = mode,
                    onSubmit = {},
                    onBookmark = {},
                )
            }
        }
    }

    @Test
    fun directTouchSelectsWholeAddressAndTypingReplacesIt() {
        render(AddressSelectionMode.SELECT_ALL)
        val field = compose.onNodeWithTag("smart_address_bar")
        field.performTouchInput { click(center) }
        compose.waitForIdle()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, url.length)))
        field.performTextInput("replacement")
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("replacement")))
    }

    @Test
    fun secondTapCanMoveTheCaretEvenBeforeTheFocusDelayEnds() {
        render(AddressSelectionMode.SELECT_ALL)
        compose.mainClock.autoAdvance = false
        val field = compose.onNodeWithTag("smart_address_bar")
        field.performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
        field.performTouchInput { click(Offset(2f, center.y)) }
        compose.mainClock.advanceTimeBy(150)
        compose.waitForIdle()
        val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        assertTrue("Second tap should leave a caret, not select the whole address", selection.collapsed)
    }

    @Test
    fun tappingFrameAlsoSelectsTheAddress() {
        render(AddressSelectionMode.SELECT_ALL)
        compose.onNodeWithTag("smart_address_frame").performTouchInput { click(Offset(2f, center.y)) }
        compose.waitForIdle()
        compose.onNodeWithTag("smart_address_bar").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, url.length)),
        )
    }

    @Test
    fun cursorModeDoesNotSelectEverything() {
        render(AddressSelectionMode.CURSOR)
        val field = compose.onNodeWithTag("smart_address_bar")
        field.performTouchInput { click(center) }
        compose.waitForIdle()
        assertTrue(field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange].collapsed)
    }

    @Test
    fun partModeSelectsOnlyOneAddressComponent() {
        render(AddressSelectionMode.SELECT_PART)
        val field = compose.onNodeWithTag("smart_address_bar")
        field.performTouchInput { click(center) }
        compose.waitForIdle()
        val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        assertTrue(selection.length in 1 until url.length)
    }
}
