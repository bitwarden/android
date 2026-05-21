package com.bitwarden.ui.platform.base.util

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.bitwarden.ui.R
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.util.assertLinkAnnotationIsAppliedAndInvokeClickAction
import org.junit.Assert.assertTrue
import org.junit.Test

class StringResExtensionsTest : BaseComposeTest() {
    @Test
    fun `toAnnotatedString should add Clickable LinkAnnotation to highlighted string`() {
        var textClickCalled = false
        setTestContent {
            val annotatedString = R.string.test_for_single_link_annotation.toAnnotatedString {
                textClickCalled = true
            }
            Text(text = annotatedString)
        }
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = "Get emails from Bitwarden",
        )
        assertTrue(textClickCalled)
    }

    @Test
    fun `toAnnotatedString should add multiple Clickable LinkAnnotations to highlighted string`() {
        setTestContent {
            val annotatedString = R.string.test_for_multi_link_annotation.toAnnotatedString()
            Text(text = annotatedString)
        }
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = "By continuing, you agree to the",
            expectedLinkCount = 2,
        )
    }

    @Test
    fun `no link annotations should be applied to non annotated string resource`() {
        setTestContent {
            Text(text = R.string.test_for_string_with_no_annotations.toAnnotatedString())
        }

        composeTestRule
            .onNodeWithText("Nothing special here.")
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
        setTestContent {
            Text(
                text =
                    R.string.test_for_string_with_annotation_and_arg_annotation
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

    @Test
    fun `string with arg annotations but no passed in args should just append empty string`() {
        setTestContent {
            Text(
                text = R.string.test_for_string_with_annotation_and_arg_annotation
                    .toAnnotatedString(),
            )
        }

        composeTestRule
            .onNodeWithText("On your computer, open a new browser tab and go to ")
            .assertIsDisplayed()
    }

    @Test
    fun `string with no annotations with args should just be handled as normal annotated string`() {
        setTestContent {
            Text(
                text = R.string.test_for_string_with_no_annotations_with_format_arg
                    .toAnnotatedString(args = arrayOf("this")),
            )
        }

        composeTestRule
            .onNodeWithText("Nothing special here, except this.")
            .assertIsDisplayed()
    }

    @Test
    fun `toAnnotatedPluralsString resolves singular form and applies arg annotations`() {
        setTestContent {
            Text(
                text = R.plurals.test_for_plurals_with_annotation_and_arg_annotation
                    .toAnnotatedPluralsString(
                        quantity = 1,
                        args = arrayOf("1", "April 21, 2026"),
                    ),
            )
        }

        composeTestRule
            .onNodeWithText("You have 1 day to resolve by April 21, 2026.")
            .assertIsDisplayed()
    }

    @Test
    fun `toAnnotatedPluralsString resolves plural form and applies arg annotations`() {
        setTestContent {
            Text(
                text = R.plurals.test_for_plurals_with_annotation_and_arg_annotation
                    .toAnnotatedPluralsString(
                        quantity = 7,
                        args = arrayOf("7", "April 21, 2026"),
                    ),
            )
        }

        composeTestRule
            .onNodeWithText("You have 7 days to resolve by April 21, 2026.")
            .assertIsDisplayed()
    }

    @Test
    fun `toAnnotatedPluralsString without annotations falls back to formatted quantity string`() {
        setTestContent {
            Text(
                text = R.plurals.test_for_plurals_with_no_annotations
                    .toAnnotatedPluralsString(
                        quantity = 3,
                        args = arrayOf("3"),
                    ),
            )
        }

        composeTestRule
            .onNodeWithText("3 days remaining.")
            .assertIsDisplayed()
    }
}
