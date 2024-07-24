package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.datasource.network.model.FolderJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFolder
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSdkFolderExtensionsTest {

    @Test
    fun `toEncryptedSdkFolderList should convert list of NetworkFolder to List of SdkFolder`() {
        val syncFolders = listOf(
            createMockFolder(number = 1),
            createMockFolder(number = 2),
        )
        val sdkFolders = syncFolders.toEncryptedSdkFolderList()
        assertEquals(
            listOf(
                createMockSdkFolder(number = 1),
                createMockSdkFolder(number = 2),
            ),
            sdkFolders,
        )
    }

    @Test
    fun `toEncryptedSdkFolder should convert a NetworkFolder to a SdkFolder`() {
        val syncFolder = createMockFolder(number = 1)
        val sdkFolder = syncFolder.toEncryptedSdkFolder()
        assertEquals(
            createMockSdkFolder(number = 1),
            sdkFolder,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSortAlphabetically should sort folders by name`() {
        val list = listOf(
            createMockFolderView(1).copy(name = "c"),
            createMockFolderView(1).copy(name = "D"),
            createMockFolderView(1).copy(name = "_"),
            createMockFolderView(1).copy(name = "4"),
            createMockFolderView(1).copy(name = "B"),
            createMockFolderView(1).copy(name = "A"),
            createMockFolderView(1).copy(name = "z"),
        )

        val expected = listOf(
            createMockFolderView(1).copy(name = "_"),
            createMockFolderView(1).copy(name = "4"),
            createMockFolderView(1).copy(name = "A"),
            createMockFolderView(1).copy(name = "B"),
            createMockFolderView(1).copy(name = "c"),
            createMockFolderView(1).copy(name = "D"),
            createMockFolderView(1).copy(name = "z"),
        )

        assertEquals(
            expected,
            list.sortAlphabetically(),
        )
    }

    @Test
    fun `toEncryptedNetworkFolder should convert a SdkFolder to a NetworkFolder`() {
        val sdkFolder = createMockSdkFolder(number = 1)
        val syncFolder = sdkFolder.toEncryptedNetworkFolder()
        assertEquals(
            FolderJsonRequest(sdkFolder.name),
            syncFolder,
        )
    }
}
