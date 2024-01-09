package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Collection
import com.bitwarden.core.CollectionView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.PasswordHistory
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
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
     * Gets the user's encryption key, which can be used to later unlock their vault via a call to
     * [initializeCrypto] with [InitUserCryptoMethod.DecryptedKey].
     *
     * This should only be called after a successful call to [initializeCrypto] for the associated
     * user.
     */
    suspend fun getUserEncryptionKey(userId: String): Result<String>

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
}
