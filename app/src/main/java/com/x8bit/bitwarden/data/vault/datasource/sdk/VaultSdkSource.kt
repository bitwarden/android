package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Collection
import com.bitwarden.core.CollectionView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult

/**
 * Source of vault information and functionality from the Bitwarden SDK.
 */
@Suppress("TooManyFunctions")
interface VaultSdkSource {

    /**
     * Attempts to initialize cryptography functionality for an individual user for the
     * Bitwarden SDK with a given [InitUserCryptoRequest].
     */
    suspend fun initializeCrypto(request: InitUserCryptoRequest): Result<InitializeCryptoResult>

    /**
     * Attempts to initialize cryptography functionality for organization data associated with
     * the current user for the Bitwarden SDK with a given [InitOrgCryptoRequest].
     *
     * This should only be called after a successful call to [initializeCrypto].
     */
    suspend fun initializeOrganizationCrypto(
        request: InitOrgCryptoRequest,
    ): Result<InitializeCryptoResult>

    /**
     * Encrypts a [CipherView] returning a [Cipher] wrapped in a [Result].
     */
    suspend fun encryptCipher(cipherView: CipherView): Result<Cipher>

    /**
     * Decrypts a [Cipher] returning a [CipherView] wrapped in a [Result].
     */
    suspend fun decryptCipher(cipher: Cipher): Result<CipherView>

    /**
     * Decrypts a list of [Cipher]s returning a list of [CipherListView] wrapped in a [Result].
     */
    suspend fun decryptCipherListCollection(cipherList: List<Cipher>): Result<List<CipherListView>>

    /**
     * Decrypts a list of [Cipher]s returning a list of [CipherView] wrapped in a [Result].
     */
    suspend fun decryptCipherList(cipherList: List<Cipher>): Result<List<CipherView>>

    /**
     * Decrypts a [Collection] returning a [CollectionView] wrapped in a [Result].
     */
    suspend fun decryptCollection(collection: Collection): Result<CollectionView>

    /**
     * Decrypts a list of [Collection]s returning a list of [CollectionView] wrapped in a [Result].
     */
    suspend fun decryptCollectionList(
        collectionList: List<Collection>,
    ): Result<List<CollectionView>>

    /**
     * Decrypts a [Send] returning a [SendView] wrapped in a [Result].
     */
    suspend fun decryptSend(send: Send): Result<SendView>

    /**
     * Decrypts a list of [Send]s returning a list of [SendView] wrapped in a [Result].
     */
    suspend fun decryptSendList(sendList: List<Send>): Result<List<SendView>>

    /**
     * Decrypts a [Folder] returning a [FolderView] wrapped in a [Result].
     */
    suspend fun decryptFolder(folder: Folder): Result<FolderView>

    /**
     * Decrypts a list of [Folder]s returning a list of [FolderView] wrapped in a [Result].
     */
    suspend fun decryptFolderList(folderList: List<Folder>): Result<List<FolderView>>
}
