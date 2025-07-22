package com.x8bit.bitwarden.e2e.pages

import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.ComposeTestRule
import e2e.pageObjects.Page
import e2e.pageObjects.login.EnvironmentSettingsPage
import e2e.pageObjects.vault.VaultPage

/**
 * Page Object representing the Login screen of the Bitwarden app.
 * This class encapsulates all the UI elements and actions available on the login screen.
 */
class LoginPage(composeTestRule: ComposeTestRule) : Page(composeTestRule) {

    private val emailField by lazy { getElement("EmailAddressEntry") }
    private val masterPasswordField by lazy { getElement("MasterPasswordEntry") }
    private val continueButton by lazy { getElement("ContinueButton") }
    private val loginWithMasterPasswordButton by lazy {
        getElement("LogInWithMasterPasswordButton")
    }
    private val regionSelectorButton by lazy { getElement("RegionSelectorDropdown") }
    private val openSettingsButton by lazy { getElement("AppSettingsButton") }
    private val otherSettingsButton by lazy { getElement("OtherSettingsButton") }
    private val allowScreenCaptureToggle by lazy { getElement("AllowScreenCaptureSwitch") }
    private val goBackButton by lazy { getElement("CloseButton") }

    /**
     * Enters the master password in the password field
     * @param password The master password to enter
     * @return This LoginPage instance for method chaining
     */
    fun performLogin(email: String, password: String): VaultPage {
        emailField
            .performClick()
            .performTextInput(email)
        continueButton
            .performClick()
        masterPasswordField
            .performClick()
            .performTextInput(password)
        loginWithMasterPasswordButton.performClick()
        return VaultPage(composeTestRule)
    }

    fun openEnvironmentSettings(): EnvironmentSettingsPage {
        regionSelectorButton.performClick()
        getElementByText("Self-hosted")
            .performClick()
        return EnvironmentSettingsPage(composeTestRule)
    }

    fun turnOnScreenRecording(): LoginPage {
        openSettingsButton.performClick()
        otherSettingsButton.performClick()
        allowScreenCaptureToggle.performClick()
        goBackButton.performClick()
        goBackButton.performClick()
        return this
    }
}
