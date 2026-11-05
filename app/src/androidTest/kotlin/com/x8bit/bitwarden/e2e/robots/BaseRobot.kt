package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
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
) {
    /**
     * Waits until a node with [testTag] exists and returns it.
     */
    protected fun waitForTag(testTag: String): SemanticsNodeInteraction =
        rule.waitForTag(testTag = testTag)

    /**
     * Waits until a node matching [matcher] exists and returns it.
     */
    protected fun waitForNode(matcher: SemanticsMatcher): SemanticsNodeInteraction =
        rule.waitForNode(matcher = matcher)

    /**
     * Scrolls the screen's list until a node matching [matcher] is composed and returns it.
     */
    protected fun scrollToNode(matcher: SemanticsMatcher): SemanticsNodeInteraction =
        rule.scrollToNode(matcher = matcher)

    /**
     * Scrolls the screen's list to a node matching [matcher] and clicks it.
     */
    protected fun scrollToAndClick(matcher: SemanticsMatcher) {
        rule.scrollToAndClick(matcher = matcher)
    }

    /**
     * Waits until no node matches [matcher].
     */
    protected fun waitForNodeToDisappear(matcher: SemanticsMatcher) {
        rule.waitForNodeToDisappear(matcher = matcher)
    }
}
