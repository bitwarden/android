package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.vault.datasource.network.api.SendsApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSend
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSendJsonRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class SendsServiceTest : BaseServiceTest() {
    private val sendsApi: SendsApi = retrofit.create()

    private val sendsService: SendsService = SendsServiceImpl(
        sendsApi = sendsApi,
        json = json,
    )

    @Test
    fun `createSend should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CREATE_UPDATE_SEND_SUCCESS_JSON))
        val result = sendsService.createSend(
            body = createMockSendJsonRequest(number = 1),
        )
        assertEquals(
            createMockSend(number = 1),
            result.getOrThrow(),
        )
    }

    @Test
    fun `updateSend with success response should return a Success with the correct send`() =
        runTest {
            server.enqueue(MockResponse().setBody(CREATE_UPDATE_SEND_SUCCESS_JSON))
            val result = sendsService.updateSend(
                sendId = "send-id-1",
                body = createMockSendJsonRequest(number = 1),
            )
            assertEquals(
                UpdateSendResponseJson.Success(
                    send = createMockSend(number = 1),
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `updateSend with an invalid response should return an Invalid with the correct data`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody(UPDATE_SEND_INVALID_JSON))
            val result = sendsService.updateSend(
                sendId = "send-id-1",
                body = createMockSendJsonRequest(number = 1),
            )
            assertEquals(
                UpdateSendResponseJson.Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `deleteSend should return a Success with the correct data`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val result = sendsService.deleteSend(sendId = "send-id-1")
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun `removeSendPassword with success response should return a Success with the correct send`() =
        runTest {
            server.enqueue(MockResponse().setBody(CREATE_UPDATE_SEND_SUCCESS_JSON))
            val result = sendsService.removeSendPassword(sendId = "send-id-1")
            assertEquals(
                UpdateSendResponseJson.Success(send = createMockSend(number = 1)),
                result.getOrThrow(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `removeSendPassword with an invalid response should return an Invalid with the correct data`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody(UPDATE_SEND_INVALID_JSON))
            val result = sendsService.removeSendPassword(sendId = "send-id-1")
            assertEquals(
                UpdateSendResponseJson.Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                ),
                result.getOrThrow(),
            )
        }
}

private const val CREATE_UPDATE_SEND_SUCCESS_JSON = """
{
  "id": "mockId-1",
  "accessId": "mockAccessId-1",
  "type": 1,
  "name": "mockName-1",
  "notes": "mockNotes-1",
  "file": {
    "id": "mockId-1",
    "fileName": "mockFileName-1",
    "size": 1,
    "sizeName": "mockSizeName-1"
  },
  "text": {
    "text": "mockText-1",
    "hidden": false
  },
  "key": "mockKey-1",
  "maxAccessCount": 1,
  "accessCount": 1,
  "password": "mockPassword-1",
  "disabled": false,
  "revisionDate": "2023-10-27T12:00:00.00Z",
  "expirationDate": "2023-10-27T12:00:00.00Z",
  "deletionDate": "2023-10-27T12:00:00.00Z",
  "hideEmail": false
}
"""

private const val UPDATE_SEND_INVALID_JSON = """
{
  "message": "You do not have permission to edit this.",
  "validationErrors": null
}
"""
