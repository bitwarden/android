package e2e.pageObjects.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import e2e.pageObjects.Page

class UnlockVaultPage(composeTestRule: ComposeTestRule) : Page(composeTestRule) {

    // UI Elements
    private val passwordEntryTag by lazy { getElement("MasterPasswordEntry") }
    private val unlockVaultButtonTag by lazy { getElement("UnlockVaultButton") }

    fun enterPassword(password: String): UnlockVaultPage {
        passwordEntryTag.performTextInput(password)
        return this
    }

    fun performUnlockVault(password: String): VaultPage {
        unlockVaultButtonTag.assertIsDisplayed()
        passwordEntryTag.performClick().performTextInput(password)
        unlockVaultButtonTag.performClick()
        return VaultPage(composeTestRule)
    }
}
