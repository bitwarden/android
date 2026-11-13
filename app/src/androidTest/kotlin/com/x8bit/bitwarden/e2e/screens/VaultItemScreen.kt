package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * View screen of a login item.
 */
class VaultItemScreen(private val rule: ComposeTestRule) {
    /**
     * The item's username.
     */
    fun username(): String = rule.scrollToNode(hasTestTag("LoginUsernameEntry")).rawText()

    /**
     * The password as currently displayed: masked until [revealPassword] is used.
     */
    fun displayedPassword(): String =
        rule.scrollToNode(hasTestTag("LoginPasswordEntry")).displayedText()

    /**
     * Toggles the password to visible.
     */
    fun revealPassword(): VaultItemScreen {
        rule.scrollToAndClick(hasTestTag("LoginViewPasswordButton"))
        return this
    }

    /**
     * The item's URI (the item must have exactly one).
     */
    fun uri(): String = rule.scrollToNode(hasTestTag("LoginUriEntry")).rawText()
}
