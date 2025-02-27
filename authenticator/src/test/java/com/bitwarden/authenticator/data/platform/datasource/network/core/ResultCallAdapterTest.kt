package com.bitwarden.authenticator.data.platform.datasource.network.core

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET

class ResultCallAdapterTest {

    private val server: MockWebServer = MockWebServer().apply { start() }
    private val testService: FakeService =
        Retrofit
            .Builder()
            .baseUrl(server.url("/").toString())
            // add the adapter being tested
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
            .create()

    @AfterEach
    fun after() {
        server.shutdown()
    }

    @Test
    fun `when server returns error response code result should be failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = testService.requestWithUnitData()
        Assertions.assertTrue(result.isFailure)
    }

    @Test
    fun `when server returns successful response result should be success`() = runBlocking {
        server.enqueue(MockResponse())
        val result = testService.requestWithUnitData()
        Assertions.assertTrue(result.isSuccess)
    }
}

/**
 * Fake retrofit service used for testing call adapters.
 */
private interface FakeService {
    @GET("/fake")
    suspend fun requestWithUnitData(): Result<Unit>
}
