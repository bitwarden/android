package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.core.CipherListView
import com.bitwarden.core.FolderView

/**
 * Represents decrypted vault data.
 *
 * @param cipherListViewList List of decrypted ciphers.
 * @param folderViewList List of decrypted folders.
 */
data class VaultData(
    val cipherListViewList: List<CipherListView>,
    val folderViewList: List<FolderView>,
)
