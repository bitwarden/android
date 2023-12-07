package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary

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
        status = when {
            isActive -> AccountSummary.Status.ACTIVE
            this.isVaultUnlocked -> AccountSummary.Status.UNLOCKED
            else -> AccountSummary.Status.LOCKED
        },
    )
