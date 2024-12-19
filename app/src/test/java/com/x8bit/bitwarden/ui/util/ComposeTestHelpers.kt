package com.x8bit.bitwarden.ui.util

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToString
import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.assertThrows

/**
 * A [SemanticsMatcher] used to find editable text nodes.
 */
val isEditableText: SemanticsMatcher
    get() = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

/**
 * A [SemanticsMatcher] used to find progressbar nodes.
 */
val isProgressBar: SemanticsMatcher
    get() = SemanticsMatcher("ProgressBar") {
        it.config
            .getOrNull(SemanticsProperties.ProgressBarRangeInfo)
            ?.let { true }
            ?: false
    }

/**
 * Asserts that no dialog currently exists.
 */
fun ComposeContentTestRule.assertNoDialogExists() {
    this
        .onNode(isDialog())
        .assertDoesNotExist()
}

/**
 * Asserts that the master password reprompt dialog is displayed.
 */
fun ComposeContentTestRule.assertMasterPasswordDialogDisplayed() {
    this
        .onAllNodesWithText(text = "Master password confirmation")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(
            text = "This action is protected, to continue please re-enter your master " +
                "password to verify your identity.",
        )
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(text = "Master password")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(text = "Cancel")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
    this
        .onAllNodesWithText(text = "Submit")
        .filterToOne(hasAnyAncestor(isDialog()))
        .assertIsDisplayed()
}

/**
 * A helper that asserts that the node does not exist in the scrollable list.
 */
fun ComposeContentTestRule.assertScrollableNodeDoesNotExist(
    text: String,
    substring: Boolean = false,
) {
    val scrollableNodeInteraction = onNode(hasScrollToNodeAction())
    assertThrows<AssertionError> {
        // throws since it cannot find the node.
        scrollableNodeInteraction.performScrollToNode(hasText(text, substring))
    }
}

/**
 * A helper used to scroll to and get the matching node in a scrollable list. This is intended to
 * be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onNodeWithTextAfterScroll(
    text: String,
    substring: Boolean = false,
): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text, substring))
    return onNodeWithText(text, substring)
}

/**
 * A helper used to scroll to and get the matching node in a scrollable list. This is intended to
 * be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onNodeWithContentDescriptionAfterScroll(
    label: String,
): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(label))
    return onNodeWithContentDescription(label)
}

/**
 * A helper used to scroll to and get a thr first matching node in a scrollable list. This is
 * intended to be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onAllNodesWithTextAfterScroll(
    text: String,
): SemanticsNodeInteractionCollection {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    return onAllNodesWithText(text)
}

/**
 * A helper used to scroll to and get a thr first matching node in a scrollable list. This is
 * intended to be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onAllNodesWithContentDescriptionAfterScroll(
    label: String,
): SemanticsNodeInteractionCollection {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(label))
    return onAllNodesWithContentDescription(label)
}

/**
 * A helper used to scroll to and get all matching nodes in a scrollable list. This is intended
 * to be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onFirstNodeWithTextAfterScroll(
    text: String,
): SemanticsNodeInteraction =
    onAllNodesWithTextAfterScroll(text).onFirst()

/**
 * A helper used to perform a custom accessibility action on a node with a given [label].
 *
 * @throws AssertionError if no action with the given [label] is found.
 */
fun SemanticsNodeInteraction.performCustomAccessibilityAction(label: String) {
    val tree = printToString()
    fetchSemanticsNode()
        .let {
            val customActions = it.config[SemanticsActions.CustomActions]
            customActions
                .find { action ->
                    action.label == label
                }
                ?.action
                ?.invoke()
                ?: throw AssertionError(
                    """
                    No action with label $label

                    Available actions: $customActions
                    in
                    $tree
                """.trimMargin(),
                )
        }
}

/**
 * Helper function to assert link annotation is applied to the given text in
 * the [mainString] and invoke click action if it is found.
 */
@Suppress("NestedBlockDepth")
fun ComposeTestRule.assertLinkAnnotationIsAppliedAndInvokeClickAction(
    mainString: String,
    expectedLinkCount: Int? = null,
) {
    this
        .onNodeWithText(mainString, substring = true, ignoreCase = true)
        .fetchSemanticsNode()
        .config
        .getOrNull(SemanticsProperties.Text)
        ?.let { text ->
            text.forEach {
                val linkAnnotations = it.getLinkAnnotations(0, it.length)
                if (linkAnnotations.isEmpty()) {
                    throw AssertionError(
                        "No link annotation found",
                    )
                } else {
                    linkAnnotations.forEach { annotationRange ->
                        val annotation = annotationRange.item as? LinkAnnotation.Clickable
                        val tag = annotation?.tag
                        assertNotNull(tag)
                        annotation?.linkInteractionListener?.onClick(annotation)
                    }
                    expectedLinkCount?.let {
                        assertEquals(expectedLinkCount, linkAnnotations.size)
                    }
                }
            }
        }
}
