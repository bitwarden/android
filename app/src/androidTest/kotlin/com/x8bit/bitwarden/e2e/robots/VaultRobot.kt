package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.x8bit.bitwarden.ui.platform.util.ScreenTags

/**
 * Opens the [VaultRobot] and runs [block] on it.
 */
fun ComposeTestRule.vault(
    block: VaultRobot.() -> Unit,
): VaultRobot = VaultRobot(rule = this).apply(block)

/**
 * Robot for the vault list screen. The bottom tab bar is a separate screen: [NavBarRobot].
 */
class VaultRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule = rule, screenTag = ScreenTags.VAULT) {
    /**
     * Asserts the vault list screen is shown.
     */
    fun assertVaultIsDisplayed() {
        assertScreenIsDisplayed()
    }

    /**
     * Opens the item named exactly [name] from the vault list.
     */
    fun openItem(name: String) {
        scrollToAndClick(hasTestTag(Tags.CIPHER_CELL) and hasAnyDescendant(hasText(name)))
    }

    private object Tags {
        const val CIPHER_CELL = "CipherCell"
    }
}
