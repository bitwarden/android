package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.vault.datasource.network.api.DownloadApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class DownloadServiceTest : BaseServiceTest() {
    private val downloadApi: DownloadApi = retrofit.create()

    private val downloadService: DownloadService = DownloadServiceImpl(
        downloadApi = downloadApi,
    )

    @Test
    fun `getDataStream should return a raw stream RespondBody`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("Bitwarden")
                .setHeader("Content-Type", "application/stream"),
        )
        val url = "/test-url"
        val result = downloadService.getDataStream(url)
        assertTrue(result.isSuccess)
        assertEquals("Bitwarden", String(result.getOrThrow().bytes()))
    }
}
