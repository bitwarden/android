package com.x8bit.bitwarden.data.platform.datasource.network.util

import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.every
import io.mockk.mockk
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

class CallExtensionsTest {

    @Test
    fun `executeForResult returns failure when execute throws IOException`() {
        val request = mockk<Request> {
            every { url } returns mockk()
        }
        val call = mockk<Call<Unit>> {
            every { request() } returns request
            every { execute() } throws IOException("Fail")
        }

        val result = call.executeForResult()

        assertTrue(result.isFailure)
    }

    @Test
    fun `executeForResult returns failure when execute throws RuntimeException`() {
        val request = mockk<Request> {
            every { url } returns mockk()
        }
        val call = mockk<Call<Unit>> {
            every { request() } returns request
            every { execute() } throws RuntimeException("Fail")
        }

        val result = call.executeForResult()

        assertTrue(result.isFailure)
    }

    @Test
    fun `executeForResult returns failure when response is failure`() {
        val request = mockk<Request> {
            every { url } returns mockk()
        }
        val call = mockk<Call<Unit>> {
            every { request() } returns request
            every { execute() } returns Response.error(400, "".toResponseBody())
        }

        val result = call.executeForResult()

        assertTrue(result.isFailure)
    }

    @Test
    fun `executeForResult returns success when response is failure`() {
        val call = mockk<Call<Unit>> {
            every { execute() } returns Response.success(Unit)
        }

        val result = call.executeForResult()

        assertEquals(Unit.asSuccess(), result)
    }
}
