package com.x8bit.bitwarden.robot

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

/**
 * The default amount of time, in milliseconds, a robot will wait for a node to appear.
 */
private const val DEFAULT_TIMEOUT_MS: Long = 10_000L

/**
 * Base class for all screen robots.
 *
 * A robot describes *what* a user does on a screen (e.g. "enter email", "tap continue") and hides
 * *how* that is done (finding nodes by test tag, scrolling, waiting). Screen robots extend this
 * class and build their actions out of these generic primitives, so tests never need to touch
 * the Compose testing APIs directly.
 *
 * Prefer test tags over text: tags are stable across locales and copy changes.
 */
abstract class BaseRobot(
    protected val composeTestRule: ComposeTestRule,
) {
    // region Finders

    /**
     * Waits for a node with the given [tag] to exist and returns it.
     */
    protected fun nodeWithTag(
        tag: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): SemanticsNodeInteraction {
        waitForTag(tag = tag, timeoutMs = timeoutMs)
        return with(composeTestRule) { onNodeWithTag(testTag = tag, useUnmergedTree = true) }
    }

    /**
     * Waits for a node with the given [text] to exist and returns it.
     */
    protected fun nodeWithText(
        text: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): SemanticsNodeInteraction {
        waitForText(text = text, timeoutMs = timeoutMs)
        return with(composeTestRule) { onNodeWithText(text = text, useUnmergedTree = true) }
    }

    // endregion Finders

    // region Actions

    /**
     * Scrolls to and clicks the node with the given [tag].
     */
    protected fun tapOnTag(tag: String) {
        nodeWithTag(tag = tag)
            .performScrollTo()
            .performClick()
    }

    /**
     * Scrolls to and clicks the node with the given [text].
     */
    protected fun tapOnText(text: String) {
        nodeWithText(text = text)
            .performScrollTo()
            .performClick()
    }

    /**
     * Replaces the contents of the text field with the given [tag] with [text].
     */
    protected fun typeTextInTag(tag: String, text: String) {
        nodeWithTag(tag = tag).apply {
            performScrollTo()
            performTextClearance()
            performTextInput(text)
        }
    }

    // endregion Actions

    // region Assertions

    /**
     * Asserts that the node with the given [tag] is displayed.
     */
    protected fun assertTagDisplayed(tag: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        nodeWithTag(tag = tag, timeoutMs = timeoutMs).assertIsDisplayed()
    }

    /**
     * Asserts that the node with the given [text] is displayed.
     */
    protected fun assertTextDisplayed(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        nodeWithText(text = text, timeoutMs = timeoutMs).assertIsDisplayed()
    }

    /**
     * Asserts that the node with the given [tag] contains [text].
     */
    protected fun assertTagContainsText(tag: String, text: String) {
        nodeWithTag(tag = tag).assertTextContains(value = text, substring = true)
    }

    /**
     * Asserts that the node with the given [tag] is enabled or disabled.
     */
    protected fun assertTagEnabled(tag: String, isEnabled: Boolean = true) {
        nodeWithTag(tag = tag).apply {
            if (isEnabled) assertIsEnabled() else assertIsNotEnabled()
        }
    }

    // endregion Assertions

    // region Waits

    /**
     * Blocks until a node with the given [tag] exists or [timeoutMs] elapses.
     *
     * End-to-end tests hit real (or fake) network and disk, so screens do not appear
     * synchronously. Every finder waits by default to keep tests free of `Thread.sleep`.
     */
    protected fun waitForTag(tag: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitForNode(matcher = hasTestTag(tag), timeoutMs = timeoutMs)
    }

    /**
     * Blocks until a node with the given [text] exists or [timeoutMs] elapses.
     */
    protected fun waitForText(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitForNode(matcher = hasText(text), timeoutMs = timeoutMs)
    }

    /**
     * Blocks until at least one node matching [matcher] exists or [timeoutMs] elapses.
     *
     * Uses the stable `waitUntil` API rather than the experimental `waitUntilAtLeastOneExists`.
     */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMs: Long) {
        with(composeTestRule) {
            waitUntil(timeoutMillis = timeoutMs) {
                onAllNodes(matcher = matcher, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
    }

    // endregion Waits
}
