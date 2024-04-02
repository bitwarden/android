package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.asSuccess
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create
import java.time.ZonedDateTime

class AuthRequestsServiceTest : BaseServiceTest() {

    private val authRequestsApi: AuthenticatedAuthRequestsApi = retrofit.create()
    private val service = AuthRequestsServiceImpl(
        authenticatedAuthRequestsApi = authRequestsApi,
    )

    @Test
    fun `getAuthRequests when request response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.getAuthRequests()
        assertTrue(actual.isFailure)
    }

    @Test
    fun `getAuthRequests when request response is Success should return Success`() = runTest {
        val json = """
            {
              "data": [
                {
                  "id": "1",
                  "publicKey": "2",
                  "requestDeviceType": "Android",
                  "requestIpAddress": "1.0.0.1",
                  "creationDate": "2024-09-13T01:00:00.00Z",
                  "requestApproved": true,
                  "origin": "www.bitwarden.com"
                }
              ]
            }
            """
        val response = MockResponse().setBody(json).setResponseCode(200)
        server.enqueue(response)
        val actual = service.getAuthRequests()
        assertTrue(actual.isSuccess)
    }

    @Test
    fun `updateAuthRequest when request response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.updateAuthRequest(
            requestId = "userId",
            deviceId = "deviceId",
            key = "secureKey",
            masterPasswordHash = null,
            isApproved = true,
        )
        assertTrue(actual.isFailure)
    }

    @Test
    fun `updateAuthRequest when request response is Success should return Success`() = runTest {
        val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
        server.enqueue(response)
        val actual = service.updateAuthRequest(
            requestId = "userId",
            deviceId = "deviceId",
            key = "secureKey",
            masterPasswordHash = "verySecureHash",
            isApproved = true,
        )
        assertEquals(AUTH_REQUEST_RESPONSE.asSuccess(), actual)
    }

    @Test
    fun `getAuthRequest when request response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.getAuthRequest(requestId = "1")
        assertTrue(actual.isFailure)
    }

    @Test
    fun `getAuthRequest when request response is Success should return Success`() = runTest {
        val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
        server.enqueue(response)
        val actual = service.getAuthRequest(requestId = "1")
        assertEquals(AUTH_REQUEST_RESPONSE.asSuccess(), actual)
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
