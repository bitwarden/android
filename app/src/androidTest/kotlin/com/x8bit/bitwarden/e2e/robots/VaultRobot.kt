package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Opens the [VaultRobot] and runs [block] on it.
 */
fun ComposeTestRule.vault(
    block: VaultRobot.() -> Unit,
): VaultRobot = VaultRobot(rule = this).apply(block)

/**
 * Robot for the unlocked vault with the bottom tab bar.
 */
class VaultRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule) {
    /**
     * Asserts the unlocked vault is shown. The settings tab only exists in the unlocked vault's
     * navigation bar.
     */
    fun assertVaultIsDisplayed() {
        waitForTag(Tags.SETTINGS_TAB).assertIsDisplayed()
    }

    /**
     * Opens the item named exactly [name] from the vault list.
     */
    fun openItem(name: String) {
        scrollToAndClick(hasTestTag(Tags.CIPHER_CELL) and hasAnyDescendant(hasText(name)))
    }

    private object Tags {
        const val SETTINGS_TAB = "SettingsTab"
        const val CIPHER_CELL = "CipherCell"
    }
}
