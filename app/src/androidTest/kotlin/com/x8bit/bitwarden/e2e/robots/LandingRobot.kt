package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.e2e.screens.appString
import com.x8bit.bitwarden.ui.platform.util.ScreenTags

/**
 * Opens the [LandingRobot] and runs [block] on it.
 */
fun ComposeTestRule.landing(
    block: LandingRobot.() -> Unit,
): LandingRobot = LandingRobot(rule = this).apply(block)

/**
 * Robot for the Landing screen: email entry and the region (server) selector.
 */
class LandingRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule = rule, screenTag = ScreenTags.LANDING) {
    /**
     * Opens the region selector dialog. Pick a region with `alertDialog { selectOption(...) }`.
     */
    fun tapRegionSelector() {
        waitForTag(Tags.REGION_SELECTOR).performClick()
    }

    /**
     * Enters [email] into the email address field.
     */
    fun enterEmail(email: String) {
        waitForTag(Tags.EMAIL_FIELD).performTextReplacement(email)
    }

    /**
     * Taps "Continue" to move on with the entered email.
     */
    fun tapContinue() {
        // The "environment saved" snackbar can sit over Continue and take the tap.
        waitForNodeToDisappear(hasText(appString(BitwardenString.environment_saved)))
        waitForTag(Tags.CONTINUE_BUTTON).performScrollTo().performClick()
    }

    private object Tags {
        const val REGION_SELECTOR = "RegionSelectorDropdown"
        const val EMAIL_FIELD = "EmailAddressEntry"
        const val CONTINUE_BUTTON = "ContinueButton"
    }
}
