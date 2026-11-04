package com.x8bit.bitwarden.robot

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Logs in with a master password, starting from the Landing screen.
 *
 * Spans the Landing and Login screens, so it lives here rather than in either robot. Use it in
 * tests that need a logged-in user but are not *about* login.
 */
fun ComposeTestRule.logInWithMasterPassword(
    email: String,
    password: String,
) {
    landing {
        enterEmail(email = email)
        tapContinue()
    }
    login {
        enterMasterPassword(password = password)
        tapLogInWithMasterPassword()
    }
}
