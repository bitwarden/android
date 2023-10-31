package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.sdk.ClientVault

/**
 * Primary implementation of [VaultSdkSource] that serves as a convenience wrapper around a
 * [ClientVault].
 */
class VaultSdkSourceImpl(
    private val clientVault: ClientVault,
) : VaultSdkSource {

    override suspend fun decryptCipher(cipher: Cipher): Result<CipherView> =
        runCatching { clientVault.ciphers().decrypt(cipher) }

    override suspend fun decryptCipherList(cipherList: List<Cipher>): Result<List<CipherListView>> =
        runCatching { clientVault.ciphers().decryptList(cipherList) }

    override suspend fun decryptFolder(folder: Folder): Result<FolderView> =
        runCatching { clientVault.folders().decrypt(folder) }

    override suspend fun decryptFolderList(folderList: List<Folder>): Result<List<FolderView>> =
        runCatching { clientVault.folders().decryptList(folderList) }
}
