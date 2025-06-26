package e2e.pageObjects.settings.accountSecurity

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import e2e.pageObjects.Page
import e2e.pageObjects.vault.UnlockVaultPage

/**
 * Page Object representing the Account Security screen of the Bitwarden app.
 * This class encapsulates all the UI elements and actions available on the account security screen.
 */
class AccountSecurityPage(composeTestRule: ComposeTestRule) : Page(composeTestRule)  {

    // UI Elements
    private val lockNowLabel by lazy { getElement("LockNowLabel") }

    /**
     * Locks the vault
     * @return This AccountSecurityPage instance for method chaining
     */
    fun lockVault(): UnlockVaultPage {
        lockNowLabel.performScrollTo().performClick()
        lockNowLabel.assertIsNotDisplayed()
        return UnlockVaultPage(composeTestRule)
    }
}
