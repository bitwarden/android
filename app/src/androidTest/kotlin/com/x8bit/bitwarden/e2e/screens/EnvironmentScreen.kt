package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement

/**
 * Self-hosted environment settings.
 */
class EnvironmentScreen(private val rule: ComposeTestRule) {
    /**
     * Sets the server URL and saves, returning to the landing screen.
     */
    fun setServerUrl(url: String): LandingScreen {
        rule.waitForTag("ServerUrlEntry").performTextReplacement(url)
        rule.waitForTag("SaveButton").performClick()
        return LandingScreen(rule)
    }
}
