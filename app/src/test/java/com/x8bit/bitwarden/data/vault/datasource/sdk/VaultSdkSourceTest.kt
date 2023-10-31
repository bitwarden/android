package com.x8bit.bitwarden.data.vault.datasource.sdk

import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherView
import com.bitwarden.core.Folder
import com.bitwarden.core.FolderView
import com.bitwarden.sdk.ClientVault
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class VaultSdkSourceTest {
    private val clientVault = mockk<ClientVault>()

    private val vaultSdkSource: VaultSdkSource = VaultSdkSourceImpl(
        clientVault = clientVault,
    )

    @Test
    fun `Cipher decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val mockCipher = mockk<Cipher>()
        val expectedResult = mockk<CipherView>()
        coEvery {
            clientVault.ciphers().decrypt(
                cipher = mockCipher,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptCipher(
            cipher = mockCipher,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.ciphers().decrypt(
                cipher = mockCipher,
            )
        }
    }

    @Test
    fun `Cipher decryptList should call SDK and return a Result with correct data`() = runBlocking {
        val mockCiphers = mockk<List<Cipher>>()
        val expectedResult = mockk<List<CipherListView>>()
        coEvery {
            clientVault.ciphers().decryptList(
                ciphers = mockCiphers,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptCipherList(
            cipherList = mockCiphers,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.ciphers().decryptList(
                ciphers = mockCiphers,
            )
        }
    }

    @Test
    fun `Folder decrypt should call SDK and return a Result with correct data`() = runBlocking {
        val mockFolder = mockk<Folder>()
        val expectedResult = mockk<FolderView>()
        coEvery {
            clientVault.folders().decrypt(
                folder = mockFolder,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptFolder(
            folder = mockFolder,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.folders().decrypt(
                folder = mockFolder,
            )
        }
    }

    @Test
    fun `Folder decryptList should call SDK and return a Result with correct data`() = runBlocking {
        val mockFolders = mockk<List<Folder>>()
        val expectedResult = mockk<List<FolderView>>()
        coEvery {
            clientVault.folders().decryptList(
                folders = mockFolders,
            )
        } returns expectedResult
        val result = vaultSdkSource.decryptFolderList(
            folderList = mockFolders,
        )
        assertEquals(
            expectedResult.asSuccess(),
            result,
        )
        coVerify {
            clientVault.folders().decryptList(
                folders = mockFolders,
            )
        }
    }
}
