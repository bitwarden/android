package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.sdk.FolderRepository
import com.bitwarden.vault.Folder
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkFolderResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SdkFolderRepositoryTest {

    private val vaultDiskSource: VaultDiskSource = mockk()

    private val sdkFolderRepository: FolderRepository = SdkFolderRepository(
        userId = USER_ID,
        vaultDiskSource = vaultDiskSource,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(
            SyncResponseJson.Folder::toEncryptedSdkFolder,
            Folder::toEncryptedNetworkFolderResponse,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SyncResponseJson.Folder::toEncryptedSdkFolder,
            Folder::toEncryptedNetworkFolderResponse,
        )
    }

    @Test
    fun `get should return null when not present`() = runTest {
        val folderId = "folderId"
        coEvery {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        } returns null

        val result = sdkFolderRepository.get(id = folderId)

        assertNull(result)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        }
    }

    @Test
    fun `get should return an encrypted sdk folder when present`() = runTest {
        val folderId = "folderId"
        val expected = mockk<Folder>()
        val responseFolder = mockk<SyncResponseJson.Folder> {
            every { toEncryptedSdkFolder() } returns expected
        }
        coEvery {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        } returns responseFolder

        val result = sdkFolderRepository.get(id = folderId)

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        }
    }

    @Test
    fun `has should return false when not present`() = runTest {
        val folderId = "folderId"
        coEvery {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        } returns null

        val result = sdkFolderRepository.has(id = folderId)

        assertFalse(result)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        }
    }

    @Test
    fun `has should return true when present`() = runTest {
        val folderId = "folderId"
        val responseFolder = mockk<SyncResponseJson.Folder> {
            every { toEncryptedSdkFolder() } returns mockk<Folder>()
        }
        coEvery {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        } returns responseFolder

        val result = sdkFolderRepository.has(id = folderId)

        assertTrue(result)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolder(userId = USER_ID, folderId = folderId)
        }
    }

    @Test
    fun `list should return empty list when nothing present`() = runTest {
        coEvery { vaultDiskSource.getFolders(userId = USER_ID) } returns emptyList()

        val result = sdkFolderRepository.list()

        assertEquals(emptyList<Folder>(), result)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolders(userId = USER_ID)
        }
    }

    @Test
    fun `list should return encrypted sdk folder list when present`() = runTest {
        val expectedFolder = mockk<Folder>()
        val expected = listOf(expectedFolder)
        val responseFolder = mockk<SyncResponseJson.Folder> {
            every { toEncryptedSdkFolder() } returns expectedFolder
        }
        coEvery { vaultDiskSource.getFolders(userId = USER_ID) } returns listOf(responseFolder)

        val result = sdkFolderRepository.list()

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolders(userId = USER_ID)
        }
    }

    @Test
    fun `remove should call deleteFolder on the vaultDiskSource`() = runTest {
        val folderId = "folderId"
        coEvery {
            vaultDiskSource.deleteFolder(userId = USER_ID, folderId = folderId)
        } just runs

        sdkFolderRepository.remove(id = folderId)

        coVerify(exactly = 1) {
            vaultDiskSource.deleteFolder(userId = USER_ID, folderId = folderId)
        }
    }

    @Test
    fun `set should do nothing if the ids don't match`() = runTest {
        val folderId = "folderId"
        val folder = mockk<Folder> {
            every { id } returns "differentFolderId"
        }

        sdkFolderRepository.set(id = folderId, value = folder)

        coVerify(exactly = 0) {
            vaultDiskSource.saveFolder(userId = any(), folder = any())
        }
    }

    @Test
    fun `set should call saveFolder on vaultDiskSource if ids match`() = runTest {
        val folderId = "folderId"
        val responseFolder = mockk<SyncResponseJson.Folder>()
        val folder = mockk<Folder> {
            every { id } returns folderId
            every { toEncryptedNetworkFolderResponse() } returns responseFolder
        }
        coEvery {
            vaultDiskSource.saveFolder(userId = USER_ID, folder = responseFolder)
        } just runs

        sdkFolderRepository.set(id = folderId, value = folder)

        coVerify(exactly = 1) {
            vaultDiskSource.saveFolder(userId = USER_ID, folder = responseFolder)
        }
    }

    @Test
    fun `setBulk should skip entries where ids do not match`() = runTest {
        val folder = mockk<Folder> {
            every { id } returns "differentId"
        }

        sdkFolderRepository.setBulk(values = mapOf("folderId" to folder))

        coVerify(exactly = 0) {
            vaultDiskSource.saveFolders(userId = any(), folders = any())
        }
    }

    @Test
    fun `setBulk should save valid entries via saveFolders`() = runTest {
        val folderId = "folderId"
        val responseFolder = mockk<SyncResponseJson.Folder>()
        val folder = mockk<Folder> {
            every { id } returns folderId
            every { toEncryptedNetworkFolderResponse() } returns responseFolder
        }
        coEvery {
            vaultDiskSource.saveFolders(userId = USER_ID, folders = listOf(responseFolder))
        } just runs

        sdkFolderRepository.setBulk(values = mapOf(folderId to folder))

        coVerify(exactly = 1) {
            vaultDiskSource.saveFolders(userId = USER_ID, folders = listOf(responseFolder))
        }
    }

    @Test
    fun `setBulk should do nothing for empty map`() = runTest {
        sdkFolderRepository.setBulk(values = emptyMap())

        coVerify(exactly = 0) {
            vaultDiskSource.saveFolders(userId = any(), folders = any())
        }
    }

    @Test
    fun `removeBulk should call deleteSelectedFolders`() = runTest {
        val ids = listOf("id1", "id2")
        coEvery {
            vaultDiskSource.deleteSelectedFolders(userId = USER_ID, folderIds = ids)
        } just runs

        sdkFolderRepository.removeBulk(keys = ids)

        coVerify(exactly = 1) {
            vaultDiskSource.deleteSelectedFolders(userId = USER_ID, folderIds = ids)
        }
    }

    @Test
    fun `removeBulk should not call deleteSelectedFolders for empty list`() = runTest {
        sdkFolderRepository.removeBulk(keys = emptyList())

        coVerify(exactly = 0) {
            vaultDiskSource.deleteSelectedFolders(userId = any(), folderIds = any())
        }
    }

    @Test
    fun `removeAll should call deleteAllFolders`() = runTest {
        coEvery { vaultDiskSource.deleteAllFolders(userId = USER_ID) } just runs

        sdkFolderRepository.removeAll()

        coVerify(exactly = 1) {
            vaultDiskSource.deleteAllFolders(userId = USER_ID)
        }
    }
}

private const val USER_ID: String = "userId"
