package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import kotlinx.coroutines.flow.StateFlow

/**
 * A helper manager for accessing and manging the vault data.
 */
interface VaultDataManager {
    /**
     * The [VaultFilterType] for the current user.
     *
     * Note that this does not affect the data provided by the repository and can be used by
     * the UI for consistent filtering across screens.
     */
    var vaultFilterType: VaultFilterType

    /**
     * Flow that represents the data for a single verification code item.
     * This may emit null if any issues arise during code generation.
     */
    fun getAuthCodeFlow(cipherId: String): StateFlow<DataState<VerificationCodeItem?>>

    /**
     * Flow that represents the data for the TOTP verification codes for ciphers items.
     * This may emit an empty list if any issues arise during code generation.
     */
    fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>>

    /**
     * Completely remove any persisted data from the vault.
     */
    fun deleteVaultData(userId: String)

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
     * the Send cannot be found.
     */
    fun getSendStateFlow(sendId: String): StateFlow<DataState<SendView?>>

    /**
     * Flow that represents the data for a specific vault list item as found by ID. This may emit
     * `null` if the item cannot be found.
     */
    fun getVaultListItemStateFlow(itemId: String): StateFlow<DataState<CipherListView?>>
}
