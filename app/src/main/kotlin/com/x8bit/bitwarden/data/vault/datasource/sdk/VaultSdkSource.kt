package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.collections.Collection
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.EnrollPinResponse
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.UpdateKdfResponse
import com.bitwarden.core.UpdatePasswordResponse
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.TrustDeviceResponse
import com.bitwarden.exporters.Account
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.Send
import com.bitwarden.send.SendView
import com.bitwarden.vault.Attachment
import com.bitwarden.vault.AttachmentEncryptResult
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.EncryptionContext
import com.bitwarden.vault.Folder
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.PasswordHistory
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.DeriveKeyConnectorResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import java.io.File
import java.time.Instant

/**
 * Source of vault information and functionality from the Bitwarden SDK.
 */
@Suppress("TooManyFunctions")
interface VaultSdkSource {

    /**
     * Clears any cryptography-related functionality for the given [userId], effectively locking
     * the associated vault.
     */
    fun clearCrypto(userId: String)

    /**
     * Gets the data to authenticate with trusted device encryption.
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun getTrustDevice(userId: String): Result<TrustDeviceResponse>

    /**
     * Derives a "key connector" key from the given information for the given `userId. This can be
     * used to later unlock their vault via a call to [initializeCrypto] with
     * [InitUserCryptoMethod.KeyConnector].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun deriveKeyConnector(
        userId: String,
        userKeyEncrypted: String,
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<DeriveKeyConnectorResult>

    /**
     * Protects the current user key with the provided PIN. This can be used to later unlock
     * their vault via a call to [initializeCrypto] with [InitUserCryptoMethod.PinEnvelope].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun enrollPin(
        userId: String,
        pin: String,
    ): Result<EnrollPinResponse>

    /**
     * Protects the current user key with the provided PIN. The result can be stored and later
     * used to initialize another client instance by using the PIN and the PIN key with
     * [initializeCrypto]. The provided pin is encrypted with the user key.
     */
    suspend fun enrollPinWithEncryptedPin(
        userId: String,
        encryptedPin: String,
    ): Result<EnrollPinResponse>

    /**
     * Validate the user pin using the [pinProtectedUserKey].
     */
    suspend fun validatePin(
        userId: String,
        pin: String,
        pinProtectedUserKey: String,
    ): Result<Boolean>

    /**
     * Gets the key for an auth request that is required to approve or decline it.
     */
    suspend fun getAuthRequestKey(
        publicKey: String,
        userId: String,
    ): Result<String>

    /**
     * Get the reset password key for this [orgPublicKey] and [userId].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun getResetPasswordKey(
        orgPublicKey: String,
        userId: String,
    ): Result<String>

    /**
     * Gets the user's encryption key, which can be used to later unlock their vault via a call to
     * [initializeCrypto] with [InitUserCryptoMethod.DecryptedKey].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun getUserEncryptionKey(userId: String): Result<String>

    /**
     * Gets the user's fingerprint.
     */
    suspend fun getUserFingerprint(userId: String): Result<String>

    /**
     * Attempts to initialize cryptography functionality for an individual user with the given
     * [userId] for the Bitwarden SDK with a given [InitUserCryptoRequest].
     */
    suspend fun initializeCrypto(
        userId: String,
        request: InitUserCryptoRequest,
    ): Result<InitializeCryptoResult>

    /**
     * Attempts to initialize cryptography functionality for organization data associated with
     * the user with the given [userId] for the Bitwarden SDK with a given [InitOrgCryptoRequest].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun initializeOrganizationCrypto(
        userId: String,
        request: InitOrgCryptoRequest,
    ): Result<InitializeCryptoResult>

    /**
     * Encrypts a [AttachmentView] for the user with the given [userId], returning an
     * [AttachmentEncryptResult] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptAttachment(
        userId: String,
        cipher: Cipher,
        attachmentView: AttachmentView,
        decryptedFilePath: String,
        encryptedFilePath: String,
    ): Result<Attachment>

    /**
     * Encrypts a [CipherView] for the user with the given [userId], returning a [Cipher] wrapped
     * in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     *
     * Note that this function will always add a [CipherView.key] to a cipher if it is missing,
     * it is important to ensure that any [CipherView] being encrypted is pushed to the cloud if
     * it was previously missing a `key` to ensure synchronization and prevent data-loss.
     */
    suspend fun encryptCipher(
        userId: String,
        cipherView: CipherView,
    ): Result<EncryptionContext>

    /**
     * Decrypts a [Cipher] for the user with the given [userId], returning a [CipherView] wrapped
     * in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptCipher(
        userId: String,
        cipher: Cipher,
    ): Result<CipherView>

    /**
     * Decrypts a list of [Cipher]s for the user with the given [userId].
     *
     * @return A [DecryptCipherListResult] containing the decrypted [CipherListView]s and references
     * to [Cipher]s that cannot be decrypted.
     */
    suspend fun decryptCipherListWithFailures(
        userId: String,
        cipherList: List<Cipher>,
    ): Result<DecryptCipherListResult>

    /**
     * Decrypts a [Collection] for the user with the given [userId], returning a [CollectionView]
     * wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptCollection(
        userId: String,
        collection: Collection,
    ): Result<CollectionView>

    /**
     * Decrypts a list of [Collection]s for the user with the given [userId], returning a list of
     * [CollectionView] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptCollectionList(
        userId: String,
        collectionList: List<Collection>,
    ): Result<List<CollectionView>>

    /**
     * Encrypts a [SendView] for the user with the given [userId], returning a [Send] wrapped
     * in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptSend(
        userId: String,
        sendView: SendView,
    ): Result<Send>

    /**
     * Encrypts a [ByteArray] file buffer for the user with the given [userId], returning an
     * encrypted [ByteArray] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptBuffer(
        userId: String,
        send: Send,
        fileBuffer: ByteArray,
    ): Result<ByteArray>

    /**
     * Encrypts a file at [path] for the user with the given [userId], returning the
     * encrypted [File] as a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptFile(
        userId: String,
        send: Send,
        path: String,
        destinationFilePath: String,
    ): Result<File>

    /**
     * Decrypts a [Send] for the user with the given [userId], returning a [SendView] wrapped in a
     * [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptSend(
        userId: String,
        send: Send,
    ): Result<SendView>

    /**
     * Decrypts a list of [Send]s for the user with the given [userId], returning a list of
     * [SendView] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptSendList(
        userId: String,
        sendList: List<Send>,
    ): Result<List<SendView>>

    /**
     * Encrypts a [FolderView] for the user with the given [userId], returning a [Folder] wrapped
     * in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptFolder(
        userId: String,
        folder: FolderView,
    ): Result<Folder>

    /**
     * Decrypts a [Folder] for the user with the given [userId], returning a [FolderView] wrapped
     * in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptFolder(
        userId: String,
        folder: Folder,
    ): Result<FolderView>

    /**
     * Decrypts a list of [Folder]s for the user with the given [userId], returning a list of
     * [FolderView] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptFolderList(
        userId: String,
        folderList: List<Folder>,
    ): Result<List<FolderView>>

    /**
     * Decrypts a [cipher] [attachmentView] file found at [encryptedFilePath] saving it at
     * [decryptedFilePath] for the user with the given [userId]
     */
    suspend fun decryptFile(
        userId: String,
        cipher: Cipher,
        attachmentView: AttachmentView,
        encryptedFilePath: String,
        decryptedFilePath: String,
    ): Result<Unit>

    /**
     * Encrypts a given password history item for the user with the given [userId].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptPasswordHistory(
        userId: String,
        passwordHistory: PasswordHistoryView,
    ): Result<PasswordHistory>

    /**
     * Decrypts a list of password history items for the user with the given [userId].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptPasswordHistoryList(
        userId: String,
        passwordHistoryList: List<PasswordHistory>,
    ): Result<List<PasswordHistoryView>>

    /**
     * Generate a verification code for the given [cipherListView] and [time].
     */
    suspend fun generateTotpForCipherListView(
        userId: String,
        cipherListView: CipherListView,
        time: Instant?,
    ): Result<TotpResponse>

    /**
     * Re-encrypts the [cipherView] with the organizations encryption key.
     */
    suspend fun moveToOrganization(
        userId: String,
        organizationId: String,
        cipherView: CipherView,
    ): Result<CipherView>

    /**
     * Validates that the given password matches the password hash.
     */
    suspend fun validatePassword(
        userId: String,
        password: String,
        passwordHash: String,
    ): Result<Boolean>

    /**
     * Validates that the given password with the encrypted user key and returns the master
     * password hash on validation or an error on failure.
     */
    suspend fun validatePasswordUserKey(
        userId: String,
        password: String,
        encryptedUserKey: String,
    ): Result<String>

    /**
     * Get the keys needed to update the user's password.
     */
    suspend fun updatePassword(
        userId: String,
        newPassword: String,
    ): Result<UpdatePasswordResponse>

    /**
     * Exports the users vault data and returns it as a string in the selected format
     * (JSON, CSV, encrypted JSON).
     */
    suspend fun exportVaultDataToString(
        userId: String,
        folders: List<Folder>,
        ciphers: List<Cipher>,
        format: ExportFormat,
    ): Result<String>

    /**
     * Exports the users vault data to a CXF formatted string.
     */
    suspend fun exportVaultDataToCxf(
        userId: String,
        account: Account,
        ciphers: List<Cipher>,
    ): Result<String>

    /**
     * Imports the given CXF formatted [payload] into the users vault.
     *
     * @return Result of the import. If successful, a list of [Cipher]s deciphered from the CXF
     * payload.
     */
    suspend fun importCxf(userId: String, payload: String): Result<List<Cipher>>

    /**
     * Register a new FIDO 2 credential to a cipher.
     *
     * @return Result of the FIDO 2 credential registration. If successful, a
     * [PublicKeyCredentialAuthenticatorAttestationResponse] is provided.
     */
    suspend fun registerFido2Credential(
        request: RegisterFido2CredentialRequest,
        fido2CredentialStore: Fido2CredentialStore,
    ): Result<PublicKeyCredentialAuthenticatorAttestationResponse>

    /**
     * Authenticate a user with a FIDO 2 credential.
     *
     * @return Result of the FIDO 2 credential registration. If successful, a
     * [PublicKeyCredentialAuthenticatorAttestationResponse] is provided.
     */
    @Suppress("LongParameterList")
    suspend fun authenticateFido2Credential(
        request: AuthenticateFido2CredentialRequest,
        fido2CredentialStore: Fido2CredentialStore,
    ): Result<PublicKeyCredentialAuthenticatorAssertionResponse>

    /**
     * Decrypt a list of FIDO 2 credential autofill view items associated with the given
     * [cipherViews].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptFido2CredentialAutofillViews(
        userId: String,
        vararg cipherViews: CipherView,
    ): Result<List<Fido2CredentialAutofillView>>

    /**
     * Silently discovers FIDO 2 credentials for a given [userId] and [relyingPartyId].
     *
     * @return A list of FIDO 2 credentials.
     */
    suspend fun silentlyDiscoverCredentials(
        userId: String,
        fido2CredentialStore: Fido2CredentialStore,
        relyingPartyId: String,
        userHandle: String?,
    ): Result<List<Fido2CredentialAutofillView>>

    /**
     * Updates the KDF settings for the user with the given [userId].
     */
    suspend fun makeUpdateKdf(
        userId: String,
        password: String,
        kdf: Kdf,
    ): Result<UpdateKdfResponse>
}
