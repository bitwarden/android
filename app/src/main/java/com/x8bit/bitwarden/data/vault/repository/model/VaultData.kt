package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.FolderView

/**
 * Represents decrypted vault data.
 *
 * @param cipherViewList List of decrypted ciphers.
 * @param collectionViewList List of decrypted collections.
 * @param folderViewList List of decrypted folders.
 * @param sendViewList List of decrypted sends.
 * @param fido2CredentialAutofillViewList List of decrypted fido 2 credentials.
 */
data class VaultData(
    val cipherViewList: List<CipherView>,
    val collectionViewList: List<CollectionView>,
    val folderViewList: List<FolderView>,
    val sendViewList: List<SendView>,
    val fido2CredentialAutofillViewList: List<Fido2CredentialAutofillView>? = null,
)
