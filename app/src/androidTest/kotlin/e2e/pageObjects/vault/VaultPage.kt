package e2e.pageObjects.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import e2e.pageObjects.Page
import e2e.pageObjects.settings.SettingsPage

class VaultPage(composeTestRule: ComposeTestRule) : Page(composeTestRule) {

    // UI Elements
    private val settingsMenuButton by lazy { getElement("SettingsTab") }
    private val addItemButton by lazy { getElement("AddItemButton") }

    fun assertVaultIsUnlocked() {
        addItemButton.assertIsDisplayed()
    }

    fun navigateToSettingsPage(): SettingsPage {
        settingsMenuButton.performClick()
        return SettingsPage(composeTestRule)
    }
}
