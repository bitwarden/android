package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthRequestsApi
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class AuthRequestsServiceTest : BaseServiceTest() {

    private val authRequestsApi: AuthRequestsApi = retrofit.create()
    private val service = AuthRequestsServiceImpl(
        authRequestsApi = authRequestsApi,
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
}
