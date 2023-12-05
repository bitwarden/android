package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.CipherView
import com.bitwarden.core.FolderView
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing vault data inside the network layer.
 */
interface VaultRepository {

    /**
     * Flow that represents the current vault data.
     */
    val vaultDataStateFlow: StateFlow<DataState<VaultData>>

    /**
     * Flow that represents the current vault state.
     */
    val vaultStateFlow: StateFlow<VaultState>

    /**
     * Flow that represents the current send data.
     */
    val sendDataStateFlow: StateFlow<DataState<SendData>>

    /**
     * Clear any previously unlocked, in-memory data (vault, send, etc).
     */
    fun clearUnlockedData()

    /**
     * Attempt to sync the vault data.
     */
    fun sync()

    /**
     * Flow that represents the data for a specific vault item as found by ID. This may emit `null`
     * if the item cannot be found.
     */
    fun getVaultItemStateFlow(itemId: String): StateFlow<DataState<CipherView?>>

    /**
     * Flow that represents the data for a specific vault folder as found by ID. This may emit
     * `null` if the folder cannot be found.
     */
    fun getVaultFolderStateFlow(folderId: String): StateFlow<DataState<FolderView?>>

    /**
     * Locks the vault for the current user if currently unlocked.
     */
    fun lockVaultForCurrentUser()

    /**
     * Locks the vault for the user with the given [userId] if necessary.
     */
    fun lockVaultIfNecessary(userId: String)

    /**
     * Attempt to unlock the vault and sync the vault data for the currently active user.
     */
    suspend fun unlockVaultAndSyncForCurrentUser(masterPassword: String): VaultUnlockResult

    /**
     * Attempt to unlock the vault with the specified user information.
     */
    @Suppress("LongParameterList")
    suspend fun unlockVault(
        userId: String,
        masterPassword: String,
        email: String,
        kdf: Kdf,
        userKey: String,
        privateKey: String,
        organizationalKeys: Map<String, String>,
    ): VaultUnlockResult

    /**
     * Attempt to create a cipher.
     */
    suspend fun createCipher(cipherView: CipherView): CreateCipherResult
}
