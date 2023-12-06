package com.x8bit.bitwarden.ui.util

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
 * A helper that asserts that the node does not exist in the scrollable list.
 */
fun ComposeContentTestRule.assertScrollableNodeDoesNotExist(text: String) {
    val scrollableNodeInteraction = onNode(hasScrollToNodeAction())
    assertThrows<AssertionError> {
        // throws since it cannot find the node.
        scrollableNodeInteraction.performScrollToNode(hasText(text))
    }
}

/**
 * A helper used to scroll to and get the matching node in a scrollable list. This is intended to
 * be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onNodeWithTextAfterScroll(text: String): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    return onNodeWithText(text)
}

/**
 * A helper used to scroll to and get a thr first matching node in a scrollable list. This is
 * intended to be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onFirstNodeWithTextAfterScroll(text: String): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    return onAllNodesWithText(text).onFirst()
}
