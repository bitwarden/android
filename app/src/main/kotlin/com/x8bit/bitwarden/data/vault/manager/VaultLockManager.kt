package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.crypto.Kdf
import com.bitwarden.sdk.AuthClient
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.Flow
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
     * Flow that indicates whether the active user is actively unlocking the vault.
     */
    val isActiveUserUnlockingFlow: StateFlow<Boolean>

    /**
     * Flow that emits whenever any vault is locked or unlocked.
     */
    val vaultStateEventFlow: Flow<VaultStateEvent>

    /**
     * Whether the user is coming from the lock flow or not.
     */
    var isFromLockFlow: Boolean

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
    fun lockVault(userId: String, isUserInitiated: Boolean)

    /**
     * Locks the vault for the current user if currently unlocked.
     */
    fun lockVaultForCurrentUser(isUserInitiated: Boolean)

    /**
     * Attempt to unlock the vault with the specified user information.
     *
     * Note that when [organizationKeys] is absent, no attempt will be made to unlock the vault
     * for organization data.
     */
    @Suppress("LongParameterList")
    suspend fun unlockVault(
        accountCryptographicState: WrappedAccountCryptographicState,
        userId: String,
        email: String,
        kdf: Kdf,
        initUserCryptoMethod: InitUserCryptoMethod,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult

    /**
     * Suspends until the vault for the given [userId] is unlocked.
     */
    suspend fun waitUntilUnlocked(userId: String)

    /**
     * This will check the vault lock state for the given user and ensure that the
     * [vaultUnlockDataStateFlow] is up-to-date.
     *
     * This is only meant to be used when the SDK unlocks the vault as a side-effect of some other
     * function, such as [AuthClient.makeRegisterTdeKeys]. When using the regular [unlockVault]
     * functions, this is not necessary.
     */
    suspend fun syncVaultState(userId: String)
}
