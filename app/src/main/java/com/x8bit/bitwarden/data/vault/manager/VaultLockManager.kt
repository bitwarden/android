package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the locking and unlocking of user vaults.
 */
interface VaultLockManager {
    /**
     * Flow that represents the current vault state.
     */
    val vaultStateFlow: StateFlow<VaultState>

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
     * Locks the vault for the user with the given [userId] only if necessary.
     */
    fun lockVaultIfNecessary(userId: String)

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
}
