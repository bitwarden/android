package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.Text
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.util.assertLinkAnnotationIsAppliedAndInvokeClickAction
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
                mainString,
                listOf(
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
            mainString,
            highLightText,
            expectedStart,
            expectedEnd,
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
                mainString,
                listOf(
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
            mainString,
            highLightText1,
            expectedStart1,
            expectedEnd1,
        )
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString,
            highlightText2,
            expectedStart2,
            expectedEnd2,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add annotation to first instance of highlighted string`() {
        val mainString = "Testing 1,2,3 testing"
        val highLightText = "testing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString,
                listOf(
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
        val expectedStart = mainString.indexOf(highLightText)
        val expectedEnd = expectedStart + highLightText.length
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString,
            highLightText,
            expectedStart,
            expectedEnd,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add annotation to last instance of highlighted string`() {
        val mainString = "Testing 1,2,3 testing"
        val highLightText = "testing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString,
                listOf(
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
        val expectedStart = mainString.lastIndexOf(highLightText)
        val expectedEnd = expectedStart + highLightText.length
        composeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
            mainString,
            highLightText,
            expectedStart,
            expectedEnd,
        )
    }
}
