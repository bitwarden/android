package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.util.ScreenTags

/**
 * Opens the [NavBarRobot] and runs [block] on it.
 */
fun ComposeTestRule.navBar(
    block: NavBarRobot.() -> Unit,
): NavBarRobot = NavBarRobot(rule = this).apply(block)

/**
 * Robot for the unlocked vault's bottom tab bar. The bar is its own screen that hosts the tab
 * screens (vault, send, generator, settings), so its tabs sit outside those screens' roots and
 * need this robot rather than e.g. [VaultRobot].
 */
class NavBarRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule = rule, screenTag = ScreenTags.VAULT_UNLOCKED_NAV_BAR) {
    /**
     * Asserts the Settings tab is shown. It only exists once the vault is unlocked.
     */
    fun assertSettingsTabIsDisplayed() {
        waitForTag(Tags.SETTINGS_TAB).assertIsDisplayed()
    }

    /**
     * Taps the Vault tab.
     */
    fun tapVaultTab() {
        waitForTag(Tags.VAULT_TAB).performClick()
    }

    /**
     * Taps the Settings tab.
     */
    fun tapSettingsTab() {
        waitForTag(Tags.SETTINGS_TAB).performClick()
    }

    private object Tags {
        const val VAULT_TAB = "VaultTab"
        const val SETTINGS_TAB = "SettingsTab"
    }
}
