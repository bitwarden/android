package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitCryptoRequest
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.ClientCrypto
import com.bitwarden.sdk.ClientVault
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult

/**
 * Primary implementation of [VaultSdkSource] that serves as a convenience wrapper around a
 * [ClientVault].
 */
class VaultSdkSourceImpl(
    private val clientVault: ClientVault,
    private val clientCrypto: ClientCrypto,
) : VaultSdkSource {
    override suspend fun initializeCrypto(
        request: InitCryptoRequest,
    ): Result<InitializeCryptoResult> =
        runCatching {
            try {
                clientCrypto.initializeCrypto(req = request)
                InitializeCryptoResult.Success
            } catch (exception: BitwardenException) {
                // The only truly expected error from the SDK is an incorrect password.
                InitializeCryptoResult.AuthenticationError
            }
        }

    override suspend fun decryptCipher(cipher: Cipher): Result<CipherView> =
        runCatching { clientVault.ciphers().decrypt(cipher) }

    override suspend fun decryptCipherList(cipherList: List<Cipher>): Result<List<CipherListView>> =
        runCatching { clientVault.ciphers().decryptList(cipherList) }

    override suspend fun decryptFolder(folder: Folder): Result<FolderView> =
        runCatching { clientVault.folders().decrypt(folder) }

    override suspend fun decryptFolderList(folderList: List<Folder>): Result<List<FolderView>> =
        runCatching { clientVault.folders().decryptList(folderList) }
}
