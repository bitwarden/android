package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.FolderManager
import com.x8bit.bitwarden.data.vault.manager.SendManager
import com.x8bit.bitwarden.data.vault.manager.VaultDataManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.Flow
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
    VaultDataManager,
    VaultLockManager,
    VaultSyncManager {

    /**
     * Flow that represents the totp code.
     */
    val totpCodeFlow: Flow<TotpCodeResult>

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
     * Returns the IDs of ciphers with a valid TOTP secret for the active user. The vault filter
     * is intentionally not applied here; callers intersect the result with their already-filtered
     * cipher list so the count stays correct when the filter changes. For non-premium users only
     * org-TOTP ciphers are included.
     */
    suspend fun getValidTotpCipherIds(isPremium: Boolean, time: Instant): Set<String>

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
}
