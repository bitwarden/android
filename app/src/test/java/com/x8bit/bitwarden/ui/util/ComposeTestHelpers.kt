package com.x8bit.bitwarden.ui.util

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import org.junit.jupiter.api.assertThrows

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
