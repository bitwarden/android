package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultItemListingTypeExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toItemListingType should transform a VaultItemListingType into a VaultItemListingState ItemListingType`() {
        val itemListingTypeList = listOf(
            VaultItemListingType.Folder(folderId = "mock"),
            VaultItemListingType.Trash,
            VaultItemListingType.Collection(collectionId = "collectionId"),
            VaultItemListingType.SshKey,
            VaultItemListingType.SendFile,
            VaultItemListingType.SendText,
            VaultItemListingType.Card,
            VaultItemListingType.Identity,
            VaultItemListingType.Login,
            VaultItemListingType.SecureNote,
        )

        val result = itemListingTypeList.map { it.toItemListingType() }

        assertEquals(
            listOf(
                VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mock"),
                VaultItemListingState.ItemListingType.Vault.Trash,
                VaultItemListingState.ItemListingType.Vault.Collection(
                    collectionId = "collectionId",
                ),
                VaultItemListingState.ItemListingType.Vault.SshKey,
                VaultItemListingState.ItemListingType.Send.SendFile,
                VaultItemListingState.ItemListingType.Send.SendText,
                VaultItemListingState.ItemListingType.Vault.Card,
                VaultItemListingState.ItemListingType.Vault.Identity,
                VaultItemListingState.ItemListingType.Vault.Login,
                VaultItemListingState.ItemListingType.Vault.SecureNote,
            ),
            result,
        )
    }
}
