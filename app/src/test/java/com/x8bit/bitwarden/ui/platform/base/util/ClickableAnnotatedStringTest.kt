package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.util.assertLinkAnnotationIsAppliedAndInvokeClickAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickableAnnotatedStringTest : BaseComposeTest() {
    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add Clickable LinkAnnotation to highlighted string`() {
        var textClickCalled = false
        val mainString = "This is me testing the thing."
        val highLightText = "testing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString = mainString,
                highlights = listOf(
                    ClickableTextHighlight(
                        textToHighlight = highLightText,
                        onTextClick = { textClickCalled = true },
                    ),
                ),
            )
            Text(text = annotatedString)
        }
        val expectedStart = mainString.indexOf(highLightText)
        val expectedEnd = expectedStart + highLightText.length
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = mainString,
            highLightText = highLightText,
            expectedStart = expectedStart,
            expectedEnd = expectedEnd,
        )
        assertTrue(textClickCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add multiple Clickable LinkAnnotations to highlighted string`() {
        val mainString = "This is me testing the thing."
        val highLightText1 = "testing"
        val highlightText2 = "thing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString = mainString,
                highlights = listOf(
                    ClickableTextHighlight(
                        textToHighlight = highLightText1,
                        onTextClick = {},
                    ),
                    ClickableTextHighlight(
                        textToHighlight = highlightText2,
                        onTextClick = {},
                    ),
                ),
            )
            Text(text = annotatedString)
        }
        val expectedStart1 = mainString.indexOf(highLightText1)
        val expectedEnd1 = expectedStart1 + highLightText1.length
        val expectedStart2 = mainString.indexOf(highlightText2)
        val expectedEnd2 = expectedStart2 + highlightText2.length
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = mainString,
            highLightText = highLightText1,
            expectedStart = expectedStart1,
            expectedEnd = expectedEnd1,
        )
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = mainString,
            highLightText = highlightText2,
            expectedStart = expectedStart2,
            expectedEnd = expectedEnd2,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add annotation to first instance of highlighted string`() {
        val mainString = "Testing 1,2,3 testing"
        val highLightText = "testing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString = mainString,
                highlights = listOf(
                    ClickableTextHighlight(
                        textToHighlight = highLightText,
                        onTextClick = {},
                        instance = ClickableTextHighlight.Instance.FIRST,
                    ),
                ),
            )
            Text(text = annotatedString)
        }
        // indexOf returns the index of the first instance.
        val expectedStart = mainString.indexOf(highLightText, ignoreCase = true)
        val expectedEnd = expectedStart + highLightText.length
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = mainString,
            highLightText = highLightText,
            expectedStart = expectedStart,
            expectedEnd = expectedEnd,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add annotation to last instance of highlighted string`() {
        val mainString = "Testing 1,2,3 testing"
        val highLightText = "testing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString = mainString,
                highlights = listOf(
                    ClickableTextHighlight(
                        textToHighlight = highLightText,
                        onTextClick = {},
                        instance = ClickableTextHighlight.Instance.LAST,
                    ),
                ),
            )
            Text(text = annotatedString)
        }
        // indexOf returns the index of the first instance.
        val expectedStart = mainString.lastIndexOf(highLightText, ignoreCase = true)
        val expectedEnd = expectedStart + highLightText.length
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString = mainString,
            highLightText = highLightText,
            expectedStart = expectedStart,
            expectedEnd = expectedEnd,
        )
    }

    @Test
    fun `clickable link annotation is not applied to text highlight not present in main string`() {
        val mainString = "This is me testing the thing."
        val highLightText = "onomatopoeia"

        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString = mainString,
                highlights = listOf(
                    ClickableTextHighlight(
                        textToHighlight = highLightText,
                        onTextClick = {},
                    ),
                ),
            )
            Text(text = annotatedString)
        }

        composeTestRule
            .onNodeWithText(mainString)
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

    @Suppress("MaxLineLength")
    @Test
    fun `link annotations are added when format args string resource is provided as main string`() {
        composeTestRule.setContent {
            Text(
                text = createClickableAnnotatedString(
                    mainStringResource = R.string.by_continuing_you_agree_to_the_terms_of_service_and_privacy_policy,
                    highlights = listOf(
                        ClickableTextHighlight(
                            textToHighlight = "any old string",
                            onTextClick = {},
                        ),
                        ClickableTextHighlight(
                            textToHighlight = "any old string pt. 2",
                            onTextClick = {},
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "By continuing", substring = true, ignoreCase = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.let { text ->
                text.forEach {
                    // get any link annotations present
                    val linkAnnotations = it.getLinkAnnotations(0, it.length)
                    assertEquals(2, linkAnnotations.size)
                }
            }
    }
}
