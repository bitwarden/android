package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

/**
 * Transforms a [VaultItemListingType] into a [VaultItemListingState.ItemListingType].
 */
fun VaultItemListingType.toItemListingType(): VaultItemListingState.ItemListingType =
    when (this) {
        is VaultItemListingType.Card -> VaultItemListingState.ItemListingType.Vault.Card
        is VaultItemListingType.Folder -> {
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = folderId)
        }

        is VaultItemListingType.Identity -> VaultItemListingState.ItemListingType.Vault.Identity
        is VaultItemListingType.Login -> VaultItemListingState.ItemListingType.Vault.Login
        is VaultItemListingType.SecureNote -> VaultItemListingState.ItemListingType.Vault.SecureNote
        is VaultItemListingType.SshKey -> VaultItemListingState.ItemListingType.Vault.SshKey
        is VaultItemListingType.Trash -> VaultItemListingState.ItemListingType.Vault.Trash
        is VaultItemListingType.Collection -> {
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = collectionId)
        }

        is VaultItemListingType.SendFile -> VaultItemListingState.ItemListingType.Send.SendFile
        is VaultItemListingType.SendText -> VaultItemListingState.ItemListingType.Send.SendText
    }
