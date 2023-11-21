package com.x8bit.bitwarden.data.vault.repository.model

/**
 * General description of the vault across multiple users.
 *
 * @property unlockedVaultUserIds The user IDs for all users that currently have unlocked vaults.
 */
data class VaultState(
    val unlockedVaultUserIds: Set<String>,
)
