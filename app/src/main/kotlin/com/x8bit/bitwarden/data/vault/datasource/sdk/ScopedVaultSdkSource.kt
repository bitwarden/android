package com.x8bit.bitwarden.data.vault.datasource.sdk

/**
 * This is a non-singleton instance of the [VaultSdkSource] that is intentionally separate; this
 * allows you to temporarily unlock vaults for a given user within its own scope without affecting
 * the foreground behavior of the app.
 *
 * Users of this class must always call [ScopedVaultSdkSource.clearCrypto] when they are done using
 * the unlocked vault in order to ensure that this instance of the vault is re-locked.
 */
interface ScopedVaultSdkSource : VaultSdkSource
