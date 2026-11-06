package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.x8bit.bitwarden.e2e.screens.scrollToAndClick
import com.x8bit.bitwarden.e2e.screens.scrollToNode
import com.x8bit.bitwarden.e2e.screens.waitForNode
import com.x8bit.bitwarden.e2e.screens.waitForNodeToDisappear
import com.x8bit.bitwarden.e2e.screens.waitForTag

/**
 * Marks the robot DSL so that inside a nested robot block (e.g. `vault { }` inside `login { }`)
 * only the innermost robot's functions can be called. Without it, a test could call a Login
 * screen action while the Vault screen is showing.
 */
@DslMarker
annotation class RobotDsl

/**
 * Base class for screen robots.
 *
 * A robot knows how to work one screen: its actions and assertions. It does not know which screen
 * comes next; the test (or a shared flow) says that by opening the next robot. This lets one
 * action lead to different screens (e.g. logging in can show the vault, an error, or two-step
 * login) without a method per outcome.
 *
 * Each robot is tied to its screen's root test tag ([screenTag]), set by hand on the screen's
 * root composable (e.g. `Modifier.testTag(ScreenTags.LOGIN)` on its `BitwardenScaffold`). Opening a
 * robot fails fast if that root never appears, and every lookup only matches nodes inside it. A
 * dialog opens in its own window, outside any screen root, so a screen robot never matches a
 * dialog's nodes; dialogs have their own robot rooted at the dialog's tag.
 *
 * Robots are opened with an entry point that extends [ComposeTestRule], so tests wrap their body
 * in `with(composeRule) { ... }` once:
 *
 * ```
 * with(composeRule) {
 *     login {
 *         enterMasterPassword(password)
 *         tapLogInWithMasterPassword()
 *
 *         vault {
 *             assertVaultIsDisplayed()
 *         }
 *     }
 * }
 * ```
 */
@RobotDsl
abstract class BaseRobot(
    private val rule: ComposeTestRule,
    private val screenTag: String,
) {
    init {
        require(screenTag.isNotBlank()) {
            "${this::class.simpleName} needs a non-blank screen root tag."
        }
        try {
            rule.waitForTag(testTag = screenTag)
        } catch (e: ComposeTimeoutException) {
            throw AssertionError(
                "${this::class.simpleName}: screen root '$screenTag' never appeared. Is the app " +
                    "on this screen, and does its root composable have this testTag?",
                e,
            )
        }
    }

    /**
     * Waits until a node with [testTag] exists inside this screen and returns it.
     */
    protected fun waitForTag(testTag: String): SemanticsNodeInteraction =
        rule.waitForNode(matcher = inScreen(hasTestTag(testTag)))

    /**
     * Waits until a node matching [matcher] exists inside this screen and returns it.
     */
    protected fun waitForNode(matcher: SemanticsMatcher): SemanticsNodeInteraction =
        rule.waitForNode(matcher = inScreen(matcher))

    /**
     * Scrolls the screen's list until a node matching [matcher] inside this screen is composed and
     * returns it.
     */
    protected fun scrollToNode(matcher: SemanticsMatcher): SemanticsNodeInteraction =
        rule.scrollToNode(matcher = inScreen(matcher))

    /**
     * Scrolls the screen's list to a node matching [matcher] inside this screen and clicks it.
     */
    protected fun scrollToAndClick(matcher: SemanticsMatcher) {
        rule.scrollToAndClick(matcher = inScreen(matcher))
    }

    /**
     * Waits until no node inside this screen matches [matcher].
     */
    protected fun waitForNodeToDisappear(matcher: SemanticsMatcher) {
        rule.waitForNodeToDisappear(matcher = inScreen(matcher))
    }

    /**
     * Asserts this screen's root is displayed.
     */
    protected fun assertScreenIsDisplayed() {
        rule.waitForTag(testTag = screenTag).assertIsDisplayed()
    }

    /**
     * Limits [matcher] to nodes inside this screen's root.
     */
    private fun inScreen(matcher: SemanticsMatcher): SemanticsMatcher =
        matcher and hasAnyAncestor(hasTestTag(screenTag))
}
