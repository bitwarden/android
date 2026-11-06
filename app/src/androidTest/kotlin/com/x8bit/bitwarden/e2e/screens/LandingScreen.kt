package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * Email entry screen with the region (server) selector.
 */
class LandingScreen(private val rule: ComposeTestRule) {
    /**
     * Picks "Self-hosted" in the region selector, which opens the environment screen.
     */
    fun selectSelfHosted(): EnvironmentScreen {
        rule.waitForTag("RegionSelectorDropdown").performClick()
        rule.waitForNode(
            hasTestTag("AlertRadioButtonOption") and
                hasAnyDescendant(hasText(appString(BitwardenString.self_hosted))),
        ).performClick()
        return EnvironmentScreen(rule)
    }

    /**
     * Enters [email] and continues to the master password screen.
     */
    fun continueWithEmail(email: String): LoginScreen {
        // The "environment saved" snackbar can sit over Continue and take the tap.
        rule.waitForNodeToDisappear(hasText(appString(BitwardenString.environment_saved)))
        rule.waitForTag("EmailAddressEntry").performTextReplacement(email)
        rule.waitForTag("ContinueButton").performScrollTo().performClick()
        return LoginScreen(rule)
    }
}
