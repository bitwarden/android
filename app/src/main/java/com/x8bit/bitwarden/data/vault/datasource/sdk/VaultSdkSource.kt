package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.AttachmentEncryptResult
import com.bitwarden.core.AttachmentView
import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Collection
import com.bitwarden.core.CollectionView
import com.bitwarden.core.DateTime
import com.bitwarden.core.DerivePinKeyResponse
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.PasswordHistory
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
import com.bitwarden.core.TotpResponse
import com.bitwarden.core.UpdatePasswordResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult

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
     * Derives a "pin key" from the given [pin] for the given [userId]. This can be used to later
     * unlock their vault via a call to [initializeCrypto] with [InitUserCryptoMethod.Pin].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun derivePinKey(
        userId: String,
        pin: String,
    ): Result<DerivePinKeyResponse>

    /**
     * Derives a pin-protected user key from the given [encryptedPin] for the given [userId]. This
     * value must be derived from a previous call to [derivePinKey] with a plaintext PIN. This can
     * be used to later unlock their vault via a call to [initializeCrypto] with
     * [InitUserCryptoMethod.Pin].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun derivePinProtectedUserKey(
        userId: String,
        encryptedPin: String,
    ): Result<String>

    /**
     * Gets the key for an auth request that is required to approve or decline it.
     */
    suspend fun getAuthRequestKey(
        publicKey: String,
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
        fileBuffer: ByteArray,
    ): Result<AttachmentEncryptResult>

    /**
     * Encrypts a [CipherView] for the user with the given [userId], returning a [Cipher] wrapped
     * in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun encryptCipher(
        userId: String,
        cipherView: CipherView,
    ): Result<Cipher>

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
     * Decrypts a list of [Cipher]s for the user with the given [userId], returning a list of
     * [CipherListView] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptCipherListCollection(
        userId: String,
        cipherList: List<Cipher>,
    ): Result<List<CipherListView>>

    /**
     * Decrypts a list of [Cipher]s  for the user with the given [userId], returning a list of
     * [CipherView] wrapped in a [Result].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun decryptCipherList(
        userId: String,
        cipherList: List<Cipher>,
    ): Result<List<CipherView>>

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
     * Generate a verification code and the period using the totp code.
     */
    suspend fun generateTotp(
        userId: String,
        totp: String,
        time: DateTime,
    ): Result<TotpResponse>

    /**
     * Validates that the given password matches the password hash.
     */
    suspend fun validatePassword(
        userId: String,
        password: String,
        passwordHash: String,
    ): Result<Boolean>

    /**
     * Get the keys needed to update the user's password.
     */
    suspend fun updatePassword(
        userId: String,
        newPassword: String,
    ): Result<UpdatePasswordResponse>
}
