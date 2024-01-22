package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultItemListingStateExtensionsTest {

    @Test
    fun `toSearchType should return Texts when item type is SendText`() {
        val expected = SearchType.Sends.Texts
        val itemType = VaultItemListingState.ItemListingType.Send.SendText

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Files when item type is SendFile`() {
        val expected = SearchType.Sends.Files
        val itemType = VaultItemListingState.ItemListingType.Send.SendFile

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Logins when item type is Login`() {
        val expected = SearchType.Vault.Logins
        val itemType = VaultItemListingState.ItemListingType.Vault.Login

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Card when item type is Card`() {
        val expected = SearchType.Vault.Cards
        val itemType = VaultItemListingState.ItemListingType.Vault.Card

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Identities when item type is Identity`() {
        val expected = SearchType.Vault.Identities
        val itemType = VaultItemListingState.ItemListingType.Vault.Identity

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return SecureNotes when item type is SecureNote`() {
        val expected = SearchType.Vault.SecureNotes
        val itemType = VaultItemListingState.ItemListingType.Vault.SecureNote

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Trash when item type is Trash`() {
        val expected = SearchType.Vault.Trash
        val itemType = VaultItemListingState.ItemListingType.Vault.Trash

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Folder when item type is Folder with an ID`() {
        val folderId = "folderId"
        val expected = SearchType.Vault.Folder(folderId)
        val itemType = VaultItemListingState.ItemListingType.Vault.Folder(folderId)

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return NoFolder when item type is Folder without an ID`() {
        val expected = SearchType.Vault.NoFolder
        val itemType = VaultItemListingState.ItemListingType.Vault.Folder(null)

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSearchType should return Collection when item type is Collection`() {
        val collectionId = "collectionId"
        val expected = SearchType.Vault.Collection(collectionId)
        val itemType = VaultItemListingState.ItemListingType.Vault.Collection(collectionId)

        val result = itemType.toSearchType()

        assertEquals(expected, result)
    }
}
