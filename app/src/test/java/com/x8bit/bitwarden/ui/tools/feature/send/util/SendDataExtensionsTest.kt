package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
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
    }

    @AfterEach
    fun tearDown() {
        // Clearing the timezone after the test.
        TimeZone.setDefault(null)
    }

    @Test
    fun `toViewState should return Empty when SendData is empty`() {
        val sendData = SendData(emptyList())

        val result = sendData.toViewState()

        assertEquals(SendState.ViewState.Empty, result)
    }

    @Test
    fun `toViewState should return Content when SendData is not empty`() {
        val list = listOf(
            createMockSendView(number = 1, type = SendType.FILE),
            createMockSendView(number = 2, type = SendType.TEXT),
        )
        val sendData = SendData(list)

        val result = sendData.toViewState()

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
                    ),
                    SendState.ViewState.Content.SendItem(
                        id = "mockId-2",
                        name = "mockName-2",
                        deletionDate = "Oct 27, 2023, 12:00 PM",
                        type = SendState.ViewState.Content.SendItem.Type.TEXT,
                    ),
                ),
            ),
            result,
        )
    }
}
