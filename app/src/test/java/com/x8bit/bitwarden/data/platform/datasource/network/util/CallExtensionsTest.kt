package com.x8bit.bitwarden.data.platform.datasource.network.util

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import io.mockk.every
import io.mockk.mockk
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.net.URL

class CallExtensionsTest {

    @Test
    fun `executeForNetworkResult returns failure when execute throws IOException`() {
        val request = createMockkRequest()
        val call = mockk<Call<Unit>> {
            every { request() } returns request
            every { execute() } throws IOException("Fail")
        }

        val result = call.executeForNetworkResult()

        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun `executeForNetworkResult returns failure when execute throws RuntimeException`() {
        val request = createMockkRequest()
        val call = mockk<Call<Unit>> {
            every { request() } returns request
            every { execute() } throws RuntimeException("Fail")
        }

        val result = call.executeForNetworkResult()

        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun `executeForNetworkResult returns failure when response is failure`() {
        val request = createMockkRequest()
        val call = mockk<Call<Unit>> {
            every { request() } returns request
            every { execute() } returns Response.error(400, "".toResponseBody())
        }

        val result = call.executeForNetworkResult()

        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun `executeForNetworkResult returns success when response is failure`() {
        val call = mockk<Call<Unit>> {
            every { execute() } returns Response.success(Unit)
        }

        val result = call.executeForNetworkResult()

        assertEquals(NetworkResult.Success(Unit), result)
    }

    private fun createMockkRequest(): Request {
        val mockkUrl = mockk<URL> {
            every { protocol } returns "http"
            every { authority } returns "bitwarden.com"
            every { path } returns "/example/path"
        }
        val mockkHttpUrl = mockk<HttpUrl> {
            every { toUrl() } returns mockkUrl
        }
        return mockk<Request> {
            every { url } returns mockkHttpUrl
        }
    }
}
