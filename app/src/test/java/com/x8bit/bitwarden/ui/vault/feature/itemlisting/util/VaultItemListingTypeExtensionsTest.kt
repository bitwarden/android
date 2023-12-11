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
        )

        val result = itemListingTypeList.map { it.toItemListingType() }

        assertEquals(
            listOf(
                VaultItemListingState.ItemListingType.Folder(folderId = "mock"),
                VaultItemListingState.ItemListingType.Trash,
            ),
            result,
        )
    }
}
