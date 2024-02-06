package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.datasource.network.api.PushApi
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create
import java.util.UUID

class PushServiceTest : BaseServiceTest() {
    private val mockAppId = UUID.randomUUID().toString()
    private val pushApi: PushApi = retrofit.create()

    private val pushService: PushService = PushServiceImpl(
        pushApi = pushApi,
        appId = mockAppId,
    )

    @Test
    fun `putDeviceToken should return the correct response`() = runTest {
        val pushToken = UUID.randomUUID().toString()
        server.enqueue(MockResponse())
        val result = pushService.putDeviceToken(
            body = PushTokenRequest(
                pushToken = pushToken,
            ),
        )
        assertEquals(
            Unit,
            result.getOrThrow(),
        )
    }
}
