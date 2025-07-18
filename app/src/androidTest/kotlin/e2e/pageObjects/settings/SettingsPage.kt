package e2e.pageObjects.settings

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import e2e.pageObjects.Page
import e2e.pageObjects.settings.accountSecurity.AccountSecurityPage

class SettingsPage(composeTestRule: ComposeTestRule) : Page(composeTestRule) {

    // UI Elements
    private val accountSecurityButton by lazy { getElement("AccountSecuritySettingsButton") }

    /**
     * Navigates to the Account Security settings
     * @return This SettingsPage instance for method chaining
     */
    fun navigateToAccountSecurity(): AccountSecurityPage {
        accountSecurityButton.performClick()
        return AccountSecurityPage(composeTestRule)
    }
}
