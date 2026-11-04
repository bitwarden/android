package com.x8bit.bitwarden.robot

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Entry point for the Vault screen robot.
 */
fun ComposeTestRule.vault(
    block: VaultRobot.() -> Unit,
): VaultRobot = VaultRobot(composeTestRule = this).apply(block)

/**
 * Robot for the Vault screen, the destination after a successful login.
 *
 * Only holds what the login sample needs; add vault actions here as vault tests are written.
 */
class VaultRobot(
    composeTestRule: ComposeTestRule,
) : BaseRobot(composeTestRule) {

    /**
     * Asserts the Vault screen is displayed.
     *
     * Uses a longer timeout because reaching the vault after login involves network, KDF and
     * sync.
     */
    fun assertVaultIsDisplayed() {
        // TODO: `LoginFilter` likely only appears when the vault has items. Add a dedicated tag
        //  to the Vault screen and use it here.
        assertTagDisplayed(tag = Tags.LOGIN_FILTER, timeoutMs = VAULT_TIMEOUT_MS)
    }

    /**
     * Locators for the Vault screen. These mirror the `testTag` values in `VaultScreen`.
     */
    private object Tags {
        const val LOGIN_FILTER = "LoginFilter"
    }

    private companion object {
        const val VAULT_TIMEOUT_MS: Long = 30_000L
    }
}
