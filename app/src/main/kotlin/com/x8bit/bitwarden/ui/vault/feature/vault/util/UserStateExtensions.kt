package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import java.util.Locale

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
 * Given the [UserState.Account], returns the first two "initials" found when looking at the
 * [UserState.Account.name].
 *
 * Ex:
 * - "First Last" -> "FL"
 * - "First Second Last" -> "FS"
 * - "First" -> "FI"
 * - name is `null`, email is "test@bitwarden.com" -> "TE"
 */
val UserState.Account.initials: String
    get() {
        val names = this.name.orEmpty().split(" ").filter { it.isNotBlank() }
        return if (names.size >= 2) {
            names
                .take(2)
                .joinToString(separator = "") { it.first().toString() }
        } else {
            (this.name ?: this.email).take(2)
        }
            .uppercase(Locale.getDefault())
    }
