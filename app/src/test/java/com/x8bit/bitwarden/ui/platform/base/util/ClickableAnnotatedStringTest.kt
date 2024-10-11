package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.LinkAnnotation
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ClickableAnnotatedStringTest : BaseComposeTest() {
    @Suppress("MaxLineLength")
    @Test
    fun `clickable annotated string should add Clickable LinkAnnotation to highlighted string`() {
        val mainString = "This is me testing the thing."
        val highLightText = "testing"
        composeTestRule.setContent {
            val annotatedString = createClickableAnnotatedString(
                mainString,
                listOf(
                    ClickableTextHighlight(
                        textToHighlight = highLightText,
                        onTextClick = {},
                    ),
                ),
            )
            Text(text = annotatedString)
        }
        val expectedStart = mainString.indexOf(highLightText)
        val expectedEnd = expectedStart + highLightText.length
        assertLinkAnnotationIsApplied(
            mainString,
            highLightText,
            expectedStart,
            expectedEnd,
        )
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
        assertLinkAnnotationIsApplied(
            mainString,
            highLightText1,
            expectedStart1,
            expectedEnd1,
        )
        assertLinkAnnotationIsApplied(
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
        assertLinkAnnotationIsApplied(
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
        assertLinkAnnotationIsApplied(
            mainString,
            highLightText,
            expectedStart,
            expectedEnd,
        )
    }

    private fun assertLinkAnnotationIsApplied(
        mainString: String,
        highLightText: String,
        expectedStart: Int,
        expectedEnd: Int,
    ) {
        composeTestRule
            .onNodeWithText(mainString)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.let { text ->
                text.forEach {
                    it.getLinkAnnotations(expectedStart, expectedEnd)
                        .forEach { annotationRange ->
                            val annotation = annotationRange.item as? LinkAnnotation.Clickable
                            assertNotNull(annotation != null)
                            assertEquals(highLightText, annotation!!.tag)
                        }
                }
            }
    }
}
