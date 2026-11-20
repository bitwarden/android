package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement

/**
 * Master password screen.
 */
class LoginScreen(private val rule: ComposeTestRule) {
    /**
     * Logs in with [password] and returns the vault once its tab bar is shown.
     */
    fun logIn(password: String): VaultScreen {
        rule.waitForTag("MasterPasswordEntry").performTextReplacement(password)
        rule.waitForTag("LogInWithMasterPasswordButton").performScrollTo().performClick()
        rule.waitForTag("SettingsTab")
        return VaultScreen(rule)
    }
}
