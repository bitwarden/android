package com.x8bit.bitwarden.example

import com.x8bit.bitwarden.data.datasource.network.ResultCallAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET

class ResultCallAdapterTests {

    private val server: MockWebServer = MockWebServer().apply { start() }
    private val testService: FakeService =
        Retrofit.Builder()
            .baseUrl(server.url("/").toString())
            // add the adapter being tested
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
            .create()

    @After
    fun after() {
        server.shutdown()
    }

    @Test
    fun `when server returns error response code result should be failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = testService.requestWithUnitData()
        assertTrue(result.isFailure)
    }

    @Test
    fun `when server returns successful response result should be success`() = runBlocking {
        server.enqueue(MockResponse())
        val result = testService.requestWithUnitData()
        assertTrue(result.isSuccess)
    }
}

/**
 * Fake retrofit service used for testing call adapters.
 */
private interface FakeService {
    @GET("/fake")
    suspend fun requestWithUnitData(): Result<Unit>
}
