package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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
        // Under test the "environment saved" snackbar covers Continue for ~25s, so click via the
        // semantics action instead of waiting for it to clear.
        waitForTag(Tags.CONTINUE_BUTTON)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    private object Tags {
        const val REGION_SELECTOR = "RegionSelectorDropdown"
        const val EMAIL_FIELD = "EmailAddressEntry"
        const val CONTINUE_BUTTON = "ContinueButton"
    }
}
