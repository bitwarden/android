package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.DevicesApi
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class DevicesServiceTest : BaseServiceTest() {

    private val devicesApi: DevicesApi = retrofit.create()
    private val service = DevicesServiceImpl(
        devicesApi = devicesApi,
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
}
