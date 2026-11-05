package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick

/**
 * Opens the [WelcomeRobot] and runs [block] on it.
 */
fun ComposeTestRule.welcome(
    block: WelcomeRobot.() -> Unit,
): WelcomeRobot = WelcomeRobot(rule = this).apply(block)

/**
 * Robot for the first-launch Welcome screen.
 */
class WelcomeRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule) {
    /**
     * Taps "Log in" to log in with an existing account.
     */
    fun tapLogIn() {
        waitForTag(Tags.CHOOSE_LOGIN_BUTTON).performClick()
    }

    private object Tags {
        const val CHOOSE_LOGIN_BUTTON = "ChooseLoginButton"
    }
}
