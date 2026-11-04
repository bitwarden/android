package com.x8bit.bitwarden.robot

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Entry point for the Login screen robot.
 *
 * ```
 * login {
 *     enterMasterPassword("hunter2")
 *     tapLogInWithMasterPassword()
 * }
 * ```
 */
fun ComposeTestRule.login(
    block: LoginRobot.() -> Unit,
): LoginRobot = LoginRobot(composeTestRule = this).apply(block)

/**
 * Robot for the Login screen, where the user enters their master password or picks another way
 * to log in. Reached from the Landing screen after entering an email.
 */
class LoginRobot(
    composeTestRule: ComposeTestRule,
) : BaseRobot(composeTestRule) {

    // region Actions

    /**
     * Enters [password] into the master password field.
     */
    fun enterMasterPassword(password: String) {
        typeTextInTag(tag = Tags.MASTER_PASSWORD_FIELD, text = password)
    }

    /**
     * Toggles master password visibility.
     */
    fun tapShowPassword() {
        tapOnTag(tag = Tags.PASSWORD_VISIBILITY_TOGGLE)
    }

    /**
     * Taps "Log in with master password".
     */
    fun tapLogInWithMasterPassword() {
        tapOnTag(tag = Tags.LOG_IN_WITH_MASTER_PASSWORD_BUTTON)
    }

    /**
     * Taps "Log in with device".
     */
    fun tapLogInWithDevice() {
        tapOnTag(tag = Tags.LOG_IN_WITH_DEVICE_BUTTON)
    }

    /**
     * Taps "Log in with SSO".
     */
    fun tapLogInWithSso() {
        tapOnTag(tag = Tags.LOG_IN_WITH_SSO_BUTTON)
    }

    /**
     * Taps "Not you?" to return to the Landing screen.
     */
    fun tapNotYou() {
        tapOnTag(tag = Tags.NOT_YOU_LABEL)
    }

    /**
     * Dismisses the currently displayed alert dialog.
     */
    fun dismissAlert() {
        tapOnTag(tag = Tags.ALERT_ACCEPT_BUTTON)
    }

    // endregion Actions

    // region Assertions

    /**
     * Asserts the Login screen is displayed.
     */
    fun assertLoginScreenIsDisplayed() {
        assertTagDisplayed(tag = Tags.MASTER_PASSWORD_FIELD)
    }

    /**
     * Asserts the "Logging in as" label shows [email].
     */
    fun assertLoggingInAs(email: String) {
        assertTagContainsText(tag = Tags.LOGGING_IN_AS_LABEL, text = email)
    }

    /**
     * Asserts the "Log in with master password" button is enabled or disabled.
     */
    fun assertLogInButtonIsEnabled(isEnabled: Boolean = true) {
        assertTagEnabled(tag = Tags.LOG_IN_WITH_MASTER_PASSWORD_BUTTON, isEnabled = isEnabled)
    }

    /**
     * Asserts an error alert is displayed, optionally containing [message].
     */
    fun assertErrorAlertIsDisplayed(message: String? = null) {
        assertTagDisplayed(tag = Tags.ALERT_POPUP)
        message?.let { assertTagContainsText(tag = Tags.ALERT_CONTENT_TEXT, text = it) }
    }

    // endregion Assertions

    /**
     * Locators for the Login screen. These mirror the `testTag` values in `LoginScreen` and
     * `BitwardenBasicDialog`.
     */
    private object Tags {
        const val MASTER_PASSWORD_FIELD = "MasterPasswordEntry"
        const val PASSWORD_VISIBILITY_TOGGLE = "PasswordVisibilityToggle"
        const val LOG_IN_WITH_MASTER_PASSWORD_BUTTON = "LogInWithMasterPasswordButton"
        const val LOG_IN_WITH_DEVICE_BUTTON = "LogInWithAnotherDeviceButton"
        const val LOG_IN_WITH_SSO_BUTTON = "LogInWithSsoButton"
        const val LOGGING_IN_AS_LABEL = "LoggingInAsLabel"
        const val NOT_YOU_LABEL = "NotYouLabel"

        // Dialogs
        const val ALERT_POPUP = "AlertPopup"
        const val ALERT_CONTENT_TEXT = "AlertContentText"
        const val ALERT_ACCEPT_BUTTON = "AcceptAlertButton"
    }
}
