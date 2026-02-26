package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.FolderManager
import com.x8bit.bitwarden.data.vault.manager.SendManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.crypto.Cipher

/**
 * Responsible for managing vault data inside the network layer.
 */
@Suppress("TooManyFunctions")
interface VaultRepository :
    CipherManager,
    FolderManager,
    SendManager,
    VaultLockManager,
    VaultSyncManager {

    /**
     * The [VaultFilterType] for the current user.
     *
     * Note that this does not affect the data provided by the repository and can be used by
     * the UI for consistent filtering across screens.
     */
    var vaultFilterType: VaultFilterType

    /**
     * Flow that represents the totp code.
     */
    val totpCodeFlow: Flow<TotpCodeResult>

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
     * the send cannot be found.
     */
    fun getSendStateFlow(sendId: String): StateFlow<DataState<SendView?>>

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
     * Silently discovers FIDO 2 credentials for a given [userId] and [relyingPartyId].
     */
    suspend fun silentlyDiscoverCredentials(
        userId: String,
        fido2CredentialStore: Fido2CredentialStore,
        relyingPartyId: String,
        userHandle: String?,
    ): Result<List<Fido2CredentialAutofillView>>

    /**
     * Emits the totp code result flow to listeners.
     */
    fun emitTotpCodeResult(totpCodeResult: TotpCodeResult)

    /**
     * Attempt to unlock the vault using a user unlock key.
     *
     * @param userId ID of the user's vault to unlock.
     * @param decryptedUserKey A decrypted unlock key for the user (ex: their authenticator
     * sync unlock key)
     */
    suspend fun unlockVaultWithDecryptedUserKey(
        userId: String,
        decryptedUserKey: String,
    ): VaultUnlockResult

    /**
     * Attempt to unlock the vault using the stored biometric key for the currently active user.
     */
    suspend fun unlockVaultWithBiometrics(cipher: Cipher): VaultUnlockResult

    /**
     * Attempt to unlock the vault with the given [masterPassword] and for the currently active
     * user.
     */
    suspend fun unlockVaultWithMasterPassword(
        masterPassword: String,
    ): VaultUnlockResult

    /**
     * Attempt to unlock the vault with the given [pin] for the currently active user.
     */
    suspend fun unlockVaultWithPin(
        pin: String,
    ): VaultUnlockResult

    /**
     * Attempt to get the verification code and the period.
     */
    suspend fun generateTotp(cipherId: String, time: Instant): GenerateTotpResult

    /**
     * Attempt to get the user's vault data for export.
     *
     * @param format The export format to use.
     * @param restrictedTypes A list of restricted types to export.
     */
    suspend fun exportVaultDataToString(
        format: ExportFormat,
        restrictedTypes: List<CipherType>,
    ): ExportVaultDataResult

    /**
     * Attempt to import a CXF payload.
     *
     * @param payload The CXF payload to import.
     */
    suspend fun importCxfPayload(payload: String): ImportCredentialsResult

    /**
     * Attempt to export the vault data to a CXF file.
     *
     * @param ciphers Ciphers selected for export.
     */
    suspend fun exportVaultDataToCxf(ciphers: List<CipherListView>): Result<String>

    /**
     * Flow that represents the data for a specific vault list item as found by ID. This may emit
     * `null` if the item cannot be found.
     */
    fun getVaultListItemStateFlow(itemId: String): StateFlow<DataState<CipherListView?>>
}
