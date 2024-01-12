package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.TimeZone

class SendDataExtensionsTest {

    @BeforeEach
    fun setup() {
        // Setting the timezone so the tests pass consistently no matter the environment.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        mockkStatic(SEND_VIEW_EXTENSIONS_PATH)
    }

    @AfterEach
    fun tearDown() {
        // Clearing the timezone after the test.
        TimeZone.setDefault(null)
        unmockkStatic(SEND_VIEW_EXTENSIONS_PATH)
    }

    @Test
    fun `toViewState should return Empty when SendData is empty`() {
        val sendData = SendData(emptyList())

        val result = sendData.toViewState(DEFAULT_BASE_URL)

        assertEquals(SendState.ViewState.Empty, result)
    }

    @Test
    fun `toViewState should return Content when SendData is not empty`() {
        val textSendView = createMockSendView(number = 2, type = SendType.TEXT)
        val fileSendView = createMockSendView(number = 1, type = SendType.FILE)
        val list = listOf(
            textSendView,
            fileSendView,
        )
        val textSendViewUrl1 = "www.test.com/#/send/mockAccessId-1/mockKey-1"
        val textSendViewUrl2 = "www.test.com/#/send/mockAccessId-2/mockKey-2"
        val sendData = SendData(list)
        every { textSendView.toSendUrl(DEFAULT_BASE_URL) } returns textSendViewUrl2
        every { fileSendView.toSendUrl(DEFAULT_BASE_URL) } returns textSendViewUrl1

        val result = sendData.toViewState(DEFAULT_BASE_URL)

        assertEquals(
            SendState.ViewState.Content(
                textTypeCount = 1,
                fileTypeCount = 1,
                sendItems = listOf(
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-1",
                        name = "mockName-1",
                        deletionDate = "Oct 27, 2023, 12:00 PM",
                        type = SendState.ViewState.Content.SendItem.Type.FILE,
                        iconList = listOf(
                            SendStatusIcon.PASSWORD,
                            SendStatusIcon.MAX_ACCESS_COUNT_REACHED,
                            SendStatusIcon.EXPIRED,
                            SendStatusIcon.PENDING_DELETE,
                        ),
                        shareUrl = "www.test.com/#/send/mockAccessId-1/mockKey-1",
                        hasPassword = true,
                    ),
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-2",
                        name = "mockName-2",
                        deletionDate = "Oct 27, 2023, 12:00 PM",
                        type = SendState.ViewState.Content.SendItem.Type.TEXT,
                        iconList = listOf(
                            SendStatusIcon.PASSWORD,
                            SendStatusIcon.MAX_ACCESS_COUNT_REACHED,
                            SendStatusIcon.EXPIRED,
                            SendStatusIcon.PENDING_DELETE,
                        ),
                        shareUrl = "www.test.com/#/send/mockAccessId-2/mockKey-2",
                        hasPassword = true,
                    ),
                ),
            ),
            result,
        )
    }
}

private const val SEND_VIEW_EXTENSIONS_PATH: String =
    "com.x8bit.bitwarden.ui.tools.feature.send.util.SendViewExtensionsKt"

private const val DEFAULT_BASE_URL: String = "www.test.com/"
