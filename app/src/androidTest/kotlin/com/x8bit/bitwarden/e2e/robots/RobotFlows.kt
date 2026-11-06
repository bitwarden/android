package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.e2e.screens.appString
import com.x8bit.bitwarden.e2e.seeder.SeededAccount

/**
 * Points a fresh install at [serverUrl] and continues to the master password screen for [email].
 * Ends on the Login screen; the caller decides what happens there.
 */
fun ComposeTestRule.goToLoginScreen(serverUrl: String, email: String) {
    accessibilityDisclosure {
        tapIUnderstand()
    }
    welcome {
        tapLogIn()
    }
    landing {
        tapRegionSelector()

        alertDialog {
            selectOption(text = appString(BitwardenString.self_hosted))
        }
    }
    environment {
        enterServerUrl(url = serverUrl)
        tapSave()
    }
    landing {
        enterEmail(email = email)
        tapContinue()
    }
}

/**
 * Logs in as [account] on [serverUrl] from a fresh install. Doesn't wait for the vault; tests
 * that need it open `vault { }`, which waits for it.
 */
fun ComposeTestRule.logInWithMasterPassword(serverUrl: String, account: SeededAccount) {
    goToLoginScreen(serverUrl = serverUrl, email = account.email)
    login {
        enterMasterPassword(password = account.password)
        tapLogInWithMasterPassword()
    }
}
