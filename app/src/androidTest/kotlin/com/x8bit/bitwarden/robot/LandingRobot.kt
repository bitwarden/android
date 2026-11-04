package com.x8bit.bitwarden.robot

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Entry point for the Landing screen robot.
 *
 * ```
 * landing {
 *     enterEmail("user@bitwarden.com")
 *     tapContinue()
 * }
 * ```
 */
fun ComposeTestRule.landing(
    block: LandingRobot.() -> Unit,
): LandingRobot = LandingRobot(composeTestRule = this).apply(block)

/**
 * Robot for the Landing screen, where the user enters their email address, picks a region and
 * continues to the Login screen.
 */
class LandingRobot(
    composeTestRule: ComposeTestRule,
) : BaseRobot(composeTestRule) {

    // region Actions

    /**
     * Enters [email] into the email address field.
     */
    fun enterEmail(email: String) {
        typeTextInTag(tag = Tags.EMAIL_FIELD, text = email)
    }

    /**
     * Toggles the "Remember me" switch.
     */
    fun tapRememberMe() {
        tapOnTag(tag = Tags.REMEMBER_ME_SWITCH)
    }

    /**
     * Opens the region selector and chooses [regionLabel] (e.g. "bitwarden.eu").
     */
    fun selectRegion(regionLabel: String) {
        tapOnTag(tag = Tags.REGION_SELECTOR)
        tapOnText(text = regionLabel)
    }

    /**
     * Taps "Continue" to move to the Login screen.
     */
    fun tapContinue() {
        tapOnTag(tag = Tags.CONTINUE_BUTTON)
    }

    // endregion Actions

    // region Assertions

    /**
     * Asserts the Landing screen is displayed.
     */
    fun assertLandingScreenIsDisplayed() {
        assertTagDisplayed(tag = Tags.EMAIL_FIELD)
    }

    /**
     * Asserts the "Continue" button is enabled or disabled.
     */
    fun assertContinueButtonIsEnabled(isEnabled: Boolean = true) {
        assertTagEnabled(tag = Tags.CONTINUE_BUTTON, isEnabled = isEnabled)
    }

    // endregion Assertions

    /**
     * Locators for the Landing screen. These mirror the `testTag` values in `LandingScreen`.
     */
    private object Tags {
        const val EMAIL_FIELD = "EmailAddressEntry"
        const val REGION_SELECTOR = "RegionSelectorDropdown"
        const val REMEMBER_ME_SWITCH = "RememberMeSwitch"
        const val CONTINUE_BUTTON = "ContinueButton"
    }
}
