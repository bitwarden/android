package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.auth.repository.model.UserState.Account

/**
 * Represents the overall "user state" of the current active user as well as any users that may be
 * switched to.
 *
 * @property activeUserId The ID of the current active user.
 * @property accounts A mapping between user IDs and the [Account] information associated with
 * that user.
 * @property specialCircumstance A special circumstance (if any) that may be present.
 */
data class UserState(
    val activeUserId: String,
    val accounts: List<Account>,
    val specialCircumstance: SpecialCircumstance? = null,
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
     * Returns `true` if a new user is in the process of being added, `false` otherwise.
     */
    val hasPendingAccountAddition: Boolean
        get() = specialCircumstance == SpecialCircumstance.PendingAccountAddition

    /**
     * Basic account information about a given user.
     *
     * @property userId The ID of the user.
     * @property email The user's email address.
     * @property name The user's name (if applicable).
     * @property avatarColorHex Hex color value for a user's avatar in the "#AARRGGBB" format.
     * @property isPremium `true` if the account has a premium membership.
     * @property isVaultUnlocked Whether or not the user's vault is currently unlocked.
     */
    data class Account(
        val userId: String,
        val name: String?,
        val email: String,
        val avatarColorHex: String,
        val isPremium: Boolean,
        val isVaultUnlocked: Boolean,
    )

    /**
     * Represents a special account-related circumstance.
     */
    sealed class SpecialCircumstance {

        /**
         * There is an additional account that is pending login/registration in order to have
         * multiple accounts available.
         */
        data object PendingAccountAddition : SpecialCircumstance()
    }
}
