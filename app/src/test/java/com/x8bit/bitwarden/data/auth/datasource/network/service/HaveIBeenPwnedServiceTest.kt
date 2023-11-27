package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.HaveIBeenPwnedApi
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class HaveIBeenPwnedServiceTest : BaseServiceTest() {

    private val haveIBeenPwnedApi: HaveIBeenPwnedApi = retrofit.create()
    private val service = HaveIBeenPwnedServiceImpl(haveIBeenPwnedApi)

    @Test
    fun `getPasswordBreachCount should return failure when service returns failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        assertTrue(service.getPasswordBreachCount(PWNED_PASSWORD).isFailure)
    }

    @Test
    fun `getPasswordBreachCount should return breach count when password is in response`() =
        runTest {
            val response = MockResponse().setBody(HIBP_RESPONSE)
            server.enqueue(response)
            val result = service.getPasswordBreachCount(PWNED_PASSWORD)
            assertEquals(36865, result.getOrThrow())
        }

    @Test
    fun `getPasswordBreachCount should returns 0 when password is not in response`() = runTest {
        val response = MockResponse().setBody(HIBP_RESPONSE)
        server.enqueue(response)
        val result = service.getPasswordBreachCount("testpassword")
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun `hasPasswordBeenBreached should return failure when service returns failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        assertTrue(service.hasPasswordBeenBreached(PWNED_PASSWORD).isFailure)
    }

    @Test
    fun `hasPasswordBeenBreached should return true when password is in response`() = runTest {
        val response = MockResponse().setBody(HIBP_RESPONSE)
        server.enqueue(response)
        val result = service.hasPasswordBeenBreached(PWNED_PASSWORD)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `hasPasswordBeenBreached should return false when password is not in response`() = runTest {
        val response = MockResponse().setBody(HIBP_RESPONSE)
        server.enqueue(response)
        val result = service.hasPasswordBeenBreached("testpassword")
        assertFalse(result.getOrThrow())
    }
}

private const val PWNED_PASSWORD = "password1234"

private val HIBP_RESPONSE = """
    FBD6D76BB5D2041542D7D2E3FAC5BB05593:36865
    F390F21EBEFEF07A1DA4E661AF830FD76A6:3
    F3CAEF537A4881A05E2A9A9A8A236FE7C14:1
    F44FD6981B10EC24A93989A0C61E71C767C:5
""".trimIndent()
