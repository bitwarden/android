package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.vault.datasource.network.api.FoldersApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.FolderJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateFolderResponseJson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create
import java.time.ZonedDateTime

class FoldersServiceTest : BaseServiceTest() {
    private val folderApi: FoldersApi = retrofit.create()

    private val folderService: FolderService = FolderServiceImpl(
        foldersApi = folderApi,
        json = json,
    )

    @Test
    fun `createFolder should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_UPDATE_FOLDER_SUCCESS_JSON))
        val result = folderService.createFolder(
            body = FolderJsonRequest(DEFAULT_NAME),
        )
        assertEquals(
            DEFAULT_FOLDER,
            result.getOrThrow(),
        )
    }

    @Test
    fun `updateFolder with success response should return a Success with the correct folder`() =
        runTest {
            server.enqueue(MockResponse().setBody(CREATE_UPDATE_FOLDER_SUCCESS_JSON))
            val result = folderService.updateFolder(
                folderId = DEFAULT_ID,
                body = FolderJsonRequest(DEFAULT_NAME),
            )

            assertEquals(
                UpdateFolderResponseJson.Success(DEFAULT_FOLDER),
                result.getOrThrow(),
            )
        }

    @Test
    fun `updateFolder with invalid response should return an Invalid with the correct data`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody(UPDATE_FOLDER_INVALID_JSON))
            val result = folderService.updateFolder(
                folderId = DEFAULT_ID,
                body = FolderJsonRequest(DEFAULT_NAME),
            )

            assertEquals(
                UpdateFolderResponseJson.Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `DeleteFolder should return a Success with the correct data`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val result = folderService.deleteFolder(DEFAULT_ID)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `getFolder should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_UPDATE_FOLDER_SUCCESS_JSON))
        val result = folderService.getFolder("FolderId")
        assertEquals(
            DEFAULT_FOLDER,
            result.getOrThrow(),
        )
    }
}

private const val DEFAULT_ID = "FolderId"
private const val DEFAULT_NAME = "TestName"

private val DEFAULT_FOLDER = SyncResponseJson.Folder(
    id = DEFAULT_ID,
    name = DEFAULT_NAME,
    revisionDate = ZonedDateTime.parse("2024-01-24T22:40:17.1559611Z"),
)

private const val CREATE_UPDATE_FOLDER_SUCCESS_JSON = """
{
  "id":"FolderId",
  "name":"TestName",
  "revisionDate":"2024-01-24T22:40:17.1559611Z",
  "object":"folder"
}
"""

private const val UPDATE_FOLDER_INVALID_JSON = """
{
  "message": "You do not have permission to edit this.",
  "validationErrors": null
}
"""
