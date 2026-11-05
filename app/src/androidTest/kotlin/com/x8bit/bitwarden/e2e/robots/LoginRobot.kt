package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement

/**
 * Opens the [LoginRobot] and runs [block] on it.
 */
fun ComposeTestRule.login(
    block: LoginRobot.() -> Unit,
): LoginRobot = LoginRobot(rule = this).apply(block)

/**
 * Robot for the master password (Login) screen.
 *
 * [tapLogInWithMasterPassword] doesn't decide where the app goes next: the test opens the robot
 * for the screen it expects (the vault, or stays here for an error).
 */
class LoginRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule) {
    /**
     * Enters [password] into the master password field.
     */
    fun enterMasterPassword(password: String) {
        waitForTag(Tags.MASTER_PASSWORD_FIELD).performTextReplacement(password)
    }

    /**
     * Taps "Log in with master password".
     */
    fun tapLogInWithMasterPassword() {
        waitForTag(Tags.LOG_IN_BUTTON).performScrollTo().performClick()
    }

    /**
     * Asserts an error dialog is shown, e.g. after a wrong master password.
     */
    fun assertErrorDialogIsDisplayed() {
        waitForTag(Tags.ALERT_POPUP).assertIsDisplayed()
    }

    private object Tags {
        const val MASTER_PASSWORD_FIELD = "MasterPasswordEntry"
        const val LOG_IN_BUTTON = "LogInWithMasterPasswordButton"
        const val ALERT_POPUP = "AlertPopup"
    }
}
