package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendViewTest {

    @Test
    fun `toSendUrl should create an appropriate url`() {
        val sendView = createMockSendView(number = 1)

        val result = sendView.toSendUrl(baseWebSendUrl = "www.test.com/")

        assertEquals("www.test.com/mockAccessId-1/mockKey-1", result)
    }
}
