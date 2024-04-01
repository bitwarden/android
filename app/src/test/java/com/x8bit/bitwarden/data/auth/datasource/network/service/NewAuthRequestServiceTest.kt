package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestTypeJson
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

class NewAuthRequestServiceTest : BaseServiceTest() {

    private val authenticatedAuthRequestsApi: AuthenticatedAuthRequestsApi = retrofit.create()
    private val unauthenticatedAuthRequestsApi: UnauthenticatedAuthRequestsApi = retrofit.create()
    private val service = NewAuthRequestServiceImpl(
        authenticatedAuthRequestsApi = authenticatedAuthRequestsApi,
        unauthenticatedAuthRequestsApi = unauthenticatedAuthRequestsApi,
    )

    @Test
    fun `createAuthRequest when LOGIN_WITH_DEVICE and request is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val deviceIdentifier = "4321"
            val actual = service.createAuthRequest(
                email = "test@gmail.com",
                publicKey = "1234",
                deviceId = deviceIdentifier,
                accessCode = "accessCode",
                fingerprint = "fingerprint",
                authRequestType = AuthRequestTypeJson.LOGIN_WITH_DEVICE,
            )

            val request = server.takeRequest()
            assertEquals(deviceIdentifier, request.getHeader("Device-Identifier"))
            assertEquals("$urlPrefix/auth-requests", request.requestUrl.toString())
            assertTrue(actual.isFailure)
        }

    @Test
    fun `createAuthRequest when LOGIN_WITH_DEVICE and request is Success should return Success`() =
        runTest {
            val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
            server.enqueue(response)
            val deviceIdentifier = "4321"
            val actual = service.createAuthRequest(
                email = "test@gmail.com",
                publicKey = "1234",
                deviceId = deviceIdentifier,
                accessCode = "accessCode",
                fingerprint = "fingerprint",
                authRequestType = AuthRequestTypeJson.LOGIN_WITH_DEVICE,
            )
            val request = server.takeRequest()
            assertEquals(deviceIdentifier, request.getHeader("Device-Identifier"))
            assertEquals("$urlPrefix/auth-requests", request.requestUrl.toString())
            assertEquals(AUTH_REQUEST_RESPONSE.asSuccess(), actual)
        }

    @Test
    fun `createAuthRequest when ADMIN_APPROVAL and request is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val deviceIdentifier = "4321"
            val actual = service.createAuthRequest(
                email = "test@gmail.com",
                publicKey = "1234",
                deviceId = deviceIdentifier,
                accessCode = "accessCode",
                fingerprint = "fingerprint",
                authRequestType = AuthRequestTypeJson.ADMIN_APPROVAL,
            )

            val request = server.takeRequest()
            assertEquals(deviceIdentifier, request.getHeader("Device-Identifier"))
            assertEquals("$urlPrefix/auth-requests/admin-request", request.requestUrl.toString())
            assertTrue(actual.isFailure)
        }

    @Test
    fun `createAuthRequest when ADMIN_APPROVAL and request is Success should return Success`() =
        runTest {
            val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
            server.enqueue(response)
            val deviceIdentifier = "4321"
            val actual = service.createAuthRequest(
                email = "test@gmail.com",
                publicKey = "1234",
                deviceId = deviceIdentifier,
                accessCode = "accessCode",
                fingerprint = "fingerprint",
                authRequestType = AuthRequestTypeJson.ADMIN_APPROVAL,
            )
            val request = server.takeRequest()
            assertEquals(deviceIdentifier, request.getHeader("Device-Identifier"))
            assertEquals("$urlPrefix/auth-requests/admin-request", request.requestUrl.toString())
            assertEquals(AUTH_REQUEST_RESPONSE.asSuccess(), actual)
        }

    @Test
    fun `createAuthRequest when UNLOCK should return Failure`() = runTest {
        val deviceIdentifier = "4321"
        val actual = service.createAuthRequest(
            email = "test@gmail.com",
            publicKey = "1234",
            deviceId = deviceIdentifier,
            accessCode = "accessCode",
            fingerprint = "fingerprint",
            authRequestType = AuthRequestTypeJson.UNLOCK,
        )
        assertTrue(actual.isFailure)
    }

    @Test
    fun `getAuthRequestUpdate when not SSO and response is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val requestId = "1"
            val accessCode = "accessCode"
            val actual = service.getAuthRequestUpdate(
                requestId = requestId,
                accessCode = accessCode,
                isSso = false,
            )
            val request = server.takeRequest()
            assertEquals(
                "$urlPrefix/auth-requests/$requestId/response?code=$accessCode",
                request.requestUrl.toString(),
            )
            assertTrue(actual.isFailure)
        }

    @Test
    fun `getAuthRequestUpdate when not SSO and response is Success should return Success`() =
        runTest {
            val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
            server.enqueue(response)
            val requestId = "1"
            val accessCode = "accessCode"
            val actual = service.getAuthRequestUpdate(
                requestId = requestId,
                accessCode = accessCode,
                isSso = false,
            )
            val request = server.takeRequest()
            assertEquals(
                "$urlPrefix/auth-requests/$requestId/response?code=$accessCode",
                request.requestUrl.toString(),
            )
            assertEquals(AUTH_REQUEST_RESPONSE.asSuccess(), actual)
        }

    @Test
    fun `getAuthRequestUpdate when SSO and response is Failure should return Failure`() =
        runTest {
            val response = MockResponse().setResponseCode(400)
            server.enqueue(response)
            val requestId = "1"
            val actual = service.getAuthRequestUpdate(
                requestId = requestId,
                accessCode = "accessCode",
                isSso = true,
            )
            val request = server.takeRequest()
            assertEquals("$urlPrefix/auth-requests/$requestId", request.requestUrl.toString())
            assertTrue(actual.isFailure)
        }

    @Test
    fun `getAuthRequestUpdate when SSO and response is Success should return Success`() =
        runTest {
            val response = MockResponse().setBody(AUTH_REQUEST_RESPONSE_JSON).setResponseCode(200)
            server.enqueue(response)
            val requestId = "1"
            val actual = service.getAuthRequestUpdate(
                requestId = requestId,
                accessCode = "accessCode",
                isSso = true,
            )
            val request = server.takeRequest()
            assertEquals("$urlPrefix/auth-requests/$requestId", request.requestUrl.toString())
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
