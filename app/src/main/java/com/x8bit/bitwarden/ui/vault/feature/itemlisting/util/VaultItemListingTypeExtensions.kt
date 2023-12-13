package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

/**
 * Transforms a [VaultItemListingType] into a [VaultItemListingState.ItemListingType].
 */
fun VaultItemListingType.toItemListingType(): VaultItemListingState.ItemListingType =
    when (this) {
        is VaultItemListingType.Card -> VaultItemListingState.ItemListingType.Card
        is VaultItemListingType.Folder -> {
            VaultItemListingState.ItemListingType.Folder(folderId = folderId)
        }

        is VaultItemListingType.Identity -> VaultItemListingState.ItemListingType.Identity
        is VaultItemListingType.Login -> VaultItemListingState.ItemListingType.Login
        is VaultItemListingType.SecureNote -> VaultItemListingState.ItemListingType.SecureNote
        is VaultItemListingType.Trash -> VaultItemListingState.ItemListingType.Trash
    }
