package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Transforms a [VaultItemListingState.ItemListingType] into a [SearchType].
 */
fun VaultItemListingState.ItemListingType.toSearchType(): SearchType =
    when (this) {
        is VaultItemListingState.ItemListingType.Vault.Card -> SearchType.Vault.Cards
        is VaultItemListingState.ItemListingType.Vault.Folder -> {
            folderId
                ?.let { SearchType.Vault.Folder(folderId = it) }
                ?: SearchType.Vault.NoFolder
        }

        is VaultItemListingState.ItemListingType.Vault.Identity -> SearchType.Vault.Identities
        is VaultItemListingState.ItemListingType.Vault.Login -> SearchType.Vault.Logins
        is VaultItemListingState.ItemListingType.Vault.SecureNote -> SearchType.Vault.SecureNotes
        is VaultItemListingState.ItemListingType.Vault.SshKey -> SearchType.Vault.SshKeys
        is VaultItemListingState.ItemListingType.Vault.Trash -> SearchType.Vault.Trash
        is VaultItemListingState.ItemListingType.Vault.Collection -> {
            SearchType.Vault.Collection(collectionId = collectionId)
        }

        is VaultItemListingState.ItemListingType.Send.SendFile -> SearchType.Sends.Files
        is VaultItemListingState.ItemListingType.Send.SendText -> SearchType.Sends.Texts
    }

/**
 * Transforms a [VaultItemListingState.ItemListingType.Vault] into a [VaultItemCipherType].
 */
fun VaultItemListingState.ItemListingType.Vault.toVaultItemCipherType(): VaultItemCipherType =
    when (this) {
        is VaultItemListingState.ItemListingType.Vault.Card -> VaultItemCipherType.CARD
        is VaultItemListingState.ItemListingType.Vault.Identity -> VaultItemCipherType.IDENTITY
        is VaultItemListingState.ItemListingType.Vault.SecureNote -> VaultItemCipherType.SECURE_NOTE
        is VaultItemListingState.ItemListingType.Vault.SshKey -> VaultItemCipherType.SSH_KEY
        is VaultItemListingState.ItemListingType.Vault.Login -> VaultItemCipherType.LOGIN
        is VaultItemListingState.ItemListingType.Vault.Collection -> VaultItemCipherType.LOGIN
        is VaultItemListingState.ItemListingType.Vault.Folder -> VaultItemCipherType.LOGIN
        is VaultItemListingState.ItemListingType.Vault.Trash,
            -> {
            throw IllegalStateException(
                "Cannot create vault item from this VaultItemListingState!",
            )
        }
    }
