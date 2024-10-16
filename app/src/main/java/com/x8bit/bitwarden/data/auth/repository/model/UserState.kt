package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.model.UserState.Account
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
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

    val activeUserFirstTimeState: FirstTimeState
        get() = activeAccount.firstTimeState

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
     * @property needsPasswordReset If the user needs to reset their password.
     * @property needsMasterPassword Indicates whether the user needs to create a password (e.g.
     * they logged in using SSO and don't yet have one). NOTE: This should **not** be used to
     * determine whether a user has a master password. There are cases in which a user can both
     * not have a password but still not need one, such as TDE.
     * @property hasMasterPassword Indicates that the user does or does not have a master password.
     * @property organizations List of [Organization]s the user is associated with, if any.
     * @property isBiometricsEnabled Indicates that the biometrics mechanism for unlocking the
     * user's vault is enabled.
     * @property vaultUnlockType The mechanism by which the user's vault may be unlocked.
     * @property isUsingKeyConnector Indicates if the account is currently using a key connector.
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
        val needsPasswordReset: Boolean,
        val needsMasterPassword: Boolean,
        val hasMasterPassword: Boolean,
        val trustedDevice: TrustedDevice?,
        val organizations: List<Organization>,
        val isBiometricsEnabled: Boolean,
        val vaultUnlockType: VaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        val isUsingKeyConnector: Boolean,
        val onboardingStatus: OnboardingStatus,
        val firstTimeState: FirstTimeState,
    ) {
        /**
         * Indicates that the user does or does not have a means to manually unlock the vault.
         */
        val hasManualUnlockMechanism: Boolean
            get() = when (vaultUnlockType) {
                VaultUnlockType.MASTER_PASSWORD -> hasMasterPassword || isBiometricsEnabled
                VaultUnlockType.PIN -> true
            }
    }

    /**
     * Models the data related to trusted device encryption (TDE).
     */
    data class TrustedDevice(
        val isDeviceTrusted: Boolean,
        val hasAdminApproval: Boolean,
        val hasLoginApprovingDevice: Boolean,
        val hasResetPasswordPermission: Boolean,
    )
}
