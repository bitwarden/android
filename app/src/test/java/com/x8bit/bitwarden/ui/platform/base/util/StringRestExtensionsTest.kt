package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.util.assertLinkAnnotationIsAppliedAndInvokeClickAction
import org.junit.Assert.assertTrue
import org.junit.Test

class StringRestExtensionsTest : BaseComposeTest() {
    @Suppress("MaxLineLength")
    @Test
    fun `toAnnotatedString should add Clickable LinkAnnotation to highlighted string`() {
        var textClickCalled = false
        composeTestRule.setContent {
            val annotatedString =
                R.string.get_emails_from_bitwarden_for_announcements_advices_and_research_opportunities_unsubscribe_any_time.toAnnotatedString {
                    textClickCalled = true
                }
            Text(text = annotatedString)
        }
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = "Get emails from Bitwarden",
        )
        assertTrue(textClickCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAnnotatedString should add multiple Clickable LinkAnnotations to highlighted string`() {
        composeTestRule.setContent {
            val annotatedString =
                R.string.by_continuing_you_agree_to_the_terms_of_service_and_privacy_policy.toAnnotatedString()
            Text(text = annotatedString)
        }
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = "By continuing, you agree to the",
            expectedLinkCount = 2,
        )
    }

    @Test
    fun `no link annotations should be applied to non annotated string resource`() {
        composeTestRule.setContent {
            Text(text = R.string.about.toAnnotatedString())
        }

        composeTestRule
            .onNodeWithText("About")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.let { text ->
                text.forEach {
                    // get any link annotations present
                    val linkAnnotations = it.getLinkAnnotations(0, it.length)
                    assertTrue(linkAnnotations.isEmpty())
                }
            }
    }

    @Test
    fun `string with args should only use the arguments available in the string`() {
        composeTestRule.setContent {
            Text(
                text =
                R.string.on_your_computer_open_a_new_browser_tab_and_go_to_vault_bitwarden_com
                    .toAnnotatedString(
                        args = arrayOf("vault.bitwarden.com", "i should not exist"),
                    ),
            )
        }

        composeTestRule
            .onNodeWithText(
                "On your computer, open a new browser " +
                    "tab and go to vault.bitwarden.com",
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("i should not exist")
            .assertDoesNotExist()
    }
}
