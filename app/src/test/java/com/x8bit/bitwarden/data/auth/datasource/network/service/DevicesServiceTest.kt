package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedDevicesApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedDevicesApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.asSuccess
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create
import java.time.ZonedDateTime

class DevicesServiceTest : BaseServiceTest() {

    private val authenticatedDevicesApi: AuthenticatedDevicesApi = retrofit.create()
    private val unauthenticatedDevicesApi: UnauthenticatedDevicesApi = retrofit.create()
    private val service = DevicesServiceImpl(
        authenticatedDevicesApi = authenticatedDevicesApi,
        unauthenticatedDevicesApi = unauthenticatedDevicesApi,
    )

    @Test
    fun `getIsKnownDevice when request response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.getIsKnownDevice("email", "id")
        assertTrue(actual.isFailure)
    }

    @Test
    fun `getIsKnownDevice when request response is Success should return Success`() = runTest {
        val response = MockResponse().setBody("false").setResponseCode(200)
        server.enqueue(response)
        val actual = service.getIsKnownDevice("email", "id")
        assertTrue(actual.isSuccess)
    }

    @Test
    fun `trustDevice when response is Failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)
        val actual = service.trustDevice(
            appId = "appId",
            encryptedUserKey = "encryptedUserKey",
            encryptedDevicePublicKey = "encryptedDevicePublicKey",
            encryptedDevicePrivateKey = "encryptedDevicePrivateKey",
        )
        assertTrue(actual.isFailure)
    }

    @Test
    fun `trustDevice when response is Success should return Success`() = runTest {
        val response = MockResponse().setBody(TRUST_DEVICE_RESPONSE_JSON).setResponseCode(200)
        server.enqueue(response)
        val actual = service.trustDevice(
            appId = "appId",
            encryptedUserKey = "encryptedUserKey",
            encryptedDevicePublicKey = "encryptedDevicePublicKey",
            encryptedDevicePrivateKey = "encryptedDevicePrivateKey",
        )
        assertEquals(TRUST_DEVICE_RESPONSE.asSuccess(), actual)
    }
}

private val TRUST_DEVICE_RESPONSE: TrustedDeviceKeysResponseJson =
    TrustedDeviceKeysResponseJson(
        id = "0d31b6fb-d282-43c7-b614-b13e0129dbd7",
        name = "Pixel 8",
        identifier = "ea7c0a13-5ce4-4f96-8e17-4fc7fa54f464",
        type = 0,
        creationDate = ZonedDateTime.parse("2024-03-25T18:04:28.23Z"),
    )

private const val TRUST_DEVICE_RESPONSE_JSON: String = """
{
  "id":"0d31b6fb-d282-43c7-b614-b13e0129dbd7",
  "name":"Pixel 8",
  "type":0,
  "identifier":"ea7c0a13-5ce4-4f96-8e17-4fc7fa54f464",
  "creationDate":"2024-03-25T18:04:28.23Z",
  "isTrusted":true,
  "object":"device"
}
"""
