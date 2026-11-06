package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.x8bit.bitwarden.e2e.screens.displayedText
import com.x8bit.bitwarden.e2e.screens.rawText
import com.x8bit.bitwarden.ui.platform.util.ScreenTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

/**
 * Opens the [VaultItemRobot] and runs [block] on it.
 */
fun ComposeTestRule.vaultItem(
    block: VaultItemRobot.() -> Unit,
): VaultItemRobot = VaultItemRobot(rule = this).apply(block)

/**
 * Robot for the View screen of a login item.
 */
class VaultItemRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule = rule, screenTag = ScreenTags.VAULT_ITEM) {
    /**
     * Taps the button that shows the password.
     */
    fun tapShowPassword() {
        scrollToAndClick(hasTestTag(Tags.VIEW_PASSWORD_BUTTON))
    }

    /**
     * Asserts the item's username is [expected].
     */
    fun assertUsername(expected: String) {
        assertEquals(expected, scrollToNode(hasTestTag(Tags.USERNAME_FIELD)).rawText())
    }

    /**
     * Asserts the password is masked, i.e. [password] is not what's displayed.
     */
    fun assertPasswordIsHidden(password: String) {
        assertNotEquals(password, displayedPassword())
    }

    /**
     * Asserts the displayed password is [expected]. Use after [tapShowPassword].
     */
    fun assertPasswordIsShown(expected: String) {
        assertEquals(expected, displayedPassword())
    }

    /**
     * Asserts the item's URI is [expected] (the item must have exactly one).
     */
    fun assertUri(expected: String) {
        assertEquals(expected, scrollToNode(hasTestTag(Tags.URI_FIELD)).rawText())
    }

    private fun displayedPassword(): String =
        scrollToNode(hasTestTag(Tags.PASSWORD_FIELD)).displayedText()

    private object Tags {
        const val USERNAME_FIELD = "LoginUsernameEntry"
        const val PASSWORD_FIELD = "LoginPasswordEntry"
        const val VIEW_PASSWORD_BUTTON = "LoginViewPasswordButton"
        const val URI_FIELD = "LoginUriEntry"
    }
}
