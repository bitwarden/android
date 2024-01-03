package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendDataExtensionsTest {

    @Test
    fun `toViewState should return Empty when SendData is empty`() {
        val sendData = SendData(emptyList())

        val result = sendData.toViewState()

        assertEquals(SendState.ViewState.Empty, result)
    }

    @Test
    fun `toViewState should return Content when SendData is not empty`() {
        val list = listOf(
            createMockSendView(number = 1),
        )
        val sendData = SendData(list)

        val result = sendData.toViewState()

        assertEquals(SendState.ViewState.Content, result)
    }
}
