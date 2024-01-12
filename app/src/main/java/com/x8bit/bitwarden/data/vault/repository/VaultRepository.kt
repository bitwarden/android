package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.Kdf
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing vault data inside the network layer.
 */
@Suppress("TooManyFunctions")
interface VaultRepository : VaultLockManager {

    /**
     * Flow that represents the current vault data.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val vaultDataStateFlow: StateFlow<DataState<VaultData>>

    /**
     * Flow that represents all ciphers for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val ciphersStateFlow: StateFlow<DataState<List<CipherView>>>

    /**
     * Flow that represents all collections for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val collectionsStateFlow: StateFlow<DataState<List<CollectionView>>>

    /**
     * Flow that represents all folders for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val foldersStateFlow: StateFlow<DataState<List<FolderView>>>

    /**
     * Flow that represents the current send data.
     */
    val sendDataStateFlow: StateFlow<DataState<SendData>>

    /**
     * Flow that represents the totp code.
     */
    val totpCodeFlow: Flow<TotpCodeResult>

    /**
     * Clear any previously unlocked, in-memory data (vault, send, etc).
     */
    fun clearUnlockedData()

    /**
     * Completely remove any persisted data from the vault.
     */
    fun deleteVaultData(userId: String)

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
     * Flow that represents the data for a specific send as found by ID. This may emit `null` if
     * the send cannot be found.
     */
    fun getSendStateFlow(sendId: String): StateFlow<DataState<SendView?>>

    /**
     * Emits the totp code result flow to listeners.
     */
    fun emitTotpCodeResult(totpCodeResult: TotpCodeResult)

    /**
     * Attempt to unlock the vault and sync the vault data for the currently active user.
     */
    suspend fun unlockVaultAndSyncForCurrentUser(masterPassword: String): VaultUnlockResult

    /**
     * Attempt to unlock the vault with the specified user information.
     *
     * Note that when [organizationKeys] is absent, no attempt will be made to unlock the vault
     * for organization data.
     */
    @Suppress("LongParameterList")
    suspend fun unlockVault(
        userId: String,
        masterPassword: String,
        email: String,
        kdf: Kdf,
        userKey: String,
        privateKey: String,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult

    /**
     * Attempt to create a cipher.
     */
    suspend fun createCipher(cipherView: CipherView): CreateCipherResult

    /**
     * Attempt to update a cipher.
     */
    suspend fun updateCipher(
        cipherId: String,
        cipherView: CipherView,
    ): UpdateCipherResult

    /**
     * Attempt to create a send.
     */
    suspend fun createSend(sendView: SendView): CreateSendResult

    /**
     * Attempt to update a send.
     */
    suspend fun updateSend(
        sendId: String,
        sendView: SendView,
    ): UpdateSendResult

    /**
     * Attempt to remove the password from a send.
     */
    suspend fun removePasswordSend(sendId: String): RemovePasswordSendResult

    /**
     * Attempt to delete a send.
     */
    suspend fun deleteSend(sendId: String): DeleteSendResult
}
