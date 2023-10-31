package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockFolder
import com.x8bit.bitwarden.data.vault.datasource.sdk.createMockSdkFolder
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
}
