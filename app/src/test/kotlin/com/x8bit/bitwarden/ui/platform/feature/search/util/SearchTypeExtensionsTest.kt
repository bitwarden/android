package com.x8bit.bitwarden.ui.platform.feature.search.util

import com.x8bit.bitwarden.ui.platform.feature.search.SearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SearchTypeExtensionsTest {

    @Test
    fun `toSearchTypeData should return Sends All when SearchType is Sends All`() {
        assertEquals(SearchTypeData.Sends.All, SearchType.Sends.All.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Sends Files when SearchType is Sends Files`() {
        assertEquals(SearchTypeData.Sends.Files, SearchType.Sends.Files.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Sends Texts when SearchType is Sends Texts`() {
        assertEquals(SearchTypeData.Sends.Texts, SearchType.Sends.Texts.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Vault All when SearchType is Vault All`() {
        assertEquals(SearchTypeData.Vault.All, SearchType.Vault.All.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Vault Archive when SearchType is Vault Archive`() {
        assertEquals(SearchTypeData.Vault.Archive, SearchType.Vault.Archive.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Vault Cards when SearchType is Vault Cards`() {
        assertEquals(SearchTypeData.Vault.Cards, SearchType.Vault.Cards.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Vault Collection when SearchType is Vault Collection`() {
        val collectionId = "collectionId"
        assertEquals(
            SearchTypeData.Vault.Collection(collectionId),
            SearchType.Vault.Collection(collectionId).toSearchTypeData(),
        )
    }

    @Test
    fun `toSearchTypeData should return Vault Folder when SearchType is Vault Folder`() {
        val folderId = "folderId"
        assertEquals(
            SearchTypeData.Vault.Folder(folderId),
            SearchType.Vault.Folder(folderId).toSearchTypeData(),
        )
    }

    @Test
    fun `toSearchTypeData should return Vault Identities when SearchType is Vault Identities`() {
        assertEquals(
            SearchTypeData.Vault.Identities,
            SearchType.Vault.Identities.toSearchTypeData(),
        )
    }

    @Test
    fun `toSearchTypeData should return Vault Logins when SearchType is Vault Logins`() {
        assertEquals(SearchTypeData.Vault.Logins, SearchType.Vault.Logins.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Vault NoFolder when SearchType is Vault NoFolder`() {
        assertEquals(SearchTypeData.Vault.NoFolder, SearchType.Vault.NoFolder.toSearchTypeData())
    }

    @Test
    fun `toSearchTypeData should return Vault SecureNotes when SearchType is Vault SecureNotes`() {
        assertEquals(
            SearchTypeData.Vault.SecureNotes,
            SearchType.Vault.SecureNotes.toSearchTypeData(),
        )
    }

    @Test
    fun `toSearchTypeData should return Vault Trash when SearchType is Vault Trash`() {
        assertEquals(SearchTypeData.Vault.Trash, SearchType.Vault.Trash.toSearchTypeData())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSearchTypeData should return Vault VerificationCodes when SearchType is Vault VerificationCodes`() {
        assertEquals(
            SearchTypeData.Vault.VerificationCodes,
            SearchType.Vault.VerificationCodes.toSearchTypeData(),
        )
    }

    @Test
    fun `toSearchTypeData should return Vault SshKeys when SearchType is Vault SshKeys`() {
        assertEquals(SearchTypeData.Vault.SshKeys, SearchType.Vault.SshKeys.toSearchTypeData())
    }
}
