package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Send
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult

/**
 * Source of vault information and functionality from the Bitwarden SDK.
 */
interface VaultSdkSource {

    /**
     * Attempts to initialize cryptography functionality for the Bitwarden SDK
     * with a given [InitCryptoRequest].
     */
    suspend fun initializeCrypto(request: InitUserCryptoRequest): Result<InitializeCryptoResult>

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
