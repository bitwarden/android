package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType

/**
 * Converts the given [UserState] to a list of [AccountSummary].
 */
fun UserState.toAccountSummaries(): List<AccountSummary> =
    accounts.map { account ->
        account.toAccountSummary(
            isActive = this.activeUserId == account.userId,
        )
    }

/**
 * Converts the given [UserState] to an [AccountSummary] with a [AccountSummary.status] of
 * [AccountSummary.Status.ACTIVE].
 */
fun UserState.toActiveAccountSummary(): AccountSummary =
    this
        .activeAccount
        .toAccountSummary(isActive = true)

/**
 * Converts the given [UserState.Account] to an [AccountSummary] with the correct
 * [AccountSummary.Status]. The status will take into account whether or not the given account
 * [isActive].
 */
fun UserState.Account.toAccountSummary(
    isActive: Boolean,
): AccountSummary =
    AccountSummary(
        userId = this.userId,
        name = this.name,
        email = this.email,
        avatarColorHex = this.avatarColorHex,
        environmentLabel = this.environment.label,
        isActive = isActive,
        isLoggedIn = this.isLoggedIn,
        isVaultUnlocked = this.isVaultUnlocked,
    )

/**
 * Converts the given [UserState.Account] to a [VaultFilterData] (if applicable). Filter data is
 * only relevant when the given account is associated with one or more organizations.
 */
fun UserState.Account.toVaultFilterData(
    isIndividualVaultDisabled: Boolean,
): VaultFilterData? =
    this
        .organizations
        .takeIf { it.isNotEmpty() }
        ?.let { organizations ->
            VaultFilterData(
                selectedVaultFilterType = VaultFilterType.AllVaults,
                vaultFilterTypes = listOfNotNull(
                    VaultFilterType.AllVaults,
                    VaultFilterType.MyVault.takeUnless { isIndividualVaultDisabled },
                    *organizations
                        .sortedBy { it.name }
                        .map { organization ->
                            VaultFilterType.OrganizationVault(
                                organizationId = organization.id,
                                organizationName = organization.name.orEmpty(),
                            )
                        }
                        .toTypedArray(),
                ),
            )
        }

/**
 * Returns a map of organization IDs and if they provide a premium status to the user for
 * items owned by that organization.
 */
fun UserState.Account.getOrganizationPremiumStatusMap(): Map<String, Boolean> {
    return organizations.associate { it.id to it.shouldUsersGetPremium }
}
