package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.auth.repository.model.UserState.Account
import com.x8bit.bitwarden.data.platform.repository.model.Environment

/**
 * Represents the overall "user state" of the current active user as well as any users that may be
 * switched to.
 *
 * @property activeUserId The ID of the current active user.
 * @property accounts A mapping between user IDs and the [Account] information associated with
 * that user.
 * @property hasPendingAccountAddition Returns `true` if there is an additional account that is
 * pending login/registration in order to have multiple accounts available.
 */
data class UserState(
    val activeUserId: String,
    val accounts: List<Account>,
    val hasPendingAccountAddition: Boolean = false,
) {
    init {
        require(accounts.any { it.userId == activeUserId })
    }

    /**
     * The [Account] associated with the current [activeUserId].
     */
    val activeAccount: Account
        get() = accounts.first { it.userId == activeUserId }

    /**
     * Basic account information about a given user.
     *
     * @property userId The ID of the user.
     * @property email The user's email address.
     * @property name The user's name (if applicable).
     * @property avatarColorHex Hex color value for a user's avatar in the "#AARRGGBB" format.
     * @property environment The [Environment] associated with the user's account.
     * @property isPremium `true` if the account has a premium membership.
     * @property isLoggedIn `true` if the account is logged in, or `false` if it requires additional
     * authentication to view their vault.
     * @property isVaultUnlocked Whether or not the user's vault is currently unlocked.
     * @property organizations List of [Organization]s the user is associated with, if any.
     * @property isBiometricsEnabled Indicates that the biometrics mechanism for unlocking the
     * user's vault is enabled.
     * @property vaultUnlockType The mechanism by which the user's vault may be unlocked.
     */
    data class Account(
        val userId: String,
        val name: String?,
        val email: String,
        val avatarColorHex: String,
        val environment: Environment,
        val isPremium: Boolean,
        val isLoggedIn: Boolean,
        val isVaultUnlocked: Boolean,
        val organizations: List<Organization>,
        val isBiometricsEnabled: Boolean,
        val vaultUnlockType: VaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
    )
}
