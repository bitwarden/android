package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick

/**
 * First-launch welcome screen.
 */
class WelcomeScreen(private val rule: ComposeTestRule) {
    /**
     * Chooses to log in with an existing account.
     */
    fun chooseLogIn(): LandingScreen {
        rule.waitForTag("ChooseLoginButton").performClick()
        return LandingScreen(rule)
    }
}
