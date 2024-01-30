package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create
import java.time.ZonedDateTime

class NewAuthRequestServiceTest : BaseServiceTest() {

    private val authRequestsApi: UnauthenticatedAuthRequestsApi = retrofit.create()
    private val service = NewAuthRequestServiceImpl(
        unauthenticatedAuthRequestsApi = authRequestsApi,
    )

    @Test
    fun `createAuthRequest when request response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.createAuthRequest(
            email = "test@gmail.com",
            publicKey = "1234",
            deviceId = "4321",
            accessCode = "accessCode",
            fingerprint = "fingerprint",
        )
        assertTrue(actual.isFailure)
    }

    @Test
    fun `createAuthRequest when request response is Success should return Success`() = runTest {
        val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
        server.enqueue(response)
        val actual = service.createAuthRequest(
            email = "test@gmail.com",
            publicKey = "1234",
            deviceId = "4321",
            accessCode = "accessCode",
            fingerprint = "fingerprint",
        )
        assertEquals(Result.success(AUTH_REQUEST_RESPONSE), actual)
    }

    @Test
    fun `getAuthRequestUpdate when request response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.getAuthRequestUpdate(
            requestId = "1",
            accessCode = "accessCode",
        )
        assertTrue(actual.isFailure)
    }

    @Test
    fun `getAuthRequestUpdate when request response is Success should return Success`() = runTest {
        val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
        server.enqueue(response)
        val actual = service.getAuthRequestUpdate(
            requestId = "1",
            accessCode = "accessCode",
        )
        assertEquals(Result.success(AUTH_REQUEST_RESPONSE), actual)
    }
}

private const val AUTH_REQUEST_RESPONSE_JSON = """
{
  "id": "1",
  "publicKey": "2",
  "requestDeviceType": "Android",
  "requestIpAddress": "1.0.0.1",
  "key": "key",
  "masterPasswordHash": "verySecureHash",
  "creationDate": "2024-09-13T01:00:00.00Z",
  "requestApproved": true,
  "origin": "www.bitwarden.com"
}
"""

private val AUTH_REQUEST_RESPONSE = AuthRequestsResponseJson.AuthRequest(
    id = "1",
    publicKey = "2",
    platform = "Android",
    ipAddress = "1.0.0.1",
    key = "key",
    masterPasswordHash = "verySecureHash",
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    responseDate = null,
    requestApproved = true,
    originUrl = "www.bitwarden.com",
)
