package com.x8bit.bitwarden.e2e.screens

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Waits until at least one node matches [matcher] and returns it; actions on the result require
 * the match to be unique. Fails with a timeout naming [matcher] if nothing matches.
 */
fun ComposeTestRule.waitForNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    waitUntil(conditionDescription = matcher.description, timeoutMillis = NETWORK_TIMEOUT_MS) {
        onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    return onNode(matcher, useUnmergedTree = true)
}

/**
 * Waits until a node with [testTag] exists and returns it.
 */
fun ComposeTestRule.waitForTag(testTag: String): SemanticsNodeInteraction =
    waitForNode(hasTestTag(testTag))

/**
 * Like [waitForNode] for content inside a lazy list, which only composes visible items: scrolls
 * the screen's list to [matcher] on each attempt, so the node is found on any screen size.
 */
fun ComposeTestRule.scrollToNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    waitUntil(conditionDescription = matcher.description, timeoutMillis = NETWORK_TIMEOUT_MS) {
        val lists = onAllNodes(hasScrollToNodeAction(), useUnmergedTree = true)
        lists.fetchSemanticsNodes().isNotEmpty() && lists.onFirst().scrolledTo(matcher)
    }
    return onNode(matcher, useUnmergedTree = true)
}

/**
 * Like [scrollToNode], then clicks the node. Scroll and click are retried together because the
 * list can recompose between them and briefly drop the node (seen on the vault after login).
 */
fun ComposeTestRule.scrollToAndClick(matcher: SemanticsMatcher) {
    waitUntil(conditionDescription = matcher.description, timeoutMillis = NETWORK_TIMEOUT_MS) {
        val lists = onAllNodes(hasScrollToNodeAction(), useUnmergedTree = true)
        lists.fetchSemanticsNodes().isNotEmpty() &&
            lists.onFirst().scrolledTo(matcher) &&
            onNode(matcher, useUnmergedTree = true).clicked()
    }
}

/**
 * Waits until no node matches [matcher], e.g. for a transient message to be dismissed.
 */
fun ComposeTestRule.waitForNodeToDisappear(matcher: SemanticsMatcher) {
    waitUntil(
        conditionDescription = "no node ${matcher.description}",
        timeoutMillis = NETWORK_TIMEOUT_MS,
    ) {
        onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }
}

/**
 * The field's raw value, before any visual transformation.
 */
fun SemanticsNodeInteraction.rawText(): String {
    val config = fetchSemanticsNode().config
    return config.getOrNull(SemanticsProperties.InputText)?.text
        ?: config.getOrNull(SemanticsProperties.EditableText)?.text
        ?: error("Node has no text field value: ${fetchSemanticsNode().config}")
}

/**
 * The field's value as displayed, after visual transformation (e.g. a masked password).
 */
fun SemanticsNodeInteraction.displayedText(): String =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text
        ?: error("Node has no displayed text field value: ${fetchSemanticsNode().config}")

/**
 * The app's localized string for [id], so text matching works in any device locale.
 */
fun appString(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

private fun SemanticsNodeInteraction.scrolledTo(matcher: SemanticsMatcher): Boolean =
    try {
        performScrollToNode(matcher)
        true
    } catch (_: AssertionError) {
        // The list doesn't contain the node yet (e.g. sync still in progress).
        false
    }

private fun SemanticsNodeInteraction.clicked(): Boolean =
    try {
        performClick()
        true
    } catch (_: AssertionError) {
        // The node was gone before the touch was injected, so nothing was clicked.
        false
    }

/**
 * Upper bound for waits that depend on the network (login, sync), which Compose idling
 * doesn't track.
 */
private const val NETWORK_TIMEOUT_MS = 30_000L
