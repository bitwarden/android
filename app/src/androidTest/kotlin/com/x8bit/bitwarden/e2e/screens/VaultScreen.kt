package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * The unlocked vault with the bottom tab bar.
 */
class VaultScreen(private val rule: ComposeTestRule) {
    /**
     * The settings tab, which only exists in the unlocked vault's navigation bar.
     */
    val settingsTab: SemanticsNodeInteraction
        get() = rule.waitForTag("SettingsTab")

    /**
     * Opens the item named exactly [name] from the vault list.
     */
    fun openItem(name: String): VaultItemScreen {
        rule.scrollToAndClick(hasTestTag("CipherCell") and hasAnyDescendant(hasText(name)))
        return VaultItemScreen(rule)
    }
}
