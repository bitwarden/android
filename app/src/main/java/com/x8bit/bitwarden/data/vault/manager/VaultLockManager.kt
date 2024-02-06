package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the locking and unlocking of user vaults.
 */
interface VaultLockManager {
    /**
     * Flow that represents the current vault lock state for each user.
     */
    val vaultUnlockDataStateFlow: StateFlow<List<VaultUnlockData>>

    /**
     * Whether or not the vault is currently locked for the given [userId].
     */
    fun isVaultUnlocked(userId: String): Boolean

    /**
     * Whether or not the vault is currently unlocking for the given [userId].
     */
    fun isVaultUnlocking(userId: String): Boolean

    /**
     * Locks the vault for the user with the given [userId].
     */
    fun lockVault(userId: String)

    /**
     * Locks the vault for the current user if currently unlocked.
     */
    fun lockVaultForCurrentUser()

    /**
     * Attempt to unlock the vault with the specified user information.
     *
     * Note that when [organizationKeys] is absent, no attempt will be made to unlock the vault
     * for organization data.
     */
    @Suppress("LongParameterList")
    suspend fun unlockVault(
        userId: String,
        email: String,
        kdf: Kdf,
        privateKey: String,
        initUserCryptoMethod: InitUserCryptoMethod,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult

    /**
     * Suspends until the vault for the given [userId] is unlocked.
     */
    suspend fun waitUntilUnlocked(userId: String)
}
